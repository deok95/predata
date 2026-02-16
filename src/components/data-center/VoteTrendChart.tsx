'use client';

import { useMemo } from 'react';
import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend } from 'recharts';
import { TrendingUp, Info } from 'lucide-react';
import type { Activity } from '@/types/api';

interface VoteTrendChartProps {
  activities: Activity[];
  isDark: boolean;
}

interface TrendPoint {
  label: string;
  cumulativeYes: number;
  cumulativeNo: number;
  yesPercentage: number;
  betVolume: number;
}

function deriveTimeSeries(activities: Activity[]): TrendPoint[] {
  const sorted = [...activities].sort(
    (a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()
  );

  // Group by hour bucket
  const buckets = new Map<string, { yesVotes: number; noVotes: number; betVolume: number }>();

  sorted.forEach((act) => {
    const dt = new Date(act.createdAt);
    const key = `${dt.getMonth() + 1}/${dt.getDate()} ${dt.getHours()}:00`;
    const bucket = buckets.get(key) || { yesVotes: 0, noVotes: 0, betVolume: 0 };

    if (act.activityType === 'VOTE') {
      if (act.choice === 'YES') bucket.yesVotes++;
      else bucket.noVotes++;
    } else if (act.activityType === 'BET') {
      bucket.betVolume += act.amount;
    }

    buckets.set(key, bucket);
  });

  // Convert to cumulative series
  let cumYes = 0;
  let cumNo = 0;
  const points: TrendPoint[] = [];

  buckets.forEach((bucket, label) => {
    cumYes += bucket.yesVotes;
    cumNo += bucket.noVotes;
    const total = cumYes + cumNo;
    points.push({
      label,
      cumulativeYes: cumYes,
      cumulativeNo: cumNo,
      yesPercentage: total > 0 ? Math.round((cumYes / total) * 1000) / 10 : 0,
      betVolume: bucket.betVolume,
    });
  });

  return points;
}

export default function VoteTrendChart({ activities, isDark }: VoteTrendChartProps) {
  const trendData = useMemo(() => deriveTimeSeries(activities), [activities]);
  const cardClass = `p-6 rounded-3xl border ${isDark ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-100'}`;
  const hasEnoughData = trendData.length >= 3;

  return (
    <div className={cardClass}>
      <div className="flex items-center gap-2 mb-2">
        <TrendingUp className="h-6 w-6 text-indigo-600" />
        <h2 className={`text-lg font-black ${isDark ? 'text-white' : 'text-slate-900'}`}>투표 추이</h2>
      </div>
      <p className="text-xs text-slate-400 mb-4">시간대별 누적 투표 및 YES 비율 추이</p>

      {hasEnoughData ? (
        <ResponsiveContainer width="100%" height={300}>
          <AreaChart data={trendData}>
            <defs>
              <linearGradient id="yesGradient" x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor="#10B981" stopOpacity={0.3} />
                <stop offset="95%" stopColor="#10B981" stopOpacity={0} />
              </linearGradient>
              <linearGradient id="noGradient" x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor="#EF4444" stopOpacity={0.3} />
                <stop offset="95%" stopColor="#EF4444" stopOpacity={0} />
              </linearGradient>
            </defs>
            <CartesianGrid strokeDasharray="3 3" stroke={isDark ? '#1e293b' : '#e2e8f0'} />
            <XAxis
              dataKey="label"
              tick={{ fill: isDark ? '#94a3b8' : '#64748b', fontSize: 11 }}
              interval="preserveStartEnd"
            />
            <YAxis
              yAxisId="left"
              tick={{ fill: isDark ? '#94a3b8' : '#64748b', fontSize: 11 }}
              label={{ value: '누적 투표', angle: -90, position: 'insideLeft', style: { fill: isDark ? '#64748b' : '#94a3b8', fontSize: 11 } }}
            />
            <YAxis
              yAxisId="right"
              orientation="right"
              domain={[0, 100]}
              tick={{ fill: isDark ? '#94a3b8' : '#64748b', fontSize: 11 }}
              label={{ value: 'YES %', angle: 90, position: 'insideRight', style: { fill: isDark ? '#64748b' : '#94a3b8', fontSize: 11 } }}
            />
            <Tooltip
              contentStyle={{
                backgroundColor: isDark ? '#0f172a' : '#fff',
                border: isDark ? '1px solid #1e293b' : '1px solid #e2e8f0',
                borderRadius: '12px',
                fontWeight: 700,
                fontSize: 12,
              }}
            />
            <Legend />
            <Area
              yAxisId="left"
              type="monotone"
              dataKey="cumulativeYes"
              stroke="#10B981"
              fill="url(#yesGradient)"
              strokeWidth={2}
              name="누적 YES"
            />
            <Area
              yAxisId="left"
              type="monotone"
              dataKey="cumulativeNo"
              stroke="#EF4444"
              fill="url(#noGradient)"
              strokeWidth={2}
              name="누적 NO"
            />
            <Area
              yAxisId="right"
              type="monotone"
              dataKey="yesPercentage"
              stroke="#6366f1"
              fill="none"
              strokeWidth={2}
              strokeDasharray="5 5"
              name="YES %"
            />
          </AreaChart>
        </ResponsiveContainer>
      ) : (
        <div className={`flex flex-col items-center justify-center py-12 rounded-2xl ${isDark ? 'bg-slate-800/50' : 'bg-slate-50'}`}>
          <Info className="h-8 w-8 text-slate-400 mb-3" />
          <p className={`text-sm font-bold ${isDark ? 'text-slate-400' : 'text-slate-500'}`}>
            활동 데이터가 부족하여 트렌드를 표시할 수 없습니다
          </p>
          <p className="text-xs text-slate-400 mt-1">최소 3개의 시간 구간 데이터가 필요합니다</p>
        </div>
      )}
    </div>
  );
}
