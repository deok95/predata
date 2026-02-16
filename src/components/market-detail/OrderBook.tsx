'use client';

import { useState, useEffect, useMemo, useCallback } from 'react';
import { ChevronDown, RefreshCw } from 'lucide-react';
import { useTheme } from '@/hooks/useTheme';
import { orderApi, type OrderBookData, type OrderBookLevel } from '@/lib/api';

interface OrderBookProps {
  questionId: number;
  yesPercent: number;  // fallback용
  totalPool: number;   // fallback용
}

export default function OrderBook({ questionId, yesPercent, totalPool }: OrderBookProps) {
  const { isDark } = useTheme();
  const [orderBook, setOrderBook] = useState<OrderBookData | null>(null);
  const [loading, setLoading] = useState(false);
  const [useApi, setUseApi] = useState(true);

  const fetchOrderBook = useCallback(async () => {
    if (!useApi) return;

    setLoading(true);
    try {
      const data = await orderApi.getOrderBook(questionId);
      setOrderBook(data);
    } catch {
      // API 실패 시 fallback 모드로 전환
      setUseApi(false);
      setOrderBook(null);
    } finally {
      setLoading(false);
    }
  }, [questionId, useApi]);

  useEffect(() => {
    fetchOrderBook();
    if (!useApi) return;

    const interval = setInterval(() => {
      if (typeof document !== 'undefined' && document.visibilityState !== 'visible') {
        return;
      }
      fetchOrderBook();
    }, 5000); // 5초마다 갱신 (hidden 탭에서는 중지)

    return () => clearInterval(interval);
  }, [fetchOrderBook, useApi]);

  // Fallback 데이터 (API 실패 시 또는 빈 오더북)
  const fallbackData = useMemo(() => {
    const yesPrice = yesPercent / 100;
    const noPrice = 1 - yesPrice;
    const yesPool = totalPool * yesPrice;
    const noPool = totalPool * noPrice;

    const levels = [
      { offset: 0, share: 0.35 },
      { offset: 1, share: 0.25 },
      { offset: 2, share: 0.20 },
      { offset: 3, share: 0.12 },
      { offset: 4, share: 0.08 },
    ];

    const bids: OrderBookLevel[] = levels
      .map(({ offset, share }) => ({
        price: Math.max(0.01, yesPrice - offset * 0.01),
        amount: Math.round(yesPool * share),
        count: 1,
      }))
      .filter(o => o.price > 0 && o.price < 1);

    const asks: OrderBookLevel[] = levels
      .map(({ offset, share }) => ({
        price: Math.min(0.99, yesPrice + offset * 0.01),
        amount: Math.round(noPool * share),
        count: 1,
      }))
      .filter(o => o.price > 0 && o.price < 1);

    const bestBid = bids[0]?.price ?? 0;
    const bestAsk = asks[0]?.price ?? 0;
    const spread = Math.abs(bestAsk - bestBid);

    return { bids, asks, spread };
  }, [yesPercent, totalPool]);

  // API 데이터 또는 fallback 사용
  const hasApiData = orderBook && (orderBook.bids.length > 0 || orderBook.asks.length > 0);
  const bids = hasApiData ? orderBook.bids : fallbackData.bids;
  const asks = hasApiData ? orderBook.asks : fallbackData.asks;
  const spread = hasApiData && orderBook.spread != null
    ? (orderBook.spread * 100).toFixed(1)
    : (fallbackData.spread * 100).toFixed(1);

  const maxVolume = Math.max(
    ...bids.map(o => o.amount),
    ...asks.map(o => o.amount),
    1
  );

  const totalLiquidity = bids.reduce((s, o) => s + o.amount, 0) + asks.reduce((s, o) => s + o.amount, 0);

  return (
    <div className={`p-6 rounded-3xl border ${isDark ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-100'}`}>
      <div className={`flex items-center justify-between border-b pb-4 mb-4 ${isDark ? 'border-slate-800' : 'border-slate-100'}`}>
        <div className="flex items-center gap-2">
          <h3 className={`font-black text-lg ${isDark ? 'text-white' : 'text-slate-900'}`}>Order Book</h3>
          {!useApi && (
            <span className="text-[8px] font-bold px-1.5 py-0.5 rounded bg-amber-500/20 text-amber-500">
              SIMULATED
            </span>
          )}
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={fetchOrderBook}
            className={`p-1 rounded transition-colors ${isDark ? 'hover:bg-slate-800' : 'hover:bg-slate-100'}`}
            disabled={loading}
          >
            <RefreshCw size={14} className={`text-slate-400 ${loading ? 'animate-spin' : ''}`} />
          </button>
          <span className={`text-[10px] font-bold px-2 py-0.5 rounded-lg ${isDark ? 'bg-slate-800 text-slate-400' : 'bg-slate-100 text-slate-500'}`}>
            Spread {spread}%
          </span>
          <ChevronDown className="text-slate-400" size={16} />
        </div>
      </div>

      {/* 헤더 */}
      <div className="grid grid-cols-2 gap-10 mb-3">
        <div className="flex justify-between text-[10px] font-black text-slate-400 uppercase">
          <span>Price</span>
          <span>Volume</span>
        </div>
        <div className="flex justify-between text-[10px] font-black text-slate-400 uppercase">
          <span>Volume</span>
          <span>Price</span>
        </div>
      </div>

      {/* 오더북 데이터 */}
      <div className="grid grid-cols-2 gap-10">
        {/* Bids (YES 매수) */}
        <div className="space-y-1.5">
          <p className="text-[10px] font-black text-emerald-500 uppercase mb-2">Yes Bids</p>
          {bids.slice(0, 5).map((order, i) => (
            <div key={i} className="relative flex justify-between text-xs font-mono text-slate-500 py-0.5">
              <div
                className="absolute left-0 top-0 bottom-0 bg-emerald-500/10 rounded-r"
                style={{ width: `${(order.amount / maxVolume) * 100}%` }}
              />
              <span className="text-emerald-500 font-bold relative z-10">
                ${order.price.toFixed(2)}
              </span>
              <span className="relative z-10">
                {order.amount.toLocaleString()}
                {order.count > 1 && <span className="text-slate-600 ml-1">({order.count})</span>}
              </span>
            </div>
          ))}
          {bids.length === 0 && (
            <p className="text-xs text-slate-500 py-2">No bids</p>
          )}
        </div>

        {/* Asks (YES 매도 = NO 매수 역산) */}
        <div className="space-y-1.5 text-right">
          <p className="text-[10px] font-black text-rose-500 uppercase mb-2">Yes Asks</p>
          {asks.slice(0, 5).map((order, i) => (
            <div key={i} className="relative flex justify-between text-xs font-mono text-slate-500 py-0.5">
              <div
                className="absolute right-0 top-0 bottom-0 bg-rose-500/10 rounded-l"
                style={{ width: `${(order.amount / maxVolume) * 100}%` }}
              />
              <span className="relative z-10">
                {order.amount.toLocaleString()}
                {order.count > 1 && <span className="text-slate-600 mr-1">({order.count})</span>}
              </span>
              <span className="text-rose-500 font-bold relative z-10">
                ${order.price.toFixed(2)}
              </span>
            </div>
          ))}
          {asks.length === 0 && (
            <p className="text-xs text-slate-500 py-2">No asks</p>
          )}
        </div>
      </div>

      {/* 총 유동성 */}
      <div className={`flex justify-between mt-4 pt-4 border-t text-xs ${isDark ? 'border-slate-800' : 'border-slate-100'}`}>
        <span className="text-slate-400">Total Liquidity</span>
        <span className={`font-bold ${isDark ? 'text-white' : 'text-slate-900'}`}>
          {'$'}{(hasApiData ? totalLiquidity : totalPool).toLocaleString()}
        </span>
      </div>
    </div>
  );
}
