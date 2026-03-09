export type VoteChoice = "YES" | "NO";

export type SettlementMode = "OBJECTIVE_RULE" | "VOTE_RESULT";

export interface MarketItem {
  id: number | string;
  title: string;
  category: string;
  volume: string;
  voters: number;
  bettors: number;
  yesPrice: number;
  change24h: number;
  timeLeft: string;
  bettingEndAt?: string;
  topDemographic?: string;
  sentiment?: string;
  comments: number;
  creator: string;
  description?: string;
  status?: string;
  phase?: string;
  submitter?: string;
  avatar?: string;
  tags: string[];
  creatorSplitInPool?: number;
  platformFeeShare?: number;
  creatorFeeShare?: number;
  voterFeeShare?: number;
  matchId?: number | string;
  matchTime?: string;
  sport?: string;
  league?: string;
  homeTeam?: string;
  awayTeam?: string;
  voteWindowType?: string;
  settlementMode?: SettlementMode;
  votingPhase?: string | null;
  voteVisibility?: string | null;
  hiddenVoteResults?: boolean;
  totalVotes?: number;
}

export interface VoteQuestion {
  id: number | string;
  title: string;
  category: string;
  sub?: string;
  sub2?: string;
  submitterId?: number | string;
  yesVotes: number;
  noVotes: number;
  totalVotes: number;
  submitter: string;
  avatar: string;
  following?: boolean;
  submitted: string;
  votingEndAt?: string;
  age: number;
  tags: string[];
  likes: number;
  commentCount: number;
  description?: string;
  yesPrice: number;
  settlementMode?: SettlementMode;
  votingPhase?: string | null;
  voteVisibility?: string | null;
  hiddenVoteResults?: boolean;
}

export interface PositionItem {
  id: number | string;
  title: string;
  side: VoteChoice;
  shares: number;
  avgPrice: number;
  currentPrice: number;
  invested: number;
}

export interface UserProfile {
  name: string;
  username: string;
  email: string;
  avatar: string;
  bio: string;
  balance: number;
  totalProfit: number;
  totalBets: number;
  winRate: number;
  voteCredits: number;
  memberSince: string;
  followers: number;
  following: number;
  questionsCreated: number;
  totalVotes: number;
  creatorEarnings: number;
}

export interface ActivityItem {
  id: number | string;
  type: string;
  text: string;
  market: string;
  detail?: string;
  time: string;
  color?: string;
}

export interface SettlementHistoryItem {
  id: number | string;
  title: string;
  side: VoteChoice;
  result: "WON" | "LOST" | string;
  shares: number;
  priceBought: number;
  priceSold: number;
  date: string;
}

export interface CreatedQuestionItem {
  id: number | string;
  title: string;
  category: string;
  status: string;
  totalVotes: number;
  earnings: number;
}

export interface VoteActivityItem {
  id: number | string;
  category: string;
  date: string;
  title: string;
  vote: VoteChoice;
  totalVotes: number;
  status: string;
}

export interface CommentItem {
  id: number | string;
  user: string;
  avatar: string;
  text: string;
  time: string;
  vote?: VoteChoice | null;
}

export interface LeaderboardEntryItem {
  rank: number;
  memberId: number;
  email: string;
  tier: string;
  accuracyScore: number;
  accuracyPercentage: number;
  totalPredictions: number;
  correctPredictions: number;
  usdcBalance: number;
}
