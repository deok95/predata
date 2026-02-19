'use client';

import { useMemo } from 'react';
import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend } from 'recharts';
import { TrendingUp, Info } from 'lucide-react';
import type { Activity } from '@/types/api';

interface BettingTrendChartProps {
  activities: Activity[];
  isDark: boolean;
}

interface BetTrendPoint {
  label: string;
  cumulativeYes: number;
  cumulativeNo: number;
  hourlyYes: number;
  hourlyNo: number;
}

function deriveBetTimeSeries(activities: Activity[]): BetTrendPoint[] {
  const bets = activities
    .filter(a => a.activityType === 'BET')
    .sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime());

  if (bets.length === 0) return [];

  const buckets = new Map<string, { yes: number; no: number }>();

  bets.forEach((bet) => {
    const dt = new Date(bet.createdAt);
    const key = `${dt.getMonth() + 1}/${dt.getDate()} ${dt.getHours()}:00`;
    const bucket = buckets.get(key) || { yes: 0, no: 0 };

    if (bet.choice === 'YES') bucket.yes += bet.amount;
    else bucket.no += bet.amount;

    buckets.set(key, bucket);
  });

  let cumYes = 0;
  let cumNo = 0;
  const points: BetTrendPoint[] = [];

  buckets.forEach((bucket, label) => {
    cumYes += bucket.yes;
    cumNo += bucket.no;
    points.push({
      label,
      cumulativeYes: cumYes,
      cumulativeNo: cumNo,
      hourlyYes: bucket.yes,
      hourlyNo: bucket.no,
    });
  });

  return points;
}

export default function BettingTrendChart({ activities, isDark }: BettingTrendChartProps) {
  const trendData = useMemo(() => deriveBetTimeSeries(activities), [activities]);
  const cardClass = `p-6 rounded-3xl border ${isDark ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-100'}`;
  const hasData = trendData.length >= 2;

  return (
    <div className={cardClass}>
      <div className="flex items-center gap-2 mb-2">
        <TrendingUp className="h-6 w-6 text-indigo-600" />
        <h2 className={`text-lg font-black ${isDark ? 'text-white' : 'text-slate-900'}`}>Betting Amount Trend</h2>
      </div>
      <p className="text-xs text-slate-400 mb-4">Cumulative betting amount by time period — Visualize YES/NO flow at a glance</p>

      {hasData ? (
        <ResponsiveContainer width="100%" height={320}>
          <AreaChart data={trendData}>
            <defs>
              <linearGradient id="betYesGrad" x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor="#10B981" stopOpacity={0.35} />
                <stop offset="95%" stopColor="#10B981" stopOpacity={0.02} />
              </linearGradient>
              <linearGradient id="betNoGrad" x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor="#EF4444" stopOpacity={0.35} />
                <stop offset="95%" stopColor="#EF4444" stopOpacity={0.02} />
              </linearGradient>
            </defs>
            <CartesianGrid strokeDasharray="3 3" stroke={isDark ? '#1e293b' : '#e2e8f0'} />
            <XAxis
              dataKey="label"
              tick={{ fill: isDark ? '#94a3b8' : '#64748b', fontSize: 11 }}
              interval="preserveStartEnd"
            />
            <YAxis
              tick={{ fill: isDark ? '#94a3b8' : '#64748b', fontSize: 11 }}
              tickFormatter={(v) => v >= 1000 ? `${(v / 1000).toFixed(0)}K` : String(v)}
            />
            <Tooltip
              contentStyle={{
                backgroundColor: isDark ? '#0f172a' : '#fff',
                border: isDark ? '1px solid #1e293b' : '1px solid #e2e8f0',
                borderRadius: '12px',
                fontWeight: 700,
                fontSize: 12,
              }}
              formatter={(value) => `$${Number(value).toLocaleString()}`}
            />
            <Legend />
            <Area
              type="monotone"
              dataKey="cumulativeYes"
              stroke="#10B981"
              fill="url(#betYesGrad)"
              strokeWidth={2.5}
              name="YES Cumulative"
            />
            <Area
              type="monotone"
              dataKey="cumulativeNo"
              stroke="#EF4444"
              fill="url(#betNoGrad)"
              strokeWidth={2.5}
              name="NO Cumulative"
            />
          </AreaChart>
        </ResponsiveContainer>
      ) : (
        <div className={`flex flex-col items-center justify-center py-12 rounded-2xl ${isDark ? 'bg-slate-800/50' : 'bg-slate-50'}`}>
          <Info className="h-8 w-8 text-slate-400 mb-3" />
          <p className={`text-sm font-bold ${isDark ? 'text-slate-400' : 'text-slate-500'}`}>
            Insufficient betting data to display trend
          </p>
          <p className="text-xs text-slate-400 mt-1">At least 2 time intervals of data required</p>
        </div>
      )}
    </div>
  );
}
