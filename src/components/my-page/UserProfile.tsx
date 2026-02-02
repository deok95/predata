'use client';

import { Wallet, Award } from 'lucide-react';
import { useTheme } from '@/hooks/useTheme';
import type { Member } from '@/types/api';

interface UserProfileProps {
  user: Member;
}

const tierColors: Record<string, string> = {
  BRONZE: 'text-amber-700 bg-amber-100',
  SILVER: 'text-slate-500 bg-slate-100',
  GOLD: 'text-yellow-600 bg-yellow-100',
  PLATINUM: 'text-indigo-600 bg-indigo-100',
};

export default function UserProfile({ user }: UserProfileProps) {
  const { isDark } = useTheme();

  return (
    <div className={`p-6 rounded-3xl border ${isDark ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-100'}`}>
      <div className="flex items-center space-x-4 mb-6">
        <div className="w-16 h-16 bg-indigo-600 rounded-2xl flex items-center justify-center text-white text-2xl font-black">
          {user.email?.[0]?.toUpperCase() || 'P'}
        </div>
        <div>
          <h3 className={`text-lg font-black ${isDark ? 'text-white' : 'text-slate-900'}`}>{user.email}</h3>
          <span className={`inline-flex items-center px-3 py-1 rounded-full text-xs font-bold ${tierColors[user.tier] || tierColors.BRONZE}`}>
            <Award size={12} className="mr-1" /> {user.tier}
          </span>
        </div>
      </div>

      <div className="space-y-3">
        {user.walletAddress && (
          <div className="flex items-center justify-between">
            <span className="text-sm text-slate-400 flex items-center"><Wallet size={14} className="mr-2" /> 지갑</span>
            <span className="text-sm font-mono font-bold">{user.walletAddress.slice(0, 6)}...{user.walletAddress.slice(-4)}</span>
          </div>
        )}
        <div className="flex items-center justify-between">
          <span className="text-sm text-slate-400">포인트 잔액</span>
          <span className="text-lg font-black text-indigo-600">{user.pointBalance.toLocaleString()} P</span>
        </div>
        <div className="flex items-center justify-between">
          <span className="text-sm text-slate-400">정확도</span>
          <span className="text-sm font-bold">{(user.accuracyScore * 100).toFixed(1)}%</span>
        </div>
        <div className="flex items-center justify-between">
          <span className="text-sm text-slate-400">총 예측</span>
          <span className="text-sm font-bold">{user.totalPredictions}회</span>
        </div>
      </div>
    </div>
  );
}
