export { API_BASE_URL, BACKEND_URL, ApiError, apiRequest, authFetch, unwrapApiEnvelope } from './api/core';
export { authApi, memberApi } from './api/account';
export { bettingApi, questionApi, settlementApi } from './api/market';
export { orderApi } from './api/order';
export type {
  CreateOrderRequest,
  CreateOrderResponse,
  OrderBookData,
  OrderBookLevel,
  OrderData,
} from './api/order';
export { paymentApi, transactionApi, votingPassApi } from './api/finance';
export { notificationApi } from './api/notification';
export { ticketApi } from './api/ticket';
export type { TicketStatus } from './api/ticket';
export { getPriceHistory } from './api/price';
export type { PriceHistoryData } from './api/price';
export { getMyPositions, getMyPositionsByQuestion } from './api/position';
export type { PositionData } from './api/position';
export {
  analysisApi,
  analyticsApi,
  blockchainApi,
  globalApi,
  leaderboardApi,
  portfolioApi,
  referralApi,
  rewardApi,
  sportsApi,
  tierApi,
} from './api/insights';

import { authApi, memberApi } from './api/account';
import { bettingApi, questionApi, settlementApi } from './api/market';
import { orderApi } from './api/order';
import { paymentApi, transactionApi, votingPassApi } from './api/finance';
import { notificationApi } from './api/notification';
import { ticketApi } from './api/ticket';
import { getPriceHistory } from './api/price';
import { getMyPositions, getMyPositionsByQuestion } from './api/position';
import {
  analysisApi,
  analyticsApi,
  blockchainApi,
  globalApi,
  leaderboardApi,
  portfolioApi,
  referralApi,
  rewardApi,
  sportsApi,
  tierApi,
} from './api/insights';

export const api = {
  auth: authApi,
  member: memberApi,
  question: questionApi,
  betting: bettingApi,
  settlement: settlementApi,
  blockchain: blockchainApi,
  analytics: analyticsApi,
  analysis: analysisApi,
  sports: sportsApi,
  global: globalApi,
  tier: tierApi,
  reward: rewardApi,
  leaderboard: leaderboardApi,
  order: orderApi,
  notification: notificationApi,
  portfolio: portfolioApi,
  referral: referralApi,
  votingPass: votingPassApi,
  payment: paymentApi,
  transaction: transactionApi,
  ticket: ticketApi,
  price: { getHistory: getPriceHistory },
  position: { getMyPositions, getMyPositionsByQuestion },
};

export default api;
