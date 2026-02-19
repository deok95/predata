'use client';

import { useState, useRef, createContext, useContext, useCallback } from 'react';
import type { ReactNode } from 'react';
import { X, Loader2, Mail, ArrowLeft, ShieldCheck, Lock } from 'lucide-react';
import { useTheme } from '@/hooks/useTheme';
import { useAuth } from '@/hooks/useAuth';
import { authApi } from '@/lib/api';

// --- Context: Modal can be opened from anywhere ---
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
  const [countryCode, setCountryCode] = useState('KR');
  const [gender, setGender] = useState<'MALE' | 'FEMALE' | 'OTHER'>('OTHER');
  const [birthDate, setBirthDate] = useState('');
  const [jobCategory, setJobCategory] = useState('');
  const [ageGroup, setAgeGroup] = useState<number | undefined>(undefined);

  const codeInputRef = useRef<HTMLInputElement>(null);
  const passwordInputRef = useRef<HTMLInputElement>(null);

  // Step 1: Enter email → Send verification code
  const handleSendCode = async () => {
    if (!email.trim() || !email.includes('@')) {
      setError('Please enter a valid email.');
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
        // Special handling for already registered email
        if (result.message?.includes('already registered')) {
          setError('This email is already registered. Please close this window and click "Sign In".');
        } else {
          setError(result.message || 'Failed to send code.');
        }
      }
    } catch (err: any) {
      const errorMsg = err?.data?.message || err?.message || 'Failed to send code.';
      if (errorMsg.includes('already registered')) {
        setError('This email is already registered. Please close this window and click "Sign In".');
      } else {
        setError(errorMsg);
      }
    } finally {
      setLoading(false);
    }
  };

  // Step 2: Verify code
  const handleVerifyCode = async () => {
    if (verificationCode.length !== 6) {
      setError('Please enter the 6-digit verification code.');
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
      setError(err?.data?.message || err?.message || 'Verification failed.');
    } finally {
      setLoading(false);
    }
  };

  // Step 3: Set password → Create account + auto login
  const handleCompleteSignup = async () => {
    if (!birthDate) {
      setError('Please enter your birth date.');
      return;
    }
    if (!password || password.length < 6) {
      setError('Password must be at least 6 characters.');
      return;
    }
    if (password !== passwordConfirm) {
      setError('Passwords do not match.');
      return;
    }
    setError('');
    setLoading(true);
    try {
      const result = await authApi.completeSignup(
        email.trim(),
        verificationCode,
        password,
        passwordConfirm,
        {
          countryCode,
          gender,
          birthDate,
          jobCategory: jobCategory || undefined,
          ageGroup,
        }
      );
      if (result.token && result.memberId) {
        localStorage.setItem('token', result.token);
        localStorage.setItem('memberId', result.memberId.toString());
        await loginById(result.memberId);
        onClose();
      } else {
        setError('Sign up failed.');
      }
    } catch (err: any) {
      setError(err?.data?.message || err?.message || 'Sign up failed.');
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

        {/* Step 1: Enter Email */}
        {step === 'email' && (
          <div>
            <div className="flex justify-center mb-4">
              <div className={`w-14 h-14 rounded-2xl flex items-center justify-center ${isDark ? 'bg-indigo-500/20' : 'bg-indigo-50'}`}>
                <Mail size={28} className="text-indigo-500" />
              </div>
            </div>

            <h2 className={`text-2xl font-black mb-2 text-center ${isDark ? 'text-white' : 'text-slate-900'}`}>
              Enter Email
            </h2>
            <p className="text-slate-500 text-sm mb-6 text-center">
              Enter your email to receive a verification code.
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
              <span>{loading ? 'Sending...' : 'Get Verification Code'}</span>
            </button>
          </div>
        )}

        {/* Step 2: Enter Verification Code */}
        {step === 'verify' && (
          <div>
            <button onClick={goBack} className="flex items-center gap-1 text-sm text-slate-400 hover:text-indigo-500 transition mb-4">
              <ArrowLeft size={16} /> Back
            </button>

            <div className="flex justify-center mb-4">
              <div className={`w-14 h-14 rounded-2xl flex items-center justify-center ${isDark ? 'bg-indigo-500/20' : 'bg-indigo-50'}`}>
                <ShieldCheck size={28} className="text-indigo-500" />
              </div>
            </div>

            <h2 className={`text-2xl font-black mb-2 text-center ${isDark ? 'text-white' : 'text-slate-900'}`}>
              Enter Verification Code
            </h2>
            <p className="text-slate-500 text-sm mb-6 text-center">
              Enter the 6-digit code sent to<br />
              <span className="text-indigo-500 font-medium">{email}</span>
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
              <span>{loading ? 'Verifying...' : 'Verify Code'}</span>
            </button>

            <button
              onClick={handleSendCode}
              disabled={loading}
              className="w-full mt-3 text-sm text-slate-400 hover:text-indigo-500 transition-colors text-center"
            >
              Resend Code
            </button>
          </div>
        )}

        {/* Step 3: Set Password */}
        {step === 'password' && (
          <div>
            <button onClick={goBack} className="flex items-center gap-1 text-sm text-slate-400 hover:text-indigo-500 transition mb-4">
              <ArrowLeft size={16} /> Back
            </button>

            <div className="flex justify-center mb-4">
              <div className={`w-14 h-14 rounded-2xl flex items-center justify-center ${isDark ? 'bg-indigo-500/20' : 'bg-indigo-50'}`}>
                <Lock size={28} className="text-indigo-500" />
              </div>
            </div>

            <h2 className={`text-2xl font-black mb-2 text-center ${isDark ? 'text-white' : 'text-slate-900'}`}>
              Set Password
            </h2>
            <p className="text-slate-500 text-sm mb-6 text-center">
              Set a password for your account.
            </p>

            <div className={`flex items-center gap-2 py-2 px-4 rounded-xl mb-6 text-sm ${isDark ? 'bg-slate-800 text-indigo-400' : 'bg-indigo-50 text-indigo-600'}`}>
              <Mail size={14} />
              <span className="font-medium">{email}</span>
            </div>

            <div className="space-y-4">
              <div>
                <label className="text-xs font-bold text-slate-400 uppercase mb-1.5 block">Country</label>
                <select
                  value={countryCode}
                  onChange={(e) => setCountryCode(e.target.value)}
                  className={`w-full p-4 rounded-2xl border text-sm font-medium transition ${isDark ? 'bg-slate-800 border-slate-700 text-white focus:border-indigo-500' : 'bg-slate-50 border-slate-200 text-slate-900 focus:border-indigo-500'}`}
                >
                  <option value="KR">South Korea</option>
                  <option value="US">United States</option>
                  <option value="JP">Japan</option>
                  <option value="CN">China</option>
                  <option value="OT">Other</option>
                </select>
              </div>

              <div>
                <label className="text-xs font-bold text-slate-400 uppercase mb-1.5 block">Gender</label>
                <div className="grid grid-cols-3 gap-2">
                  {([
                    ['MALE', 'Male'],
                    ['FEMALE', 'Female'],
                    ['OTHER', 'Other'],
                  ] as const).map(([value, label]) => (
                    <button
                      key={value}
                      type="button"
                      onClick={() => setGender(value)}
                      className={`py-2.5 rounded-xl text-sm font-bold border transition ${
                        gender === value
                          ? 'bg-indigo-600 text-white border-indigo-600'
                          : isDark
                            ? 'bg-slate-800 border-slate-700 text-slate-300'
                            : 'bg-slate-50 border-slate-200 text-slate-600'
                      }`}
                    >
                      {label}
                    </button>
                  ))}
                </div>
              </div>

              <div>
                <label className="text-xs font-bold text-slate-400 uppercase mb-1.5 block">Birth Date</label>
                <input
                  type="date"
                  value={birthDate}
                  onChange={(e) => setBirthDate(e.target.value)}
                  className={`w-full p-4 rounded-2xl border text-sm font-medium transition ${isDark ? 'bg-slate-800 border-slate-700 text-white focus:border-indigo-500' : 'bg-slate-50 border-slate-200 text-slate-900 focus:border-indigo-500'}`}
                />
              </div>

              <div>
                <label className="text-xs font-bold text-slate-400 uppercase mb-1.5 block">Occupation (Optional)</label>
                <select
                  value={jobCategory}
                  onChange={(e) => setJobCategory(e.target.value)}
                  className={`w-full p-4 rounded-2xl border text-sm font-medium transition ${isDark ? 'bg-slate-800 border-slate-700 text-white focus:border-indigo-500' : 'bg-slate-50 border-slate-200 text-slate-900 focus:border-indigo-500'}`}
                >
                  <option value="">Not Selected</option>
                  <option value="Student">Student</option>
                  <option value="Office Worker">Office Worker</option>
                  <option value="Self-employed">Self-employed</option>
                  <option value="Freelancer">Freelancer</option>
                  <option value="Other">Other</option>
                </select>
              </div>

              <div>
                <label className="text-xs font-bold text-slate-400 uppercase mb-1.5 block">Age Group (Optional)</label>
                <select
                  value={ageGroup ?? ''}
                  onChange={(e) => setAgeGroup(e.target.value ? parseInt(e.target.value, 10) : undefined)}
                  className={`w-full p-4 rounded-2xl border text-sm font-medium transition ${isDark ? 'bg-slate-800 border-slate-700 text-white focus:border-indigo-500' : 'bg-slate-50 border-slate-200 text-slate-900 focus:border-indigo-500'}`}
                >
                  <option value="">Not Selected</option>
                  <option value="20">20s</option>
                  <option value="30">30s</option>
                  <option value="40">40s</option>
                  <option value="50">50+</option>
                </select>
              </div>

              <div>
                <label className="text-xs font-bold text-slate-400 uppercase mb-1.5 block">Password</label>
                <input
                  ref={passwordInputRef}
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="At least 6 characters"
                  className={`w-full p-4 rounded-2xl border text-sm font-medium transition ${isDark ? 'bg-slate-800 border-slate-700 text-white placeholder:text-slate-500 focus:border-indigo-500' : 'bg-slate-50 border-slate-200 text-slate-900 placeholder:text-slate-400 focus:border-indigo-500'}`}
                  autoFocus
                />
              </div>

              <div>
                <label className="text-xs font-bold text-slate-400 uppercase mb-1.5 block">Confirm Password</label>
                <input
                  type="password"
                  value={passwordConfirm}
                  onChange={(e) => setPasswordConfirm(e.target.value)}
                  onKeyDown={(e) => e.key === 'Enter' && handleCompleteSignup()}
                  placeholder="Re-enter password"
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
              <span>{loading ? 'Signing up...' : 'Complete Sign Up'}</span>
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
