'use client';

import { useState, useEffect, useMemo, useCallback } from 'react';
import { SlidersHorizontal, Vote as VoteIcon, AlertCircle, Ticket } from 'lucide-react';
import MainLayout from '@/components/layout/MainLayout';
import MarketCard from '@/components/market/MarketCard';
import CategoryFilter from '@/components/market/CategoryFilter';
import { useTheme } from '@/hooks/useTheme';
import { questionApi, ticketApi } from '@/lib/api';
import { useAuth } from '@/hooks/useAuth';
import { useVotedQuestions } from '@/hooks/useVotedQuestions';
import type { Question, QuestionCategory } from '@/types/api';
import type { TicketStatus } from '@/lib/api/ticket';

function VoteContent() {
  const { isDark } = useTheme();
  const { user } = useAuth();
  const { getChoice } = useVotedQuestions(user?.id);
  const [questions, setQuestions] = useState<Question[]>([]);
  const [loading, setLoading] = useState(true);
  const [category, setCategory] = useState<QuestionCategory>('ALL');
  const [sortBy, setSortBy] = useState<'views' | 'recent'>('views');
  const [ticketStatus, setTicketStatus] = useState<TicketStatus | null>(null);

  const fetchQuestions = useCallback(() => {
    questionApi.getAll().then(res => {
      if (res.success && res.data) {
        // Filter questions with VOTING status and voting period not ended
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

  // Fetch ticket status
  const fetchTicketStatus = useCallback(() => {
    if (!user) return;
    ticketApi.getStatus().then(setTicketStatus).catch(() => setTicketStatus(null));
  }, [user]);

  // Initial load
  useEffect(() => {
    fetchQuestions();
    fetchTicketStatus();
  }, [fetchQuestions, fetchTicketStatus]);

  // Auto-refresh every 5 seconds
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
          <h1 className={`text-3xl font-black ${isDark ? 'text-white' : 'text-slate-900'}`}>Vote</h1>
        </div>
        <p className="text-slate-400">Vote on VOTING status questions (5 per day limit)</p>

        {user && !user.hasVotingPass ? (
          <div className={`mt-4 p-4 rounded-xl flex items-center gap-3 ${
            isDark ? 'bg-amber-950 border border-amber-800' : 'bg-amber-50 border border-amber-200'
          }`}>
            <AlertCircle className="w-5 h-5 text-amber-500" />
            <div className="flex-1">
              <p className={`font-bold ${isDark ? 'text-amber-300' : 'text-amber-700'}`}>
                Voting Pass required. Please purchase in My Page.
              </p>
            </div>
          </div>
        ) : user && ticketStatus && (
          <div className={`mt-4 p-4 rounded-xl flex items-center gap-3 ${
            ticketStatus.remainingTickets === 0
              ? isDark ? 'bg-red-950 border border-red-800' : 'bg-red-50 border border-red-200'
              : isDark ? 'bg-indigo-950 border border-indigo-800' : 'bg-indigo-50 border border-indigo-200'
          }`}>
            {ticketStatus.remainingTickets === 0 ? (
              <>
                <AlertCircle className="w-5 h-5 text-red-500" />
                <div className="flex-1">
                  <p className={`font-bold ${isDark ? 'text-red-400' : 'text-red-600'}`}>
                    You&apos;ve used all votes for today
                  </p>
                  <p className="text-sm text-slate-400">You can vote again tomorrow</p>
                </div>
              </>
            ) : (
              <>
                <Ticket className={`w-5 h-5 ${isDark ? 'text-indigo-400' : 'text-indigo-600'}`} />
                <p className={`font-bold ${isDark ? 'text-indigo-400' : 'text-indigo-600'}`}>
                  Remaining Votes: {ticketStatus.remainingTickets}/{ticketStatus.maxTickets}
                </p>
              </>
            )}
          </div>
        )}
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
            Most Viewed
          </button>
          <button
            onClick={() => setSortBy('recent')}
            className={`px-3 py-1.5 rounded-xl text-xs font-bold transition ${
              sortBy === 'recent'
                ? 'bg-indigo-600 text-white'
                : isDark ? 'text-slate-400 hover:bg-slate-800' : 'text-slate-500 hover:bg-slate-100'
            }`}
          >
            Latest
          </button>
        </div>
      </div>

      {filtered.length === 0 ? (
        <div className="text-center py-20">
          <VoteIcon className={`w-16 h-16 mx-auto mb-4 ${isDark ? 'text-slate-700' : 'text-slate-300'}`} />
          <p className="text-slate-400 text-lg">
            {questions.length === 0 ? 'No questions available for voting.' : 'No votable questions in this category.'}
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
