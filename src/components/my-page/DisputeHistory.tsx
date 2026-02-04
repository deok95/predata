'use client';

import { useState, useEffect } from 'react';
import { Scale, CheckCircle, XCircle, Clock, AlertTriangle } from 'lucide-react';
import { useTheme } from '@/hooks/useTheme';
import { settlementApi } from '@/lib/api';
import { Skeleton } from '@/components/ui/Skeleton';
import type { SettlementHistory } from '@/types/api';
import { useI18n } from '@/lib/i18n';

interface DisputeHistoryProps {
  memberId: number;
}

export default function DisputeHistory({ memberId }: DisputeHistoryProps) {
  const { t } = useI18n();
  const { isDark } = useTheme();
  const [history, setHistory] = useState<SettlementHistory[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    settlementApi.getHistory(memberId).then(res => {
      if (res.success && res.data) setHistory(res.data);
    }).catch(() => {}).finally(() => setLoading(false));
  }, [memberId]);

  return (
    <div className={`p-6 rounded-3xl border ${isDark ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-100'}`}>
      <div className="flex items-center gap-3 mb-5">
        <Scale size={22} className="text-amber-500" />
        <h3 className={`font-black text-lg ${isDark ? 'text-white' : 'text-slate-900'}`}>{t('dispute.title')}</h3>
      </div>

      {loading ? (
        <div className="space-y-3">
          {Array.from({ length: 3 }).map((_, i) => (
            <div key={i} className={`p-4 rounded-xl ${isDark ? 'bg-slate-800' : 'bg-slate-50'}`}>
              <Skeleton className="h-4 w-3/4 mb-2" />
              <Skeleton className="h-3 w-1/2" />
            </div>
          ))}
        </div>
      ) : history.length === 0 ? (
        <div className="text-center py-8">
          <Clock size={32} className="text-slate-400 mx-auto mb-3" />
          <p className="text-sm text-slate-400">{t('dispute.empty')}</p>
          <p className="text-xs text-slate-500 mt-1">{t('dispute.emptyDesc')}</p>
        </div>
      ) : (
        <div className="space-y-2 max-h-96 overflow-y-auto custom-scrollbar">
          {history.map((item, idx) => {
            const isWinner = item.isWinner;
            return (
              <div
                key={idx}
                className={`p-4 rounded-xl transition-all ${
                  isDark ? 'bg-slate-800 hover:bg-slate-750' : 'bg-slate-50 hover:bg-slate-100'
                }`}
              >
                <div className="flex items-start justify-between mb-2">
                  <div className="flex-1 min-w-0">
                    <p className={`text-sm font-bold truncate ${isDark ? 'text-white' : 'text-slate-900'}`}>
                      {item.questionTitle}
                    </p>
                    <div className="flex items-center gap-2 mt-1">
                      <span className={`text-[10px] px-2 py-0.5 rounded font-bold ${
                        item.myChoice === 'YES' ? 'bg-emerald-500/10 text-emerald-500' : 'bg-rose-500/10 text-rose-500'
                      }`}>
                        {t('dispute.myChoice')}: {item.myChoice}
                      </span>
                      <span className="text-[10px] text-slate-400">→</span>
                      <span className={`text-[10px] px-2 py-0.5 rounded font-bold ${
                        item.finalResult === 'YES' ? 'bg-emerald-500/10 text-emerald-500' : 'bg-rose-500/10 text-rose-500'
                      }`}>
                        {t('dispute.result')}: {item.finalResult}
                      </span>
                    </div>
                  </div>
                  <div className="ml-3">
                    {isWinner ? (
                      <CheckCircle size={20} className="text-emerald-500" />
                    ) : (
                      <XCircle size={20} className="text-rose-400" />
                    )}
                  </div>
                </div>
                <div className="flex items-center justify-between mt-2 pt-2 border-t border-dashed" style={{ borderColor: isDark ? '#1e293b' : '#e2e8f0' }}>
                  <span className="text-xs text-slate-400">{t('dispute.betAmount')}: {item.betAmount.toLocaleString()} P</span>
                  <span className={`text-sm font-black ${
                    item.profit > 0 ? 'text-emerald-500' : item.profit < 0 ? 'text-rose-500' : 'text-slate-400'
                  }`}>
                    {item.profit > 0 ? '+' : ''}{item.profit.toLocaleString()} P
                  </span>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {history.length > 0 && (
        <div className={`mt-4 p-3 rounded-xl text-center ${isDark ? 'bg-slate-800' : 'bg-slate-50'}`}>
          <div className="flex items-center justify-center gap-4 text-xs">
            <span className="text-slate-400">
              {t('dispute.total')} {history.length}
            </span>
            <span className="text-emerald-500 font-bold">
              {t('dispute.wins')} {history.filter(h => h.isWinner).length}
            </span>
            <span className="text-rose-400 font-bold">
              {t('dispute.losses')} {history.filter(h => !h.isWinner).length}
            </span>
            <span className={`font-black ${
              history.reduce((sum, h) => sum + h.profit, 0) >= 0 ? 'text-emerald-500' : 'text-rose-500'
            }`}>
              {history.reduce((sum, h) => sum + h.profit, 0) >= 0 ? '+' : ''}
              {history.reduce((sum, h) => sum + h.profit, 0).toLocaleString()} P
            </span>
          </div>
        </div>
      )}
    </div>
  );
}
