'use client';

import { useState, useEffect, useCallback } from 'react';
import { useParams } from 'next/navigation';
import { ArrowLeft, CheckCircle, Clock, Shield, ExternalLink, AlertTriangle } from 'lucide-react';
import Link from 'next/link';
import MainLayout from '@/components/layout/MainLayout';
import ProbabilityChart from '@/components/market-detail/ProbabilityChart';
import TradingPanel from '@/components/market-detail/TradingPanel';
import ActivityFeed from '@/components/market-detail/ActivityFeed';
import MyBetsPanel from '@/components/market-detail/MyBetsPanel';
import { useTheme } from '@/hooks/useTheme';
import { questionApi } from '@/lib/api';
import { useAuth } from '@/hooks/useAuth';
import { useVotedQuestions } from '@/hooks/useVotedQuestions';
import type { Question } from '@/types/api';
import { ApiError } from '@/lib/api/core';
import { swapApi, type PoolStateResponse } from '@/lib/api/swap';

function QuestionDetailContent() {
  const { isDark } = useTheme();
  const params = useParams();
  const questionId = Number(params.id);
  const { user, refreshUser } = useAuth();
  const { getChoice, markVoted } = useVotedQuestions(user?.id);
  const [question, setQuestion] = useState<Question | null>(null);
  const [loading, setLoading] = useState(true);
  const [refreshKey, setRefreshKey] = useState(0);
  const [loadError, setLoadError] = useState<{ status: number; message: string } | null>(null);
  const [showTradingModal, setShowTradingModal] = useState(false);
  const [poolState, setPoolState] = useState<PoolStateResponse | null>(null);

  const fetchQuestion = useCallback(async () => {
    try {
      const response = await questionApi.getById(questionId);
      if (response.success && response.data) {
        setQuestion(response.data);
        setLoadError(null); // Reset error on success
      } else {
        setQuestion(null);
        setLoadError({ status: 404, message: 'Question not found.' });
      }
    } catch (error) {
      // Branch by status code if ApiError
      if (error instanceof ApiError) {
        if (error.status === 404) {
          setQuestion(null);
          setLoadError({ status: 404, message: 'Question not found.' });
        } else {
          // 429, 5xx, 408(timeout) etc: Temporary error → Keep existing data
          setLoadError({
            status: error.status,
            message: error.message
          });
          // Keep existing question value (maintain null if initial load)
        }
      } else {
        // Network error or unexpected error
        setLoadError({
          status: 0,
          message: error instanceof Error ? error.message : 'An unknown error occurred.'
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

  // Increment view count (once per tab/session: prevents duplicate calls on StrictMode remount)
  useEffect(() => {
    // Only call when question exists on server
    if (!question) return;
    if (!questionId || isNaN(questionId)) return;
    if (typeof window === 'undefined') return;

    const storageKey = `viewed_${questionId}`;
    if (sessionStorage.getItem(storageKey)) return;

    sessionStorage.setItem(storageKey, 'true');
    questionApi.incrementViewCount(questionId).catch(() => {});
  }, [questionId, question]);

  useEffect(() => {
    if (!question) return;
    const interval = setInterval(() => {
      if (document.visibilityState !== 'visible') return;
      fetchQuestion();
    }, 15000); // Skip inactive tabs, 15-second polling
    return () => clearInterval(interval);
  }, [question, fetchQuestion]);

  // Fetch pool state for AMM questions
  useEffect(() => {
    if (!question?.id) {
      setPoolState(null);
      return;
    }

    const fetchPoolState = async () => {
      try {
        const state = await swapApi.getPoolState(question.id);
        setPoolState(state);
      } catch {
        // Pool doesn't exist for this question (legacy or voting questions)
        setPoolState(null);
      }
    };

    fetchPoolState();
  }, [question?.id, refreshKey]);

  const handleTradeComplete = () => {
    fetchQuestion();
    refreshUser();
    setRefreshKey((k) => k + 1);
  };

  // Swipe gesture for back/forward navigation
  useEffect(() => {
    let touchStartX = 0;
    let touchStartY = 0;
    let touchStartTime = 0;
    let touchEndX = 0;
    let touchEndY = 0;
    let touchEndTime = 0;

    const handleTouchStart = (e: TouchEvent) => {
      touchStartX = e.changedTouches[0].screenX;
      touchStartY = e.changedTouches[0].screenY;
      touchStartTime = Date.now();
    };

    const handleTouchEnd = (e: TouchEvent) => {
      touchEndX = e.changedTouches[0].screenX;
      touchEndY = e.changedTouches[0].screenY;
      touchEndTime = Date.now();
      handleSwipe();
    };

    const handleSwipe = () => {
      const deltaX = touchEndX - touchStartX;
      const deltaY = touchEndY - touchStartY;
      const deltaTime = touchEndTime - touchStartTime;

      const minSwipeDistance = 250; // Minimum swipe distance (increased)
      const maxSwipeTime = 400; // Maximum swipe time (ms) - fast swipes only
      const minVelocity = 0.8; // Minimum swipe velocity (px/ms) - faster
      const maxVerticalDeviation = 50; // Maximum allowed vertical deviation (stricter)

      // If moved too much vertically, consider it as scroll
      if (Math.abs(deltaY) > maxVerticalDeviation) {
        return;
      }

      const velocity = Math.abs(deltaX) / deltaTime;
      const isHorizontalSwipe = Math.abs(deltaX) > Math.abs(deltaY) * 3; // Horizontal:vertical ratio 3:1 or more

      // Conditions: sufficient distance + horizontal swipe + appropriate time + sufficient velocity
      if (
        Math.abs(deltaX) > minSwipeDistance &&
        isHorizontalSwipe &&
        deltaTime < maxSwipeTime &&
        velocity > minVelocity
      ) {
        if (deltaX > 0) {
          // Right swipe: go back
          window.history.back();
        } else {
          // Left swipe: go forward
          window.history.forward();
        }
      }
    };

    document.addEventListener('touchstart', handleTouchStart, { passive: true });
    document.addEventListener('touchend', handleTouchEnd, { passive: true });

    return () => {
      document.removeEventListener('touchstart', handleTouchStart);
      document.removeEventListener('touchend', handleTouchEnd);
    };
  }, []);

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="w-8 h-8 border-4 border-indigo-600 border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  if (!question) {
    // Initial loading failed due to temporary error (429, 5xx, timeout, etc.)
    if (loadError) {
      return (
        <div className={`text-center py-20 ${isDark ? 'text-slate-300' : 'text-slate-700'}`}>
          <AlertTriangle className="mx-auto mb-4 text-amber-500" size={48} />
          <p className="text-lg font-bold mb-2">Unable to load question</p>
          <p className="text-sm text-slate-400 mb-1">
            {loadError.status > 0 ? `HTTP ${loadError.status}` : 'Network Error'}
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
            Retry
          </button>
        </div>
      );
    }

    // Question doesn't exist (404, etc.)
    return (
      <div className="text-center py-20">
        <p className="text-slate-400 text-lg">Market not found.</p>
        <Link href="/marketplace" className="text-indigo-600 text-sm font-bold mt-2 inline-block hover:underline">
          Back to Marketplace
        </Link>
      </div>
    );
  }

  const legacyYesPercent = question.totalBetPool > 0
    ? Math.round((question.yesBetPool / question.totalBetPool) * 100)
    : 50;

  // AMM questions display probability/pool amount based on poolState.currentPrice
  const yesPercent = poolState?.currentPrice?.yes != null
    ? Math.round(poolState.currentPrice.yes * 100)
    : legacyYesPercent;

  const displayTotalPool = poolState?.collateralLocked ?? question.totalBetPool;

  // Determine back link based on question status
  const backLink = question.status === 'VOTING' ? '/vote' : '/marketplace';
  const backLabel = question.status === 'VOTING' ? 'Vote List' : 'Market List';

  const isReadOnlyFallback = false;
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
              {question.status === 'VOTING' ? 'Voting' : question.status === 'BETTING' ? 'Betting' : question.status === 'BREAK' ? 'Break' : question.status === 'SETTLED' ? 'Settled' : question.status}
            </span>
          </div>
          <h1 className={`text-2xl font-black mb-3 ${isDark ? 'text-white' : 'text-slate-900'}`}>{question.title}</h1>
          <div className="flex items-center gap-4 text-sm text-slate-400">
            {question.status !== 'VOTING' && (
              <span className="font-bold">Vol. {question.totalBetPool.toLocaleString()} USDC</span>
            )}
            {(question.matchId || (question.category === 'SPORTS' && question.type === 'VERIFIABLE')) ? (
              question.bettingStartAt && (
                <span className="flex items-center gap-1">
                  <Clock size={14} />
                  Match Time: {new Date(question.bettingStartAt).toLocaleString('en-US')}
                </span>
              )
            ) : (
              question.expiredAt && (
                <span className="flex items-center gap-1">
                  <Clock size={14} />
                  End Time: {new Date(question.expiredAt).toLocaleString('en-US')}
                </span>
              )
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
                Data update failed ({loadError.status > 0 ? `HTTP ${loadError.status}` : 'Network Error'})
              </span>
            </div>
            <button
              onClick={() => {
                setLoadError(null);
                fetchQuestion();
              }}
              className="px-3 py-1 text-xs font-bold rounded-lg bg-rose-600 hover:bg-rose-700 text-white transition"
            >
              Retry
            </button>
          </div>
          <p className="text-xs mt-1 ml-6">{loadError.message}</p>
        </div>
      )}

      {isReadOnlyFallback && (
        <div className={`mb-6 p-4 rounded-2xl border ${
          isDark ? 'bg-amber-950/20 border-amber-900/30 text-amber-400' : 'bg-amber-50 border-amber-200 text-amber-700'
        }`}>
          This question could not be found on the server and is displayed in read-only mode. Real trading/order/activity history API calls are disabled.
        </div>
      )}

      {question.status === 'SETTLED' && question.finalResult && question.disputeDeadline && new Date(question.disputeDeadline) > new Date() && (
        <div className={`mb-6 p-5 rounded-2xl ${isDark ? 'bg-amber-950/20 border border-amber-900/30' : 'bg-amber-50 border border-amber-100'}`}>
          <div className="flex items-center gap-3 mb-2">
            <AlertTriangle className="text-amber-500" size={24} />
            <div>
              <div className="font-black text-amber-500">Settlement Verification (Disputes Allowed)</div>
              <div className="text-sm text-slate-400">
                Expected Result: <span className={`font-black ${question.finalResult === 'YES' ? 'text-emerald-500' : 'text-rose-500'}`}>{question.finalResult}</span>
                <span className="ml-2">
                  · Finalization Deadline: {new Date(question.disputeDeadline).toLocaleString('en-US')}
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
              View Settlement Source
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
              <div className="font-black text-emerald-500">Settlement Complete</div>
              <div className="text-sm text-slate-400">
                Final Result: <span className={`font-black ${question.finalResult === 'YES' ? 'text-emerald-500' : 'text-rose-500'}`}>
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
              View Settlement Source
              <ExternalLink size={12} />
            </a>
          )}
        </div>
      )}

      {isVotingMode ? (
        // VOTING mode layout: centered single column
        <div className="max-w-2xl mx-auto space-y-6">
          {/* Question information card */}
          <div className={`rounded-2xl shadow-sm p-6 ${isDark ? 'bg-slate-900 border border-slate-800' : 'bg-white border border-slate-100'}`}>
            <div className="flex items-center gap-3 mb-4">
              {question.category && (
                <span className={`px-3 py-1 rounded-full text-xs font-bold ${isDark ? 'bg-slate-800 text-slate-400' : 'bg-slate-100 text-slate-600'}`}>
                  {question.category}
                </span>
              )}
              <span className="px-3 py-1 rounded-full text-xs font-bold bg-blue-100 text-blue-700">
                Voting
              </span>
            </div>
            <h1 className={`text-2xl font-bold mb-4 ${isDark ? 'text-white' : 'text-slate-900'}`}>
              {question.title}
            </h1>
            <div className="space-y-2">
              {(question.matchId || (question.category === 'SPORTS' && question.type === 'VERIFIABLE')) ? (
                question.bettingStartAt && (
                  <div className={`flex items-center gap-2 text-sm ${isDark ? 'text-slate-400' : 'text-slate-600'}`}>
                    <Clock size={16} />
                    <span>
                      Match Time: {new Date(question.bettingStartAt).toLocaleString('en-US')}
                    </span>
                  </div>
                )
              ) : (
                question.expiredAt && (
                  <div className={`flex items-center gap-2 text-sm ${isDark ? 'text-slate-400' : 'text-slate-600'}`}>
                    <Clock size={16} />
                    <span>
                      End Time: {new Date(question.expiredAt).toLocaleString('en-US')}
                    </span>
                  </div>
                )
              )}
              {question.votingEndAt && (
                <div className={`flex items-center gap-2 text-sm ${isDark ? 'text-slate-400' : 'text-slate-600'}`}>
                  <Clock size={16} />
                  <span>
                    Voting ends in{' '}
                    {(() => {
                      const now = new Date().getTime();
                      const end = new Date(question.votingEndAt).getTime();
                      const diff = end - now;
                      if (diff <= 0) return 'Ended';
                      const hours = Math.floor(diff / (1000 * 60 * 60));
                      const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
                      return hours > 0 ? `${hours}h ${minutes}m` : `${minutes}m`;
                    })()}
                  </span>
                </div>
              )}
            </div>
          </div>
          {/* Voting panel */}
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
        // Existing betting layout: grid
        <>
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
            {/* Mobile: top / Desktop: right panel */}
            <div className="order-1 lg:order-2 lg:col-span-4">
              {/* Desktop: right fixed panel */}
              <div className="hidden lg:block">
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
                    Trading panel disabled due to missing server market data.
                  </div>
                )}
              </div>

              {/* Mobile: top panel */}
              <div className="lg:hidden">
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
            </div>

            {/* Mobile: bottom / Desktop: left main content */}
	            <div className="order-2 lg:order-1 lg:col-span-8 space-y-6 pb-20 lg:pb-0">
	              <ProbabilityChart
	                yesPercent={yesPercent}
	                totalPool={displayTotalPool}
	                yesPool={question.yesBetPool}
	                noPool={question.noBetPool}
	                questionId={question.id}
	                disableApi={isReadOnlyFallback}
	                executionModel={question.executionModel}
	                ammPool={poolState ? {
	                  collateralLocked: Number(poolState.collateralLocked),
	                  yesShares: Number(poolState.yesShares),
	                  noShares: Number(poolState.noShares),
	                  totalVolumeUsdc: Number(poolState.totalVolumeUsdc),
	                  version: poolState.version
	                } : null}
              />
              {!isReadOnlyFallback && (
                <ActivityFeed
                  questionId={question.id}
                  refreshKey={refreshKey}
                  executionModel={question.executionModel}
                />
              )}
              {!isReadOnlyFallback && user && (
                <MyBetsPanel
                  questionId={question.id}
                  executionModel={question.executionModel}
                />
              )}
            </div>
          </div>

          {/* Mobile: bottom fixed buttons */}
	          {user && !isReadOnlyFallback && (
	            <div className="lg:hidden fixed bottom-0 left-0 right-0 z-50">
	              <div className={`grid grid-cols-2 gap-2 p-4 border-t ${
	                isDark ? 'bg-slate-950/95 border-slate-800 backdrop-blur-xl' : 'bg-white/95 border-slate-200 backdrop-blur-xl'
	              }`}>
                <button
                  onClick={() => setShowTradingModal(true)}
	                  className="py-3 px-4 rounded-xl font-bold text-sm bg-emerald-500 hover:bg-emerald-600 text-white transition"
	                >
	                  Buy {yesPercent}%
	                </button>
                <button
                  onClick={() => setShowTradingModal(true)}
	                  className="py-3 px-4 rounded-xl font-bold text-sm bg-rose-500 hover:bg-rose-600 text-white transition"
	                >
	                  Sell {100 - yesPercent}%
	                </button>
	              </div>
	            </div>
	          )}

          {/* Mobile: TradingPanel modal */}
          {showTradingModal && user && (
            <div className="lg:hidden fixed inset-0 z-[100] flex items-end animate-fade-in">
              <div
                className="absolute inset-0 bg-black/50 backdrop-blur-sm"
                onClick={() => setShowTradingModal(false)}
              />
              <div
                className={`relative w-full rounded-t-3xl overflow-hidden transform transition-transform duration-300 ease-out ${
                  isDark ? 'bg-slate-900' : 'bg-white'
                }`}
                style={{ animation: 'slideUp 0.3s ease-out' }}
              >
                <div className="max-h-[85vh] overflow-y-auto">
                  <TradingPanel
                    question={question}
                    user={user}
                    onTradeComplete={() => {
                      handleTradeComplete();
                      setShowTradingModal(false);
                    }}
                    votedChoice={getChoice(question.id)}
                    onVoted={(choice) => markVoted(question.id, choice)}
                  />
                </div>
              </div>
            </div>
          )}

          <style jsx>{`
            @keyframes slideUp {
              from {
                transform: translateY(100%);
              }
              to {
                transform: translateY(0);
              }
            }
          `}</style>
        </>
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
