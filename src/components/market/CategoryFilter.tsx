'use client';

import { useTheme } from '@/hooks/useTheme';
import type { QuestionCategory } from '@/types/api';

interface CategoryFilterProps {
  selected: QuestionCategory;
  onSelect: (category: QuestionCategory) => void;
}

const categories: { id: QuestionCategory; label: string }[] = [
  { id: 'ALL', label: '전체' },
  { id: 'ECONOMY', label: 'ECONOMY' },
  { id: 'SPORTS', label: 'SPORTS' },
  { id: 'POLITICS', label: 'POLITICS' },
  { id: 'TECH', label: 'TECH' },
  { id: 'CULTURE', label: 'CULTURE' },
];

export default function CategoryFilter({ selected, onSelect }: CategoryFilterProps) {
  const { isDark } = useTheme();

  return (
    <div className="flex space-x-2 lg:space-x-3 overflow-x-auto pb-2 scrollbar-hide">
      {categories.map(cat => (
        <button
          key={cat.id}
          onClick={() => onSelect(cat.id)}
          className={`px-3 py-2 lg:px-6 lg:py-3 rounded-2xl font-bold text-xs lg:text-sm whitespace-nowrap transition-all ${
            selected === cat.id
              ? 'bg-indigo-600 text-white shadow-lg'
              : isDark ? 'bg-slate-800 text-slate-400 hover:bg-slate-700' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
          }`}
        >
          {cat.label}
        </button>
      ))}
    </div>
  );
}
