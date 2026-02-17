'use client';

import { useState, useEffect, useMemo, useCallback } from 'react';
import { SlidersHorizontal, Vote as VoteIcon } from 'lucide-react';
import MainLayout from '@/components/layout/MainLayout';
import MarketCard from '@/components/market/MarketCard';
import CategoryFilter from '@/components/market/CategoryFilter';
import { useTheme } from '@/hooks/useTheme';
import { questionApi } from '@/lib/api';
import { useAuth } from '@/hooks/useAuth';
import { useVotedQuestions } from '@/hooks/useVotedQuestions';
import type { Question, QuestionCategory } from '@/types/api';

function VoteContent() {
  const { isDark } = useTheme();
  const { user } = useAuth();
  const { getChoice } = useVotedQuestions(user?.id);
  const [questions, setQuestions] = useState<Question[]>([]);
  const [loading, setLoading] = useState(true);
  const [category, setCategory] = useState<QuestionCategory>('ALL');
  const [sortBy, setSortBy] = useState<'views' | 'recent'>('views');

  const fetchQuestions = useCallback(() => {
    questionApi.getAll().then(res => {
      if (res.success && res.data) {
        // VOTING 상태이면서 투표 종료 시간이 지나지 않은 질문만 필터링
        const now = new Date();
        const votingQuestions = res.data.filter(q =>
          q.status === 'VOTING' && new Date(q.votingEndAt) > now
        );
        setQuestions(votingQuestions);
      }
    }).catch(() => {
      setQuestions([]);
    }).finally(() => setLoading(false));
  }, []);

  // 초기 로드
  useEffect(() => {
    fetchQuestions();
  }, [fetchQuestions]);

  // 5초마다 자동 리프레시
  useEffect(() => {
    const interval = setInterval(() => {
      fetchQuestions();
    }, 5000);

    return () => clearInterval(interval);
  }, [fetchQuestions]);

  const filtered = useMemo(() => {
    let result = questions;
    if (category !== 'ALL') {
      result = result.filter(q => q.category === category);
    }
    if (sortBy === 'views') {
      result = [...result].sort((a, b) => (b.viewCount || 0) - (a.viewCount || 0));
    } else {
      result = [...result].sort((a, b) =>
        new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
      );
    }
    return result;
  }, [questions, category, sortBy]);

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="w-8 h-8 border-4 border-indigo-600 border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto animate-fade-in">
      <div className="mb-8">
        <div className="flex items-center gap-3 mb-2">
          <VoteIcon className={`w-8 h-8 ${isDark ? 'text-indigo-400' : 'text-indigo-600'}`} />
          <h1 className={`text-3xl font-black ${isDark ? 'text-white' : 'text-slate-900'}`}>투표</h1>
        </div>
        <p className="text-slate-400">VOTING 상태의 질문에 투표하세요 (하루 5개 제한)</p>
      </div>

      <div className="flex items-center justify-between mb-6 gap-4 flex-wrap">
        <CategoryFilter selected={category} onSelect={setCategory} />
        <div className="flex items-center gap-2">
          <SlidersHorizontal size={16} className="text-slate-400" />
          <button
            onClick={() => setSortBy('views')}
            className={`px-3 py-1.5 rounded-xl text-xs font-bold transition ${
              sortBy === 'views'
                ? 'bg-indigo-600 text-white'
                : isDark ? 'text-slate-400 hover:bg-slate-800' : 'text-slate-500 hover:bg-slate-100'
            }`}
          >
            조회순
          </button>
          <button
            onClick={() => setSortBy('recent')}
            className={`px-3 py-1.5 rounded-xl text-xs font-bold transition ${
              sortBy === 'recent'
                ? 'bg-indigo-600 text-white'
                : isDark ? 'text-slate-400 hover:bg-slate-800' : 'text-slate-500 hover:bg-slate-100'
            }`}
          >
            최신순
          </button>
        </div>
      </div>

      {filtered.length === 0 ? (
        <div className="text-center py-20">
          <VoteIcon className={`w-16 h-16 mx-auto mb-4 ${isDark ? 'text-slate-700' : 'text-slate-300'}`} />
          <p className="text-slate-400 text-lg">
            {questions.length === 0 ? '현재 투표 가능한 질문이 없습니다.' : '해당 카테고리에 투표 가능한 질문이 없습니다.'}
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {filtered.map((q) => (
            <MarketCard key={q.id} question={q} votedChoice={getChoice(q.id)} />
          ))}
        </div>
      )}
    </div>
  );
}

export default function VotePage() {
  return (
    <MainLayout>
      <VoteContent />
    </MainLayout>
  );
}
