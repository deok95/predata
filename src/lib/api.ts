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
  Notification,
  PortfolioSummary,
  OpenPosition,
  CategoryPerformance,
  AccuracyTrendPoint,
  ReferralStats,
  ReferralResult,
  TicketPurchaseResponse,
  TicketPurchaseHistory,
} from '@/types/api';

const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

// Export for reuse across components
export const BACKEND_URL = API_URL;
export const API_BASE_URL = `${API_URL}/api`;

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

  // localStorage에서 JWT 토큰 가져오기
  const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null;

  try {
    const response = await fetch(`${API_URL}${endpoint}`, {
      headers: {
        'Content-Type': 'application/json',
        ...(token && { Authorization: `Bearer ${token}` }),
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

  // Google OAuth 로그인
  googleLogin: (googleToken: string, additionalInfo?: {
    countryCode?: string;
    jobCategory?: string;
    ageGroup?: number;
  }) =>
    apiRequest<{
      success: boolean;
      message: string;
      token?: string;
      memberId?: number;
      needsAdditionalInfo?: boolean;
    }>('/api/auth/google', {
      method: 'POST',
      body: JSON.stringify({
        googleToken,
        countryCode: additionalInfo?.countryCode,
        jobCategory: additionalInfo?.jobCategory,
        ageGroup: additionalInfo?.ageGroup,
      }),
    }),

  // Google OAuth 회원가입 완료 (추가 정보 입력)
  completeGoogleRegistration: (data: {
    googleId: string;
    email: string;
    countryCode: string;
    jobCategory?: string;
    ageGroup?: number;
  }) =>
    apiRequest<{
      success: boolean;
      message: string;
      token?: string;
      memberId?: number;
    }>('/api/auth/google/complete-registration', {
      method: 'POST',
      body: JSON.stringify(data),
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

// ===== OrderBook API =====
export interface OrderBookLevel {
  price: number;
  amount: number;
  count: number;
}

export interface OrderBookData {
  questionId: number;
  bids: OrderBookLevel[];
  asks: OrderBookLevel[];
  lastPrice?: number;
  spread?: number;
}

export interface CreateOrderRequest {
  memberId: number;
  questionId: number;
  side: 'YES' | 'NO';
  price: number;  // 0.01 ~ 0.99
  amount: number;
}

export interface CreateOrderResponse {
  success: boolean;
  message?: string;
  orderId?: number;
  filledAmount: number;
  remainingAmount: number;
}

export interface OrderData {
  orderId: number;
  memberId: number;
  questionId: number;
  side: 'YES' | 'NO';
  price: number;
  amount: number;
  remainingAmount: number;
  status: 'OPEN' | 'FILLED' | 'PARTIAL' | 'CANCELLED';
  createdAt: string;
  updatedAt: string;
}

export const orderApi = {
  // 오더북 조회
  getOrderBook: (questionId: number) =>
    apiRequest<OrderBookData>(`/api/questions/${questionId}/orderbook`),

  // Limit Order 생성
  createOrder: (data: CreateOrderRequest) =>
    apiRequest<CreateOrderResponse>('/api/orders', {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  // 주문 취소
  cancelOrder: (orderId: number, memberId: number) =>
    apiRequest<{ success: boolean; message?: string; refundedAmount?: number }>(
      `/api/orders/${orderId}?memberId=${memberId}`,
      { method: 'DELETE' }
    ),

  // 회원의 활성 주문 조회
  getActiveOrders: (memberId: number) =>
    apiRequest<OrderData[]>(`/api/orders/member/${memberId}`),

  // 회원의 특정 질문에 대한 주문 조회
  getOrdersByQuestion: (memberId: number, questionId: number) =>
    apiRequest<OrderData[]>(`/api/orders/member/${memberId}/question/${questionId}`),
};

// ===== Notification API =====
export const notificationApi = {
  getAll: async (memberId: number): Promise<ApiResponse<Notification[]>> => {
    const raw = await apiRequest<any[]>(`/api/notifications?memberId=${memberId}`);
    return { success: true, data: Array.isArray(raw) ? raw : [] };
  },

  markAsRead: async (id: number): Promise<ApiResponse<void>> => {
    const raw = await apiRequest<any>(`/api/notifications/${id}/read`, {
      method: 'POST',
    });
    return { success: raw?.success ?? true };
  },

  markAllAsRead: async (memberId: number): Promise<ApiResponse<void>> => {
    const raw = await apiRequest<any>('/api/notifications/read-all', {
      method: 'POST',
      body: JSON.stringify({ memberId }),
    });
    return { success: raw?.success ?? true };
  },
};

// ===== Portfolio API =====
export const portfolioApi = {
  getSummary: async (): Promise<ApiResponse<PortfolioSummary>> => {
    const raw = await apiRequest<any>('/api/portfolio/summary');
    return { success: true, data: raw };
  },

  getPositions: async (): Promise<ApiResponse<OpenPosition[]>> => {
    const raw = await apiRequest<any[]>('/api/portfolio/positions');
    return { success: true, data: Array.isArray(raw) ? raw : [] };
  },

  getCategoryBreakdown: async (): Promise<ApiResponse<CategoryPerformance[]>> => {
    const raw = await apiRequest<any[]>('/api/portfolio/category-breakdown');
    return { success: true, data: Array.isArray(raw) ? raw : [] };
  },

  getAccuracyTrend: async (): Promise<ApiResponse<AccuracyTrendPoint[]>> => {
    const raw = await apiRequest<any[]>('/api/portfolio/accuracy-trend');
    return { success: true, data: Array.isArray(raw) ? raw : [] };
  },
};

// ===== Referral API =====
export const referralApi = {
  getStats: async (): Promise<ApiResponse<ReferralStats>> => {
    const raw = await apiRequest<any>('/api/referrals/stats');
    return { success: true, data: raw };
  },

  getCode: async (): Promise<ApiResponse<{ code: string }>> => {
    const raw = await apiRequest<any>('/api/referrals/code');
    return { success: true, data: raw };
  },

  applyReferral: async (referralCode: string): Promise<ApiResponse<ReferralResult>> => {
    const raw = await apiRequest<any>('/api/referrals/apply', {
      method: 'POST',
      body: JSON.stringify({ referralCode }),
    });
    return { success: true, data: raw };
  },
};

// ===== Ticket API =====
export const ticketApi = {
  purchase: async (quantity: number): Promise<ApiResponse<TicketPurchaseResponse>> => {
    const raw = await apiRequest<any>('/api/tickets/purchase', {
      method: 'POST',
      body: JSON.stringify({ quantity }),
    });
    return { success: true, data: raw };
  },

  getHistory: async (memberId: number): Promise<ApiResponse<TicketPurchaseHistory[]>> => {
    const raw = await apiRequest<any[]>(`/api/tickets/history/${memberId}`);
    return { success: true, data: Array.isArray(raw) ? raw : [] };
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
  order: orderApi,
  notification: notificationApi,
  portfolio: portfolioApi,
  referral: referralApi,
  ticket: ticketApi,
};

export default api;
