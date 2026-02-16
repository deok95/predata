'use client';

import Link from 'next/link';
import { Target, ExternalLink } from 'lucide-react';
import { SkeletonTable } from './PortfolioSkeletons';
import type { OpenPosition } from '@/types/api';

export default function OpenPositionsTable({
  positions,
  loading,
  isDark,
  t,
}: {
  positions: OpenPosition[];
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
      <div className="flex items-center justify-between mb-6">
        <h3 className={`text-lg font-black ${isDark ? 'text-white' : 'text-slate-900'}`}>
          {t('portfolio.openPositions')}
        </h3>
        <span
          className={`text-xs font-bold px-3 py-1 rounded-full ${
            isDark ? 'bg-indigo-500/10 text-indigo-400' : 'bg-indigo-50 text-indigo-600'
          }`}
        >
          {positions.length} {t('portfolio.active')}
        </span>
      </div>

      {positions.length === 0 ? (
        <div className="text-center py-12">
          <Target size={40} className="text-slate-400 mx-auto mb-3" />
          <p className={`font-bold ${isDark ? 'text-slate-400' : 'text-slate-500'}`}>
            {t('portfolio.noPositions')}
          </p>
          <p className="text-sm text-slate-400 mt-1">{t('portfolio.noPositionsDesc')}</p>
        </div>
      ) : (
        <div className="overflow-x-auto -mx-6">
          <table className="w-full min-w-[640px]">
            <thead>
              <tr className={`text-xs font-bold uppercase ${isDark ? 'text-slate-500' : 'text-slate-400'}`}>
                <th className="text-left px-6 pb-3">{t('portfolio.market')}</th>
                <th className="text-center pb-3">{t('portfolio.choice')}</th>
                <th className="text-right pb-3">{t('portfolio.invested')}</th>
                <th className="text-right pb-3">{t('portfolio.estPayout')}</th>
                <th className="text-right pb-3 pr-6">{t('portfolio.pnl')}</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/50">
              {positions.map((pos) => {
                const pnlColor = pos.estimatedProfitLoss >= 0 ? 'text-emerald-500' : 'text-rose-500';
                return (
                  <tr
                    key={pos.activityId}
                    className={`transition-colors ${
                      isDark ? 'hover:bg-slate-800/50' : 'hover:bg-slate-50'
                    }`}
                  >
                    <td className="px-6 py-4">
                      <Link
                        href={`/question/${pos.questionId}`}
                        className="flex items-center gap-2 group"
                      >
                        <span
                          className={`text-sm font-bold group-hover:text-indigo-500 transition-colors truncate max-w-[280px] ${
                            isDark ? 'text-white' : 'text-slate-900'
                          }`}
                        >
                          {pos.questionTitle}
                        </span>
                        <ExternalLink size={14} className="text-slate-400 opacity-0 group-hover:opacity-100 transition-opacity flex-shrink-0" />
                      </Link>
                      {pos.category && (
                        <span className="text-xs text-slate-400 mt-0.5 block">{pos.category}</span>
                      )}
                    </td>
                    <td className="text-center py-4">
                      <span
                        className={`text-xs font-black px-3 py-1 rounded-full ${
                          pos.choice === 'YES'
                            ? 'bg-emerald-500/10 text-emerald-500'
                            : 'bg-rose-500/10 text-rose-500'
                        }`}
                      >
                        {pos.choice}
                      </span>
                    </td>
                    <td className={`text-right py-4 font-bold text-sm ${isDark ? 'text-slate-300' : 'text-slate-700'}`}>
                      {'$'}{pos.betAmount.toLocaleString()}
                    </td>
                    <td className={`text-right py-4 font-bold text-sm ${isDark ? 'text-slate-300' : 'text-slate-700'}`}>
                      {'$'}{pos.estimatedPayout.toLocaleString()}
                    </td>
                    <td className={`text-right py-4 pr-6 font-black text-sm ${pnlColor}`}>
                      {pos.estimatedProfitLoss >= 0 ? '+' : ''}
                      {'$'}{pos.estimatedProfitLoss.toLocaleString()}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
