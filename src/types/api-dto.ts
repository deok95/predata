export interface ApiEnvelope<T> {
  success: boolean;
  data: T;
  error?: { message?: string } | null;
  timestamp?: string;
}

export interface AuthResponseDto {
  token?: string;
  user?: Record<string, unknown>;
  memberId?: string | number;
  needsAdditionalInfo?: boolean;
  googleId?: string;
  email?: string;
}

export type QuestionStatusDto =
  | "VOTING"
  | "BREAK"
  | "BETTING"
  | "SETTLED"
  | "CANCELLED";

export type VoteVisibilityDto = "HIDDEN" | "REVEALED";

export type VotingPhaseDto =
  | "COMMIT_OPEN"
  | "REVEAL_OPEN"
  | "VOTING_REVEAL_CLOSED";

export type QuestionSettlementModeDto = "OBJECTIVE_RULE" | "VOTE_RESULT";

export interface BaseQuestionDto {
  id?: number | string;
  questionId?: number | string;
  title?: string;
  description?: string;
  category?: string;
  status?: QuestionStatusDto | string;
  phase?: string;
  matchTime?: string;
  creatorNickname?: string;
  creatorUsername?: string;
  creatorAvatar?: string;
  subcategory?: string;
  sub?: string;
  tags?: string[];
  tagsJson?: string;
  sourceLinksJson?: string;
  matchId?: number | string;
  yesPrice?: number;
  totalVolume?: number;
  totalVoteCount?: number;
  totalVotes?: number;
  yesVoteCount?: number;
  noVoteCount?: number;
  yesVotes?: number;
  noVotes?: number;
  commentCount?: number;
  createdAtRelative?: string;
  submitted?: string;
  ageHours?: number;
  age?: number;
  voteWindowType?: string;
  bettingEndAt?: string;
  creatorSplitInPool?: number;
  platformFeeShare?: string | number;
  creatorFeeShare?: string | number;
  voterFeeShare?: string | number;
  timeRemaining?: string;
  timeLeft?: string;
  topDemographic?: string;
  sentiment?: string;
  bettorCount?: number;
  totalBettors?: number;
  priceChange24h?: number;
  likeCount?: number;
  likes?: number;
  submitter?: string;
  submitterId?: number | string;
  submitterUsername?: string;
  submitterDisplayName?: string;
  isFollowingSubmitter?: boolean;
  submittedAt?: string;
  lastVoteAt?: string;
  votingEndAt?: string;
  avatar?: string;
  isFollowing?: boolean;
  following?: boolean;
}

export interface VoteResultQuestionDto extends BaseQuestionDto {
  settlementMode: "VOTE_RESULT";
  voteResultSettlement?: true;
  votingPhase?: VotingPhaseDto | string;
  voteVisibility?: VoteVisibilityDto | string;
}

export interface ObjectiveRuleQuestionDto extends BaseQuestionDto {
  settlementMode?: "OBJECTIVE_RULE";
  voteResultSettlement?: false;
  votingPhase?: never;
  voteVisibility?: never;
}

export interface LegacyQuestionDto extends BaseQuestionDto {
  settlementMode?: "OBJECTIVE" | "SUBJECTIVE";
  voteResultSettlement?: boolean;
  votingPhase?: VotingPhaseDto | string;
  voteVisibility?: VoteVisibilityDto | string;
}

export interface UnknownQuestionDto extends BaseQuestionDto {
  settlementMode?: string;
  voteResultSettlement?: boolean;
  votingPhase?: string;
  voteVisibility?: string;
}

export type QuestionDto =
  | VoteResultQuestionDto
  | ObjectiveRuleQuestionDto
  | LegacyQuestionDto
  | UnknownQuestionDto;

export interface PositionDto {
  id?: number | string;
  questionId?: number | string;
  title?: string;
  questionTitle?: string;
  side?: "YES" | "NO" | string;
  outcome?: "YES" | "NO" | string;
  shareCount?: number;
  shares?: number;
  averageEntryPrice?: number;
  averagePrice?: number;
  avgPrice?: number;
  currentPrice?: number;
  investedAmount?: number;
  totalInvested?: number;
  invested?: number;
}

export interface MemberDto {
  nickname?: string;
  displayName?: string;
  username?: string;
  email?: string;
  avatarUrl?: string;
  bio?: string;
  balance?: number;
  totalPnl?: number;
  totalProfit?: number;
  betCount?: number;
  totalBets?: number;
  winRate?: number;
  remainingCredits?: number;
  remainingVotes?: number;
  createdAt?: string;
  followerCount?: number;
  followingCount?: number;
  questionCreatedCount?: number;
  questionCount?: number;
  voteCount?: number;
  totalVotes?: number;
  creatorEarnings?: number;
  memberId?: string | number;
  walletAddress?: string;
}
