'use client';

import React from 'react';

export function StatCard({
  label,
  value,
  suffix,
  icon: Icon,
  colorMode,
  isDark,
}: {
  label: string;
  value: string | number;
  suffix?: string;
  icon: React.ElementType;
  colorMode: 'positive' | 'negative' | 'neutral' | 'auto';
  isDark: boolean;
}) {
  const numericValue = typeof value === 'number' ? value : parseFloat(String(value));
  const resolvedColor =
    colorMode === 'auto'
      ? numericValue > 0
        ? 'positive'
        : numericValue < 0
          ? 'negative'
          : 'neutral'
      : colorMode;

  const textColor =
    resolvedColor === 'positive'
      ? 'text-emerald-500'
      : resolvedColor === 'negative'
        ? 'text-rose-500'
        : isDark
          ? 'text-white'
          : 'text-slate-900';

  const iconBg =
    resolvedColor === 'positive'
      ? isDark ? 'bg-emerald-500/10' : 'bg-emerald-50'
      : resolvedColor === 'negative'
        ? isDark ? 'bg-rose-500/10' : 'bg-rose-50'
        : isDark ? 'bg-indigo-500/10' : 'bg-indigo-50';

  const iconColor =
    resolvedColor === 'positive'
      ? 'text-emerald-500'
      : resolvedColor === 'negative'
        ? 'text-rose-500'
        : 'text-indigo-500';

  return (
    <div
      className={`p-6 rounded-2xl border transition-all ${
        isDark ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-100 shadow-sm'
      }`}
    >
      <div className="flex items-center justify-between mb-4">
        <span className={`text-sm font-medium ${isDark ? 'text-slate-400' : 'text-slate-500'}`}>
          {label}
        </span>
        <div className={`w-10 h-10 rounded-xl flex items-center justify-center ${iconBg}`}>
          <Icon size={20} className={iconColor} />
        </div>
      </div>
      <p className={`text-2xl font-black ${textColor}`}>
        {typeof value === 'number' ? value.toLocaleString() : value}
        {suffix && <span className="text-base font-bold ml-1">{suffix}</span>}
      </p>
    </div>
  );
}

export function SkeletonCard({ isDark }: { isDark: boolean }) {
  return (
    <div
      className={`p-6 rounded-2xl border animate-pulse ${
        isDark ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-100'
      }`}
    >
      <div className="flex items-center justify-between mb-4">
        <div className={`h-4 w-24 rounded ${isDark ? 'bg-slate-800' : 'bg-slate-200'}`} />
        <div className={`w-10 h-10 rounded-xl ${isDark ? 'bg-slate-800' : 'bg-slate-200'}`} />
      </div>
      <div className={`h-8 w-32 rounded ${isDark ? 'bg-slate-800' : 'bg-slate-200'}`} />
    </div>
  );
}

export function SkeletonTable({ isDark, rows = 3 }: { isDark: boolean; rows?: number }) {
  return (
    <div className="space-y-3">
      {Array.from({ length: rows }).map((_, i) => (
        <div
          key={i}
          className={`h-14 rounded-xl animate-pulse ${isDark ? 'bg-slate-800' : 'bg-slate-200'}`}
        />
      ))}
    </div>
  );
}
