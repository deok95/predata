import type {
  Question,
  GlobalStats,
  Activity,
  SettlementHistory,
  QualityDashboard,
} from '@/types/api';

// ===== 더미 질문 (마켓) 데이터 =====
export const mockQuestions: Question[] = [
  {
    id: 1,
    title: '2025년 비트코인이 $150,000를 돌파할까?',
    category: 'ECONOMY',
    status: 'BETTING',
    type: 'VERIFIABLE',
    executionModel: 'ORDERBOOK_LEGACY',
    totalBetPool: 2847500,
    yesBetPool: 1708500,
    noBetPool: 1139000,
    yesPercentage: 60,
    noPercentage: 40,
    votingEndAt: '2025-01-20T23:59:59Z',
    bettingStartAt: '2025-01-21T00:00:00Z',
    bettingEndAt: '2025-12-31T23:59:59Z',
    expiredAt: '2025-12-31T23:59:59Z',
    createdAt: '2025-01-15T09:00:00Z',
  },
  {
    id: 2,
    title: '손흥민이 2025 시즌 프리미어리그 득점왕이 될까?',
    category: 'SPORTS',
    status: 'BETTING',
    type: 'VERIFIABLE',
    executionModel: 'ORDERBOOK_LEGACY',
    totalBetPool: 1523000,
    yesBetPool: 685350,
    noBetPool: 837650,
    yesPercentage: 45,
    noPercentage: 55,
    votingEndAt: '2025-01-25T23:59:59Z',
    bettingStartAt: '2025-01-26T00:00:00Z',
    bettingEndAt: '2025-05-25T23:59:59Z',
    expiredAt: '2025-05-25T23:59:59Z',
    createdAt: '2025-01-20T12:00:00Z',
  },
  {
    id: 3,
    title: '2025년 한국은행이 기준금리를 2.5% 이하로 인하할까?',
    category: 'ECONOMY',
    status: 'BETTING',
    type: 'VERIFIABLE',
    executionModel: 'ORDERBOOK_LEGACY',
    totalBetPool: 1985000,
    yesBetPool: 1290250,
    noBetPool: 694750,
    yesPercentage: 65,
    noPercentage: 35,
    votingEndAt: '2025-01-15T23:59:59Z',
    bettingStartAt: '2025-01-16T00:00:00Z',
    bettingEndAt: '2025-12-31T23:59:59Z',
    expiredAt: '2025-12-31T23:59:59Z',
    createdAt: '2025-01-10T10:00:00Z',
  },
  {
    id: 4,
    title: 'Apple이 2025년 내 폴더블 아이폰을 출시할까?',
    category: 'TECH',
    status: 'BETTING',
    type: 'VERIFIABLE',
    executionModel: 'ORDERBOOK_LEGACY',
    totalBetPool: 3210000,
    yesBetPool: 1284000,
    noBetPool: 1926000,
    yesPercentage: 40,
    noPercentage: 60,
    votingEndAt: '2025-01-10T23:59:59Z',
    bettingStartAt: '2025-01-11T00:00:00Z',
    bettingEndAt: '2025-12-31T23:59:59Z',
    expiredAt: '2025-12-31T23:59:59Z',
    createdAt: '2025-01-05T08:00:00Z',
  },
  {
    id: 5,
    title: '2025년 총선에서 여당이 과반 의석을 확보할까?',
    category: 'POLITICS',
    status: 'BETTING',
    type: 'VERIFIABLE',
    executionModel: 'ORDERBOOK_LEGACY',
    totalBetPool: 4125000,
    yesBetPool: 1856250,
    noBetPool: 2268750,
    yesPercentage: 45,
    noPercentage: 55,
    votingEndAt: '2025-02-06T23:59:59Z',
    bettingStartAt: '2025-02-07T00:00:00Z',
    bettingEndAt: '2025-06-15T23:59:59Z',
    expiredAt: '2025-06-15T23:59:59Z',
    createdAt: '2025-02-01T06:00:00Z',
  },
  {
    id: 6,
    title: '넷플릭스 오징어게임 시즌3 공개 첫주 시청 1억 돌파?',
    category: 'CULTURE',
    status: 'BETTING',
    type: 'VERIFIABLE',
    executionModel: 'ORDERBOOK_LEGACY',
    totalBetPool: 1876000,
    yesBetPool: 1313200,
    noBetPool: 562800,
    yesPercentage: 70,
    noPercentage: 30,
    votingEndAt: '2025-01-30T23:59:59Z',
    bettingStartAt: '2025-01-31T00:00:00Z',
    bettingEndAt: '2025-08-01T23:59:59Z',
    expiredAt: '2025-08-01T23:59:59Z',
    createdAt: '2025-01-25T14:00:00Z',
  },
  {
    id: 7,
    title: 'Ethereum이 2025년 상반기에 $5,000 도달할까?',
    category: 'ECONOMY',
    status: 'BETTING',
    type: 'VERIFIABLE',
    executionModel: 'ORDERBOOK_LEGACY',
    totalBetPool: 2150000,
    yesBetPool: 1075000,
    noBetPool: 1075000,
    yesPercentage: 50,
    noPercentage: 50,
    votingEndAt: '2025-01-23T23:59:59Z',
    bettingStartAt: '2025-01-24T00:00:00Z',
    bettingEndAt: '2025-06-30T23:59:59Z',
    expiredAt: '2025-06-30T23:59:59Z',
    createdAt: '2025-01-18T11:00:00Z',
  },
  {
    id: 8,
    title: '대한민국 남자축구 대표팀이 2026 월드컵 16강 진출?',
    category: 'SPORTS',
    status: 'BETTING',
    type: 'VERIFIABLE',
    executionModel: 'ORDERBOOK_LEGACY',
    totalBetPool: 3560000,
    yesBetPool: 2492000,
    noBetPool: 1068000,
    yesPercentage: 70,
    noPercentage: 30,
    votingEndAt: '2025-01-17T23:59:59Z',
    bettingStartAt: '2025-01-18T00:00:00Z',
    bettingEndAt: '2026-07-19T23:59:59Z',
    expiredAt: '2026-07-19T23:59:59Z',
    createdAt: '2025-01-12T10:00:00Z',
  },
  {
    id: 9,
    title: 'OpenAI GPT-5가 2025년 상반기에 공개될까?',
    category: 'TECH',
    status: 'BETTING',
    type: 'VERIFIABLE',
    executionModel: 'ORDERBOOK_LEGACY',
    totalBetPool: 2780000,
    yesBetPool: 1668000,
    noBetPool: 1112000,
    yesPercentage: 60,
    noPercentage: 40,
    votingEndAt: '2025-01-13T23:59:59Z',
    bettingStartAt: '2025-01-14T00:00:00Z',
    bettingEndAt: '2025-06-30T23:59:59Z',
    expiredAt: '2025-06-30T23:59:59Z',
    createdAt: '2025-01-08T09:00:00Z',
  },
  {
    id: 10,
    title: '2025년 서울 아파트 평균가가 15억을 넘을까?',
    category: 'ECONOMY',
    status: 'SETTLED',
    type: 'VERIFIABLE',
    executionModel: 'ORDERBOOK_LEGACY',
    totalBetPool: 1945000,
    yesBetPool: 1167000,
    noBetPool: 778000,
    yesPercentage: 60,
    noPercentage: 40,
    finalResult: 'YES',
    votingEndAt: '2024-12-06T23:59:59Z',
    bettingStartAt: '2024-12-07T00:00:00Z',
    bettingEndAt: '2025-01-31T23:59:59Z',
    expiredAt: '2025-01-31T23:59:59Z',
    createdAt: '2024-12-01T10:00:00Z',
  },
  {
    id: 11,
    title: 'BTS 완전체 컴백이 2025년에 이루어질까?',
    category: 'CULTURE',
    status: 'BETTING',
    type: 'VERIFIABLE',
    executionModel: 'ORDERBOOK_LEGACY',
    totalBetPool: 2340000,
    yesBetPool: 1872000,
    noBetPool: 468000,
    yesPercentage: 80,
    noPercentage: 20,
    votingEndAt: '2025-01-27T23:59:59Z',
    bettingStartAt: '2025-01-28T00:00:00Z',
    bettingEndAt: '2025-12-31T23:59:59Z',
    expiredAt: '2025-12-31T23:59:59Z',
    createdAt: '2025-01-22T15:00:00Z',
  },
  {
    id: 12,
    title: '삼성전자 주가가 2025년 내 10만원 돌파할까?',
    category: 'ECONOMY',
    status: 'BETTING',
    type: 'VERIFIABLE',
    executionModel: 'ORDERBOOK_LEGACY',
    totalBetPool: 2650000,
    yesBetPool: 1060000,
    noBetPool: 1590000,
    yesPercentage: 40,
    noPercentage: 60,
    votingEndAt: '2025-01-19T23:59:59Z',
    bettingStartAt: '2025-01-20T00:00:00Z',
    bettingEndAt: '2025-12-31T23:59:59Z',
    expiredAt: '2025-12-31T23:59:59Z',
    createdAt: '2025-01-14T08:30:00Z',
  },
];

// ===== 글로벌 통계 더미 데이터 =====
export const mockGlobalStats: GlobalStats = {
  totalPredictions: 124082,
  tvl: 4240000,
  activeUsers: 8412,
  cumulativeRewards: 1200000,
  activeMarkets: 12,
};

// ===== 활동 피드 더미 데이터 =====
export const mockActivities: Activity[] = [
  { id: 1, memberId: 101, questionId: 1, activityType: 'BET', choice: 'YES', amount: 5000, latencyMs: 3200, createdAt: '2025-01-28T14:32:00Z' },
  { id: 2, memberId: 102, questionId: 1, activityType: 'VOTE', choice: 'YES', amount: 0, latencyMs: 4100, createdAt: '2025-01-28T14:28:00Z' },
  { id: 3, memberId: 103, questionId: 1, activityType: 'BET', choice: 'NO', amount: 12000, latencyMs: 2800, createdAt: '2025-01-28T14:25:00Z' },
  { id: 4, memberId: 104, questionId: 1, activityType: 'VOTE', choice: 'NO', amount: 0, latencyMs: 5600, createdAt: '2025-01-28T14:20:00Z' },
  { id: 5, memberId: 105, questionId: 1, activityType: 'BET', choice: 'YES', amount: 25000, latencyMs: 3800, createdAt: '2025-01-28T14:15:00Z' },
  { id: 6, memberId: 106, questionId: 2, activityType: 'BET', choice: 'YES', amount: 8000, latencyMs: 4200, createdAt: '2025-01-28T13:50:00Z' },
  { id: 7, memberId: 107, questionId: 2, activityType: 'VOTE', choice: 'NO', amount: 0, latencyMs: 3100, createdAt: '2025-01-28T13:45:00Z' },
  { id: 8, memberId: 108, questionId: 3, activityType: 'BET', choice: 'YES', amount: 15000, latencyMs: 2900, createdAt: '2025-01-28T13:40:00Z' },
  { id: 9, memberId: 109, questionId: 4, activityType: 'BET', choice: 'NO', amount: 20000, latencyMs: 5200, createdAt: '2025-01-28T13:35:00Z' },
  { id: 10, memberId: 110, questionId: 5, activityType: 'VOTE', choice: 'YES', amount: 0, latencyMs: 4500, createdAt: '2025-01-28T13:30:00Z' },
  { id: 11, memberId: 111, questionId: 1, activityType: 'BET', choice: 'YES', amount: 50000, latencyMs: 6100, createdAt: '2025-01-28T13:20:00Z' },
  { id: 12, memberId: 112, questionId: 6, activityType: 'BET', choice: 'YES', amount: 10000, latencyMs: 3300, createdAt: '2025-01-28T13:15:00Z' },
];

// ===== 정산 내역 더미 데이터 (마이페이지 베팅 히스토리) =====
export const mockSettlementHistory: SettlementHistory[] = [
  { questionId: 10, questionTitle: '2025년 서울 아파트 평균가가 15억을 넘을까?', myChoice: 'YES', finalResult: 'YES', betAmount: 5000, payout: 8325, profit: 3325, isWinner: true },
  { questionId: 20, questionTitle: 'Tesla 주가가 2024년 $300 도달할까?', myChoice: 'YES', finalResult: 'NO', betAmount: 10000, payout: 0, profit: -10000, isWinner: false },
  { questionId: 21, questionTitle: '2024 파리올림픽에서 한국이 금메달 10개 이상?', myChoice: 'YES', finalResult: 'YES', betAmount: 8000, payout: 14560, profit: 6560, isWinner: true },
  { questionId: 22, questionTitle: '2024년 미국 기준금리 인하가 3회 이상 될까?', myChoice: 'NO', finalResult: 'NO', betAmount: 15000, payout: 22050, profit: 7050, isWinner: true },
  { questionId: 23, questionTitle: 'ChatGPT가 2024년 구독자 1억명 돌파?', myChoice: 'YES', finalResult: 'YES', betAmount: 3000, payout: 4950, profit: 1950, isWinner: true },
  { questionId: 24, questionTitle: '2024 K리그 우승팀이 울산인가?', myChoice: 'NO', finalResult: 'YES', betAmount: 7000, payout: 0, profit: -7000, isWinner: false },
];

// ===== 게스트 유저 활동 더미 데이터 =====
export const mockGuestActivities: Activity[] = [
  { id: 201, memberId: -1, questionId: 1, activityType: 'BET', choice: 'YES', amount: 2000, latencyMs: 3500, createdAt: '2025-01-28T12:00:00Z' },
  { id: 202, memberId: -1, questionId: 4, activityType: 'BET', choice: 'NO', amount: 3000, latencyMs: 4200, createdAt: '2025-01-27T15:30:00Z' },
  { id: 203, memberId: -1, questionId: 9, activityType: 'VOTE', choice: 'YES', amount: 0, latencyMs: 2800, createdAt: '2025-01-27T10:15:00Z' },
];

// ===== 데이터센터 품질 대시보드 더미 데이터 =====
// questionId에 따라 다른 mock 데이터를 생성하는 함수
function seededRandom(seed: number): () => number {
  let s = seed;
  return () => {
    s = (s * 16807 + 0) % 2147483647;
    return (s - 1) / 2147483646;
  };
}

export function generateMockDashboard(questionId: number): QualityDashboard {
  const rand = seededRandom(questionId * 31 + 7);
  const r = () => rand();

  const totalVotes = Math.floor(1500 + r() * 5000);
  const yesRatio = 0.35 + r() * 0.3; // 35~65%
  const qualityScore = 60 + r() * 35; // 60~95
  const gapPct = 1 + r() * 15; // 1~16%

  const mkCountry = (code: string) => {
    const total = Math.floor(100 + r() * 800);
    const yesPct = 40 + r() * 30;
    const yesCount = Math.floor(total * yesPct / 100);
    return { countryCode: code, yesCount, noCount: total - yesCount, yesPercentage: Math.round(yesPct * 10) / 10, total };
  };
  const mkJob = (cat: string) => {
    const total = Math.floor(100 + r() * 500);
    const yesPct = 35 + r() * 35;
    const yesCount = Math.floor(total * yesPct / 100);
    return { jobCategory: cat, yesCount, noCount: total - yesCount, yesPercentage: Math.round(yesPct * 10) / 10, total };
  };
  const mkAge = (group: number) => {
    const total = Math.floor(100 + r() * 600);
    const yesPct = 40 + r() * 30;
    const yesCount = Math.floor(total * yesPct / 100);
    return { ageGroup: group, yesCount, noCount: total - yesCount, yesPercentage: Math.round(yesPct * 10) / 10, total };
  };

  const voteYes = Math.round(yesRatio * 1000) / 10;
  const betYes = Math.round((yesRatio + (r() - 0.5) * 0.1) * 1000) / 10;
  const voteYesCount = Math.floor(totalVotes * yesRatio);
  const betTotal = Math.floor(totalVotes * (0.5 + r() * 0.3));
  const betYesCount = Math.floor(betTotal * betYes / 100);
  const beforeTotal = Math.floor(totalVotes * (1.05 + r() * 0.1));
  const filteredCount = beforeTotal - totalVotes;

  return {
    questionId,
    overallQualityScore: Math.round(qualityScore * 10) / 10,
    demographics: {
      questionId,
      totalVotes,
      byCountry: ['KR', 'US', 'JP', 'CN', 'UK', 'DE'].map(mkCountry),
      byJob: ['IT/개발', '금융', '학생', '공무원', '자영업', '기타'].map(mkJob),
      byAge: [20, 30, 40, 50, 60].map(mkAge),
    },
    gapAnalysis: {
      questionId,
      voteDistribution: { yesPercentage: voteYes, noPercentage: Math.round((100 - voteYes) * 10) / 10, yesCount: voteYesCount, noCount: totalVotes - voteYesCount },
      betDistribution: { yesPercentage: betYes, noPercentage: Math.round((100 - betYes) * 10) / 10, yesCount: betYesCount, noCount: betTotal - betYesCount },
      gapPercentage: Math.round(gapPct * 10) / 10,
      qualityScore: Math.round((100 - gapPct * 2) * 10) / 10,
    },
    filteringEffect: {
      questionId,
      beforeFiltering: { totalCount: beforeTotal, yesPercentage: Math.round((voteYes + r() * 3) * 10) / 10, noPercentage: Math.round((100 - voteYes - r() * 3) * 10) / 10 },
      afterFiltering: { totalCount: totalVotes, yesPercentage: voteYes, noPercentage: Math.round((100 - voteYes) * 10) / 10 },
      filteredCount,
      filteredPercentage: Math.round((filteredCount / beforeTotal) * 1000) / 10,
    },
  };
}

export const mockQualityDashboard: QualityDashboard = generateMockDashboard(1);
