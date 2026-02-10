'use client';

import { GoogleOAuthProvider as GoogleProvider } from '@react-oauth/google';
import type { ReactNode } from 'react';

export function GoogleOAuthProvider({ children }: { children: ReactNode }) {
  const clientId = process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID || '';

  // 항상 GoogleProvider로 감싸서 context 에러 방지
  return (
    <GoogleProvider clientId={clientId}>
      {children}
    </GoogleProvider>
  );
}
