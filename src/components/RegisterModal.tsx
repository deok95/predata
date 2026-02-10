'use client';

import { useState, useRef, createContext, useContext, useCallback } from 'react';
import type { ReactNode } from 'react';
import { X, Loader2, Mail, ArrowLeft, ShieldCheck, Lock } from 'lucide-react';
import { useTheme } from '@/hooks/useTheme';
import { useAuth } from '@/hooks/useAuth';
import { authApi } from '@/lib/api';

// --- Context: 어디서든 모달을 열 수 있음 ---
const RegisterModalContext = createContext<{ open: () => void } | null>(null);

export function useRegisterModal() {
  const ctx = useContext(RegisterModalContext);
  if (!ctx) throw new Error('useRegisterModal must be used within RegisterModalProvider');
  return ctx;
}

export function RegisterModalProvider({ children }: { children: ReactNode }) {
  const [isOpen, setIsOpen] = useState(false);
  const open = useCallback(() => setIsOpen(true), []);
  const close = useCallback(() => setIsOpen(false), []);

  return (
    <RegisterModalContext.Provider value={{ open }}>
      {children}
      {isOpen && <RegisterModalInner onClose={close} />}
    </RegisterModalContext.Provider>
  );
}

type Step = 'email' | 'verify' | 'password';

function RegisterModalInner({ onClose }: { onClose: () => void }) {
  const { isDark } = useTheme();
  const { loginById } = useAuth();

  const [step, setStep] = useState<Step>('email');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  // State for each step
  const [email, setEmail] = useState('');
  const [verificationCode, setVerificationCode] = useState('');
  const [password, setPassword] = useState('');
  const [passwordConfirm, setPasswordConfirm] = useState('');

  const codeInputRef = useRef<HTMLInputElement>(null);
  const passwordInputRef = useRef<HTMLInputElement>(null);

  // Step 1: 이메일 입력 → 인증 코드 발송
  const handleSendCode = async () => {
    if (!email.trim() || !email.includes('@')) {
      setError('유효한 이메일을 입력해주세요.');
      return;
    }
    setError('');
    setLoading(true);
    try {
      const result = await authApi.sendCode(email.trim());
      if (result.success) {
        setStep('verify');
        setVerificationCode('');
        setTimeout(() => codeInputRef.current?.focus(), 100);
      } else {
        // 이미 가입된 이메일인 경우 특별 처리
        if (result.message?.includes('이미 가입된')) {
          setError('이미 가입된 회원입니다. 화면을 닫고 "로그인" 버튼을 눌러주세요.');
        } else {
          setError(result.message || '코드 발송에 실패했습니다.');
        }
      }
    } catch (err: any) {
      const errorMsg = err?.data?.message || err?.message || '코드 발송에 실패했습니다.';
      if (errorMsg.includes('이미 가입된')) {
        setError('이미 가입된 회원입니다. 화면을 닫고 "로그인" 버튼을 눌러주세요.');
      } else {
        setError(errorMsg);
      }
    } finally {
      setLoading(false);
    }
  };

  // Step 2: 인증 코드 검증
  const handleVerifyCode = async () => {
    if (verificationCode.length !== 6) {
      setError('6자리 인증 코드를 입력해주세요.');
      return;
    }
    setError('');
    setLoading(true);
    try {
      const result = await authApi.verifyCode(email.trim(), verificationCode);
      setStep('password');
      setPassword('');
      setPasswordConfirm('');
      setTimeout(() => passwordInputRef.current?.focus(), 100);
    } catch (err: any) {
      setError(err?.data?.message || err?.message || '인증에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  // Step 3: 비밀번호 설정 → 회원 생성 + 자동 로그인
  const handleCompleteSignup = async () => {
    if (!password || password.length < 6) {
      setError('비밀번호는 6자 이상이어야 합니다.');
      return;
    }
    if (password !== passwordConfirm) {
      setError('비밀번호가 일치하지 않습니다.');
      return;
    }
    setError('');
    setLoading(true);
    try {
      const result = await authApi.completeSignup(email.trim(), verificationCode, password, passwordConfirm);
      if (result.token && result.memberId) {
        localStorage.setItem('token', result.token);
        localStorage.setItem('memberId', result.memberId.toString());
        await loginById(result.memberId);
        onClose();
      } else {
        setError('회원가입에 실패했습니다.');
      }
    } catch (err: any) {
      setError(err?.data?.message || err?.message || '회원가입에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const goBack = () => {
    if (step === 'verify') {
      setStep('email');
      setVerificationCode('');
    } else if (step === 'password') {
      setStep('verify');
      setPassword('');
      setPasswordConfirm('');
    }
    setError('');
  };

  return (
    <div className="fixed inset-0 z-[100] flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-black/60" onClick={onClose} />
      <div className={`relative w-full max-w-md rounded-3xl border p-8 shadow-2xl ${isDark ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-100'}`}>
        <button onClick={onClose} className="absolute top-4 right-4 text-slate-400 hover:text-slate-300">
          <X size={20} />
        </button>

        {/* Step 1: 이메일 입력 */}
        {step === 'email' && (
          <div>
            <div className="flex justify-center mb-4">
              <div className={`w-14 h-14 rounded-2xl flex items-center justify-center ${isDark ? 'bg-indigo-500/20' : 'bg-indigo-50'}`}>
                <Mail size={28} className="text-indigo-500" />
              </div>
            </div>

            <h2 className={`text-2xl font-black mb-2 text-center ${isDark ? 'text-white' : 'text-slate-900'}`}>
              이메일 입력
            </h2>
            <p className="text-slate-500 text-sm mb-6 text-center">
              이메일을 입력하면 인증 코드가 발송됩니다.
            </p>

            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && handleSendCode()}
              placeholder="example@email.com"
              className={`w-full p-4 rounded-2xl border text-sm font-medium mb-4 transition ${isDark ? 'bg-slate-800 border-slate-700 text-white placeholder:text-slate-500 focus:border-indigo-500' : 'bg-slate-50 border-slate-200 text-slate-900 placeholder:text-slate-400 focus:border-indigo-500'}`}
              autoFocus
            />

            {error && <p className="mb-4 text-sm text-rose-500">{error}</p>}

            <button
              onClick={handleSendCode}
              disabled={loading || !email.trim()}
              className="w-full py-4 rounded-2xl font-bold text-lg bg-indigo-600 text-white hover:bg-indigo-700 transition-all active:scale-95 disabled:opacity-50 flex items-center justify-center gap-2"
            >
              {loading ? <Loader2 size={20} className="animate-spin" /> : <Mail size={20} />}
              <span>{loading ? '발송 중...' : '인증 코드 받기'}</span>
            </button>
          </div>
        )}

        {/* Step 2: 인증 코드 입력 */}
        {step === 'verify' && (
          <div>
            <button onClick={goBack} className="flex items-center gap-1 text-sm text-slate-400 hover:text-indigo-500 transition mb-4">
              <ArrowLeft size={16} /> 뒤로
            </button>

            <div className="flex justify-center mb-4">
              <div className={`w-14 h-14 rounded-2xl flex items-center justify-center ${isDark ? 'bg-indigo-500/20' : 'bg-indigo-50'}`}>
                <ShieldCheck size={28} className="text-indigo-500" />
              </div>
            </div>

            <h2 className={`text-2xl font-black mb-2 text-center ${isDark ? 'text-white' : 'text-slate-900'}`}>
              인증 코드 입력
            </h2>
            <p className="text-slate-500 text-sm mb-6 text-center">
              <span className="text-indigo-500 font-medium">{email}</span>으로<br />
              발송된 6자리 코드를 입력해주세요.
            </p>

            <input
              ref={codeInputRef}
              type="text"
              inputMode="numeric"
              maxLength={6}
              value={verificationCode}
              onChange={(e) => setVerificationCode(e.target.value.replace(/\D/g, '').slice(0, 6))}
              onKeyDown={(e) => e.key === 'Enter' && verificationCode.length === 6 && handleVerifyCode()}
              placeholder="000000"
              className={`w-full p-4 rounded-2xl border text-center text-2xl font-mono font-bold tracking-[0.5em] mb-4 transition ${isDark ? 'bg-slate-800 border-slate-700 text-white placeholder:text-slate-600 focus:border-indigo-500' : 'bg-slate-50 border-slate-200 text-slate-900 placeholder:text-slate-300 focus:border-indigo-500'}`}
              autoFocus
            />

            {error && <p className="mb-4 text-sm text-rose-500">{error}</p>}

            <button
              onClick={handleVerifyCode}
              disabled={loading || verificationCode.length !== 6}
              className="w-full py-4 rounded-2xl font-bold text-lg bg-indigo-600 text-white hover:bg-indigo-700 transition-all active:scale-95 disabled:opacity-50 flex items-center justify-center gap-2"
            >
              {loading ? <Loader2 size={20} className="animate-spin" /> : <ShieldCheck size={20} />}
              <span>{loading ? '확인 중...' : '인증 확인'}</span>
            </button>

            <button
              onClick={handleSendCode}
              disabled={loading}
              className="w-full mt-3 text-sm text-slate-400 hover:text-indigo-500 transition-colors text-center"
            >
              인증 코드 재발송
            </button>
          </div>
        )}

        {/* Step 3: 비밀번호 설정 */}
        {step === 'password' && (
          <div>
            <button onClick={goBack} className="flex items-center gap-1 text-sm text-slate-400 hover:text-indigo-500 transition mb-4">
              <ArrowLeft size={16} /> 뒤로
            </button>

            <div className="flex justify-center mb-4">
              <div className={`w-14 h-14 rounded-2xl flex items-center justify-center ${isDark ? 'bg-indigo-500/20' : 'bg-indigo-50'}`}>
                <Lock size={28} className="text-indigo-500" />
              </div>
            </div>

            <h2 className={`text-2xl font-black mb-2 text-center ${isDark ? 'text-white' : 'text-slate-900'}`}>
              비밀번호 설정
            </h2>
            <p className="text-slate-500 text-sm mb-6 text-center">
              로그인에 사용할 비밀번호를 설정해주세요.
            </p>

            <div className={`flex items-center gap-2 py-2 px-4 rounded-xl mb-6 text-sm ${isDark ? 'bg-slate-800 text-indigo-400' : 'bg-indigo-50 text-indigo-600'}`}>
              <Mail size={14} />
              <span className="font-medium">{email}</span>
            </div>

            <div className="space-y-4">
              <div>
                <label className="text-xs font-bold text-slate-400 uppercase mb-1.5 block">비밀번호</label>
                <input
                  ref={passwordInputRef}
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="6자 이상 입력"
                  className={`w-full p-4 rounded-2xl border text-sm font-medium transition ${isDark ? 'bg-slate-800 border-slate-700 text-white placeholder:text-slate-500 focus:border-indigo-500' : 'bg-slate-50 border-slate-200 text-slate-900 placeholder:text-slate-400 focus:border-indigo-500'}`}
                  autoFocus
                />
              </div>

              <div>
                <label className="text-xs font-bold text-slate-400 uppercase mb-1.5 block">비밀번호 확인</label>
                <input
                  type="password"
                  value={passwordConfirm}
                  onChange={(e) => setPasswordConfirm(e.target.value)}
                  onKeyDown={(e) => e.key === 'Enter' && handleCompleteSignup()}
                  placeholder="비밀번호 재입력"
                  className={`w-full p-4 rounded-2xl border text-sm font-medium transition ${isDark ? 'bg-slate-800 border-slate-700 text-white placeholder:text-slate-500 focus:border-indigo-500' : 'bg-slate-50 border-slate-200 text-slate-900 placeholder:text-slate-400 focus:border-indigo-500'}`}
                />
              </div>
            </div>

            {error && <p className="mt-4 text-sm text-rose-500">{error}</p>}

            <button
              onClick={handleCompleteSignup}
              disabled={loading || !password || !passwordConfirm}
              className="w-full mt-6 py-4 rounded-2xl font-bold text-lg bg-indigo-600 text-white hover:bg-indigo-700 transition-all active:scale-95 disabled:opacity-50 flex items-center justify-center gap-2"
            >
              {loading ? <Loader2 size={20} className="animate-spin" /> : <Lock size={20} />}
              <span>{loading ? '가입 중...' : '가입 완료'}</span>
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
