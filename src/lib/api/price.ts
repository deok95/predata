import { apiClient } from './core';

/**
 * 가격 이력 데이터
 */
export interface PriceHistoryData {
  timestamp: string;
  midPrice: number | null;
  lastTradePrice: number | null;
  spread: number | null;
}

/**
 * 가격 이력 조회
 * GET /api/questions/{id}/price-history?interval=1m&limit=100
 */
export async function getPriceHistory(
  questionId: number,
  interval: string = '1m',
  limit: number = 100
): Promise<PriceHistoryData[]> {
  try {
    const response = await apiClient(
      `/api/questions/${questionId}/price-history?interval=${interval}&limit=${limit}`
    );
    return response.data || [];
  } catch (error) {
    console.error('Failed to fetch price history:', error);
    return [];
  }
}
