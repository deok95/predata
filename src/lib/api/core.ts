const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

export const BACKEND_URL = API_URL;
export const API_BASE_URL = `${API_URL}/api`;

export function authFetch(url: string, options?: RequestInit): Promise<Response> {
  const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null;

  return fetch(url, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(token && { Authorization: `Bearer ${token}` }),
      ...options?.headers,
    },
  });
}

export class ApiError extends Error {
  constructor(
    public status: number,
    public message: string,
    public data?: unknown
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

interface ApiEnvelope<T> {
  success: boolean;
  data?: T;
  message?: string;
}

function isApiEnvelope<T>(raw: unknown): raw is ApiEnvelope<T> {
  return typeof raw === 'object' && raw !== null && 'success' in raw;
}

export function unwrapApiEnvelope<T>(raw: T | ApiEnvelope<T>): T {
  if (isApiEnvelope<T>(raw)) {
    if (!raw.success) {
      throw new Error(raw.message || '요청이 실패했습니다.');
    }

    if (raw.data === undefined) {
      throw new Error(raw.message || '응답 데이터가 없습니다.');
    }

    return raw.data;
  }

  return raw;
}

export async function apiRequest<T>(endpoint: string, options?: RequestInit): Promise<T> {
  const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null;

  try {
    const fetchOnce = async () => {
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 15000);
      try {
        return await fetch(`${API_URL}${endpoint}`, {
          headers: {
            'Content-Type': 'application/json',
            ...(token && { Authorization: `Bearer ${token}` }),
            ...options?.headers,
          },
          ...options,
          signal: controller.signal,
        });
      } finally {
        clearTimeout(timeoutId);
      }
    };

    let response = await fetchOnce();

    // 429 Rate Limit 대응: 안전한 메서드(GET, HEAD)만 1회 재시도
    if (response.status === 429) {
      const method = (options?.method || 'GET').toUpperCase();
      const isSafeMethod = method === 'GET' || method === 'HEAD';

      if (!isSafeMethod) {
        // POST, PUT, DELETE, PATCH 등은 재시도하지 않고 즉시 에러 throw
        let data;
        try {
          data = await response.json();
        } catch {
          throw new ApiError(429, 'Rate limit exceeded (안전하지 않은 메서드는 재시도하지 않습니다)');
        }
        throw new ApiError(429, data.message || 'Rate limit exceeded', data);
      }

      // Retry-After 헤더 값 사용, 없으면 기본 2초, 최대 5초로 제한
      const retryAfterRaw = response.headers.get('Retry-After');
      let retryAfterSeconds = 2; // 기본값 2초

      if (retryAfterRaw) {
        const parsed = parseInt(retryAfterRaw, 10);
        if (Number.isFinite(parsed) && parsed > 0) {
          retryAfterSeconds = Math.min(parsed, 5); // 최대 5초로 cap
        }
      }

      await new Promise(r => setTimeout(r, retryAfterSeconds * 1000));

      // NOTE: body는 string(JSON) 기반을 전제로 1회 재요청합니다. (Stream body는 재사용 불가)
      response = await fetchOnce();
    }

    let data;
    try {
      data = await response.json();
    } catch {
      throw new ApiError(response.status, '서버 응답을 처리할 수 없습니다.');
    }

    if (!response.ok) {
      if (response.status === 401 && typeof window !== 'undefined') {
        // 게스트 체크: 게스트는 401이 발생해도 로그아웃하지 않음
        const savedUser = localStorage.getItem('predataUser');
        let isGuest = false;

        if (savedUser) {
          try {
            const user = JSON.parse(savedUser);
            isGuest = user.id < 0 || (user.email?.endsWith('@predata.demo') ?? false);
          } catch {
            // JSON 파싱 실패 시 게스트 아님
          }
        }

        // 게스트가 아닌 경우에만 로그아웃 처리
        if (!isGuest) {
          const { clearAllAuthCookies } = await import('@/lib/cookieUtils');
          localStorage.removeItem('predataUser');
          localStorage.removeItem('token');
          localStorage.removeItem('memberId');
          clearAllAuthCookies();
          if (window.location.pathname !== '/') {
            window.location.href = '/';
          }
        }
      }

      throw new ApiError(
        response.status,
        data.message || `HTTP ${response.status}: ${response.statusText}`,
        data
      );
    }

    return data;
  } catch (error) {
    if (error instanceof ApiError) {
      throw error;
    }

    if (error instanceof DOMException && error.name === 'AbortError') {
      throw new ApiError(408, '서버 응답 시간이 초과되었습니다.');
    }

    throw new ApiError(
      500,
      error instanceof Error ? error.message : '네트워크 오류가 발생했습니다.',
      error
    );
  }
}
