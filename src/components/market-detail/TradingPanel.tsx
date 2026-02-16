'use client';

import { useState } from 'react';
import { CheckCircle, UserPlus, Clock } from 'lucide-react';
import { useTheme } from '@/hooks/useTheme';
import { useToast } from '@/components/ui/Toast';
import { useAuth } from '@/hooks/useAuth';
import { useRegisterModal } from '@/components/RegisterModal';
import { bettingApi, orderApi, ApiError } from '@/lib/api';
import { BET_MIN_USDC, BET_MAX_USDC } from '@/lib/contracts';
import DepositModal from '@/components/payment/DepositModal';
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

  const yesOdds = question.totalBetPool > 0
    ? Math.round((question.yesBetPool / question.totalBetPool) * 100)
    : 50;

  const handleTrade = async () => {
    if (!tradeAmount || parseFloat(tradeAmount) <= 0) {
      showToast('금액을 입력해주세요', 'error');
      return;
    }

    const amt = parseFloat(tradeAmount);
    if (amt < BET_MIN_USDC || amt > BET_MAX_USDC) {
      showToast(`베팅 금액은 ${BET_MIN_USDC}~${BET_MAX_USDC} $ 범위여야 합니다.`, 'error');
      return;
    }

    // 잔액 부족 시 충전 모달
    if (user.usdcBalance < amt) {
      setShowDepositModal(true);
      return;
    }

    // Limit Order인 경우 별도 처리
    if (orderType === 'limit') {
      await handleLimitOrder();
      return;
    }

    setLoading(true);
    try {
      if (activeTab === 'buy') {
        const result = await bettingApi.bet({
          memberId: user.id,
          questionId: question.id,
          choice: selectedOutcome,
          amount: Number(tradeAmount),
        });
        if (result.success) {
          showToast(`${selectedOutcome} 구매 완료!`);
          setTradeAmount('');
          await refreshUser();
          onTradeComplete();
        }
      } else {
        // Sell: 반대 포지션에 베팅하여 헤지
        const oppositeChoice = selectedOutcome === 'YES' ? 'NO' : 'YES';
        const result = await bettingApi.bet({
          memberId: user.id,
          questionId: question.id,
          choice: oppositeChoice as 'YES' | 'NO',
          amount: Number(tradeAmount),
        });
        if (result.success) {
          showToast(`${selectedOutcome} 매도(헤지) 완료! (${oppositeChoice} 포지션 구매)`);
          setTradeAmount('');
          await refreshUser();
          onTradeComplete();
        }
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
      const orderSide = activeTab === 'buy'
        ? selectedOutcome
        : (selectedOutcome === 'YES' ? 'NO' : 'YES');

      const result = await orderApi.createOrder({
        questionId: question.id,
        side: orderSide,
        price: parseFloat(limitPrice),
        amount: Number(tradeAmount),
      });

      if (result.success) {
        const actionLabel = activeTab === 'buy' ? '매수' : '매도(헤지)';
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

  const handleVote = async (choice: 'YES' | 'NO') => {
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

  const estimatedProfit = tradeAmount && parseFloat(tradeAmount) > 0
    ? (() => {
        const odds = selectedOutcome === 'YES' ? yesOdds : 100 - yesOdds;
        if (odds <= 0) return '0.00';
        if (activeTab === 'buy') {
          return (parseFloat(tradeAmount) * (100 / odds) - parseFloat(tradeAmount)).toFixed(2);
        } else {
          // Sell: 반대 포지션 수익 추정
          const oppositeOdds = selectedOutcome === 'YES' ? (100 - yesOdds) : yesOdds;
          if (oppositeOdds <= 0) return '0.00';
          return (parseFloat(tradeAmount) * (100 / oppositeOdds) - parseFloat(tradeAmount)).toFixed(2);
        }
      })()
    : null;

  // Check if voting period has expired
  const isVotingExpired = question.votingEndAt && new Date(question.votingEndAt) < new Date();

  // Render different UI based on question status
  if (question.status === 'VOTING' && !isVotingExpired) {
    // VOTING status: Show only voting buttons
    return (
      <div className={`p-6 rounded-[2.5rem] border sticky top-24 ${isDark ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-100 shadow-xl'}`}>
        <p className="text-xs font-bold text-slate-400 uppercase mb-4">무료 투표</p>
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
        ) : votedChoice ? (
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
          <div className="space-y-3">
            <button
              onClick={() => handleVote('YES')}
              disabled={loading}
              className={`w-full py-5 rounded-2xl font-black text-lg transition-all shadow-lg ${isDark ? 'bg-emerald-500/20 text-emerald-400 hover:bg-emerald-500/30 border-2 border-emerald-500/50' : 'bg-emerald-50 text-emerald-600 hover:bg-emerald-100 border-2 border-emerald-200'}`}
            >
              {loading ? '처리 중...' : 'Vote Yes'}
            </button>
            <button
              onClick={() => handleVote('NO')}
              disabled={loading}
              className={`w-full py-5 rounded-2xl font-black text-lg transition-all shadow-lg ${isDark ? 'bg-rose-500/20 text-rose-400 hover:bg-rose-500/30 border-2 border-rose-500/50' : 'bg-rose-50 text-rose-600 hover:bg-rose-100 border-2 border-rose-200'}`}
            >
              {loading ? '처리 중...' : 'Vote No'}
            </button>
          </div>
        )}
        <div className={`mt-6 pt-6 border-t ${isDark ? 'border-slate-800' : 'border-slate-100'}`}>
          <div className="flex justify-between items-center text-sm">
            <span className="text-slate-400">현재 풀 비율</span>
            <div className="flex gap-3">
              <span className="text-emerald-500 font-bold">{yesOdds}% YES</span>
              <span className="text-rose-500 font-bold">{100 - yesOdds}% NO</span>
            </div>
          </div>
        </div>
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
  return (
    <div className={`p-6 rounded-[2.5rem] border sticky top-24 ${isDark ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-100 shadow-xl'}`}>
      <div className={`flex border-b mb-4 ${isDark ? 'border-slate-800' : 'border-slate-100'}`}>
        <button
          onClick={() => setActiveTab('buy')}
          className={`flex-1 pb-4 font-black text-sm transition-all ${activeTab === 'buy' ? 'border-b-4 border-indigo-600 text-indigo-600' : 'text-slate-400 hover:text-slate-300'}`}
        >
          Buy
        </button>
        <button
          onClick={() => setActiveTab('sell')}
          className={`flex-1 pb-4 font-black text-sm transition-all ${activeTab === 'sell' ? 'border-b-4 border-rose-500 text-rose-500' : 'text-slate-400 hover:text-slate-300'}`}
        >
          Sell
        </button>
      </div>

      {/* Market / Limit 선택 */}
      <div className="flex gap-2 mb-6">
        <button
          onClick={() => setOrderType('market')}
          className={`flex-1 py-2 rounded-xl text-xs font-bold transition-all ${
            orderType === 'market'
              ? 'bg-indigo-600 text-white'
              : isDark ? 'bg-slate-800 text-slate-400 hover:bg-slate-700' : 'bg-slate-100 text-slate-500 hover:bg-slate-200'
          }`}
        >
          Market
        </button>
        <button
          onClick={() => setOrderType('limit')}
          className={`flex-1 py-2 rounded-xl text-xs font-bold transition-all ${
            orderType === 'limit'
              ? 'bg-indigo-600 text-white'
              : isDark ? 'bg-slate-800 text-slate-400 hover:bg-slate-700' : 'bg-slate-100 text-slate-500 hover:bg-slate-200'
          }`}
        >
          Limit
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

      {activeTab === 'sell' && (
        <div className={`rounded-2xl p-3 mb-4 text-xs ${isDark ? 'bg-amber-950/20 border border-amber-900/30 text-amber-400' : 'bg-amber-50 border border-amber-200 text-amber-700'}`}>
          매도는 반대 포지션을 구매하여 기존 포지션을 헤지합니다
        </div>
      )}

      {/* Limit Order 가격 입력 */}
      {orderType === 'limit' && (
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

      {estimatedProfit && (
        <div className={`rounded-2xl p-4 mb-6 ${isDark ? 'bg-indigo-950/20 border border-indigo-900/30' : 'bg-indigo-50'}`}>
          <div className="flex justify-between text-sm mb-1">
            <span className="text-slate-500">{activeTab === 'buy' ? '예상 수익' : '헤지 수익'}</span>
            <span className="font-bold text-indigo-600">{'$'}{estimatedProfit}</span>
          </div>
          <div className="flex justify-between text-sm">
            <span className="text-slate-500">예상 수익률</span>
            <span className="font-bold text-indigo-600">
              {parseFloat(tradeAmount) > 0 ? ((parseFloat(estimatedProfit) / parseFloat(tradeAmount)) * 100).toFixed(1) : '0.0'}%
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
      ) : (
        <button
          onClick={handleTrade}
          disabled={loading || !tradeAmount}
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
      )}

      <DepositModal isOpen={showDepositModal} onClose={() => setShowDepositModal(false)} />
    </div>
  );
}
