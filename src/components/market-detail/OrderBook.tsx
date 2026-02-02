'use client';

import { useMemo } from 'react';
import { ChevronDown } from 'lucide-react';
import { useTheme } from '@/hooks/useTheme';

interface OrderBookProps {
  yesPercent: number;
  totalPool: number;
}

export default function OrderBook({ yesPercent, totalPool }: OrderBookProps) {
  const { isDark } = useTheme();

  const { yesOrders, noOrders, spread } = useMemo(() => {
    const yesPrice = yesPercent / 100;
    const noPrice = 1 - yesPrice;
    const yesPool = totalPool * yesPrice;
    const noPool = totalPool * noPrice;

    // 풀 비율 기반 오더 깊이
    const levels = [
      { offset: 0, share: 0.35 },
      { offset: 1, share: 0.25 },
      { offset: 2, share: 0.20 },
      { offset: 3, share: 0.12 },
      { offset: 4, share: 0.08 },
    ];

    const yes = levels
      .map(({ offset, share }) => ({
        price: Math.max(0.01, yesPrice - offset * 0.01).toFixed(2),
        volume: Math.round(yesPool * share),
        barWidth: share * 100 / 0.35,
      }))
      .filter(o => parseFloat(o.price) > 0 && parseFloat(o.price) < 1);

    const no = levels
      .map(({ offset, share }) => ({
        price: Math.min(0.99, noPrice + offset * 0.01).toFixed(2),
        volume: Math.round(noPool * share),
        barWidth: share * 100 / 0.35,
      }))
      .filter(o => parseFloat(o.price) > 0 && parseFloat(o.price) < 1);

    const bestYes = yes.length > 0 ? parseFloat(yes[0].price) : 0;
    const bestNo = no.length > 0 ? parseFloat(no[0].price) : 0;
    const sp = Math.abs(1 - bestYes - bestNo);

    return { yesOrders: yes, noOrders: no, spread: (sp * 100).toFixed(1) };
  }, [yesPercent, totalPool]);

  return (
    <div className={`p-6 rounded-3xl border ${isDark ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-100'}`}>
      <div className={`flex items-center justify-between border-b pb-4 mb-4 ${isDark ? 'border-slate-800' : 'border-slate-100'}`}>
        <h3 className={`font-black text-lg ${isDark ? 'text-white' : 'text-slate-900'}`}>Order Book</h3>
        <div className="flex items-center gap-2">
          <span className={`text-[10px] font-bold px-2 py-0.5 rounded-lg ${isDark ? 'bg-slate-800 text-slate-400' : 'bg-slate-100 text-slate-500'}`}>
            Spread {spread}%
          </span>
          <ChevronDown className="text-slate-400" size={16} />
        </div>
      </div>

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

      <div className="grid grid-cols-2 gap-10">
        <div className="space-y-1.5">
          <p className="text-[10px] font-black text-emerald-500 uppercase mb-2">Yes Bids</p>
          {yesOrders.map((order, i) => (
            <div key={i} className="relative flex justify-between text-xs font-mono text-slate-500 py-0.5">
              <div className="absolute left-0 top-0 bottom-0 bg-emerald-500/10 rounded-r" style={{ width: `${order.barWidth}%` }} />
              <span className="text-emerald-500 font-bold relative z-10">${order.price}</span>
              <span className="relative z-10">{order.volume.toLocaleString()}</span>
            </div>
          ))}
        </div>
        <div className="space-y-1.5 text-right">
          <p className="text-[10px] font-black text-rose-500 uppercase mb-2">No Asks</p>
          {noOrders.map((order, i) => (
            <div key={i} className="relative flex justify-between text-xs font-mono text-slate-500 py-0.5">
              <div className="absolute right-0 top-0 bottom-0 bg-rose-500/10 rounded-l" style={{ width: `${order.barWidth}%` }} />
              <span className="relative z-10">{order.volume.toLocaleString()}</span>
              <span className="text-rose-500 font-bold relative z-10">${order.price}</span>
            </div>
          ))}
        </div>
      </div>

      <div className={`flex justify-between mt-4 pt-4 border-t text-xs ${isDark ? 'border-slate-800' : 'border-slate-100'}`}>
        <span className="text-slate-400">Total Liquidity</span>
        <span className={`font-bold ${isDark ? 'text-white' : 'text-slate-900'}`}>{totalPool.toLocaleString()} P</span>
      </div>
    </div>
  );
}
