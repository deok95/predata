'use client';

import { useState, useEffect, useCallback } from 'react';
import { useParams } from 'next/navigation';
import { ArrowLeft, CheckCircle, Clock, Shield, ExternalLink, AlertTriangle } from 'lucide-react';
import Link from 'next/link';
import MainLayout from '@/components/layout/MainLayout';
import ProbabilityChart from '@/components/market-detail/ProbabilityChart';
import OrderBook from '@/components/market-detail/OrderBook';
import TradingPanel from '@/components/market-detail/TradingPanel';
import ActivityFeed from '@/components/market-detail/ActivityFeed';
import MyBetsPanel from '@/components/market-detail/MyBetsPanel';
import { useTheme } from '@/hooks/useTheme';
import { questionApi } from '@/lib/api';
import { mockQuestions } from '@/lib/mockData';
import { useAuth } from '@/hooks/useAuth';
import { useVotedQuestions } from '@/hooks/useVotedQuestions';
import type { Question } from '@/types/api';

function QuestionDetailContent() {
  const { isDark } = useTheme();
  const params = useParams();
  const questionId = Number(params.id);
  const { user, refreshUser } = useAuth();
  const { getChoice, markVoted } = useVotedQuestions(user?.id);
  const [question, setQuestion] = useState<Question | null>(null);
  const [loading, setLoading] = useState(true);
  const [refreshKey, setRefreshKey] = useState(0);

  const fetchQuestion = useCallback(async () => {
    try {
      const response = await questionApi.getById(questionId);
      if (response.success && response.data) {
        setQuestion(response.data);
      } else {
        const mock = mockQuestions.find(q => q.id === questionId) || null;
        setQuestion(mock);
      }
    } catch {
      const mock = mockQuestions.find(q => q.id === questionId) || null;
      setQuestion(mock);
    } finally {
      setLoading(false);
    }
  }, [questionId]);

  useEffect(() => {
    if (questionId && !isNaN(questionId)) fetchQuestion();
    else setLoading(false);
  }, [questionId, fetchQuestion]);

  useEffect(() => {
    if (!question) return;
    const interval = setInterval(() => fetchQuestion(), 5000);
    return () => clearInterval(interval);
  }, [question, fetchQuestion]);

  const handleTradeComplete = () => {
    fetchQuestion();
    refreshUser();
    setRefreshKey((k) => k + 1);
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="w-8 h-8 border-4 border-indigo-600 border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  if (!question) {
    return (
      <div className="text-center py-20">
        <p className="text-slate-400 text-lg">마켓을 찾을 수 없습니다.</p>
        <Link href="/marketplace" className="text-indigo-600 text-sm font-bold mt-2 inline-block hover:underline">
          마켓 탐색으로 돌아가기
        </Link>
      </div>
    );
  }

  const yesPercent = question.totalBetPool > 0
    ? Math.round((question.yesBetPool / question.totalBetPool) * 100)
    : 50;

  // Determine back link based on question status
  const backLink = question.status === 'VOTING' ? '/vote' : '/marketplace';
  const backLabel = question.status === 'VOTING' ? '투표 목록' : '마켓 목록';

  return (
    <div className="max-w-7xl mx-auto animate-fade-in">
      <Link
        href={backLink}
        className="inline-flex items-center gap-2 text-sm text-slate-400 hover:text-indigo-600 transition mb-6"
      >
        <ArrowLeft size={16} />
        {backLabel}
      </Link>

      <div className="mb-8">
        <div className="flex items-center gap-3 mb-3">
          {question.category && (
            <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">
              {question.category}
            </span>
          )}
          <span className={`px-3 py-1 rounded-full text-[10px] font-black uppercase ${
            question.status === 'VOTING' || question.status === 'BETTING'
              ? 'bg-emerald-500/10 text-emerald-500'
              : question.status === 'BREAK'
                ? 'bg-amber-500/10 text-amber-500'
                : question.status === 'SETTLED'
                  ? 'bg-indigo-500/10 text-indigo-500'
                  : 'bg-slate-500/10 text-slate-500'
          }`}>
            {question.status === 'VOTING' ? '투표 진행 중' : question.status === 'BETTING' ? '베팅 진행 중' : question.status === 'BREAK' ? '휴식' : question.status === 'SETTLED' ? '정산 완료' : question.status}
          </span>
        </div>
        <h1 className={`text-2xl font-black mb-3 ${isDark ? 'text-white' : 'text-slate-900'}`}>{question.title}</h1>
        <div className="flex items-center gap-4 text-sm text-slate-400">
          <span className="font-bold">Vol. {question.totalBetPool.toLocaleString()} USDC</span>
          {question.expiredAt && (
            <span className="flex items-center gap-1">
              <Clock size={14} />
              {new Date(question.expiredAt).toLocaleDateString('ko-KR')}
            </span>
          )}
        </div>
      </div>

      {question.status === 'SETTLED' && question.finalResult && question.disputeDeadline && new Date(question.disputeDeadline) > new Date() && (
        <div className={`mb-6 p-5 rounded-2xl ${isDark ? 'bg-amber-950/20 border border-amber-900/30' : 'bg-amber-50 border border-amber-100'}`}>
          <div className="flex items-center gap-3 mb-2">
            <AlertTriangle className="text-amber-500" size={24} />
            <div>
              <div className="font-black text-amber-500">정산 검증 중 (이의 제기 가능)</div>
              <div className="text-sm text-slate-400">
                예상 결과: <span className={`font-black ${question.finalResult === 'YES' ? 'text-emerald-500' : 'text-rose-500'}`}>{question.finalResult}</span>
                <span className="ml-2">
                  · 확정 기한: {new Date(question.disputeDeadline).toLocaleString('ko-KR')}
                </span>
              </div>
            </div>
          </div>
          {question.sourceUrl && (
            <a
              href={question.sourceUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center gap-1.5 mt-2 ml-9 text-sm font-bold text-amber-600 hover:text-amber-700 hover:underline"
            >
              <Shield size={14} />
              정산 근거 보기
              <ExternalLink size={12} />
            </a>
          )}
        </div>
      )}

      {question.status === 'SETTLED' && question.finalResult && (!question.disputeDeadline || new Date(question.disputeDeadline) <= new Date()) && (
        <div className={`mb-6 p-5 rounded-2xl ${isDark ? 'bg-emerald-950/20 border border-emerald-900/30' : 'bg-emerald-50 border border-emerald-100'}`}>
          <div className="flex items-center gap-3">
            <CheckCircle className="text-emerald-500" size={24} />
            <div>
              <div className="font-black text-emerald-500">정산 완료</div>
              <div className="text-sm text-slate-400">
                최종 결과: <span className={`font-black ${question.finalResult === 'YES' ? 'text-emerald-500' : 'text-rose-500'}`}>
                  {question.finalResult}
                </span>
              </div>
            </div>
          </div>
          {question.sourceUrl && (
            <a
              href={question.sourceUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center gap-1.5 mt-2 ml-9 text-sm font-bold text-indigo-600 hover:text-indigo-700 hover:underline"
            >
              <Shield size={14} />
              정산 근거 보기
              <ExternalLink size={12} />
            </a>
          )}
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        <div className="lg:col-span-8 space-y-6">
          <ProbabilityChart
            yesPercent={yesPercent}
            totalPool={question.totalBetPool}
            yesPool={question.yesBetPool}
            noPool={question.noBetPool}
            questionId={question.id}
          />
          <OrderBook questionId={question.id} yesPercent={yesPercent} totalPool={question.totalBetPool} />
          <ActivityFeed questionId={question.id} refreshKey={refreshKey} />
          {user && <MyBetsPanel questionId={question.id} />}
        </div>
        <div className="lg:col-span-4">
          {user && (
            <TradingPanel
              question={question}
              user={user}
              onTradeComplete={handleTradeComplete}
              votedChoice={getChoice(question.id)}
              onVoted={(choice) => markVoted(question.id, choice)}
            />
          )}
        </div>
      </div>
    </div>
  );
}

export default function QuestionDetailPage() {
  return (
    <MainLayout>
      <QuestionDetailContent />
    </MainLayout>
  );
}
