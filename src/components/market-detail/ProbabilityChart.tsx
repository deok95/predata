'use client';

import { useState, useEffect } from 'react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Scatter } from 'recharts';
import { useTheme } from '@/hooks/useTheme';
import { getPriceHistory } from '@/lib/api/price';

interface ProbabilityChartProps {
  yesPercent: number;
  totalPool: number;
  yesPool: number;
  noPool: number;
  questionId: number;
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
  questionId
}: ProbabilityChartProps) {
  const { isDark } = useTheme();
  const [chartData, setChartData] = useState<ChartDataPoint[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchPriceHistory = async () => {
      setLoading(true);
      try {
        const history = await getPriceHistory(questionId, '1m', 100);

        if (history.length === 0) {
          // No data: use mock data
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
          // Real data from API
          const formatted = history.reverse().map(d => {
            const timestamp = new Date(d.timestamp);
            const hours = timestamp.getHours().toString().padStart(2, '0');
            const mins = timestamp.getMinutes().toString().padStart(2, '0');

            return {
              time: `${hours}:${mins}`,
              probability: d.midPrice ? d.midPrice * 100 : null,
              lastTrade: d.lastTradePrice ? d.lastTradePrice * 100 : null,
            };
          });
          setChartData(formatted);
        }
      } catch (error) {
        console.error('Failed to fetch price history:', error);
        // Fallback to current price
        setChartData([{
          time: 'Now',
          probability: yesPercent,
          lastTrade: null,
        }]);
      } finally {
        setLoading(false);
      }
    };

    fetchPriceHistory();
  }, [questionId, yesPercent]);

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
            {'$'}{totalPool.toLocaleString()}
          </span>
        </div>
      </div>

      {/* Chart */}
      <div className="mt-6" style={{ height: 240 }}>
        {loading ? (
          <div className="flex items-center justify-center h-full">
            <div className="text-slate-400 text-sm">Loading chart...</div>
          </div>
        ) : (
          <ResponsiveContainer width="100%" height="100%">
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
                formatter={(value: number) => `${value.toFixed(1)}%`}
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
      </div>
    </div>
  );
}
