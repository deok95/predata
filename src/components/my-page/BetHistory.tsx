'use client';

import { useState, useEffect, useRef } from 'react';
import { CheckCircle, XCircle } from 'lucide-react';
import { useTheme } from '@/hooks/useTheme';
import { settlementApi, bettingApi, questionApi, transactionApi } from '@/lib/api';
import { useRouter } from 'next/navigation';
import type { SettlementHistory, Activity, TransactionHistoryItem } from '@/types/api';

interface BetHistoryProps {
  memberId: number;
}

export default function BetHistory({ memberId }: BetHistoryProps) {
  const { isDark } = useTheme();
  const router = useRouter();
  const [history, setHistory] = useState<SettlementHistory[]>([]);
  const [activities, setActivities] = useState<Activity[]>([]);
  const [filter, setFilter] = useState<'all' | 'active' | 'settled'>('all');
  const [summary, setSummary] = useState({ totalProfit: 0, wins: 0, losses: 0 });
  const [settlementTransactions, setSettlementTransactions] = useState<TransactionHistoryItem[]>([]);
  const [questionTitles, setQuestionTitles] = useState<Record<number, string>>({});
  const questionTitlesRef = useRef<Record<number, string>>({});

  useEffect(() => {
    const fetchData = async () => {
      try {
        const [historyRes, activityRes, txRes] = await Promise.all([
          settlementApi.getHistory().catch(() => null),
          bettingApi.getActivitiesByMember('BET').catch(() => null),
          transactionApi.getMyTransactions(memberId, undefined, 0, 300).catch(() => null),
        ]);

        const historyData = historyRes?.success && historyRes.data ? historyRes.data : [];
        const activityData = activityRes?.success && activityRes.data ? activityRes.data : [];
        const settlementTx = (txRes?.content || []).filter((tx) => {
          const t = (tx.type || '').toUpperCase();
          return t.includes('SETTLEMENT');
        });

        setHistory(historyData);
        setActivities(activityData);
        setSettlementTransactions(settlementTx);

        const settledRows = historyData.length > 0
          ? historyData.map((h) => ({ profit: h.profit, isWinner: h.isWinner }))
          : settlementTx.map((tx) => ({ profit: tx.amount, isWinner: tx.amount > 0 }));

        const totalProfit = settledRows.reduce((sum, row) => sum + row.profit, 0);
        const wins = settledRows.filter((row) => row.isWinner).length;
        const losses = settledRows.filter((row) => !row.isWinner).length;

        setSummary({ totalProfit, wins, losses });

        const questionIds = Array.from(
          new Set(
            [...historyData.map((h) => h.questionId), ...activityData.map((a) => a.questionId)]
              .concat(settlementTx.map((tx) => tx.questionId).filter((id): id is number => typeof id === 'number' && id > 0))
              .filter(
                (id): id is number =>
                  typeof id === 'number' && id > 0 && !(id in questionTitlesRef.current)
              )
          )
        );

        if (questionIds.length > 0) {
          const titlePairs = await Promise.all(
            questionIds.map(async (id) => {
              try {
                const response = await questionApi.getById(id);
                if (response.success && response.data?.title) {
                  return [id, response.data.title] as const;
                }
              } catch {
                // ignore
              }
              return null;
            })
          );

          const fetchedTitles: Record<number, string> = {};
          titlePairs.forEach((pair) => {
            if (pair) fetchedTitles[pair[0]] = pair[1];
          });

          if (Object.keys(fetchedTitles).length > 0) {
            setQuestionTitles((prev) => {
              const next = { ...prev, ...fetchedTitles };
              questionTitlesRef.current = next;
              return next;
            });
          }
        }
      } catch {
        setHistory([]);
        setActivities([]);
        setSummary({ totalProfit: 0, wins: 0, losses: 0 });
      }
    };

    fetchData();
  }, [memberId]);

  const settledFromTransactions: SettlementHistory[] = history.length > 0
    ? history
    : settlementTransactions.map((tx) => ({
        questionId: tx.questionId ?? 0,
        questionTitle: tx.questionId ? (questionTitles[tx.questionId] || `Question #${tx.questionId}`) : tx.description,
        myChoice: '-',
        finalResult: '-',
        betAmount: 0,
        payout: tx.amount,
        profit: tx.amount,
        isWinner: tx.amount > 0,
      }));

  const goToQuestion = (questionId?: number | null) => {
    if (!questionId) return;
    router.push(`/question/${questionId}`);
  };

  return (
    <div className={`p-6 rounded-3xl border ${isDark ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-100'}`}>
      <h3 className={`font-black text-lg mb-4 ${isDark ? 'text-white' : 'text-slate-900'}`}>Bet History</h3>

      {/* Stats summary */}
      <div className="grid grid-cols-3 gap-3 mb-6">
        <div className={`p-3 rounded-2xl text-center ${isDark ? 'bg-slate-800' : 'bg-slate-50'}`}>
          <p className="text-xs text-slate-400 mb-1">Total Profit</p>
          <p className={`font-black ${summary.totalProfit >= 0 ? 'text-emerald-500' : 'text-rose-500'}`}>{summary.totalProfit >= 0 ? '+' : ''}{'$'}{summary.totalProfit.toLocaleString()}</p>
        </div>
        <div className={`p-3 rounded-2xl text-center ${isDark ? 'bg-slate-800' : 'bg-slate-50'}`}>
          <p className="text-xs text-slate-400 mb-1">Wins</p>
          <p className="font-black text-emerald-500">{summary.wins}</p>
        </div>
        <div className={`p-3 rounded-2xl text-center ${isDark ? 'bg-slate-800' : 'bg-slate-50'}`}>
          <p className="text-xs text-slate-400 mb-1">Losses</p>
          <p className="font-black text-rose-500">{summary.losses}</p>
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
            {f === 'all' ? 'All' : f === 'active' ? 'Active' : 'Settled'}
          </button>
        ))}
      </div>

      {/* List */}
      <div className="space-y-3 max-h-96 overflow-y-auto custom-scrollbar">
        {filter !== 'active' && settledFromTransactions.map((h, i) => (
          <div
            key={`h-${i}`}
            onClick={() => goToQuestion(h.questionId)}
            onKeyDown={(e) => {
              if (!h.questionId) return;
              if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                goToQuestion(h.questionId);
              }
            }}
            role={h.questionId ? 'button' : undefined}
            tabIndex={h.questionId ? 0 : -1}
            className={`flex items-center justify-between p-3 rounded-xl ${
              h.questionId ? 'cursor-pointer' : ''
            } ${isDark ? 'hover:bg-slate-800' : 'hover:bg-slate-50'}`}
          >
            <div className="flex items-center space-x-3">
              {h.isWinner
                ? <CheckCircle size={18} className="text-emerald-500" />
                : <XCircle size={18} className="text-rose-500" />
              }
              <div>
                <p className="text-sm font-bold line-clamp-1">{h.questionTitle || questionTitles[h.questionId] || `Question #${h.questionId}`}</p>
                <p className="text-xs text-slate-400">{h.myChoice} / Result: {h.finalResult}</p>
              </div>
            </div>
            <div className="text-right">
              <p className={`text-sm font-black ${h.profit >= 0 ? 'text-emerald-500' : 'text-rose-500'}`}>
                {h.profit >= 0 ? '+' : ''}{'$'}{h.profit}
              </p>
              <p className="text-xs text-slate-400">{'$'}{h.betAmount} bet</p>
            </div>
          </div>
        ))}

        {filter !== 'settled' && activities.filter(a => a.activityType === 'BET').map((a) => (
          <div
            key={`a-${a.id}`}
            onClick={() => goToQuestion(a.questionId)}
            onKeyDown={(e) => {
              if (!a.questionId) return;
              if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                goToQuestion(a.questionId);
              }
            }}
            role={a.questionId ? 'button' : undefined}
            tabIndex={a.questionId ? 0 : -1}
            className={`flex items-center justify-between p-3 rounded-xl ${
              a.questionId ? 'cursor-pointer' : ''
            } ${isDark ? 'hover:bg-slate-800' : 'hover:bg-slate-50'}`}
          >
            <div className="flex items-center space-x-3">
              <div className={`w-8 h-8 rounded-lg flex items-center justify-center text-xs font-black ${
                a.choice === 'YES' ? 'bg-emerald-500/10 text-emerald-500' : 'bg-rose-500/10 text-rose-500'
              }`}>{a.choice}</div>
              <div>
                <p className="text-sm font-bold line-clamp-1">{questionTitles[a.questionId] || `Question #${a.questionId}`}</p>
                <p className="text-xs text-slate-400">Active</p>
              </div>
            </div>
            <div className="text-right">
              <p className="text-sm font-bold">{'$'}{a.amount}</p>
              <p className="text-xs text-slate-400">{new Date(a.createdAt).toLocaleDateString('en-US')}</p>
            </div>
          </div>
        ))}

        {history.length === 0 && activities.length === 0 && (
          <p className="text-center text-sm text-slate-400 py-8">No bet history yet.</p>
        )}
      </div>
    </div>
  );
}
