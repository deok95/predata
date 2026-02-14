'use client';

import { useState, useEffect, useCallback } from 'react';
import { ArrowUpCircle, ArrowDownCircle, ShieldCheck, TrendingUp, Wallet } from 'lucide-react';
import { useTheme } from '@/hooks/useTheme';
import { transactionApi } from '@/lib/api';
import type { TransactionHistoryItem, TransactionType } from '@/types/api';

interface TransactionHistoryProps {
  memberId: number;
}

const FILTERS: { key: string | null; label: string }[] = [
  { key: null, label: '전체' },
  { key: 'DEPOSIT', label: '충전' },
  { key: 'BET', label: '베팅' },
  { key: 'SETTLEMENT', label: '정산' },
  { key: 'WITHDRAW', label: '출금' },
  { key: 'VOTING_PASS', label: '투표패스' },
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
  const [items, setItems] = useState<TransactionHistoryItem[]>([]);
  const [filter, setFilter] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(false);

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const res = await transactionApi.getMyTransactions(memberId, filter ?? undefined, page, 20);
      setItems(res.content);
      setTotalPages(res.totalPages);
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

  return (
    <div className={`p-6 rounded-3xl border ${isDark ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-100'}`}>
      <h3 className={`font-black text-lg mb-4 ${isDark ? 'text-white' : 'text-slate-900'}`}>거래 내역</h3>

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
          <p className="text-center text-sm text-slate-400 py-8">로딩 중...</p>
        ) : items.length === 0 ? (
          <p className="text-center text-sm text-slate-400 py-8">거래 내역이 없습니다.</p>
        ) : (
          items.map(tx => (
            <div
              key={tx.id}
              className={`flex items-center justify-between p-3 rounded-xl ${isDark ? 'hover:bg-slate-800' : 'hover:bg-slate-50'}`}
            >
              <div className="flex items-center space-x-3">
                {getIcon(tx.type)}
                <div>
                  <p className={`text-sm font-bold line-clamp-1 ${isDark ? 'text-white' : 'text-slate-900'}`}>
                    {tx.description}
                  </p>
                  <p className="text-xs text-slate-400">
                    {new Date(tx.createdAt).toLocaleDateString('ko-KR')}{' '}
                    {new Date(tx.createdAt).toLocaleTimeString('ko-KR', {
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
                <p className="text-xs text-slate-400">잔액 ${tx.balanceAfter.toLocaleString()}</p>
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
            이전
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
            다음
          </button>
        </div>
      )}
    </div>
  );
}
