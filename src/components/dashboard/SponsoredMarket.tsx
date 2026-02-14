'use client';

import Link from 'next/link';
import { ShieldCheck } from 'lucide-react';
import { useTheme } from '@/hooks/useTheme';
import type { Question } from '@/types/api';

interface SponsoredMarketProps {
  question: Question | null;
}

export default function SponsoredMarket({ question }: SponsoredMarketProps) {
  const { isDark } = useTheme();

  if (!question) return null;

  const yesPercent = question.totalBetPool > 0
    ? Math.round((question.yesBetPool / question.totalBetPool) * 100)
    : 50;

  return (
    <div className="space-y-10">
      <Link href={`/question/${question.id}`}>
        <div className="bg-gradient-to-r from-indigo-600 via-purple-600 to-pink-600 p-8 rounded-[2.5rem] text-white relative overflow-hidden cursor-pointer hover:shadow-2xl transition-all group">
          <div className="absolute top-0 right-0 w-64 h-64 bg-white/5 rounded-full -translate-y-1/2 translate-x-1/2 group-hover:scale-110 transition-transform" />
          <div className="relative z-10">
            <div className="flex items-center space-x-2 mb-4">
              <span className="bg-white/20 px-3 py-1 rounded-full text-[10px] font-black uppercase backdrop-blur-sm">Featured</span>
              <span className="text-xs opacity-80">by PRE(D)ATA</span>
            </div>
            <h3 className="text-3xl font-black mb-4 leading-tight">{question.title}</h3>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
              <div><p className="text-xs opacity-70 mb-1">Yes 확률</p><p className="font-black text-lg">{yesPercent}%</p></div>
              <div><p className="text-xs opacity-70 mb-1">카테고리</p><p className="font-black text-lg">{question.category || '-'}</p></div>
              <div><p className="text-xs opacity-70 mb-1">총 풀</p><p className="font-black text-lg">{'$'}{question.totalBetPool.toLocaleString()}</p></div>
              <div><p className="text-xs opacity-70 mb-1">마감</p><p className="font-black text-lg">{(() => { if (!question.expiredAt) return '미정'; const d = new Date(question.expiredAt); return isNaN(d.getTime()) ? '미정' : d.toLocaleDateString('ko-KR'); })()}</p></div>
            </div>
            <span className="bg-white text-indigo-600 px-8 py-4 rounded-2xl font-black text-sm hover:shadow-2xl transition-all active:scale-95 inline-block">상세 보기 및 투표 →</span>
          </div>
        </div>
      </Link>

      <div className={`rounded-3xl p-8 flex flex-col md:flex-row items-center justify-between ${isDark ? 'bg-indigo-950/20 border border-indigo-900/30' : 'bg-slate-900'}`}>
        <div className="flex items-center space-x-6">
          <div className="w-14 h-14 bg-indigo-600 rounded-2xl flex items-center justify-center text-white"><ShieldCheck size={28} /></div>
          <div>
            <h4 className="text-white font-bold">보안 및 검증 완료</h4>
            <p className="text-slate-400 text-sm">모든 데이터는 오라클 노드에 의해 실시간으로 검증됩니다.</p>
          </div>
        </div>
        <div className="flex items-center space-x-2 mt-4 md:mt-0">
          <span className="text-emerald-500 font-bold text-xs uppercase tracking-widest">System Live</span>
          <div className="w-2 h-2 bg-emerald-500 rounded-full animate-pulse" />
        </div>
      </div>
    </div>
  );
}
