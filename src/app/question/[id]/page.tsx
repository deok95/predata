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
import { ApiError } from '@/lib/api/core';

function QuestionDetailContent() {
  const { isDark } = useTheme();
  const params = useParams();
  const questionId = Number(params.id);
  const { user, refreshUser } = useAuth();
  const { getChoice, markVoted } = useVotedQuestions(user?.id);
  const [question, setQuestion] = useState<Question | null>(null);
  const [loading, setLoading] = useState(true);
  const [refreshKey, setRefreshKey] = useState(0);
  const [isMockData, setIsMockData] = useState(false);
  const [loadError, setLoadError] = useState<{ status: number; message: string } | null>(null);

  const fetchQuestion = useCallback(async () => {
    try {
      const response = await questionApi.getById(questionId);
      if (response.success && response.data) {
        setQuestion(response.data);
        setIsMockData(false);
        setLoadError(null); // 성공 시 에러 초기화
      } else {
        const mock = mockQuestions.find(q => q.id === questionId) || null;
        setQuestion(mock);
        setIsMockData(true);
        setLoadError(null);
      }
    } catch (error) {
      // ApiError인 경우 status 코드로 분기
      if (error instanceof ApiError) {
        if (error.status === 404) {
          // 404: 질문이 존재하지 않음 → mock 데이터로 전환
          const mock = mockQuestions.find(q => q.id === questionId) || null;
          setQuestion(mock);
          setIsMockData(true);
          setLoadError(null);
        } else {
          // 429, 5xx, 408(timeout) 등: 일시적 에러 → 기존 데이터 유지
          setLoadError({
            status: error.status,
            message: error.message
          });
          // isMockData는 변경하지 않음
          // question도 기존 값 유지 (최초 로딩이면 null 유지)
        }
      } else {
        // 네트워크 에러 등 예상치 못한 에러
        setLoadError({
          status: 0,
          message: error instanceof Error ? error.message : '알 수 없는 오류가 발생했습니다.'
        });
      }
    } finally {
      setLoading(false);
    }
  }, [questionId]);

  useEffect(() => {
    if (questionId && !isNaN(questionId)) fetchQuestion();
    else setLoading(false);
  }, [questionId, fetchQuestion]);

  // 조회수 증가 (탭(session) 기준 1회만: StrictMode 재마운트에도 중복 호출 방지)
  useEffect(() => {
    if (!questionId || isNaN(questionId)) return;
    if (typeof window === 'undefined') return;

    const storageKey = `viewed_${questionId}`;
    if (sessionStorage.getItem(storageKey)) return;

    sessionStorage.setItem(storageKey, 'true');
    questionApi.incrementViewCount(questionId).catch(() => {});
  }, [questionId]);

  useEffect(() => {
    if (!question || isMockData) return;
    const interval = setInterval(() => {
      if (document.visibilityState !== 'visible') return;
      fetchQuestion();
    }, 15000); // 비활성 탭 스킵, 15초 폴링
    return () => clearInterval(interval);
  }, [question, isMockData, fetchQuestion]);

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
    // 일시적 에러 (429, 5xx, timeout 등)로 최초 로딩이 실패한 경우
    if (loadError) {
      return (
        <div className={`text-center py-20 ${isDark ? 'text-slate-300' : 'text-slate-700'}`}>
          <AlertTriangle className="mx-auto mb-4 text-amber-500" size={48} />
          <p className="text-lg font-bold mb-2">질문을 불러올 수 없습니다</p>
          <p className="text-sm text-slate-400 mb-1">
            {loadError.status > 0 ? `HTTP ${loadError.status}` : '네트워크 오류'}
          </p>
          <p className="text-sm text-slate-400 mb-6">{loadError.message}</p>
          <button
            onClick={() => {
              setLoading(true);
              setLoadError(null);
              fetchQuestion();
            }}
            className="px-6 py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white font-bold rounded-xl transition"
          >
            다시 시도
          </button>
        </div>
      );
    }

    // 질문이 존재하지 않는 경우 (404 등)
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

  const isReadOnlyFallback = isMockData;
  const isVotingMode = question.status === 'VOTING';

  return (
    <div className={`${isVotingMode ? 'max-w-3xl' : 'max-w-7xl'} mx-auto animate-fade-in`}>
      <Link
        href={backLink}
        className="inline-flex items-center gap-2 text-sm text-slate-400 hover:text-indigo-600 transition mb-6"
      >
        <ArrowLeft size={16} />
        {backLabel}
      </Link>

      {!isVotingMode && (
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
            {question.status !== 'VOTING' && (
              <span className="font-bold">Vol. {question.totalBetPool.toLocaleString()} USDC</span>
            )}
            {question.bettingStartAt && (
              <span className="flex items-center gap-1">
                <Clock size={14} />
                경기 일시: {new Date(question.bettingStartAt).toLocaleString('ko-KR')}
              </span>
            )}
          </div>
        </div>
      )}

      {loadError && (
        <div className={`mb-6 p-4 rounded-2xl border ${
          isDark ? 'bg-rose-950/20 border-rose-900/30 text-rose-400' : 'bg-rose-50 border-rose-200 text-rose-700'
        }`}>
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <AlertTriangle size={18} />
              <span>
                데이터 업데이트 실패 ({loadError.status > 0 ? `HTTP ${loadError.status}` : '네트워크 오류'})
              </span>
            </div>
            <button
              onClick={() => {
                setLoadError(null);
                fetchQuestion();
              }}
              className="px-3 py-1 text-xs font-bold rounded-lg bg-rose-600 hover:bg-rose-700 text-white transition"
            >
              재시도
            </button>
          </div>
          <p className="text-xs mt-1 ml-6">{loadError.message}</p>
        </div>
      )}

      {isReadOnlyFallback && (
        <div className={`mb-6 p-4 rounded-2xl border ${
          isDark ? 'bg-amber-950/20 border-amber-900/30 text-amber-400' : 'bg-amber-50 border-amber-200 text-amber-700'
        }`}>
          현재 질문은 서버에서 찾을 수 없어 읽기 전용으로 표시됩니다. 실거래/주문/활동 내역 API 호출은 비활성화됩니다.
        </div>
      )}

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

      {isVotingMode ? (
        // VOTING 전용 레이아웃: 중앙 정렬 단일 컬럼
        <div className="max-w-2xl mx-auto space-y-6">
          {/* 질문 정보 카드 */}
          <div className={`rounded-2xl shadow-sm p-6 ${isDark ? 'bg-slate-900 border border-slate-800' : 'bg-white border border-slate-100'}`}>
            <div className="flex items-center gap-3 mb-4">
              {question.category && (
                <span className={`px-3 py-1 rounded-full text-xs font-bold ${isDark ? 'bg-slate-800 text-slate-400' : 'bg-slate-100 text-slate-600'}`}>
                  {question.category}
                </span>
              )}
              <span className="px-3 py-1 rounded-full text-xs font-bold bg-blue-100 text-blue-700">
                투표 진행 중
              </span>
            </div>
            <h1 className={`text-2xl font-bold mb-4 ${isDark ? 'text-white' : 'text-slate-900'}`}>
              {question.title}
            </h1>
            <div className="space-y-2">
              {question.bettingStartAt && (
                <div className={`flex items-center gap-2 text-sm ${isDark ? 'text-slate-400' : 'text-slate-600'}`}>
                  <Clock size={16} />
                  <span>
                    경기 일시: {new Date(question.bettingStartAt).toLocaleString('ko-KR')}
                  </span>
                </div>
              )}
              {question.votingEndAt && (
                <div className={`flex items-center gap-2 text-sm ${isDark ? 'text-slate-400' : 'text-slate-600'}`}>
                  <Clock size={16} />
                  <span>
                    투표 종료까지{' '}
                    {(() => {
                      const now = new Date().getTime();
                      const end = new Date(question.votingEndAt).getTime();
                      const diff = end - now;
                      if (diff <= 0) return '종료됨';
                      const hours = Math.floor(diff / (1000 * 60 * 60));
                      const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
                      return hours > 0 ? `${hours}시간 ${minutes}분` : `${minutes}분`;
                    })()}
                  </span>
                </div>
              )}
            </div>
          </div>
          {/* 투표 패널 */}
          {user && !isReadOnlyFallback && (
            <TradingPanel
              question={question}
              user={user}
              onTradeComplete={handleTradeComplete}
              votedChoice={getChoice(question.id)}
              onVoted={(choice) => markVoted(question.id, choice)}
            />
          )}
        </div>
      ) : (
        // 기존 베팅 레이아웃: 그리드
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
          <div className="lg:col-span-8 space-y-6">
            <ProbabilityChart
              yesPercent={yesPercent}
              totalPool={question.totalBetPool}
              yesPool={question.yesBetPool}
              noPool={question.noBetPool}
              questionId={question.id}
              disableApi={isReadOnlyFallback}
            />
            {!isReadOnlyFallback && (
              <OrderBook questionId={question.id} yesPercent={yesPercent} totalPool={question.totalBetPool} />
            )}
            {!isReadOnlyFallback && (
              <ActivityFeed questionId={question.id} refreshKey={refreshKey} />
            )}
            {!isReadOnlyFallback && user && <MyBetsPanel questionId={question.id} />}
          </div>
          <div className="lg:col-span-4">
            {user && !isReadOnlyFallback && (
              <TradingPanel
                question={question}
                user={user}
                onTradeComplete={handleTradeComplete}
                votedChoice={getChoice(question.id)}
                onVoted={(choice) => markVoted(question.id, choice)}
              />
            )}
            {user && isReadOnlyFallback && (
              <div className={`p-6 rounded-[2.5rem] border ${
                isDark ? 'bg-slate-900 border-slate-800 text-slate-400' : 'bg-white border-slate-100 text-slate-600 shadow-xl'
              }`}>
                서버 마켓 데이터가 없어 거래 패널이 비활성화되었습니다.
              </div>
            )}
          </div>
        </div>
      )}
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
