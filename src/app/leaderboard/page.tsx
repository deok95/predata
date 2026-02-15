'use client';

import { useState, useEffect } from 'react';
import { Trophy, Medal, Crown, TrendingUp } from 'lucide-react';
import MainLayout from '@/components/layout/MainLayout';
import { useTheme } from '@/hooks/useTheme';
import { useAuth } from '@/hooks/useAuth';
import { leaderboardApi } from '@/lib/api';
import { Skeleton } from '@/components/ui/Skeleton';
import { useI18n } from '@/lib/i18n';

interface LeaderboardEntry {
  rank: number;
  memberId: number;
  email: string;
  tier: string;
  accuracyScore: number;
  accuracyPercentage: number;
  totalPredictions: number;
  correctPredictions: number;
  usdcBalance: number;
}

const tierBadge: Record<string, { label: string; color: string }> = {
  BRONZE: { label: 'B', color: 'bg-amber-700/20 text-amber-600' },
  SILVER: { label: 'S', color: 'bg-slate-400/20 text-slate-500' },
  GOLD: { label: 'G', color: 'bg-yellow-500/20 text-yellow-600' },
  PLATINUM: { label: 'P', color: 'bg-indigo-500/20 text-indigo-500' },
};

function RankIcon({ rank }: { rank: number }) {
  if (rank === 1) return <Crown size={20} className="text-yellow-500" />;
  if (rank === 2) return <Medal size={20} className="text-slate-400" />;
  if (rank === 3) return <Medal size={20} className="text-amber-600" />;
  return <span className="text-sm font-black text-slate-400 w-5 text-center">{rank}</span>;
}

function LeaderboardContent() {
  const { isDark } = useTheme();
  const { user } = useAuth();
  const { t } = useI18n();
  const [entries, setEntries] = useState<LeaderboardEntry[]>([]);
  const [myRank, setMyRank] = useState<LeaderboardEntry | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    leaderboardApi.getTop(50).then(res => {
      if (res.success && res.data) setEntries(res.data as unknown as LeaderboardEntry[]);
    }).catch(() => {}).finally(() => setLoading(false));

    if (user && !user.email?.includes('guest')) {
      leaderboardApi.getMemberRank(user.id).then(res => {
        if (res.success && res.data) setMyRank(res.data as unknown as LeaderboardEntry);
      }).catch(() => {});
    }
  }, [user]);

  return (
    <div className="max-w-4xl mx-auto animate-fade-in">
      <div className="mb-8">
        <div className="flex items-center gap-3 mb-2">
          <Trophy size={28} className="text-yellow-500" />
          <h1 className={`text-3xl font-black ${isDark ? 'text-white' : 'text-slate-900'}`}>{t('leaderboard.title')}</h1>
        </div>
        <p className="text-slate-400">{t('leaderboard.subtitle')}</p>
      </div>

      {myRank && (
        <div className={`p-5 rounded-2xl border mb-6 ${isDark ? 'bg-indigo-950/20 border-indigo-900/30' : 'bg-indigo-50 border-indigo-100'}`}>
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-4">
              <div className="w-10 h-10 bg-indigo-600 rounded-xl flex items-center justify-center text-white font-black text-sm">
                #{myRank.rank}
              </div>
              <div>
                <p className={`font-bold text-sm ${isDark ? 'text-white' : 'text-slate-900'}`}>{t('leaderboard.myRank')}</p>
                <p className="text-xs text-slate-400">{t('leaderboard.score')}: {myRank.accuracyScore} / {t('leaderboard.accuracy')}: {myRank.accuracyPercentage.toFixed(1)}%</p>
              </div>
            </div>
            <div className="text-right">
              <p className="text-lg font-black text-indigo-600">${myRank.usdcBalance.toLocaleString()} USDC</p>
              <p className="text-xs text-slate-400">{myRank.totalPredictions}{t('leaderboard.predictions')}</p>
            </div>
          </div>
        </div>
      )}

      <div className={`rounded-3xl border overflow-hidden ${isDark ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-100'}`}>
        <div className={`grid grid-cols-12 gap-2 px-6 py-3 text-xs font-bold text-slate-400 border-b ${isDark ? 'border-slate-800 bg-slate-950' : 'border-slate-100 bg-slate-50'}`}>
          <div className="col-span-1">#</div>
          <div className="col-span-4">{t('leaderboard.predictor')}</div>
          <div className="col-span-2 text-center">{t('leaderboard.tier')}</div>
          <div className="col-span-2 text-right">{t('leaderboard.accuracy')}</div>
          <div className="col-span-3 text-right">{t('leaderboard.points')}</div>
        </div>

        {loading ? (
          <div className="p-6 space-y-4">
            {Array.from({ length: 10 }).map((_, i) => (
              <div key={i} className="flex items-center gap-4">
                <Skeleton className="h-6 w-6 rounded-full" />
                <Skeleton className="h-5 w-32" />
                <Skeleton className="h-5 w-12 ml-auto" />
                <Skeleton className="h-5 w-16" />
              </div>
            ))}
          </div>
        ) : entries.length === 0 ? (
          <div className="text-center py-16">
            <TrendingUp size={40} className="text-slate-400 mx-auto mb-4" />
            <p className="text-slate-400 font-bold">{t('leaderboard.noData')}</p>
            <p className="text-xs text-slate-500 mt-1">{t('leaderboard.noDataDesc')}</p>
          </div>
        ) : (
          entries.map((entry) => {
            const isMe = user && entry.memberId === user.id;
            return (
              <div
                key={entry.memberId}
                className={`grid grid-cols-12 gap-2 px-6 py-4 items-center border-b last:border-b-0 transition-all ${
                  isMe
                    ? isDark ? 'bg-indigo-950/20 border-indigo-900/20' : 'bg-indigo-50/50 border-indigo-100/50'
                    : isDark ? 'border-slate-800/50 hover:bg-slate-800/50' : 'border-slate-50 hover:bg-slate-50'
                }`}
              >
                <div className="col-span-1 flex items-center">
                  <RankIcon rank={entry.rank} />
                </div>
                <div className="col-span-4">
                  <p className={`text-sm font-bold truncate ${isDark ? 'text-white' : 'text-slate-900'}`}>
                    {entry.email} {isMe && <span className="text-indigo-500 text-xs">{t('leaderboard.me')}</span>}
                  </p>
                  <p className="text-[10px] text-slate-400">{entry.totalPredictions}{t('leaderboard.predictions')} / {entry.correctPredictions}{t('leaderboard.correct')}</p>
                </div>
                <div className="col-span-2 flex justify-center">
                  <span className={`px-2.5 py-1 rounded-lg text-[10px] font-black ${tierBadge[entry.tier]?.color || 'bg-slate-100 text-slate-400'}`}>
                    {entry.tier}
                  </span>
                </div>
                <div className="col-span-2 text-right">
                  <span className="text-sm font-black text-emerald-500">{entry.accuracyPercentage.toFixed(1)}%</span>
                </div>
                <div className="col-span-3 text-right">
                  <span className={`text-sm font-bold ${isDark ? 'text-slate-300' : 'text-slate-700'}`}>${entry.usdcBalance.toLocaleString()} USDC</span>
                </div>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
}

export default function LeaderboardPage() {
  return (
    <MainLayout>
      <LeaderboardContent />
    </MainLayout>
  );
}
