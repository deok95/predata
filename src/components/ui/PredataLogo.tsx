'use client';

import React from 'react';
import { useTheme } from '@/hooks/useTheme';

interface PredataLogoProps {
  /**
   * Back-compat: historically used to size the whole component, but that caused
   * the text to overflow and appear off-center. Now this sizes the icon (svg).
   */
  className?: string;
  /** Optional classes for the outer wrapper. */
  wrapperClassName?: string;
  iconOnly?: boolean;
}

export default function PredataLogo({
  className = 'w-10 h-10',
  wrapperClassName = '',
  iconOnly = false,
}: PredataLogoProps) {
  const { isDark } = useTheme();

  return (
    <div className={`inline-flex items-center space-x-3 ${wrapperClassName}`}>
      <div className="relative flex-shrink-0">
        <svg viewBox="0 0 40 40" className={`${className} fill-none`} xmlns="http://www.w3.org/2000/svg">
          <rect width="40" height="40" rx="12" className="fill-indigo-600 shadow-lg" />
          <path d="M12 10V30H20C25.5228 30 30 25.5228 30 20C30 14.4772 25.5228 10 20 10H12Z" stroke="white" strokeWidth="2.5" strokeLinejoin="round" />
          <circle cx="20" cy="20" r="3" className="fill-white animate-pulse" />
          <path d="M12 20H17" stroke="white" strokeWidth="2" strokeLinecap="round" />
        </svg>
      </div>
      {!iconOnly && (
        <div className="flex flex-col leading-none">
          <span className={`text-xl font-black tracking-tighter ${isDark ? 'text-white' : 'text-slate-900'}`}>
            PRE<span className="text-indigo-600">(</span>D<span className="text-indigo-600">)</span>ATA
          </span>
          <span className="text-[8px] font-bold text-slate-400 tracking-[0.2em] uppercase mt-1">지능형 예측 오라클</span>
        </div>
      )}
    </div>
  );
}
