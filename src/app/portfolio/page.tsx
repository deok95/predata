'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import MainLayout from '@/components/layout/MainLayout';
import BetHistory from '@/components/my-page/BetHistory';
import DisputeHistory from '@/components/my-page/DisputeHistory';
import PortfolioSection from '@/components/my-page/PortfolioSection';
import { useTheme } from '@/hooks/useTheme';
import { useAuth } from '@/hooks/useAuth';
import { useI18n } from '@/lib/i18n';

function PortfolioContent() {
  const { isDark } = useTheme();
  const { user, isGuest } = useAuth();
  const { t } = useI18n();
  const router = useRouter();

  useEffect(() => {
    if (isGuest) {
      router.replace('/');
    }
  }, [isGuest, router]);

  if (!user || isGuest) {
    return null;
  }

  return (
    <div className="max-w-7xl mx-auto animate-fade-in">
      <div className="mb-8">
        <h1 className={`text-3xl font-black mb-2 ${isDark ? 'text-white' : 'text-slate-900'}`}>{t('nav.portfolio')}</h1>
        <p className="text-slate-400">{t('myPage.subtitle')}</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <BetHistory memberId={user.id} />
        <DisputeHistory memberId={user.id} />
      </div>

      <div className="mt-8">
        <PortfolioSection />
      </div>
    </div>
  );
}

export default function PortfolioPage() {
  return (
    <MainLayout>
      <PortfolioContent />
    </MainLayout>
  );
}
