'use client';

import { useState, useEffect } from 'react';
import { CheckCircle, UserPlus, Clock, Shield, AlertTriangle, Ticket } from 'lucide-react';
import { useTheme } from '@/hooks/useTheme';
import { useToast } from '@/components/ui/Toast';
import { useAuth } from '@/hooks/useAuth';
import { useRegisterModal } from '@/components/RegisterModal';
import { bettingApi, orderApi, ticketApi, ApiError, getMyPositionsByQuestion } from '@/lib/api';
import type { PositionData, OrderData } from '@/lib/api';
import { BET_MIN_USDC, BET_MAX_USDC } from '@/lib/contracts';
import DepositModal from '@/components/payment/DepositModal';
import type { TicketStatus } from '@/lib/api/ticket';
import {
  generateSalt,
  generateCommitHash,
  saveSaltToStorage,
  getSaltFromStorage,
  removeSaltFromStorage,
  hasSaltInStorage,
} from '@/lib/voteCommit';
import type { Question, Member } from '@/types/api';

interface TradingPanelProps {
  question: Question;
  user: Member;
  onTradeComplete: () => void;
  votedChoice?: 'YES' | 'NO';
  onVoted?: (choice: 'YES' | 'NO') => void;
}

export default function TradingPanel({ question, user, onTradeComplete, votedChoice, onVoted }: TradingPanelProps) {
  const { isDark } = useTheme();
  const { showToast } = useToast();
  const { isGuest, refreshUser } = useAuth();
  const { open: openRegister } = useRegisterModal();
  const [tradeAmount, setTradeAmount] = useState('');
  const [selectedOutcome, setSelectedOutcome] = useState<'YES' | 'NO'>('YES');
  const [activeTab, setActiveTab] = useState<'buy' | 'sell'>('buy');
  const [orderType, setOrderType] = useState<'market' | 'limit'>('market');
  const [limitPrice, setLimitPrice] = useState('');
  const [loading, setLoading] = useState(false);
  const [showDepositModal, setShowDepositModal] = useState(false);
  const [showAdvanced, setShowAdvanced] = useState(false);
  const [hasCommitted, setHasCommitted] = useState(false);
  const [committedChoice, setCommittedChoice] = useState<'YES' | 'NO' | null>(null);
  const [ticketStatus, setTicketStatus] = useState<TicketStatus | null>(null);
  const [positions, setPositions] = useState<PositionData[]>([]);
  const [openSellOrders, setOpenSellOrders] = useState<OrderData[]>([]);
  const [isMobilePanelOpen, setIsMobilePanelOpen] = useState(false);
  const [isMobile, setIsMobile] = useState(false);

  const isExternalSportsMatch = Boolean(question.matchId);

  // Detect mobile screen
  useEffect(() => {
    const checkMobile = () => {
      const mobile = window.innerWidth < 768;
      console.log('Mobile detection:', mobile, 'Width:', window.innerWidth);
      setIsMobile(mobile);
    };
    checkMobile();
    window.addEventListener('resize', checkMobile);
    return () => window.removeEventListener('resize', checkMobile);
  }, []);

  // Refresh positions and orders (for SELL tab)
  const refreshPositionsAndOrders = async () => {
    if (user?.id && question.status === 'BETTING') {
      try {
        const [posData, orderData] = await Promise.all([
          getMyPositionsByQuestion(question.id),
          orderApi.getOrdersByQuestion(question.id)
        ]);
        setPositions(posData);
        const sellOrders = orderData.filter(
          o => o.direction === 'SELL' && (o.status === 'OPEN' || o.status === 'PARTIAL')
        );
        setOpenSellOrders(sellOrders);
      } catch (error) {
        console.error('Failed to refresh positions/orders:', error);
      }
    }
  };

  // Check if user has already committed (salt exists in localStorage)
  useEffect(() => {
    if (question.id && user?.id) {
      const hasSalt = hasSaltInStorage(question.id);
      setHasCommitted(hasSalt);
      // Try to retrieve committed choice from localStorage (if stored)
      const storedChoice = localStorage.getItem(`vote_choice_${question.id}`);
      if (storedChoice === 'YES' || storedChoice === 'NO') {
        setCommittedChoice(storedChoice);
      }
    }
  }, [question.id, user?.id]);

  // Fetch ticket status for voting phase
  useEffect(() => {
    if (question.status === 'VOTING' && user?.id) {
      ticketApi.getStatus().then(setTicketStatus).catch(() => setTicketStatus(null));
    }
  }, [question.status, user?.id]);

  // Fetch positions and open sell orders for SELL tab
  useEffect(() => {
    if (activeTab === 'sell' && user?.id && question.status === 'BETTING') {
      // Fetch positions
      getMyPositionsByQuestion(question.id).then(setPositions).catch(() => setPositions([]));

      // Fetch open sell orders
      orderApi.getOrdersByQuestion(question.id).then(orders => {
        const sellOrders = orders.filter(
          o => o.direction === 'SELL' && (o.status === 'OPEN' || o.status === 'PARTIAL')
        );
        setOpenSellOrders(sellOrders);
      }).catch(() => setOpenSellOrders([]));
    }
  }, [activeTab, user?.id, question.id, question.status]);

  const yesOdds = question.totalBetPool > 0
    ? Math.round((question.yesBetPool / question.totalBetPool) * 100)
    : 50;

  const handleTrade = async () => {
    if (!tradeAmount || parseFloat(tradeAmount) <= 0) {
      showToast('금액을 입력해주세요', 'error');
      return;
    }

    // SELL 임시 비활성화: UI 우회 호출 방지
    if (activeTab === 'sell') {
      showToast('매도 기능은 준비 중입니다. 현재는 매수(BUY)만 가능합니다.', 'error');
      return;
    }

    const amt = parseFloat(tradeAmount);
    if (amt < BET_MIN_USDC || amt > BET_MAX_USDC) {
      showToast(`베팅 금액은 ${BET_MIN_USDC}~${BET_MAX_USDC} $ 범위여야 합니다.`, 'error');
      return;
    }

    // BUY: 잔액 부족 시 충전 모달
    // SELL: 포지션 체크
    if (activeTab === 'buy') {
      // LIMIT이면 지정가 기준, MARKET이면 최악(0.99) 기준으로 보수적으로 체크
      const priceForCheck =
        orderType === 'limit' && limitPrice && !isNaN(parseFloat(limitPrice))
          ? parseFloat(limitPrice)
          : 0.99;
      const required = amt * priceForCheck;
      if (user.usdcBalance < required) {
        setShowDepositModal(true);
        return;
      }
    } else {
      // SELL: 보유 수량 체크
      const currentPosition = positions.find(p => p.side === selectedOutcome);
      const holdingQty = currentPosition?.quantity || 0;
      const pendingSellQty = openSellOrders
        .filter(o => o.side === selectedOutcome)
        .reduce((sum, o) => sum + o.remainingAmount, 0);
      const availableToSell = Math.max(0, holdingQty - pendingSellQty);

      if (holdingQty === 0) {
        showToast(`${selectedOutcome} 포지션이 없습니다. 먼저 구매해주세요.`, 'error');
        return;
      }

      if (amt > availableToSell) {
        showToast(`판매 가능 수량을 초과합니다. (판매 가능: ${availableToSell}개)`, 'error');
        return;
      }
    }

    // Limit Order인 경우 별도 처리
    if (orderType === 'limit') {
      await handleLimitOrder();
      return;
    }

    setLoading(true);
    try {
      // BUY/SELL 모두 주문 모델로 통일
      const result = await orderApi.createOrder({
        questionId: question.id,
        side: selectedOutcome,
        // MARKET 주문은 price 불필요 (서버가 최적가로 체결)
        amount: Number(tradeAmount),
        direction: activeTab === 'buy' ? 'BUY' : 'SELL',
        orderType: 'MARKET',
      });
      if (result.success) {
        const action = activeTab === 'buy' ? '구매' : '매도';
        showToast(`${selectedOutcome} ${action} 완료! ${result.filledAmount > 0 ? `(${result.filledAmount} $ 체결)` : ''}`);
        setTradeAmount('');
        await refreshUser();
        await refreshPositionsAndOrders();
        onTradeComplete();
      }
    } catch (error) {
      if (error instanceof ApiError) {
        showToast(error.message, 'error');
      } else {
        showToast(activeTab === 'buy' ? '베팅 중 오류가 발생했습니다.' : '매도 중 오류가 발생했습니다.', 'error');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleLimitOrder = async () => {
    if (!limitPrice || parseFloat(limitPrice) <= 0 || parseFloat(limitPrice) >= 1) {
      showToast('유효한 가격을 입력해주세요 (0.01 ~ 0.99)', 'error');
      return;
    }

    setLoading(true);
    try {
      const result = await orderApi.createOrder({
        questionId: question.id,
        side: selectedOutcome,
        price: parseFloat(limitPrice),
        amount: Number(tradeAmount),
        direction: activeTab === 'buy' ? 'BUY' : 'SELL',
      });

      if (result.success) {
        const actionLabel = activeTab === 'buy' ? '매수' : '매도';
        if (result.filledAmount === Number(tradeAmount)) {
          showToast(`${actionLabel} 주문이 완전 체결되었습니다!`);
        } else if (result.filledAmount > 0) {
          showToast(`${actionLabel} 부분 체결: ${result.filledAmount}/${tradeAmount} $`);
        } else {
          showToast(`${actionLabel} 주문이 오더북에 등록되었습니다.`);
        }
        setTradeAmount('');
        setLimitPrice('');
        await refreshUser();
        await refreshPositionsAndOrders();
        onTradeComplete();
      }
    } catch (error) {
      if (error instanceof ApiError) {
        showToast(error.message, 'error');
      } else {
        showToast('주문 중 오류가 발생했습니다.', 'error');
      }
    } finally {
      setLoading(false);
    }
  };

  /**
   * 1단계: Commit (투표 내용 암호화하여 제출)
   */
  const handleVoteCommit = async (choice: 'YES' | 'NO') => {
    setLoading(true);
    try {
      // 1. salt 생성
      const salt = generateSalt();

      // 2. commitHash 생성: SHA-256(questionId:memberId:choice:salt)
      const commitHash = await generateCommitHash(question.id, user.id, choice, salt);

      // 3. salt를 localStorage에 저장 (reveal 시 사용)
      saveSaltToStorage(question.id, salt);
      localStorage.setItem(`vote_choice_${question.id}`, choice);

      // 4. 서버에 commitHash만 전송 (choice, salt 미전송)
      const result = await bettingApi.voteCommit({
        questionId: question.id,
        commitHash,
      });

      if (result.success) {
        showToast(`${choice} 투표 커밋 완료! 공개 시간에 다시 방문하세요.`, 'vote');
        setHasCommitted(true);
        setCommittedChoice(choice);
        onVoted?.(choice);

        // Update ticket status from response
        if (result.remainingTickets !== undefined) {
          setTicketStatus(prev => prev ? { ...prev, remainingTickets: result.remainingTickets! } : null);
        }

        await refreshUser();
        onTradeComplete();
      }
    } catch (error) {
      if (error instanceof ApiError) {
        showToast(error.message, 'error');
      } else {
        showToast('투표 커밋 중 오류가 발생했습니다.', 'error');
      }
    } finally {
      setLoading(false);
    }
  };

  /**
   * 2단계: Reveal (투표 내용 공개)
   */
  const handleVoteReveal = async () => {
    if (!committedChoice) {
      showToast('커밋된 투표를 찾을 수 없습니다.', 'error');
      return;
    }

    // localStorage에서 salt 조회
    const salt = getSaltFromStorage(question.id);
    if (!salt) {
      showToast('투표 데이터가 유실되었습니다. 이 브라우저에서 투표한 경우에만 공개할 수 있습니다.', 'error');
      return;
    }

    setLoading(true);
    try {
      const result = await bettingApi.voteReveal({
        questionId: question.id,
        choice: committedChoice,
        salt,
      });

      if (result.success) {
        showToast(`${committedChoice} 투표 공개 완료!`, 'vote');
        // localStorage에서 salt와 choice 삭제
        removeSaltFromStorage(question.id);
        localStorage.removeItem(`vote_choice_${question.id}`);
        onVoted?.(committedChoice);
        await refreshUser();
        onTradeComplete();
      }
    } catch (error) {
      if (error instanceof ApiError) {
        showToast(error.message, 'error');
      } else {
        showToast('투표 공개 중 오류가 발생했습니다.', 'error');
      }
    } finally {
      setLoading(false);
    }
  };

  // Legacy: 기존 Activity 기반 투표 (fallback)
  const handleVoteLegacy = async (choice: 'YES' | 'NO') => {
    setLoading(true);
    try {
      const result = await bettingApi.vote({
        memberId: user.id,
        questionId: question.id,
        choice,
        latencyMs: Math.floor(Math.random() * 10000) + 2000,
      });
      if (result.success) {
        showToast(`${choice} 투표 완료!`, 'vote');
        onVoted?.(choice);
        await refreshUser();
        onTradeComplete();
      }
    } catch (error) {
      if (error instanceof ApiError) {
        showToast(error.message, 'error');
      } else {
        showToast('투표 중 오류가 발생했습니다.', 'error');
      }
    } finally {
      setLoading(false);
    }
  };

  const handlePresetAdd = (preset: number) => {
    const currentAmount = parseFloat(tradeAmount) || 0;
    const nextAmount = Math.min(currentAmount + preset, BET_MAX_USDC);
    setTradeAmount(String(nextAmount));
  };

  // Polymarket 스타일 수익 계산
  const tradeEstimates = tradeAmount && parseFloat(tradeAmount) > 0
    ? (() => {
        const amount = parseFloat(tradeAmount);
        const avgPrice = (selectedOutcome === 'YES' ? yesOdds : 100 - yesOdds) / 100;

        if (avgPrice <= 0 || avgPrice >= 1) {
          return { avgPrice: 0, shares: 0, toWin: 0, profit: 0 };
        }

        if (activeTab === 'buy') {
          const shares = amount / avgPrice;
          const toWin = shares * 1.0; // 정답 시 $1 정산
          const profit = toWin - amount;
          return { avgPrice, shares, toWin, profit };
        } else {
          // Sell: 반대 포지션 수익 추정
          const oppositeOdds = selectedOutcome === 'YES' ? (100 - yesOdds) : yesOdds;
          const oppositePrice = oppositeOdds / 100;
          if (oppositePrice <= 0) return { avgPrice: 0, shares: 0, toWin: 0, profit: 0 };
          const shares = amount / oppositePrice;
          const toWin = shares * 1.0;
          const profit = toWin - amount;
          return { avgPrice: oppositePrice, shares, toWin, profit };
        }
      })()
    : null;

  // Check if voting period has expired
  const isVotingExpired = question.votingEndAt && new Date(question.votingEndAt) < new Date();

  // Render trading panel content (shared between mobile and desktop)
  const renderTradingPanelContent = () => {
    // 포지션 체크 (Sell 탭 활성화 조건)
    const hasAnyPosition = positions.length > 0 && positions.some(p => p.quantity > 0);

    // SELL 임시 비활성화: 현재 YES BUY ↔ NO BUY 매칭 구조에서 SELL 정상 체결 불가
    const sellTemporarilyDisabled = true;

    return (
      <>
        <div className={`flex border-b mb-4 ${isDark ? 'border-slate-800' : 'border-slate-100'}`}>
          <button
            onClick={() => setActiveTab('buy')}
            className={`flex-1 pb-4 font-black text-sm transition-all ${activeTab === 'buy' ? 'border-b-4 border-indigo-600 text-indigo-600' : 'text-slate-400 hover:text-slate-300'}`}
          >
            Buy
          </button>
          <button
            onClick={() => {
              if (!sellTemporarilyDisabled && hasAnyPosition) {
                setActiveTab('sell');
              }
            }}
            disabled={sellTemporarilyDisabled || !hasAnyPosition}
            className={`flex-1 pb-4 font-black text-sm transition-all ${
              activeTab === 'sell'
                ? 'border-b-4 border-rose-500 text-rose-500'
                : sellTemporarilyDisabled
                  ? 'text-slate-600 opacity-40 cursor-not-allowed'
                  : hasAnyPosition
                    ? 'text-slate-400 hover:text-slate-300'
                    : 'text-slate-600 opacity-40 cursor-not-allowed'
            }`}
          >
            Sell {sellTemporarilyDisabled ? '(준비 중)' : !hasAnyPosition ? '(No position)' : ''}
          </button>
        </div>

        {/* SELL 임시 비활성화 안내 */}
        {sellTemporarilyDisabled && (
          <div className={`mb-4 px-4 py-3 rounded-xl flex items-center gap-2 text-xs ${
            isDark ? 'bg-amber-950/20 border border-amber-900/30 text-amber-400' : 'bg-amber-50 border border-amber-200 text-amber-700'
          }`}>
            <AlertTriangle size={14} />
            <span>매도 기능은 현재 준비 중입니다. 포지션은 만기 정산 시 자동 지급됩니다.</span>
          </div>
        )}

        {/* Advanced 토글 */}
        <div className="mb-4">
          <button
            onClick={() => {
              setShowAdvanced(!showAdvanced);
              if (!showAdvanced) {
                setOrderType('limit');
              } else {
                setOrderType('market');
                setLimitPrice('');
              }
            }}
            className={`w-full py-2.5 rounded-xl text-xs font-bold transition-all ${
              showAdvanced
                ? 'bg-indigo-600 text-white'
                : isDark ? 'bg-slate-800 text-slate-400 hover:bg-slate-700' : 'bg-slate-100 text-slate-500 hover:bg-slate-200'
            }`}
          >
            {showAdvanced ? '✓ Advanced (Limit Orders)' : 'Advanced (Limit Orders)'}
          </button>
        </div>

        <div className="grid grid-cols-2 gap-3 mb-6">
          <button
            onClick={() => setSelectedOutcome('YES')}
            className={`p-4 rounded-2xl transition-all border-2 ${
              selectedOutcome === 'YES'
                ? activeTab === 'buy'
                  ? 'bg-emerald-500 text-white border-emerald-600 scale-105 shadow-lg shadow-emerald-500/30'
                  : 'bg-rose-500 text-white border-rose-600 scale-105 shadow-lg shadow-rose-500/30'
                : 'bg-emerald-500/10 border-emerald-500/30 text-emerald-600 hover:bg-emerald-500/20'
            }`}
          >
            <p className="text-[10px] font-black uppercase opacity-60">
              {activeTab === 'buy' ? 'Buy Yes' : 'Sell Yes'}
            </p>
            <p className="text-xl font-black">{yesOdds}&#162;</p>
          </button>
          <button
            onClick={() => setSelectedOutcome('NO')}
            className={`p-4 rounded-2xl transition-all border-2 ${
              selectedOutcome === 'NO'
                ? activeTab === 'buy'
                  ? 'bg-rose-500 text-white border-rose-600 scale-105 shadow-lg shadow-rose-500/30'
                  : 'bg-emerald-500 text-white border-emerald-600 scale-105 shadow-lg shadow-emerald-500/30'
                : isDark
                  ? 'bg-slate-800 border-slate-700 text-slate-400 hover:bg-slate-700'
                  : 'bg-slate-100 border-slate-200 text-slate-500 hover:bg-slate-200'
            }`}
          >
            <p className="text-[10px] font-black uppercase opacity-60">
              {activeTab === 'buy' ? 'Buy No' : 'Sell No'}
            </p>
            <p className="text-xl font-black">{100 - yesOdds}&#162;</p>
          </button>
        </div>

        {activeTab === 'sell' && (() => {
          const currentPosition = positions.find(p => p.side === selectedOutcome);
          const holdingQty = currentPosition?.quantity || 0;

          // Calculate pending sell quantity for the selected side
          const pendingSellQty = openSellOrders
            .filter(o => o.side === selectedOutcome)
            .reduce((sum, o) => sum + o.remainingAmount, 0);

          const availableToSell = Math.max(0, holdingQty - pendingSellQty);

          return (
            <div className="mb-4 space-y-3">
              {/* Position info */}
              <div className={`rounded-2xl p-4 ${isDark ? 'bg-slate-800 border border-slate-700' : 'bg-slate-50 border border-slate-200'}`}>
                <div className="flex justify-between items-center mb-2">
                  <span className="text-xs font-bold text-slate-400 uppercase">보유 포지션</span>
                  <span className={`text-sm font-black ${holdingQty > 0 ? 'text-indigo-500' : 'text-slate-500'}`}>
                    {selectedOutcome} {holdingQty}개
                  </span>
                </div>
                {pendingSellQty > 0 && (
                  <div className="flex justify-between items-center mb-2">
                    <span className="text-xs text-slate-400">판매 대기</span>
                    <span className="text-sm font-bold text-amber-500">{pendingSellQty}개</span>
                  </div>
                )}
                <div className="flex justify-between items-center pt-2 border-t border-slate-200 dark:border-slate-700">
                  <span className="text-xs font-bold text-slate-400 uppercase">판매 가능</span>
                  <span className={`text-base font-black ${availableToSell > 0 ? 'text-green-500' : 'text-red-500'}`}>
                    {availableToSell}개
                  </span>
                </div>
              </div>

              {holdingQty === 0 && (
                <div className={`rounded-2xl p-3 text-xs flex items-center gap-2 ${isDark ? 'bg-rose-950/20 border border-rose-900/30 text-rose-400' : 'bg-rose-50 border border-rose-200 text-rose-700'}`}>
                  <AlertTriangle size={14} />
                  <span>보유 포지션이 없습니다. 먼저 {selectedOutcome} 포지션을 구매해주세요.</span>
                </div>
              )}

              {availableToSell < holdingQty && availableToSell > 0 && (
                <div className={`rounded-2xl p-3 text-xs ${isDark ? 'bg-amber-950/20 border border-amber-900/30 text-amber-400' : 'bg-amber-50 border border-amber-200 text-amber-700'}`}>
                  판매 대기 중인 주문이 있습니다. 추가 판매는 {availableToSell}개까지 가능합니다.
                </div>
              )}
            </div>
          );
        })()}

        {/* Limit Order 가격 입력 (Advanced 모드일 때만) */}
        {showAdvanced && (
          <div className="mb-4">
            <label className="text-xs font-bold text-slate-400 uppercase mb-2 block">Limit Price</label>
            <div className="relative">
              <input
                type="number"
                step="0.01"
                min="0.01"
                max="0.99"
                value={limitPrice}
                onChange={(e) => setLimitPrice(e.target.value)}
                placeholder="0.50"
                className={`w-full p-4 rounded-2xl border-2 font-bold text-lg transition-all ${
                  isDark
                    ? 'bg-slate-800 border-slate-700 text-white focus:border-indigo-500'
                    : 'bg-slate-50 border-slate-200 text-slate-900 focus:border-indigo-500'
                }`}
              />
              <span className="absolute right-4 top-1/2 -translate-y-1/2 text-slate-400 font-bold">$</span>
            </div>
            <div className="flex space-x-2 mt-2">
              {[0.25, 0.50, 0.75].map(price => (
                <button
                  key={price}
                  onClick={() => setLimitPrice(price.toFixed(2))}
                  className={`flex-1 py-1.5 rounded-xl text-xs font-bold transition-all ${isDark ? 'bg-slate-800 hover:bg-indigo-600 hover:text-white' : 'bg-slate-100 hover:bg-indigo-600 hover:text-white'}`}
                >
                  ${price.toFixed(2)}
                </button>
              ))}
            </div>
          </div>
        )}

        <div className="mb-6">
          <label className="text-xs font-bold text-slate-400 uppercase mb-2 block">Amount</label>
          <div className="relative">
            <input
              type="number"
              value={tradeAmount}
              onChange={(e) => setTradeAmount(e.target.value)}
              placeholder="0"
              className={`w-full p-5 rounded-2xl border-2 font-black text-2xl transition-all ${
                isDark
                  ? 'bg-slate-800 border-slate-700 text-white focus:border-indigo-500'
                  : 'bg-slate-50 border-slate-200 text-slate-900 focus:border-indigo-500'
              }`}
            />
            <span className="absolute right-4 top-1/2 -translate-y-1/2 text-slate-400 font-bold">$</span>
          </div>
          <div className="grid grid-cols-5 gap-2 mt-3">
            {[1, 5, 10, 50, 100].map(amt => (
              <button
                key={amt}
                onClick={() => handlePresetAdd(amt)}
                className={`flex-1 py-2 rounded-xl text-xs font-bold transition-all ${isDark ? 'bg-slate-800 hover:bg-indigo-600 hover:text-white' : 'bg-slate-100 hover:bg-indigo-600 hover:text-white'}`}
              >
                +{amt}$
              </button>
            ))}
          </div>
        </div>

        {tradeEstimates && (
          <div className={`rounded-2xl p-4 mb-6 ${isDark ? 'bg-indigo-950/20 border border-indigo-900/30' : 'bg-indigo-50'}`}>
            <div className="flex justify-between text-sm mb-2">
              <span className="text-slate-500">Avg price</span>
              <span className="font-bold text-indigo-600">
                {Math.round(tradeEstimates.avgPrice * 100)}¢
              </span>
            </div>
            <div className="flex justify-between text-sm mb-2">
              <span className="text-slate-500">Shares</span>
              <span className="font-bold text-slate-700 dark:text-slate-300">
                {tradeEstimates.shares.toFixed(2)}
              </span>
            </div>
            <div className="flex justify-between text-sm mb-2">
              <span className="text-slate-500">To win</span>
              <span className="font-bold text-emerald-600">
                ${tradeEstimates.toWin.toFixed(2)}
              </span>
            </div>
            <div className="flex justify-between text-xs pt-2 border-t border-slate-200 dark:border-slate-700">
              <span className="text-slate-400">Profit if correct</span>
              <span className={`font-bold ${tradeEstimates.profit > 0 ? 'text-emerald-600' : 'text-slate-500'}`}>
                {tradeEstimates.profit > 0 ? '+' : ''}${tradeEstimates.profit.toFixed(2)}
              </span>
            </div>
          </div>
        )}

        {isGuest ? (
          <button
            onClick={openRegister}
            className="w-full py-4 rounded-2xl font-black text-lg transition-all shadow-xl active:scale-95 bg-indigo-600 text-white hover:bg-indigo-700 flex items-center justify-center gap-2"
          >
            <UserPlus size={20} />
            회원가입 후 참여 가능
          </button>
        ) : (() => {
          // Calculate available to sell for button state
          const currentPosition = positions.find(p => p.side === selectedOutcome);
          const holdingQty = currentPosition?.quantity || 0;
          const pendingSellQty = openSellOrders
            .filter(o => o.side === selectedOutcome)
            .reduce((sum, o) => sum + o.remainingAmount, 0);
          const availableToSell = Math.max(0, holdingQty - pendingSellQty);

          const isDisabled = loading || !tradeAmount || (activeTab === 'sell' && availableToSell === 0);

          return (
            <button
              onClick={handleTrade}
              disabled={isDisabled}
              className={`w-full py-4 rounded-2xl font-black text-lg transition-all shadow-xl active:scale-95 disabled:opacity-50 ${
                activeTab === 'buy'
                  ? selectedOutcome === 'YES'
                    ? 'bg-emerald-500 text-white shadow-emerald-500/20 hover:bg-emerald-600'
                    : 'bg-rose-500 text-white shadow-rose-500/20 hover:bg-rose-600'
                  : 'bg-orange-500 text-white shadow-orange-500/20 hover:bg-orange-600'
              }`}
            >
              {loading ? '처리 중...' : orderType === 'limit'
                ? (activeTab === 'buy'
                  ? `Place ${selectedOutcome} Limit Order`
                  : `Place ${selectedOutcome} Sell Hedge Order`)
                : activeTab === 'buy' ? `Buy ${selectedOutcome}` : `Sell ${selectedOutcome}`}
            </button>
          );
        })()}
      </>
    );
  };

  // Render different UI based on question status and voting phase
  if (question.status === 'VOTING' && !isVotingExpired) {
    const votingPhase = question.votingPhase || 'VOTING_COMMIT_OPEN';
    const isCommitPhase = votingPhase === 'VOTING_COMMIT_OPEN';
    const isRevealPhase = votingPhase === 'VOTING_REVEAL_OPEN';

    return (
      <div className={`p-6 rounded-2xl border ${isDark ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-100 shadow-sm'}`}>
        {/* 단계 표시 배지 */}
        {isCommitPhase && (
          <div className="mb-6 inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-blue-100 text-blue-700">
            <Shield size={14} />
            <span className="text-sm font-bold">투표 접수 중</span>
          </div>
        )}
        {isRevealPhase && (
          <div className="mb-6 inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-green-100 text-green-700">
            <Shield size={14} />
            <span className="text-sm font-bold">투표 공개 중</span>
          </div>
        )}

        {isExternalSportsMatch && (
          <div className={`mb-6 px-4 py-2 rounded-xl text-xs font-bold ${
            isDark ? 'bg-slate-800 text-slate-300' : 'bg-slate-50 text-slate-700'
          }`}>
            홈팀 승리를 예상하면 YES, 승리하지 못할 것 같으면 NO를 선택하세요.
          </div>
        )}

        {/* 티켓 상태 표시 */}
        {ticketStatus && (
          <div className={`mb-6 px-4 py-3 rounded-xl flex items-center gap-3 ${
            ticketStatus.remainingTickets === 0
              ? isDark ? 'bg-red-950 border border-red-800' : 'bg-red-50 border border-red-200'
              : isDark ? 'bg-indigo-950 border border-indigo-800' : 'bg-indigo-50 border border-indigo-200'
          }`}>
            <Ticket className={`w-4 h-4 ${
              ticketStatus.remainingTickets === 0
                ? 'text-red-500'
                : isDark ? 'text-indigo-400' : 'text-indigo-600'
            }`} />
            <p className={`text-sm font-bold ${
              ticketStatus.remainingTickets === 0
                ? isDark ? 'text-red-400' : 'text-red-600'
                : isDark ? 'text-indigo-400' : 'text-indigo-600'
            }`}>
              {ticketStatus.remainingTickets === 0
                ? '오늘 투표 가능 횟수를 모두 사용했습니다'
                : `남은 투표권: ${ticketStatus.remainingTickets}/${ticketStatus.maxTickets}`
              }
            </p>
          </div>
        )}

        {isGuest ? (
          <div className={`text-center py-8 rounded-xl ${isDark ? 'bg-slate-800/50' : 'bg-slate-50'}`}>
            <p className={`text-sm ${isDark ? 'text-slate-400' : 'text-slate-500'}`}>회원가입 후 투표에 참여할 수 있습니다</p>
            <button
              onClick={openRegister}
              className="mt-4 px-6 py-3 rounded-xl font-black text-sm transition-all bg-indigo-600 text-white hover:bg-indigo-700 flex items-center justify-center gap-2 mx-auto"
            >
              <UserPlus size={16} />
              회원가입
            </button>
          </div>
        ) : isCommitPhase ? (
          // Commit Phase: Show vote buttons
          hasCommitted && committedChoice ? (
            <div>
              <div className={`flex items-center justify-center gap-2 py-8 rounded-xl ${
                committedChoice === 'YES'
                  ? isDark ? 'bg-emerald-500/10 border border-emerald-500/30' : 'bg-emerald-50 border border-emerald-200'
                  : isDark ? 'bg-rose-500/10 border border-rose-500/30' : 'bg-rose-50 border border-rose-200'
              }`}>
                <CheckCircle size={20} className={committedChoice === 'YES' ? 'text-emerald-500' : 'text-rose-500'} />
                <span className={`font-bold ${committedChoice === 'YES' ? 'text-emerald-500' : 'text-rose-500'}`}>
                  {committedChoice} 투표 커밋 완료
                </span>
              </div>
              <div className={`mt-4 p-3 rounded-xl text-sm flex items-start gap-2 ${isDark ? 'bg-slate-800 text-slate-400' : 'bg-slate-50 text-slate-600'}`}>
                <Shield className="mt-0.5 flex-shrink-0" size={14} />
                <span>선택은 암호화되어 저장되며, 베팅 종료 후 공개됩니다.</span>
              </div>
            </div>
          ) : (
            <div>
              <div className="grid grid-cols-2 gap-4 mb-4">
                <button
                  onClick={() => handleVoteCommit('YES')}
                  disabled={loading || ticketStatus?.remainingTickets === 0}
                  className="bg-green-500 hover:bg-green-600 text-white py-4 rounded-xl text-lg font-bold transition-all disabled:opacity-50"
                >
                  {loading ? '처리 중...' : 'YES 투표'}
                </button>
                <button
                  onClick={() => handleVoteCommit('NO')}
                  disabled={loading || ticketStatus?.remainingTickets === 0}
                  className="bg-red-500 hover:bg-red-600 text-white py-4 rounded-xl text-lg font-bold transition-all disabled:opacity-50"
                >
                  {loading ? '처리 중...' : 'NO 투표'}
                </button>
              </div>
              <div className={`p-3 rounded-xl text-sm flex items-start gap-2 ${isDark ? 'bg-slate-800 text-slate-400' : 'bg-slate-50 text-slate-600'}`}>
                <Shield className="mt-0.5 flex-shrink-0" size={14} />
                <span>선택은 암호화되어 저장되며, 베팅 종료 후 공개됩니다.</span>
              </div>
            </div>
          )
        ) : isRevealPhase ? (
          // Reveal Phase: Show reveal button
          hasCommitted && committedChoice ? (
            <div>
              <div className={`mb-4 p-4 rounded-xl ${isDark ? 'bg-slate-800' : 'bg-slate-50'}`}>
                <p className={`text-sm ${isDark ? 'text-slate-400' : 'text-slate-600'}`}>
                  투표한 선택: <span className={`font-bold ${committedChoice === 'YES' ? 'text-green-600' : 'text-red-600'}`}>{committedChoice}</span>
                </p>
              </div>
              <button
                onClick={handleVoteReveal}
                disabled={loading}
                className="w-full py-4 rounded-xl font-bold text-lg transition-all bg-green-500 hover:bg-green-600 text-white disabled:opacity-50"
              >
                {loading ? '처리 중...' : '투표 공개하기'}
              </button>
            </div>
          ) : (
            <div className={`text-center py-8 rounded-xl ${isDark ? 'bg-slate-800/50 border border-slate-700' : 'bg-slate-50 border border-slate-200'}`}>
              <AlertTriangle size={32} className="mx-auto mb-2 text-amber-500" />
              <p className={`text-sm font-bold ${isDark ? 'text-slate-300' : 'text-slate-700'}`}>투표 데이터 없음</p>
              <p className={`text-xs mt-2 ${isDark ? 'text-slate-400' : 'text-slate-500'}`}>
                이 브라우저에서 투표하지 않았거나 데이터가 유실되었습니다.
              </p>
            </div>
          )
        ) : votedChoice ? (
          // Already revealed
          <div className={`flex items-center justify-center gap-2 py-8 rounded-xl ${
            votedChoice === 'YES'
              ? isDark ? 'bg-emerald-500/10 border border-emerald-500/30' : 'bg-emerald-50 border border-emerald-200'
              : isDark ? 'bg-rose-500/10 border border-rose-500/30' : 'bg-rose-50 border border-rose-200'
          }`}>
            <CheckCircle size={20} className={votedChoice === 'YES' ? 'text-emerald-500' : 'text-rose-500'} />
            <span className={`font-bold ${votedChoice === 'YES' ? 'text-emerald-500' : 'text-rose-500'}`}>
              {votedChoice} 투표 완료
            </span>
          </div>
        ) : (
          // Fallback: Legacy voting (if votingPhase not supported)
          <div className="space-y-3">
            <button
              onClick={() => handleVoteLegacy('YES')}
              disabled={loading}
              className={`w-full py-5 rounded-2xl font-black text-lg transition-all shadow-lg ${isDark ? 'bg-emerald-500/20 text-emerald-400 hover:bg-emerald-500/30 border-2 border-emerald-500/50' : 'bg-emerald-50 text-emerald-600 hover:bg-emerald-100 border-2 border-emerald-200'}`}
            >
              {loading ? '처리 중...' : 'Vote Yes'}
            </button>
            <button
              onClick={() => handleVoteLegacy('NO')}
              disabled={loading}
              className={`w-full py-5 rounded-2xl font-black text-lg transition-all shadow-lg ${isDark ? 'bg-rose-500/20 text-rose-400 hover:bg-rose-500/30 border-2 border-rose-500/50' : 'bg-rose-50 text-rose-600 hover:bg-rose-100 border-2 border-rose-200'}`}
            >
              {loading ? '처리 중...' : 'Vote No'}
            </button>
          </div>
        )}

        {question.votingEndAt && (
          <div className={`mt-6 pt-6 border-t ${isDark ? 'border-slate-800' : 'border-slate-100'}`}>
            <div className="flex justify-between items-center text-sm">
              <span className="text-slate-400">{isRevealPhase ? 'Reveal 종료까지' : '투표 종료까지'}</span>
              <span className="text-indigo-500 font-bold">
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
          </div>
        )}
      </div>
    );
  }

  if (question.status === 'BREAK' || (question.status === 'VOTING' && isVotingExpired)) {
    // BREAK status or voting expired: Show message that voting ended and betting will start soon
    return (
      <div className={`p-6 rounded-[2.5rem] border sticky top-24 ${isDark ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-100 shadow-xl'}`}>
        <div className={`text-center py-12 rounded-xl ${isDark ? 'bg-amber-950/20 border border-amber-900/30' : 'bg-amber-50 border border-amber-200'}`}>
          <Clock size={48} className="mx-auto mb-4 text-amber-500" />
          <h3 className={`text-lg font-black mb-2 ${isDark ? 'text-amber-400' : 'text-amber-700'}`}>휴식 시간</h3>
          <p className={`text-sm ${isDark ? 'text-amber-500/70' : 'text-amber-600'}`}>
            투표가 종료되었습니다.<br/>
            잠시 후 베팅이 시작됩니다.
          </p>
        </div>
      </div>
    );
  }

  if (question.status === 'SETTLED') {
    // SETTLED status: Show final result
    return (
      <div className={`p-6 rounded-[2.5rem] border sticky top-24 ${isDark ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-100 shadow-xl'}`}>
        <div className={`text-center py-12 rounded-xl ${isDark ? 'bg-emerald-950/20 border border-emerald-900/30' : 'bg-emerald-50 border border-emerald-200'}`}>
          <CheckCircle size={48} className="mx-auto mb-4 text-emerald-500" />
          <h3 className={`text-lg font-black mb-2 ${isDark ? 'text-emerald-400' : 'text-emerald-700'}`}>정산 완료</h3>
          <p className={`text-sm ${isDark ? 'text-emerald-500/70' : 'text-emerald-600'}`}>
            이 질문의 정산이 완료되었습니다.
          </p>
        </div>
      </div>
    );
  }

  // BETTING status: Show full trading interface
  // 포지션 체크 (Sell 탭 활성화 조건)
  const hasAnyPosition = positions.length > 0 && positions.some(p => p.quantity > 0);

  // SELL 임시 비활성화: 현재 YES BUY ↔ NO BUY 매칭 구조에서 SELL 정상 체결 불가
  const sellTemporarilyDisabled = true;

  // Mobile: Show bottom button and slide-up panel
  if (isMobile) {
    return (
      <>
        {/* Mobile Bottom Fixed Buttons */}
        <div className="fixed bottom-0 left-0 right-0 z-[100] p-4 bg-gradient-to-t from-slate-900 via-slate-900/95 to-transparent pointer-events-none">
          <div className="grid grid-cols-2 gap-3 max-w-md mx-auto pointer-events-auto">
            <button
              onClick={() => {
                console.log('Buy button clicked');
                setSelectedOutcome('YES');
                setActiveTab('buy');
                setIsMobilePanelOpen(true);
              }}
              className="bg-emerald-500 text-white py-4 rounded-2xl font-black text-base shadow-lg shadow-emerald-500/30 active:scale-95 transition-transform"
            >
              Buy {yesOdds}%
            </button>
            <button
              onClick={() => {
                console.log('Sell button clicked');
                setSelectedOutcome('NO');
                setActiveTab('buy');
                setIsMobilePanelOpen(true);
              }}
              className="bg-rose-500 text-white py-4 rounded-2xl font-black text-base shadow-lg shadow-rose-500/30 active:scale-95 transition-transform"
            >
              Sell {100 - yesOdds}%
            </button>
          </div>
        </div>

        {/* Mobile Slide-up Panel */}
        {isMobilePanelOpen && (
          <>
            {/* Backdrop */}
            <div
              className="fixed inset-0 bg-black/50 z-[200] animate-fade-in"
              onClick={() => setIsMobilePanelOpen(false)}
            />

            {/* Panel */}
            <div className={`fixed bottom-0 left-0 right-0 z-[201] max-h-[85vh] overflow-y-auto rounded-t-3xl animate-slide-up ${isDark ? 'bg-slate-900' : 'bg-white'}`}>
              <div className={`sticky top-0 z-10 flex items-center justify-between p-4 border-b bg-inherit ${isDark ? 'border-slate-800' : 'border-slate-100'}`}>
                <h3 className={`text-lg font-black ${isDark ? 'text-slate-200' : 'text-slate-900'}`}>베팅하기</h3>
                <button
                  onClick={() => setIsMobilePanelOpen(false)}
                  className="p-2 rounded-full hover:bg-slate-800 transition-colors"
                >
                  <svg className="w-6 h-6 text-slate-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                  </svg>
                </button>
              </div>

              <div className="p-6">
                {renderTradingPanelContent()}
              </div>
            </div>

            <DepositModal isOpen={showDepositModal} onClose={() => setShowDepositModal(false)} />
          </>
        )}
      </>
    );
  }

  // Desktop: Show traditional sticky panel
  return (
    <div className={`p-6 rounded-[2.5rem] border sticky top-24 ${isDark ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-100 shadow-xl'}`}>
      {renderTradingPanelContent()}
      <DepositModal isOpen={showDepositModal} onClose={() => setShowDepositModal(false)} />
    </div>
  );
}
