export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE';

export interface ApiErrorDetail {
  code: string;
  message: string;
  status: number;
  details?: string[];
  timestamp?: string;
}

export interface ApiEnvelope<T> {
  success: boolean;
  data: T | null;
  error?: ApiErrorDetail | null;
  message?: string | null;
  timestamp?: string;
}

export interface VoteStatusResponse {
  questionId: number;
  canVote: boolean;
  alreadyVoted: boolean;
  remainingDailyVotes: number;
  reason?: string | null;
}

export interface PortfolioSummary {
  memberId: number;
  totalInvested: number;
  totalReturns: number;
  netProfit: number;
  unrealizedValue: number;
  currentBalance: number;
  winRate: number;
  totalBets: number;
  openBets: number;
  settledBets: number;
  roi: number;
}

export interface OpenPositionItem {
  activityId: number;
  questionId: number;
  questionTitle: string;
  category: string;
  choice: 'YES' | 'NO';
  betAmount: number;
  currentYesPercentage: number;
  currentNoPercentage: number;
  estimatedPayout: number;
  estimatedProfitLoss: number;
  expiresAt: string;
  placedAt: string;
}

export interface SettlementHistoryItem {
  questionId: number;
  questionTitle: string;
  myChoice: 'YES' | 'NO';
  finalResult: 'YES' | 'NO' | 'PENDING';
  betAmount: number;
  payout: number;
  profit: number;
  isWinner: boolean;
}

export interface DraftOpenResponse {
  draftId: string;
  expiresAt: string;
}

export interface DraftSubmitRequest {
  title: string;
  category: string;
  voteWindowType: 'H6' | 'D1' | 'D3';
  resolutionRule: string;
  resolutionSource: string;
  creatorSplitInPool: number;
}

export interface VoteRequest {
  questionId: number;
  choice: 'YES' | 'NO';
  latencyMs?: number;
}

export interface SwapRequest {
  questionId: number;
  action: 'BUY' | 'SELL';
  outcome: 'YES' | 'NO';
  usdcIn?: string;
  sharesIn?: string;
  minSharesOut?: string;
  minUsdcOut?: string;
}
