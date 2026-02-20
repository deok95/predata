'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { Bookmark, CheckCircle, Clock } from 'lucide-react';
import { useTheme } from '@/hooks/useTheme';
import type { Question } from '@/types/api';

interface MarketCardProps {
  question: Question;
  votedChoice?: 'YES' | 'NO';
}

function useTimeRemaining(targetDate: string | null) {
  const [timeLeft, setTimeLeft] = useState('Ended');

  useEffect(() => {
    if (!targetDate) {
      return;
    }

    const calculateTimeLeft = () => {
      const now = new Date().getTime();
      const target = new Date(targetDate).getTime();
      const difference = target - now;

      if (difference <= 0) {
        setTimeLeft('Ended');
        return;
      }

      const days = Math.floor(difference / (1000 * 60 * 60 * 24));
      const hours = Math.floor((difference % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
      const minutes = Math.floor((difference % (1000 * 60 * 60)) / (1000 * 60));

      if (days > 0) {
        setTimeLeft(`${days}d ${hours}h`);
      } else if (hours > 0) {
        setTimeLeft(`${hours}h ${minutes}m`);
      } else {
        setTimeLeft(`${minutes}m`);
      }
    };

    calculateTimeLeft();
    const interval = setInterval(calculateTimeLeft, 60000); // Update every minute

    return () => clearInterval(interval);
  }, [targetDate]);

  return timeLeft;
}

export default function MarketCard({ question, votedChoice }: MarketCardProps) {
  const { isDark } = useTheme();
  const yesOdds = question.totalBetPool > 0
    ? Math.round((question.yesBetPool / question.totalBetPool) * 100)
    : 50;

  const isHot = question.totalBetPool > 1000;

  // Calculate time remaining
  const getEndDate = () => {
    if (question.status === 'SETTLED') return null;
    if (question.status === 'VOTING') return question.votingEndAt;
    if (question.status === 'BREAK') return question.bettingStartAt;
    if (question.status === 'BETTING') return question.bettingEndAt;
    return question.expiredAt;
  };

  const endDate = getEndDate();
  const timeRemaining = useTimeRemaining(endDate);

  // Check if voting period has expired (even if status is still VOTING)
  const isVotingExpired = question.status === 'VOTING' && question.votingEndAt && new Date(question.votingEndAt) < new Date();
  const isBettingExpired = question.status === 'BETTING' && question.bettingEndAt && new Date(question.bettingEndAt) < new Date();

  // Status badge and description setup
  // Category badge helper
  const getCategoryBadge = () => {
    if (question.category === 'TRENDING') {
      return (
        <span className="px-2 py-1 bg-gradient-to-r from-red-500 to-orange-500 text-white rounded text-xs font-bold whitespace-nowrap">
          🔥 Real-time Issue
        </span>
      );
    }

    if (question.category === 'BRANDED') {
      return (
        <span className="px-2 py-1 bg-gradient-to-r from-yellow-500 to-amber-500 text-white rounded text-xs font-bold whitespace-nowrap">
          ⭐ Sponsored
        </span>
      );
    }

    return (
      <span className="text-xs font-bold text-slate-400 uppercase">
        {question.category || 'GENERAL'}
      </span>
    );
  };

  const getStatusBadge = () => {
    // If voting expired but status not updated yet, show BREAK state
    if (isVotingExpired) {
      return { text: 'Preparing Betting', color: 'bg-amber-500', description: 'Break' };
    }

    // If betting expired but status not updated yet, show pending settlement
    if (isBettingExpired) {
      return { text: 'Pending Settlement', color: 'bg-amber-500', description: 'Pending' };
    }

    switch (question.status) {
      case 'VOTING':
        return { text: 'Voting Active', color: 'bg-blue-600', description: 'Voting' };
      case 'BREAK':
        return { text: 'Preparing Betting', color: 'bg-amber-500', description: 'Break' };
      case 'BETTING':
        return { text: 'Betting Active', color: 'bg-emerald-500', description: 'Active' };
      case 'SETTLED':
        return { text: 'Settled', color: 'bg-slate-500', description: 'Settled' };
      default:
        return { text: question.status, color: 'bg-slate-500', description: question.status };
    }
  };

  const statusBadge = getStatusBadge();

  return (
    <Link href={`/question/${question.id}`}>
      <div className={`h-[320px] md:h-auto p-4 lg:p-6 rounded-3xl border transition-all hover:shadow-xl hover:-translate-y-1 cursor-pointer group flex flex-col ${isDark ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-100'}`}>
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center space-x-2">
            {isHot && <span className="bg-rose-500 text-white text-[10px] font-black px-2 py-1 rounded-full">HOT</span>}
            <span className={`${statusBadge.color} text-white text-[10px] font-black px-2 py-1 rounded-full`}>
              {statusBadge.description}
            </span>
            {getCategoryBadge()}
          </div>
          <div className="flex items-center gap-2">
            {votedChoice && (
              <span className={`flex items-center gap-1 text-[10px] font-black px-2 py-1 rounded-full ${
                votedChoice === 'YES'
                  ? 'bg-emerald-500/10 text-emerald-500'
                  : 'bg-rose-500/10 text-rose-500'
              }`}>
                <CheckCircle size={10} />
                {votedChoice}
              </span>
            )}
            <Bookmark size={16} className="text-slate-400 group-hover:text-indigo-500 transition-colors" />
          </div>
        </div>

        <h3 className={`text-base lg:text-lg font-black mb-4 leading-tight line-clamp-2 min-h-[52px] ${isDark ? 'text-white' : 'text-slate-900'}`}>
          {question.title}
        </h3>

        {question.status !== 'VOTING' && (
          <div className="space-y-2 mb-4 h-[88px]">
            <div className="flex justify-between text-xs">
              <span className="text-slate-400">Probability</span>
              <span className="font-bold text-indigo-600">{yesOdds}% Yes</span>
            </div>
            <div className="w-full bg-slate-200 dark:bg-slate-800 rounded-full h-2">
              <div className="bg-indigo-600 h-2 rounded-full transition-all" style={{ width: `${yesOdds}%` }} />
            </div>
            <div className="flex justify-between text-xs">
              <span className="text-slate-400">Volume</span>
              <span className="font-bold">{'$'}{question.totalBetPool.toLocaleString()}</span>
            </div>
          </div>
        )}
        {question.status === 'VOTING' && (
          <div className="mb-4 h-[88px] flex items-center">
            <div className={`w-full text-center py-3 lg:py-4 rounded-xl ${isDark ? 'bg-blue-600/20 border border-blue-500/40' : 'bg-blue-100 border border-blue-300'}`}>
              <span className="text-blue-600 font-bold text-sm">Participate →</span>
            </div>
          </div>
        )}

        <div className="mt-auto">
          {question.status !== 'SETTLED' && (
            <div className={`pt-4 border-t flex items-center justify-between text-xs ${isDark ? 'border-slate-800' : 'border-slate-100'}`}>
              <div className="flex items-center gap-1 text-slate-400">
                <Clock size={12} />
                <span>{question.status === 'BREAK' ? 'Until Betting' : 'Time Left'}</span>
              </div>
              <span className={`font-bold ${timeRemaining === 'Ended' ? 'text-slate-400' : 'text-rose-500'}`}>
                {timeRemaining}
              </span>
            </div>
          )}
          {question.status === 'SETTLED' && (
            <div className={`pt-4 border-t flex items-center justify-center text-xs ${isDark ? 'border-slate-800' : 'border-slate-100'}`}>
              <span className="text-slate-400 font-bold">Ended</span>
            </div>
          )}
        </div>
      </div>
    </Link>
  );
}
