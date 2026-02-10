'use client';

import { useState, useEffect, useMemo } from 'react';
import { useRouter } from 'next/navigation';
import { Plus, Trash2, TrendingUp, Gavel, X, AlertTriangle, Loader2, Sparkles, Settings, ChevronUp, ChevronDown, ChevronsUpDown } from 'lucide-react';
import LiveMatchesDashboard from '@/components/LiveMatchesDashboard';
import MainLayout from '@/components/layout/MainLayout';
import { useTheme } from '@/hooks/useTheme';
import { useAuth } from '@/hooks/useAuth';
import { API_BASE_URL } from '@/lib/api';

interface QuestionAdminView {
  id: number;
  title: string;
  category: string;
  status: string;
  totalBetPool: number;
  totalVotes: number;
  expiredAt: string;
  createdAt: string;
  disputeDeadline?: string;
}

function AdminQuestionContent() {
  const { isDark } = useTheme();
  const { user, isAuthenticated, isLoading: authLoading } = useAuth();
  const router = useRouter();
  const [questions, setQuestions] = useState<QuestionAdminView[]>([]);
  const [loading, setLoading] = useState(false);
  const [showCreateForm, setShowCreateForm] = useState(false);

  // 정산 모달
  const [settleTarget, setSettleTarget] = useState<QuestionAdminView | null>(null);
  const [settleFinalResult, setSettleFinalResult] = useState<'YES' | 'NO'>('YES');
  const [settleSourceUrl, setSettleSourceUrl] = useState('');

  // 폼 데이터
  const [title, setTitle] = useState('');
  const [category, setCategory] = useState('ECONOMY');
  const [expiredAt, setExpiredAt] = useState('');

  // 자동 질문 생성기 설정
  const [generatorEnabled, setGeneratorEnabled] = useState(false);
  const [generatorInterval, setGeneratorInterval] = useState(3600);
  const [generatorCategories, setGeneratorCategories] = useState<string[]>([]);
  const [generatorLoading, setGeneratorLoading] = useState(false);
  const [generating, setGenerating] = useState(false);
  const [isDemoMode, setIsDemoMode] = useState(false);

  // 정렬 상태
  const [sortBy, setSortBy] = useState<'id' | 'title' | 'category' | 'status' | 'totalBetPool' | 'expiredAt'>('id');
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>('desc');

  const categories = ['ECONOMY', 'SPORTS', 'POLITICS', 'TECH', 'CULTURE'];
  const intervalOptions = [
    { value: 1800, label: '30분' },
    { value: 3600, label: '1시간' },
    { value: 7200, label: '2시간' },
    { value: 21600, label: '6시간' },
  ];

  // 비로그인 → 홈으로 리다이렉트
  useEffect(() => {
    if (!authLoading && !isAuthenticated) {
      router.push('/');
    }
  }, [authLoading, isAuthenticated, router]);

  useEffect(() => {
    if (isAuthenticated && user?.role === 'ADMIN') {
      fetchQuestions();
      fetchGeneratorSettings();
    }
  }, [isAuthenticated, user?.role]);

  // 정렬된 질문 목록
  const sortedQuestions = useMemo(() => {
    return [...questions].sort((a, b) => {
      let aVal: string | number = a[sortBy];
      let bVal: string | number = b[sortBy];

      // 날짜 필드는 Date로 변환
      if (sortBy === 'expiredAt') {
        aVal = new Date(a.expiredAt).getTime();
        bVal = new Date(b.expiredAt).getTime();
      }

      // 문자열 비교
      if (typeof aVal === 'string' && typeof bVal === 'string') {
        return sortOrder === 'asc'
          ? aVal.localeCompare(bVal)
          : bVal.localeCompare(aVal);
      }

      // 숫자 비교
      if (sortOrder === 'asc') {
        return (aVal as number) - (bVal as number);
      }
      return (bVal as number) - (aVal as number);
    });
  }, [questions, sortBy, sortOrder]);

  // 정렬 핸들러
  const handleSort = (column: typeof sortBy) => {
    if (sortBy === column) {
      setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc');
    } else {
      setSortBy(column);
      setSortOrder('desc');
    }
  };

  // 정렬 아이콘 렌더링
  const SortIcon = ({ column }: { column: typeof sortBy }) => {
    if (sortBy !== column) {
      return <ChevronsUpDown size={14} className="opacity-40" />;
    }
    return sortOrder === 'asc'
      ? <ChevronUp size={14} />
      : <ChevronDown size={14} />;
  };

  const fetchGeneratorSettings = async () => {
    try {
      const response = await fetch(`${API_BASE_URL}/admin/settings/question-generator`);
      const data = await response.json();
      setGeneratorEnabled(data.enabled);
      setGeneratorInterval(data.intervalSeconds);
      setGeneratorCategories(data.categories || []);
      setIsDemoMode(data.isDemoMode || false);
    } catch (error) {
      if (process.env.NODE_ENV === 'development') {
        console.error('Failed to fetch generator settings:', error);
      }
    }
  };

  const updateGeneratorSettings = async (updates: {
    enabled?: boolean;
    intervalSeconds?: number;
    categories?: string[];
  }) => {
    setGeneratorLoading(true);
    try {
      const response = await fetch(`${API_BASE_URL}/admin/settings/question-generator`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(updates),
      });
      const data = await response.json();
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
      const response = await fetch(`${API_BASE_URL}/admin/questions/generate`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
      });
      const data = await response.json();
      if (data.success) {
        alert('✅ 질문이 생성되었습니다!');
        fetchQuestions();
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
      ? generatorCategories.filter(c => c !== cat)
      : [...generatorCategories, cat];
    setGeneratorCategories(newCategories);
    await updateGeneratorSettings({ categories: newCategories });
  };

  const fetchQuestions = async () => {
    setLoading(true);
    try {
      const response = await fetch(`${API_BASE_URL}/admin/questions`);
      const data = await response.json();
      setQuestions(Array.isArray(data) ? data : []);
    } catch (error) {
      if (process.env.NODE_ENV === 'development') {
        console.error('Failed to fetch questions:', error);
      }
    } finally {
      setLoading(false);
    }
  };

  const handleCreate = async () => {
    if (!title || !expiredAt) {
      alert('제목과 마감일을 입력해주세요.');
      return;
    }

    setLoading(true);
    try {
      const response = await fetch(`${API_BASE_URL}/admin/questions`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          title,
          category,
          expiredAt: new Date(expiredAt).toISOString(),
          categoryWeight: 1.0
        })
      });

      const data = await response.json();

      if (data.success) {
        alert('✅ 질문이 생성되었습니다!');
        setTitle('');
        setExpiredAt('');
        setShowCreateForm(false);
        fetchQuestions();
      } else {
        alert(`❌ 생성 실패: ${data.message}`);
      }
    } catch (error) {
      if (process.env.NODE_ENV === 'development') {
        console.error('Failed to create question:', error);
      }
      alert('❌ 질문 생성 중 오류가 발생했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm('정말 이 질문을 삭제하시겠습니까?')) return;

    try {
      const response = await fetch(`${API_BASE_URL}/admin/questions/${id}`, {
        method: 'DELETE'
      });
      const data = await response.json();

      if (data.success) {
        alert('✅ 질문이 삭제되었습니다!');
        fetchQuestions();
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

  const handleSettle = async () => {
    if (!settleTarget) return;
    setLoading(true);
    try {
      const response = await fetch(`${API_BASE_URL}/questions/${settleTarget.id}/settle`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          finalResult: settleFinalResult,
          sourceUrl: settleSourceUrl || null,
        }),
      });
      const data = await response.json();
      if (response.ok) {
        alert(`정산 시작: ${data.message}`);
        setSettleTarget(null);
        setSettleSourceUrl('');
        fetchQuestions();
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
      const response = await fetch(`${API_BASE_URL}/questions/${id}/settle/finalize`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ force: true }),
      });
      const data = await response.json();
      if (response.ok) {
        alert(`정산 확정: ${data.message}`);
        fetchQuestions();
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
      const response = await fetch(`${API_BASE_URL}/questions/${id}/settle/cancel`, {
        method: 'POST',
      });
      const data = await response.json();
      if (response.ok) {
        alert(`정산 취소: ${data.message}`);
        fetchQuestions();
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

  // 로딩 중
  if (authLoading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <Loader2 className="w-8 h-8 animate-spin text-indigo-500" />
      </div>
    );
  }

  // 비로그인 (리다이렉트 중)
  if (!isAuthenticated) {
    return null;
  }

  // Admin이 아닌 경우
  if (user?.role !== 'ADMIN') {
    return (
      <div className={`flex flex-col items-center justify-center min-h-[60vh] gap-4 ${isDark ? 'text-white' : 'text-slate-800'}`}>
        <AlertTriangle className="w-16 h-16 text-amber-500" />
        <h1 className="text-2xl font-bold">접근 권한이 없습니다</h1>
        <p className={isDark ? 'text-slate-400' : 'text-slate-500'}>이 페이지는 관리자만 접근할 수 있습니다.</p>
        <button
          onClick={() => router.push('/')}
          className="px-6 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition"
        >
          홈으로 돌아가기
        </button>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto">

      {/* 헤더 */}
      <div className="mb-8 flex items-center justify-between">
        <div>
          <div className="flex items-center gap-3 mb-2">
            <Settings className={`w-8 h-8 ${isDark ? 'text-indigo-400' : 'text-indigo-600'}`} />
            <h1 className={`text-3xl font-black ${isDark ? 'text-white' : 'text-slate-900'}`}>질문 관리</h1>
          </div>
          <p className={isDark ? 'text-slate-400' : 'text-slate-600'}>예측 시장 질문을 생성하고 관리하세요</p>
        </div>
        <button
          onClick={() => setShowCreateForm(!showCreateForm)}
          className="flex items-center gap-2 px-6 py-3 bg-indigo-600 text-white rounded-xl hover:bg-indigo-700 transition font-bold"
        >
          <Plus size={20} />
          새 질문 생성
        </button>
      </div>

      {/* 실시간 LIVE 경기 */}
      <div className="mb-8">
        <LiveMatchesDashboard />
      </div>

      {/* 자동 질문 생성기 */}
      <div className={`rounded-2xl p-6 mb-8 border ${
        isDark
          ? 'bg-slate-800/50 border-slate-700'
          : 'bg-white border-slate-200 shadow-lg'
      }`}>
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-3">
            <Sparkles className="text-purple-500" size={24} />
            <h2 className={`text-xl font-bold ${isDark ? 'text-white' : 'text-slate-800'}`}>
              자동 질문 생성기
            </h2>
            {isDemoMode && (
              <span className="px-3 py-1 bg-amber-500/20 text-amber-400 text-xs font-bold rounded-full border border-amber-500/30">
                DEMO MODE
              </span>
            )}
          </div>
          <button
            onClick={handleGenerateQuestion}
            disabled={generating}
            className="flex items-center gap-2 px-4 py-2 bg-purple-600 text-white rounded-lg hover:bg-purple-700 transition font-semibold disabled:opacity-50"
          >
            {generating ? (
              <>
                <Loader2 size={16} className="animate-spin" />
                생성 중...
              </>
            ) : (
              <>
                <Sparkles size={16} />
                지금 생성
              </>
            )}
          </button>
        </div>

        {/* 데모 모드 알림 */}
        {isDemoMode && (
          <div className={`mb-4 p-3 rounded-lg border ${
            isDark
              ? 'bg-amber-500/10 border-amber-500/30'
              : 'bg-amber-50 border-amber-200'
          }`}>
            <p className={`text-sm ${isDark ? 'text-amber-300' : 'text-amber-700'}`}>
              <span className="font-semibold">데모 모드:</span> API 키가 설정되지 않아 미리 준비된 샘플 질문을 사용합니다.
            </p>
          </div>
        )}

        {/* 토글 스위치 */}
        <div className="flex items-center gap-3 mb-4">
          <span className={`text-sm font-medium ${isDark ? 'text-slate-300' : 'text-slate-700'}`}>
            자동 생성 활성화
          </span>
          <button
            onClick={() => handleToggleGenerator(!generatorEnabled)}
            disabled={generatorLoading}
            className={`relative w-12 h-6 rounded-full transition-colors ${
              generatorEnabled ? 'bg-purple-600' : isDark ? 'bg-slate-600' : 'bg-slate-300'
            } ${generatorLoading ? 'opacity-50' : ''}`}
          >
            <span
              className={`absolute top-1 w-4 h-4 bg-white rounded-full shadow transition-transform ${
                generatorEnabled ? 'left-7' : 'left-1'
              }`}
            />
          </button>
        </div>

        {/* 설정 영역 (토글 on일 때만 표시) */}
        {generatorEnabled && (
          <div className={`space-y-4 pt-4 border-t ${isDark ? 'border-slate-700' : 'border-slate-200'}`}>
            {/* 생성 간격 */}
            <div className="flex items-center gap-4">
              <label className={`text-sm font-medium w-24 ${isDark ? 'text-slate-300' : 'text-slate-700'}`}>
                생성 간격
              </label>
              <select
                value={generatorInterval}
                onChange={(e) => handleIntervalChange(Number(e.target.value))}
                disabled={generatorLoading}
                className={`px-3 py-2 rounded-lg focus:ring-2 focus:ring-purple-500 text-sm ${
                  isDark
                    ? 'bg-slate-700 border-slate-600 text-white'
                    : 'bg-white border-slate-300 text-slate-800'
                } border`}
              >
                {intervalOptions.map((opt) => (
                  <option key={opt.value} value={opt.value}>{opt.label}</option>
                ))}
              </select>
            </div>

            {/* 카테고리 체크박스 */}
            <div className="flex items-center gap-4">
              <label className={`text-sm font-medium w-24 ${isDark ? 'text-slate-300' : 'text-slate-700'}`}>
                카테고리
              </label>
              <div className="flex flex-wrap gap-3">
                {categories.map((cat) => (
                  <label key={cat} className="flex items-center gap-2 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={generatorCategories.includes(cat)}
                      onChange={() => handleCategoryToggle(cat)}
                      disabled={generatorLoading}
                      className="w-4 h-4 rounded border-slate-300 text-purple-600 focus:ring-purple-500"
                    />
                    <span className={`text-sm ${isDark ? 'text-slate-300' : 'text-slate-600'}`}>{cat}</span>
                  </label>
                ))}
              </div>
            </div>
          </div>
        )}
      </div>

      {/* 질문 생성 폼 */}
      {showCreateForm && (
        <div className={`rounded-2xl p-6 mb-8 border-2 ${
          isDark
            ? 'bg-slate-800/50 border-indigo-500/50'
            : 'bg-white border-indigo-200 shadow-lg'
        }`}>
          <h2 className={`text-2xl font-bold mb-4 ${isDark ? 'text-white' : 'text-slate-800'}`}>
            새 질문 만들기
          </h2>

          <div className="space-y-4">
            {/* 제목 */}
            <div>
              <label className={`block text-sm font-semibold mb-2 ${isDark ? 'text-slate-300' : 'text-slate-700'}`}>
                질문 제목
              </label>
              <input
                type="text"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="예: Will Bitcoin reach $100,000 by end of 2026?"
                className={`w-full p-3 rounded-lg focus:ring-2 focus:ring-indigo-500 ${
                  isDark
                    ? 'bg-slate-700 border-slate-600 text-white placeholder-slate-400'
                    : 'bg-white border-slate-300 text-slate-800'
                } border`}
              />
            </div>

            {/* 카테고리 */}
            <div>
              <label className={`block text-sm font-semibold mb-2 ${isDark ? 'text-slate-300' : 'text-slate-700'}`}>
                카테고리
              </label>
              <select
                value={category}
                onChange={(e) => setCategory(e.target.value)}
                className={`w-full p-3 rounded-lg focus:ring-2 focus:ring-indigo-500 ${
                  isDark
                    ? 'bg-slate-700 border-slate-600 text-white'
                    : 'bg-white border-slate-300 text-slate-800'
                } border`}
              >
                {categories.map((cat) => (
                  <option key={cat} value={cat}>{cat}</option>
                ))}
              </select>
            </div>

            {/* 마감일 */}
            <div>
              <label className={`block text-sm font-semibold mb-2 ${isDark ? 'text-slate-300' : 'text-slate-700'}`}>
                마감일
              </label>
              <input
                type="datetime-local"
                value={expiredAt}
                onChange={(e) => setExpiredAt(e.target.value)}
                className={`w-full p-3 rounded-lg focus:ring-2 focus:ring-indigo-500 ${
                  isDark
                    ? 'bg-slate-700 border-slate-600 text-white'
                    : 'bg-white border-slate-300 text-slate-800'
                } border`}
              />
            </div>

            {/* 버튼 */}
            <div className="flex gap-3">
              <button
                onClick={() => setShowCreateForm(false)}
                className={`flex-1 py-3 rounded-lg transition font-semibold ${
                  isDark
                    ? 'bg-slate-700 text-slate-300 hover:bg-slate-600'
                    : 'bg-slate-200 text-slate-800 hover:bg-slate-300'
                }`}
              >
                취소
              </button>
              <button
                onClick={handleCreate}
                disabled={loading}
                className="flex-1 py-3 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition font-semibold disabled:opacity-50"
              >
                {loading ? '생성 중...' : '질문 생성'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 통계 */}
      <div className="grid grid-cols-4 gap-4 mb-8">
        <div className={`rounded-2xl p-6 ${isDark ? 'bg-slate-800/50 border border-slate-700' : 'bg-white shadow-lg'}`}>
          <p className={`text-sm mb-1 ${isDark ? 'text-slate-400' : 'text-slate-600'}`}>총 질문</p>
          <p className="text-3xl font-black text-blue-500">{questions.length}</p>
        </div>
        <div className={`rounded-2xl p-6 ${isDark ? 'bg-slate-800/50 border border-slate-700' : 'bg-white shadow-lg'}`}>
          <p className={`text-sm mb-1 ${isDark ? 'text-slate-400' : 'text-slate-600'}`}>투표 중</p>
          <p className="text-3xl font-black text-blue-500">
            {questions.filter(q => q.status === 'VOTING').length}
          </p>
        </div>
        <div className={`rounded-2xl p-6 ${isDark ? 'bg-slate-800/50 border border-slate-700' : 'bg-white shadow-lg'}`}>
          <p className={`text-sm mb-1 ${isDark ? 'text-slate-400' : 'text-slate-600'}`}>베팅 중</p>
          <p className="text-3xl font-black text-emerald-500">
            {questions.filter(q => q.status === 'BETTING').length}
          </p>
        </div>
        <div className={`rounded-2xl p-6 ${isDark ? 'bg-slate-800/50 border border-slate-700' : 'bg-white shadow-lg'}`}>
          <p className={`text-sm mb-1 ${isDark ? 'text-slate-400' : 'text-slate-600'}`}>정산 완료</p>
          <p className="text-3xl font-black text-purple-500">
            {questions.filter(q => q.status === 'SETTLED').length}
          </p>
        </div>
      </div>

      {/* 질문 목록 */}
      <div className={`rounded-2xl overflow-hidden ${isDark ? 'bg-slate-800/50 border border-slate-700' : 'bg-white shadow-lg'}`}>
        <table className="w-full">
          <thead className={isDark ? 'bg-slate-700/50' : 'bg-slate-100'}>
            <tr>
              <th className={`p-4 text-left font-bold text-sm ${isDark ? 'text-white' : 'text-slate-800'}`}>
                <button
                  onClick={() => handleSort('id')}
                  className="flex items-center gap-1 hover:text-indigo-400 transition"
                >
                  ID <SortIcon column="id" />
                </button>
              </th>
              <th className={`p-4 text-left font-bold text-sm ${isDark ? 'text-white' : 'text-slate-800'}`}>
                <button
                  onClick={() => handleSort('title')}
                  className="flex items-center gap-1 hover:text-indigo-400 transition"
                >
                  제목 <SortIcon column="title" />
                </button>
              </th>
              <th className={`p-4 text-left font-bold text-sm ${isDark ? 'text-white' : 'text-slate-800'}`}>
                <button
                  onClick={() => handleSort('category')}
                  className="flex items-center gap-1 hover:text-indigo-400 transition"
                >
                  카테고리 <SortIcon column="category" />
                </button>
              </th>
              <th className={`p-4 text-left font-bold text-sm ${isDark ? 'text-white' : 'text-slate-800'}`}>
                <button
                  onClick={() => handleSort('status')}
                  className="flex items-center gap-1 hover:text-indigo-400 transition"
                >
                  상태 <SortIcon column="status" />
                </button>
              </th>
              <th className={`p-4 text-left font-bold text-sm ${isDark ? 'text-white' : 'text-slate-800'}`}>
                <button
                  onClick={() => handleSort('totalBetPool')}
                  className="flex items-center gap-1 hover:text-indigo-400 transition"
                >
                  거래량 <SortIcon column="totalBetPool" />
                </button>
              </th>
              <th className={`p-4 text-left font-bold text-sm ${isDark ? 'text-white' : 'text-slate-800'}`}>
                <button
                  onClick={() => handleSort('expiredAt')}
                  className="flex items-center gap-1 hover:text-indigo-400 transition"
                >
                  마감일 <SortIcon column="expiredAt" />
                </button>
              </th>
              <th className={`p-4 text-left font-bold text-sm ${isDark ? 'text-white' : 'text-slate-800'}`}>작업</th>
            </tr>
          </thead>
          <tbody>
            {sortedQuestions.map((question) => (
              <tr key={question.id} className={`border-b ${
                isDark
                  ? 'border-slate-700 hover:bg-slate-700/50'
                  : 'border-slate-100 hover:bg-slate-50'
              }`}>
                <td className={`p-4 text-sm font-mono ${isDark ? 'text-slate-300' : 'text-slate-600'}`}>
                  #{question.id}
                </td>
                <td className={`p-4 text-sm font-semibold max-w-md truncate ${isDark ? 'text-white' : 'text-slate-800'}`}>
                  {question.title}
                </td>
                <td className="p-4">
                  <span className="px-2 py-1 bg-blue-500/20 text-blue-400 rounded text-xs font-bold">
                    {question.category}
                  </span>
                </td>
                <td className="p-4">
                  <span className={`px-2 py-1 rounded text-xs font-bold ${
                    question.status === 'VOTING' ? 'bg-blue-500/20 text-blue-400' :
                    question.status === 'BREAK' ? 'bg-amber-500/20 text-amber-400' :
                    question.status === 'BETTING' ? 'bg-emerald-500/20 text-emerald-400' :
                    question.status === 'SETTLED' ? 'bg-purple-500/20 text-purple-400' :
                    'bg-slate-500/20 text-slate-400'
                  }`}>
                    {question.status === 'VOTING' ? '투표중' :
                     question.status === 'BREAK' ? '휴식' :
                     question.status === 'BETTING' ? '베팅중' :
                     question.status === 'SETTLED' ? '정산완료' : question.status}
                  </span>
                </td>
                <td className={`p-4 text-sm ${isDark ? 'text-slate-300' : 'text-slate-600'}`}>
                  ${question.totalBetPool.toLocaleString()}
                </td>
                <td className={`p-4 text-sm ${isDark ? 'text-slate-400' : 'text-slate-600'}`}>
                  {new Date(question.expiredAt).toLocaleDateString('ko-KR')}
                </td>
                <td className="p-4">
                  <div className="flex gap-2">
                    <button
                      onClick={() => window.location.href = `/question/${question.id}`}
                      className={`p-2 rounded transition ${
                        isDark ? 'text-blue-400 hover:bg-blue-500/20' : 'text-blue-600 hover:bg-blue-50'
                      }`}
                      title="보기"
                    >
                      <TrendingUp size={16} />
                    </button>
                    {(question.status === 'BETTING' || question.status === 'VOTING') && (
                      <button
                        onClick={() => { setSettleTarget(question); setSettleFinalResult('YES'); setSettleSourceUrl(''); }}
                        className={`p-2 rounded transition ${
                          isDark ? 'text-amber-400 hover:bg-amber-500/20' : 'text-amber-600 hover:bg-amber-50'
                        }`}
                        title="정산 시작"
                      >
                        <Gavel size={16} />
                      </button>
                    )}
                    {question.status === 'SETTLED' && question.disputeDeadline && new Date(question.disputeDeadline) > new Date() && (
                      <>
                        <button
                          onClick={() => handleFinalize(question.id)}
                          className="px-2 py-1 text-xs font-bold text-emerald-400 bg-emerald-500/20 hover:bg-emerald-500/30 rounded transition"
                        >
                          확정
                        </button>
                        <button
                          onClick={() => handleCancelSettle(question.id)}
                          className="px-2 py-1 text-xs font-bold text-red-400 bg-red-500/20 hover:bg-red-500/30 rounded transition"
                        >
                          취소
                        </button>
                      </>
                    )}
                    {question.status !== 'SETTLED' && (
                      <button
                        onClick={() => handleDelete(question.id)}
                        className={`p-2 rounded transition ${
                          isDark ? 'text-red-400 hover:bg-red-500/20' : 'text-red-600 hover:bg-red-50'
                        }`}
                        title="삭제"
                      >
                        <Trash2 size={16} />
                      </button>
                    )}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* 정산 모달 */}
      {settleTarget && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className={`rounded-2xl shadow-2xl p-8 max-w-lg w-full mx-4 ${
            isDark ? 'bg-slate-800' : 'bg-white'
          }`}>
            <div className="flex items-center justify-between mb-6">
              <h3 className={`text-xl font-black ${isDark ? 'text-white' : 'text-slate-800'}`}>정산 시작</h3>
              <button
                onClick={() => setSettleTarget(null)}
                className={isDark ? 'text-slate-400 hover:text-slate-300' : 'text-slate-400 hover:text-slate-600'}
              >
                <X size={20} />
              </button>
            </div>

            <p className={`text-sm mb-4 font-medium ${isDark ? 'text-slate-300' : 'text-slate-600'}`}>
              &quot;{settleTarget.title}&quot;
            </p>

            <div className="mb-4">
              <label className={`block text-sm font-semibold mb-2 ${isDark ? 'text-slate-300' : 'text-slate-700'}`}>
                최종 결과
              </label>
              <div className="grid grid-cols-2 gap-3">
                <button
                  onClick={() => setSettleFinalResult('YES')}
                  className={`py-3 rounded-xl font-bold text-sm transition-all border-2 ${
                    settleFinalResult === 'YES'
                      ? 'bg-emerald-500 text-white border-emerald-600'
                      : isDark
                        ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30 hover:bg-emerald-500/20'
                        : 'bg-emerald-50 text-emerald-600 border-emerald-200 hover:bg-emerald-100'
                  }`}
                >
                  YES
                </button>
                <button
                  onClick={() => setSettleFinalResult('NO')}
                  className={`py-3 rounded-xl font-bold text-sm transition-all border-2 ${
                    settleFinalResult === 'NO'
                      ? 'bg-rose-500 text-white border-rose-600'
                      : isDark
                        ? 'bg-rose-500/10 text-rose-400 border-rose-500/30 hover:bg-rose-500/20'
                        : 'bg-rose-50 text-rose-600 border-rose-200 hover:bg-rose-100'
                  }`}
                >
                  NO
                </button>
              </div>
            </div>

            <div className="mb-6">
              <label className={`block text-sm font-semibold mb-2 ${isDark ? 'text-slate-300' : 'text-slate-700'}`}>
                정산 근거 URL (선택)
              </label>
              <input
                type="url"
                value={settleSourceUrl}
                onChange={(e) => setSettleSourceUrl(e.target.value)}
                placeholder="https://news.example.com/article/..."
                className={`w-full p-3 rounded-lg focus:ring-2 focus:ring-amber-500 text-sm ${
                  isDark
                    ? 'bg-slate-700 border-slate-600 text-white placeholder-slate-400'
                    : 'bg-white border-slate-300 text-slate-800'
                } border`}
              />
              <p className={`text-xs mt-1 ${isDark ? 'text-slate-500' : 'text-slate-400'}`}>
                결과 판정의 근거가 되는 뉴스/기사 링크를 입력하세요
              </p>
            </div>

            <div className={`rounded-xl p-4 mb-6 border ${
              isDark
                ? 'bg-amber-500/10 border-amber-500/30'
                : 'bg-amber-50 border-amber-200'
            }`}>
              <div className="flex items-start gap-2">
                <AlertTriangle size={16} className="text-amber-500 mt-0.5 flex-shrink-0" />
                <p className={`text-xs ${isDark ? 'text-amber-300' : 'text-amber-700'}`}>
                  정산을 시작하면 24시간 이의 제기 기간이 시작됩니다. 이 기간 동안 배당금은 분배되지 않으며, &quot;확정&quot; 버튼을 눌러야 최종 정산됩니다.
                </p>
              </div>
            </div>

            <div className="flex gap-3">
              <button
                onClick={() => setSettleTarget(null)}
                className={`flex-1 py-3 rounded-lg transition font-semibold ${
                  isDark
                    ? 'bg-slate-700 text-slate-300 hover:bg-slate-600'
                    : 'bg-slate-200 text-slate-800 hover:bg-slate-300'
                }`}
              >
                취소
              </button>
              <button
                onClick={handleSettle}
                disabled={loading}
                className="flex-1 py-3 bg-amber-600 text-white rounded-lg hover:bg-amber-700 transition font-bold disabled:opacity-50"
              >
                {loading ? '처리 중...' : '정산 시작'}
              </button>
            </div>
          </div>
        </div>
      )}
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
