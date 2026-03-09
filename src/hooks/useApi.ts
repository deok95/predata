import { useState, useEffect, useCallback } from "react";
import type { Dispatch, SetStateAction } from "react";
import {
  questionApi, voteApi, marketApi, portfolioApi,
  activityApi, memberApi, paymentApi, authApi, leaderboardApi,
  mapQuestionToMarket, mapToVoteQuestion, mapToPosition, mapToUser,
  setToken, getToken, isLoggedIn,
} from "../services/api";
import { getWsManager } from "../services/websocket";
import type {
  MarketItem,
  VoteQuestion,
  PositionItem,
  UserProfile,
  ActivityItem,
  SettlementHistoryItem,
  LeaderboardEntryItem,
} from "../types/domain";

type ApiHookResult<TData, TArgs extends unknown[] = []> = {
  data: TData | null;
  loading: boolean;
  error: string | null;
  execute: (...args: TArgs) => Promise<TData>;
  setData: Dispatch<SetStateAction<TData | null>>;
};

// ─── Generic fetch hook ─────────────────────────────────────
function useApi<TData, TArgs extends unknown[] = []>(
  fetcher: (...args: unknown[]) => Promise<TData>,
  deps: readonly unknown[] = [],
  immediate = true
): ApiHookResult<TData, TArgs> {
  const [data, setData] = useState<TData | null>(null);
  const [loading, setLoading] = useState(immediate);
  const [error, setError] = useState<string | null>(null);

  const execute = useCallback(async (...args: unknown[]) => {
    setLoading(true);
    setError(null);
    try {
      const result = await fetcher(...args);
      setData(result);
      return result;
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : "Unknown error";
      setError(message);
      throw err;
    } finally {
      setLoading(false);
    }
  }, deps);

  useEffect(() => {
    if (immediate) execute().catch(() => {});
  }, [immediate, ...deps]);

  return { data, loading, error, execute: execute as (...args: TArgs) => Promise<TData>, setData };
}

// ═══════════════════════════════════════════════════════════════
// AUTH HOOKS
// ═══════════════════════════════════════════════════════════════
export function useAuth() {
  const [user, setUser] = useState<Record<string, unknown> | null>(null);
  const [loggedIn, setLoggedIn] = useState(isLoggedIn());

  const login = async (email: string, password: string) => {
    const res = await authApi.login(email, password);
    setToken(res.token ?? null);
    setLoggedIn(true);
    setUser(res.user ?? null);
    return res;
  };

  const logout = () => {
    setToken(null);
    setLoggedIn(false);
    setUser(null);
  };

  const signup = async (email: string, code: string, profileData: Record<string, unknown>) => {
    await authApi.verifyCode(email, code);
    const profile = profileData as { password: string; passwordConfirm?: string; nickname?: string };
    const res = await authApi.completeSignup({ email, code, ...profile });
    setToken(res.token ?? null);
    setLoggedIn(true);
    setUser(res.user ?? null);
    return res;
  };

  return { user, loggedIn, login, logout, signup, setUser };
}

// ═══════════════════════════════════════════════════════════════
// QUESTION / MARKET HOOKS
// ═══════════════════════════════════════════════════════════════

// 마켓 뷰 라이프사이클: VOTING → BREAK(5분) → BETTING → SETTLED/CANCELLED
// - VOTING          : Vote 페이지 전용 (마켓 뷰 미표시)
// - BREAK           : 마켓 뷰에 "베팅 준비중" 으로 표시 (베팅 불가)
// - BETTING         : 마켓 뷰에 베팅 버튼과 함께 표시
// - SETTLED/CANCELLED: 만료 탭으로 자동 이동
const EXPIRED_STATUSES = ["SETTLED", "CANCELLED"];
const isExpiredMarket = (m: MarketItem) =>
  !!m.status && EXPIRED_STATUSES.includes(m.status.toUpperCase());
const isBettingMarket = (m: MarketItem) => {
  if (!m.status) return true; // status 미설정 시 기본 표시
  const s = m.status.toUpperCase();
  const p = (m.phase || "").toUpperCase();
  // 경기 종료(FINISHED) 이상은 LIVE 노출에서 즉시 제외 (정산 중 포함)
  if (p === "FINISHED" || p === "SETTLED") return false;
  return s === "BETTING" || s === "BREAK"; // BREAK도 마켓 뷰에 표시 (베팅 준비중 라벨)
};
const isActiveUserTrackMarket = (m: MarketItem) => {
  if (!m.status) return true;
  const s = m.status.toUpperCase();
  // User market list should only show market-phase items.
  // VOTING items must stay in Vote page only.
  return s === "BREAK" || s === "BETTING";
};
const isLiveMarket = (m: MarketItem) => (m.category || "").toUpperCase() === "LIVE";
const isSportsApiMarket = (m: MarketItem) => m.matchId !== undefined && m.matchId !== null;
const isLiveTrackMarket = (m: MarketItem) => isLiveMarket(m) || isSportsApiMarket(m);
const isUserTrackMarket = (m: MarketItem) => !isLiveTrackMarket(m);
type UseMarketsOptions = {
  includePrematchLive?: boolean;
};

// 홈 - 질문 목록 (마켓 카드로 변환)
// 백엔드 /api/questions는 category 파라미터를 지원하지 않으므로 전체 fetch 후 클라이언트 필터링
export function useMarkets(category = "trending", options: UseMarketsOptions = {}) {
  const [allMarkets, setAllMarkets] = useState<MarketItem[]>([]);
  const [markets, setMarkets] = useState<MarketItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const includePrematchLive = Boolean(options.includePrematchLive);

  const fetch = useCallback(async () => {
    setLoading(true);
    try {
      const questions = await questionApi.list({
        page: 0,
        size: 300,
        sortBy: "createdAt",
        sortDir: "desc",
      });
      setAllMarkets((questions || []).map(mapQuestionToMarket));
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : "Unknown error";
      setError(message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetch();
    // Subscribe to any pool update and refresh market list
    let cancelled = false;
    const controller = new AbortController();
    let sub: { unsubscribe: () => void } | null = null;
    getWsManager()?.subscribe("/topic/markets", () => {
      if (!cancelled) fetch();
    }, controller.signal).then(s => {
      if (cancelled) {
        s.unsubscribe();
        return;
      }
      sub = s;
    }).catch(() => {});
    return () => {
      cancelled = true;
      controller.abort();
      sub?.unsubscribe();
    };
  }, [fetch]);

  // 카테고리 변경 시 클라이언트 사이드 필터링 (API 재호출 없음)
  // "expired" = SETTLED/CANCELLED만 표시
  // 그 외   = BETTING 상태만 표시 (VOTING 등은 Vote 페이지 전용)
  useEffect(() => {
    if (category === "expired") {
      // Expired tab should only show user-track markets.
      setMarkets(allMarkets.filter((m) => isExpiredMarket(m) && isUserTrackMarket(m)));
      return;
    }
    const betting = allMarkets.filter(isBettingMarket);
    const bettingLiveTrack = betting.filter(isLiveTrackMarket);
    const activeUserTrack = allMarkets.filter((m) => isUserTrackMarket(m) && isActiveUserTrackMarket(m));

    if (category === "live") {
      // Home LIVE: only active LIVE category cards.
      // Header LIVE tab: all live-track cards (prematch + live + end filtering handled in ExploreView).
      if (includePrematchLive) {
        setMarkets(allMarkets.filter(isLiveTrackMarket));
      } else {
        setMarkets(bettingLiveTrack.filter(isLiveMarket));
      }
      return;
    }

    setMarkets(
      category === "trending"
        ? activeUserTrack
        : activeUserTrack.filter((m) => m.category?.toLowerCase() === category.toLowerCase())
    );
  }, [category, allMarkets, includePrematchLive]);

  return { markets, loading, error, refresh: fetch };
}

// 질문 상세
export function useQuestionDetail(id: number | string | null) {
  return useApi(() => (id ? questionApi.detail(id) : Promise.resolve(null)), [id], !!id);
}

// 상태별 질문
export function useQuestionsByStatus(status: string) {
  return useApi(() => questionApi.byStatus(status), [status]);
}

// ═══════════════════════════════════════════════════════════════
// VOTE HOOKS
// ═══════════════════════════════════════════════════════════════

// 투표 피드
export function useVoteFeed(params = {}) {
  const [questions, setQuestions] = useState<VoteQuestion[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetch = useCallback(async () => {
    setLoading(true);
    try {
      const data = await voteApi.feed(params);
      const primary = (data || []).map(mapToVoteQuestion);

      // Fallback: if vote feed is unexpectedly sparse, backfill from VOTING questions.
      // This prevents "only 2 items shown" issues when feed API temporarily under-returns.
      let resolved = primary;
      if (primary.length < 5) {
        const fallbackRaw = await questionApi.list({
          page: 0,
          size: 300,
          sortBy: "createdAt",
          sortDir: "desc",
        });
        const requestedCategory = String((params as any)?.category ?? "").toLowerCase();
        const requestedWindow = String((params as any)?.voteWindowType ?? (params as any)?.window ?? "").toUpperCase();
        const inferVoteWindowType = (q: any): "H6" | "D1" | "D3" | null => {
          const raw = String(q?.voteWindowType ?? "").toUpperCase();
          if (raw === "H6" || raw === "D1" || raw === "D3") return raw as "H6" | "D1" | "D3";
          const startRaw = q?.createdAt ?? q?.submittedAt ?? q?.submitted ?? null;
          const endRaw = q?.votingEndAt ?? null;
          if (!startRaw || !endRaw) return null;
          const start = new Date(String(startRaw)).getTime();
          const end = new Date(String(endRaw)).getTime();
          if (Number.isNaN(start) || Number.isNaN(end) || end <= start) return null;
          const hours = (end - start) / 3600000;
          if (hours <= 9) return "H6";
          if (hours <= 36) return "D1";
          return "D3";
        };

        const fallback = (fallbackRaw || [])
          .filter((q: any) => {
            const status = String(q?.status ?? "").toUpperCase();
            const category = String(q?.category ?? "").toUpperCase();
            if (status !== "VOTING") return false;
            if (category === "LIVE") return false;
            if (requestedCategory && String(q?.category ?? "").toLowerCase() !== requestedCategory) return false;
            if (requestedWindow) {
              const qWindow = inferVoteWindowType(q);
              // Strict window match: if requested, only exact H6/D1/D3 items are allowed.
              if (qWindow !== requestedWindow) return false;
            }
            // OBJECTIVE_RULE 및 VOTE_RESULT(OPINION) 타입 모두 포함
            // (백엔드가 /api/votes에서 양쪽 타입을 허용하므로 타입 제한 없음)
            return true;
          })
          .map(mapToVoteQuestion);

        if (fallback.length > resolved.length) {
          resolved = fallback;
        }
      }

      setQuestions(resolved);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : "Unknown error";
      setError(message);
    } finally {
      setLoading(false);
    }
  }, [JSON.stringify(params)]);

  useEffect(() => {
    fetch();
    // Subscribe to any vote update and refresh feed
    let cancelled = false;
    let sub: { unsubscribe: () => void } | null = null;
    getWsManager()?.subscribe("/topic/votes", () => {
      if (!cancelled) fetch();
    }).then(s => { sub = s; });
    return () => {
      cancelled = true;
      sub?.unsubscribe();
    };
  }, [fetch]);

  const castVote = async (questionId: number | string, choice: "YES" | "NO") => {
    // 타입별 라우팅:
    // - OBJECTIVE_RULE: /api/votes (표준 단일 투표)
    // - VOTE_RESULT (OPINION): /api/votes (백엔드가 양쪽 타입 허용)
    //   commit/reveal 2단계 플로우가 필요한 경우엔 voteApi.commit()/reveal() 직접 호출
    const res: any = await voteApi.vote(questionId, choice);
    // Optimistic update
    setQuestions(prev =>
      prev.map((q: VoteQuestion) => {
        if (q.id !== questionId) return q;
        return {
          ...q,
          yesVotes: choice === "YES" ? q.yesVotes + 1 : q.yesVotes,
          noVotes: choice === "NO" ? q.noVotes + 1 : q.noVotes,
        };
      })
    );
    return res;
  };

  return { questions, loading, error, refresh: fetch, castVote };
}

// 투표 가능 여부
export function useVoteStatus(questionId: number | string | null) {
  return useApi(() => (questionId ? voteApi.status(questionId) : Promise.resolve(null)), [questionId], !!questionId);
}

// ═══════════════════════════════════════════════════════════════
// MARKET / AMM HOOKS
// ═══════════════════════════════════════════════════════════════

// 풀 상태
export function usePool(questionId: number | string | null) {
  return useApi(() => (questionId ? marketApi.pool(questionId) : Promise.resolve(null)), [questionId], !!questionId);
}

// 스왑 시뮬레이션
export function useSwapSimulation() {
  const [result, setResult] = useState<unknown>(null);
  const [loading, setLoading] = useState(false);

  const simulate = async (params: { questionId: number | string; side?: string; amount: number | string; direction?: string }) => {
    setLoading(true);
    try {
      const data = await marketApi.simulate(params);
      setResult(data);
      return data;
    } catch (err: unknown) {
      setResult(null);
      throw err;
    } finally {
      setLoading(false);
    }
  };

  return { result, loading, simulate };
}

// 스왑 실행
export function useSwap() {
  const [loading, setLoading] = useState(false);

  const executeSwap = async (data: { questionId: number | string; direction?: string; side?: string; amount?: number | string; shares?: number | string }) => {
    setLoading(true);
    try {
      return await marketApi.swap(data);
    } finally {
      setLoading(false);
    }
  };

  return { loading, executeSwap };
}

// 내 쉐어 (로그인 상태에서만 호출)
export function useMyShares(questionId: number | string | null) {
  return useApi(() => (questionId && isLoggedIn() ? marketApi.myShares(questionId) : Promise.resolve(null)), [questionId], !!questionId && isLoggedIn());
}

// 거래 히스토리
export function useSwapHistory(questionId: number | string | null) {
  return useApi(() => (questionId ? marketApi.history(questionId) : Promise.resolve([])), [questionId], !!questionId);
}

// ═══════════════════════════════════════════════════════════════
// PORTFOLIO HOOKS
// ═══════════════════════════════════════════════════════════════

export function usePortfolioSummary() {
  return useApi(() => portfolioApi.summary(), []);
}

export function usePositions() {
  const { data, ...rest } = useApi(() => (isLoggedIn() ? portfolioApi.positions() : Promise.resolve([])), []);
  return { positions: ((data as unknown[]) || []).map(mapToPosition), ...rest };
}

export function useSettlementHistory() {
  return useApi<SettlementHistoryItem[]>(() => (isLoggedIn() ? portfolioApi.settlementHistory() : Promise.resolve([])), []);
}

// ═══════════════════════════════════════════════════════════════
// MEMBER / SOCIAL HOOKS
// ═══════════════════════════════════════════════════════════════

export function useMyProfile() {
  const [user, setUser] = useState<UserProfile | null>(null);
  const [loading, setLoading] = useState(isLoggedIn());
  const [error, setError] = useState<string | null>(null);

  const fetch = useCallback(async () => {
    setLoading(true);
    try {
      const [me, dashboard] = await Promise.all([
        memberApi.me(),
        memberApi.dashboard(),
      ]);
      const meObj = (me && typeof me === "object") ? me as Record<string, unknown> : {};
      const dashboardObj = (dashboard && typeof dashboard === "object") ? dashboard as Record<string, unknown> : {};
      setUser(mapToUser({ ...meObj, ...dashboardObj }));
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : "Unknown error";
      setError(message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (!isLoggedIn()) {
      setLoading(false);
      setUser(null);
      return;
    }
    fetch();
  }, [fetch]);

  return { user, loading, error, refresh: fetch };
}

export function useFollow() {
  const follow = async (userId: number | string) => memberApi.follow(userId);
  const unfollow = async (userId: number | string) => memberApi.unfollow(userId);
  return { follow, unfollow };
}

// ═══════════════════════════════════════════════════════════════
// ACTIVITY HOOKS
// ═══════════════════════════════════════════════════════════════

export function useMyActivity(params = {}) {
  return useApi(() => (isLoggedIn() ? activityApi.myActivity(params) : Promise.resolve([])), [JSON.stringify(params)]) as {
    data: ActivityItem[] | null;
    loading: boolean;
    error: string | null;
    execute: (...args: unknown[]) => Promise<ActivityItem[]>;
    setData: Dispatch<SetStateAction<ActivityItem[] | null>>;
  };
}

// ═══════════════════════════════════════════════════════════════
// QUESTION SUBMIT HOOKS
// ═══════════════════════════════════════════════════════════════

export function useQuestionSubmit() {
  const [loading, setLoading] = useState(false);
  const [draftId, setDraftId] = useState<number | string | null>(null);

  const createAndSubmit = async (data: Record<string, unknown>) => {
    setLoading(true);
    try {
      // Step 1: Create draft — response contains { draftId, idempotencyKey, ... }
      const draft = await questionApi.createDraft() as Record<string, unknown>;
      const id = (draft.draftId ?? draft.id) as number | string;
      setDraftId(id);
      // Step 2: Submit draft — use idempotencyKey from createDraft response
      const result = await questionApi.submitDraft(id, data, (draft.idempotencyKey ?? draft.submitIdempotencyKey) as string | undefined);
      return result;
    } finally {
      setLoading(false);
    }
  };

  const cancelDraft = async () => {
    if (draftId) {
      await questionApi.cancelDraft(draftId);
      setDraftId(null);
    }
  };

  return { loading, createAndSubmit, cancelDraft };
}

// ═══════════════════════════════════════════════════════════════
// PAYMENT HOOKS
// ═══════════════════════════════════════════════════════════════

export function useTransactions() {
  return useApi(() => (isLoggedIn() ? paymentApi.myTransactions() : Promise.resolve([])), []);
}

export function useLeaderboardTop(limit = 50) {
  return useApi<LeaderboardEntryItem[]>(() => leaderboardApi.top(limit), [limit]);
}
