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
  usdcBalance: number;
  hasVotingPass: boolean;
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
  status: 'VOTING' | 'BREAK' | 'BETTING' | 'SETTLED';
  votingPhase?: 'VOTING_COMMIT_OPEN' | 'VOTING_REVEAL_OPEN' | 'BETTING_OPEN' | 'SETTLED';
  type: 'VERIFIABLE' | 'OPINION';
  totalBetPool: number;
  yesBetPool: number;
  noBetPool: number;
  yesPercentage: number;
  noPercentage: number;
  finalResult?: 'YES' | 'NO' | 'PENDING';
  sourceUrl?: string;
  disputeDeadline?: string;
  votingEndAt: string;
  bettingStartAt: string;
  bettingEndAt: string;
  expiredAt: string;
  createdAt: string;
  viewCount?: number;
  matchId?: number;
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
  activityType: 'VOTE' | 'BET' | 'BET_SELL';
  choice: 'YES' | 'NO';
  amount: number;
  latencyMs?: number;
  createdAt: string;
  parentBetId?: number;
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

export interface SellBetRequest {
  memberId: number;
  betId: number;
}

export interface SellBetResponse {
  success: boolean;
  message?: string;
  originalBetAmount?: number;
  refundAmount?: number;
  profit?: number;
  newPoolYes?: number;
  newPoolNo?: number;
  newPoolTotal?: number;
  sellActivityId?: number;
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
  usdcBalance: number;
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

// Data Center Time-Series Types
export interface VoteTrendPoint {
  timestamp: string;
  cumulativeYes: number;
  cumulativeNo: number;
  yesPercentage: number;
  hourlyVotes: number;
  hourlyBetVolume: number;
}

export interface QualityScoreResult {
  qualityScore: number;
  grade: string;
}

// ===== Notification Types =====
export interface Notification {
  id: number;
  type: string;                    // 'BET_PLACED', 'VOTE_RECORDED', 'SETTLEMENT_COMPLETE', 'FAUCET_CLAIMED', 'TIER_CHANGE'
  title: string;
  message: string;
  relatedQuestionId?: number | null;
  isRead: boolean;
  createdAt: string;               // ISO datetime string
}

// ===== Portfolio Types =====
export interface PortfolioSummary {
  memberId: number;
  totalInvested: number;
  totalReturns: number;
  netProfit: number;
  unrealizedValue: number;
  currentBalance: number;
  winRate: number;                 // 0-100
  totalBets: number;
  openBets: number;
  settledBets: number;
  roi: number;                     // Return on Investment percentage
}

export interface OpenPosition {
  activityId: number;
  questionId: number;
  questionTitle: string;
  category?: string;
  choice: string;                  // 'YES' or 'NO'
  betAmount: number;
  currentYesPercentage: number;    // 0-100
  currentNoPercentage: number;     // 0-100
  estimatedPayout: number;
  estimatedProfitLoss: number;     // estimatedPayout - betAmount
  expiresAt: string;               // ISO datetime
  placedAt: string;                // ISO datetime
}

export interface CategoryPerformance {
  category: string;                // ECONOMY, SPORTS, POLITICS, TECH, CULTURE, OTHER
  totalBets: number;
  wins: number;
  losses: number;
  pending: number;
  invested: number;
  returned: number;
  profit: number;                  // returned - invested
  winRate: number;                 // 0-100
}

export interface AccuracyTrendPoint {
  date: string;                    // "yyyy-MM" format
  totalPredictions: number;
  correctPredictions: number;
  accuracy: number;                // 0-100
  cumulativeAccuracy: number;      // 0-100
}

// ===== Referral Types =====
export interface ReferralStats {
  referralCode: string;
  totalReferrals: number;
  totalPointsEarned: number;
  referees: RefereeInfo[];
}

export interface RefereeInfo {
  email: string;                   // Masked email (e.g., "ha***@example.com")
  joinedAt: string;                // ISO datetime string
  rewardAmount: number;            // Points earned from this referral
}

export interface ReferralResult {
  success: boolean;
  message: string;
  referrerReward?: number;         // Points given to referrer (500)
  refereeReward?: number;          // Points given to referee (500)
}

// ===== Voting Pass Types =====
export interface VotingPassPurchaseResponse {
  success: boolean;
  hasVotingPass: boolean;
  remainingBalance: number;
  message: string;
}

// ===== Payment Types =====
export interface PaymentVerifyDepositRequest {
  txHash: string;
  amount: number;
}

export interface PaymentVerifyResponse {
  success: boolean;
  txHash: string;
  amount: number;
  newBalance?: number;
  message?: string;
}

export interface WithdrawRequest {
  memberId: number;
  amount: number;
  walletAddress: string;
}

export interface WithdrawResponse {
  success: boolean;
  txHash?: string;
  amount?: number;
  newBalance?: number;
  message: string;
}

// ===== Transaction History Types =====
export type TransactionType = 'DEPOSIT' | 'WITHDRAW' | 'BET' | 'SETTLEMENT' | 'VOTING_PASS';

export interface TransactionHistoryItem {
  id: number;
  type: TransactionType;
  amount: number;
  balanceAfter: number;
  description: string;
  questionId?: number | null;
  txHash?: string | null;
  createdAt: string;
}

export interface TransactionHistoryPage {
  content: TransactionHistoryItem[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

// ===== Commit-Reveal Vote Types =====
export interface VoteCommitRequest {
  questionId: number;
  commitHash: string;
}

export interface VoteCommitResponse {
  success: boolean;
  message: string;
  voteCommitId?: number;
}

export interface VoteRevealRequest {
  questionId: number;
  choice: 'YES' | 'NO';
  salt: string;
}

export interface VoteRevealResponse {
  success: boolean;
  message: string;
}
