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
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), 15000);
  const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null;

  try {
    const response = await fetch(`${API_URL}${endpoint}`, {
      headers: {
        'Content-Type': 'application/json',
        ...(token && { Authorization: `Bearer ${token}` }),
        ...options?.headers,
      },
      ...options,
      signal: controller.signal,
    });

    let data;
    try {
      data = await response.json();
    } catch {
      throw new ApiError(response.status, '서버 응답을 처리할 수 없습니다.');
    }

    if (!response.ok) {
      if (response.status === 401 && typeof window !== 'undefined') {
        const { clearAllAuthCookies } = await import('@/lib/cookieUtils');
        localStorage.removeItem('predataUser');
        localStorage.removeItem('token');
        localStorage.removeItem('memberId');
        clearAllAuthCookies();
        if (window.location.pathname !== '/') {
          window.location.href = '/';
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
  } finally {
    clearTimeout(timeoutId);
  }
}
