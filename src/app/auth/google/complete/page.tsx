'use client';

import { useEffect, useState, Suspense } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { Loader2, Globe, Briefcase, Users } from 'lucide-react';
import { authApi } from '@/lib/api';

const COUNTRIES = [
  { code: 'KR', name: '대한민국' },
  { code: 'US', name: '미국' },
  { code: 'JP', name: '일본' },
  { code: 'CN', name: '중국' },
  { code: 'GB', name: '영국' },
  { code: 'OTHER', name: '기타' },
];

const JOB_CATEGORIES = [
  '학생',
  '직장인',
  '프리랜서',
  '자영업',
  '무직',
  '기타',
];

const AGE_GROUPS = [
  { value: 10, label: '10대' },
  { value: 20, label: '20대' },
  { value: 30, label: '30대' },
  { value: 40, label: '40대' },
  { value: 50, label: '50대 이상' },
];

function GoogleCompleteContent() {
  const router = useRouter();
  const searchParams = useSearchParams();

  const [googleId, setGoogleId] = useState<string | null>(null);
  const [email, setEmail] = useState<string | null>(null);
  const [name, setName] = useState<string | null>(null);

  const [countryCode, setCountryCode] = useState('KR');
  const [jobCategory, setJobCategory] = useState('');
  const [ageGroup, setAgeGroup] = useState(20);

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    // URL에서 파라미터 추출
    const googleIdParam = searchParams.get('googleId');
    const emailParam = searchParams.get('email');
    const nameParam = searchParams.get('name');

    if (!googleIdParam || !emailParam) {
      setError('잘못된 접근입니다.');
      return;
    }

    setGoogleId(googleIdParam);
    setEmail(decodeURIComponent(emailParam));
    setName(nameParam ? decodeURIComponent(nameParam) : null);
  }, [searchParams]);

  const handleSubmit = async () => {
    if (!googleId || !email) {
      setError('필수 정보가 누락되었습니다.');
      return;
    }

    if (!countryCode) {
      setError('국가를 선택해주세요.');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const result = await authApi.completeGoogleRegistration({
        googleId,
        email,
        countryCode,
        jobCategory: jobCategory || undefined,
        ageGroup,
      });

      if (result.success && result.token && result.memberId) {
        // 토큰과 memberId를 localStorage에 저장
        localStorage.setItem('token', result.token);
        localStorage.setItem('memberId', result.memberId.toString());

        // callback 페이지로 redirect
        router.push(`/auth/google/callback?token=${result.token}&memberId=${result.memberId}`);
      } else {
        setError(result.message || '회원가입에 실패했습니다.');
      }
    } catch (err: any) {
      setError(err?.data?.message || err?.message || '회원가입 중 오류가 발생했습니다.');
    } finally {
      setLoading(false);
    }
  };

  if (error && !googleId) {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center p-6 bg-slate-50 dark:bg-slate-950">
        <div className="max-w-md w-full p-10 rounded-3xl bg-white dark:bg-slate-900 border border-slate-100 dark:border-slate-800 shadow-2xl text-center">
          <div className="w-16 h-16 rounded-full bg-rose-100 dark:bg-rose-900/20 flex items-center justify-center mx-auto mb-6">
            <span className="text-3xl">❌</span>
          </div>
          <h1 className="text-2xl font-black text-slate-900 dark:text-white mb-4">
            오류
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
      <div className="max-w-md w-full p-10 rounded-3xl bg-white dark:bg-slate-900 border border-slate-100 dark:border-slate-800 shadow-2xl">
        <div className="text-center mb-8">
          <div className="w-16 h-16 rounded-full bg-indigo-100 dark:bg-indigo-900/20 flex items-center justify-center mx-auto mb-4">
            <span className="text-3xl">✨</span>
          </div>
          <h1 className="text-2xl font-black text-slate-900 dark:text-white mb-2">
            추가 정보 입력
          </h1>
          <p className="text-slate-600 dark:text-slate-400 text-sm">
            {name && `${name}님, `}회원가입을 완료하기 위해 추가 정보를 입력해주세요.
          </p>
        </div>

        <div className="space-y-4">
          {/* 국가 선택 */}
          <div>
            <label className="flex items-center gap-2 text-sm font-bold text-slate-900 dark:text-white mb-2">
              <Globe size={16} />
              국가 <span className="text-rose-500">*</span>
            </label>
            <select
              value={countryCode}
              onChange={(e) => setCountryCode(e.target.value)}
              className="w-full p-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-white font-medium focus:outline-none focus:ring-2 focus:ring-indigo-500"
            >
              {COUNTRIES.map((country) => (
                <option key={country.code} value={country.code}>
                  {country.name}
                </option>
              ))}
            </select>
          </div>

          {/* 직업 선택 */}
          <div>
            <label className="flex items-center gap-2 text-sm font-bold text-slate-900 dark:text-white mb-2">
              <Briefcase size={16} />
              직업 (선택)
            </label>
            <select
              value={jobCategory}
              onChange={(e) => setJobCategory(e.target.value)}
              className="w-full p-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-white font-medium focus:outline-none focus:ring-2 focus:ring-indigo-500"
            >
              <option value="">선택 안 함</option>
              {JOB_CATEGORIES.map((job) => (
                <option key={job} value={job}>
                  {job}
                </option>
              ))}
            </select>
          </div>

          {/* 연령대 선택 */}
          <div>
            <label className="flex items-center gap-2 text-sm font-bold text-slate-900 dark:text-white mb-2">
              <Users size={16} />
              연령대 (선택)
            </label>
            <select
              value={ageGroup}
              onChange={(e) => setAgeGroup(parseInt(e.target.value))}
              className="w-full p-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-white font-medium focus:outline-none focus:ring-2 focus:ring-indigo-500"
            >
              {AGE_GROUPS.map((age) => (
                <option key={age.value} value={age.value}>
                  {age.label}
                </option>
              ))}
            </select>
          </div>
        </div>

        {error && (
          <p className="mt-4 text-sm text-rose-500 font-medium text-center">
            {error}
          </p>
        )}

        <button
          onClick={handleSubmit}
          disabled={loading || !countryCode}
          className="w-full mt-6 py-4 rounded-xl bg-indigo-600 text-white font-bold hover:bg-indigo-700 transition-all active:scale-95 disabled:opacity-50 flex items-center justify-center gap-2"
        >
          {loading ? (
            <>
              <Loader2 size={20} className="animate-spin" />
              <span>처리 중...</span>
            </>
          ) : (
            <span>회원가입 완료</span>
          )}
        </button>
      </div>
    </div>
  );
}

export default function GoogleCompletePage() {
  return (
    <Suspense fallback={
      <div className="min-h-screen flex items-center justify-center">
        <Loader2 size={40} className="animate-spin text-indigo-600" />
      </div>
    }>
      <GoogleCompleteContent />
    </Suspense>
  );
}
