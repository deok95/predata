'use client';

import { useMemo } from 'react';
import { Coins, Users, TrendingUp, Trophy, BarChart3 } from 'lucide-react';
import type { Question, DistributionData, Activity } from '@/types/api';

interface BettingOverviewPanelProps {
  question: Question | null;
  betDistribution: DistributionData;
  activities: Activity[];
  totalVotes: number;
  parsedPools?: {
    totalPool: number;
    yesPool: number;
    noPool: number;
  };
  isDark: boolean;
}

interface BetStats {
  totalBettors: number;
  yesBettors: number;
  noBettors: number;
  avgBet: number;
  avgYesBet: number;
  avgNoBet: number;
  maxBet: number;
  participationRate: number;
}

function computeBetStats(activities: Activity[], totalVotes: number): BetStats {
  const bets = activities.filter(a => a.activityType === 'BET');
  const yesBets = bets.filter(b => b.choice === 'YES');
  const noBets = bets.filter(b => b.choice === 'NO');
  const totalBettorsSet = new Set(bets.map((b) => b.memberId));
  const yesBettorsSet = new Set(yesBets.map((b) => b.memberId));
  const noBettorsSet = new Set(noBets.map((b) => b.memberId));

  const totalAmount = bets.reduce((s, b) => s + b.amount, 0);
  const yesAmount = yesBets.reduce((s, b) => s + b.amount, 0);
  const noAmount = noBets.reduce((s, b) => s + b.amount, 0);
  const maxBet = bets.length > 0 ? Math.max(...bets.map(b => b.amount)) : 0;

  return {
    totalBettors: totalBettorsSet.size,
    yesBettors: yesBettorsSet.size,
    noBettors: noBettorsSet.size,
    avgBet: bets.length > 0 ? Math.round(totalAmount / bets.length) : 0,
    avgYesBet: yesBets.length > 0 ? Math.round(yesAmount / yesBets.length) : 0,
    avgNoBet: noBets.length > 0 ? Math.round(noAmount / noBets.length) : 0,
    maxBet,
    participationRate: totalVotes > 0 ? Math.round((totalBettorsSet.size / totalVotes) * 1000) / 10 : 0,
  };
}

export default function BettingOverviewPanel({ question, betDistribution, activities, totalVotes, parsedPools, isDark }: BettingOverviewPanelProps) {
  const cardClass = `p-6 rounded-3xl border ${isDark ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-100'}`;
  const statBg = isDark ? 'bg-slate-800/60' : 'bg-slate-50';
  const questionTotalPool = question?.totalBetPool ?? 0;
  const questionYesPool = question?.yesBetPool ?? 0;
  const questionNoPool = question?.noBetPool ?? 0;
  const hasQuestionPools = questionTotalPool > 0 || questionYesPool > 0 || questionNoPool > 0;
  const totalPool = hasQuestionPools ? questionTotalPool : (parsedPools?.totalPool ?? 0);
  const yesPool = hasQuestionPools ? questionYesPool : (parsedPools?.yesPool ?? 0);
  const noPool = hasQuestionPools ? questionNoPool : (parsedPools?.noPool ?? 0);
  const yesPct = totalPool > 0 ? (yesPool / totalPool) * 100 : 50;

  const stats = useMemo(() => computeBetStats(activities, totalVotes), [activities, totalVotes]);

  return (
    <div className={cardClass}>
      <div className="flex items-center gap-2 mb-5">
        <Coins className="h-6 w-6 text-indigo-500" />
        <h2 className={`text-lg font-black ${isDark ? 'text-white' : 'text-slate-900'}`}>Betting Analysis</h2>
      </div>

      {/* Pool cards */}
      <div className="grid grid-cols-3 gap-3 mb-5">
        <div className={`p-4 rounded-2xl text-center ${isDark ? 'bg-indigo-950/20 border border-indigo-900/30' : 'bg-indigo-50'}`}>
          <p className="text-[10px] font-bold text-slate-400 uppercase mb-1">Total Pool</p>
          <p className="text-lg font-black text-indigo-500">{totalPool.toLocaleString()}</p>
        </div>
        <div className={`p-4 rounded-2xl text-center ${isDark ? 'bg-emerald-950/20 border border-emerald-900/30' : 'bg-emerald-50'}`}>
          <p className="text-[10px] font-bold text-slate-400 uppercase mb-1">YES Pool</p>
          <p className="text-lg font-black text-emerald-500">{yesPool.toLocaleString()}</p>
        </div>
        <div className={`p-4 rounded-2xl text-center ${isDark ? 'bg-rose-950/20 border border-rose-900/30' : 'bg-rose-50'}`}>
          <p className="text-[10px] font-bold text-slate-400 uppercase mb-1">NO Pool</p>
          <p className="text-lg font-black text-rose-500">{noPool.toLocaleString()}</p>
        </div>
      </div>

      {/* Pool ratio bar */}
      <div className="mb-5">
        <div className="flex items-center justify-between text-xs mb-1">
          <span className="font-bold text-emerald-500">YES {yesPct.toFixed(1)}%</span>
          <span className="font-bold text-rose-500">NO {(100 - yesPct).toFixed(1)}%</span>
        </div>
        <div className={`rounded-xl overflow-hidden h-3 ${isDark ? 'bg-slate-800' : 'bg-slate-100'}`}>
          <div className="h-full flex">
            <div className="h-full bg-emerald-500 transition-all" style={{ width: `${yesPct}%` }} />
            <div className="h-full bg-rose-500 transition-all" style={{ width: `${100 - yesPct}%` }} />
          </div>
        </div>
      </div>

      {/* Extended stats */}
      <div className="space-y-2">
        {/* Participants */}
        <div className={`p-3 rounded-xl flex items-center justify-between ${statBg}`}>
          <div className="flex items-center gap-2">
            <Users size={14} className="text-indigo-500" />
            <span className={`text-xs font-bold ${isDark ? 'text-slate-300' : 'text-slate-600'}`}>Bettors</span>
          </div>
          <div className="flex items-center gap-3 text-xs">
            <span className="font-black text-emerald-500">YES {stats.yesBettors}</span>
            <span className="font-black text-rose-500">NO {stats.noBettors}</span>
            <span className={`font-black ${isDark ? 'text-white' : 'text-slate-900'}`}>Total {stats.totalBettors}</span>
          </div>
        </div>

        {/* Average bet */}
        <div className={`p-3 rounded-xl flex items-center justify-between ${statBg}`}>
          <div className="flex items-center gap-2">
            <BarChart3 size={14} className="text-purple-500" />
            <span className={`text-xs font-bold ${isDark ? 'text-slate-300' : 'text-slate-600'}`}>Avg Bet</span>
          </div>
          <div className="flex items-center gap-3 text-xs">
            <span className="font-bold text-emerald-500">{stats.avgYesBet.toLocaleString()}</span>
            <span className="font-bold text-rose-500">{stats.avgNoBet.toLocaleString()}</span>
            <span className={`font-black ${isDark ? 'text-white' : 'text-slate-900'}`}>{'$'}{stats.avgBet.toLocaleString()}</span>
          </div>
        </div>

        {/* Max bet */}
        <div className={`p-3 rounded-xl flex items-center justify-between ${statBg}`}>
          <div className="flex items-center gap-2">
            <Trophy size={14} className="text-amber-500" />
            <span className={`text-xs font-bold ${isDark ? 'text-slate-300' : 'text-slate-600'}`}>Max Bet</span>
          </div>
          <span className={`text-xs font-black ${isDark ? 'text-white' : 'text-slate-900'}`}>{'$'}{stats.maxBet.toLocaleString()}</span>
        </div>

        {/* Participation rate */}
        <div className={`p-3 rounded-xl flex items-center justify-between ${statBg}`}>
          <div className="flex items-center gap-2">
            <TrendingUp size={14} className="text-blue-500" />
            <span className={`text-xs font-bold ${isDark ? 'text-slate-300' : 'text-slate-600'}`}>Betting Rate</span>
          </div>
          <div className="flex items-center gap-2">
            <span className={`text-xs font-black ${isDark ? 'text-white' : 'text-slate-900'}`}>{stats.participationRate}%</span>
            <span className="text-[10px] text-slate-400">(vs votes)</span>
          </div>
        </div>
      </div>
    </div>
  );
}
