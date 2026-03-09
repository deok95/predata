export const PREdataEndpoints = {
  auth: {
    login: '/api/auth/login',
    sendCode: '/api/auth/send-code',
    verifyCode: '/api/auth/verify-code',
    completeSignup: '/api/auth/complete-signup',
  },
  question: {
    list: '/api/questions',
    detail: (id: number | string) => `/api/questions/${id}`,
    myCreated: '/api/questions/me/created',
    byStatus: (status: string) => `/api/questions/status/${status}`,
    draftOpen: '/api/questions/drafts/open',
    draftSubmit: (draftId: string) => `/api/questions/drafts/${draftId}/submit`,
    draftCancel: (draftId: string) => `/api/questions/drafts/${draftId}/cancel`,
  },
  voting: {
    vote: '/api/votes',
    status: (questionId: number | string) => `/api/votes/status/${questionId}`,
    feed: '/api/votes/feed',
  },
  market: {
    swap: '/api/swap',
    pool: (questionId: number | string) => `/api/pool/${questionId}`,
    swapSimulate: '/api/swap/simulate',
  },
  portfolio: {
    summary: '/api/portfolio/summary',
    positions: '/api/portfolio/positions',
    categoryBreakdown: '/api/portfolio/category-breakdown',
    accuracyTrend: '/api/portfolio/accuracy-trend',
  },
  settlement: {
    myHistory: '/api/settlements/history/me',
  },
  activity: {
    my: '/api/activities/me',
    byQuestion: (questionId: number | string) => `/api/activities/question/${questionId}`,
    myByQuestion: (questionId: number | string) => `/api/activities/me/question/${questionId}`,
  },
  member: {
    meDashboard: '/api/members/me/dashboard',
  },
  admin: {
    marketBatches: '/api/admin/markets/batches',
    marketBatchCandidates: (batchId: number | string) => `/api/admin/markets/batches/${batchId}/candidates`,
    marketBatchSummary: (batchId: number | string) => `/api/admin/markets/batches/${batchId}/summary`,
    walletLedgers: '/api/admin/finance/wallet-ledgers',
    treasuryLedgers: '/api/admin/finance/treasury-ledgers',
    voteOpsUsage: '/api/admin/vote-ops/usage',
    voteOpsRelay: '/api/admin/vote-ops/relay',
  },
} as const;
