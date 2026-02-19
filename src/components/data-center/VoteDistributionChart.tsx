'use client';

import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip } from 'recharts';
import { Vote } from 'lucide-react';
import type { DistributionData } from '@/types/api';

interface VoteDistributionChartProps {
  voteDistribution: DistributionData;
  isDark: boolean;
}

const COLORS = { yes: '#10B981', no: '#EF4444' };

export default function VoteDistributionChart({ voteDistribution, isDark }: VoteDistributionChartProps) {
  const data = [
    { name: 'YES', value: voteDistribution.yesCount, percentage: voteDistribution.yesPercentage },
    { name: 'NO', value: voteDistribution.noCount, percentage: voteDistribution.noPercentage },
  ];

  const cardClass = `p-6 rounded-3xl border ${isDark ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-100'}`;

  return (
    <div className={cardClass}>
      <div className="flex items-center gap-2 mb-4">
        <Vote className="h-6 w-6 text-indigo-600" />
        <h2 className={`text-lg font-black ${isDark ? 'text-white' : 'text-slate-900'}`}>Vote Distribution</h2>
      </div>

      <div className="flex items-center justify-center">
        <div className="relative" style={{ width: 220, height: 220 }}>
          <ResponsiveContainer width="100%" height="100%">
            <PieChart>
              <Pie
                data={data}
                cx="50%"
                cy="50%"
                innerRadius={65}
                outerRadius={95}
                paddingAngle={4}
                dataKey="value"
                strokeWidth={0}
              >
                {data.map((entry, index) => (
                  <Cell key={entry.name} fill={index === 0 ? COLORS.yes : COLORS.no} />
                ))}
              </Pie>
              <Tooltip
                formatter={(value, name) => [`${Number(value).toLocaleString()} votes`, String(name)]}
                contentStyle={{
                  backgroundColor: isDark ? '#0f172a' : '#fff',
                  border: isDark ? '1px solid #1e293b' : '1px solid #e2e8f0',
                  borderRadius: '12px',
                  fontWeight: 700,
                  fontSize: 13,
                }}
              />
            </PieChart>
          </ResponsiveContainer>
          <div className="absolute inset-0 flex flex-col items-center justify-center">
            <p className={`text-2xl font-black ${isDark ? 'text-white' : 'text-slate-900'}`}>
              {voteDistribution.yesPercentage.toFixed(1)}%
            </p>
            <p className="text-xs font-bold text-emerald-500">YES</p>
          </div>
        </div>
      </div>

      <div className="mt-4 grid grid-cols-2 gap-3">
        <div className={`p-3 rounded-xl text-center ${isDark ? 'bg-emerald-950/20' : 'bg-emerald-50'}`}>
          <p className="text-xs font-bold text-slate-400">YES</p>
          <p className="text-lg font-black text-emerald-500">{voteDistribution.yesCount.toLocaleString()}</p>
        </div>
        <div className={`p-3 rounded-xl text-center ${isDark ? 'bg-rose-950/20' : 'bg-rose-50'}`}>
          <p className="text-xs font-bold text-slate-400">NO</p>
          <p className="text-lg font-black text-rose-500">{voteDistribution.noCount.toLocaleString()}</p>
        </div>
      </div>
    </div>
  );
}
