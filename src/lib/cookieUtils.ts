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
