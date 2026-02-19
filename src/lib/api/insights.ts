import type {
  AccuracyTrendPoint,
  ApiResponse,
  BlockchainQuestion,
  CategoryPerformance,
  DashboardData,
  GlobalStats,
  OpenPosition,
  PortfolioSummary,
  QualityDashboard,
  ReferralResult,
  ReferralStats,
  RewardSummary,
  TierProgress,
} from '@/types/api';
import { apiRequest, unwrapApiEnvelope } from './core';

export const blockchainApi = {
  getQuestionData: (questionId: number) =>
    apiRequest<ApiResponse<BlockchainQuestion>>(`/api/blockchain/question/${questionId}`),

  getStatus: () =>
    apiRequest<ApiResponse<Record<string, unknown>>>('/api/blockchain/status'),
};

export const analyticsApi = {
  getDashboard: async (questionId: number): Promise<ApiResponse<QualityDashboard>> => {
    const raw = await apiRequest<QualityDashboard | { success: boolean; data?: QualityDashboard; message?: string }>(
      `/api/analytics/dashboard/${questionId}`
    );
    const data = unwrapApiEnvelope(raw);
    return { success: true, data };
  },

  getPremiumData: (filters: Record<string, unknown>) =>
    apiRequest<ApiResponse<Record<string, unknown>>>('/api/premium-data/preview', {
      method: 'POST',
      body: JSON.stringify(filters),
    }),

  exportData: (filters: Record<string, unknown>) =>
    apiRequest<Blob>('/api/premium-data/export', {
      method: 'POST',
      body: JSON.stringify(filters),
    }),
};

export const analysisApi = {
  getQualityScore: async (questionId: number) => {
    const raw = await apiRequest<{ qualityScore: number; grade: string }>(`/api/analysis/questions/${questionId}/quality-score`);
    return { success: true as const, data: raw as { qualityScore: number; grade: string } };
  },

  getAbusingReport: async (questionId: number) => {
    const raw = await apiRequest<Record<string, unknown>>(`/api/analysis/questions/${questionId}/abusing-report`);
    return { success: true as const, data: raw };
  },

  getByCountry: async (questionId: number) => {
    const raw = await apiRequest<Record<string, unknown>>(`/api/analysis/questions/${questionId}/by-country`);
    return { success: true as const, data: raw };
  },
};

export const sportsApi = {
  getLiveMatches: () =>
    apiRequest<ApiResponse<Record<string, unknown>[]>>('/api/admin/sports/live'),

  getBettingSuspension: (questionId: number) =>
    apiRequest<ApiResponse<Record<string, unknown>>>(`/api/betting/suspension/question/${questionId}`),
};

export const globalApi = {
  getStats: async (): Promise<ApiResponse<GlobalStats>> => {
    const raw = await apiRequest<
      | GlobalStats
      | {
          success?: boolean;
          data?: Partial<GlobalStats> & {
            totalValueLocked?: number;
            totalRewards?: number;
            totalQuestions?: number;
          };
          totalPredictions?: number;
          tvl?: number;
          cumulativeRewards?: number;
          activeUsers?: number;
          activeMarkets?: number;
          totalValueLocked?: number;
          totalRewards?: number;
          totalQuestions?: number;
        }
    >('/api/analytics/global/stats');

    const normalized = unwrapApiEnvelope(raw as never) as Record<string, unknown>;
    const stats: GlobalStats = {
      totalPredictions: Number(normalized.totalPredictions ?? 0),
      tvl: Number(normalized.tvl ?? normalized.totalValueLocked ?? 0),
      cumulativeRewards: Number(normalized.cumulativeRewards ?? normalized.totalRewards ?? 0),
      activeUsers: Number(normalized.activeUsers ?? 0),
      activeMarkets: Number(normalized.activeMarkets ?? normalized.totalQuestions ?? 0),
    };

    return { success: true, data: stats };
  },
};

export const tierApi = {
  getProgress: (memberId: number) =>
    apiRequest<ApiResponse<TierProgress>>(`/api/tiers/progress/${memberId}`),
};

export const rewardApi = {
  getSummary: (memberId: number) =>
    apiRequest<ApiResponse<RewardSummary>>(`/api/rewards/${memberId}`),
};

export const leaderboardApi = {
  getTop: async (limit: number = 50): Promise<ApiResponse<Record<string, unknown>[]>> => {
    const raw = await apiRequest<Record<string, unknown>[]>(`/api/leaderboard/top?limit=${limit}`);
    return { success: true, data: Array.isArray(raw) ? raw : [] };
  },

  getMemberRank: async (memberId: number): Promise<ApiResponse<Record<string, unknown>>> => {
    const raw = await apiRequest<Record<string, unknown>>(`/api/leaderboard/member/${memberId}`);
    return { success: true, data: raw };
  },
};

export const portfolioApi = {
  getSummary: async (): Promise<ApiResponse<PortfolioSummary>> => {
    const raw = await apiRequest<PortfolioSummary>('/api/portfolio/summary');
    return { success: true, data: raw };
  },

  getPositions: async (): Promise<ApiResponse<OpenPosition[]>> => {
    const raw = await apiRequest<OpenPosition[]>('/api/portfolio/positions');
    return { success: true, data: Array.isArray(raw) ? raw : [] };
  },

  getCategoryBreakdown: async (): Promise<ApiResponse<CategoryPerformance[]>> => {
    const raw = await apiRequest<CategoryPerformance[]>('/api/portfolio/category-breakdown');
    return { success: true, data: Array.isArray(raw) ? raw : [] };
  },

  getAccuracyTrend: async (): Promise<ApiResponse<AccuracyTrendPoint[]>> => {
    const raw = await apiRequest<AccuracyTrendPoint[]>('/api/portfolio/accuracy-trend');
    return { success: true, data: Array.isArray(raw) ? raw : [] };
  },
};

export const referralApi = {
  getStats: async (): Promise<ApiResponse<ReferralStats>> => {
    const raw = await apiRequest<ReferralStats>('/api/referrals/stats');
    return { success: true, data: raw };
  },

  getCode: async (): Promise<ApiResponse<{ code: string }>> => {
    const raw = await apiRequest<{ code: string }>('/api/referrals/code');
    return { success: true, data: raw };
  },

  applyReferral: async (referralCode: string): Promise<ApiResponse<ReferralResult>> => {
    const raw = await apiRequest<ReferralResult>('/api/referrals/apply', {
      method: 'POST',
      body: JSON.stringify({ referralCode }),
    });
    return { success: true, data: raw };
  },
};
