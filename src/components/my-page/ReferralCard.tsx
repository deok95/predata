'use client';

import { useState, useEffect } from 'react';
import { Users, Copy, Check, Gift, Share2 } from 'lucide-react';
import { useTheme } from '@/hooks/useTheme';
import { useI18n } from '@/lib/i18n';
import { referralApi } from '@/lib/api';
import type { ReferralStats } from '@/types/api';

interface ReferralCardProps {
  memberId: number;
}

export default function ReferralCard({ memberId }: ReferralCardProps) {
  const { isDark } = useTheme();
  const { t } = useI18n();
  const [stats, setStats] = useState<ReferralStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [codeCopied, setCodeCopied] = useState(false);
  const [linkCopied, setLinkCopied] = useState(false);

  useEffect(() => {
    let cancelled = false;
    async function fetchStats() {
      try {
        const res = await referralApi.getStats();
        if (!cancelled && res.data) {
          setStats(res.data);
        }
      } catch {
        // silently fail - component will show empty state
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    fetchStats();
    return () => { cancelled = true; };
  }, [memberId]);

  const handleCopyCode = async () => {
    if (!stats?.referralCode) return;
    try {
      await navigator.clipboard.writeText(stats.referralCode);
      setCodeCopied(true);
      setTimeout(() => setCodeCopied(false), 2000);
    } catch {
      // clipboard API not available
    }
  };

  const handleCopyLink = async () => {
    if (!stats?.referralCode) return;
    const link = `${window.location.origin}?ref=${stats.referralCode}`;
    try {
      await navigator.clipboard.writeText(link);
      setLinkCopied(true);
      setTimeout(() => setLinkCopied(false), 2000);
    } catch {
      // clipboard API not available
    }
  };

  const maskEmail = (email: string): string => {
    const [local, domain] = email.split('@');
    if (!local || !domain) return '***';
    const masked = local.length <= 2
      ? local[0] + '***'
      : local.slice(0, 2) + '***';
    return `${masked}@${domain}`;
  };

  if (loading) {
    return (
      <div className={`rounded-2xl border p-6 ${isDark ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-100 shadow-sm'}`}>
        <div className="flex items-center gap-3 mb-5">
          <div className="w-10 h-10 rounded-xl bg-indigo-600 flex items-center justify-center">
            <Gift size={20} className="text-white" />
          </div>
          <div>
            <h3 className={`font-black text-lg ${isDark ? 'text-white' : 'text-slate-900'}`}>{t('referral.title')}</h3>
          </div>
        </div>
        <div className="animate-pulse space-y-3">
          <div className={`h-12 rounded-xl ${isDark ? 'bg-slate-800' : 'bg-slate-100'}`} />
          <div className={`h-8 rounded-xl ${isDark ? 'bg-slate-800' : 'bg-slate-100'}`} />
          <div className={`h-8 rounded-xl ${isDark ? 'bg-slate-800' : 'bg-slate-100'}`} />
        </div>
      </div>
    );
  }

  return (
    <div className={`rounded-2xl border p-6 ${isDark ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-100 shadow-sm'}`}>
      {/* Header */}
      <div className="flex items-center gap-3 mb-5">
        <div className="w-10 h-10 rounded-xl bg-indigo-600 flex items-center justify-center">
          <Gift size={20} className="text-white" />
        </div>
        <div>
          <h3 className={`font-black text-lg ${isDark ? 'text-white' : 'text-slate-900'}`}>{t('referral.title')}</h3>
          <p className="text-xs text-slate-400">{t('referral.inviteDesc')}</p>
        </div>
      </div>

      {/* Referral Code */}
      <div className="mb-4">
        <label className="text-xs font-bold text-slate-400 uppercase mb-1.5 block">{t('referral.code')}</label>
        <div className={`flex items-center gap-2 p-3 rounded-xl border ${isDark ? 'bg-slate-800 border-slate-700' : 'bg-slate-50 border-slate-200'}`}>
          <code className={`flex-1 font-mono font-bold text-sm tracking-wider ${isDark ? 'text-indigo-400' : 'text-indigo-600'}`}>
            {stats?.referralCode || '---'}
          </code>
          <button
            onClick={handleCopyCode}
            className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-bold transition-all ${
              codeCopied
                ? 'bg-emerald-500/10 text-emerald-500'
                : isDark
                  ? 'bg-slate-700 text-slate-300 hover:bg-slate-600'
                  : 'bg-white text-slate-600 hover:bg-slate-100 border border-slate-200'
            }`}
          >
            {codeCopied ? <Check size={14} /> : <Copy size={14} />}
            {codeCopied ? t('referral.copied') : t('referral.code')}
          </button>
        </div>
      </div>

      {/* Copy Link Button */}
      <button
        onClick={handleCopyLink}
        className={`w-full flex items-center justify-center gap-2 py-3 rounded-xl font-bold text-sm transition-all mb-5 ${
          linkCopied
            ? 'bg-emerald-500/10 text-emerald-500 border border-emerald-500/30'
            : 'bg-indigo-600 text-white hover:bg-indigo-700 active:scale-[0.98]'
        }`}
      >
        {linkCopied ? <Check size={16} /> : <Share2 size={16} />}
        {linkCopied ? t('referral.copied') : t('referral.copyLink')}
      </button>

      {/* Stats */}
      <div className="grid grid-cols-2 gap-3 mb-5">
        <div className={`p-4 rounded-xl text-center ${isDark ? 'bg-slate-800' : 'bg-slate-50'}`}>
          <Users size={18} className="text-indigo-500 mx-auto mb-1.5" />
          <p className={`font-black text-xl ${isDark ? 'text-white' : 'text-slate-900'}`}>
            {stats?.totalReferrals ?? 0}
          </p>
          <p className="text-[11px] text-slate-400 font-medium">{t('referral.totalReferrals')}</p>
        </div>
        <div className={`p-4 rounded-xl text-center ${isDark ? 'bg-slate-800' : 'bg-slate-50'}`}>
          <Gift size={18} className="text-amber-500 mx-auto mb-1.5" />
          <p className={`font-black text-xl ${isDark ? 'text-white' : 'text-slate-900'}`}>
            {(stats?.totalPointsEarned ?? 0).toLocaleString()}P
          </p>
          <p className="text-[11px] text-slate-400 font-medium">{t('referral.totalRewards')}</p>
        </div>
      </div>

      {/* Referee List */}
      <div>
        <h4 className={`text-xs font-bold text-slate-400 uppercase mb-3`}>{t('referral.refereeList')}</h4>
        {(!stats?.referees || stats.referees.length === 0) ? (
          <div className={`text-center py-6 rounded-xl ${isDark ? 'bg-slate-800/50' : 'bg-slate-50'}`}>
            <Users size={24} className="text-slate-400 mx-auto mb-2" />
            <p className="text-sm text-slate-400">{t('referral.noReferrals')}</p>
          </div>
        ) : (
          <div className="space-y-2">
            {stats.referees.map((referee, idx) => (
              <div
                key={idx}
                className={`flex items-center justify-between p-3 rounded-xl ${isDark ? 'bg-slate-800' : 'bg-slate-50'}`}
              >
                <div>
                  <p className={`text-sm font-medium ${isDark ? 'text-white' : 'text-slate-900'}`}>
                    {maskEmail(referee.email)}
                  </p>
                  <p className="text-[11px] text-slate-400">
                    {new Date(referee.joinedAt).toLocaleDateString()}
                  </p>
                </div>
                <span className={`text-xs font-bold px-2.5 py-1 rounded-lg ${isDark ? 'bg-emerald-500/10 text-emerald-400' : 'bg-emerald-50 text-emerald-600'}`}>
                  +{referee.rewardAmount}P
                </span>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
