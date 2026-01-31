'use client';

import React, { useState, useEffect } from 'react';
import { Plus, Trash2, Edit, CheckCircle, Clock, TrendingUp } from 'lucide-react';
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
        <div className="grid grid-cols-3 gap-4 mb-8">
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
                      question.status === 'SETTLED' ? 'bg-purple-100 text-purple-700' :
                      'bg-slate-100 text-slate-700'
                    }`}>
                      {question.status}
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
                      <button
                        onClick={() => handleDelete(question.id)}
                        className="p-2 text-red-600 hover:bg-red-50 rounded transition"
                        title="삭제"
                        disabled={question.totalBetPool > 0}
                      >
                        <Trash2 size={16} />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
