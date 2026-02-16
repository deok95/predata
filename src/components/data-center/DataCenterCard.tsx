'use client';

import Link from 'next/link';
import { BarChart3, TrendingUp, Users } from 'lucide-react';
import { useTheme } from '@/hooks/useTheme';
import type { Question } from '@/types/api';

interface DataCenterCardProps {
  question: Question;
}

const STATUS_MAP: Record<string, { text: string; color: string }> = {
  VOTING: { text: '투표중', color: 'bg-blue-500' },
  BREAK: { text: '대기', color: 'bg-amber-500' },
  BETTING: { text: '베팅중', color: 'bg-emerald-500' },
  SETTLED: { text: '정산완료', color: 'bg-slate-500' },
};

export default function DataCenterCard({ question }: DataCenterCardProps) {
  const { isDark } = useTheme();
  const yesOdds = question.totalBetPool > 0
    ? Math.round((question.yesBetPool / question.totalBetPool) * 100)
    : question.yesPercentage;
  const noOdds = 100 - yesOdds;
  const status = STATUS_MAP[question.status] || { text: question.status, color: 'bg-slate-500' };

  return (
    <Link href={`/data-center/${question.id}`}>
      <div className={`p-6 rounded-3xl border transition-all hover:shadow-xl hover:-translate-y-1 cursor-pointer group ${
        isDark ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-100'
      }`}>
        {/* Header: status + category */}
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center gap-2">
            <span className={`${status.color} text-white text-[10px] font-black px-2 py-1 rounded-full`}>
              {status.text}
            </span>
            <span className="text-xs font-bold text-slate-400 uppercase">{question.category || 'GENERAL'}</span>
          </div>
          <BarChart3 size={16} className="text-slate-400 group-hover:text-indigo-500 transition-colors" />
        </div>

        {/* Title */}
        <h3 className={`text-base font-black mb-4 leading-tight line-clamp-2 ${
          isDark ? 'text-white' : 'text-slate-900'
        }`}>
          {question.title}
        </h3>

        {/* YES/NO ratio bar */}
        <div className="mb-4">
          <div className="flex justify-between text-xs mb-1.5">
            <span className="font-black text-emerald-500">YES {yesOdds}%</span>
            <span className="font-black text-rose-500">NO {noOdds}%</span>
          </div>
          <div className={`h-2 rounded-full overflow-hidden flex ${isDark ? 'bg-slate-800' : 'bg-slate-100'}`}>
            <div className="h-full bg-emerald-500 transition-all" style={{ width: `${yesOdds}%` }} />
            <div className="h-full bg-rose-500 transition-all" style={{ width: `${noOdds}%` }} />
          </div>
        </div>

        {/* Stats row */}
        <div className={`pt-3 border-t flex items-center justify-between text-xs ${
          isDark ? 'border-slate-800' : 'border-slate-100'
        }`}>
          <div className="flex items-center gap-1 text-slate-400">
            <TrendingUp size={12} />
            <span className="font-bold">{'$'}{question.totalBetPool.toLocaleString()}</span>
          </div>
          <div className="flex items-center gap-1 text-slate-400">
            <Users size={12} />
            <span className="font-bold">데이터 분석</span>
          </div>
        </div>
      </div>
    </Link>
  );
}
