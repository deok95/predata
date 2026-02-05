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

function useTimeRemaining(targetDate: string) {
  const [timeLeft, setTimeLeft] = useState('');

  useEffect(() => {
    const calculateTimeLeft = () => {
      const now = new Date().getTime();
      const target = new Date(targetDate).getTime();
      const difference = target - now;

      if (difference <= 0) {
        setTimeLeft('종료됨');
        return;
      }

      const days = Math.floor(difference / (1000 * 60 * 60 * 24));
      const hours = Math.floor((difference % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
      const minutes = Math.floor((difference % (1000 * 60 * 60)) / (1000 * 60));

      if (days > 0) {
        setTimeLeft(`${days}일 ${hours}시간`);
      } else if (hours > 0) {
        setTimeLeft(`${hours}시간 ${minutes}분`);
      } else {
        setTimeLeft(`${minutes}분`);
      }
    };

    calculateTimeLeft();
    const interval = setInterval(calculateTimeLeft, 60000); // 1분마다 업데이트

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

  // 남은 시간 계산
  const getEndDate = () => {
    if (question.status === 'SETTLED') return null;
    if (question.status === 'VOTING') return question.votingEndAt;
    if (question.status === 'BREAK') return question.bettingStartAt;
    if (question.status === 'BETTING') return question.bettingEndAt;
    return question.expiredAt;
  };

  const endDate = getEndDate();
  const timeRemaining = endDate ? useTimeRemaining(endDate) : '종료됨';

  // 상태 배지 및 설명 설정
  const getStatusBadge = () => {
    switch (question.status) {
      case 'VOTING':
        return { text: '투표 진행 중', color: 'bg-blue-500', description: '투표중' };
      case 'BREAK':
        return { text: '베팅 준비 중', color: 'bg-amber-500', description: '투표 마감 - 베팅 준비 중' };
      case 'BETTING':
        return { text: '베팅 진행 중', color: 'bg-emerald-500', description: '베팅중' };
      case 'SETTLED':
        return { text: '정산 완료', color: 'bg-slate-500', description: '정산완료' };
      default:
        return { text: question.status, color: 'bg-slate-500', description: question.status };
    }
  };

  const statusBadge = getStatusBadge();

  // 디버깅 로그
  useEffect(() => {
    console.log(`[MarketCard #${question.id}] Status: ${question.status}, EndDate: ${endDate}, TimeRemaining: ${timeRemaining}`);
  }, [question.id, question.status, endDate, timeRemaining]);

  return (
    <Link href={`/question/${question.id}`}>
      <div className={`p-6 rounded-3xl border transition-all hover:shadow-xl hover:-translate-y-1 cursor-pointer group ${isDark ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-100'}`}>
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center space-x-2">
            {isHot && <span className="bg-rose-500 text-white text-[10px] font-black px-2 py-1 rounded-full">HOT</span>}
            <span className={`${statusBadge.color} text-white text-[10px] font-black px-2 py-1 rounded-full`}>
              {statusBadge.description}
            </span>
            <span className="text-xs font-bold text-slate-400 uppercase">{question.category || 'GENERAL'}</span>
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

        <h3 className={`text-lg font-black mb-4 leading-tight line-clamp-2 ${isDark ? 'text-white' : 'text-slate-900'}`}>
          {question.title}
        </h3>

        <div className="space-y-2 mb-4">
          <div className="flex justify-between text-xs">
            <span className="text-slate-400">확률</span>
            <span className="font-bold text-indigo-600">{yesOdds}% Yes</span>
          </div>
          <div className="w-full bg-slate-200 dark:bg-slate-800 rounded-full h-2">
            <div className="bg-indigo-600 h-2 rounded-full transition-all" style={{ width: `${yesOdds}%` }} />
          </div>
          <div className="flex justify-between text-xs">
            <span className="text-slate-400">거래량</span>
            <span className="font-bold">{question.totalBetPool.toLocaleString()} P</span>
          </div>
        </div>

        {question.status !== 'SETTLED' && (
          <div className={`pt-4 border-t flex items-center justify-between text-xs ${isDark ? 'border-slate-800' : 'border-slate-100'}`}>
            <div className="flex items-center gap-1 text-slate-400">
              <Clock size={12} />
              <span>{question.status === 'BREAK' ? '베팅 시작까지' : '남은 시간'}</span>
            </div>
            <span className={`font-bold ${timeRemaining === '종료됨' ? 'text-slate-400' : 'text-rose-500'}`}>
              {timeRemaining}
            </span>
          </div>
        )}
        {question.status === 'SETTLED' && (
          <div className={`pt-4 border-t flex items-center justify-center text-xs ${isDark ? 'border-slate-800' : 'border-slate-100'}`}>
            <span className="text-slate-400 font-bold">종료됨</span>
          </div>
        )}
      </div>
    </Link>
  );
}
