import { apiRequest } from './core';

/**
 * Position data
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
 * Fetch my positions (all)
 * GET /api/positions/me
 */
export async function getMyPositions(): Promise<PositionData[]> {
  try {
    const response = await apiRequest<{ success: boolean; data: PositionData[] }>('/api/positions/me');
    return response.data || [];
  } catch (error) {
    console.error('Failed to fetch positions:', error);
    return [];
  }
}

/**
 * Fetch my positions for specific question
 * GET /api/positions/me/question/{id}
 */
export async function getMyPositionsByQuestion(questionId: number): Promise<PositionData[]> {
  try {
    const response = await apiRequest<{ success: boolean; data: PositionData[] }>(`/api/positions/me/question/${questionId}`);
    return response.data || [];
  } catch (error) {
    console.error('Failed to fetch positions by question:', error);
    return [];
  }
}
