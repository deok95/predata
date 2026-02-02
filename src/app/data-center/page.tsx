'use client';

import { useState, useEffect } from 'react';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import { Database, TrendingUp, AlertCircle, CheckCircle, Filter, Users, Briefcase } from 'lucide-react';
import MainLayout from '@/components/layout/MainLayout';
import { useTheme } from '@/hooks/useTheme';
import { analyticsApi } from '@/lib/api';
import { mockQualityDashboard } from '@/lib/mockData';
import type { QualityDashboard } from '@/types/api';

const COLORS = {
  yes: '#10B981',
  no: '#EF4444',
  primary: '#4f46e5',
  secondary: '#8B5CF6',
};

function DataCenterContent() {
  const { isDark } = useTheme();
  const [dashboardData, setDashboardData] = useState<QualityDashboard | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [questionId] = useState(1);

  useEffect(() => {
    const fetchDashboard = async () => {
      setLoading(true);
      setError(null);
      try {
        const res = await analyticsApi.getDashboard(questionId);
        if (res.success && res.data) {
          const data = res.data as unknown as QualityDashboard;
          if (data.gapAnalysis && data.demographics && data.filteringEffect) {
            setDashboardData(data);
          } else {
            setDashboardData(mockQualityDashboard);
          }
        } else {
          setDashboardData(mockQualityDashboard);
        }
      } catch {
        setDashboardData(mockQualityDashboard);
      } finally {
        setLoading(false);
      }
    };
    fetchDashboard();
  }, [questionId]);

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-center">
          <Database className="h-12 w-12 text-indigo-600 mx-auto mb-4 animate-pulse" />
          <p className={`text-lg font-black ${isDark ? 'text-white' : 'text-slate-900'}`}>데이터 로딩 중...</p>
        </div>
      </div>
    );
  }

  if (error || !dashboardData) {
    return (
      <div className="max-w-7xl mx-auto animate-fade-in">
        <div className="mb-8">
          <div className="flex items-center gap-3 mb-2">
            <Database className="h-8 w-8 text-indigo-600" />
            <h1 className={`text-3xl font-black ${isDark ? 'text-white' : 'text-slate-900'}`}>데이터 센터</h1>
          </div>
          <p className="text-slate-400">고품질 예측 데이터 분석 대시보드</p>
        </div>
        <div className={`p-8 rounded-[2.5rem] border text-center ${isDark ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-100'}`}>
          <AlertCircle className="h-12 w-12 text-rose-500 mx-auto mb-4" />
          <p className={`text-lg font-black mb-2 ${isDark ? 'text-white' : 'text-slate-900'}`}>데이터 로드 실패</p>
          <p className="text-slate-400 text-sm">{error || '알 수 없는 오류가 발생했습니다.'}</p>
        </div>
      </div>
    );
  }

  const { demographics, gapAnalysis, filteringEffect, overallQualityScore } = dashboardData;

  const comparisonData = [
    {
      name: '티케터 (투표)',
      YES: gapAnalysis.voteDistribution.yesPercentage,
      NO: gapAnalysis.voteDistribution.noPercentage,
    },
    {
      name: '베터 (베팅)',
      YES: gapAnalysis.betDistribution.yesPercentage,
      NO: gapAnalysis.betDistribution.noPercentage,
    },
  ];

  const countryChartData = demographics.byCountry.map(c => ({
    name: c.countryCode,
    YES: c.yesPercentage,
    NO: 100 - c.yesPercentage,
    total: c.total,
  }));

  const jobChartData = demographics.byJob.map(j => ({
    name: j.jobCategory,
    YES: j.yesPercentage,
    NO: 100 - j.yesPercentage,
    total: j.total,
  }));

  const cardClass = `p-6 rounded-3xl border ${isDark ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-100'}`;
  const labelClass = 'text-xs font-bold text-slate-400 uppercase tracking-wider';

  return (
    <div className="max-w-7xl mx-auto animate-fade-in">
      {/* 헤더 */}
      <div className="mb-8">
        <div className="flex items-center gap-3 mb-2">
          <Database className="h-8 w-8 text-indigo-600" />
          <h1 className={`text-3xl font-black ${isDark ? 'text-white' : 'text-slate-900'}`}>데이터 센터</h1>
        </div>
        <p className="text-slate-400">고품질 예측 데이터 분석 대시보드</p>
      </div>

      {/* 품질 점수 카드 */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-8">
        <div className={cardClass}>
          <p className={labelClass}>전체 품질 점수</p>
          <p className="text-3xl font-black text-indigo-600 mt-2">{overallQualityScore.toFixed(1)}점</p>
        </div>
        <div className={cardClass}>
          <p className={labelClass}>총 투표 수</p>
          <p className="text-3xl font-black text-emerald-500 mt-2">{demographics.totalVotes.toLocaleString()}</p>
        </div>
        <div className={cardClass}>
          <p className={labelClass}>괴리율</p>
          <p className="text-3xl font-black text-purple-500 mt-2">{gapAnalysis.gapPercentage.toFixed(1)}%</p>
        </div>
        <div className={cardClass}>
          <p className={labelClass}>필터링율</p>
          <p className="text-3xl font-black text-amber-500 mt-2">{filteringEffect.filteredPercentage.toFixed(1)}%</p>
        </div>
      </div>

      {/* 투표 vs 베팅 괴리율 */}
      <div className={`${cardClass} mb-8`}>
        <div className="flex items-center gap-2 mb-4">
          <TrendingUp className="h-6 w-6 text-indigo-600" />
          <h2 className={`text-xl font-black ${isDark ? 'text-white' : 'text-slate-900'}`}>티케터 vs 베터 괴리율 분석</h2>
        </div>
        <p className="text-sm text-slate-400 mb-6">
          티케터(투표)와 베터(베팅)의 선택 분포를 비교하여 데이터 품질을 검증합니다.
          괴리율이 낮을수록 고품질 데이터입니다.
        </p>

        <ResponsiveContainer width="100%" height={300}>
          <BarChart data={comparisonData}>
            <CartesianGrid strokeDasharray="3 3" stroke={isDark ? '#1e293b' : '#e2e8f0'} />
            <XAxis dataKey="name" tick={{ fill: isDark ? '#94a3b8' : '#64748b', fontWeight: 700, fontSize: 12 }} />
            <YAxis tick={{ fill: isDark ? '#94a3b8' : '#64748b', fontSize: 12 }} />
            <Tooltip
              contentStyle={{
                backgroundColor: isDark ? '#0f172a' : '#fff',
                border: isDark ? '1px solid #1e293b' : '1px solid #e2e8f0',
                borderRadius: '12px',
                fontWeight: 700,
              }}
            />
            <Legend />
            <Bar dataKey="YES" fill={COLORS.yes} name="YES %" radius={[6, 6, 0, 0]} />
            <Bar dataKey="NO" fill={COLORS.no} name="NO %" radius={[6, 6, 0, 0]} />
          </BarChart>
        </ResponsiveContainer>

        <div className={`mt-4 p-4 rounded-2xl ${
          gapAnalysis.gapPercentage < 10
            ? isDark ? 'bg-emerald-950/20 border border-emerald-900/30' : 'bg-emerald-50'
            : isDark ? 'bg-rose-950/20 border border-rose-900/30' : 'bg-rose-50'
        }`}>
          {gapAnalysis.gapPercentage < 10 ? (
            <div className="flex items-center gap-2 text-emerald-500">
              <CheckCircle size={20} />
              <p className="font-black">우수한 데이터 품질! 괴리율 {gapAnalysis.gapPercentage.toFixed(1)}%</p>
            </div>
          ) : (
            <div className="flex items-center gap-2 text-rose-500">
              <AlertCircle size={20} />
              <p className="font-black">높은 괴리율 감지! {gapAnalysis.gapPercentage.toFixed(1)}% (어뷰징 필터링 필요)</p>
            </div>
          )}
        </div>
      </div>

      {/* 페르소나별 분석 */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
        {/* 국가별 분포 */}
        <div className={cardClass}>
          <div className="flex items-center gap-2 mb-4">
            <Users className="h-6 w-6 text-indigo-600" />
            <h2 className={`text-xl font-black ${isDark ? 'text-white' : 'text-slate-900'}`}>국가별 투표 분포</h2>
          </div>
          <ResponsiveContainer width="100%" height={300}>
            <BarChart data={countryChartData}>
              <CartesianGrid strokeDasharray="3 3" stroke={isDark ? '#1e293b' : '#e2e8f0'} />
              <XAxis dataKey="name" tick={{ fill: isDark ? '#94a3b8' : '#64748b', fontWeight: 700, fontSize: 12 }} />
              <YAxis tick={{ fill: isDark ? '#94a3b8' : '#64748b', fontSize: 12 }} />
              <Tooltip
                contentStyle={{
                  backgroundColor: isDark ? '#0f172a' : '#fff',
                  border: isDark ? '1px solid #1e293b' : '1px solid #e2e8f0',
                  borderRadius: '12px',
                  fontWeight: 700,
                }}
              />
              <Legend />
              <Bar dataKey="YES" fill={COLORS.yes} name="YES %" radius={[6, 6, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>

        {/* 직업별 분포 */}
        <div className={cardClass}>
          <div className="flex items-center gap-2 mb-4">
            <Briefcase className="h-6 w-6 text-purple-500" />
            <h2 className={`text-xl font-black ${isDark ? 'text-white' : 'text-slate-900'}`}>직업별 투표 분포</h2>
          </div>
          <ResponsiveContainer width="100%" height={300}>
            <BarChart data={jobChartData}>
              <CartesianGrid strokeDasharray="3 3" stroke={isDark ? '#1e293b' : '#e2e8f0'} />
              <XAxis dataKey="name" tick={{ fill: isDark ? '#94a3b8' : '#64748b', fontWeight: 700, fontSize: 12 }} />
              <YAxis tick={{ fill: isDark ? '#94a3b8' : '#64748b', fontSize: 12 }} />
              <Tooltip
                contentStyle={{
                  backgroundColor: isDark ? '#0f172a' : '#fff',
                  border: isDark ? '1px solid #1e293b' : '1px solid #e2e8f0',
                  borderRadius: '12px',
                  fontWeight: 700,
                }}
              />
              <Legend />
              <Bar dataKey="YES" fill={COLORS.secondary} name="YES %" radius={[6, 6, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* 어뷰징 필터링 효과 */}
      <div className={cardClass}>
        <div className="flex items-center gap-2 mb-4">
          <Filter className="h-6 w-6 text-amber-500" />
          <h2 className={`text-xl font-black ${isDark ? 'text-white' : 'text-slate-900'}`}>어뷰징 필터링 효과</h2>
        </div>
        <p className="text-sm text-slate-400 mb-6">
          무지성 투표(2초 이하 응답)를 필터링하여 데이터 품질을 향상시킵니다.
        </p>

        <div className="grid grid-cols-2 gap-4 mb-4">
          <div className={`p-5 rounded-2xl ${isDark ? 'bg-rose-950/20 border border-rose-900/30' : 'bg-rose-50'}`}>
            <p className="text-xs font-bold text-slate-400 uppercase mb-1">필터링 전</p>
            <p className="text-2xl font-black text-rose-500">{filteringEffect.beforeFiltering.totalCount.toLocaleString()}개</p>
            <p className="text-sm text-slate-400 mt-1">YES: {filteringEffect.beforeFiltering.yesPercentage.toFixed(1)}%</p>
          </div>
          <div className={`p-5 rounded-2xl ${isDark ? 'bg-emerald-950/20 border border-emerald-900/30' : 'bg-emerald-50'}`}>
            <p className="text-xs font-bold text-slate-400 uppercase mb-1">필터링 후</p>
            <p className="text-2xl font-black text-emerald-500">{filteringEffect.afterFiltering.totalCount.toLocaleString()}개</p>
            <p className="text-sm text-slate-400 mt-1">YES: {filteringEffect.afterFiltering.yesPercentage.toFixed(1)}%</p>
          </div>
        </div>

        <div className={`p-4 rounded-2xl ${isDark ? 'bg-indigo-950/20 border border-indigo-900/30' : 'bg-indigo-50'}`}>
          <p className="text-center text-base font-black text-indigo-600">
            {filteringEffect.filteredCount.toLocaleString()}개 ({filteringEffect.filteredPercentage.toFixed(1)}%) 제거됨
          </p>
        </div>
      </div>
    </div>
  );
}

export default function DataCenterPage() {
  return (
    <MainLayout>
      <DataCenterContent />
    </MainLayout>
  );
}
