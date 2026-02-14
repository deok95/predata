'use client';

import { PieChart } from 'lucide-react';
import { SkeletonTable } from './PortfolioSkeletons';
import type { CategoryPerformance } from '@/types/api';

const categoryColors: Record<string, string> = {
  ECONOMY: 'bg-blue-500',
  SPORTS: 'bg-emerald-500',
  POLITICS: 'bg-amber-500',
  TECH: 'bg-purple-500',
  CULTURE: 'bg-pink-500',
  OTHER: 'bg-slate-500',
};

export default function CategoryBreakdownPanel({
  categories,
  loading,
  isDark,
  t,
}: {
  categories: CategoryPerformance[];
  loading: boolean;
  isDark: boolean;
  t: (key: string) => string;
}) {
  if (loading) {
    return (
      <div
        className={`p-6 rounded-2xl border ${
          isDark ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-100 shadow-sm'
        }`}
      >
        <div className={`h-6 w-40 rounded mb-6 ${isDark ? 'bg-slate-800' : 'bg-slate-200'}`} />
        <SkeletonTable isDark={isDark} rows={4} />
      </div>
    );
  }

  return (
    <div
      className={`p-6 rounded-2xl border ${
        isDark ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-100 shadow-sm'
      }`}
    >
      <h3 className={`text-lg font-black mb-6 ${isDark ? 'text-white' : 'text-slate-900'}`}>
        {t('portfolio.categoryBreakdown')}
      </h3>

      {categories.length === 0 ? (
        <div className="text-center py-12">
          <PieChart size={40} className="text-slate-400 mx-auto mb-3" />
          <p className={`font-bold ${isDark ? 'text-slate-400' : 'text-slate-500'}`}>
            {t('portfolio.noCategories')}
          </p>
        </div>
      ) : (
        <div className="space-y-4">
          {categories.map((cat) => {
            const totalSettled = cat.wins + cat.losses;
            const barWidth = totalSettled > 0 ? (cat.wins / totalSettled) * 100 : 0;
            const profitColor = cat.profit >= 0 ? 'text-emerald-500' : 'text-rose-500';
            const dotColor = categoryColors[cat.category] || categoryColors.OTHER;

            return (
              <div key={cat.category} className="space-y-2">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <div className={`w-3 h-3 rounded-full ${dotColor}`} />
                    <span className={`text-sm font-bold ${isDark ? 'text-white' : 'text-slate-900'}`}>
                      {cat.category}
                    </span>
                    <span className="text-xs text-slate-400">
                      {cat.totalBets} {t('portfolio.bets')}
                    </span>
                  </div>
                  <div className="flex items-center gap-3">
                    <span className="text-xs text-slate-400">
                      {cat.wins}W / {cat.losses}L
                    </span>
                    <span className={`text-sm font-black ${profitColor}`}>
                      {cat.profit >= 0 ? '+' : ''}
                      {'$'}{cat.profit.toLocaleString()}
                    </span>
                  </div>
                </div>

                <div className={`w-full h-2 rounded-full ${isDark ? 'bg-slate-800' : 'bg-slate-100'}`}>
                  <div
                    className="h-full rounded-full bg-emerald-500 transition-all duration-500"
                    style={{ width: `${barWidth}%` }}
                  />
                </div>
                <div className="flex justify-between text-xs text-slate-400">
                  <span>{t('portfolio.winRate')}: {cat.winRate}%</span>
                  <span>
                    {t('portfolio.invested')}: {'$'}{cat.invested.toLocaleString()}
                  </span>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
