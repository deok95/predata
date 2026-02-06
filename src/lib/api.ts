import type {
  ApiResponse,
  Member,
  CreateMemberRequest,
  Question,
  CreateQuestionRequest,
  Activity,
  VoteRequest,
  BetRequest,
  SettlementHistory,
  SettleQuestionRequest,
  BlockchainQuestion,
  DashboardData,
  TicketStatus,
  GlobalStats,
  TierProgress,
  RewardSummary,
  FaucetStatus,
  FaucetClaimResponse,
  SendCodeResponse,
  VerifyCodeResponse,
} from '@/types/api';

const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

// API Error Class
export class ApiError extends Error {
  constructor(
    public status: number,
    public message: string,
    public data?: any
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

// Generic API Request Helper — returns raw parsed JSON
async function apiRequest<T>(
  endpoint: string,
  options?: RequestInit
): Promise<T> {
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), 15000);

  try {
    const response = await fetch(`${API_URL}${endpoint}`, {
      headers: {
        'Content-Type': 'application/json',
        ...options?.headers,
      },
      ...options,
      signal: controller.signal,
    });

    let data;
    try {
      data = await response.json();
    } catch {
      throw new ApiError(response.status, '서버 응답을 처리할 수 없습니다.');
    }

    if (!response.ok) {
      throw new ApiError(
        response.status,
        data.message || `HTTP ${response.status}: ${response.statusText}`,
        data
      );
    }

    return data;
  } catch (error) {
    if (error instanceof ApiError) {
      throw error;
    }

    if (error instanceof DOMException && error.name === 'AbortError') {
      throw new ApiError(408, '서버 응답 시간이 초과되었습니다.');
    }

    throw new ApiError(
      500,
      error instanceof Error ? error.message : '네트워크 오류가 발생했습니다.',
      error
    );
  } finally {
    clearTimeout(timeoutId);
  }
}

// ===== Field Mappers =====
// Backend field names differ from frontend types

function mapQuestion(raw: any): Question {
  return {
    id: raw.id,
    title: raw.title,
    category: raw.category ?? undefined,
    status: raw.status,
    type: raw.type ?? 'VERIFIABLE',
    totalBetPool: raw.totalBetPool ?? 0,
    yesBetPool: raw.yesBetPool ?? 0,
    noBetPool: raw.noBetPool ?? 0,
    yesPercentage: raw.yesPercentage ?? 50,
    noPercentage: raw.noPercentage ?? 50,
    finalResult: raw.finalResult ?? undefined,
    sourceUrl: raw.sourceUrl ?? undefined,
    disputeDeadline: raw.disputeDeadline ?? undefined,
    votingEndAt: raw.votingEndAt,
    bettingStartAt: raw.bettingStartAt,
    bettingEndAt: raw.bettingEndAt,
    expiredAt: raw.expiredAt || raw.expiresAt,
    createdAt: raw.createdAt,
  };
}

function mapMember(raw: any): Member {
  return {
    id: raw.memberId ?? raw.id,
    email: raw.email,
    walletAddress: raw.walletAddress ?? undefined,
    countryCode: raw.countryCode,
    jobCategory: raw.jobCategory ?? undefined,
    ageGroup: raw.ageGroup != null ? String(raw.ageGroup) : undefined,
    tier: raw.tier ?? 'BRONZE',
    tierWeight: raw.tierWeight ?? 1.0,
    accuracyScore: raw.accuracyScore ?? 0,
    pointBalance: raw.pointBalance ?? 0,
    totalPredictions: raw.totalPredictions ?? 0,
    correctPredictions: raw.correctPredictions ?? 0,
    role: raw.role ?? 'USER',
    createdAt: raw.createdAt ?? new Date().toISOString(),
  };
}

// ===== Member API =====
export const memberApi = {
  getByEmail: async (email: string): Promise<ApiResponse<Member>> => {
    const raw = await apiRequest<any>(`/api/members/by-email?email=${encodeURIComponent(email)}`);
    return { success: true, data: mapMember(raw) };
  },

  getByWallet: async (address: string): Promise<ApiResponse<Member>> => {
    const raw = await apiRequest<any>(`/api/members/by-wallet?address=${address}`);
    return { success: true, data: mapMember(raw) };
  },

  getById: async (id: number): Promise<ApiResponse<Member>> => {
    const raw = await apiRequest<any>(`/api/members/${id}`);
    return { success: true, data: mapMember(raw) };
  },

  create: async (data: CreateMemberRequest): Promise<ApiResponse<Member>> => {
    const raw = await apiRequest<any>('/api/members', {
      method: 'POST',
      body: JSON.stringify(data),
    });
    return { success: true, data: mapMember(raw) };
  },

  getTickets: async (memberId: number): Promise<ApiResponse<TicketStatus>> => {
    const raw = await apiRequest<any>(`/api/tickets/${memberId}`);
    return { success: true, data: raw };
  },
};

// ===== Question API =====
export const questionApi = {
  getAll: async (): Promise<ApiResponse<Question[]>> => {
    const raw = await apiRequest<any[]>('/api/questions');
    const questions = Array.isArray(raw) ? raw.map(mapQuestion) : [];
    return { success: true, data: questions };
  },

  getById: async (id: number): Promise<ApiResponse<Question>> => {
    const raw = await apiRequest<any>(`/api/questions/${id}`);
    return { success: true, data: mapQuestion(raw) };
  },

  getOdds: (id: number) =>
    apiRequest<{ yesOdds: number; noOdds: number; poolYes: number; poolNo: number; totalPool: number }>(`/api/questions/${id}/odds`),

  create: async (data: CreateQuestionRequest): Promise<ApiResponse<any>> => {
    const raw = await apiRequest<any>('/api/admin/questions', {
      method: 'POST',
      body: JSON.stringify(data),
    });
    return { success: true, data: raw };
  },

  delete: async (id: number): Promise<ApiResponse<void>> => {
    const raw = await apiRequest<any>(`/api/admin/questions/${id}`, {
      method: 'DELETE',
    });
    return { success: raw?.success ?? true };
  },

  getAdminList: async (): Promise<ApiResponse<Question[]>> => {
    const raw = await apiRequest<any[]>('/api/admin/questions');
    return { success: true, data: Array.isArray(raw) ? raw.map(mapQuestion) : [] };
  },
};

// ===== Betting API =====
// Backend endpoints: POST /api/vote, POST /api/bet
// Backend returns ActivityResponse: { success, message, activityId, remainingTickets }
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
    const raw = await apiRequest<any[]>(
      `/api/activities/question/${questionId}${type ? `?type=${type}` : ''}`
    );
    return { success: true, data: Array.isArray(raw) ? raw : [] };
  },

  getActivitiesByMember: async (memberId: number, type?: 'VOTE' | 'BET'): Promise<ApiResponse<Activity[]>> => {
    const raw = await apiRequest<any[]>(
      `/api/activities/member/${memberId}${type ? `?type=${type}` : ''}`
    );
    return { success: true, data: Array.isArray(raw) ? raw : [] };
  },
};

// ===== Settlement API =====
export const settlementApi = {
  settle: (questionId: number, data: SettleQuestionRequest) =>
    apiRequest<any>(`/api/questions/${questionId}/settle`, {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  finalize: (questionId: number, force: boolean = false) =>
    apiRequest<any>(`/api/questions/${questionId}/settle/finalize`, {
      method: 'POST',
      body: JSON.stringify({ force }),
    }),

  cancel: (questionId: number) =>
    apiRequest<any>(`/api/questions/${questionId}/settle/cancel`, {
      method: 'POST',
    }),

  getHistory: async (memberId: number): Promise<ApiResponse<SettlementHistory[]>> => {
    const raw = await apiRequest<any[]>(`/api/settlements/history/${memberId}`);
    return { success: true, data: Array.isArray(raw) ? raw : [] };
  },
};

// ===== Faucet API =====
export const faucetApi = {
  claim: (memberId: number) =>
    apiRequest<FaucetClaimResponse>(`/api/faucet/claim/${memberId}`, {
      method: 'POST',
    }),

  getStatus: (memberId: number) =>
    apiRequest<FaucetStatus>(`/api/faucet/status/${memberId}`),
};

// ===== Auth API =====
export const authApi = {
  // Step 1: 이메일로 인증 코드 발송
  sendCode: (email: string) =>
    apiRequest<{ success: boolean; message: string; expiresInSeconds: number }>('/api/auth/send-code', {
      method: 'POST',
      body: JSON.stringify({ email }),
    }),

  // Step 2: 인증 코드 검증
  verifyCode: (email: string, code: string) =>
    apiRequest<{ success: boolean; message: string }>('/api/auth/verify-code', {
      method: 'POST',
      body: JSON.stringify({ email, code }),
    }),

  // Step 3: 비밀번호 설정 및 회원 생성
  completeSignup: (email: string, code: string, password: string, passwordConfirm: string) =>
    apiRequest<{ success: boolean; message: string; token?: string; memberId?: number }>('/api/auth/complete-signup', {
      method: 'POST',
      body: JSON.stringify({ email, code, password, passwordConfirm }),
    }),

  // 로그인
  login: (email: string, password: string) =>
    apiRequest<{ success: boolean; message: string; token?: string; memberId?: number }>('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    }),
};

// ===== Blockchain API =====
export const blockchainApi = {
  getQuestionData: (questionId: number) =>
    apiRequest<ApiResponse<BlockchainQuestion>>(`/api/blockchain/question/${questionId}`),

  getStatus: () =>
    apiRequest<ApiResponse<any>>('/api/blockchain/status'),
};

// ===== Analytics API =====
export const analyticsApi = {
  getDashboard: (questionId: number) =>
    apiRequest<ApiResponse<DashboardData>>(`/api/analytics/dashboard/${questionId}`),

  getPremiumData: (filters: any) =>
    apiRequest<ApiResponse<any>>('/api/premium-data/preview', {
      method: 'POST',
      body: JSON.stringify(filters),
    }),

  exportData: (filters: any) =>
    apiRequest<Blob>('/api/premium-data/export', {
      method: 'POST',
      body: JSON.stringify(filters),
    }),
};

// ===== Sports API =====
export const sportsApi = {
  getLiveMatches: () =>
    apiRequest<ApiResponse<any[]>>('/api/admin/sports/live'),

  getBettingSuspension: (questionId: number) =>
    apiRequest<ApiResponse<any>>(`/api/betting/suspension/question/${questionId}`),
};

// ===== Global Stats API =====
export const globalApi = {
  getStats: () =>
    apiRequest<ApiResponse<GlobalStats>>('/api/analytics/global/stats'),
};

// ===== Tier API =====
export const tierApi = {
  getProgress: (memberId: number) =>
    apiRequest<ApiResponse<TierProgress>>(`/api/tiers/progress/${memberId}`),
};

// ===== Reward API =====
export const rewardApi = {
  getSummary: (memberId: number) =>
    apiRequest<ApiResponse<RewardSummary>>(`/api/rewards/${memberId}`),
};

// ===== Leaderboard API =====
export const leaderboardApi = {
  getTop: async (limit: number = 50): Promise<ApiResponse<any[]>> => {
    const raw = await apiRequest<any[]>(`/api/leaderboard/top?limit=${limit}`);
    return { success: true, data: Array.isArray(raw) ? raw : [] };
  },

  getMemberRank: async (memberId: number): Promise<ApiResponse<any>> => {
    const raw = await apiRequest<any>(`/api/leaderboard/member/${memberId}`);
    return { success: true, data: raw };
  },
};

// Export all APIs
export const api = {
  auth: authApi,
  member: memberApi,
  question: questionApi,
  betting: bettingApi,
  settlement: settlementApi,
  faucet: faucetApi,
  blockchain: blockchainApi,
  analytics: analyticsApi,
  sports: sportsApi,
  global: globalApi,
  tier: tierApi,
  reward: rewardApi,
  leaderboard: leaderboardApi,
};

export default api;
