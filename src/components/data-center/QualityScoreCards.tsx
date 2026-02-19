'use client';

import { Award, Users, GitCompareArrows, Filter } from 'lucide-react';

interface QualityScoreCardsProps {
  overallQualityScore: number;
  totalVotes: number;
  gapPercentage: number;
  filteredPercentage: number;
  isDark: boolean;
}

export default function QualityScoreCards({
  overallQualityScore,
  totalVotes,
  gapPercentage,
  filteredPercentage,
  isDark,
}: QualityScoreCardsProps) {
  const cardClass = `p-6 rounded-3xl border transition-all hover:shadow-lg hover:-translate-y-0.5 ${
    isDark ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-100'
  }`;
  const labelClass = 'text-xs font-bold text-slate-400 uppercase tracking-wider';

  const scoreColor = overallQualityScore >= 80 ? 'text-emerald-500' : overallQualityScore >= 60 ? 'text-amber-500' : 'text-rose-500';
  const scoreBarColor = overallQualityScore >= 80 ? 'bg-emerald-500' : overallQualityScore >= 60 ? 'bg-amber-500' : 'bg-rose-500';
  const grade = overallQualityScore >= 90 ? 'A+' : overallQualityScore >= 80 ? 'A' : overallQualityScore >= 70 ? 'B' : overallQualityScore >= 60 ? 'C' : 'D';

  const cards = [
    {
      label: 'Quality Score',
      value: `${overallQualityScore.toFixed(1)}`,
      suffix: grade,
      color: scoreColor,
      icon: Award,
      iconColor: 'text-indigo-500',
      progress: overallQualityScore,
      progressColor: scoreBarColor,
    },
    {
      label: 'Total Votes',
      value: totalVotes.toLocaleString(),
      suffix: '',
      color: 'text-emerald-500',
      icon: Users,
      iconColor: 'text-emerald-500',
    },
    {
      label: 'Gap Rate',
      value: `${gapPercentage.toFixed(1)}%`,
      suffix: '',
      color: gapPercentage < 10 ? 'text-emerald-500' : 'text-rose-500',
      icon: GitCompareArrows,
      iconColor: 'text-purple-500',
    },
    {
      label: 'Filter Rate',
      value: `${filteredPercentage.toFixed(1)}%`,
      suffix: '',
      color: 'text-amber-500',
      icon: Filter,
      iconColor: 'text-amber-500',
    },
  ];

  return (
    <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
      {cards.map((card) => (
        <div key={card.label} className={cardClass}>
          <div className="flex items-center justify-between mb-3">
            <p className={labelClass}>{card.label}</p>
            <div className={`p-2 rounded-xl ${isDark ? 'bg-slate-800' : 'bg-slate-50'}`}>
              <card.icon className={`h-4 w-4 ${card.iconColor}`} />
            </div>
          </div>
          <div className="flex items-baseline gap-2">
            <p className={`text-2xl font-black ${card.color}`}>{card.value}</p>
            {card.suffix && (
              <span className={`text-sm font-black px-2 py-0.5 rounded-lg ${
                isDark ? 'bg-indigo-950/50 text-indigo-400' : 'bg-indigo-50 text-indigo-600'
              }`}>
                {card.suffix}
              </span>
            )}
          </div>
          {card.progress !== undefined && (
            <div className={`mt-3 h-1.5 rounded-full ${isDark ? 'bg-slate-800' : 'bg-slate-100'}`}>
              <div
                className={`h-full rounded-full transition-all ${card.progressColor}`}
                style={{ width: `${Math.min(card.progress, 100)}%` }}
              />
            </div>
          )}
        </div>
      ))}
    </div>
  );
}
