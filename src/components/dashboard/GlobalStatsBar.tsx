'use client';

import { useState, useEffect } from 'react';
import { Activity, Database, Award, Globe, ArrowUpRight } from 'lucide-react';
import { useTheme } from '@/hooks/useTheme';
import { globalApi } from '@/lib/api';
import type { LucideIcon } from 'lucide-react';

function GlobalStat({ label, value, trend, icon: Icon, isDark }: {
  label: string; value: string; trend?: string; icon: LucideIcon; isDark: boolean;
}) {
  return (
    <div className={`p-5 rounded-2xl border shadow-sm flex items-center justify-between transition-all hover:shadow-lg hover:-translate-y-0.5 ${
      isDark ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-100'
    }`}>
      <div>
        <p className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">{label}</p>
        <div className="flex items-baseline space-x-2">
          <span className={`text-2xl font-black ${isDark ? 'text-white' : 'text-slate-900'}`}>{value}</span>
          {trend && (
            <span className="text-[10px] font-bold text-emerald-500 flex items-center">
              <ArrowUpRight size={10} className="mr-0.5" /> {trend}
            </span>
          )}
        </div>
      </div>
      <div className={`w-12 h-12 rounded-xl flex items-center justify-center text-slate-400 ${
        isDark ? 'bg-slate-800' : 'bg-slate-50'
      }`}>
        <Icon size={20} />
      </div>
    </div>
  );
}

export default function GlobalStatsBar() {
  const { isDark } = useTheme();
  const [stats, setStats] = useState({ totalPredictions: 0, tvl: 0, cumulativeRewards: 0, activeUsers: 0 });

  useEffect(() => {
    globalApi.getStats().then(res => {
      if (res.success && res.data) setStats(res.data);
      else setStats({ totalPredictions: 0, tvl: 0, cumulativeRewards: 0, activeUsers: 0 });
    }).catch(() => {
      setStats({ totalPredictions: 0, tvl: 0, cumulativeRewards: 0, activeUsers: 0 });
    });
  }, []);

  const fmt = (n: number) => n.toLocaleString();

  return (
    <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
      <GlobalStat label="Total Predictions" value={fmt(stats.totalPredictions)} icon={Activity} isDark={isDark} />
      <GlobalStat label="Global TVL" value={`$${stats.tvl.toLocaleString()}`} icon={Database} isDark={isDark} />
      <GlobalStat label="Cumulative Rewards" value={`${stats.cumulativeRewards.toLocaleString()} PRE`} icon={Award} isDark={isDark} />
      <GlobalStat label="Active Users" value={fmt(stats.activeUsers)} icon={Globe} isDark={isDark} />
    </div>
  );
}
