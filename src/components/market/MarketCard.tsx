'use client';

import Link from 'next/link';
import { Bookmark, CheckCircle } from 'lucide-react';
import { useTheme } from '@/hooks/useTheme';
import type { Question } from '@/types/api';

interface MarketCardProps {
  question: Question;
  votedChoice?: 'YES' | 'NO';
}

export default function MarketCard({ question, votedChoice }: MarketCardProps) {
  const { isDark } = useTheme();
  const yesOdds = question.totalBetPool > 0
    ? Math.round((question.yesBetPool / question.totalBetPool) * 100)
    : 50;

  const isHot = question.totalBetPool > 1000;

  return (
    <Link href={`/question/${question.id}`}>
      <div className={`p-6 rounded-3xl border transition-all hover:shadow-xl hover:-translate-y-1 cursor-pointer group ${isDark ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-100'}`}>
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center space-x-2">
            {isHot && <span className="bg-rose-500 text-white text-[10px] font-black px-2 py-1 rounded-full">HOT</span>}
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

        <div className={`pt-4 border-t flex items-center justify-between text-xs ${isDark ? 'border-slate-800' : 'border-slate-100'}`}>
          <span className="text-slate-400">마감</span>
          <span className="text-rose-500 font-bold">
            {(() => {
              if (!question.expiresAt) return '미정';
              const d = new Date(question.expiresAt);
              return isNaN(d.getTime()) ? '미정' : d.toLocaleDateString('ko-KR');
            })()}
          </span>
        </div>
      </div>
    </Link>
  );
}
