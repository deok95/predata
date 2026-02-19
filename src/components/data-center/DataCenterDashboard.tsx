'use client';

import { useEffect, useMemo } from 'react';
import { useRouter } from 'next/navigation';
import { Database, ArrowLeft } from 'lucide-react';
import { useTheme } from '@/hooks/useTheme';
import { useAuth } from '@/hooks/useAuth';
import { useQualityDashboard, useQuestionActivities, useQuestion, useQuestionSwapHistory } from '@/hooks/useQueries';
import QualityScoreCards from './QualityScoreCards';
import VoteDistributionChart from './VoteDistributionChart';
import VoteBetGapChart from './VoteBetGapChart';
import VoteTrendChart from './VoteTrendChart';
import DemographicsSection from './DemographicsSection';
import BettingOverviewPanel from './BettingOverviewPanel';
import BettingTrendChart from './BettingTrendChart';
import FilteringEffectPanel from './FilteringEffectPanel';
import type { DistributionData, VoteBetGapReport } from '@/types/api';

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
  const { data: swapHistory = [] } = useQuestionSwapHistory(questionId);

  const realtimeMetrics = useMemo(() => {
    const voteActivities = activities.filter((a) => a.activityType === 'VOTE');
    const betActivities = activities.filter((a) => a.activityType === 'BET');
    const useSwapAsPrimary = question?.executionModel === 'AMM_FPMM' && swapHistory.length > 0;
    const buySwaps = swapHistory.filter((s) => s.action === 'BUY');

    const uniqueVotePeople = new Set(voteActivities.map((a) => a.memberId)).size;

    const voteYesCount = voteActivities.filter((a) => a.choice === 'YES').length;
    const voteNoCount = voteActivities.length - voteYesCount;
    const voteTotal = voteYesCount + voteNoCount;

    const voteDistribution: DistributionData = {
      yesCount: voteYesCount,
      noCount: voteNoCount,
      yesPercentage: voteTotal > 0 ? (voteYesCount / voteTotal) * 100 : 0,
      noPercentage: voteTotal > 0 ? (voteNoCount / voteTotal) * 100 : 0,
    };

    const betYesAmount = useSwapAsPrimary
      ? buySwaps.filter((s) => s.outcome === 'YES').reduce((sum, s) => sum + s.usdcAmount, 0)
      : betActivities.filter((a) => a.choice === 'YES').reduce((sum, a) => sum + a.amount, 0);
    const betNoAmount = useSwapAsPrimary
      ? buySwaps.filter((s) => s.outcome === 'NO').reduce((sum, s) => sum + s.usdcAmount, 0)
      : betActivities.filter((a) => a.choice === 'NO').reduce((sum, a) => sum + a.amount, 0);
    const betTotalAmount = betYesAmount + betNoAmount;

    const betYesCount = useSwapAsPrimary
      ? buySwaps.filter((s) => s.outcome === 'YES').length
      : betActivities.filter((a) => a.choice === 'YES').length;
    const betNoCount = useSwapAsPrimary
      ? buySwaps.filter((s) => s.outcome === 'NO').length
      : betActivities.filter((a) => a.choice === 'NO').length;

    const betDistribution: DistributionData = {
      yesCount: betYesCount,
      noCount: betNoCount,
      yesPercentage: betTotalAmount > 0 ? (betYesAmount / betTotalAmount) * 100 : 0,
      noPercentage: betTotalAmount > 0 ? (betNoAmount / betTotalAmount) * 100 : 0,
    };

    const gapPercentage = Math.abs(voteDistribution.yesPercentage - betDistribution.yesPercentage);

    const parsedPools = {
      totalPool: betTotalAmount,
      yesPool: betYesAmount,
      noPool: betNoAmount,
    };

    const gapReport: VoteBetGapReport = {
      questionId,
      voteDistribution,
      betDistribution,
      gapPercentage,
      qualityScore: dashboardData?.gapAnalysis.qualityScore ?? 0,
    };

    return {
      votePeople: uniqueVotePeople,
      voteDistribution,
      gapReport,
      parsedPools,
      effectiveBetActivities: useSwapAsPrimary
        ? buySwaps.map((s, idx) => ({
            id: s.swapId ?? idx,
            memberId: s.memberId,
            questionId,
            activityType: 'BET' as const,
            choice: s.outcome,
            amount: s.usdcAmount,
            createdAt: s.createdAt,
          }))
        : betActivities,
    };
  }, [activities, question?.executionModel, questionId, dashboardData?.gapAnalysis.qualityScore, swapHistory]);

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
  const effectiveVoteDistribution =
    realtimeMetrics.votePeople > 0 ? realtimeMetrics.voteDistribution : gapAnalysis.voteDistribution;
  const effectiveGapAnalysis =
    realtimeMetrics.votePeople > 0 ? realtimeMetrics.gapReport : gapAnalysis;
  const effectiveVotePeople =
    realtimeMetrics.votePeople > 0 ? realtimeMetrics.votePeople : demographics.totalVotes;

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
          totalVotes={effectiveVotePeople}
          gapPercentage={effectiveGapAnalysis.gapPercentage}
          filteredPercentage={filteringEffect.filteredPercentage}
          isDark={isDark}
        />
      </div>

      {/* Vote Distribution + Gap Analysis */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4 mb-6">
        <VoteDistributionChart
          voteDistribution={effectiveVoteDistribution}
          isDark={isDark}
        />
        <VoteBetGapChart
          gapAnalysis={effectiveGapAnalysis}
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
          betDistribution={effectiveGapAnalysis.betDistribution}
          activities={realtimeMetrics.effectiveBetActivities}
          totalVotes={effectiveVotePeople}
          parsedPools={realtimeMetrics.parsedPools}
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
