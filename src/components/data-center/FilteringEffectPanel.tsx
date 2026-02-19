'use client';

import { Shield, ArrowRight } from 'lucide-react';
import type { FilteringEffectReport } from '@/types/api';

interface FilteringEffectPanelProps {
  filteringEffect: FilteringEffectReport;
  isDark: boolean;
}

export default function FilteringEffectPanel({ filteringEffect, isDark }: FilteringEffectPanelProps) {
  const cardClass = `p-6 rounded-3xl border ${isDark ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-100'}`;
  const remaining = filteringEffect.afterFiltering.totalCount;
  const filtered = filteringEffect.filteredCount;
  const total = filteringEffect.beforeFiltering.totalCount;
  const remainingPct = total > 0 ? (remaining / total) * 100 : 100;

  return (
    <div className={cardClass}>
      <div className="flex items-center gap-2 mb-4">
        <Shield className="h-6 w-6 text-amber-500" />
        <h2 className={`text-lg font-black ${isDark ? 'text-white' : 'text-slate-900'}`}>Abuse Filtering Effect</h2>
      </div>
      <p className="text-xs text-slate-400 mb-5">Filters mindless votes (responses under 2 seconds) to improve data quality.</p>

      <div className="grid grid-cols-2 gap-4 mb-5">
        <div className={`p-5 rounded-2xl ${isDark ? 'bg-rose-950/20 border border-rose-900/30' : 'bg-rose-50'}`}>
          <p className="text-xs font-bold text-slate-400 uppercase mb-1">Before Filtering</p>
          <p className="text-2xl font-black text-rose-500">{filteringEffect.beforeFiltering.totalCount.toLocaleString()}</p>
          <p className="text-xs text-slate-400 mt-1">YES {filteringEffect.beforeFiltering.yesPercentage.toFixed(1)}%</p>
        </div>
        <div className={`p-5 rounded-2xl ${isDark ? 'bg-emerald-950/20 border border-emerald-900/30' : 'bg-emerald-50'}`}>
          <p className="text-xs font-bold text-slate-400 uppercase mb-1">After Filtering</p>
          <p className="text-2xl font-black text-emerald-500">{filteringEffect.afterFiltering.totalCount.toLocaleString()}</p>
          <p className="text-xs text-slate-400 mt-1">YES {filteringEffect.afterFiltering.yesPercentage.toFixed(1)}%</p>
        </div>
      </div>

      {/* Ratio bar */}
      <div className={`rounded-xl overflow-hidden h-3 ${isDark ? 'bg-slate-800' : 'bg-slate-100'}`}>
        <div className="h-full flex">
          <div className="h-full bg-emerald-500 transition-all" style={{ width: `${remainingPct}%` }} />
          <div className="h-full bg-rose-500 transition-all" style={{ width: `${100 - remainingPct}%` }} />
        </div>
      </div>
      <div className="flex items-center justify-between mt-2 text-xs">
        <span className="font-bold text-emerald-500">Valid {remaining.toLocaleString()}</span>
        <span className="font-bold text-rose-500">Removed {filtered.toLocaleString()} ({filteringEffect.filteredPercentage.toFixed(1)}%)</span>
      </div>
    </div>
  );
}
