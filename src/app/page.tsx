'use client';

import { useState, useEffect } from 'react';
import MainLayout from '@/components/layout/MainLayout';
import SponsoredMarket from '@/components/dashboard/SponsoredMarket';
import TrendingMarkets from '@/components/dashboard/TrendingMarkets';
import { questionApi } from '@/lib/api';
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
      else setQuestions([]);
    }).catch(() => {
      setQuestions([]);
    }).finally(() => setLoading(false));
  }, []);

  const featuredQuestions = (() => {
    if (questions.length === 0) return [];

    const statusPriority = (status: Question['status']) => {
      if (status === 'VOTING') return 0;
      if (status === 'BETTING') return 1;
      return 2;
    };

    return [...questions]
      .filter((q) => q.status === 'VOTING' || q.status === 'BETTING')
      .sort((a, b) => {
        const statusDiff = statusPriority(a.status) - statusPriority(b.status);
        if (statusDiff !== 0) return statusDiff;
        if (b.totalBetPool !== a.totalBetPool) return b.totalBetPool - a.totalBetPool;
        return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
      })
      .slice(0, 8);
  })();

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="w-8 h-8 border-4 border-indigo-600 border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto space-y-10 animate-fade-in">
      {questions.length === 0 ? (
        <div className="text-center py-20">
          <p className="text-slate-400 text-lg">No markets available.</p>
          <p className="text-slate-500 text-sm mt-2">Please check if the backend service is running.</p>
        </div>
      ) : (
        <>
          <SponsoredMarket questions={featuredQuestions} />
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
