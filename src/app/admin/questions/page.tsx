'use client';

import React, { useState, useEffect } from 'react';
import { Plus, Trash2, Edit, CheckCircle, Clock, TrendingUp, Gavel, X, AlertTriangle } from 'lucide-react';
import LiveMatchesDashboard from '@/components/LiveMatchesDashboard';

const BACKEND_URL = 'http://localhost:8080/api';

interface QuestionAdminView {
  id: number;
  title: string;
  category: string;
  status: string;
  totalBetPool: number;
  totalVotes: number;
  expiredAt: string;
  createdAt: string;
}

export default function AdminQuestionManagement() {
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

  const categories = ['ECONOMY', 'SPORTS', 'POLITICS', 'TECH', 'CULTURE'];

  useEffect(() => {
    fetchQuestions();
  }, []);

  const fetchQuestions = async () => {
    setLoading(true);
    try {
      const response = await fetch(`${BACKEND_URL}/admin/questions`);
      const data = await response.json();
      setQuestions(data);
    } catch (error) {
      console.error('Failed to fetch questions:', error);
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
      const response = await fetch(`${BACKEND_URL}/admin/questions`, {
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
      console.error('Failed to create question:', error);
      alert('❌ 질문 생성 중 오류가 발생했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm('정말 이 질문을 삭제하시겠습니까?')) return;

    try {
      const response = await fetch(`${BACKEND_URL}/admin/questions/${id}`, {
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
      console.error('Failed to delete question:', error);
      alert('❌ 질문 삭제 중 오류가 발생했습니다.');
    }
  };

  const handleSettle = async () => {
    if (!settleTarget) return;
    setLoading(true);
    try {
      const response = await fetch(`${BACKEND_URL}/questions/${settleTarget.id}/settle`, {
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
      console.error('Settlement failed:', error);
      alert('정산 중 오류가 발생했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const handleFinalize = async (id: number) => {
    if (!confirm('정산을 확정하시겠습니까? 배당금이 분배됩니다.')) return;
    try {
      const response = await fetch(`${BACKEND_URL}/questions/${id}/settle/finalize`, {
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
      console.error('Finalize failed:', error);
      alert('확정 중 오류가 발생했습니다.');
    }
  };

  const handleCancelSettle = async (id: number) => {
    if (!confirm('정산을 취소하시겠습니까? 질문이 OPEN 상태로 돌아갑니다.')) return;
    try {
      const response = await fetch(`${BACKEND_URL}/questions/${id}/settle/cancel`, {
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
      console.error('Cancel failed:', error);
      alert('취소 중 오류가 발생했습니다.');
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 to-red-50 py-8 px-4">
      <div className="max-w-7xl mx-auto">
        
        {/* 헤더 */}
        <div className="mb-8 flex items-center justify-between">
          <div>
            <h1 className="text-4xl font-black text-slate-800 mb-2">질문 관리</h1>
            <p className="text-slate-600 text-lg">예측 시장 질문을 생성하고 관리하세요</p>
          </div>
          <button
            onClick={() => setShowCreateForm(!showCreateForm)}
            className="flex items-center gap-2 px-6 py-3 bg-red-600 text-white rounded-lg hover:bg-red-700 transition font-bold"
          >
            <Plus size={20} />
            새 질문 생성
          </button>
        </div>

        {/* 실시간 LIVE 경기 */}
        <div className="mb-8">
          <LiveMatchesDashboard />
        </div>

        {/* 질문 생성 폼 */}
        {showCreateForm && (
          <div className="bg-white rounded-xl shadow-lg p-6 mb-8 border-2 border-red-200">
            <h2 className="text-2xl font-bold mb-4">새 질문 만들기</h2>
            
            <div className="space-y-4">
              {/* 제목 */}
              <div>
                <label className="block text-sm font-semibold text-slate-700 mb-2">질문 제목</label>
                <input
                  type="text"
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  placeholder="예: Will Bitcoin reach $100,000 by end of 2026?"
                  className="w-full p-3 border border-slate-300 rounded-lg focus:ring-2 focus:ring-red-500"
                />
              </div>

              {/* 카테고리 */}
              <div>
                <label className="block text-sm font-semibold text-slate-700 mb-2">카테고리</label>
                <select
                  value={category}
                  onChange={(e) => setCategory(e.target.value)}
                  className="w-full p-3 border border-slate-300 rounded-lg focus:ring-2 focus:ring-red-500"
                >
                  {categories.map((cat) => (
                    <option key={cat} value={cat}>{cat}</option>
                  ))}
                </select>
              </div>

              {/* 마감일 */}
              <div>
                <label className="block text-sm font-semibold text-slate-700 mb-2">마감일</label>
                <input
                  type="datetime-local"
                  value={expiredAt}
                  onChange={(e) => setExpiredAt(e.target.value)}
                  className="w-full p-3 border border-slate-300 rounded-lg focus:ring-2 focus:ring-red-500"
                />
              </div>

              {/* 버튼 */}
              <div className="flex gap-3">
                <button
                  onClick={() => setShowCreateForm(false)}
                  className="flex-1 py-3 bg-slate-200 text-slate-800 rounded-lg hover:bg-slate-300 transition font-semibold"
                >
                  취소
                </button>
                <button
                  onClick={handleCreate}
                  disabled={loading}
                  className="flex-1 py-3 bg-red-600 text-white rounded-lg hover:bg-red-700 transition font-semibold disabled:opacity-50"
                >
                  {loading ? '생성 중...' : '질문 생성'}
                </button>
              </div>
            </div>
          </div>
        )}

        {/* 통계 */}
        <div className="grid grid-cols-4 gap-4 mb-8">
          <div className="bg-white rounded-xl shadow-lg p-6">
            <p className="text-sm text-slate-600 mb-1">총 질문</p>
            <p className="text-3xl font-black text-blue-600">{questions.length}</p>
          </div>
          <div className="bg-white rounded-xl shadow-lg p-6">
            <p className="text-sm text-slate-600 mb-1">진행 중</p>
            <p className="text-3xl font-black text-green-600">
              {questions.filter(q => q.status === 'OPEN').length}
            </p>
          </div>
          <div className="bg-white rounded-xl shadow-lg p-6">
            <p className="text-sm text-slate-600 mb-1">정산 대기</p>
            <p className="text-3xl font-black text-amber-600">
              {questions.filter(q => q.status === 'PENDING_SETTLEMENT').length}
            </p>
          </div>
          <div className="bg-white rounded-xl shadow-lg p-6">
            <p className="text-sm text-slate-600 mb-1">정산 완료</p>
            <p className="text-3xl font-black text-purple-600">
              {questions.filter(q => q.status === 'SETTLED').length}
            </p>
          </div>
        </div>

        {/* 질문 목록 */}
        <div className="bg-white rounded-xl shadow-lg overflow-hidden">
          <table className="w-full">
            <thead className="bg-slate-100">
              <tr>
                <th className="p-4 text-left font-semibold text-sm">ID</th>
                <th className="p-4 text-left font-semibold text-sm">제목</th>
                <th className="p-4 text-left font-semibold text-sm">카테고리</th>
                <th className="p-4 text-left font-semibold text-sm">상태</th>
                <th className="p-4 text-left font-semibold text-sm">거래량</th>
                <th className="p-4 text-left font-semibold text-sm">마감일</th>
                <th className="p-4 text-left font-semibold text-sm">작업</th>
              </tr>
            </thead>
            <tbody>
              {questions.map((question) => (
                <tr key={question.id} className="border-b hover:bg-slate-50">
                  <td className="p-4 text-sm font-mono">#{question.id}</td>
                  <td className="p-4 text-sm font-semibold max-w-md truncate">{question.title}</td>
                  <td className="p-4">
                    <span className="px-2 py-1 bg-blue-100 text-blue-700 rounded text-xs font-bold">
                      {question.category}
                    </span>
                  </td>
                  <td className="p-4">
                    <span className={`px-2 py-1 rounded text-xs font-bold ${
                      question.status === 'OPEN' ? 'bg-green-100 text-green-700' :
                      question.status === 'PENDING_SETTLEMENT' ? 'bg-amber-100 text-amber-700' :
                      question.status === 'SETTLED' ? 'bg-purple-100 text-purple-700' :
                      'bg-slate-100 text-slate-700'
                    }`}>
                      {question.status === 'PENDING_SETTLEMENT' ? '정산 대기' : question.status}
                    </span>
                  </td>
                  <td className="p-4 text-sm">${question.totalBetPool.toLocaleString()}</td>
                  <td className="p-4 text-sm text-slate-600">
                    {new Date(question.expiredAt).toLocaleDateString('ko-KR')}
                  </td>
                  <td className="p-4">
                    <div className="flex gap-2">
                      <button
                        onClick={() => window.location.href = `/question/${question.id}`}
                        className="p-2 text-blue-600 hover:bg-blue-50 rounded transition"
                        title="보기"
                      >
                        <TrendingUp size={16} />
                      </button>
                      {question.status === 'OPEN' && (
                        <button
                          onClick={() => { setSettleTarget(question); setSettleFinalResult('YES'); setSettleSourceUrl(''); }}
                          className="p-2 text-amber-600 hover:bg-amber-50 rounded transition"
                          title="정산 시작"
                        >
                          <Gavel size={16} />
                        </button>
                      )}
                      {question.status === 'PENDING_SETTLEMENT' && (
                        <>
                          <button
                            onClick={() => handleFinalize(question.id)}
                            className="px-2 py-1 text-xs font-bold text-green-700 bg-green-100 hover:bg-green-200 rounded transition"
                          >
                            확정
                          </button>
                          <button
                            onClick={() => handleCancelSettle(question.id)}
                            className="px-2 py-1 text-xs font-bold text-red-700 bg-red-100 hover:bg-red-200 rounded transition"
                          >
                            취소
                          </button>
                        </>
                      )}
                      {question.status !== 'SETTLED' && question.status !== 'PENDING_SETTLEMENT' && (
                        <button
                          onClick={() => handleDelete(question.id)}
                          className="p-2 text-red-600 hover:bg-red-50 rounded transition"
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
      </div>

      {/* 정산 모달 */}
      {settleTarget && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-white rounded-2xl shadow-2xl p-8 max-w-lg w-full mx-4">
            <div className="flex items-center justify-between mb-6">
              <h3 className="text-xl font-black text-slate-800">정산 시작</h3>
              <button onClick={() => setSettleTarget(null)} className="text-slate-400 hover:text-slate-600">
                <X size={20} />
              </button>
            </div>

            <p className="text-sm text-slate-600 mb-4 font-medium">
              &quot;{settleTarget.title}&quot;
            </p>

            <div className="mb-4">
              <label className="block text-sm font-semibold text-slate-700 mb-2">최종 결과</label>
              <div className="grid grid-cols-2 gap-3">
                <button
                  onClick={() => setSettleFinalResult('YES')}
                  className={`py-3 rounded-xl font-bold text-sm transition-all border-2 ${
                    settleFinalResult === 'YES'
                      ? 'bg-emerald-500 text-white border-emerald-600'
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
                      : 'bg-rose-50 text-rose-600 border-rose-200 hover:bg-rose-100'
                  }`}
                >
                  NO
                </button>
              </div>
            </div>

            <div className="mb-6">
              <label className="block text-sm font-semibold text-slate-700 mb-2">정산 근거 URL (선택)</label>
              <input
                type="url"
                value={settleSourceUrl}
                onChange={(e) => setSettleSourceUrl(e.target.value)}
                placeholder="https://news.example.com/article/..."
                className="w-full p-3 border border-slate-300 rounded-lg focus:ring-2 focus:ring-amber-500 text-sm"
              />
              <p className="text-xs text-slate-400 mt-1">결과 판정의 근거가 되는 뉴스/기사 링크를 입력하세요</p>
            </div>

            <div className="bg-amber-50 border border-amber-200 rounded-xl p-4 mb-6">
              <div className="flex items-start gap-2">
                <AlertTriangle size={16} className="text-amber-600 mt-0.5 flex-shrink-0" />
                <p className="text-xs text-amber-700">
                  정산을 시작하면 24시간 이의 제기 기간이 시작됩니다. 이 기간 동안 배당금은 분배되지 않으며, &quot;확정&quot; 버튼을 눌러야 최종 정산됩니다.
                </p>
              </div>
            </div>

            <div className="flex gap-3">
              <button
                onClick={() => setSettleTarget(null)}
                className="flex-1 py-3 bg-slate-200 text-slate-800 rounded-lg hover:bg-slate-300 transition font-semibold"
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
