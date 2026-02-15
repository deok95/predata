'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { Loader2 } from 'lucide-react';
import LiveMatchesDashboard from '@/components/LiveMatchesDashboard';
import MainLayout from '@/components/layout/MainLayout';
import { useTheme } from '@/hooks/useTheme';
import { useAuth } from '@/hooks/useAuth';
import { GENERATOR_INTERVAL_OPTIONS, MARKET_DURATION_OPTIONS, QUESTION_CATEGORIES } from './constants';
import { useAdminQuestions } from './useAdminQuestions';
import AccessDenied from './components/AccessDenied';
import AdminHeader from './components/AdminHeader';
import AutoGeneratorPanel from './components/AutoGeneratorPanel';
import CreateQuestionForm from './components/CreateQuestionForm';
import QuestionStats from './components/QuestionStats';
import QuestionsTable from './components/QuestionsTable';
import SettlementModal from './components/SettlementModal';

function AdminQuestionContent() {
  const { isDark } = useTheme();
  const { user, isAuthenticated, isLoading: authLoading } = useAuth();
  const router = useRouter();
  const isAdmin = isAuthenticated && user?.role === 'ADMIN';

  const state = useAdminQuestions(Boolean(isAdmin));

  useEffect(() => {
    if (!authLoading && !isAuthenticated) {
      router.push('/');
    }
  }, [authLoading, isAuthenticated, router]);

  if (authLoading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <Loader2 className="w-8 h-8 animate-spin text-indigo-500" />
      </div>
    );
  }

  if (!isAuthenticated) {
    return null;
  }

  if (!isAdmin) {
    return <AccessDenied isDark={isDark} onGoHome={() => router.push('/')} />;
  }

  return (
    <div className="max-w-7xl mx-auto">
      <AdminHeader
        isDark={isDark}
        showCreateForm={state.showCreateForm}
        onToggleCreateForm={() => state.setShowCreateForm(!state.showCreateForm)}
      />

      <div className="mb-8">
        <LiveMatchesDashboard />
      </div>

      <AutoGeneratorPanel
        isDark={isDark}
        isDemoMode={state.isDemoMode}
        generating={state.generating}
        generatorEnabled={state.generatorEnabled}
        generatorLoading={state.generatorLoading}
        generatorInterval={state.generatorInterval}
        generatorCategories={state.generatorCategories}
        categories={QUESTION_CATEGORIES}
        intervalOptions={GENERATOR_INTERVAL_OPTIONS}
        onGenerateNow={state.handleGenerateQuestion}
        onToggleGenerator={() => state.handleToggleGenerator(!state.generatorEnabled)}
        onChangeInterval={state.handleIntervalChange}
        onToggleCategory={state.handleCategoryToggle}
      />

      {state.showCreateForm && (
        <CreateQuestionForm
          isDark={isDark}
          categories={QUESTION_CATEGORIES}
          durationOptions={MARKET_DURATION_OPTIONS}
          title={state.title}
          category={state.category}
          questionType={state.questionType}
          votingDuration={state.votingDuration}
          bettingDuration={state.bettingDuration}
          loading={state.loading}
          onChangeTitle={state.setTitle}
          onChangeCategory={state.setCategory}
          onChangeQuestionType={state.setQuestionType}
          onChangeVotingDuration={state.setVotingDuration}
          onChangeBettingDuration={state.setBettingDuration}
          onCancel={() => state.setShowCreateForm(false)}
          onCreate={state.handleCreate}
        />
      )}

      <QuestionStats isDark={isDark} questions={state.questions} />

      <QuestionsTable
        isDark={isDark}
        questions={state.sortedQuestions}
        sortBy={state.sortBy}
        sortOrder={state.sortOrder}
        onSort={state.handleSort}
        onOpenQuestion={(id) => {
          window.location.href = `/question/${id}`;
        }}
        onOpenSettle={state.openSettleModal}
        onFinalize={state.handleFinalize}
        onCancelSettle={state.handleCancelSettle}
        onDelete={state.handleDelete}
      />

      <SettlementModal
        isDark={isDark}
        target={state.settleTarget}
        finalResult={state.settleFinalResult}
        sourceUrl={state.settleSourceUrl}
        loading={state.loading}
        onClose={state.closeSettleModal}
        onChangeFinalResult={state.setSettleFinalResult}
        onChangeSourceUrl={state.setSettleSourceUrl}
        onSubmit={state.handleSettle}
      />
    </div>
  );
}

export default function AdminQuestionManagement() {
  return (
    <MainLayout>
      <AdminQuestionContent />
    </MainLayout>
  );
}
