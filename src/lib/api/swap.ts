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

// ===== API Functions =====

export const swapApi = {
  /**
   * 스왑 실행
   */
  executeSwap: async (request: SwapRequest) => {
    const response = await apiRequest<{ success: boolean; data: SwapResponse }>('/api/swap', {
      method: 'POST',
      body: JSON.stringify(request),
    });
    return unwrapApiEnvelope(response);
  },

  /**
   * 스왑 시뮬레이션
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
   * 풀 상태 조회
   */
  getPoolState: async (questionId: number) => {
    const response = await apiRequest<{ success: boolean; data: PoolStateResponse }>(`/api/pool/${questionId}`);
    return unwrapApiEnvelope(response);
  },

  /**
   * 내 포지션 조회
   */
  getMyShares: async (questionId: number) => {
    const response = await apiRequest<{ success: boolean; data: MySharesSnapshot }>(`/api/swap/my-shares/${questionId}`);
    return unwrapApiEnvelope(response);
  },

  /**
   * 가격 히스토리 조회
   */
  getPriceHistory: async (questionId: number, limit: number = 100) => {
    const response = await apiRequest<{ success: boolean; data: PricePointResponse[] }>(`/api/swap/price-history/${questionId}?limit=${limit}`);
    return unwrapApiEnvelope(response);
  },
};
