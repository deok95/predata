'use client';

import { useState, useEffect, useCallback, useRef } from 'react';
import { ArrowUpCircle, ArrowDownCircle, ShieldCheck, TrendingUp, Wallet } from 'lucide-react';
import { useTheme } from '@/hooks/useTheme';
import { questionApi, transactionApi } from '@/lib/api';
import { useRouter } from 'next/navigation';
import type { TransactionHistoryItem, TransactionType } from '@/types/api';

interface TransactionHistoryProps {
  memberId: number;
}

const FILTERS: { key: string | null; label: string }[] = [
  { key: null, label: 'All' },
  { key: 'DEPOSIT', label: 'Deposit' },
  { key: 'BET', label: 'Bet' },
  { key: 'SETTLEMENT', label: 'Settlement' },
  { key: 'WITHDRAW', label: 'Withdraw' },
  { key: 'VOTING_PASS', label: 'Vote Pass' },
];

function getIcon(type: TransactionType) {
  switch (type) {
    case 'DEPOSIT':
      return <ArrowDownCircle size={18} className="text-emerald-500" />;
    case 'WITHDRAW':
      return <ArrowUpCircle size={18} className="text-rose-500" />;
    case 'BET':
      return <TrendingUp size={18} className="text-amber-500" />;
    case 'SETTLEMENT':
      return <Wallet size={18} className="text-indigo-500" />;
    case 'VOTING_PASS':
      return <ShieldCheck size={18} className="text-violet-500" />;
  }
}

export default function TransactionHistory({ memberId }: TransactionHistoryProps) {
  const { isDark } = useTheme();
  const router = useRouter();
  const [items, setItems] = useState<TransactionHistoryItem[]>([]);
  const [filter, setFilter] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(false);
  const [questionTitles, setQuestionTitles] = useState<Record<number, string>>({});
  const questionTitlesRef = useRef<Record<number, string>>({});

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const res = await transactionApi.getMyTransactions(memberId, filter ?? undefined, page, 20);
      setItems(res.content);
      setTotalPages(res.totalPages);

      const missingQuestionIds = Array.from(
        new Set(
          res.content
            .map((tx) => tx.questionId)
            .filter((id): id is number => typeof id === 'number' && !(id in questionTitlesRef.current))
        )
      );

      if (missingQuestionIds.length > 0) {
        const titlePairs = await Promise.all(
          missingQuestionIds.map(async (id) => {
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
          if (pair) {
            fetchedTitles[pair[0]] = pair[1];
          }
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
      setItems([]);
    } finally {
      setLoading(false);
    }
  }, [memberId, filter, page]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const handleFilterChange = (key: string | null) => {
    setFilter(key);
    setPage(0);
  };

  const getDisplayDescription = (tx: TransactionHistoryItem) => {
    if (!tx.questionId) return tx.description;
    const title = questionTitles[tx.questionId];
    if (!title) return tx.description;
    return tx.description.replace(/Question #\d+/g, title);
  };

  const goToQuestion = (questionId?: number | null) => {
    if (!questionId) return;
    router.push(`/question/${questionId}`);
  };

  return (
    <div className={`p-6 rounded-3xl border ${isDark ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-100'}`}>
      <h3 className={`font-black text-lg mb-4 ${isDark ? 'text-white' : 'text-slate-900'}`}>Transaction History</h3>

      {/* Filter buttons */}
      <div className="flex flex-wrap gap-2 mb-4">
        {FILTERS.map(f => (
          <button
            key={f.key ?? 'all'}
            onClick={() => handleFilterChange(f.key)}
            className={`px-4 py-2 rounded-xl text-xs font-bold transition-all ${
              filter === f.key
                ? 'bg-indigo-600 text-white'
                : isDark ? 'bg-slate-800 text-slate-400' : 'bg-slate-100 text-slate-500'
            }`}
          >
            {f.label}
          </button>
        ))}
      </div>

      {/* List */}
      <div className="space-y-3 max-h-96 overflow-y-auto custom-scrollbar">
        {loading ? (
          <p className="text-center text-sm text-slate-400 py-8">Loading...</p>
        ) : items.length === 0 ? (
          <p className="text-center text-sm text-slate-400 py-8">No transactions yet.</p>
        ) : (
          items.map(tx => (
            <div
              key={tx.id}
              onClick={() => goToQuestion(tx.questionId)}
              onKeyDown={(e) => {
                if (!tx.questionId) return;
                if (e.key === 'Enter' || e.key === ' ') {
                  e.preventDefault();
                  goToQuestion(tx.questionId);
                }
              }}
              role={tx.questionId ? 'button' : undefined}
              tabIndex={tx.questionId ? 0 : -1}
              className={`flex items-center justify-between p-3 rounded-xl ${
                tx.questionId ? 'cursor-pointer' : ''
              } ${isDark ? 'hover:bg-slate-800' : 'hover:bg-slate-50'}`}
            >
              <div className="flex items-center space-x-3">
                {getIcon(tx.type)}
                <div>
                  <p className={`text-sm font-bold line-clamp-1 ${isDark ? 'text-white' : 'text-slate-900'}`}>
                    {getDisplayDescription(tx)}
                  </p>
                  <p className="text-xs text-slate-400">
                    {new Date(tx.createdAt).toLocaleDateString('en-US')}{' '}
                    {new Date(tx.createdAt).toLocaleTimeString('en-US', {
                      hour: '2-digit',
                      minute: '2-digit',
                    })}
                  </p>
                </div>
              </div>
              <div className="text-right">
                <p className={`text-sm font-black ${tx.amount >= 0 ? 'text-emerald-500' : 'text-rose-500'}`}>
                  {tx.amount >= 0 ? '+' : ''}${Math.abs(tx.amount).toLocaleString()}
                </p>
                <p className="text-xs text-slate-400">Balance ${tx.balanceAfter.toLocaleString()}</p>
              </div>
            </div>
          ))
        )}
      </div>

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex justify-center gap-2 mt-4">
          <button
            onClick={() => setPage(p => Math.max(0, p - 1))}
            disabled={page === 0}
            className={`px-3 py-1.5 rounded-lg text-xs font-bold disabled:opacity-30 ${
              isDark ? 'bg-slate-800 text-slate-300' : 'bg-slate-100 text-slate-600'
            }`}
          >
            Previous
          </button>
          <span className="text-xs text-slate-400 flex items-center">
            {page + 1} / {totalPages}
          </span>
          <button
            onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
            disabled={page >= totalPages - 1}
            className={`px-3 py-1.5 rounded-lg text-xs font-bold disabled:opacity-30 ${
              isDark ? 'bg-slate-800 text-slate-300' : 'bg-slate-100 text-slate-600'
            }`}
          >
            Next
          </button>
        </div>
      )}
    </div>
  );
}
