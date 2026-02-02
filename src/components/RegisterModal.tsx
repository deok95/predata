'use client';

import { useState, useRef, createContext, useContext, useCallback } from 'react';
import type { ReactNode } from 'react';
import { X, Wallet, Loader2, Mail, ArrowLeft, ShieldCheck } from 'lucide-react';
import { useConnect, useAccount, useDisconnect } from 'wagmi';
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

const COUNTRIES = [
  { code: 'KR', label: '한국' }, { code: 'US', label: '미국' },
  { code: 'JP', label: '일본' }, { code: 'CN', label: '중국' },
  { code: 'GB', label: '영국' }, { code: 'DE', label: '독일' },
  { code: 'FR', label: '프랑스' }, { code: 'SG', label: '싱가포르' },
];

const JOB_CATEGORIES = ['IT/개발', '금융', '교육', '의료', '법률', '미디어', '공무원', '자영업', '학생', '기타'];

const AGE_GROUPS = [
  { value: '10', label: '10대' }, { value: '20', label: '20대' },
  { value: '30', label: '30대' }, { value: '40', label: '40대' },
  { value: '50', label: '50+' },
];

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

function RegisterModalInner({ onClose }: { onClose: () => void }) {
  const { isDark } = useTheme();
  const { register, loginById } = useAuth();
  const { connectors, connectAsync } = useConnect();
  const { isConnected } = useAccount();
  const { disconnectAsync } = useDisconnect();

  const [step, setStep] = useState<'choose' | 'wallet-connect' | 'email-input' | 'email-verify' | 'form'>('choose');
  const [mode, setMode] = useState<'wallet' | 'email'>('wallet');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [walletAddress, setWalletAddress] = useState('');
  const [email, setEmail] = useState('');
  const [verificationCode, setVerificationCode] = useState('');
  const [demoCode, setDemoCode] = useState('');
  const codeInputRef = useRef<HTMLInputElement>(null);
  const [form, setForm] = useState({ countryCode: 'KR', jobCategory: '', ageGroup: '' });

  const connector = connectors.find(
    (c) => c.name === 'MetaMask' || c.id === 'metaMask' || c.id === 'io.metamask'
  ) || connectors[0];

  const handleWalletConnect = async () => {
    setError('');
    setLoading(true);
    try {
      if (isConnected) await disconnectAsync();
      const result = await connectAsync({ connector });
      const addr = result.accounts[0];
      if (!addr) { setError('지갑 주소를 가져올 수 없습니다.'); return; }
      setWalletAddress(addr);
      setMode('wallet');
      setStep('form');
    } catch (err: any) {
      setError(err?.message?.includes('rejected')
        ? '지갑 연결이 취소되었습니다.'
        : '메타마스크를 확인해주세요.');
    } finally {
      setLoading(false);
    }
  };

  // 이메일: 인증 코드 발송
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
        setDemoCode(result.code || '');
        setVerificationCode('');
        setStep('email-verify');
        setTimeout(() => codeInputRef.current?.focus(), 100);
      } else {
        setError(result.message || '코드 발송에 실패했습니다.');
      }
    } catch {
      setError('서버 오류가 발생했습니다.');
    } finally {
      setLoading(false);
    }
  };

  // 이메일: 인증 코드 검증
  const handleVerifyCode = async () => {
    if (verificationCode.length !== 6) {
      setError('6자리 인증 코드를 입력해주세요.');
      return;
    }
    setError('');
    setLoading(true);
    try {
      const result = await authApi.verifyCode(email.trim(), verificationCode);
      if (result.success && result.verified) {
        if (!result.isNewUser && result.memberId) {
          // 이미 존재하는 회원 → 바로 로그인됨
          await loginById(result.memberId);
          onClose();
          return;
        }
        // 신규 → 프로필 입력 폼
        setMode('email');
        setStep('form');
      } else {
        setError(result.message || '인증에 실패했습니다.');
      }
    } catch (err: any) {
      setError(err?.data?.message || err?.message || '인증에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const handleRegister = async () => {
    if (!form.jobCategory || !form.ageGroup) {
      setError('모든 항목을 입력해주세요.');
      return;
    }
    setError('');
    setLoading(true);
    try {
      const memberEmail = mode === 'wallet'
        ? `${walletAddress.slice(0, 8)}@predata.wallet`
        : email.trim();
      const member = await register({
        email: memberEmail,
        walletAddress: mode === 'wallet' ? walletAddress : undefined,
        countryCode: form.countryCode,
        jobCategory: form.jobCategory,
        ageGroup: form.ageGroup,
      });
      if (member) {
        onClose();
      } else {
        setError(mode === 'wallet'
          ? '회원가입 실패. 이미 등록된 지갑일 수 있습니다.'
          : '회원가입 실패. 이미 등록된 이메일일 수 있습니다.');
      }
    } catch {
      setError('서버 오류가 발생했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const goBack = () => {
    if (step === 'form') {
      setStep('choose');
    } else if (step === 'email-verify') {
      setStep('email-input');
      setVerificationCode('');
      setDemoCode('');
    } else if (step === 'wallet-connect' || step === 'email-input') {
      setStep('choose');
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

        {step === 'choose' && (
          <div className="text-center">
            <h2 className={`text-2xl font-black mb-2 ${isDark ? 'text-white' : 'text-slate-900'}`}>회원가입</h2>
            <p className="text-slate-500 text-sm mb-8">가입 방법을 선택해주세요.</p>
            <div className="space-y-3">
              <button
                onClick={() => { setStep('email-input'); setError(''); }}
                className="w-full py-4 rounded-2xl font-bold text-lg bg-indigo-600 text-white hover:bg-indigo-700 transition-all active:scale-95 flex items-center justify-center gap-3"
              >
                <Mail size={20} />
                <span>이메일로 가입</span>
              </button>
              <button
                onClick={() => { setStep('wallet-connect'); setError(''); }}
                className={`w-full py-4 rounded-2xl font-bold text-lg border-2 transition-all active:scale-95 flex items-center justify-center gap-3 ${isDark ? 'border-slate-700 text-white hover:bg-slate-800' : 'border-slate-200 text-slate-900 hover:bg-slate-50'}`}
              >
                <Wallet size={20} />
                <span>MetaMask로 가입</span>
              </button>
            </div>
            {error && <p className="mt-4 text-sm text-rose-500">{error}</p>}
          </div>
        )}

        {step === 'wallet-connect' && (
          <div className="text-center">
            <button onClick={goBack} className="flex items-center gap-1 text-sm text-slate-400 hover:text-indigo-500 transition mb-4">
              <ArrowLeft size={16} /> 뒤로
            </button>
            <h2 className={`text-2xl font-black mb-2 ${isDark ? 'text-white' : 'text-slate-900'}`}>지갑 연결</h2>
            <p className="text-slate-500 text-sm mb-8">메타마스크 지갑을 연결해주세요.</p>
            <button
              onClick={handleWalletConnect}
              disabled={loading}
              className="w-full py-4 rounded-2xl font-bold text-lg bg-indigo-600 text-white hover:bg-indigo-700 transition-all active:scale-95 disabled:opacity-50 flex items-center justify-center gap-3"
            >
              {loading ? <Loader2 size={20} className="animate-spin" /> : <Wallet size={20} />}
              <span>메타마스크 연결</span>
            </button>
            {error && <p className="mt-4 text-sm text-rose-500">{error}</p>}
          </div>
        )}

        {step === 'email-input' && (
          <div>
            <button onClick={goBack} className="flex items-center gap-1 text-sm text-slate-400 hover:text-indigo-500 transition mb-4">
              <ArrowLeft size={16} /> 뒤로
            </button>
            <h2 className={`text-2xl font-black mb-2 text-center ${isDark ? 'text-white' : 'text-slate-900'}`}>이메일 입력</h2>
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

        {step === 'email-verify' && (
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

            {demoCode && (
              <div className={`mb-4 py-2 px-4 rounded-xl text-xs text-center ${isDark ? 'bg-amber-500/10 text-amber-400' : 'bg-amber-50 text-amber-600'}`}>
                데모 모드 — 인증 코드: <span className="font-mono font-bold">{demoCode}</span>
              </div>
            )}

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

        {step === 'form' && (
          <>
            <button onClick={goBack} className="flex items-center gap-1 text-sm text-slate-400 hover:text-indigo-500 transition mb-4">
              <ArrowLeft size={16} /> 뒤로
            </button>
            <h2 className={`text-2xl font-black mb-2 text-center ${isDark ? 'text-white' : 'text-slate-900'}`}>프로필 설정</h2>
            <div className={`flex items-center justify-center gap-2 py-2 px-4 rounded-xl mb-6 text-sm ${mode === 'wallet' ? 'font-mono' : ''} ${isDark ? 'bg-slate-800 text-indigo-400' : 'bg-indigo-50 text-indigo-600'}`}>
              {mode === 'wallet' ? (
                <><Wallet size={14} />{walletAddress.slice(0, 6)}...{walletAddress.slice(-4)}</>
              ) : (
                <><Mail size={14} />{email}</>
              )}
            </div>

            <div className="space-y-4 text-left">
              <div>
                <label className="text-xs font-bold text-slate-400 uppercase mb-1.5 block">국가</label>
                <select value={form.countryCode} onChange={(e) => setForm(f => ({ ...f, countryCode: e.target.value }))}
                  className={`w-full p-3 rounded-xl border text-sm ${isDark ? 'bg-slate-800 border-slate-700 text-white' : 'bg-slate-50 border-slate-200 text-slate-900'}`}>
                  {COUNTRIES.map(c => <option key={c.code} value={c.code}>{c.label}</option>)}
                </select>
              </div>
              <div>
                <label className="text-xs font-bold text-slate-400 uppercase mb-1.5 block">연령대</label>
                <div className="grid grid-cols-5 gap-2">
                  {AGE_GROUPS.map(a => (
                    <button key={a.value} type="button" onClick={() => setForm(f => ({ ...f, ageGroup: a.value }))}
                      className={`py-2.5 rounded-xl text-xs font-bold transition-all ${form.ageGroup === a.value ? 'bg-indigo-600 text-white' : isDark ? 'bg-slate-800 text-slate-400' : 'bg-slate-100 text-slate-500'}`}>
                      {a.label}
                    </button>
                  ))}
                </div>
              </div>
              <div>
                <label className="text-xs font-bold text-slate-400 uppercase mb-1.5 block">직업군</label>
                <select value={form.jobCategory} onChange={(e) => setForm(f => ({ ...f, jobCategory: e.target.value }))}
                  className={`w-full p-3 rounded-xl border text-sm ${isDark ? 'bg-slate-800 border-slate-700 text-white' : 'bg-slate-50 border-slate-200 text-slate-900'}`}>
                  <option value="">선택해주세요</option>
                  {JOB_CATEGORIES.map(j => <option key={j} value={j}>{j}</option>)}
                </select>
              </div>
            </div>

            {error && <p className="mt-4 text-sm text-rose-500">{error}</p>}

            <button onClick={handleRegister} disabled={loading || !form.jobCategory || !form.ageGroup}
              className="w-full mt-6 py-4 rounded-2xl font-bold text-lg bg-indigo-600 text-white hover:bg-indigo-700 transition-all active:scale-95 disabled:opacity-50 flex items-center justify-center gap-2">
              {loading ? <Loader2 size={20} className="animate-spin" /> : null}
              <span>{loading ? '가입 중...' : '가입 완료'}</span>
            </button>
          </>
        )}
      </div>
    </div>
  );
}
