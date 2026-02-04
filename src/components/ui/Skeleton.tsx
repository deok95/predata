'use client';

import { useTheme } from '@/hooks/useTheme';

interface SkeletonProps {
  className?: string;
}

export function Skeleton({ className = '' }: SkeletonProps) {
  const { isDark } = useTheme();
  return (
    <div className={`animate-pulse rounded-xl ${isDark ? 'bg-slate-800' : 'bg-slate-200'} ${className}`} />
  );
}

export function MarketCardSkeleton() {
  const { isDark } = useTheme();
  return (
    <div className={`p-6 rounded-3xl border ${isDark ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-100'}`}>
      <div className="flex items-center justify-between mb-4">
        <Skeleton className="h-5 w-16" />
        <Skeleton className="h-5 w-5 rounded-full" />
      </div>
      <Skeleton className="h-6 w-full mb-2" />
      <Skeleton className="h-6 w-3/4 mb-4" />
      <div className="space-y-2 mb-4">
        <Skeleton className="h-4 w-full" />
        <Skeleton className="h-2 w-full rounded-full" />
        <Skeleton className="h-4 w-1/2" />
      </div>
      <div className={`pt-4 border-t flex justify-between ${isDark ? 'border-slate-800' : 'border-slate-100'}`}>
        <Skeleton className="h-4 w-10" />
        <Skeleton className="h-4 w-20" />
      </div>
    </div>
  );
}
