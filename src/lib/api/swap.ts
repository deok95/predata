import { apiRequest, unwrapApiEnvelope } from './core';

// ===== Request Types =====

export interface SwapRequest {
  questionId: number;
  action: 'BUY' | 'SELL';
  outcome: 'YES' | 'NO';
  usdcIn?: number;
  sharesIn?: number;
  minSharesOut?: number;
  minUsdcOut?: number;
}

// ===== Response Types =====

export interface PriceSnapshot {
  yes: number;
  no: number;
}

export interface PoolSnapshot {
  yesShares: number;
  noShares: number;
  collateralLocked: number;
}

export interface MySharesSnapshot {
  yesShares: number;
  noShares: number;
  yesCostBasis: number;
  noCostBasis: number;
}

export interface SwapResponse {
  sharesAmount: number;
  usdcAmount: number;
  effectivePrice: number;
  fee: number;
  priceBefore: PriceSnapshot;
  priceAfter: PriceSnapshot;
  poolState: PoolSnapshot;
  myShares: MySharesSnapshot;
}

export interface SwapSimulationResponse {
  sharesOut?: number;
  usdcOut?: number;
  effectivePrice: number;
  slippage: number;
  fee: number;
  minReceived: number;
  priceBefore: PriceSnapshot;
  priceAfter: PriceSnapshot;
}

export interface PoolStateResponse {
  questionId: number;
  status: string;
  yesShares: number;
  noShares: number;
  k: number;
  feeRate: number;
  collateralLocked: number;
  totalVolumeUsdc: number;
  totalFeesUsdc: number;
  currentPrice: PriceSnapshot;
  version: number;
}

export interface PricePointResponse {
  timestamp: string;
  yesPrice: number;
  noPrice: number;
}

export interface SwapHistoryResponse {
  swapId: number;
  memberId: number;
  memberEmail?: string;
  action: 'BUY' | 'SELL';
  outcome: 'YES' | 'NO';
  usdcAmount: number;
  sharesAmount: number;
  effectivePrice: number;
  feeUsdc: number;
  priceAfterYes: number;
  createdAt: string;
}

// ===== API Functions =====

export const swapApi = {
  /**
   * Execute swap
   */
  executeSwap: async (request: SwapRequest) => {
    const response = await apiRequest<{ success: boolean; data: SwapResponse }>('/api/swap', {
      method: 'POST',
      body: JSON.stringify(request),
    });
    return unwrapApiEnvelope(response);
  },

  /**
   * Simulate swap
   */
  simulateSwap: async (params: {
    questionId: number;
    action: 'BUY' | 'SELL';
    outcome: 'YES' | 'NO';
    amount: number;
  }) => {
    const query = new URLSearchParams({
      questionId: String(params.questionId),
      action: params.action,
      outcome: params.outcome,
      amount: String(params.amount),
    });
    const response = await apiRequest<{ success: boolean; data: SwapSimulationResponse }>(`/api/swap/simulate?${query}`);
    return unwrapApiEnvelope(response);
  },

  /**
   * Fetch pool state
   */
  getPoolState: async (questionId: number) => {
    const response = await apiRequest<{ success: boolean; data: PoolStateResponse }>(`/api/pool/${questionId}`);
    return unwrapApiEnvelope(response);
  },

  /**
   * Fetch my position
   */
  getMyShares: async (questionId: number) => {
    const response = await apiRequest<{ success: boolean; data: MySharesSnapshot }>(`/api/swap/my-shares/${questionId}`);
    return unwrapApiEnvelope(response);
  },

  /**
   * Fetch price history
   */
  getPriceHistory: async (questionId: number, limit: number = 100) => {
    const response = await apiRequest<{ success: boolean; data: PricePointResponse[] }>(`/api/swap/price-history/${questionId}?limit=${limit}`);
    return unwrapApiEnvelope(response);
  },

  /**
   * Fetch swap history (public)
   */
  getSwapHistory: async (questionId: number, limit: number = 20) => {
    const response = await apiRequest<{ success: boolean; data: SwapHistoryResponse[] }>(
      `/api/swap/history/${questionId}?limit=${limit}`
    );
    return unwrapApiEnvelope(response);
  },

  /**
   * Fetch my swap history (authentication required)
   */
  getMySwapHistory: async (questionId: number, limit: number = 50) => {
    const response = await apiRequest<{ success: boolean; data: SwapHistoryResponse[] }>(
      `/api/swap/my-history/${questionId}?limit=${limit}`
    );
    return unwrapApiEnvelope(response);
  },
};
