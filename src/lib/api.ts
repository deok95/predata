export { API_BASE_URL, BACKEND_URL, ApiError, apiRequest, authFetch, unwrapApiEnvelope } from './api/core';
export { authApi, memberApi } from './api/account';
export { bettingApi, questionApi, settlementApi } from './api/market';
export {
  CreateOrderRequest,
  CreateOrderResponse,
  OrderBookData,
  OrderBookLevel,
  OrderData,
  orderApi,
} from './api/order';
export { faucetApi, paymentApi, transactionApi, votingPassApi } from './api/finance';
export { notificationApi } from './api/notification';
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
import { faucetApi, paymentApi, transactionApi, votingPassApi } from './api/finance';
import { notificationApi } from './api/notification';
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
  faucet: faucetApi,
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
};

export default api;
