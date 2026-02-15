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
      alert('설정 업데이트에 실패했습니다.');
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
        alert('✅ 질문이 생성되었습니다!');
        void fetchQuestions();
      } else {
        alert(`❌ 생성 실패: ${data.message}`);
      }
    } catch (error) {
      if (process.env.NODE_ENV === 'development') {
        console.error('Failed to generate question:', error);
      }
      alert('❌ 질문 생성 중 오류가 발생했습니다.');
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
      alert('제목을 입력해주세요.');
      return;
    }

    setLoading(true);
    try {
      const response = await authFetch(`${API_BASE_URL}/admin/questions`, {
        method: 'POST',
        body: JSON.stringify({
          title,
          type: questionType,
          category,
          votingDuration,
          bettingDuration,
        }),
      });

      const data = (await response.json()) as ApiResult;
      if (data.success) {
        alert('질문이 생성되었습니다!');
        setTitle('');
        setShowCreateForm(false);
        void fetchQuestions();
      } else {
        alert(`생성 실패: ${data.message}`);
      }
    } catch (error) {
      if (process.env.NODE_ENV === 'development') {
        console.error('Failed to create question:', error);
      }
      alert('질문 생성 중 오류가 발생했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm('정말 이 질문을 삭제하시겠습니까?')) return;

    try {
      const response = await authFetch(`${API_BASE_URL}/admin/questions/${id}`, {
        method: 'DELETE',
      });
      const data = (await response.json()) as ApiResult;

      if (data.success) {
        alert('✅ 질문이 삭제되었습니다!');
        void fetchQuestions();
      } else {
        alert(`❌ 삭제 실패: ${data.message}`);
      }
    } catch (error) {
      if (process.env.NODE_ENV === 'development') {
        console.error('Failed to delete question:', error);
      }
      alert('❌ 질문 삭제 중 오류가 발생했습니다.');
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
        alert(`정산 시작: ${data.message}`);
        closeSettleModal();
        setSettleSourceUrl('');
        void fetchQuestions();
      } else {
        alert(`정산 실패: ${data.message || '오류 발생'}`);
      }
    } catch (error) {
      if (process.env.NODE_ENV === 'development') {
        console.error('Settlement failed:', error);
      }
      alert('정산 중 오류가 발생했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const handleFinalize = async (id: number) => {
    if (!confirm('정산을 확정하시겠습니까? 배당금이 분배됩니다.')) return;

    try {
      const response = await authFetch(`${API_BASE_URL}/admin/settlements/questions/${id}/finalize`, {
        method: 'POST',
        body: JSON.stringify({ force: true }),
      });
      const data = (await response.json()) as ApiResult;

      if (response.ok) {
        alert(`정산 확정: ${data.message}`);
        void fetchQuestions();
      } else {
        alert(`확정 실패: ${data.message || '오류 발생'}`);
      }
    } catch (error) {
      if (process.env.NODE_ENV === 'development') {
        console.error('Finalize failed:', error);
      }
      alert('확정 중 오류가 발생했습니다.');
    }
  };

  const handleCancelSettle = async (id: number) => {
    if (!confirm('정산을 취소하시겠습니까? 질문이 OPEN 상태로 돌아갑니다.')) return;

    try {
      const response = await authFetch(`${API_BASE_URL}/admin/settlements/questions/${id}/cancel`, {
        method: 'POST',
      });
      const data = (await response.json()) as ApiResult;

      if (response.ok) {
        alert(`정산 취소: ${data.message}`);
        void fetchQuestions();
      } else {
        alert(`취소 실패: ${data.message || '오류 발생'}`);
      }
    } catch (error) {
      if (process.env.NODE_ENV === 'development') {
        console.error('Cancel failed:', error);
      }
      alert('취소 중 오류가 발생했습니다.');
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
