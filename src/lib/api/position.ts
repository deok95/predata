import { apiClient } from './core';

/**
 * 포지션 데이터
 */
export interface PositionData {
  positionId: number;
  questionId: number;
  questionTitle: string;
  side: 'YES' | 'NO';
  quantity: number;
  avgPrice: number;
  currentMidPrice: number | null;
  unrealizedPnL: number;
  createdAt: string;
  updatedAt: string;
}

/**
 * 내 포지션 조회 (전체)
 * GET /api/positions/me
 */
export async function getMyPositions(): Promise<PositionData[]> {
  try {
    const response = await apiClient('/api/positions/me');
    return response.data || [];
  } catch (error) {
    console.error('Failed to fetch positions:', error);
    return [];
  }
}

/**
 * 특정 질문에 대한 내 포지션 조회
 * GET /api/positions/me/question/{id}
 */
export async function getMyPositionsByQuestion(questionId: number): Promise<PositionData[]> {
  try {
    const response = await apiClient(`/api/positions/me/question/${questionId}`);
    return response.data || [];
  } catch (error) {
    console.error('Failed to fetch positions by question:', error);
    return [];
  }
}
