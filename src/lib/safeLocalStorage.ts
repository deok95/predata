/**
 * Browser-only localStorage wrapper
 * Safe to use in SSR environment
 */

export const safeLocalStorage = {
  getItem: (key: string): string | null => {
    if (typeof window === 'undefined') return null;
    try {
      return localStorage.getItem(key);
    } catch (error) {
      if (process.env.NODE_ENV === 'development') {
        console.error('localStorage.getItem error:', error);
      }
      return null;
    }
  },

  setItem: (key: string, value: string): void => {
    if (typeof window === 'undefined') return;
    try {
      localStorage.setItem(key, value);
    } catch (error) {
      if (process.env.NODE_ENV === 'development') {
        console.error('localStorage.setItem error:', error);
      }
    }
  },

  removeItem: (key: string): void => {
    if (typeof window === 'undefined') return;
    try {
      localStorage.removeItem(key);
    } catch (error) {
      if (process.env.NODE_ENV === 'development') {
        console.error('localStorage.removeItem error:', error);
      }
    }
  },

  clear: (): void => {
    if (typeof window === 'undefined') return;
    try {
      localStorage.clear();
    } catch (error) {
      if (process.env.NODE_ENV === 'development') {
        console.error('localStorage.clear error:', error);
      }
    }
  }
};
