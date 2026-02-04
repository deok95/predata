'use client';

import Link from 'next/link';
import { Home, ArrowLeft } from 'lucide-react';
import { useI18n } from '@/lib/i18n';

export default function NotFound() {
  const { t } = useI18n();

  return (
    <div className="min-h-screen flex items-center justify-center bg-slate-950 text-white p-4">
      <div className="text-center max-w-md">
        <div className="text-8xl font-black text-indigo-600 mb-4">404</div>
        <h1 className="text-2xl font-black mb-3">{t('notFound.title')}</h1>
        <p className="text-slate-400 mb-8">
          {t('notFound.desc')}
        </p>
        <div className="flex gap-3 justify-center">
          <Link
            href="/"
            className="inline-flex items-center gap-2 px-6 py-3 bg-indigo-600 text-white rounded-2xl font-bold hover:bg-indigo-700 transition-all"
          >
            <Home size={18} />
            {t('notFound.goHome')}
          </Link>
          <button
            onClick={() => window.history.back()}
            className="inline-flex items-center gap-2 px-6 py-3 bg-slate-800 text-slate-300 rounded-2xl font-bold hover:bg-slate-700 transition-all"
          >
            <ArrowLeft size={18} />
            {t('notFound.goBack')}
          </button>
        </div>
      </div>
    </div>
  );
}
