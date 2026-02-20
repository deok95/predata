'use client';

import { useState, useEffect } from 'react';
import { useTheme } from '@/hooks/useTheme';
import { swapApi } from '@/lib/api';
import type { SwapHistoryResponse, MySharesSnapshot } from '@/lib/api/swap';

type Tab = 'orders' | 'positions';

interface MyBetsPanelProps {
  questionId: number;
  executionModel?: 'AMM_FPMM';
}

export default function MyBetsPanel({ questionId }: MyBetsPanelProps) {
  const { isDark } = useTheme();
  const [activeTab, setActiveTab] = useState<Tab>('positions');
  const [myShares, setMyShares] = useState<MySharesSnapshot | null>(null);
  const [swapHistory, setSwapHistory] = useState<SwapHistoryResponse[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    fetchData();
  }, [activeTab, questionId]);

  const fetchData = async () => {
    setLoading(true);
    try {
      if (activeTab === 'orders') {
        const data = await swapApi.getMySwapHistory(questionId, 50);
        setSwapHistory(data);
      } else {
        const data = await swapApi.getMyShares(questionId);
        setMyShares(data);
      }
    } catch (error) {
      console.error('Failed to fetch data:', error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className={`p-6 rounded-2xl border ${isDark ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-100 shadow-sm'}`}>
      {/* Tabs */}
      <div className="flex border-b border-slate-200 dark:border-slate-700 mb-4">
        <button
          onClick={() => setActiveTab('orders')}
          className={`px-6 py-3 font-semibold text-sm transition-colors ${
            activeTab === 'orders'
              ? 'border-b-2 border-indigo-600 text-indigo-600'
              : isDark
              ? 'text-slate-400 hover:text-slate-300'
              : 'text-slate-600 hover:text-slate-900'
          }`}
        >
          My Trades
        </button>
        <button
          onClick={() => setActiveTab('positions')}
          className={`px-6 py-3 font-semibold text-sm transition-colors ${
            activeTab === 'positions'
              ? 'border-b-2 border-indigo-600 text-indigo-600'
              : isDark
              ? 'text-slate-400 hover:text-slate-300'
              : 'text-slate-600 hover:text-slate-900'
          }`}
        >
          My Position
        </button>
      </div>

      {/* Content */}
      {loading ? (
        <div className="text-center py-8">
          <div className="text-slate-400 text-sm">Loading...</div>
        </div>
      ) : activeTab === 'orders' ? (
        <div className="space-y-2">
          {swapHistory.length === 0 ? (
            <p className="text-slate-500 text-sm text-center py-8">No trades yet</p>
          ) : (
            swapHistory.map(swap => (
              <div
                key={swap.swapId}
                className={`flex justify-between items-center p-4 rounded-xl ${
                  isDark ? 'bg-slate-800/50' : 'bg-slate-50'
                }`}
              >
                <div className="flex-1">
                  <div className="flex items-center gap-2 mb-1">
                    <span
                      className={`font-bold ${
                        swap.action === 'BUY' ? 'text-emerald-600' : 'text-rose-600'
                      }`}
                    >
                      {swap.action} {swap.outcome}
                    </span>
                  </div>
                  <div className="text-sm text-slate-500">
                    {swap.sharesAmount.toFixed(2)} shares @ {(swap.effectivePrice * 100).toFixed(0)}¢
                  </div>
                  <div className="text-xs text-slate-400 mt-1">
                    {new Date(swap.createdAt).toLocaleString('en-US')}
                  </div>
                </div>
                <div className="text-right">
                  <div className="font-bold">
                    {swap.action === 'BUY' ? '-' : '+'}${swap.usdcAmount.toFixed(2)}
                  </div>
                  <div className="text-xs text-slate-400">
                    Fee: ${swap.feeUsdc.toFixed(2)}
                  </div>
                </div>
              </div>
            ))
          )}
        </div>
      ) : (
        <div className="space-y-2">
          {!myShares || (myShares.yesShares === 0 && myShares.noShares === 0) ? (
            <p className="text-slate-500 text-sm text-center py-8">No positions</p>
          ) : (
            <>
              {myShares.yesShares > 0 && (
                <div
                  className={`p-4 rounded-xl ${
                    isDark ? 'bg-slate-800/50' : 'bg-slate-50'
                  }`}
                >
                  <div className="flex justify-between items-start">
                    <div className="flex-1">
                      <span className="font-bold text-lg text-emerald-600">YES</span>
                      <div className="grid grid-cols-2 gap-x-4 gap-y-1 text-sm mt-2">
                        <div className={isDark ? 'text-slate-400' : 'text-slate-600'}>
                          Shares: <span className="font-medium">{myShares.yesShares.toFixed(2)}</span>
                        </div>
                        <div className={isDark ? 'text-slate-400' : 'text-slate-600'}>
                          Cost: <span className="font-medium">${myShares.yesCostBasis.toFixed(2)}</span>
                        </div>
                        <div className={isDark ? 'text-slate-400' : 'text-slate-600'}>
                          Avg: <span className="font-medium">${(myShares.yesCostBasis / myShares.yesShares).toFixed(2)}</span>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              )}
              {myShares.noShares > 0 && (
                <div
                  className={`p-4 rounded-xl ${
                    isDark ? 'bg-slate-800/50' : 'bg-slate-50'
                  }`}
                >
                  <div className="flex justify-between items-start">
                    <div className="flex-1">
                      <span className="font-bold text-lg text-rose-600">NO</span>
                      <div className="grid grid-cols-2 gap-x-4 gap-y-1 text-sm mt-2">
                        <div className={isDark ? 'text-slate-400' : 'text-slate-600'}>
                          Shares: <span className="font-medium">{myShares.noShares.toFixed(2)}</span>
                        </div>
                        <div className={isDark ? 'text-slate-400' : 'text-slate-600'}>
                          Cost: <span className="font-medium">${myShares.noCostBasis.toFixed(2)}</span>
                        </div>
                        <div className={isDark ? 'text-slate-400' : 'text-slate-600'}>
                          Avg: <span className="font-medium">${(myShares.noCostBasis / myShares.noShares).toFixed(2)}</span>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              )}
            </>
          )}
        </div>
      )}
    </div>
  );
}
