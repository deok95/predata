'use client';

import { useState, useMemo, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { Database, ArrowUpDown } from 'lucide-react';
import MainLayout from '@/components/layout/MainLayout';
import CategoryFilter from '@/components/market/CategoryFilter';
import DataCenterCard from '@/components/data-center/DataCenterCard';
import GlobalStatsBar from '@/components/dashboard/GlobalStatsBar';
import { useTheme } from '@/hooks/useTheme';
import { useAuth } from '@/hooks/useAuth';
import { useQuestions } from '@/hooks/useQueries';
import type { QuestionCategory } from '@/types/api';

function DataCenterList() {
  const { isDark } = useTheme();
  const { user } = useAuth();
  const router = useRouter();
  const { data: questions = [], isLoading } = useQuestions();
  const [category, setCategory] = useState<QuestionCategory>('ALL');
  const [sortBy, setSortBy] = useState<'volume' | 'latest'>('volume');

  // Admin access check
  useEffect(() => {
    if (user && user.role !== 'ADMIN') {
      router.push('/');
    }
  }, [user, router]);

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

  if (isLoading) {
    return (
      <div className="max-w-7xl mx-auto animate-fade-in">
        <div className="mb-8">
          <div className="flex items-center gap-3 mb-2">
            <Database className="h-8 w-8 text-indigo-600" />
            <h1 className={`text-3xl font-black ${isDark ? 'text-white' : 'text-slate-900'}`}>Data Center</h1>
          </div>
          <p className="text-slate-400">Select a question to analyze data quality</p>
        </div>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {[...Array(6)].map((_, i) => (
            <div key={i} className={`h-48 rounded-3xl animate-pulse ${isDark ? 'bg-slate-800' : 'bg-slate-200'}`} />
          ))}
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto animate-fade-in">
      {/* Header */}
      <div className="mb-6">
        <div className="flex items-center gap-3 mb-2">
          <Database className="h-8 w-8 text-indigo-600" />
          <h1 className={`text-3xl font-black ${isDark ? 'text-white' : 'text-slate-900'}`}>Data Center</h1>
        </div>
        <p className="text-slate-400">Select a question to analyze data quality</p>
      </div>

      <div className="mb-8">
        <GlobalStatsBar />
      </div>

      {/* Filters + Sort */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6">
        <CategoryFilter selected={category} onSelect={setCategory} />
        <div className="flex items-center gap-2 shrink-0">
          <ArrowUpDown size={14} className="text-slate-400" />
          <button
            onClick={() => setSortBy('volume')}
            className={`px-4 py-2 rounded-xl text-xs font-bold transition-all ${
              sortBy === 'volume'
                ? 'bg-indigo-600 text-white'
                : isDark ? 'bg-slate-800 text-slate-400 hover:bg-slate-700' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
            }`}
          >
            By Volume
          </button>
          <button
            onClick={() => setSortBy('latest')}
            className={`px-4 py-2 rounded-xl text-xs font-bold transition-all ${
              sortBy === 'latest'
                ? 'bg-indigo-600 text-white'
                : isDark ? 'bg-slate-800 text-slate-400 hover:bg-slate-700' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
            }`}
          >
            Latest
          </button>
        </div>
      </div>

      {/* Question count */}
      <p className={`text-sm font-bold mb-4 ${isDark ? 'text-slate-400' : 'text-slate-500'}`}>
        Total {filtered.length} questions
      </p>

      {/* Card Grid */}
      {filtered.length > 0 ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {filtered.map((q) => (
            <DataCenterCard key={q.id} question={q} />
          ))}
        </div>
      ) : (
        <div className={`p-12 rounded-3xl border text-center ${isDark ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-100'}`}>
          <Database className="h-10 w-10 text-slate-400 mx-auto mb-3" />
          <p className={`font-black ${isDark ? 'text-white' : 'text-slate-900'}`}>No questions in this category</p>
        </div>
      )}
    </div>
  );
}

export default function DataCenterPage() {
  return (
    <MainLayout>
      <DataCenterList />
    </MainLayout>
  );
}
