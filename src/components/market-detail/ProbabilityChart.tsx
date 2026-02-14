'use client';

import { useMemo } from 'react';
import { useTheme } from '@/hooks/useTheme';

interface ProbabilityChartProps {
  yesPercent: number;
  totalPool: number;
  yesPool: number;
  noPool: number;
  marketId?: number;
}

export default function ProbabilityChart({ yesPercent, totalPool, yesPool, noPool, marketId = 1 }: ProbabilityChartProps) {
  const { isDark } = useTheme();

  const { linePoints, areaPoints, timeLabels, lastPoint } = useMemo(() => {
    const numPoints = 24;
    const svgWidth = 300;
    const svgHeight = 100;
    const padding = 5;

    const seededRandom = (i: number) => {
      const x = Math.sin(marketId * 127.1 + i * 311.7) * 43758.5453;
      return x - Math.floor(x);
    };

    const startPercent = 50 + (seededRandom(0) - 0.5) * 20;
    const points: { x: number; y: number; percent: number }[] = [];

    for (let i = 0; i < numPoints; i++) {
      const t = i / (numPoints - 1);
      const basePercent = startPercent + (yesPercent - startPercent) * t;
      const noiseScale = Math.max(0, 1 - t) * 12;
      const noise = (seededRandom(i + 1) - 0.5) * 2 * noiseScale;
      const percent = Math.max(5, Math.min(95, basePercent + noise));
      const x = (i / (numPoints - 1)) * svgWidth;
      const y = padding + ((100 - percent) / 100) * (svgHeight - padding * 2);
      points.push({ x, y, percent: Math.round(percent) });
    }

    points[points.length - 1] = {
      x: svgWidth,
      y: padding + ((100 - yesPercent) / 100) * (svgHeight - padding * 2),
      percent: yesPercent,
    };

    const line = points.map(p => `${p.x},${p.y}`).join(' ');
    const area = `${line} ${svgWidth},${svgHeight} 0,${svgHeight}`;

    const now = new Date();
    const labels: string[] = [];
    const labelIndices = [0, 6, 12, 18, 23];
    labelIndices.forEach(idx => {
      const hoursAgo = numPoints - 1 - idx;
      const time = new Date(now.getTime() - hoursAgo * 3600000);
      labels.push(time.getHours().toString().padStart(2, '0') + ':00');
    });

    const last = points[points.length - 1];
    const lastPctY = ((last.y - padding) / (svgHeight - padding * 2)) * 100;

    return { linePoints: line, areaPoints: area, timeLabels: labels, lastPoint: { pctY: lastPctY } };
  }, [yesPercent, marketId]);

  const yesChange = yesPercent >= 50 ? `+${yesPercent - 50}` : `${yesPercent - 50}`;
  const isPositive = yesPercent >= 50;

  // Y축 라벨 위치 (0%, 25%, 50%, 75%, 100% 에 해당하는 CSS top %)
  const yLabels = [
    { label: '100%', top: '0%' },
    { label: '75%', top: '25%' },
    { label: '50%', top: '50%' },
    { label: '25%', top: '75%' },
    { label: '0%', top: '100%' },
  ];

  return (
    <div className={`p-8 rounded-[2.5rem] border ${isDark ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-100 shadow-sm'}`}>
      <div className="flex items-center justify-between mb-2">
        <div>
          <span className="text-5xl font-black text-indigo-600">{yesPercent}%</span>
          <span className="ml-3 text-slate-500 font-bold text-sm">Chance</span>
          <span className={`ml-2 text-xs font-bold px-2 py-0.5 rounded-lg ${isPositive ? 'bg-emerald-500/10 text-emerald-500' : 'bg-rose-500/10 text-rose-500'}`}>
            {yesChange}%
          </span>
        </div>
        <div className="text-right">
          <p className="text-xs text-slate-400 uppercase font-bold mb-1">Pool</p>
          <span className={`font-bold text-lg ${isDark ? 'text-white' : 'text-slate-900'}`}>{'$'}{totalPool.toLocaleString()}</span>
        </div>
      </div>

      {/* 차트 영역 — SVG는 선/그래디언트만, 텍스트는 HTML 오버레이 */}
      <div className="relative mt-6" style={{ height: 240 }}>
        {/* Y축 라벨 (HTML) */}
        <div className="absolute left-0 top-0 bottom-0 w-10 z-10">
          {yLabels.map(({ label, top }) => (
            <span
              key={label}
              className="absolute right-1 text-[10px] font-bold text-slate-400 -translate-y-1/2"
              style={{ top }}
            >
              {label}
            </span>
          ))}
        </div>

        {/* 차트 SVG */}
        <div className="absolute left-10 right-0 top-0 bottom-0">
          {/* 그리드 라인 (HTML) */}
          {[0, 25, 50, 75, 100].map(pct => (
            <div
              key={pct}
              className={`absolute left-0 right-0 border-t border-dashed ${
                pct === 50
                  ? isDark ? 'border-slate-700' : 'border-slate-300'
                  : isDark ? 'border-slate-800/60' : 'border-slate-200/80'
              }`}
              style={{ top: `${pct}%` }}
            />
          ))}

          {/* SVG — 선과 그래디언트 영역만 */}
          <svg
            viewBox="0 0 300 100"
            className="absolute inset-0 w-full h-full"
            preserveAspectRatio="none"
          >
            <defs>
              <linearGradient id={`chartGradient-${marketId}`} x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stopColor="#6366f1" stopOpacity="0.3" />
                <stop offset="100%" stopColor="#6366f1" stopOpacity="0" />
              </linearGradient>
            </defs>
            <polyline points={areaPoints} fill={`url(#chartGradient-${marketId})`} />
            <polyline points={linePoints} fill="none" stroke="#6366f1" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" vectorEffect="non-scaling-stroke" />
          </svg>

          {/* 현재 가격 마커 (HTML) */}
          <div
            className="absolute right-0 z-20 -translate-y-1/2 flex items-center"
            style={{ top: `${lastPoint.pctY}%` }}
          >
            {/* 점 + 펄스 */}
            <div className="relative w-3 h-3">
              <div className="absolute inset-0 bg-indigo-500 rounded-full" />
              <div className="absolute -inset-1 bg-indigo-500/30 rounded-full animate-ping" style={{ animationDuration: '2s' }} />
            </div>
            {/* 라벨 뱃지 */}
            <div className="ml-2 bg-indigo-600 text-white text-[11px] font-bold px-2.5 py-1 rounded-lg whitespace-nowrap">
              Yes {yesPercent}%
            </div>
          </div>
        </div>
      </div>

      {/* 시간 라벨 */}
      <div className="flex justify-between pl-10 mt-2 text-[10px] text-slate-400 font-bold uppercase tracking-widest">
        {timeLabels.map((label, i) => (
          <span key={i} className={i === timeLabels.length - 1 ? 'text-indigo-600' : ''}>
            {i === timeLabels.length - 1 ? 'Now' : label}
          </span>
        ))}
      </div>

      {/* Yes/No Pool 카드 */}
      <div className="grid grid-cols-2 gap-4 mt-6">
        <div className={`p-4 rounded-2xl ${isDark ? 'bg-emerald-950/20' : 'bg-emerald-50'}`}>
          <p className="text-xs font-bold text-emerald-500 mb-1">Yes Pool</p>
          <p className={`text-lg font-black ${isDark ? 'text-white' : 'text-slate-900'}`}>{'$'}{yesPool.toLocaleString()}</p>
          <p className="text-[10px] text-slate-400 mt-0.5">
            {totalPool > 0 ? ((yesPool / totalPool) * 100).toFixed(1) : '0'}% of total
          </p>
        </div>
        <div className={`p-4 rounded-2xl ${isDark ? 'bg-rose-950/20' : 'bg-rose-50'}`}>
          <p className="text-xs font-bold text-rose-500 mb-1">No Pool</p>
          <p className={`text-lg font-black ${isDark ? 'text-white' : 'text-slate-900'}`}>{'$'}{noPool.toLocaleString()}</p>
          <p className="text-[10px] text-slate-400 mt-0.5">
            {totalPool > 0 ? ((noPool / totalPool) * 100).toFixed(1) : '0'}% of total
          </p>
        </div>
      </div>
    </div>
  );
}
