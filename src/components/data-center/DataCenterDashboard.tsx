'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { Database, ArrowLeft } from 'lucide-react';
import { useTheme } from '@/hooks/useTheme';
import { useAuth } from '@/hooks/useAuth';
import { useQualityDashboard, useQuestionActivities, useQuestion } from '@/hooks/useQueries';
import QualityScoreCards from './QualityScoreCards';
import VoteDistributionChart from './VoteDistributionChart';
import VoteBetGapChart from './VoteBetGapChart';
import VoteTrendChart from './VoteTrendChart';
import DemographicsSection from './DemographicsSection';
import BettingOverviewPanel from './BettingOverviewPanel';
import BettingTrendChart from './BettingTrendChart';
import FilteringEffectPanel from './FilteringEffectPanel';

interface DataCenterDashboardProps {
  questionId: number;
}

export default function DataCenterDashboard({ questionId }: DataCenterDashboardProps) {
  const { isDark } = useTheme();
  const { user } = useAuth();
  const router = useRouter();

  const { data: dashboardData, isLoading: dashboardLoading } = useQualityDashboard(questionId);
  const { data: activities = [] } = useQuestionActivities(questionId);
  const { data: question } = useQuestion(questionId);

  // Admin access check
  useEffect(() => {
    if (user && user.role !== 'ADMIN') {
      router.push('/');
    }
  }, [user, router]);

  const backButton = (
    <button
      onClick={() => router.push('/data-center')}
      className={`flex items-center gap-2 text-sm font-bold mb-4 transition-colors ${
        isDark ? 'text-slate-400 hover:text-white' : 'text-slate-500 hover:text-slate-900'
      }`}
    >
      <ArrowLeft size={16} />
      Back to Data Center
    </button>
  );

  if (dashboardLoading) {
    return (
      <div className="max-w-7xl mx-auto animate-fade-in">
        <div className="mb-8">
          {backButton}
          <div className="flex items-center gap-3 mb-2">
            <Database className="h-8 w-8 text-indigo-600" />
            <h1 className={`text-3xl font-black ${isDark ? 'text-white' : 'text-slate-900'}`}>Question Analysis</h1>
          </div>
        </div>
        <div className="space-y-4">
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            {[...Array(4)].map((_, i) => (
              <div key={i} className={`h-32 rounded-3xl animate-pulse ${isDark ? 'bg-slate-800' : 'bg-slate-200'}`} />
            ))}
          </div>
          <div className={`h-80 rounded-3xl animate-pulse ${isDark ? 'bg-slate-800' : 'bg-slate-200'}`} />
        </div>
      </div>
    );
  }

  if (!dashboardData) {
    return (
      <div className="max-w-7xl mx-auto animate-fade-in">
        {backButton}
        <div className={`p-8 rounded-[2.5rem] border text-center ${isDark ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-100'}`}>
          <Database className="h-12 w-12 text-slate-400 mx-auto mb-4" />
          <p className={`text-lg font-black ${isDark ? 'text-white' : 'text-slate-900'}`}>Unable to load data</p>
        </div>
      </div>
    );
  }

  const { demographics, gapAnalysis, filteringEffect, overallQualityScore } = dashboardData;

  return (
    <div className="max-w-7xl mx-auto animate-fade-in">
      {/* Back button + Header */}
      <div className="mb-6">
        {backButton}
        <div className="flex items-center gap-3 mb-2">
          <Database className="h-8 w-8 text-indigo-600" />
          <h1 className={`text-3xl font-black ${isDark ? 'text-white' : 'text-slate-900'}`}>Question Analysis</h1>
        </div>
        {question && (
          <p className={`text-base font-bold mt-1 ${isDark ? 'text-slate-300' : 'text-slate-700'}`}>
            {question.title}
          </p>
        )}
      </div>

      {/* Quality Score Cards */}
      <div className="mb-6">
        <QualityScoreCards
          overallQualityScore={overallQualityScore}
          totalVotes={demographics.totalVotes}
          gapPercentage={gapAnalysis.gapPercentage}
          filteredPercentage={filteringEffect.filteredPercentage}
          isDark={isDark}
        />
      </div>

      {/* Vote Distribution + Gap Analysis */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4 mb-6">
        <VoteDistributionChart
          voteDistribution={gapAnalysis.voteDistribution}
          isDark={isDark}
        />
        <VoteBetGapChart
          gapAnalysis={gapAnalysis}
          isDark={isDark}
        />
      </div>

      {/* Vote Trend (Time-Series) */}
      <div className="mb-6">
        <VoteTrendChart
          activities={activities}
          isDark={isDark}
        />
      </div>

      {/* Demographics (3-column donuts) */}
      <div className="mb-6">
        <DemographicsSection
          demographics={demographics}
          isDark={isDark}
        />
      </div>

      {/* Betting Trend Chart */}
      <div className="mb-6">
        <BettingTrendChart
          activities={activities}
          isDark={isDark}
        />
      </div>

      {/* Betting Overview + Filtering Effect */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <BettingOverviewPanel
          question={question ?? null}
          betDistribution={gapAnalysis.betDistribution}
          activities={activities}
          totalVotes={demographics.totalVotes}
          isDark={isDark}
        />
        <FilteringEffectPanel
          filteringEffect={filteringEffect}
          isDark={isDark}
        />
      </div>
    </div>
  );
}
