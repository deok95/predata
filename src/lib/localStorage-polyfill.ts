/**
 * localStorage polyfill for SSR
 * Prevents localStorage errors in Next.js server-side rendering environment
 */

if (typeof window === 'undefined') {
  // Provide localStorage mock in server environment
  global.localStorage = {
    getItem: () => null,
    setItem: () => {},
    removeItem: () => {},
    clear: () => {},
    key: () => null,
    length: 0,
  } as Storage;
}

export {};
