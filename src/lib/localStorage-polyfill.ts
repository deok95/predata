/**
 * localStorage polyfill for SSR
 * Next.js 서버 사이드 렌더링 환경에서 localStorage 에러 방지
 */

if (typeof window === 'undefined') {
  // 서버 환경에서는 localStorage mock 제공
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
