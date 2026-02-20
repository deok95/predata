'use client';

import { useState } from 'react';
import { MousePointer2, Sun, Moon, ArrowLeft, Loader2, Mail, Lock, ChevronDown } from 'lucide-react';
import Image from 'next/image';
import PredataLogo from '@/components/ui/PredataLogo';
import { useTheme } from '@/hooks/useTheme';
import { useAuth } from '@/hooks/useAuth';
import { useRegisterModal } from '@/components/RegisterModal';
import { authApi, BACKEND_URL } from '@/lib/api';

type Step = 'main' | 'email-login';

export default function LoginModal() {
  const { isDark, toggleTheme } = useTheme();
  const { loginAsGuest, loginById, loginWithGoogle } = useAuth();
  const { open: openRegisterModal } = useRegisterModal();

  const [step, setStep] = useState<Step>('main');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showEmailAccordion, setShowEmailAccordion] = useState(false);

  // Google login handler (OAuth2 Redirect)
  const handleGoogleLogin = () => {
    // Redirect to Spring Security OAuth2 authorization endpoint
    window.location.href = `${BACKEND_URL}/oauth2/authorization/google`;
  };

  // Email + password login
  const handleEmailLogin = async () => {
    if (!email.trim() || !email.includes('@')) {
      setError('Please enter a valid email.');
      return;
    }
    if (!password || password.length < 6) {
      setError('Please enter your password.');
      return;
    }
    setError('');
    setLoading(true);
    try {
      const result = await authApi.login(email.trim(), password);
      if (result.success && result.token && result.memberId) {
        localStorage.setItem('token', result.token);
        localStorage.setItem('memberId', result.memberId.toString());
        await loginById(result.memberId);
      } else {
        setError(result.message || 'Login failed.');
      }
    } catch (err: unknown) {
      const error = err as { data?: { message?: string }; message?: string };
      setError(error?.data?.message || error?.message || 'Login failed.');
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
      <div className="mb-12 scale-125 w-full flex justify-center">
        <PredataLogo />
      </div>

      <div className={`max-w-md w-full p-10 rounded-[2.5rem] border text-center shadow-2xl transition-all ${isDark ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-100'}`}>

        {step === 'main' && (
          <>
            <h1 className={`text-3xl font-black mb-4 ${isDark ? 'text-white' : 'text-slate-900'}`}>
              Welcome to Predata
            </h1>
            <p className="text-slate-500 mb-8 text-sm leading-relaxed">
              Sign in to participate in predictions
            </p>

            <div className="space-y-4">
              {/* Google login button */}
              <button
                onClick={handleGoogleLogin}
                className={`w-full py-3.5 px-4 rounded-xl font-medium border-2 transition-all flex items-center justify-center gap-3 ${
                  isDark
                    ? 'bg-white text-slate-900 border-slate-300 hover:bg-slate-50'
                    : 'bg-white text-slate-900 border-slate-300 hover:bg-slate-50'
                }`}
              >
                <svg width="20" height="20" viewBox="0 0 48 48">
                  <path fill="#EA4335" d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z"/>
                  <path fill="#4285F4" d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z"/>
                  <path fill="#FBBC05" d="M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z"/>
                  <path fill="#34A853" d="M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z"/>
                  <path fill="none" d="M0 0h48v48H0z"/>
                </svg>
                <span>Continue with Google</span>
              </button>

              {/* OR divider */}
              <div className="flex items-center gap-3 my-6">
                <div className={`flex-1 h-px ${isDark ? 'bg-slate-700' : 'bg-slate-200'}`} />
                <span className="text-slate-400 text-sm">OR</span>
                <div className={`flex-1 h-px ${isDark ? 'bg-slate-700' : 'bg-slate-200'}`} />
              </div>

              {/* Email login accordion */}
              <div className={`border-2 rounded-2xl overflow-hidden ${isDark ? 'border-slate-700' : 'border-slate-200'}`}>
                <button
                  onClick={() => setShowEmailAccordion(!showEmailAccordion)}
                  className={`w-full p-4 flex items-center justify-between transition ${
                    isDark ? 'bg-slate-800 hover:bg-slate-750' : 'bg-slate-50 hover:bg-slate-100'
                  }`}
                >
                  <div className="flex items-center gap-2">
                    <Mail size={18} className={isDark ? 'text-slate-400' : 'text-slate-600'} />
                    <span className={`font-medium ${isDark ? 'text-slate-200' : 'text-slate-900'}`}>Login with Email</span>
                  </div>
                  <ChevronDown
                    size={20}
                    className={`transition-transform ${showEmailAccordion ? 'rotate-180' : ''} ${isDark ? 'text-slate-400' : 'text-slate-600'}`}
                  />
                </button>

                {showEmailAccordion && (
                  <div className={`p-4 space-y-3 ${isDark ? 'bg-slate-900' : 'bg-white'}`}>
                    <button
                      onClick={() => { setStep('email-login'); setError(''); }}
                      className={`w-full py-3 rounded-xl font-bold transition ${
                        isDark
                          ? 'bg-slate-700 text-white hover:bg-slate-600'
                          : 'bg-slate-700 text-white hover:bg-slate-800'
                      }`}
                    >
                      Sign In
                    </button>
                    <button
                      onClick={openRegisterModal}
                      className={`w-full py-3 rounded-xl font-bold border-2 transition ${
                        isDark
                          ? 'border-slate-700 text-slate-200 hover:bg-slate-800'
                          : 'border-slate-200 hover:bg-slate-50'
                      }`}
                    >
                      Sign Up
                    </button>
                  </div>
                )}
              </div>

              {/* Guest login */}
              <button
                onClick={loginAsGuest}
                className="w-full py-3 rounded-xl font-medium text-slate-500 hover:text-indigo-500 hover:bg-slate-50 dark:hover:bg-slate-800 transition flex items-center justify-center gap-2"
              >
                <MousePointer2 size={14} />
                <span>Browse as Guest</span>
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
              <ArrowLeft size={16} /> Back
            </button>

            <div className="flex justify-center mb-4">
              <div className={`w-14 h-14 rounded-2xl flex items-center justify-center ${isDark ? 'bg-indigo-500/20' : 'bg-indigo-50'}`}>
                <Lock size={28} className="text-indigo-500" />
              </div>
            </div>

            <h2 className={`text-2xl font-black mb-2 ${isDark ? 'text-white' : 'text-slate-900'}`}>
              Sign In
            </h2>
            <p className="text-slate-500 text-sm mb-6">
              Enter your email and password.
            </p>

            <div className="space-y-4 mb-4">
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="Email"
                className={`w-full p-4 rounded-2xl border text-sm font-medium transition ${isDark ? 'bg-slate-800 border-slate-700 text-white placeholder:text-slate-500 focus:border-indigo-500' : 'bg-slate-50 border-slate-200 text-slate-900 placeholder:text-slate-400 focus:border-indigo-500'}`}
                autoFocus
              />
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && handleEmailLogin()}
                placeholder="Password"
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
              <span>{loading ? 'Signing in...' : 'Sign In'}</span>
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
