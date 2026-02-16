'use client';

import { useState, useEffect } from 'react';
import { useTheme } from '@/hooks/useTheme';
import { orderApi } from '@/lib/api';
import { getMyPositionsByQuestion, PositionData } from '@/lib/api/position';
import type { OrderData } from '@/lib/api/order';

type Tab = 'orders' | 'positions';

interface MyBetsPanelProps {
  questionId: number;
}

export default function MyBetsPanel({ questionId }: MyBetsPanelProps) {
  const { isDark } = useTheme();
  const [activeTab, setActiveTab] = useState<Tab>('orders');
  const [orders, setOrders] = useState<OrderData[]>([]);
  const [positions, setPositions] = useState<PositionData[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    fetchData();
  }, [activeTab, questionId]);

  const fetchData = async () => {
    setLoading(true);
    try {
      if (activeTab === 'orders') {
        const orderData = await orderApi.getOrdersByQuestion(questionId);
        setOrders(orderData);
      } else {
        const positionData = await getMyPositionsByQuestion(questionId);
        setPositions(positionData);
      }
    } catch (error) {
      console.error('Failed to fetch data:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleCancelOrder = async (orderId: number) => {
    try {
      await orderApi.cancelOrder(orderId);
      // Refresh orders
      fetchData();
    } catch (error) {
      console.error('Failed to cancel order:', error);
    }
  };

  return (
    <div className={`p-6 rounded-2xl border ${isDark ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-100 shadow-sm'}`}>
      {/* Tabs */}
      <div className="flex border-b border-slate-200 dark:border-slate-700 mb-4">
        <button
          onClick={() => setActiveTab('orders')}
          className={`px-6 py-3 font-semibold text-sm transition-colors ${
            activeTab === 'orders'
              ? 'border-b-2 border-indigo-600 text-indigo-600'
              : isDark
              ? 'text-slate-400 hover:text-slate-300'
              : 'text-slate-600 hover:text-slate-900'
          }`}
        >
          내 주문
        </button>
        <button
          onClick={() => setActiveTab('positions')}
          className={`px-6 py-3 font-semibold text-sm transition-colors ${
            activeTab === 'positions'
              ? 'border-b-2 border-indigo-600 text-indigo-600'
              : isDark
              ? 'text-slate-400 hover:text-slate-300'
              : 'text-slate-600 hover:text-slate-900'
          }`}
        >
          내 포지션
        </button>
      </div>

      {/* Content */}
      {loading ? (
        <div className="text-center py-8">
          <div className="text-slate-400 text-sm">Loading...</div>
        </div>
      ) : activeTab === 'orders' ? (
        <div className="space-y-2">
          {orders.length === 0 ? (
            <p className="text-slate-500 text-sm text-center py-8">미체결 주문이 없습니다</p>
          ) : (
            orders.map(order => (
              <div
                key={order.orderId}
                className={`flex justify-between items-center p-4 rounded-xl ${
                  isDark ? 'bg-slate-800/50' : 'bg-slate-50'
                }`}
              >
                <div className="flex-1">
                  <div className="flex items-center gap-2 mb-1">
                    <span
                      className={`font-bold ${
                        order.side === 'YES' ? 'text-emerald-600' : 'text-rose-600'
                      }`}
                    >
                      {order.side}
                    </span>
                    <span className="text-sm text-slate-400">@ ${order.price.toFixed(2)}</span>
                  </div>
                  <div className="text-sm text-slate-500">
                    {order.remainingAmount}/{order.amount} remaining
                  </div>
                  <div className="text-xs text-slate-400 mt-1">
                    Status: <span className="font-medium">{order.status}</span>
                  </div>
                </div>
                <button
                  onClick={() => handleCancelOrder(order.orderId)}
                  className="text-sm text-rose-600 hover:text-rose-700 font-medium transition-colors px-4 py-2 rounded-lg hover:bg-rose-50 dark:hover:bg-rose-900/20"
                >
                  취소
                </button>
              </div>
            ))
          )}
        </div>
      ) : (
        <div className="space-y-2">
          {positions.length === 0 ? (
            <p className="text-slate-500 text-sm text-center py-8">체결된 포지션이 없습니다</p>
          ) : (
            positions.map(pos => (
              <div
                key={pos.positionId}
                className={`p-4 rounded-xl ${
                  isDark ? 'bg-slate-800/50' : 'bg-slate-50'
                }`}
              >
                <div className="flex justify-between items-start">
                  <div className="flex-1">
                    <div className="flex items-center gap-2 mb-2">
                      <span
                        className={`font-bold text-lg ${
                          pos.side === 'YES' ? 'text-emerald-600' : 'text-rose-600'
                        }`}
                      >
                        {pos.side}
                      </span>
                    </div>
                    <div className="grid grid-cols-2 gap-x-4 gap-y-1 text-sm">
                      <div className={isDark ? 'text-slate-400' : 'text-slate-600'}>
                        Qty: <span className="font-medium">{pos.quantity}</span>
                      </div>
                      <div className={isDark ? 'text-slate-400' : 'text-slate-600'}>
                        Avg: <span className="font-medium">${pos.avgPrice.toFixed(2)}</span>
                      </div>
                      {pos.currentMidPrice && (
                        <>
                          <div className={isDark ? 'text-slate-400' : 'text-slate-600'}>
                            Current: <span className="font-medium">${pos.currentMidPrice.toFixed(2)}</span>
                          </div>
                        </>
                      )}
                    </div>
                  </div>
                  <div className="text-right">
                    <div
                      className={`text-lg font-bold ${
                        pos.unrealizedPnL >= 0 ? 'text-emerald-600' : 'text-rose-600'
                      }`}
                    >
                      {pos.unrealizedPnL >= 0 ? '+' : ''}${pos.unrealizedPnL.toFixed(2)}
                    </div>
                    <div className="text-xs text-slate-400 mt-1">unrealized P&L</div>
                  </div>
                </div>
              </div>
            ))
          )}
        </div>
      )}
    </div>
  );
}
