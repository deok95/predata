'use client';

import { useState, useEffect } from 'react';
import { CheckCircle, UserPlus, Clock, Shield, AlertTriangle, Ticket } from 'lucide-react';
import { useTheme } from '@/hooks/useTheme';
import { useToast } from '@/components/ui/Toast';
import { useAuth } from '@/hooks/useAuth';
import { useRegisterModal } from '@/components/RegisterModal';
import { bettingApi, ticketApi, ApiError } from '@/lib/api';
import { swapApi } from '@/lib/api/swap';
import type { PoolStateResponse, SwapSimulationResponse, MySharesSnapshot } from '@/lib/api/swap';
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
  const hasVotingPass = Boolean(user?.hasVotingPass);
  const [tradeAmount, setTradeAmount] = useState('');
  const [selectedOutcome, setSelectedOutcome] = useState<'YES' | 'NO'>('YES');
  const [activeTab, setActiveTab] = useState<'buy' | 'sell'>('buy');
  const [loading, setLoading] = useState(false);
  const [showDepositModal, setShowDepositModal] = useState(false);
  const [showAdvanced, setShowAdvanced] = useState(false);
  const [hasCommitted, setHasCommitted] = useState(false);
  const [committedChoice, setCommittedChoice] = useState<'YES' | 'NO' | null>(null);
  const [ticketStatus, setTicketStatus] = useState<TicketStatus | null>(null);
  const [isMobilePanelOpen, setIsMobilePanelOpen] = useState(false);
  const [isMobile, setIsMobile] = useState(false);

  // AMM Swap states
  const [poolState, setPoolState] = useState<PoolStateResponse | null>(null);
  const [simulation, setSimulation] = useState<SwapSimulationResponse | null>(null);
  const [simulationLoading, setSimulationLoading] = useState(false);
  const [myShares, setMyShares] = useState<MySharesSnapshot | null>(null);

  const isExternalSportsMatch = Boolean(question.matchId);

  // Detect mobile screen
  useEffect(() => {
    const checkMobile = () => {
      setIsMobile(window.innerWidth < 768);
    };
    checkMobile();
    window.addEventListener('resize', checkMobile);
    return () => window.removeEventListener('resize', checkMobile);
  }, []);

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

  // Fetch pool state and my shares (BETTING mode)
  useEffect(() => {
    if (question.status === 'BETTING') {
      swapApi.getPoolState(question.id)
        .then(setPoolState)
        .catch(() => setPoolState(null));

      if (user?.id && !isGuest) {
        swapApi.getMyShares(question.id)
          .then(response => setMyShares(response))
          .catch(() => setMyShares(null));
      }
    }
  }, [question.id, question.status, user?.id, isGuest]);

  // Simulate swap when amount changes (debounced)
  useEffect(() => {
    if (!tradeAmount || parseFloat(tradeAmount) <= 0 || !poolState) {
      setSimulation(null);
      return;
    }

    const amt = parseFloat(tradeAmount);
    if (amt < BET_MIN_USDC || amt > BET_MAX_USDC) {
      setSimulation(null);
      return;
    }

    const timer = setTimeout(() => {
      setSimulationLoading(true);
      swapApi.simulateSwap({
        questionId: question.id,
        action: activeTab === 'buy' ? 'BUY' : 'SELL',
        outcome: selectedOutcome,
        amount: amt,
      })
        .then(response => {
          setSimulation(response);
          setSimulationLoading(false);
        })
        .catch(error => {
          console.error('Simulation failed:', error);
          setSimulation(null);
          setSimulationLoading(false);
        });
    }, 300);

    return () => clearTimeout(timer);
  }, [tradeAmount, selectedOutcome, activeTab, question.id, poolState]);

  const yesOdds = question.totalBetPool > 0
    ? Math.round((question.yesBetPool / question.totalBetPool) * 100)
    : 50;

  const handleTrade = async () => {
    if (isGuest) {
      showToast('Login required', 'error');
      return;
    }

    if (!tradeAmount || parseFloat(tradeAmount) <= 0) {
      showToast('Please enter an amount', 'error');
      return;
    }

    if (!poolState) {
      showToast('Loading pool information. Please try again later.', 'error');
      return;
    }

    const amt = parseFloat(tradeAmount);
    if (amt < BET_MIN_USDC || amt > BET_MAX_USDC) {
      showToast(`Bet amount must be between ${BET_MIN_USDC} and ${BET_MAX_USDC} dollars.`, 'error');
      return;
    }

    // BUY: Check balance
    if (activeTab === 'buy') {
      if (user.usdcBalance < amt) {
        setShowDepositModal(true);
        return;
      }
    } else {
      // SELL: Check owned shares
      const mySharesAmount = selectedOutcome === 'YES'
        ? myShares?.yesShares || 0
        : myShares?.noShares || 0;

      if (mySharesAmount === 0) {
        showToast(`No ${selectedOutcome} position. Please buy first.`, 'error');
        return;
      }

      if (amt > mySharesAmount) {
        showToast(`Exceeds sellable amount. (Owned: ${mySharesAmount.toFixed(2)} shares)`, 'error');
        return;
      }
    }

    setLoading(true);
    try {
      const swapRequest = {
        questionId: question.id,
        action: activeTab === 'buy' ? 'BUY' as const : 'SELL' as const,
        outcome: selectedOutcome,
        ...(activeTab === 'buy'
          ? {
              usdcIn: amt,
              minSharesOut: simulation ? simulation.minReceived * 0.98 : undefined,
            }
          : {
              sharesIn: amt,
              minUsdcOut: simulation ? simulation.minReceived * 0.98 : undefined,
            }
        ),
      };

      const response = await swapApi.executeSwap(swapRequest);

      const action = activeTab === 'buy' ? 'Buy' : 'Sell';
      const resultAmount = activeTab === 'buy'
        ? `${response.sharesAmount.toFixed(2)} shares`
        : `${response.usdcAmount.toFixed(2)} USDC`;

      showToast(`${selectedOutcome} ${action} Complete! (${resultAmount})`, 'success');
      setTradeAmount('');
      setSimulation(null);

      // Update my shares from response
      setMyShares(response.myShares);

      // Refresh pool state and user balance
      await refreshUser();
      swapApi.getPoolState(question.id)
        .then(setPoolState)
        .catch(console.error);
      onTradeComplete();
    } catch (error) {
      if (error instanceof ApiError) {
        if (error.status === 409) {
          showToast('Concurrent trade conflict occurred. Please try again.', 'error');
        } else if (error.message.includes('slippage')) {
          showToast('Price has changed. Please try again.', 'error');
        } else if (error.message.includes('balance')) {
          showToast('Insufficient balance.', 'error');
        } else {
          showToast(error.message, 'error');
        }
      } else {
        showToast(activeTab === 'buy' ? 'An error occurred during purchase.' : 'An error occurred during sale.', 'error');
      }
    } finally {
      setLoading(false);
    }
  };

  /**
   * Step 1: Commit (Submit encrypted vote)
   */
  const handleVoteCommit = async (choice: 'YES' | 'NO') => {
    setLoading(true);
    try {
      // 1. Generate salt
      const salt = generateSalt();

      // 2. Generate commitHash: SHA-256(questionId:memberId:choice:salt)
      const commitHash = await generateCommitHash(question.id, user.id, choice, salt);

      // 3. Save salt to localStorage (used for reveal)
      saveSaltToStorage(question.id, salt);
      localStorage.setItem(`vote_choice_${question.id}`, choice);

      // 4. Send only commitHash to server (choice and salt not sent)
      const result = await bettingApi.voteCommit({
        questionId: question.id,
        commitHash,
      });

      if (result.success) {
        showToast(`${choice} vote committed! Please return at reveal time.`, 'vote');
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
        showToast('An error occurred while committing vote.', 'error');
      }
    } finally {
      setLoading(false);
    }
  };

  /**
   * Step 2: Reveal (Reveal vote content)
   */
  const handleVoteReveal = async () => {
    if (!committedChoice) {
      showToast('Cannot find committed vote.', 'error');
      return;
    }

    // Retrieve salt from localStorage
    const salt = getSaltFromStorage(question.id);
    if (!salt) {
      showToast('Vote data has been lost. You can only reveal if you voted in this browser.', 'error');
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
        showToast(`${committedChoice} vote revealed!`, 'vote');
        // Delete salt and choice from localStorage
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
        showToast('An error occurred while revealing vote.', 'error');
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

  // Polymarket-style profit calculation
  const tradeEstimates = tradeAmount && parseFloat(tradeAmount) > 0
    ? (() => {
        const amount = parseFloat(tradeAmount);
        const avgPrice = (selectedOutcome === 'YES' ? yesOdds : 100 - yesOdds) / 100;

        if (avgPrice <= 0 || avgPrice >= 1) {
          return { avgPrice: 0, shares: 0, toWin: 0, profit: 0 };
        }

        if (activeTab === 'buy') {
          const shares = amount / avgPrice;
          const toWin = shares * 1.0; // $1 settlement if correct
          const profit = toWin - amount;
          return { avgPrice, shares, toWin, profit };
        } else {
          // Sell: Estimate profit from opposite position
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
    // Get current prices from pool state
    const currentYesPrice = poolState?.currentPrice.yes || (yesOdds / 100);
    const currentNoPrice = poolState?.currentPrice.no || ((100 - yesOdds) / 100);

    // Get my shares from state
    const myYesShares = myShares?.yesShares || 0;
    const myNoShares = myShares?.noShares || 0;
    const myYesCost = myShares?.yesCostBasis || 0;
    const myNoCost = myShares?.noCostBasis || 0;

    // Check if user has shares for SELL tab
    const hasYesShares = myYesShares > 0;
    const hasNoShares = myNoShares > 0;
    const canSell = (selectedOutcome === 'YES' && hasYesShares) || (selectedOutcome === 'NO' && hasNoShares);

    return (
      <>
        {/* Current Prices */}
        {poolState && (
          <div className={`mb-4 p-4 rounded-2xl ${isDark ? 'bg-slate-800/50' : 'bg-slate-50'}`}>
            <div className="flex justify-between items-center">
              <div className="flex items-center gap-2">
                <div className="text-xs font-bold text-slate-400 uppercase">YES</div>
                <div className="text-lg font-black text-emerald-500">
                  {Math.round(currentYesPrice * 100)}¢
                </div>
              </div>
              <div className="flex items-center gap-2">
                <div className="text-xs font-bold text-slate-400 uppercase">NO</div>
                <div className="text-lg font-black text-rose-500">
                  {Math.round(currentNoPrice * 100)}¢
                </div>
              </div>
            </div>
          </div>
        )}

        {/* BUY/SELL Tabs */}
        <div className={`flex border-b mb-4 ${isDark ? 'border-slate-800' : 'border-slate-100'}`}>
          <button
            onClick={() => setActiveTab('buy')}
            className={`flex-1 pb-4 font-black text-sm transition-all ${activeTab === 'buy' ? 'border-b-4 border-indigo-600 text-indigo-600' : 'text-slate-400 hover:text-slate-300'}`}
          >
            BUY
          </button>
          <button
            onClick={() => setActiveTab('sell')}
            className={`flex-1 pb-4 font-black text-sm transition-all ${activeTab === 'sell' ? 'border-b-4 border-rose-500 text-rose-500' : 'text-slate-400 hover:text-slate-300'}`}
          >
            SELL
          </button>
        </div>

        {/* YES/NO Outcome Selection */}
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
            <p className="text-xl font-black">{Math.round(currentYesPrice * 100)}¢</p>
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
            <p className="text-xl font-black">{Math.round(currentNoPrice * 100)}¢</p>
          </button>
        </div>

        {/* Amount Input */}
        <div className="mb-6">
          <label className="text-xs font-bold text-slate-400 uppercase mb-2 block">
            {activeTab === 'buy' ? 'Amount (USDC)' : 'Amount (Shares)'}
          </label>
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
            <span className="absolute right-4 top-1/2 -translate-y-1/2 text-slate-400 font-bold">
              {activeTab === 'buy' ? 'USDC' : 'shares'}
            </span>
          </div>
          {activeTab === 'buy' && (
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
          )}
        </div>

        {/* Simulation Results */}
        {simulation && !simulationLoading && (
          <div className={`rounded-2xl p-4 mb-6 ${isDark ? 'bg-indigo-950/20 border border-indigo-900/30' : 'bg-indigo-50'}`}>
            {activeTab === 'buy' ? (
              <>
                <div className="flex justify-between text-sm mb-2">
                  <span className="text-slate-500">Expected Receive</span>
                  <span className="font-bold text-slate-700 dark:text-slate-300">
                    {simulation.sharesOut?.toFixed(2)} shares
                  </span>
                </div>
                <div className="flex justify-between text-sm mb-2">
                  <span className="text-slate-500">Avg Price</span>
                  <span className="font-bold text-indigo-600">
                    {Math.round(simulation.effectivePrice * 100)}¢
                  </span>
                </div>
                <div className="flex justify-between text-sm mb-2">
                  <span className="text-slate-500">Slippage</span>
                  <span className={`font-bold ${
                    simulation.slippage < 0.001 ? 'text-emerald-600' :
                    simulation.slippage < 0.01 ? 'text-amber-500' :
                    'text-rose-500'
                  }`}>
                    {(simulation.slippage * 100).toFixed(2)}%
                  </span>
                </div>
                <div className="flex justify-between text-sm mb-2">
                  <span className="text-slate-500">Fee</span>
                  <span className="font-bold text-slate-700 dark:text-slate-300">
                    {simulation.fee.toFixed(2)} USDC
                  </span>
                </div>
                <div className="flex justify-between text-xs pt-2 border-t border-slate-200 dark:border-slate-700">
                  <span className="text-slate-400">Profit if Correct</span>
                  <span className="font-bold text-emerald-600">
                    +{((simulation.sharesOut || 0) - parseFloat(tradeAmount || '0')).toFixed(2)} USDC
                  </span>
                </div>
              </>
            ) : (
              <>
                <div className="flex justify-between text-sm mb-2">
                  <span className="text-slate-500">Expected Receive</span>
                  <span className="font-bold text-emerald-600">
                    {simulation.usdcOut?.toFixed(2)} USDC
                  </span>
                </div>
                <div className="flex justify-between text-sm mb-2">
                  <span className="text-slate-500">Fee</span>
                  <span className="font-bold text-slate-700 dark:text-slate-300">
                    {simulation.fee.toFixed(2)} USDC
                  </span>
                </div>
              </>
            )}
          </div>
        )}

        {/* My Position */}
        {!isGuest && (hasYesShares || hasNoShares) && (
          <div className={`rounded-2xl p-4 mb-6 ${isDark ? 'bg-slate-800 border border-slate-700' : 'bg-slate-50 border border-slate-200'}`}>
            <div className="text-xs font-bold text-slate-400 uppercase mb-3">My Position</div>
            {hasYesShares && (
              <div className="flex justify-between items-center mb-2">
                <span className="text-sm text-slate-500">YES</span>
                <div className="text-right">
                  <div className="font-bold text-emerald-600">{myYesShares.toFixed(2)} shares</div>
                  <div className="text-xs text-slate-400">
                    Value: ${(myYesShares * currentYesPrice).toFixed(2)}
                  </div>
                </div>
              </div>
            )}
            {hasNoShares && (
              <div className="flex justify-between items-center">
                <span className="text-sm text-slate-500">NO</span>
                <div className="text-right">
                  <div className="font-bold text-rose-600">{myNoShares.toFixed(2)} shares</div>
                  <div className="text-xs text-slate-400">
                    Value: ${(myNoShares * currentNoPrice).toFixed(2)}
                  </div>
                </div>
              </div>
            )}
          </div>
        )}

        {/* Action Button */}
        {isGuest ? (
          <button
            onClick={openRegister}
            className="w-full py-4 rounded-2xl font-black text-lg transition-all shadow-xl active:scale-95 bg-indigo-600 text-white hover:bg-indigo-700 flex items-center justify-center gap-2"
          >
            <UserPlus size={20} />
            Sign up to participate
          </button>
        ) : (
          <button
            onClick={handleTrade}
            disabled={loading || !tradeAmount || (activeTab === 'sell' && !canSell)}
            className={`w-full py-4 rounded-2xl font-black text-lg transition-all shadow-xl active:scale-95 disabled:opacity-50 ${
              activeTab === 'buy'
                ? selectedOutcome === 'YES'
                  ? 'bg-emerald-500 text-white shadow-emerald-500/20 hover:bg-emerald-600'
                  : 'bg-rose-500 text-white shadow-rose-500/20 hover:bg-rose-600'
                : 'bg-orange-500 text-white shadow-orange-500/20 hover:bg-orange-600'
            }`}
          >
            {loading ? 'Processing...' : activeTab === 'buy' ? `Buy ${selectedOutcome}` : `Sell ${selectedOutcome}`}
          </button>
        )}

        {/* Pool Stats */}
        {poolState && (
          <div className={`mt-6 pt-6 border-t ${isDark ? 'border-slate-800' : 'border-slate-100'}`}>
            <div className="text-xs font-bold text-slate-400 uppercase mb-3">Pool Stats</div>
            <div className="space-y-2">
              <div className="flex justify-between text-sm">
                <span className="text-slate-500">Total Volume</span>
                <span className="font-bold text-slate-700 dark:text-slate-300">
                  ${poolState.totalVolumeUsdc.toFixed(2)}
                </span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-slate-500">Total Liquidity</span>
                <span className="font-bold text-slate-700 dark:text-slate-300">
                  ${poolState.collateralLocked.toFixed(2)}
                </span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-slate-500">YES Pool</span>
                <span className="font-bold text-emerald-600">
                  {poolState.yesShares.toFixed(2)}
                </span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-slate-500">NO Pool</span>
                <span className="font-bold text-rose-600">
                  {poolState.noShares.toFixed(2)}
                </span>
              </div>
            </div>
          </div>
        )}
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
        {/* Stage indicator badge */}
        {isCommitPhase && (
          <div className="mb-6 inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-blue-100 text-blue-700">
            <Shield size={14} />
            <span className="text-sm font-bold">Accepting Votes</span>
          </div>
        )}
        {isRevealPhase && (
          <div className="mb-6 inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-green-100 text-green-700">
            <Shield size={14} />
            <span className="text-sm font-bold">Revealing Votes</span>
          </div>
        )}

        {isExternalSportsMatch && (
          <div className={`mb-6 px-4 py-2 rounded-xl text-xs font-bold ${
            isDark ? 'bg-slate-800 text-slate-300' : 'bg-slate-50 text-slate-700'
          }`}>
            Choose YES if you expect home team to win, NO if you think they won&apos;t.
          </div>
        )}

        {/* Ticket status display */}
        {!isGuest && !hasVotingPass ? (
          <div className={`mb-6 px-4 py-3 rounded-xl flex items-center gap-3 ${
            isDark ? 'bg-amber-950 border border-amber-800' : 'bg-amber-50 border border-amber-200'
          }`}>
            <AlertTriangle className="w-4 h-4 text-amber-500" />
            <p className={`text-sm font-bold ${isDark ? 'text-amber-300' : 'text-amber-700'}`}>
              Voting pass required. Please purchase from My Page.
            </p>
          </div>
        ) : ticketStatus && (
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
                ? 'You have used all your votes for today'
                : `Remaining votes: ${ticketStatus.remainingTickets}/${ticketStatus.maxTickets}`
              }
            </p>
          </div>
        )}

        {isGuest ? (
          <div className={`text-center py-8 rounded-xl ${isDark ? 'bg-slate-800/50' : 'bg-slate-50'}`}>
            <p className={`text-sm ${isDark ? 'text-slate-400' : 'text-slate-500'}`}>You can participate in voting after signing up</p>
            <button
              onClick={openRegister}
              className="mt-4 px-6 py-3 rounded-xl font-black text-sm transition-all bg-indigo-600 text-white hover:bg-indigo-700 flex items-center justify-center gap-2 mx-auto"
            >
              <UserPlus size={16} />
              Sign Up
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
                  {committedChoice} Vote Committed
                </span>
              </div>
              <div className={`mt-4 p-3 rounded-xl text-sm flex items-start gap-2 ${isDark ? 'bg-slate-800 text-slate-400' : 'bg-slate-50 text-slate-600'}`}>
                <Shield className="mt-0.5 flex-shrink-0" size={14} />
                <span>Your choice is encrypted and will be revealed after betting ends.</span>
              </div>
            </div>
          ) : (
            <div>
              <div className="grid grid-cols-2 gap-4 mb-4">
                <button
                  onClick={() => handleVoteCommit('YES')}
                  disabled={loading || !hasVotingPass || ticketStatus?.remainingTickets === 0}
                  className="bg-green-500 hover:bg-green-600 text-white py-4 rounded-xl text-lg font-bold transition-all disabled:opacity-50"
                >
                  {loading ? 'Processing...' : 'Vote YES'}
                </button>
                <button
                  onClick={() => handleVoteCommit('NO')}
                  disabled={loading || !hasVotingPass || ticketStatus?.remainingTickets === 0}
                  className="bg-red-500 hover:bg-red-600 text-white py-4 rounded-xl text-lg font-bold transition-all disabled:opacity-50"
                >
                  {loading ? 'Processing...' : 'Vote NO'}
                </button>
              </div>
              <div className={`p-3 rounded-xl text-sm flex items-start gap-2 ${isDark ? 'bg-slate-800 text-slate-400' : 'bg-slate-50 text-slate-600'}`}>
                <Shield className="mt-0.5 flex-shrink-0" size={14} />
                <span>Your choice is encrypted and will be revealed after betting ends.</span>
              </div>
            </div>
          )
        ) : isRevealPhase ? (
          // Reveal Phase: Show reveal button
          hasCommitted && committedChoice ? (
            <div>
              <div className={`mb-4 p-4 rounded-xl ${isDark ? 'bg-slate-800' : 'bg-slate-50'}`}>
                <p className={`text-sm ${isDark ? 'text-slate-400' : 'text-slate-600'}`}>
                  Your vote: <span className={`font-bold ${committedChoice === 'YES' ? 'text-green-600' : 'text-red-600'}`}>{committedChoice}</span>
                </p>
              </div>
              <button
                onClick={handleVoteReveal}
                disabled={loading}
                className="w-full py-4 rounded-xl font-bold text-lg transition-all bg-green-500 hover:bg-green-600 text-white disabled:opacity-50"
              >
                {loading ? 'Processing...' : 'Reveal Vote'}
              </button>
            </div>
          ) : (
            <div className={`text-center py-8 rounded-xl ${isDark ? 'bg-slate-800/50 border border-slate-700' : 'bg-slate-50 border border-slate-200'}`}>
              <AlertTriangle size={32} className="mx-auto mb-2 text-amber-500" />
              <p className={`text-sm font-bold ${isDark ? 'text-slate-300' : 'text-slate-700'}`}>No Vote Data</p>
              <p className={`text-xs mt-2 ${isDark ? 'text-slate-400' : 'text-slate-500'}`}>
                You did not vote in this browser or the data has been lost.
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
              {votedChoice} Vote Complete
            </span>
          </div>
        ) : (
          <div className={`text-center py-8 rounded-xl ${isDark ? 'bg-slate-800/50 border border-slate-700' : 'bg-slate-50 border border-slate-200'}`}>
            <AlertTriangle size={32} className="mx-auto mb-2 text-amber-500" />
            <p className={`text-sm font-bold ${isDark ? 'text-slate-300' : 'text-slate-700'}`}>Cannot check current voting status</p>
            <p className={`text-xs mt-2 ${isDark ? 'text-slate-400' : 'text-slate-500'}`}>
              Please refresh and try again later.
            </p>
          </div>
        )}

        {question.votingEndAt && (
          <div className={`mt-6 pt-6 border-t ${isDark ? 'border-slate-800' : 'border-slate-100'}`}>
            <div className="flex justify-between items-center text-sm">
              <span className="text-slate-400">{isRevealPhase ? 'Until Reveal Ends' : 'Until Voting Ends'}</span>
              <span className="text-indigo-500 font-bold">
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
          <h3 className={`text-lg font-black mb-2 ${isDark ? 'text-amber-400' : 'text-amber-700'}`}>Break Time</h3>
          <p className={`text-sm ${isDark ? 'text-amber-500/70' : 'text-amber-600'}`}>
            Voting has ended.<br/>
            Betting will start soon.
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
          <h3 className={`text-lg font-black mb-2 ${isDark ? 'text-emerald-400' : 'text-emerald-700'}`}>Settlement Complete</h3>
          <p className={`text-sm ${isDark ? 'text-emerald-500/70' : 'text-emerald-600'}`}>
            Settlement for this question has been completed.
          </p>
        </div>
      </div>
    );
  }

  // BETTING status: Show full trading interface
  // Mobile: Show bottom button and slide-up panel
  if (isMobile) {
    const currentYesPrice = poolState?.currentPrice.yes || (yesOdds / 100);
    const currentNoPrice = poolState?.currentPrice.no || ((100 - yesOdds) / 100);

    return (
      <>
        {/* Mobile Bottom Fixed Buttons */}
        <div className="fixed bottom-0 left-0 right-0 z-[100] p-4 bg-gradient-to-t from-slate-900 via-slate-900/95 to-transparent pointer-events-none">
          <div className="grid grid-cols-2 gap-3 max-w-md mx-auto pointer-events-auto">
            <button
              onClick={() => {
                setSelectedOutcome('YES');
                setActiveTab('buy');
                setIsMobilePanelOpen(true);
              }}
              className="bg-emerald-500 text-white py-4 rounded-2xl font-black text-base shadow-lg shadow-emerald-500/30 active:scale-95 transition-transform"
            >
              Buy {Math.round(currentYesPrice * 100)}¢
            </button>
            <button
              onClick={() => {
                setSelectedOutcome('NO');
                setActiveTab('buy');
                setIsMobilePanelOpen(true);
              }}
              className="bg-rose-500 text-white py-4 rounded-2xl font-black text-base shadow-lg shadow-rose-500/30 active:scale-95 transition-transform"
            >
              Sell {Math.round(currentNoPrice * 100)}¢
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
                <h3 className={`text-lg font-black ${isDark ? 'text-slate-200' : 'text-slate-900'}`}>Place Bet</h3>
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
