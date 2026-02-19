'use client';

import { useEffect, useState, useRef, Suspense } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { Loader2 } from 'lucide-react';
import { useAuth } from '@/hooks/useAuth';

function GoogleCallbackContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { loginById } = useAuth();
  const [error, setError] = useState<string | null>(null);
  const isProcessed = useRef(false);

  useEffect(() => {
    // Prevent duplicate execution
    if (isProcessed.current) return;
    isProcessed.current = true;

    const tokenParam = searchParams.get('token');
    const memberIdParam = searchParams.get('memberId');
    const errorParam = searchParams.get('error');
    const messageParam = searchParams.get('message');
    const hasCallbackParams = Boolean(tokenParam || memberIdParam || errorParam || messageParam);

    // Redirect to home if token exists and no callback params
    if (localStorage.getItem('token') && !hasCallbackParams) {
      router.replace('/');
      return;
    }

    const handleCallback = async () => {
      // Read token from URL fragment (#token=xxx) for enhanced security
      let token = tokenParam;
      let memberId = memberIdParam;

      // Try parsing token from fragment (priority)
      if (typeof window !== 'undefined' && window.location.hash) {
        const hash = window.location.hash.substring(1); // Remove #
        const hashParams = new URLSearchParams(hash);
        const hashToken = hashParams.get('token');
        const hashMemberId = hashParams.get('memberId');

        if (hashToken && hashMemberId) {
          token = hashToken;
          memberId = hashMemberId;
          // Remove fragment params (security)
          window.history.replaceState(null, '', window.location.pathname + window.location.search);
        }
      }

      if (errorParam) {
        // Map error codes to user-friendly messages
        if (errorParam === 'session_expired' || errorParam.includes('authorization_request_not_found')) {
          setError('Session expired. Please login again.');
        } else if (errorParam === 'login_failed') {
          setError('Login failed. Please try again.');
        } else {
          // Other errors (prevent internal message exposure)
          setError(messageParam ? decodeURIComponent(messageParam) : 'An error occurred during login.');
        }
        return;
      }

      if (token && memberId) {
        try {
          // Save token to localStorage
          localStorage.setItem('token', token);
          localStorage.setItem('memberId', memberId);

          const parsedMemberId = Number(memberId);
          if (!Number.isInteger(parsedMemberId) || parsedMemberId <= 0) {
            localStorage.removeItem('token');
            localStorage.removeItem('memberId');
            setError('Invalid user information. Please login again.');
            return;
          }

          // Process login via AuthProvider + validate result
          const member = await loginById(parsedMemberId);
          if (!member) {
            localStorage.removeItem('token');
            localStorage.removeItem('memberId');
            setError('Failed to verify login session. Please login again.');
            return;
          }

          // Redirect to home — use replace to remove callback URL from history
          router.replace('/');
        } catch (err) {
          setError('An error occurred during login processing.');
        }
      } else if (!hasCallbackParams) {
        router.replace('/');
      } else {
        setError('Invalid access. Please login again.');
      }
    };

    handleCallback();
  }, [searchParams, router, loginById]);

  if (error) {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center p-6 bg-slate-50 dark:bg-slate-950">
        <div className="max-w-md w-full p-10 rounded-3xl bg-white dark:bg-slate-900 border border-slate-100 dark:border-slate-800 shadow-2xl text-center">
          <div className="w-16 h-16 rounded-full bg-rose-100 dark:bg-rose-900/20 flex items-center justify-center mx-auto mb-6">
            <span className="text-3xl">❌</span>
          </div>
          <h1 className="text-2xl font-black text-slate-900 dark:text-white mb-4">
            Login Failed
          </h1>
          <p className="text-slate-600 dark:text-slate-400 mb-8">
            {error}
          </p>
          <button
            onClick={() => router.push('/')}
            className="w-full py-3 rounded-xl bg-indigo-600 text-white font-bold hover:bg-indigo-700 transition"
          >
            Back to Home
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen flex flex-col items-center justify-center p-6 bg-slate-50 dark:bg-slate-950">
      <div className="text-center">
        <Loader2 size={48} className="animate-spin text-indigo-600 mx-auto mb-4" />
        <h1 className="text-xl font-bold text-slate-900 dark:text-white mb-2">
          Processing Google login...
        </h1>
        <p className="text-slate-600 dark:text-slate-400">
          Please wait.
        </p>
      </div>
    </div>
  );
}

export default function GoogleCallbackPage() {
  return (
    <Suspense fallback={
      <div className="min-h-screen flex items-center justify-center">
        <Loader2 size={48} className="animate-spin text-indigo-600" />
      </div>
    }>
      <GoogleCallbackContent />
    </Suspense>
  );
}
