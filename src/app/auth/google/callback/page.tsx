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
    // 중복 실행 방지
    if (isProcessed.current) return;
    isProcessed.current = true;

    // 이미 로그인된 상태면 콜백 재처리 방지
    if (localStorage.getItem('token')) {
      router.replace('/');
      return;
    }

    const handleCallback = async () => {
      // URL에서 파라미터 추출
      const token = searchParams.get('token');
      const memberId = searchParams.get('memberId');
      const errorParam = searchParams.get('error');

      if (errorParam) {
        setError(decodeURIComponent(errorParam));
        return;
      }

      if (token && memberId) {
        try {
          // localStorage에 토큰 저장
          localStorage.setItem('token', token);
          localStorage.setItem('memberId', memberId);

          // AuthProvider를 통해 로그인 처리
          await loginById(parseInt(memberId));

          // 홈으로 redirect — replace로 콜백 URL을 히스토리에서 제거
          router.replace('/');
        } catch (err) {
          setError('로그인 처리 중 오류가 발생했습니다.');
        }
      } else {
        setError('잘못된 접근입니다.');
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
            로그인 실패
          </h1>
          <p className="text-slate-600 dark:text-slate-400 mb-8">
            {error}
          </p>
          <button
            onClick={() => router.push('/')}
            className="w-full py-3 rounded-xl bg-indigo-600 text-white font-bold hover:bg-indigo-700 transition"
          >
            홈으로 돌아가기
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
          Google 로그인 처리 중...
        </h1>
        <p className="text-slate-600 dark:text-slate-400">
          잠시만 기다려주세요.
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
