'use client';

import React, { useState, useEffect } from 'react';
import { BarChart, Bar, PieChart, Pie, Cell, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import { Database, TrendingUp, AlertCircle, CheckCircle, Filter, Users, Briefcase, Calendar } from 'lucide-react';

const BACKEND_URL = 'http://localhost:8080/api';

const COLORS = {
  yes: '#10B981',
  no: '#EF4444',
  primary: '#3B82F6',
  secondary: '#8B5CF6',
  warning: '#F59E0B'
};

const COUNTRY_COLORS = ['#3B82F6', '#8B5CF6', '#EC4899', '#F59E0B', '#10B981'];

interface DashboardData {
  questionId: number;
  demographics: {
    totalVotes: number;
    byCountry: Array<{ countryCode: string; yesCount: number; noCount: number; yesPercentage: number; total: number }>;
    byJob: Array<{ jobCategory: string; yesCount: number; noCount: number; yesPercentage: number; total: number }>;
    byAge: Array<{ ageGroup: number; yesCount: number; noCount: number; yesPercentage: number; total: number }>;
  };
  gapAnalysis: {
    voteDistribution: { yesPercentage: number; noPercentage: number; yesCount: number; noCount: number };
    betDistribution: { yesPercentage: number; noPercentage: number; yesCount: number; noCount: number };
    gapPercentage: number;
    qualityScore: number;
  };
  filteringEffect: {
    beforeFiltering: { totalCount: number; yesPercentage: number };
    afterFiltering: { totalCount: number; yesPercentage: number };
    filteredCount: number;
    filteredPercentage: number;
  };
  overallQualityScore: number;
}

export default function DataCenterPage() {
  const [dashboardData, setDashboardData] = useState<DashboardData | null>(null);
  const [loading, setLoading] = useState(true);
  const [questionId] = useState(1); // 일단 질문 1번 고정

  useEffect(() => {
    fetchDashboard();
  }, []);

  const fetchDashboard = async () => {
    setLoading(true);
    try {
      const response = await fetch(`${BACKEND_URL}/analytics/dashboard/${questionId}`);
      const data = await response.json();
      setDashboardData(data);
    } catch (error) {
      console.error('Failed to fetch dashboard:', error);
    } finally {
      setLoading(false);
    }
  };

  if (loading || !dashboardData) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-slate-50 to-blue-50 flex items-center justify-center">
        <div className="text-center">
          <Database className="h-16 w-16 text-blue-600 mx-auto mb-4 animate-pulse" />
          <p className="text-xl font-bold text-slate-700">데이터 로딩 중...</p>
        </div>
      </div>
    );
  }

  const { demographics, gapAnalysis, filteringEffect, overallQualityScore } = dashboardData;

  // 투표 vs 베팅 비교 데이터
  const comparisonData = [
    {
      name: '티케터 (투표)',
      YES: gapAnalysis.voteDistribution.yesPercentage,
      NO: gapAnalysis.voteDistribution.noPercentage
    },
    {
      name: '베터 (베팅)',
      YES: gapAnalysis.betDistribution.yesPercentage,
      NO: gapAnalysis.betDistribution.noPercentage
    }
  ];

  // 국가별 데이터
  const countryChartData = demographics.byCountry.map(c => ({
    name: c.countryCode,
    YES: c.yesPercentage,
    NO: 100 - c.yesPercentage,
    total: c.total
  }));

  // 직업별 데이터
  const jobChartData = demographics.byJob.map(j => ({
    name: j.jobCategory,
    YES: j.yesPercentage,
    NO: 100 - j.yesPercentage,
    total: j.total
  }));

  // 연령대별 데이터 (상위 10개만)
  const ageChartData = demographics.byAge.slice(0, 10).map(a => ({
    name: `${a.ageGroup}세`,
    YES: a.yesPercentage,
    total: a.total
  }));

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 to-blue-50 py-8 px-4">
      <div className="max-w-7xl mx-auto">
        
        {/* 헤더 */}
        <div className="mb-8">
          <div className="flex items-center gap-3 mb-2">
            <Database className="h-8 w-8 text-blue-600" />
            <h1 className="text-4xl font-black text-slate-800">데이터 센터</h1>
          </div>
          <p className="text-slate-600 text-lg">고품질 예측 데이터 분석 대시보드</p>
        </div>

        {/* 품질 점수 카드 */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-8">
          <div className="bg-white rounded-xl shadow-lg p-6 border-l-4 border-blue-500">
            <p className="text-sm text-slate-600 mb-1">전체 품질 점수</p>
            <p className="text-3xl font-black text-blue-600">{overallQualityScore.toFixed(1)}점</p>
          </div>
          <div className="bg-white rounded-xl shadow-lg p-6 border-l-4 border-green-500">
            <p className="text-sm text-slate-600 mb-1">총 투표 수</p>
            <p className="text-3xl font-black text-green-600">{demographics.totalVotes.toLocaleString()}</p>
          </div>
          <div className="bg-white rounded-xl shadow-lg p-6 border-l-4 border-purple-500">
            <p className="text-sm text-slate-600 mb-1">괴리율</p>
            <p className="text-3xl font-black text-purple-600">{gapAnalysis.gapPercentage.toFixed(1)}%</p>
          </div>
          <div className="bg-white rounded-xl shadow-lg p-6 border-l-4 border-orange-500">
            <p className="text-sm text-slate-600 mb-1">필터링율</p>
            <p className="text-3xl font-black text-orange-600">{filteringEffect.filteredPercentage.toFixed(1)}%</p>
          </div>
        </div>

        {/* 투표 vs 베팅 괴리율 */}
        <div className="bg-white rounded-xl shadow-lg p-6 mb-8">
          <div className="flex items-center gap-2 mb-4">
            <TrendingUp className="h-6 w-6 text-blue-600" />
            <h2 className="text-2xl font-bold">티케터 vs 베터 괴리율 분석</h2>
          </div>
          <p className="text-sm text-slate-600 mb-4">
            티케터(투표)와 베터(베팅)의 선택 분포를 비교하여 데이터 품질을 검증합니다.
            괴리율이 낮을수록 고품질 데이터입니다.
          </p>
          
          <ResponsiveContainer width="100%" height={300}>
            <BarChart data={comparisonData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="name" />
              <YAxis />
              <Tooltip />
              <Legend />
              <Bar dataKey="YES" fill={COLORS.yes} name="YES %" />
              <Bar dataKey="NO" fill={COLORS.no} name="NO %" />
            </BarChart>
          </ResponsiveContainer>

          <div className={`mt-4 p-4 rounded-lg ${gapAnalysis.gapPercentage < 10 ? 'bg-green-50' : 'bg-red-50'}`}>
            {gapAnalysis.gapPercentage < 10 ? (
              <div className="flex items-center gap-2 text-green-700">
                <CheckCircle size={20} />
                <p className="font-semibold">우수한 데이터 품질! 괴리율 {gapAnalysis.gapPercentage.toFixed(1)}%</p>
              </div>
            ) : (
              <div className="flex items-center gap-2 text-red-700">
                <AlertCircle size={20} />
                <p className="font-semibold">높은 괴리율 감지! {gapAnalysis.gapPercentage.toFixed(1)}% (어뷰징 필터링 필요)</p>
              </div>
            )}
          </div>
        </div>

        {/* 페르소나별 분석 */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 mb-8">
          
          {/* 국가별 분포 */}
          <div className="bg-white rounded-xl shadow-lg p-6">
            <div className="flex items-center gap-2 mb-4">
              <Users className="h-6 w-6 text-blue-600" />
              <h2 className="text-2xl font-bold">국가별 투표 분포</h2>
            </div>
            <ResponsiveContainer width="100%" height={300}>
              <BarChart data={countryChartData}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="name" />
                <YAxis />
                <Tooltip />
                <Legend />
                <Bar dataKey="YES" fill={COLORS.yes} name="YES %" />
              </BarChart>
            </ResponsiveContainer>
          </div>

          {/* 직업별 분포 */}
          <div className="bg-white rounded-xl shadow-lg p-6">
            <div className="flex items-center gap-2 mb-4">
              <Briefcase className="h-6 w-6 text-purple-600" />
              <h2 className="text-2xl font-bold">직업별 투표 분포</h2>
            </div>
            <ResponsiveContainer width="100%" height={300}>
              <BarChart data={jobChartData}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="name" />
                <YAxis />
                <Tooltip />
                <Legend />
                <Bar dataKey="YES" fill={COLORS.secondary} name="YES %" />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* 어뷰징 필터링 효과 */}
        <div className="bg-white rounded-xl shadow-lg p-6">
          <div className="flex items-center gap-2 mb-4">
            <Filter className="h-6 w-6 text-orange-600" />
            <h2 className="text-2xl font-bold">어뷰징 필터링 효과</h2>
          </div>
          <p className="text-sm text-slate-600 mb-4">
            무지성 투표(2초 이하 응답)를 필터링하여 데이터 품질을 향상시킵니다.
          </p>

          <div className="grid grid-cols-2 gap-4 mb-4">
            <div className="p-4 bg-red-50 rounded-lg">
              <p className="text-sm text-slate-600">필터링 전</p>
              <p className="text-2xl font-bold text-red-600">{filteringEffect.beforeFiltering.totalCount.toLocaleString()}개</p>
              <p className="text-sm text-slate-500">YES: {filteringEffect.beforeFiltering.yesPercentage.toFixed(1)}%</p>
            </div>
            <div className="p-4 bg-green-50 rounded-lg">
              <p className="text-sm text-slate-600">필터링 후</p>
              <p className="text-2xl font-bold text-green-600">{filteringEffect.afterFiltering.totalCount.toLocaleString()}개</p>
              <p className="text-sm text-slate-500">YES: {filteringEffect.afterFiltering.yesPercentage.toFixed(1)}%</p>
            </div>
          </div>

          <div className="p-4 bg-blue-50 rounded-lg">
            <p className="text-center text-lg font-semibold text-blue-700">
              🗑️ {filteringEffect.filteredCount.toLocaleString()}개 ({filteringEffect.filteredPercentage.toFixed(1)}%) 제거됨
            </p>
          </div>
        </div>

      </div>
    </div>
  );
}
