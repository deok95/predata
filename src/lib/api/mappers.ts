import type { Member, Question } from '@/types/api';

export function mapQuestion(raw: Record<string, unknown>): Question {
  return {
    id: Number(raw.id),
    title: String(raw.title ?? ''),
    category: (raw.category as Question['category']) ?? undefined,
    status: String(raw.status) as Question['status'],
    type: (raw.type as Question['type']) ?? 'VERIFIABLE',
    totalBetPool: Number(raw.totalBetPool ?? 0),
    yesBetPool: Number(raw.yesBetPool ?? 0),
    noBetPool: Number(raw.noBetPool ?? 0),
    yesPercentage: Number(raw.yesPercentage ?? 50),
    noPercentage: Number(raw.noPercentage ?? 50),
    finalResult: (raw.finalResult as Question['finalResult']) ?? undefined,
    sourceUrl: (raw.sourceUrl as string | undefined) ?? undefined,
    disputeDeadline: (raw.disputeDeadline as string | undefined) ?? undefined,
    votingEndAt: raw.votingEndAt as string,
    bettingStartAt: raw.bettingStartAt as string,
    bettingEndAt: raw.bettingEndAt as string,
    expiredAt: (raw.expiredAt as string | undefined) || (raw.expiresAt as string | undefined) || '',
    createdAt: raw.createdAt as string,
    viewCount: Number(raw.viewCount ?? 0),
    matchId: raw.matchId ? Number(raw.matchId) : undefined,
  };
}

export function mapMember(raw: Record<string, unknown>): Member {
  const rawAgeGroup = raw.ageGroup;

  return {
    id: Number(raw.memberId ?? raw.id),
    email: String(raw.email ?? ''),
    walletAddress: (raw.walletAddress as string | undefined) ?? undefined,
    countryCode: String(raw.countryCode ?? ''),
    jobCategory: (raw.jobCategory as string | undefined) ?? undefined,
    ageGroup: rawAgeGroup != null ? String(rawAgeGroup) : undefined,
    tier: (raw.tier as Member['tier']) ?? 'BRONZE',
    tierWeight: Number(raw.tierWeight ?? 1.0),
    accuracyScore: Number(raw.accuracyScore ?? 0),
    usdcBalance: Number(raw.usdcBalance ?? 0),
    hasVotingPass: Boolean(raw.hasVotingPass ?? false),
    totalPredictions: Number(raw.totalPredictions ?? 0),
    correctPredictions: Number(raw.correctPredictions ?? 0),
    role: (raw.role as Member['role']) ?? 'USER',
    createdAt: (raw.createdAt as string | undefined) ?? new Date().toISOString(),
  };
}
