'use client';

import { useEffect, useState, Suspense } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { Loader2, Globe, Briefcase, Users } from 'lucide-react';
import { authApi } from '@/lib/api';

const COUNTRIES = [
  { code: 'KR', name: 'South Korea' },
  { code: 'US', name: 'United States' },
  { code: 'JP', name: 'Japan' },
  { code: 'CN', name: 'China' },
  { code: 'GB', name: 'United Kingdom' },
  { code: 'OT', name: 'Other' },
];

const JOB_CATEGORIES = [
  'Student',
  'Employee',
  'Freelancer',
  'Self-Employed',
  'Unemployed',
  'Other',
];

const AGE_GROUPS = [
  { value: 10, label: '10s' },
  { value: 20, label: '20s' },
  { value: 30, label: '30s' },
  { value: 40, label: '40s' },
  { value: 50, label: '50s+' },
];

const GENDERS = [
  { value: 'MALE', label: 'Male' },
  { value: 'FEMALE', label: 'Female' },
  { value: 'OTHER', label: 'Other' },
] as const;

function GoogleCompleteContent() {
  const router = useRouter();
  const searchParams = useSearchParams();

  const [googleId, setGoogleId] = useState<string | null>(null);
  const [email, setEmail] = useState<string | null>(null);
  const [name, setName] = useState<string | null>(null);

  const [countryCode, setCountryCode] = useState('KR');
  const [gender, setGender] = useState<'MALE' | 'FEMALE' | 'OTHER'>('OTHER');
  const [birthDate, setBirthDate] = useState('');
  const [jobCategory, setJobCategory] = useState('');
  const [ageGroup, setAgeGroup] = useState(20);

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    // Extract parameters from URL
    const googleIdParam = searchParams.get('googleId');
    const emailParam = searchParams.get('email');
    const nameParam = searchParams.get('name');

    if (!googleIdParam || !emailParam) {
      setError('Invalid access.');
      return;
    }

    setGoogleId(googleIdParam);
    setEmail(decodeURIComponent(emailParam));
    setName(nameParam ? decodeURIComponent(nameParam) : null);
  }, [searchParams]);

  const handleSubmit = async () => {
    if (!googleId || !email) {
      setError('Required information is missing.');
      return;
    }

    if (!countryCode) {
      setError('Please select a country.');
      return;
    }

    if (!birthDate) {
      setError('Please enter your birth date.');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const result = await authApi.completeGoogleRegistration({
        googleId,
        email,
        countryCode,
        gender,
        birthDate,
        jobCategory: jobCategory || undefined,
        ageGroup,
      });

      if (result.success && result.token && result.memberId) {
        // Save token and memberId to localStorage
        localStorage.setItem('token', result.token);
        localStorage.setItem('memberId', result.memberId.toString());

        // Redirect to callback page — remove complete URL from history with replace
        router.replace(`/auth/google/callback?token=${result.token}&memberId=${result.memberId}`);
      } else {
        setError(result.message || 'Registration failed.');
      }
    } catch (err: any) {
      setError(err?.data?.message || err?.message || 'An error occurred during registration.');
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
            Error
          </h1>
          <p className="text-slate-600 dark:text-slate-400 mb-8">
            {error}
          </p>
          <button
            onClick={() => router.push('/')}
            className="w-full py-3 rounded-xl bg-indigo-600 text-white font-bold hover:bg-indigo-700 transition"
          >
            Return to Home
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
            Additional Information
          </h1>
          <p className="text-slate-600 dark:text-slate-400 text-sm">
            {name && `${name}, `}Please enter additional information to complete your registration.
          </p>
        </div>

        <div className="space-y-4">
          {/* Country selection */}
          <div>
            <label className="flex items-center gap-2 text-sm font-bold text-slate-900 dark:text-white mb-2">
              <Globe size={16} />
              Country <span className="text-rose-500">*</span>
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

          <div>
            <label className="flex items-center gap-2 text-sm font-bold text-slate-900 dark:text-white mb-2">
              Gender <span className="text-rose-500">*</span>
            </label>
            <div className="grid grid-cols-3 gap-2">
              {GENDERS.map((item) => (
                <button
                  key={item.value}
                  type="button"
                  onClick={() => setGender(item.value)}
                  className={`py-2 rounded-xl text-sm font-bold border transition ${
                    gender === item.value
                      ? 'bg-indigo-600 text-white border-indigo-600'
                      : 'border-slate-200 dark:border-slate-700 text-slate-700 dark:text-slate-300'
                  }`}
                >
                  {item.label}
                </button>
              ))}
            </div>
          </div>

          <div>
            <label className="flex items-center gap-2 text-sm font-bold text-slate-900 dark:text-white mb-2">
              Birth Date <span className="text-rose-500">*</span>
            </label>
            <input
              type="date"
              value={birthDate}
              onChange={(e) => setBirthDate(e.target.value)}
              className="w-full p-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-white font-medium focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>

          {/* Job selection */}
          <div>
            <label className="flex items-center gap-2 text-sm font-bold text-slate-900 dark:text-white mb-2">
              <Briefcase size={16} />
              Occupation (Optional)
            </label>
            <select
              value={jobCategory}
              onChange={(e) => setJobCategory(e.target.value)}
              className="w-full p-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-white font-medium focus:outline-none focus:ring-2 focus:ring-indigo-500"
            >
              <option value="">Not Selected</option>
              {JOB_CATEGORIES.map((job) => (
                <option key={job} value={job}>
                  {job}
                </option>
              ))}
            </select>
          </div>

          {/* Age group selection */}
          <div>
            <label className="flex items-center gap-2 text-sm font-bold text-slate-900 dark:text-white mb-2">
              <Users size={16} />
              Age Group (Optional)
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
          disabled={loading || !countryCode || !birthDate}
          className="w-full mt-6 py-4 rounded-xl bg-indigo-600 text-white font-bold hover:bg-indigo-700 transition-all active:scale-95 disabled:opacity-50 flex items-center justify-center gap-2"
        >
          {loading ? (
            <>
              <Loader2 size={20} className="animate-spin" />
              <span>Processing...</span>
            </>
          ) : (
            <span>Complete Registration</span>
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
