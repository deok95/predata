'use client';

import { useState, useEffect } from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { GoogleOAuthProvider } from '@/providers/GoogleOAuthProvider';
import { Web3Provider } from '@/lib/Web3Provider';
import { AuthProvider } from '@/hooks/useAuth';
import { ThemeProvider } from '@/hooks/useTheme';
import { I18nProvider } from '@/hooks/useI18n';
import ErrorBoundary from '@/components/ErrorBoundary';
import '@/lib/localStorage-polyfill';

function ServiceWorkerRegistrar() {
  useEffect(() => {
    if ('serviceWorker' in navigator && process.env.NODE_ENV === 'production') {
      navigator.serviceWorker.register('/sw.js').catch(() => {});
    }
  }, []);
  return null;
}

export default function ClientProviders({
  children,
}: {
  children: React.ReactNode;
}) {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            retry: 1,
            refetchOnWindowFocus: false,
          },
        },
      })
  );

  return (
    <ErrorBoundary>
      <GoogleOAuthProvider>
        <QueryClientProvider client={queryClient}>
          <ThemeProvider>
            <Web3Provider>
              <AuthProvider>
                <I18nProvider>
                  <ServiceWorkerRegistrar />
                  {children}
                </I18nProvider>
              </AuthProvider>
            </Web3Provider>
          </ThemeProvider>
        </QueryClientProvider>
      </GoogleOAuthProvider>
    </ErrorBoundary>
  );
}
