'use client';

import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip, Legend } from 'recharts';
import type { LucideIcon } from 'lucide-react';

interface DemographicsPieChartProps {
  title: string;
  icon: LucideIcon;
  iconColor: string;
  data: Array<{ name: string; value: number; yesPercentage: number }>;
  isDark: boolean;
}

const PALETTE = ['#6366f1', '#8b5cf6', '#a78bfa', '#c4b5fd', '#818cf8', '#4f46e5', '#7c3aed', '#5b21b6'];

export default function DemographicsPieChart({ title, icon: Icon, iconColor, data, isDark }: DemographicsPieChartProps) {
  const cardClass = `p-6 rounded-3xl border ${isDark ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-100'}`;
  const totalVotes = data.reduce((sum, d) => sum + d.value, 0);

  return (
    <div className={cardClass}>
      <div className="flex items-center gap-2 mb-4">
        <Icon className={`h-5 w-5 ${iconColor}`} />
        <h3 className={`text-base font-black ${isDark ? 'text-white' : 'text-slate-900'}`}>{title}</h3>
      </div>

      <div className="relative" style={{ width: '100%', height: 200 }}>
        <ResponsiveContainer width="100%" height="100%">
          <PieChart>
            <Pie
              data={data}
              cx="50%"
              cy="50%"
              innerRadius={50}
              outerRadius={80}
              paddingAngle={3}
              dataKey="value"
              strokeWidth={0}
            >
              {data.map((_, index) => (
                <Cell key={index} fill={PALETTE[index % PALETTE.length]} />
              ))}
            </Pie>
            <Tooltip
              contentStyle={{
                backgroundColor: isDark ? '#0f172a' : '#fff',
                border: isDark ? '1px solid #1e293b' : '1px solid #e2e8f0',
                borderRadius: '12px',
                fontWeight: 700,
                fontSize: 12,
              }}
              formatter={(value, _name, props) => {
                const entry = (props as any).payload;
                return [`${Number(value).toLocaleString()} votes (YES ${entry.yesPercentage.toFixed(1)}%)`, entry.name];
              }}
            />
          </PieChart>
        </ResponsiveContainer>
        <div className="absolute inset-0 flex flex-col items-center justify-center pointer-events-none">
          <p className={`text-lg font-black ${isDark ? 'text-white' : 'text-slate-900'}`}>{totalVotes.toLocaleString()}</p>
          <p className="text-[10px] font-bold text-slate-400">Total Votes</p>
        </div>
      </div>

      <div className="mt-2 space-y-1.5 max-h-32 overflow-y-auto">
        {data.map((d, i) => (
          <div key={d.name} className="flex items-center justify-between text-xs">
            <div className="flex items-center gap-2">
              <div className="w-2.5 h-2.5 rounded-full shrink-0" style={{ backgroundColor: PALETTE[i % PALETTE.length] }} />
              <span className={`font-bold ${isDark ? 'text-slate-300' : 'text-slate-600'}`}>{d.name}</span>
            </div>
            <span className="font-black text-slate-400">{d.value.toLocaleString()}</span>
          </div>
        ))}
      </div>
    </div>
  );
}
