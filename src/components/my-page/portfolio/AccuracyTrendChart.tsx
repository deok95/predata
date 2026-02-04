'use client';

import { BarChart3 } from 'lucide-react';
import type { AccuracyTrendPoint } from '@/types/api';

export default function AccuracyTrendChart({
  trend,
  loading,
  isDark,
  t,
}: {
  trend: AccuracyTrendPoint[];
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
        <div className={`h-6 w-48 rounded mb-6 ${isDark ? 'bg-slate-800' : 'bg-slate-200'}`} />
        <div className={`h-48 rounded-xl animate-pulse ${isDark ? 'bg-slate-800' : 'bg-slate-200'}`} />
      </div>
    );
  }

  const maxAccuracy = 100;

  return (
    <div
      className={`p-6 rounded-2xl border ${
        isDark ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-100 shadow-sm'
      }`}
    >
      <h3 className={`text-lg font-black mb-6 ${isDark ? 'text-white' : 'text-slate-900'}`}>
        {t('portfolio.accuracyTrend')}
      </h3>

      {trend.length === 0 ? (
        <div className="text-center py-12">
          <BarChart3 size={40} className="text-slate-400 mx-auto mb-3" />
          <p className={`font-bold ${isDark ? 'text-slate-400' : 'text-slate-500'}`}>
            {t('portfolio.noTrend')}
          </p>
          <p className="text-sm text-slate-400 mt-1">{t('portfolio.noTrendDesc')}</p>
        </div>
      ) : (
        <div>
          <div className="flex items-end gap-2 h-48 mb-4">
            {trend.map((point, idx) => {
              const barHeight = (point.accuracy / maxAccuracy) * 100;
              const cumHeight = (point.cumulativeAccuracy / maxAccuracy) * 100;
              return (
                <div key={idx} className="flex-1 flex flex-col items-center gap-1 relative group">
                  <div className={`absolute bottom-full mb-2 px-3 py-2 rounded-lg text-xs font-bold opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none z-10 whitespace-nowrap ${isDark ? 'bg-slate-700 text-white' : 'bg-slate-800 text-white'}`}>
                    <div>{point.date}</div>
                    <div>{t('portfolio.monthly')}: {point.accuracy}%</div>
                    <div>{t('portfolio.cumulative')}: {point.cumulativeAccuracy}%</div>
                    <div>{point.correctPredictions}/{point.totalPredictions} {t('portfolio.correct')}</div>
                  </div>

                  <div className="w-full flex items-end gap-0.5" style={{ height: '100%' }}>
                    <div
                      className="flex-1 rounded-t-md bg-indigo-500 transition-all duration-500 min-h-[4px]"
                      style={{ height: `${barHeight}%` }}
                    />
                    <div
                      className="flex-1 rounded-t-md transition-all duration-500 min-h-[4px]"
                      style={{
                        height: `${cumHeight}%`,
                        backgroundColor: isDark ? 'rgba(99,102,241,0.3)' : 'rgba(99,102,241,0.2)',
                      }}
                    />
                  </div>
                </div>
              );
            })}
          </div>

          <div className="flex gap-2">
            {trend.map((point, idx) => (
              <div key={idx} className="flex-1 text-center text-xs text-slate-400 truncate">
                {point.date}
              </div>
            ))}
          </div>

          <div className="flex items-center gap-6 mt-4 justify-center">
            <div className="flex items-center gap-2">
              <div className="w-3 h-3 rounded-sm bg-indigo-500" />
              <span className="text-xs text-slate-400">{t('portfolio.monthly')}</span>
            </div>
            <div className="flex items-center gap-2">
              <div
                className="w-3 h-3 rounded-sm"
                style={{ backgroundColor: isDark ? 'rgba(99,102,241,0.3)' : 'rgba(99,102,241,0.2)' }}
              />
              <span className="text-xs text-slate-400">{t('portfolio.cumulative')}</span>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
