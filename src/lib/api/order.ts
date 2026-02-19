import { apiRequest } from './core';

export interface OrderBookLevel {
  price: number;
  amount: number;
  count: number;
}

export interface OrderBookData {
  questionId: number;
  bids: OrderBookLevel[];
  asks: OrderBookLevel[];
  lastPrice?: number;
  spread?: number;
}

export interface CreateOrderRequest {
  questionId: number;
  side: 'YES' | 'NO';
  // MARKET orders don't send price (server fills at best price), LIMIT orders require price
  price?: number;
  amount: number;
  direction?: 'BUY' | 'SELL'; // BUY: Buy (USDC deposit), SELL: Sell (position collateral), defaults to BUY
  orderType?: 'LIMIT' | 'MARKET'; // Order type, defaults to LIMIT
}

export interface CreateOrderResponse {
  success: boolean;
  message?: string;
  orderId?: number;
  filledAmount: number;
  remainingAmount: number;
}

export interface OrderData {
  orderId: number;
  memberId: number;
  questionId: number;
  side: 'YES' | 'NO';
  direction: 'BUY' | 'SELL';
  price: number;
  amount: number;
  remainingAmount: number;
  status: 'OPEN' | 'FILLED' | 'PARTIAL' | 'CANCELLED';
  createdAt: string;
  updatedAt: string;
}

export const orderApi = {
  getOrderBook: (questionId: number) =>
    apiRequest<OrderBookData>(`/api/questions/${questionId}/orderbook`),

  createOrder: (data: CreateOrderRequest) =>
    apiRequest<CreateOrderResponse>('/api/orders', {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  cancelOrder: (orderId: number) =>
    apiRequest<{ success: boolean; message?: string; refundedAmount?: number }>(
      `/api/orders/${orderId}`,
      { method: 'DELETE' }
    ),

  getActiveOrders: () =>
    apiRequest<OrderData[]>('/api/orders/me'),

  getOrdersByQuestion: (questionId: number) =>
    apiRequest<OrderData[]>(`/api/orders/me/question/${questionId}`),
};
