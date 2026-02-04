'use client';

import { AlertTriangle } from 'lucide-react';
import { useTheme } from '@/hooks/useTheme';
import { useI18n } from '@/lib/i18n';

export default function DemoBanner({ className = '' }: { className?: string }) {
  const { isDark } = useTheme();
  const { t } = useI18n();

  return (
    <div className={`flex items-center gap-2 px-3 py-2 rounded-xl text-xs font-bold ${
      isDark
        ? 'bg-amber-950/30 border border-amber-800/40 text-amber-400'
        : 'bg-amber-50 border border-amber-200 text-amber-700'
    } ${className}`}>
      <AlertTriangle size={14} />
      <span>{t('demo.banner')}</span>
    </div>
  );
}
