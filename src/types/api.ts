// API Response Wrapper
export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  message?: string;
  errorCode?: string;
}

// Member Types
export interface Member {
  id: number;
  email: string;
  walletAddress?: string;
  countryCode: string;
  jobCategory?: string;
  ageGroup?: string;
  tier: 'BRONZE' | 'SILVER' | 'GOLD' | 'PLATINUM';
  tierWeight: number;
  accuracyScore: number;
  pointBalance: number;
  totalPredictions: number;
  correctPredictions: number;
  role?: 'USER' | 'ADMIN';
  createdAt: string;
}

export interface CreateMemberRequest {
  email: string;
  walletAddress?: string;
  countryCode: string;
  jobCategory?: string;
  ageGroup?: string;
}

// Question Types
export interface Question {
  id: number;
  title: string;
  category?: string;
  status: 'OPEN' | 'CLOSED' | 'PENDING_SETTLEMENT' | 'SETTLED';
  totalBetPool: number;
  yesBetPool: number;
  noBetPool: number;
  finalResult?: 'YES' | 'NO';
  sourceUrl?: string;
  disputeDeadline?: string;
  expiresAt?: string;
  createdAt: string;
}

export interface CreateQuestionRequest {
  title: string;
  category?: string;
  expiresAt?: string;
}

// Activity/Betting Types
export interface Activity {
  id: number;
  memberId: number;
  questionId: number;
  activityType: 'VOTE' | 'BET';
  choice: 'YES' | 'NO';
  amount: number;
  latencyMs?: number;
  createdAt: string;
}

export interface VoteRequest {
  memberId: number;
  questionId: number;
  choice: 'YES' | 'NO';
  latencyMs: number;
}

export interface BetRequest {
  memberId: number;
  questionId: number;
  choice: 'YES' | 'NO';
  amount: number;
}

// Settlement Types
export interface Settlement {
  questionId: number;
  finalResult: string;
  totalBets: number;
  totalWinners: number;
  totalPayout: number;
}

export interface SettlementHistory {
  questionId: number;
  questionTitle: string;
  myChoice: string;
  finalResult: string;
  betAmount: number;
  payout: number;
  profit: number;
  isWinner: boolean;
}

export interface SettleQuestionRequest {
  finalResult: 'YES' | 'NO';
  sourceUrl?: string;
}

// Auth Types
export interface SendCodeResponse {
  success: boolean;
  message: string;
  expiresInSeconds: number;
  code?: string; // demo only
}

export interface VerifyCodeResponse {
  success: boolean;
  verified: boolean;
  isNewUser: boolean;
  memberId?: number;
  message: string;
}

// Faucet Types
export interface FaucetStatus {
  claimed: boolean;
  amount: number;
  resetDate: string;
}

export interface FaucetClaimResponse {
  success: boolean;
  amount: number;
  newBalance: number;
  message: string;
}

// Blockchain Types
export interface BlockchainQuestion {
  questionId: number;
  transactionHash: string;
  blockNumber: number;
  timestamp: string;
}

// Analytics Types
export interface DashboardData {
  totalQuestions: number;
  totalBets: number;
  totalVolume: number;
  activeUsers: number;
}

// User Context
export interface UserContext {
  memberId: number;
  email: string;
  walletAddress?: string;
  tier: string;
  pointBalance: number;
}

// Ticket Types
export interface TicketStatus {
  remainingCount: number;
  resetDate: string;
}

// Global Stats Types
export interface GlobalStats {
  totalPredictions: number;
  tvl: number;
  activeUsers: number;
  cumulativeRewards: number;
  activeMarkets: number;
}

// Tier Progress Types
export interface TierProgress {
  currentTier: 'BRONZE' | 'SILVER' | 'GOLD' | 'PLATINUM';
  nextTier: 'SILVER' | 'GOLD' | 'PLATINUM' | null;
  progressPercentage: number;
  totalPredictions: number;
  correctPredictions: number;
  accuracyScore: number;
  requirementsForNext: {
    predictions: number;
    accuracy: number;
  } | null;
}

// Reward Types
export interface RewardSummary {
  memberId: number;
  totalRewards: number;
  totalVotes: number;
  totalBets: number;
  winCount: number;
  lossCount: number;
  winRate: number;
}

// Category type
export type QuestionCategory = 'ALL' | 'ECONOMY' | 'SPORTS' | 'POLITICS' | 'TECH' | 'CULTURE';

// Data Center / Quality Dashboard Types
export interface QualityDashboard {
  questionId: number;
  demographics: VoteDemographicsReport;
  gapAnalysis: VoteBetGapReport;
  filteringEffect: FilteringEffectReport;
  overallQualityScore: number;
}

export interface VoteDemographicsReport {
  questionId: number;
  totalVotes: number;
  byCountry: CountryVoteData[];
  byJob: JobVoteData[];
  byAge: AgeVoteData[];
}

export interface CountryVoteData {
  countryCode: string;
  yesCount: number;
  noCount: number;
  yesPercentage: number;
  total: number;
}

export interface JobVoteData {
  jobCategory: string;
  yesCount: number;
  noCount: number;
  yesPercentage: number;
  total: number;
}

export interface AgeVoteData {
  ageGroup: number;
  yesCount: number;
  noCount: number;
  yesPercentage: number;
  total: number;
}

export interface VoteBetGapReport {
  questionId: number;
  voteDistribution: DistributionData;
  betDistribution: DistributionData;
  gapPercentage: number;
  qualityScore: number;
}

export interface DistributionData {
  yesPercentage: number;
  noPercentage: number;
  yesCount: number;
  noCount: number;
}

export interface FilteringEffectReport {
  questionId: number;
  beforeFiltering: FilteringData;
  afterFiltering: FilteringData;
  filteredCount: number;
  filteredPercentage: number;
}

export interface FilteringData {
  totalCount: number;
  yesPercentage: number;
  noPercentage: number;
}
