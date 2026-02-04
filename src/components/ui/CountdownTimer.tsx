'use client';

import { useState, useEffect } from 'react';
import { Clock } from 'lucide-react';
import { useI18n } from '@/lib/i18n';

interface CountdownTimerProps {
  expiresAt: string;
  compact?: boolean;
}

type Urgency = 'normal' | 'warning' | 'critical' | 'expired';

export default function CountdownTimer({ expiresAt, compact = false }: CountdownTimerProps) {
  const { t } = useI18n();
  const [remaining, setRemaining] = useState('');
  const [urgency, setUrgency] = useState<Urgency>('normal');

  useEffect(() => {
    const update = () => {
      const now = Date.now();
      const target = new Date(expiresAt).getTime();
      const diff = target - now;

      if (diff <= 0) {
        setRemaining(t('timer.expired'));
        setUrgency('expired');
        return;
      }

      const days = Math.floor(diff / 86400000);
      const hours = Math.floor((diff % 86400000) / 3600000);
      const minutes = Math.floor((diff % 3600000) / 60000);
      const seconds = Math.floor((diff % 60000) / 1000);

      if (diff < 3600000) {
        setUrgency('critical');
        setRemaining(`${minutes}${t('timer.min')} ${seconds}${t('timer.sec')}`);
      } else if (diff < 86400000) {
        setUrgency('warning');
        setRemaining(compact ? `${hours}${t('timer.hour')} ${minutes}${t('timer.min')}` : `${hours}${t('timer.hour')} ${minutes}${t('timer.min')} ${seconds}${t('timer.sec')}`);
      } else {
        setUrgency('normal');
        setRemaining(compact ? `${days}${t('timer.day')} ${hours}${t('timer.hour')}` : `${days}${t('timer.day')} ${hours}${t('timer.hour')} ${minutes}${t('timer.min')}`);
      }
    };

    update();
    const interval = setInterval(update, 1000);
    return () => clearInterval(interval);
  }, [expiresAt, compact]);

  const colorClass = {
    normal: 'text-slate-400',
    warning: 'text-amber-500',
    critical: 'text-rose-500 animate-pulse',
    expired: 'text-slate-500',
  }[urgency];

  if (compact) {
    return <span className={`text-xs font-bold ${colorClass}`}>{remaining}</span>;
  }

  return (
    <span className={`inline-flex items-center gap-1 text-sm font-bold ${colorClass}`}>
      <Clock size={14} />
      {remaining}
    </span>
  );
}
