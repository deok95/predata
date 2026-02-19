'use client';

import { useTheme } from '@/hooks/useTheme';
import type { QuestionCategory } from '@/types/api';

interface CategoryFilterProps {
  selected: QuestionCategory;
  onSelect: (category: QuestionCategory) => void;
}

const categories: { id: QuestionCategory; label: string; icon: string }[] = [
  { id: 'ALL', label: 'All', icon: '🌐' },
  { id: 'TRENDING', label: 'Trending', icon: '🔥' },
  { id: 'SPORTS', label: 'Sports', icon: '⚽' },
  { id: 'ECONOMY', label: 'Economy', icon: '📈' },
  { id: 'POLITICS', label: 'Politics', icon: '🏛' },
  { id: 'TECH', label: 'Tech', icon: '💻' },
  { id: 'CULTURE', label: 'Culture', icon: '🎬' },
  { id: 'BRANDED', label: 'Branded', icon: '⭐' },
];

export default function CategoryFilter({ selected, onSelect }: CategoryFilterProps) {
  const { isDark } = useTheme();

  const getCategoryStyle = (catId: QuestionCategory, isSelected: boolean) => {
    if (isSelected) {
      if (catId === 'TRENDING') {
        return 'bg-gradient-to-r from-red-500 to-orange-500 text-white shadow-lg';
      }
      if (catId === 'BRANDED') {
        return 'bg-gradient-to-r from-yellow-500 to-amber-500 text-white shadow-lg';
      }
      return 'bg-indigo-600 text-white shadow-lg';
    }
    return isDark ? 'bg-slate-800 text-slate-400 hover:bg-slate-700' : 'bg-slate-100 text-slate-600 hover:bg-slate-200';
  };

  return (
    <div className="flex space-x-2 lg:space-x-3 overflow-x-auto pb-2 scrollbar-hide">
      {categories.map(cat => (
        <button
          key={cat.id}
          onClick={() => onSelect(cat.id)}
          className={`px-3 py-2 lg:px-6 lg:py-3 rounded-2xl font-bold text-xs lg:text-sm whitespace-nowrap transition-all ${
            getCategoryStyle(cat.id, selected === cat.id)
          }`}
        >
          <span className="mr-1">{cat.icon}</span>
          {cat.label}
        </button>
      ))}
    </div>
  );
}
