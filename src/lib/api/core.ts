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
      throw new Error(raw.message || 'Request failed.');
    }

    if (raw.data === undefined) {
      throw new Error(raw.message || 'No response data.');
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

    // 429 Rate Limit handling: Retry only safe methods (GET, HEAD) once
    if (response.status === 429) {
      const method = (options?.method || 'GET').toUpperCase();
      const isSafeMethod = method === 'GET' || method === 'HEAD';

      if (!isSafeMethod) {
        // POST, PUT, DELETE, PATCH are not retried and throw error immediately
        let data;
        try {
          data = await response.json();
        } catch {
          throw new ApiError(429, 'Rate limit exceeded (unsafe methods are not retried)');
        }
        throw new ApiError(429, data.message || 'Rate limit exceeded', data);
      }

      // Use Retry-After header value, default 2 seconds, max 5 seconds
      const retryAfterRaw = response.headers.get('Retry-After');
      let retryAfterSeconds = 2; // Default 2 seconds

      if (retryAfterRaw) {
        const parsed = parseInt(retryAfterRaw, 10);
        if (Number.isFinite(parsed) && parsed > 0) {
          retryAfterSeconds = Math.min(parsed, 5); // Cap at max 5 seconds
        }
      }

      await new Promise(r => setTimeout(r, retryAfterSeconds * 1000));

      // NOTE: Assumes body is string (JSON) based for single retry. (Stream body cannot be reused)
      response = await fetchOnce();
    }

    let data;
    try {
      data = await response.json();
    } catch {
      throw new ApiError(response.status, 'Unable to process server response.');
    }

    if (!response.ok) {
      if (response.status === 401 && typeof window !== 'undefined') {
        // Guest check: Guests are not logged out on 401
        const savedUser = localStorage.getItem('predataUser');
        let isGuest = false;

        if (savedUser) {
          try {
            const user = JSON.parse(savedUser);
            isGuest = user.id < 0 || (user.email?.endsWith('@predata.demo') ?? false);
          } catch {
            // Not a guest if JSON parsing fails
          }
        }

        // Logout only if not a guest
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
      throw new ApiError(408, 'Server response timeout.');
    }

    throw new ApiError(
      500,
      error instanceof Error ? error.message : 'Network error occurred.',
      error
    );
  }
}
