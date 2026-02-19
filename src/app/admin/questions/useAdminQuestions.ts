import { useCallback, useEffect, useMemo, useState } from 'react';
import { API_BASE_URL, authFetch } from '@/lib/api';
import type { QuestionAdminView, SortBy, SortOrder } from './types';

interface GeneratorSettingsResponse {
  enabled: boolean;
  intervalSeconds: number;
  categories?: string[];
  isDemoMode?: boolean;
}

interface ApiResult {
  success?: boolean;
  message?: string;
}

export function useAdminQuestions(isAdmin: boolean) {
  const [questions, setQuestions] = useState<QuestionAdminView[]>([]);
  const [loading, setLoading] = useState(false);
  const [showCreateForm, setShowCreateForm] = useState(false);

  const [settleTarget, setSettleTarget] = useState<QuestionAdminView | null>(null);
  const [settleFinalResult, setSettleFinalResult] = useState<'YES' | 'NO'>('YES');
  const [settleSourceUrl, setSettleSourceUrl] = useState('');

  const [title, setTitle] = useState('');
  const [category, setCategory] = useState('ECONOMY');
  const [questionType, setQuestionType] = useState<'VERIFIABLE' | 'OPINION'>('VERIFIABLE');
  const [votingDuration, setVotingDuration] = useState(3600);
  const [bettingDuration, setBettingDuration] = useState(3600);

  const [generatorEnabled, setGeneratorEnabled] = useState(false);
  const [generatorInterval, setGeneratorInterval] = useState(3600);
  const [generatorCategories, setGeneratorCategories] = useState<string[]>([]);
  const [generatorLoading, setGeneratorLoading] = useState(false);
  const [generating, setGenerating] = useState(false);
  const [isDemoMode, setIsDemoMode] = useState(false);

  const [sortBy, setSortBy] = useState<SortBy>('id');
  const [sortOrder, setSortOrder] = useState<SortOrder>('desc');

  const fetchGeneratorSettings = useCallback(async () => {
    try {
      const response = await authFetch(`${API_BASE_URL}/admin/settings/question-generator`);
      const data = (await response.json()) as GeneratorSettingsResponse;
      setGeneratorEnabled(data.enabled);
      setGeneratorInterval(data.intervalSeconds);
      setGeneratorCategories(data.categories || []);
      setIsDemoMode(Boolean(data.isDemoMode));
    } catch (error) {
      if (process.env.NODE_ENV === 'development') {
        console.error('Failed to fetch generator settings:', error);
      }
    }
  }, []);

  const fetchQuestions = useCallback(async () => {
    setLoading(true);
    try {
      const response = await authFetch(`${API_BASE_URL}/admin/questions?page=0&size=1000`);
      const data = (await response.json()) as QuestionAdminView[] | { content?: QuestionAdminView[] };
      const questionList = Array.isArray(data) ? data : (data.content ?? []);
      setQuestions(questionList);
    } catch (error) {
      if (process.env.NODE_ENV === 'development') {
        console.error('Failed to fetch questions:', error);
      }
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (isAdmin) {
      void fetchQuestions();
      void fetchGeneratorSettings();
    }
  }, [isAdmin, fetchGeneratorSettings, fetchQuestions]);

  const sortedQuestions = useMemo(() => {
    return [...questions].sort((a, b) => {
      let aVal: string | number = a[sortBy];
      let bVal: string | number = b[sortBy];

      if (sortBy === 'expiredAt') {
        aVal = new Date(a.expiredAt).getTime();
        bVal = new Date(b.expiredAt).getTime();
      }

      if (typeof aVal === 'string' && typeof bVal === 'string') {
        return sortOrder === 'asc' ? aVal.localeCompare(bVal) : bVal.localeCompare(aVal);
      }

      return sortOrder === 'asc' ? (aVal as number) - (bVal as number) : (bVal as number) - (aVal as number);
    });
  }, [questions, sortBy, sortOrder]);

  const handleSort = (column: SortBy) => {
    if (sortBy === column) {
      setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc');
    } else {
      setSortBy(column);
      setSortOrder('desc');
    }
  };

  const updateGeneratorSettings = async (updates: {
    enabled?: boolean;
    intervalSeconds?: number;
    categories?: string[];
  }) => {
    setGeneratorLoading(true);
    try {
      const response = await authFetch(`${API_BASE_URL}/admin/settings/question-generator`, {
        method: 'PUT',
        body: JSON.stringify(updates),
      });
      const data = (await response.json()) as GeneratorSettingsResponse;
      setGeneratorEnabled(data.enabled);
      setGeneratorInterval(data.intervalSeconds);
      setGeneratorCategories(data.categories || []);
    } catch (error) {
      if (process.env.NODE_ENV === 'development') {
        console.error('Failed to update generator settings:', error);
      }
      alert('Failed to update settings.');
    } finally {
      setGeneratorLoading(false);
    }
  };

  const handleGenerateQuestion = async () => {
    setGenerating(true);
    try {
      const response = await authFetch(`${API_BASE_URL}/admin/questions/generate`, {
        method: 'POST',
      });
      const data = (await response.json()) as ApiResult;
      if (data.success) {
        alert('✅ Question generated successfully!');
        void fetchQuestions();
      } else {
        alert(`❌ Generation failed: ${data.message}`);
      }
    } catch (error) {
      if (process.env.NODE_ENV === 'development') {
        console.error('Failed to generate question:', error);
      }
      alert('❌ An error occurred while generating question.');
    } finally {
      setGenerating(false);
    }
  };

  const handleToggleGenerator = async (enabled: boolean) => {
    setGeneratorEnabled(enabled);
    await updateGeneratorSettings({ enabled });
  };

  const handleIntervalChange = async (intervalSeconds: number) => {
    setGeneratorInterval(intervalSeconds);
    await updateGeneratorSettings({ intervalSeconds });
  };

  const handleCategoryToggle = async (cat: string) => {
    const newCategories = generatorCategories.includes(cat)
      ? generatorCategories.filter((c) => c !== cat)
      : [...generatorCategories, cat];
    setGeneratorCategories(newCategories);
    await updateGeneratorSettings({ categories: newCategories });
  };

  const handleCreate = async () => {
    if (!title) {
      alert('Please enter a title.');
      return;
    }

    setLoading(true);
    try {
      const response = await authFetch(`${API_BASE_URL}/admin/questions`, {
        method: 'POST',
        body: JSON.stringify({
          title,
          type: questionType,
          marketType: questionType,
          resolutionRule: questionType === 'OPINION'
            ? 'Settlement by majority vote of market participants'
            : 'Manual settlement based on admin verified data',
          category,
          votingDuration,
          bettingDuration,
          executionModel: 'AMM_FPMM',
          seedUsdc: 1000,
          feeRate: 0.01,
        }),
      });

      const data = (await response.json()) as ApiResult;
      if (data.success) {
        alert('Question created successfully!');
        setTitle('');
        setShowCreateForm(false);
        void fetchQuestions();
      } else {
        alert(`Creation failed: ${data.message}`);
      }
    } catch (error) {
      if (process.env.NODE_ENV === 'development') {
        console.error('Failed to create question:', error);
      }
      alert('An error occurred while creating question.');
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm('Are you sure you want to delete this question?')) return;

    try {
      const response = await authFetch(`${API_BASE_URL}/admin/questions/${id}`, {
        method: 'DELETE',
      });
      const data = (await response.json()) as ApiResult;

      if (data.success) {
        alert('✅ Question deleted successfully!');
        void fetchQuestions();
      } else {
        alert(`❌ Delete failed: ${data.message}`);
      }
    } catch (error) {
      if (process.env.NODE_ENV === 'development') {
        console.error('Failed to delete question:', error);
      }
      alert('❌ An error occurred while deleting question.');
    }
  };

  const openSettleModal = (question: QuestionAdminView) => {
    setSettleTarget(question);
    setSettleFinalResult('YES');
    setSettleSourceUrl('');
  };

  const closeSettleModal = () => {
    setSettleTarget(null);
  };

  const handleSettle = async () => {
    if (!settleTarget) return;

    setLoading(true);
    try {
      const response = await authFetch(`${API_BASE_URL}/admin/settlements/questions/${settleTarget.id}/settle`, {
        method: 'POST',
        body: JSON.stringify({
          finalResult: settleFinalResult,
          sourceUrl: settleSourceUrl || null,
        }),
      });
      const data = (await response.json()) as ApiResult;
      if (response.ok) {
        alert(`Settlement started: ${data.message}`);
        closeSettleModal();
        setSettleSourceUrl('');
        void fetchQuestions();
      } else {
        alert(`Settlement failed: ${data.message || 'Error occurred'}`);
      }
    } catch (error) {
      if (process.env.NODE_ENV === 'development') {
        console.error('Settlement failed:', error);
      }
      alert('An error occurred during settlement.');
    } finally {
      setLoading(false);
    }
  };

  const handleFinalize = async (id: number) => {
    if (!confirm('Do you want to finalize settlement? Dividends will be distributed.')) return;

    try {
      const response = await authFetch(`${API_BASE_URL}/admin/settlements/questions/${id}/finalize`, {
        method: 'POST',
        body: JSON.stringify({ force: true }),
      });
      const data = (await response.json()) as ApiResult;

      if (response.ok) {
        alert(`Settlement finalized: ${data.message}`);
        void fetchQuestions();
      } else {
        alert(`Finalization failed: ${data.message || 'Error occurred'}`);
      }
    } catch (error) {
      if (process.env.NODE_ENV === 'development') {
        console.error('Finalize failed:', error);
      }
      alert('An error occurred during finalization.');
    }
  };

  const handleCancelSettle = async (id: number) => {
    if (!confirm('Do you want to cancel settlement? The question will return to OPEN status.')) return;

    try {
      const response = await authFetch(`${API_BASE_URL}/admin/settlements/questions/${id}/cancel`, {
        method: 'POST',
      });
      const data = (await response.json()) as ApiResult;

      if (response.ok) {
        alert(`Settlement cancelled: ${data.message}`);
        void fetchQuestions();
      } else {
        alert(`Cancellation failed: ${data.message || 'Error occurred'}`);
      }
    } catch (error) {
      if (process.env.NODE_ENV === 'development') {
        console.error('Cancel failed:', error);
      }
      alert('An error occurred during cancellation.');
    }
  };

  return {
    questions,
    sortedQuestions,
    loading,
    showCreateForm,
    setShowCreateForm,
    settleTarget,
    settleFinalResult,
    setSettleFinalResult,
    settleSourceUrl,
    setSettleSourceUrl,
    title,
    setTitle,
    category,
    setCategory,
    questionType,
    setQuestionType,
    votingDuration,
    setVotingDuration,
    bettingDuration,
    setBettingDuration,
    generatorEnabled,
    generatorInterval,
    generatorCategories,
    generatorLoading,
    generating,
    isDemoMode,
    sortBy,
    sortOrder,
    handleSort,
    handleGenerateQuestion,
    handleToggleGenerator,
    handleIntervalChange,
    handleCategoryToggle,
    handleCreate,
    handleDelete,
    openSettleModal,
    closeSettleModal,
    handleSettle,
    handleFinalize,
    handleCancelSettle,
  };
}
