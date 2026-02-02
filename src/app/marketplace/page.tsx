'use client';

import { useState, useEffect, useMemo } from 'react';
import { SlidersHorizontal } from 'lucide-react';
import MainLayout from '@/components/layout/MainLayout';
import MarketCard from '@/components/market/MarketCard';
import CategoryFilter from '@/components/market/CategoryFilter';
import { useTheme } from '@/hooks/useTheme';
import { questionApi } from '@/lib/api';
import { mockQuestions } from '@/lib/mockData';
import { useAuth } from '@/hooks/useAuth';
import { useVotedQuestions } from '@/hooks/useVotedQuestions';
import type { Question, QuestionCategory } from '@/types/api';

function MarketplaceContent() {
  const { isDark } = useTheme();
  const { user } = useAuth();
  const { getChoice } = useVotedQuestions(user?.id);
  const [questions, setQuestions] = useState<Question[]>([]);
  const [loading, setLoading] = useState(true);
  const [category, setCategory] = useState<QuestionCategory>('ALL');
  const [sortBy, setSortBy] = useState<'volume' | 'recent'>('volume');

  useEffect(() => {
    questionApi.getAll().then(res => {
      if (res.success && res.data && res.data.length > 0) setQuestions(res.data);
      else setQuestions(mockQuestions);
    }).catch(() => {
      setQuestions(mockQuestions);
    }).finally(() => setLoading(false));
  }, []);

  const filtered = useMemo(() => {
    let result = questions;
    if (category !== 'ALL') {
      result = result.filter(q => q.category === category);
    }
    if (sortBy === 'volume') {
      result = [...result].sort((a, b) => b.totalBetPool - a.totalBetPool);
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
        <h1 className={`text-3xl font-black mb-2 ${isDark ? 'text-white' : 'text-slate-900'}`}>마켓 탐색</h1>
        <p className="text-slate-400">다양한 예측 마켓에 참여하세요</p>
      </div>

      <div className="flex items-center justify-between mb-6 gap-4 flex-wrap">
        <CategoryFilter selected={category} onSelect={setCategory} />
        <div className="flex items-center gap-2">
          <SlidersHorizontal size={16} className="text-slate-400" />
          <button
            onClick={() => setSortBy('volume')}
            className={`px-3 py-1.5 rounded-xl text-xs font-bold transition ${
              sortBy === 'volume'
                ? 'bg-indigo-600 text-white'
                : isDark ? 'text-slate-400 hover:bg-slate-800' : 'text-slate-500 hover:bg-slate-100'
            }`}
          >
            거래량순
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
          <p className="text-slate-400 text-lg">
            {questions.length === 0 ? '현재 등록된 마켓이 없습니다.' : '해당 카테고리에 마켓이 없습니다.'}
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

export default function MarketplacePage() {
  return (
    <MainLayout>
      <MarketplaceContent />
    </MainLayout>
  );
}
