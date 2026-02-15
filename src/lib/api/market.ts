import type {
  Activity,
  ApiResponse,
  BetRequest,
  CreateQuestionRequest,
  Question,
  SettleQuestionRequest,
  SettlementHistory,
  VoteRequest,
} from '@/types/api';
import { apiRequest, unwrapApiEnvelope } from './core';
import { mapQuestion } from './mappers';

export const questionApi = {
  getAll: async (): Promise<ApiResponse<Question[]>> => {
    const raw = await apiRequest<Record<string, unknown>[] | { success: boolean; data?: Record<string, unknown>[] }>('/api/questions');
    const data = unwrapApiEnvelope(raw);
    const questions = Array.isArray(data) ? data.map((item) => mapQuestion(item)) : [];
    return { success: true, data: questions };
  },

  getById: async (id: number): Promise<ApiResponse<Question>> => {
    const raw = await apiRequest<Record<string, unknown> | { success: boolean; data?: Record<string, unknown> }>(`/api/questions/${id}`);
    return { success: true, data: mapQuestion(unwrapApiEnvelope(raw)) };
  },

  getOdds: async (id: number) => {
    const raw = await apiRequest<
      | { success: boolean; data?: { yesOdds: number; noOdds: number; poolYes: number; poolNo: number; totalPool: number } }
      | { yesOdds: number; noOdds: number; poolYes: number; poolNo: number; totalPool: number }
    >(`/api/questions/${id}/odds`);
    return unwrapApiEnvelope(raw);
  },

  create: async (data: CreateQuestionRequest): Promise<ApiResponse<Record<string, unknown>>> => {
    const raw = await apiRequest<Record<string, unknown>>('/api/admin/questions', {
      method: 'POST',
      body: JSON.stringify(data),
    });
    return { success: true, data: raw };
  },

  delete: async (id: number): Promise<ApiResponse<void>> => {
    const raw = await apiRequest<{ success?: boolean }>(`/api/admin/questions/${id}`, {
      method: 'DELETE',
    });
    return { success: raw?.success ?? true };
  },

  getAdminList: async (): Promise<ApiResponse<Question[]>> => {
    const raw = await apiRequest<Record<string, unknown>[]>('/api/admin/questions');
    return { success: true, data: Array.isArray(raw) ? raw.map((item) => mapQuestion(item)) : [] };
  },
};

export const bettingApi = {
  vote: (data: VoteRequest) =>
    apiRequest<{ success: boolean; message?: string; activityId?: number; remainingTickets?: number }>('/api/vote', {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  bet: (data: BetRequest) =>
    apiRequest<{ success: boolean; message?: string; activityId?: number; remainingTickets?: number }>('/api/bet', {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  getActivitiesByQuestion: async (questionId: number, type?: 'VOTE' | 'BET'): Promise<ApiResponse<Activity[]>> => {
    const raw = await apiRequest<Activity[]>(`/api/activities/question/${questionId}${type ? `?type=${type}` : ''}`);
    return { success: true, data: Array.isArray(raw) ? raw : [] };
  },

  getActivitiesByMember: async (memberId: number, type?: 'VOTE' | 'BET'): Promise<ApiResponse<Activity[]>> => {
    const raw = await apiRequest<Activity[]>(`/api/activities/member/${memberId}${type ? `?type=${type}` : ''}`);
    return { success: true, data: Array.isArray(raw) ? raw : [] };
  },
};

export const settlementApi = {
  settle: (questionId: number, data: SettleQuestionRequest) =>
    apiRequest<Record<string, unknown>>(`/api/questions/${questionId}/settle`, {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  finalize: (questionId: number, force: boolean = false) =>
    apiRequest<Record<string, unknown>>(`/api/questions/${questionId}/settle/finalize`, {
      method: 'POST',
      body: JSON.stringify({ force }),
    }),

  cancel: (questionId: number) =>
    apiRequest<Record<string, unknown>>(`/api/questions/${questionId}/settle/cancel`, {
      method: 'POST',
    }),

  getHistory: async (memberId: number): Promise<ApiResponse<SettlementHistory[]>> => {
    const raw = await apiRequest<SettlementHistory[]>(`/api/settlements/history/${memberId}`);
    return { success: true, data: Array.isArray(raw) ? raw : [] };
  },
};
