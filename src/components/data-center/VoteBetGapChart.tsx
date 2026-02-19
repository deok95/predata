'use client';

import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import { GitCompareArrows, CheckCircle, AlertCircle } from 'lucide-react';
import type { VoteBetGapReport } from '@/types/api';

interface VoteBetGapChartProps {
  gapAnalysis: VoteBetGapReport;
  isDark: boolean;
}

const COLORS = { yes: '#10B981', no: '#EF4444' };

export default function VoteBetGapChart({ gapAnalysis, isDark }: VoteBetGapChartProps) {
  const comparisonData = [
    {
      name: 'Vote (Ticketer)',
      YES: gapAnalysis.voteDistribution.yesPercentage,
      NO: gapAnalysis.voteDistribution.noPercentage,
    },
    {
      name: 'Bet (Bettor)',
      YES: gapAnalysis.betDistribution.yesPercentage,
      NO: gapAnalysis.betDistribution.noPercentage,
    },
  ];

  const cardClass = `p-6 rounded-3xl border ${isDark ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-100'}`;
  const isGood = gapAnalysis.gapPercentage < 10;

  return (
    <div className={cardClass}>
      <div className="flex items-center gap-2 mb-2">
        <GitCompareArrows className="h-6 w-6 text-purple-500" />
        <h2 className={`text-lg font-black ${isDark ? 'text-white' : 'text-slate-900'}`}>Vote vs Bet Gap</h2>
        <span className={`ml-auto text-sm font-black px-3 py-1 rounded-xl ${
          isGood
            ? isDark ? 'bg-emerald-950/30 text-emerald-400' : 'bg-emerald-50 text-emerald-600'
            : isDark ? 'bg-rose-950/30 text-rose-400' : 'bg-rose-50 text-rose-600'
        }`}>
          GAP {gapAnalysis.gapPercentage.toFixed(1)}%
        </span>
      </div>
      <p className="text-xs text-slate-400 mb-4">
        Compare vote and bet choice distributions to verify data quality.
      </p>

      <ResponsiveContainer width="100%" height={240}>
        <BarChart data={comparisonData} layout="vertical" barGap={8}>
          <CartesianGrid strokeDasharray="3 3" stroke={isDark ? '#1e293b' : '#e2e8f0'} horizontal={false} />
          <XAxis type="number" domain={[0, 100]} tick={{ fill: isDark ? '#94a3b8' : '#64748b', fontSize: 12 }} unit="%" />
          <YAxis type="category" dataKey="name" tick={{ fill: isDark ? '#94a3b8' : '#64748b', fontWeight: 700, fontSize: 12 }} width={110} />
          <Tooltip
            contentStyle={{
              backgroundColor: isDark ? '#0f172a' : '#fff',
              border: isDark ? '1px solid #1e293b' : '1px solid #e2e8f0',
              borderRadius: '12px',
              fontWeight: 700,
            }}
            formatter={(value) => `${Number(value).toFixed(1)}%`}
          />
          <Legend />
          <Bar dataKey="YES" fill={COLORS.yes} name="YES %" radius={[0, 6, 6, 0]} barSize={28} />
          <Bar dataKey="NO" fill={COLORS.no} name="NO %" radius={[0, 6, 6, 0]} barSize={28} />
        </BarChart>
      </ResponsiveContainer>

      <div className={`mt-4 p-3 rounded-2xl flex items-center gap-2 ${
        isGood
          ? isDark ? 'bg-emerald-950/20 border border-emerald-900/30' : 'bg-emerald-50'
          : isDark ? 'bg-rose-950/20 border border-rose-900/30' : 'bg-rose-50'
      }`}>
        {isGood ? (
          <>
            <CheckCircle size={18} className="text-emerald-500 shrink-0" />
            <p className="font-black text-emerald-500 text-sm">Excellent data quality — Gap rate {gapAnalysis.gapPercentage.toFixed(1)}%</p>
          </>
        ) : (
          <>
            <AlertCircle size={18} className="text-rose-500 shrink-0" />
            <p className="font-black text-rose-500 text-sm">High gap detected — {gapAnalysis.gapPercentage.toFixed(1)}% (filtering recommended)</p>
          </>
        )}
      </div>
    </div>
  );
}
