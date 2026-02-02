'use client';

import { useTheme } from '@/hooks/useTheme';
import type { Member } from '@/types/api';

interface TierProgressProps {
  user: Member;
}

const tierOrder = ['BRONZE', 'SILVER', 'GOLD', 'PLATINUM'];
const tierRequirements: Record<string, { predictions: number; accuracy: number }> = {
  SILVER: { predictions: 50, accuracy: 55 },
  GOLD: { predictions: 200, accuracy: 65 },
  PLATINUM: { predictions: 500, accuracy: 75 },
};

export default function TierProgress({ user }: TierProgressProps) {
  const { isDark } = useTheme();
  const currentIdx = tierOrder.indexOf(user.tier);
  const nextTier = currentIdx < tierOrder.length - 1 ? tierOrder[currentIdx + 1] : null;
  const req = nextTier ? tierRequirements[nextTier] : null;
  const progress = req
    ? Math.min(100, Math.round((user.totalPredictions / req.predictions) * 100))
    : 100;

  return (
    <div className={`p-6 rounded-3xl border ${isDark ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-100'}`}>
      <h3 className={`font-black text-lg mb-4 ${isDark ? 'text-white' : 'text-slate-900'}`}>티어 진행도</h3>

      <div className="flex justify-between text-xs font-bold mb-2">
        <span className="text-indigo-600">{user.tier}</span>
        <span className="text-slate-400">{nextTier || 'MAX'}</span>
      </div>
      <div className={`w-full h-3 rounded-full ${isDark ? 'bg-slate-800' : 'bg-slate-100'}`}>
        <div className="bg-indigo-600 h-3 rounded-full transition-all" style={{ width: `${progress}%` }} />
      </div>
      <p className="text-xs text-slate-400 mt-2">{progress}% 완료</p>

      {req && (
        <div className={`mt-4 p-4 rounded-2xl ${isDark ? 'bg-slate-800' : 'bg-slate-50'}`}>
          <p className="text-xs font-bold text-slate-400 mb-2">다음 티어 조건</p>
          <div className="space-y-1 text-sm">
            <div className="flex justify-between">
              <span className="text-slate-400">예측 횟수</span>
              <span className="font-bold">{user.totalPredictions} / {req.predictions}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-400">정확도</span>
              <span className="font-bold">{(user.accuracyScore * 100).toFixed(1)}% / {req.accuracy}%</span>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
