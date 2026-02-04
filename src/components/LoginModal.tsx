'use client';

import { useState } from 'react';
import { MousePointer2, Sun, Moon, ArrowLeft, Loader2, Mail, Lock } from 'lucide-react';
import PredataLogo from '@/components/ui/PredataLogo';
import { useTheme } from '@/hooks/useTheme';
import { useAuth } from '@/hooks/useAuth';
import { useRegisterModal } from '@/components/RegisterModal';
import { authApi } from '@/lib/api';

type Step = 'main' | 'email-login';

export default function LoginModal() {
  const { isDark, toggleTheme } = useTheme();
  const { loginAsGuest, loginById } = useAuth();
  const { open: openRegisterModal } = useRegisterModal();

  const [step, setStep] = useState<Step>('main');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');

  // 이메일 + 비밀번호 로그인
  const handleEmailLogin = async () => {
    if (!email.trim() || !email.includes('@')) {
      setError('유효한 이메일을 입력해주세요.');
      return;
    }
    if (!password || password.length < 6) {
      setError('비밀번호를 입력해주세요.');
      return;
    }
    setError('');
    setLoading(true);
    try {
      const result = await authApi.login(email.trim(), password);
      if (result.success && result.token && result.memberId) {
        await loginById(result.memberId);
      } else {
        setError(result.message || '로그인에 실패했습니다.');
      }
    } catch (err: any) {
      setError(err?.data?.message || err?.message || '로그인에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const goBack = () => {
    setStep('main');
    setEmail('');
    setPassword('');
    setError('');
  };

  return (
    <div className={`min-h-screen flex flex-col items-center justify-center p-6 transition-all duration-500 ${isDark ? 'bg-slate-950' : 'bg-slate-50'}`}>
      <div className="mb-12 scale-125">
        <PredataLogo />
      </div>

      <div className={`max-w-md w-full p-10 rounded-[2.5rem] border text-center shadow-2xl transition-all ${isDark ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-100'}`}>

        {step === 'main' && (
          <>
            <h1 className={`text-3xl font-black mb-4 ${isDark ? 'text-white' : 'text-slate-900'}`}>
              데이터로 예측하는<br />현명한 미래
            </h1>
            <p className="text-slate-500 mb-10 text-sm leading-relaxed">
              PRE(D)ATA는 지능형 오라클과 AI를 활용한<br />차세대 탈중앙화 예측 플랫폼입니다.
            </p>

            <div className="space-y-3">
              <button
                onClick={openRegisterModal}
                disabled={loading}
                className="w-full py-4 rounded-2xl font-bold text-lg flex items-center justify-center space-x-3 bg-indigo-600 text-white hover:bg-indigo-700 transition-all active:scale-95 disabled:opacity-50"
              >
                <Mail size={20} />
                <span>이메일로 시작하기</span>
              </button>

              <button
                onClick={() => { setStep('email-login'); setError(''); }}
                className="w-full text-sm text-slate-400 hover:text-indigo-500 transition-colors"
              >
                이미 계정이 있으신가요? <span className="font-semibold">로그인</span>
              </button>

              <button
                onClick={loginAsGuest}
                className="w-full pt-4 text-sm text-slate-400 hover:text-indigo-500 transition-colors flex items-center justify-center space-x-1"
              >
                <MousePointer2 size={14} />
                <span>게스트로 둘러보기</span>
              </button>
            </div>

            {error && (
              <p className="mt-4 text-sm text-rose-500 font-medium">{error}</p>
            )}
          </>
        )}

        {step === 'email-login' && (
          <>
            <button onClick={goBack} className="flex items-center gap-1 text-sm text-slate-400 hover:text-indigo-500 transition mb-6">
              <ArrowLeft size={16} /> 뒤로
            </button>

            <div className="flex justify-center mb-4">
              <div className={`w-14 h-14 rounded-2xl flex items-center justify-center ${isDark ? 'bg-indigo-500/20' : 'bg-indigo-50'}`}>
                <Lock size={28} className="text-indigo-500" />
              </div>
            </div>

            <h2 className={`text-2xl font-black mb-2 ${isDark ? 'text-white' : 'text-slate-900'}`}>
              로그인
            </h2>
            <p className="text-slate-500 text-sm mb-6">
              이메일과 비밀번호를 입력해주세요.
            </p>

            <div className="space-y-4 mb-4">
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="이메일"
                className={`w-full p-4 rounded-2xl border text-sm font-medium transition ${isDark ? 'bg-slate-800 border-slate-700 text-white placeholder:text-slate-500 focus:border-indigo-500' : 'bg-slate-50 border-slate-200 text-slate-900 placeholder:text-slate-400 focus:border-indigo-500'}`}
                autoFocus
              />
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && handleEmailLogin()}
                placeholder="비밀번호"
                className={`w-full p-4 rounded-2xl border text-sm font-medium transition ${isDark ? 'bg-slate-800 border-slate-700 text-white placeholder:text-slate-500 focus:border-indigo-500' : 'bg-slate-50 border-slate-200 text-slate-900 placeholder:text-slate-400 focus:border-indigo-500'}`}
              />
            </div>

            {error && (
              <p className="mb-4 text-sm text-rose-500 font-medium">{error}</p>
            )}

            <button
              onClick={handleEmailLogin}
              disabled={loading || !email.trim() || !password}
              className="w-full py-4 rounded-2xl font-bold text-lg bg-indigo-600 text-white hover:bg-indigo-700 transition-all active:scale-95 disabled:opacity-50 flex items-center justify-center space-x-2"
            >
              {loading ? <Loader2 size={20} className="animate-spin" /> : <Lock size={20} />}
              <span>{loading ? '로그인 중...' : '로그인'}</span>
            </button>
          </>
        )}

        <div className={`mt-8 pt-8 border-t flex justify-center ${isDark ? 'border-slate-800' : 'border-slate-100'}`}>
          <button onClick={toggleTheme} className="text-slate-400 hover:text-indigo-500 transition-colors">
            {isDark ? <Sun size={20} /> : <Moon size={20} />}
          </button>
        </div>
      </div>

      <p className="mt-8 text-xs text-slate-400">&copy; 2025 PRE(D)ATA. All rights reserved.</p>
    </div>
  );
}
