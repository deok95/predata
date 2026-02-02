'use client';

import { useState, useEffect } from 'react';
import { CheckCircle, XCircle } from 'lucide-react';
import { useTheme } from '@/hooks/useTheme';
import { settlementApi, bettingApi } from '@/lib/api';
import { mockSettlementHistory, mockGuestActivities } from '@/lib/mockData';
import type { SettlementHistory, Activity } from '@/types/api';

interface BetHistoryProps {
  memberId: number;
}

export default function BetHistory({ memberId }: BetHistoryProps) {
  const { isDark } = useTheme();
  const [history, setHistory] = useState<SettlementHistory[]>([]);
  const [activities, setActivities] = useState<Activity[]>([]);
  const [filter, setFilter] = useState<'all' | 'active' | 'settled'>('all');

  useEffect(() => {
    settlementApi.getHistory(memberId).then(res => {
      if (res.success && res.data && res.data.length > 0) setHistory(res.data);
      else setHistory(mockSettlementHistory);
    }).catch(() => {
      setHistory(mockSettlementHistory);
    });

    bettingApi.getActivitiesByMember(memberId).then(res => {
      if (res.success && res.data && res.data.length > 0) setActivities(res.data);
      else setActivities(mockGuestActivities);
    }).catch(() => {
      setActivities(mockGuestActivities);
    });
  }, [memberId]);

  const totalProfit = history.reduce((sum, h) => sum + h.profit, 0);
  const wins = history.filter(h => h.isWinner).length;
  const losses = history.filter(h => !h.isWinner).length;

  return (
    <div className={`p-6 rounded-3xl border ${isDark ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-100'}`}>
      <h3 className={`font-black text-lg mb-4 ${isDark ? 'text-white' : 'text-slate-900'}`}>베팅 내역</h3>

      {/* Stats summary */}
      <div className="grid grid-cols-3 gap-3 mb-6">
        <div className={`p-3 rounded-2xl text-center ${isDark ? 'bg-slate-800' : 'bg-slate-50'}`}>
          <p className="text-xs text-slate-400 mb-1">총 수익</p>
          <p className={`font-black ${totalProfit >= 0 ? 'text-emerald-500' : 'text-rose-500'}`}>{totalProfit >= 0 ? '+' : ''}{totalProfit.toLocaleString()} P</p>
        </div>
        <div className={`p-3 rounded-2xl text-center ${isDark ? 'bg-slate-800' : 'bg-slate-50'}`}>
          <p className="text-xs text-slate-400 mb-1">승리</p>
          <p className="font-black text-emerald-500">{wins}</p>
        </div>
        <div className={`p-3 rounded-2xl text-center ${isDark ? 'bg-slate-800' : 'bg-slate-50'}`}>
          <p className="text-xs text-slate-400 mb-1">패배</p>
          <p className="font-black text-rose-500">{losses}</p>
        </div>
      </div>

      {/* Filter */}
      <div className="flex space-x-2 mb-4">
        {(['all', 'active', 'settled'] as const).map(f => (
          <button
            key={f}
            onClick={() => setFilter(f)}
            className={`px-4 py-2 rounded-xl text-xs font-bold transition-all ${
              filter === f
                ? 'bg-indigo-600 text-white'
                : isDark ? 'bg-slate-800 text-slate-400' : 'bg-slate-100 text-slate-500'
            }`}
          >
            {f === 'all' ? '전체' : f === 'active' ? '진행 중' : '완료'}
          </button>
        ))}
      </div>

      {/* List */}
      <div className="space-y-3 max-h-96 overflow-y-auto custom-scrollbar">
        {filter !== 'active' && history.map((h, i) => (
          <div key={`h-${i}`} className={`flex items-center justify-between p-3 rounded-xl ${isDark ? 'hover:bg-slate-800' : 'hover:bg-slate-50'}`}>
            <div className="flex items-center space-x-3">
              {h.isWinner
                ? <CheckCircle size={18} className="text-emerald-500" />
                : <XCircle size={18} className="text-rose-500" />
              }
              <div>
                <p className="text-sm font-bold line-clamp-1">{h.questionTitle}</p>
                <p className="text-xs text-slate-400">{h.myChoice} / 결과: {h.finalResult}</p>
              </div>
            </div>
            <div className="text-right">
              <p className={`text-sm font-black ${h.profit >= 0 ? 'text-emerald-500' : 'text-rose-500'}`}>
                {h.profit >= 0 ? '+' : ''}{h.profit} P
              </p>
              <p className="text-xs text-slate-400">{h.betAmount} P 베팅</p>
            </div>
          </div>
        ))}

        {filter !== 'settled' && activities.filter(a => a.activityType === 'BET').map((a) => (
          <div key={`a-${a.id}`} className={`flex items-center justify-between p-3 rounded-xl ${isDark ? 'hover:bg-slate-800' : 'hover:bg-slate-50'}`}>
            <div className="flex items-center space-x-3">
              <div className={`w-8 h-8 rounded-lg flex items-center justify-center text-xs font-black ${
                a.choice === 'YES' ? 'bg-emerald-500/10 text-emerald-500' : 'bg-rose-500/10 text-rose-500'
              }`}>{a.choice}</div>
              <div>
                <p className="text-sm font-bold">Question #{a.questionId}</p>
                <p className="text-xs text-slate-400">진행 중</p>
              </div>
            </div>
            <div className="text-right">
              <p className="text-sm font-bold">{a.amount} P</p>
              <p className="text-xs text-slate-400">{new Date(a.createdAt).toLocaleDateString('ko-KR')}</p>
            </div>
          </div>
        ))}

        {history.length === 0 && activities.length === 0 && (
          <p className="text-center text-sm text-slate-400 py-8">아직 베팅 내역이 없습니다.</p>
        )}
      </div>
    </div>
  );
}
