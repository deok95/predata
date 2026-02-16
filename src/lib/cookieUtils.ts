const COOKIE_MAX_AGE = 7 * 24 * 60 * 60; // 7 days

export function setAuthCookies(isAdmin: boolean): void {
  if (typeof document === 'undefined') return;

  document.cookie = `predata-auth=true; path=/; max-age=${COOKIE_MAX_AGE}; SameSite=Lax`;

  if (isAdmin) {
    document.cookie = `predata-admin=true; path=/; max-age=${COOKIE_MAX_AGE}; SameSite=Lax`;
  } else {
    document.cookie = `predata-admin=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/`;
  }
}

export function clearAllAuthCookies(): void {
  if (typeof document === 'undefined') return;

  const paths = ['/', window.location.pathname];
  const hostname = window.location.hostname;

  for (const name of ['predata-auth', 'predata-admin']) {
    for (const path of paths) {
      document.cookie = `${name}=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=${path};`;
      document.cookie = `${name}=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=${path}; domain=${hostname};`;
      if (hostname.includes('.')) {
        document.cookie = `${name}=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=${path}; domain=.${hostname};`;
      }
    }
  }
}
