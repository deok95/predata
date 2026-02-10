'use client';

import { useEffect } from 'react';
import { RefreshCw, Home } from 'lucide-react';
import Link from 'next/link';
import { useI18n } from '@/lib/i18n';

export default function Error({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  const { t } = useI18n();

  useEffect(() => {
    if (process.env.NODE_ENV === 'development') {
      console.error('[App Error]', error);
    }
  }, [error]);

  return (
    <div className="min-h-screen flex items-center justify-center bg-slate-950 text-white p-4">
      <div className="text-center max-w-md">
        <div className="w-20 h-20 bg-rose-500/10 rounded-full flex items-center justify-center mx-auto mb-6">
          <span className="text-4xl">!</span>
        </div>
        <h1 className="text-2xl font-black mb-3">{t('error.title')}</h1>
        <p className="text-slate-400 mb-2">
          {t('error.desc')}
        </p>
        {error.digest && (
          <p className="text-xs text-slate-600 mb-6 font-mono">Error ID: {error.digest}</p>
        )}
        <div className="flex gap-3 justify-center">
          <button
            onClick={reset}
            className="inline-flex items-center gap-2 px-6 py-3 bg-indigo-600 text-white rounded-2xl font-bold hover:bg-indigo-700 transition-all"
          >
            <RefreshCw size={18} />
            {t('error.retry')}
          </button>
          <Link
            href="/"
            className="inline-flex items-center gap-2 px-6 py-3 bg-slate-800 text-slate-300 rounded-2xl font-bold hover:bg-slate-700 transition-all"
          >
            <Home size={18} />
            {t('error.goHome')}
          </Link>
        </div>
      </div>
    </div>
  );
}
