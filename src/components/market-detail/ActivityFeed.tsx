'use client';

import { useState, useEffect } from 'react';
import { useTheme } from '@/hooks/useTheme';
import { swapApi } from '@/lib/api';
import type { SwapHistoryResponse } from '@/lib/api/swap';

interface ActivityFeedProps {
  questionId: number;
  refreshKey: number;
}

export default function ActivityFeed({ questionId, refreshKey }: ActivityFeedProps) {
  const { isDark } = useTheme();
  const [swapHistory, setSwapHistory] = useState<SwapHistoryResponse[]>([]);

  useEffect(() => {
    swapApi.getSwapHistory(questionId, 20)
      .then(data => setSwapHistory(data))
      .catch(() => setSwapHistory([]));
  }, [questionId, refreshKey]);

  return (
    <div className={`p-6 rounded-3xl border ${isDark ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-100'}`}>
      <h3 className={`font-black text-lg mb-4 ${isDark ? 'text-white' : 'text-slate-900'}`}>Recent Activity</h3>
      {swapHistory.length === 0 ? (
        <p className="text-sm text-slate-400 text-center py-8">No activity yet.</p>
      ) : (
        <div className="space-y-3 max-h-64 overflow-y-auto custom-scrollbar">
          {swapHistory.map(swap => (
            <div key={swap.swapId} className={`flex items-center justify-between py-2 px-3 rounded-xl text-sm ${isDark ? 'hover:bg-slate-800' : 'hover:bg-slate-50'}`}>
              <div className="flex items-center space-x-3">
                <span className={`w-8 h-8 rounded-lg flex items-center justify-center text-xs font-black ${
                  swap.outcome === 'YES' ? 'bg-emerald-500/10 text-emerald-500' : 'bg-rose-500/10 text-rose-500'
                }`}>
                  {swap.action === 'BUY' ? '↗' : '↘'}
                </span>
                <div className="flex flex-col">
                  <span className="text-xs text-slate-400">
                    {swap.action} {swap.outcome}
                  </span>
                  <span className="text-[10px] text-slate-500">
                    {swap.memberEmail || `#${swap.memberId}`}
                  </span>
                </div>
              </div>
              <div className="text-right">
                <span className="font-bold text-xs">{swap.sharesAmount.toFixed(2)} shares</span>
                <p className="text-[10px] text-slate-500">
                  @ {(swap.effectivePrice * 100).toFixed(0)}¢
                </p>
                <p className="text-[10px] text-slate-500">
                  {new Date(swap.createdAt).toLocaleTimeString('en-US')}
                </p>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
