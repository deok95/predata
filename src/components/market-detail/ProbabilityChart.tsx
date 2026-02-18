'use client';

import { useState, useEffect } from 'react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Scatter } from 'recharts';
import { useTheme } from '@/hooks/useTheme';
import { swapApi } from '@/lib/api/swap';

interface AmmPool {
  collateralLocked: number;
  yesShares: number;
  noShares: number;
  totalVolumeUsdc: number;
  version: number;
}

interface ProbabilityChartProps {
  yesPercent: number;
  totalPool: number;
  yesPool: number;
  noPool: number;
  questionId: number;
  disableApi?: boolean;
  executionModel?: 'AMM_FPMM' | 'ORDERBOOK_LEGACY';
  ammPool?: AmmPool | null;
}

interface ChartDataPoint {
  time: string;
  probability: number | null;
  lastTrade: number | null;
}

export default function ProbabilityChart({
  yesPercent,
  totalPool,
  yesPool,
  noPool,
  questionId,
  disableApi = false,
  executionModel = 'ORDERBOOK_LEGACY',
  ammPool = null
}: ProbabilityChartProps) {
  const { isDark } = useTheme();
  const [chartData, setChartData] = useState<ChartDataPoint[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchPriceHistory = async () => {
      setLoading(true);

      if (disableApi) {
        const mockData: ChartDataPoint[] = Array.from({ length: 24 }, (_, i) => {
          const now = new Date();
          const timeAgo = new Date(now.getTime() - (23 - i) * 3600000);
          const hours = timeAgo.getHours().toString().padStart(2, '0');
          const mins = timeAgo.getMinutes().toString().padStart(2, '0');

          return {
            time: `${hours}:${mins}`,
            probability: yesPercent,
            lastTrade: null,
          };
        });
        setChartData(mockData);
        setLoading(false);
        return;
      }

      try {
        // Only call AMM API if question uses AMM execution model
        if (executionModel !== 'AMM_FPMM') {
          // ORDERBOOK_LEGACY: show horizontal line with current yesPercent
          const mockData: ChartDataPoint[] = Array.from({ length: 24 }, (_, i) => {
            const now = new Date();
            const timeAgo = new Date(now.getTime() - (23 - i) * 3600000);
            const hours = timeAgo.getHours().toString().padStart(2, '0');
            const mins = timeAgo.getMinutes().toString().padStart(2, '0');

            return {
              time: `${hours}:${mins}`,
              probability: yesPercent,
              lastTrade: null,
            };
          });
          setChartData(mockData);
          setLoading(false);
          return;
        }

        // Use swap price history from AMM
        const history = await swapApi.getPriceHistory(questionId, 100);

        if (history.length === 0) {
          // No pool (ORDERBOOK_LEGACY) - show horizontal line with current yesPercent
          const mockData: ChartDataPoint[] = Array.from({ length: 24 }, (_, i) => {
            const now = new Date();
            const timeAgo = new Date(now.getTime() - (23 - i) * 3600000);
            const hours = timeAgo.getHours().toString().padStart(2, '0');
            const mins = timeAgo.getMinutes().toString().padStart(2, '0');

            return {
              time: `${hours}:${mins}`,
              probability: yesPercent,
              lastTrade: null,
            };
          });
          setChartData(mockData);
        } else {
          // Real swap data from AMM
          const formatted = history.map(d => {
            const timestamp = new Date(d.timestamp);
            const hours = timestamp.getHours().toString().padStart(2, '0');
            const mins = timestamp.getMinutes().toString().padStart(2, '0');

            return {
              time: `${hours}:${mins}`,
              probability: d.yesPrice * 100,
              lastTrade: d.yesPrice * 100,
            };
          });
          setChartData(formatted);
        }
      } catch (error) {
        console.error('Failed to fetch price history:', error);
        // Fallback to horizontal line with current yesPercent
        const mockData: ChartDataPoint[] = Array.from({ length: 24 }, (_, i) => {
          const now = new Date();
          const timeAgo = new Date(now.getTime() - (23 - i) * 3600000);
          const hours = timeAgo.getHours().toString().padStart(2, '0');
          const mins = timeAgo.getMinutes().toString().padStart(2, '0');

          return {
            time: `${hours}:${mins}`,
            probability: yesPercent,
            lastTrade: null,
          };
        });
        setChartData(mockData);
      } finally {
        setLoading(false);
      }
    };

    fetchPriceHistory();
  }, [questionId, yesPercent, disableApi, executionModel, ammPool?.version]);

  const yesChange = yesPercent >= 50 ? `+${yesPercent - 50}` : `${yesPercent - 50}`;
  const isPositive = yesPercent >= 50;

  return (
    <div className={`p-8 rounded-[2.5rem] border ${isDark ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-100 shadow-sm'}`}>
      {/* Header */}
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
          <span className={`font-bold text-lg ${isDark ? 'text-white' : 'text-slate-900'}`}>
            {'$'}{Number(totalPool).toLocaleString()}
          </span>
        </div>
      </div>

      {/* Chart */}
      <div className="mt-6" style={{ width: '100%', minHeight: 240 }}>
        {loading ? (
          <div className="flex items-center justify-center h-full">
            <div className="text-slate-400 text-sm">Loading chart...</div>
          </div>
        ) : (
          <ResponsiveContainer width="100%" height={240} minWidth={300} minHeight={240}>
            <LineChart data={chartData}>
              <defs>
                <linearGradient id="colorProbability" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stopColor="#6366f1" stopOpacity={0.3} />
                  <stop offset="100%" stopColor="#6366f1" stopOpacity={0} />
                </linearGradient>
              </defs>
              <CartesianGrid
                strokeDasharray="3 3"
                stroke={isDark ? '#334155' : '#e2e8f0'}
                horizontal={true}
                vertical={false}
              />
              <XAxis
                dataKey="time"
                stroke={isDark ? '#64748b' : '#94a3b8'}
                fontSize={10}
                tickLine={false}
                tick={{ fill: isDark ? '#64748b' : '#94a3b8', fontSize: 10 }}
              />
              <YAxis
                domain={[0, 100]}
                tickFormatter={(v) => `${v}%`}
                stroke={isDark ? '#64748b' : '#94a3b8'}
                fontSize={10}
                tickLine={false}
                tick={{ fill: isDark ? '#64748b' : '#94a3b8', fontSize: 10 }}
              />
              <Tooltip
                formatter={(value: unknown) => typeof value === 'number' ? `${value.toFixed(1)}%` : String(value ?? '')}
                contentStyle={{
                  backgroundColor: isDark ? '#1e293b' : '#ffffff',
                  border: `1px solid ${isDark ? '#334155' : '#e2e8f0'}`,
                  borderRadius: '0.5rem',
                  fontSize: '12px',
                }}
                labelStyle={{ color: isDark ? '#cbd5e1' : '#64748b' }}
              />
              <Line
                type="monotone"
                dataKey="probability"
                stroke="#6366f1"
                strokeWidth={2}
                dot={false}
                fill="url(#colorProbability)"
              />
              <Scatter
                dataKey="lastTrade"
                fill="#10b981"
                r={3}
              />
            </LineChart>
          </ResponsiveContainer>
        )}
      </div>

      {/* Pool Cards */}
      <div className="grid grid-cols-2 gap-4 mt-6">
        {ammPool ? (
          <>
            {/* AMM Liquidity & Volume */}
            <div className={`p-4 rounded-2xl ${isDark ? 'bg-indigo-950/20' : 'bg-indigo-50'}`}>
              <p className="text-xs font-bold text-indigo-500 mb-1">총 유동성</p>
              <p className={`text-lg font-black ${isDark ? 'text-white' : 'text-slate-900'}`}>
                {'$'}{ammPool.collateralLocked.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
              </p>
              <p className="text-[10px] text-slate-400 mt-0.5">
                Collateral Locked
              </p>
            </div>
            <div className={`p-4 rounded-2xl ${isDark ? 'bg-purple-950/20' : 'bg-purple-50'}`}>
              <p className="text-xs font-bold text-purple-500 mb-1">거래량</p>
              <p className={`text-lg font-black ${isDark ? 'text-white' : 'text-slate-900'}`}>
                {'$'}{ammPool.totalVolumeUsdc.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
              </p>
              <p className="text-[10px] text-slate-400 mt-0.5">
                Total Volume
              </p>
            </div>
          </>
        ) : (
          <>
            {/* Legacy Pool Statistics */}
            <div className={`p-4 rounded-2xl ${isDark ? 'bg-emerald-950/20' : 'bg-emerald-50'}`}>
              <p className="text-xs font-bold text-emerald-500 mb-1">Yes Pool</p>
              <p className={`text-lg font-black ${isDark ? 'text-white' : 'text-slate-900'}`}>
                {'$'}{yesPool.toLocaleString()}
              </p>
              <p className="text-[10px] text-slate-400 mt-0.5">
                {totalPool > 0 ? ((yesPool / totalPool) * 100).toFixed(1) : '0'}% of total
              </p>
            </div>
            <div className={`p-4 rounded-2xl ${isDark ? 'bg-rose-950/20' : 'bg-rose-50'}`}>
              <p className="text-xs font-bold text-rose-500 mb-1">No Pool</p>
              <p className={`text-lg font-black ${isDark ? 'text-white' : 'text-slate-900'}`}>
                {'$'}{noPool.toLocaleString()}
              </p>
              <p className="text-[10px] text-slate-400 mt-0.5">
                {totalPool > 0 ? ((noPool / totalPool) * 100).toFixed(1) : '0'}% of total
              </p>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
