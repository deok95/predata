'use client';

import { useState, useRef } from 'react';
import { Wallet, MousePointer2, Sun, Moon, ArrowLeft, Loader2, Mail, ShieldCheck } from 'lucide-react';
import { useConnect, useAccount, useDisconnect } from 'wagmi';
import PredataLogo from '@/components/ui/PredataLogo';
import { useTheme } from '@/hooks/useTheme';
import { useAuth } from '@/hooks/useAuth';
import { authApi } from '@/lib/api';

type Step = 'main' | 'register-form' | 'email-login' | 'email-verify' | 'email-register-form';

const COUNTRIES = [
  { code: 'KR', label: '한국' },
  { code: 'US', label: '미국' },
  { code: 'JP', label: '일본' },
  { code: 'CN', label: '중국' },
  { code: 'GB', label: '영국' },
  { code: 'DE', label: '독일' },
  { code: 'FR', label: '프랑스' },
  { code: 'SG', label: '싱가포르' },
];

const JOB_CATEGORIES = [
  'IT/개발', '금융', '교육', '의료', '법률', '미디어', '공무원', '자영업', '학생', '기타',
];

const AGE_GROUPS = [
  { value: '10', label: '10대' },
  { value: '20', label: '20대' },
  { value: '30', label: '30대' },
  { value: '40', label: '40대' },
  { value: '50', label: '50+' },
];

export default function LoginModal() {
  const { isDark, toggleTheme } = useTheme();
  const { loginAsGuest, loginWithWallet, loginById, register } = useAuth();
  const { connectors, connectAsync } = useConnect();
  const { isConnected } = useAccount();
  const { disconnectAsync } = useDisconnect();

  const [step, setStep] = useState<Step>('main');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [walletAddress, setWalletAddress] = useState('');
  const [email, setEmail] = useState('');
  const [verificationCode, setVerificationCode] = useState('');
  const [demoCode, setDemoCode] = useState('');
  const codeInputRef = useRef<HTMLInputElement>(null);

  const [form, setForm] = useState({
    countryCode: 'KR',
    jobCategory: '',
    ageGroup: '',
  });

  const metaMaskConnector = connectors.find(
    (c) => c.name === 'MetaMask' || c.id === 'metaMask' || c.id === 'io.metamask'
  ) || connectors[0];

  const connectWallet = async (): Promise<string | null> => {
    if (isConnected) {
      await disconnectAsync();
    }
    const result = await connectAsync({ connector: metaMaskConnector });
    return result.accounts[0] || null;
  };

  // 로그인: 메타마스크 연결 → 기존 회원 조회
  const handleLogin = async () => {
    setError('');
    setLoading(true);
    try {
      const addr = await connectWallet();
      if (!addr) { setError('지갑 주소를 가져올 수 없습니다.'); return; }

      const member = await loginWithWallet(addr);
      if (!member) {
        setError('등록되지 않은 지갑입니다. 회원가입을 먼저 해주세요.');
        await disconnectAsync();
      }
    } catch (err: any) {
      setError(err?.message?.includes('rejected')
        ? '지갑 연결이 취소되었습니다.'
        : '지갑 연결에 실패했습니다. 메타마스크를 확인해주세요.');
    } finally {
      setLoading(false);
    }
  };

  // 회원가입 1단계: 메타마스크 연결 → 정보 입력 폼으로
  const handleSignUpConnect = async () => {
    setError('');
    setLoading(true);
    try {
      const addr = await connectWallet();
      if (!addr) { setError('지갑 주소를 가져올 수 없습니다.'); return; }
      setWalletAddress(addr);
      setStep('register-form');
    } catch (err: any) {
      setError(err?.message?.includes('rejected')
        ? '지갑 연결이 취소되었습니다.'
        : '메타마스크가 설치되어 있는지 확인해주세요.');
    } finally {
      setLoading(false);
    }
  };

  // 회원가입 2단계: 폼 제출 (MetaMask)
  const handleRegister = async () => {
    if (!form.jobCategory || !form.ageGroup) {
      setError('모든 항목을 입력해주세요.');
      return;
    }
    setError('');
    setLoading(true);
    try {
      const memberEmail = `${walletAddress.slice(0, 8)}@predata.wallet`;
      const member = await register({
        email: memberEmail,
        walletAddress,
        countryCode: form.countryCode,
        jobCategory: form.jobCategory,
        ageGroup: form.ageGroup,
      });
      if (!member) {
        setError('회원가입에 실패했습니다. 이미 등록된 지갑일 수 있습니다.');
      }
    } catch {
      setError('서버 오류가 발생했습니다.');
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
          // 기존 회원 → 로그인
          await loginById(result.memberId);
          return;
        }
        // 신규 → 회원가입 폼
        setStep('email-register-form');
      } else {
        setError(result.message || '인증에 실패했습니다.');
      }
    } catch (err: any) {
      setError(err?.data?.message || err?.message || '인증에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  // 이메일 회원가입: 인증 완료 후 프로필
  const handleEmailRegister = async () => {
    if (!form.jobCategory || !form.ageGroup) {
      setError('모든 항목을 입력해주세요.');
      return;
    }
    setError('');
    setLoading(true);
    try {
      const member = await register({
        email: email.trim(),
        countryCode: form.countryCode,
        jobCategory: form.jobCategory,
        ageGroup: form.ageGroup,
      });
      if (!member) {
        setError('회원가입에 실패했습니다. 이미 등록된 이메일일 수 있습니다.');
      }
    } catch {
      setError('서버 오류가 발생했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const goBack = async () => {
    if (step === 'email-verify') {
      setStep('email-login');
      setVerificationCode('');
      setDemoCode('');
    } else {
      setStep('main');
      setEmail('');
      setWalletAddress('');
    }
    setError('');
    try { await disconnectAsync(); } catch { /* ignore */ }
  };

  // 프로필 입력 폼 공통 렌더
  const renderProfileForm = (onSubmit: () => void, backStep: Step | (() => void)) => (
    <>
      <button onClick={typeof backStep === 'function' ? backStep : () => { setStep(backStep); setError(''); }} className="flex items-center gap-1 text-sm text-slate-400 hover:text-indigo-500 transition mb-6">
        <ArrowLeft size={16} /> 뒤로
      </button>

      <h2 className={`text-2xl font-black mb-2 ${isDark ? 'text-white' : 'text-slate-900'}`}>
        {walletAddress ? '프로필 설정' : '회원가입'}
      </h2>
      <p className="text-slate-500 text-sm mb-4">
        {walletAddress ? '지갑 연결 완료! 정보를 입력해주세요.' : '처음이시군요! 정보를 입력해주세요.'}
      </p>

      {walletAddress ? (
        <div className={`flex items-center justify-center gap-2 py-3 px-4 rounded-xl mb-6 text-sm font-mono ${isDark ? 'bg-slate-800 text-indigo-400' : 'bg-indigo-50 text-indigo-600'}`}>
          <Wallet size={14} />
          {walletAddress.slice(0, 6)}...{walletAddress.slice(-4)}
        </div>
      ) : (
        <div className={`flex items-center justify-center gap-2 py-2 px-4 rounded-xl mb-6 text-sm ${isDark ? 'bg-slate-800 text-indigo-400' : 'bg-indigo-50 text-indigo-600'}`}>
          <Mail size={14} />
          {email}
        </div>
      )}

      <div className="space-y-4 text-left">
        <div>
          <label className="text-xs font-bold text-slate-400 uppercase mb-1.5 block">국가</label>
          <select
            value={form.countryCode}
            onChange={(e) => setForm(f => ({ ...f, countryCode: e.target.value }))}
            className={`w-full p-3.5 rounded-xl border text-sm font-medium transition ${isDark ? 'bg-slate-800 border-slate-700 text-white' : 'bg-slate-50 border-slate-200 text-slate-900'}`}
          >
            {COUNTRIES.map(c => (
              <option key={c.code} value={c.code}>{c.label}</option>
            ))}
          </select>
        </div>

        <div>
          <label className="text-xs font-bold text-slate-400 uppercase mb-1.5 block">연령대</label>
          <div className="grid grid-cols-5 gap-2">
            {AGE_GROUPS.map(a => (
              <button
                key={a.value}
                type="button"
                onClick={() => setForm(f => ({ ...f, ageGroup: a.value }))}
                className={`py-3 rounded-xl text-xs font-bold transition-all ${
                  form.ageGroup === a.value
                    ? 'bg-indigo-600 text-white'
                    : isDark ? 'bg-slate-800 text-slate-400 hover:bg-slate-700' : 'bg-slate-100 text-slate-500 hover:bg-slate-200'
                }`}
              >
                {a.label}
              </button>
            ))}
          </div>
        </div>

        <div>
          <label className="text-xs font-bold text-slate-400 uppercase mb-1.5 block">직업군</label>
          <select
            value={form.jobCategory}
            onChange={(e) => setForm(f => ({ ...f, jobCategory: e.target.value }))}
            className={`w-full p-3.5 rounded-xl border text-sm font-medium transition ${isDark ? 'bg-slate-800 border-slate-700 text-white' : 'bg-slate-50 border-slate-200 text-slate-900'}`}
          >
            <option value="">선택해주세요</option>
            {JOB_CATEGORIES.map(j => (
              <option key={j} value={j}>{j}</option>
            ))}
          </select>
        </div>
      </div>

      {error && (
        <p className="mt-4 text-sm text-rose-500 font-medium">{error}</p>
      )}

      <button
        onClick={onSubmit}
        disabled={loading || !form.jobCategory || !form.ageGroup}
        className="w-full mt-6 py-4 rounded-2xl font-bold text-lg bg-indigo-600 text-white hover:bg-indigo-700 transition-all active:scale-95 disabled:opacity-50 flex items-center justify-center space-x-2"
      >
        {loading ? <Loader2 size={20} className="animate-spin" /> : null}
        <span>{loading ? '가입 중...' : '가입 완료'}</span>
      </button>
    </>
  );

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
                onClick={() => { setStep('email-login'); setError(''); }}
                disabled={loading}
                className="w-full py-4 rounded-2xl font-bold text-lg flex items-center justify-center space-x-3 bg-indigo-600 text-white hover:bg-indigo-700 transition-all active:scale-95 disabled:opacity-50"
              >
                <Mail size={20} />
                <span>이메일로 시작하기</span>
              </button>

              <button
                onClick={handleLogin}
                disabled={loading}
                className={`w-full py-4 rounded-2xl font-bold text-lg flex items-center justify-center space-x-3 border-2 transition-all active:scale-95 disabled:opacity-50 ${isDark ? 'border-slate-700 text-white hover:bg-slate-800' : 'border-slate-200 text-slate-900 hover:bg-slate-50'}`}
              >
                {loading ? <Loader2 size={20} className="animate-spin" /> : <Wallet size={20} />}
                <span>MetaMask 로그인</span>
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

        {step === 'register-form' && renderProfileForm(handleRegister, 'main')}

        {step === 'email-login' && (
          <>
            <button onClick={goBack} className="flex items-center gap-1 text-sm text-slate-400 hover:text-indigo-500 transition mb-6">
              <ArrowLeft size={16} /> 뒤로
            </button>

            <h2 className={`text-2xl font-black mb-2 ${isDark ? 'text-white' : 'text-slate-900'}`}>
              이메일 로그인
            </h2>
            <p className="text-slate-500 text-sm mb-6">
              이메일을 입력하면 인증 코드가 발송됩니다.<br />신규 회원이면 자동으로 가입 화면으로 이동합니다.
            </p>

            <div className="mb-4">
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && handleSendCode()}
                placeholder="example@email.com"
                className={`w-full p-4 rounded-2xl border text-sm font-medium transition ${isDark ? 'bg-slate-800 border-slate-700 text-white placeholder:text-slate-500 focus:border-indigo-500' : 'bg-slate-50 border-slate-200 text-slate-900 placeholder:text-slate-400 focus:border-indigo-500'}`}
                autoFocus
              />
            </div>

            {error && (
              <p className="mb-4 text-sm text-rose-500 font-medium">{error}</p>
            )}

            <button
              onClick={handleSendCode}
              disabled={loading || !email.trim()}
              className="w-full py-4 rounded-2xl font-bold text-lg bg-indigo-600 text-white hover:bg-indigo-700 transition-all active:scale-95 disabled:opacity-50 flex items-center justify-center space-x-2"
            >
              {loading ? <Loader2 size={20} className="animate-spin" /> : <Mail size={20} />}
              <span>{loading ? '발송 중...' : '인증 코드 받기'}</span>
            </button>
          </>
        )}

        {step === 'email-verify' && (
          <>
            <button onClick={goBack} className="flex items-center gap-1 text-sm text-slate-400 hover:text-indigo-500 transition mb-6">
              <ArrowLeft size={16} /> 뒤로
            </button>

            <div className="flex justify-center mb-4">
              <div className={`w-14 h-14 rounded-2xl flex items-center justify-center ${isDark ? 'bg-indigo-500/20' : 'bg-indigo-50'}`}>
                <ShieldCheck size={28} className="text-indigo-500" />
              </div>
            </div>

            <h2 className={`text-2xl font-black mb-2 ${isDark ? 'text-white' : 'text-slate-900'}`}>
              인증 코드 입력
            </h2>
            <p className="text-slate-500 text-sm mb-6">
              <span className="text-indigo-500 font-medium">{email}</span>으로<br />
              발송된 6자리 코드를 입력해주세요.
            </p>

            {demoCode && (
              <div className={`mb-4 py-2 px-4 rounded-xl text-xs ${isDark ? 'bg-amber-500/10 text-amber-400' : 'bg-amber-50 text-amber-600'}`}>
                데모 모드 — 인증 코드: <span className="font-mono font-bold">{demoCode}</span>
              </div>
            )}

            <div className="mb-4">
              <input
                ref={codeInputRef}
                type="text"
                inputMode="numeric"
                maxLength={6}
                value={verificationCode}
                onChange={(e) => setVerificationCode(e.target.value.replace(/\D/g, '').slice(0, 6))}
                onKeyDown={(e) => e.key === 'Enter' && verificationCode.length === 6 && handleVerifyCode()}
                placeholder="000000"
                className={`w-full p-4 rounded-2xl border text-center text-2xl font-mono font-bold tracking-[0.5em] transition ${isDark ? 'bg-slate-800 border-slate-700 text-white placeholder:text-slate-600 focus:border-indigo-500' : 'bg-slate-50 border-slate-200 text-slate-900 placeholder:text-slate-300 focus:border-indigo-500'}`}
                autoFocus
              />
            </div>

            {error && (
              <p className="mb-4 text-sm text-rose-500 font-medium">{error}</p>
            )}

            <button
              onClick={handleVerifyCode}
              disabled={loading || verificationCode.length !== 6}
              className="w-full py-4 rounded-2xl font-bold text-lg bg-indigo-600 text-white hover:bg-indigo-700 transition-all active:scale-95 disabled:opacity-50 flex items-center justify-center space-x-2"
            >
              {loading ? <Loader2 size={20} className="animate-spin" /> : <ShieldCheck size={20} />}
              <span>{loading ? '확인 중...' : '인증 확인'}</span>
            </button>

            <button
              onClick={handleSendCode}
              disabled={loading}
              className="w-full mt-3 text-sm text-slate-400 hover:text-indigo-500 transition-colors"
            >
              인증 코드 재발송
            </button>
          </>
        )}

        {step === 'email-register-form' && renderProfileForm(handleEmailRegister, () => { setStep('email-login'); setError(''); })}

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
