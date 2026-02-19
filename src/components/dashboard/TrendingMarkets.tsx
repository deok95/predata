'use client';

import Link from 'next/link';
import { ChevronRight } from 'lucide-react';
import { useTheme } from '@/hooks/useTheme';
import MarketCard from '@/components/market/MarketCard';
import type { Question } from '@/types/api';

interface TrendingMarketsProps {
  questions: Question[];
  getVotedChoice?: (questionId: number) => 'YES' | 'NO' | undefined;
}

export default function TrendingMarkets({ questions, getVotedChoice }: TrendingMarketsProps) {
  const { isDark } = useTheme();
  const topMarkets = [...questions].sort((a, b) => b.totalBetPool - a.totalBetPool).slice(0, 3);

  return (
    <section>
      <div className="flex items-center justify-between mb-8">
        <h2 className={`text-2xl font-black ${isDark ? 'text-white' : 'text-slate-900'}`}>Trending Markets</h2>
        <Link href="/marketplace" className="text-indigo-600 text-xs font-bold flex items-center cursor-pointer hover:underline">
          View All <ChevronRight size={14} />
        </Link>
      </div>
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {topMarkets.map(q => (
          <MarketCard key={q.id} question={q} votedChoice={getVotedChoice?.(q.id)} />
        ))}
      </div>
    </section>
  );
}
