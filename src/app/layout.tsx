'use client'

import "./globals.css";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { Web3Provider } from "@/lib/Web3Provider";
import { AuthProvider } from "@/hooks/useAuth";
import { ThemeProvider } from "@/hooks/useTheme";
import ErrorBoundary from "@/components/ErrorBoundary";
import '@/lib/localStorage-polyfill'; // localStorage polyfill

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});

// Cannot export metadata directly in client component
// Must use Head or separate server component instead

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <head>
        <title>Predata - Web3 Prediction Market</title>
        <meta name="description" content="Transparent and fair prediction market platform" />
      </head>
      <body
        className="antialiased"
      >
        <ErrorBoundary>
          <QueryClientProvider client={queryClient}>
            <ThemeProvider>
              <Web3Provider>
                <AuthProvider>
                  {children}
                </AuthProvider>
              </Web3Provider>
            </ThemeProvider>
          </QueryClientProvider>
        </ErrorBoundary>
      </body>
    </html>
  );
}
