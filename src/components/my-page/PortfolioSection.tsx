'use client';

import { useState, useEffect, useCallback } from 'react';
import { useTheme } from '@/hooks/useTheme';
import { useI18n } from '@/lib/i18n';
import { portfolioApi } from '@/lib/api';
import {
  DollarSign,
  TrendingUp,
  TrendingDown,
  Target,
  RefreshCw,
  AlertCircle,
} from 'lucide-react';
import { StatCard, SkeletonCard } from './portfolio/PortfolioSkeletons';
import OpenPositionsTable from './portfolio/OpenPositionsTable';
import CategoryBreakdownPanel from './portfolio/CategoryBreakdownPanel';
import AccuracyTrendChart from './portfolio/AccuracyTrendChart';
import type {
  PortfolioSummary,
  OpenPosition,
  CategoryPerformance,
  AccuracyTrendPoint,
} from '@/types/api';

export default function PortfolioSection() {
  const { isDark } = useTheme();
  const { t } = useI18n();

  const [summary, setSummary] = useState<PortfolioSummary | null>(null);
  const [positions, setPositions] = useState<OpenPosition[]>([]);
  const [categories, setCategories] = useState<CategoryPerformance[]>([]);
  const [trend, setTrend] = useState<AccuracyTrendPoint[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchData = useCallback(async () => {
    setLoading(true);
    setError(null);

    const [summaryResult, positionsResult, categoriesResult, trendResult] = await Promise.allSettled([
      portfolioApi.getSummary(),
      portfolioApi.getPositions(),
      portfolioApi.getCategoryBreakdown(),
      portfolioApi.getAccuracyTrend(),
    ]);

    if (summaryResult.status === 'fulfilled' && summaryResult.value.data) setSummary(summaryResult.value.data);
    if (positionsResult.status === 'fulfilled' && positionsResult.value.data) setPositions(positionsResult.value.data);
    if (categoriesResult.status === 'fulfilled' && categoriesResult.value.data) setCategories(categoriesResult.value.data);
    if (trendResult.status === 'fulfilled' && trendResult.value.data) setTrend(trendResult.value.data);

    const results = [summaryResult, positionsResult, categoriesResult, trendResult];
    const allFailed = results.every(r => r.status === 'rejected');

    if (allFailed) {
      const firstError = results.find(r => r.status === 'rejected') as PromiseRejectedResult | undefined;
      setError(firstError?.reason?.message || 'An error occurred.');
    }

    setLoading(false);
  }, []);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  if (error) {
    return (
      <div className={`p-8 rounded-2xl border text-center ${isDark ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-100 shadow-sm'}`}>
        <AlertCircle size={36} className="text-rose-500 mx-auto mb-3" />
        <p className={`font-bold mb-2 ${isDark ? 'text-white' : 'text-slate-900'}`}>{t('common.error')}</p>
        <p className="text-sm text-slate-400 mb-4">{error}</p>
        <button
          onClick={() => fetchData()}
          className="px-4 py-2 rounded-xl text-sm font-bold bg-indigo-600 text-white hover:bg-indigo-700 transition-all inline-flex items-center gap-2"
        >
          <RefreshCw size={14} />
          {t('common.retry')}
        </button>
      </div>
    );
  }

  return (
    <div>
      {/* Section Header */}
      <div className="flex items-center justify-between mb-6">
        <h2 className={`text-xl font-black ${isDark ? 'text-white' : 'text-slate-900'}`}>
          {t('portfolio.title')}
        </h2>
        <button
          onClick={() => fetchData()}
          disabled={loading}
          className={`p-2 rounded-xl transition-all ${
            isDark ? 'hover:bg-slate-800 text-slate-400' : 'hover:bg-slate-100 text-slate-500'
          } ${loading ? 'animate-spin' : ''}`}
        >
          <RefreshCw size={18} />
        </button>
      </div>

      {/* Stat Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        {loading ? (
          <>
            <SkeletonCard isDark={isDark} />
            <SkeletonCard isDark={isDark} />
            <SkeletonCard isDark={isDark} />
            <SkeletonCard isDark={isDark} />
          </>
        ) : summary ? (
          <>
            <StatCard label={t('portfolio.totalInvested')} value={summary.totalInvested} suffix="$" icon={DollarSign} colorMode="neutral" isDark={isDark} />
            <StatCard label={t('portfolio.totalReturns')} value={summary.totalReturns} suffix="$" icon={TrendingUp} colorMode="auto" isDark={isDark} />
            <StatCard label={t('portfolio.netPnL')} value={summary.netProfit} suffix="$" icon={summary.netProfit >= 0 ? TrendingUp : TrendingDown} colorMode="auto" isDark={isDark} />
            <StatCard label={t('portfolio.roi')} value={summary.roi} suffix="%" icon={Target} colorMode="auto" isDark={isDark} />
          </>
        ) : null}
      </div>

      {/* Secondary Stats */}
      {!loading && summary && (
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 mb-6">
          {[
            { label: t('portfolio.winRate'), value: `${summary.winRate}%` },
            { label: t('portfolio.totalBets'), value: summary.totalBets },
            { label: t('portfolio.openBets'), value: summary.openBets, highlight: true },
            { label: t('portfolio.balance'), value: `$${summary.currentBalance.toLocaleString()}` },
          ].map((stat) => (
            <div
              key={stat.label}
              className={`p-4 rounded-xl border text-center ${
                isDark ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-100 shadow-sm'
              }`}
            >
              <p className="text-xs text-slate-400 mb-1">{stat.label}</p>
              <p className={`text-lg font-black ${stat.highlight ? 'text-indigo-500' : isDark ? 'text-white' : 'text-slate-900'}`}>
                {stat.value}
              </p>
            </div>
          ))}
        </div>
      )}

      {/* Positions + Categories */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 mb-6">
        <div className="lg:col-span-7">
          <OpenPositionsTable positions={positions} loading={loading} isDark={isDark} t={t} />
        </div>
        <div className="lg:col-span-5">
          <CategoryBreakdownPanel categories={categories} loading={loading} isDark={isDark} t={t} />
        </div>
      </div>

      {/* Accuracy Trend */}
      <AccuracyTrendChart trend={trend} loading={loading} isDark={isDark} t={t} />
    </div>
  );
}
