'use client';

import { useState, useEffect } from 'react';
import MainLayout from '@/components/layout/MainLayout';
import GlobalStatsBar from '@/components/dashboard/GlobalStatsBar';
import SponsoredMarket from '@/components/dashboard/SponsoredMarket';
import TrendingMarkets from '@/components/dashboard/TrendingMarkets';
import { questionApi } from '@/lib/api';
import { mockQuestions } from '@/lib/mockData';
import { useAuth } from '@/hooks/useAuth';
import { useVotedQuestions } from '@/hooks/useVotedQuestions';
import type { Question } from '@/types/api';

function DashboardContent() {
  const { user } = useAuth();
  const { getChoice } = useVotedQuestions(user?.id);
  const [questions, setQuestions] = useState<Question[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    questionApi.getAll().then(res => {
      if (res.success && res.data && res.data.length > 0) setQuestions(res.data);
      else setQuestions(mockQuestions);
    }).catch(() => {
      setQuestions(mockQuestions);
    }).finally(() => setLoading(false));
  }, []);

  const featured = questions.find(q => q.status === 'OPEN') || questions[0] || null;

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="w-8 h-8 border-4 border-indigo-600 border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto space-y-10 animate-fade-in">
      <GlobalStatsBar />
      {questions.length === 0 ? (
        <div className="text-center py-20">
          <p className="text-slate-400 text-lg">현재 등록된 마켓이 없습니다.</p>
          <p className="text-slate-500 text-sm mt-2">백엔드 서비스가 실행 중인지 확인해주세요.</p>
        </div>
      ) : (
        <>
          <SponsoredMarket question={featured} />
          <TrendingMarkets questions={questions} getVotedChoice={getChoice} />
        </>
      )}
    </div>
  );
}

export default function HomePage() {
  return (
    <MainLayout>
      <DashboardContent />
    </MainLayout>
  );
}
