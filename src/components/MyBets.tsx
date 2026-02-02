'use client';

import React, { useState, useEffect } from 'react';
import { CheckCircle, XCircle, DollarSign, TrendingUp, TrendingDown } from 'lucide-react';
import { settlementApi, ApiError } from '@/lib/api';

interface SettlementHistoryItem {
  questionId: number;
  questionTitle: string;
  myChoice: string;
  finalResult: string;
  betAmount: number;
  payout: number;
  profit: number;
  isWinner: boolean;
}

interface MyBetsProps {
  memberId: number;
  onClose: () => void;
}

export default function MyBets({ memberId, onClose }: MyBetsProps) {
  const [history, setHistory] = useState<SettlementHistoryItem[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchHistory();
  }, [memberId]);

  const fetchHistory = async () => {
    setLoading(true);
    try {
      const response = await settlementApi.getHistory(memberId);
      if (response.success && response.data) {
        setHistory(response.data);
      }
    } catch (error) {
      console.error('Failed to fetch settlement history:', error);
      if (error instanceof ApiError) {
        console.error('API Error:', error.message);
      }
    } finally {
      setLoading(false);
    }
  };

  const totalProfit = history.reduce((sum, item) => sum + item.profit, 0);
  const winRate = history.length > 0 ? (history.filter(h => h.isWinner).length / history.length) * 100 : 0;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
      <div className="bg-white rounded-lg shadow-xl p-8 w-full max-w-4xl max-h-[80vh] overflow-y-auto">
        <div className="flex justify-between items-center mb-6">
          <h2 className="text-2xl font-bold">내 베팅 내역</h2>
          <button
            onClick={onClose}
            className="text-slate-400 hover:text-slate-600 text-2xl"
          >
            ×
          </button>
        </div>

        {loading ? (
          <div className="text-center py-8">로딩 중...</div>
        ) : (
          <>
            {/* 통계 요약 */}
            <div className="grid grid-cols-3 gap-4 mb-6">
              <div className="bg-slate-50 p-4 rounded-lg">
                <p className="text-sm text-slate-600">총 베팅</p>
                <p className="text-2xl font-bold">{history.length}건</p>
              </div>
              <div className={`p-4 rounded-lg ${totalProfit >= 0 ? 'bg-green-50' : 'bg-red-50'}`}>
                <p className="text-sm text-slate-600">총 수익</p>
                <p className={`text-2xl font-bold ${totalProfit >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                  {totalProfit >= 0 ? '+' : ''}{totalProfit.toLocaleString()}P
                </p>
              </div>
              <div className="bg-blue-50 p-4 rounded-lg">
                <p className="text-sm text-slate-600">승률</p>
                <p className="text-2xl font-bold text-blue-600">{winRate.toFixed(1)}%</p>
              </div>
            </div>

            {/* 베팅 내역 리스트 */}
            {history.length === 0 ? (
              <div className="text-center py-8 text-slate-400">
                아직 정산된 베팅이 없습니다.
              </div>
            ) : (
              <div className="space-y-3">
                {history.map((item, idx) => (
                  <div
                    key={idx}
                    className={`border rounded-lg p-4 ${
                      item.isWinner ? 'border-green-200 bg-green-50' : 'border-red-200 bg-red-50'
                    }`}
                  >
                    <div className="flex justify-between items-start mb-2">
                      <div className="flex-1">
                        <h3 className="font-semibold text-sm">{item.questionTitle}</h3>
                        <div className="flex items-center gap-2 mt-1">
                          <span className={`text-xs px-2 py-1 rounded ${
                            item.myChoice === 'YES' ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'
                          }`}>
                            내 선택: {item.myChoice}
                          </span>
                          <span className="text-xs text-slate-500">
                            결과: {item.finalResult}
                          </span>
                        </div>
                      </div>
                      <div className="text-right">
                        {item.isWinner ? (
                          <CheckCircle className="text-green-500" size={24} />
                        ) : (
                          <XCircle className="text-red-500" size={24} />
                        )}
                      </div>
                    </div>

                    <div className="flex justify-between items-center text-sm mt-3 pt-3 border-t border-slate-200">
                      <div className="flex gap-4">
                        <div>
                          <span className="text-slate-600">베팅액:</span>
                          <span className="ml-1 font-semibold">{item.betAmount.toLocaleString()}P</span>
                        </div>
                        <div>
                          <span className="text-slate-600">배당금:</span>
                          <span className="ml-1 font-semibold">{item.payout.toLocaleString()}P</span>
                        </div>
                      </div>
                      <div className={`flex items-center gap-1 font-bold ${
                        item.profit >= 0 ? 'text-green-600' : 'text-red-600'
                      }`}>
                        {item.profit >= 0 ? <TrendingUp size={16} /> : <TrendingDown size={16} />}
                        {item.profit >= 0 ? '+' : ''}{item.profit.toLocaleString()}P
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </>
        )}

        <div className="mt-6 text-center">
          <button
            onClick={onClose}
            className="px-6 py-2 bg-slate-200 text-slate-800 rounded-md hover:bg-slate-300 transition font-semibold"
          >
            닫기
          </button>
        </div>
      </div>
    </div>
  );
}
