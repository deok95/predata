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

    const tokenParam = searchParams.get('token');
    const memberIdParam = searchParams.get('memberId');
    const errorParam = searchParams.get('error');
    const messageParam = searchParams.get('message');
    const hasCallbackParams = Boolean(tokenParam || memberIdParam || errorParam || messageParam);

    // 콜백 파라미터 없는 일반 진입일 때만 홈으로
    if (localStorage.getItem('token') && !hasCallbackParams) {
      router.replace('/');
      return;
    }

    const handleCallback = async () => {
      const token = tokenParam;
      const memberId = memberIdParam;

      if (errorParam) {
        // 에러 코드별 사용자 친화적 메시지 매핑
        if (errorParam === 'session_expired' || errorParam.includes('authorization_request_not_found')) {
          setError('세션이 만료되었습니다. 다시 로그인해주세요.');
        } else if (errorParam === 'login_failed') {
          setError('로그인에 실패했습니다. 다시 시도해주세요.');
        } else {
          // 기타 에러 (내부 메시지 노출 방지)
          setError(messageParam ? decodeURIComponent(messageParam) : '로그인 중 오류가 발생했습니다.');
        }
        return;
      }

      if (token && memberId) {
        try {
          // localStorage에 토큰 저장
          localStorage.setItem('token', token);
          localStorage.setItem('memberId', memberId);

          const parsedMemberId = Number(memberId);
          if (!Number.isInteger(parsedMemberId) || parsedMemberId <= 0) {
            localStorage.removeItem('token');
            localStorage.removeItem('memberId');
            setError('잘못된 사용자 정보입니다. 다시 로그인해주세요.');
            return;
          }

          // AuthProvider를 통해 로그인 처리 + 결과 검증
          const member = await loginById(parsedMemberId);
          if (!member) {
            localStorage.removeItem('token');
            localStorage.removeItem('memberId');
            setError('로그인 세션 확인에 실패했습니다. 다시 로그인해주세요.');
            return;
          }

          // 홈으로 redirect — replace로 콜백 URL을 히스토리에서 제거
          router.replace('/');
        } catch (err) {
          setError('로그인 처리 중 오류가 발생했습니다.');
        }
      } else if (!hasCallbackParams) {
        router.replace('/');
      } else {
        setError('잘못된 접근입니다. 다시 로그인해주세요.');
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
