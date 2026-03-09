import type {
  MarketItem,
  VoteQuestion,
  PositionItem,
  UserProfile,
  CommentItem,
  CreatedQuestionItem,
  SettlementHistoryItem,
  LeaderboardEntryItem,
} from "../types/domain";
import type { VoteChoice, SettlementMode } from "../types/domain";
import type {
  AuthResponseDto,
  QuestionDto,
  VoteResultQuestionDto,
  PositionDto,
  MemberDto,
} from "../types/api-dto";

type ApiRecord = Record<string, unknown>;
type QueryValue = string | number | boolean | null | undefined;
type QueryParams = Record<string, QueryValue>;

const toQueryString = (params: QueryParams = {}) =>
  new URLSearchParams(
    Object.entries(params)
      .filter(([, v]) => v !== undefined && v !== null)
      .map(([k, v]) => [k, String(v)])
  ).toString();

const asRecord = (value: unknown): ApiRecord =>
  value && typeof value === "object" ? (value as ApiRecord) : {};

const asNumber = (value: unknown, fallback = 0): number =>
  typeof value === "number" ? value : (typeof value === "string" && value !== "" ? Number(value) : fallback);

const recoverMojibake = (input: string): string => {
  // Recover common UTF-8 -> latin1 mojibake (e.g. "íŠ¸ëŸ¼í”„" -> "트럼프")
  if (!/[ÃÂìíëêðÐœžŸ]/.test(input)) return input;
  if (/[가-힣]/.test(input)) return input;
  try {
    const bytes = Uint8Array.from([...input].map((ch) => ch.charCodeAt(0) & 0xff));
    const decoded = new TextDecoder("utf-8", { fatal: false }).decode(bytes);
    return /[가-힣]/.test(decoded) ? decoded : input;
  } catch {
    return input;
  }
};

const asString = (value: unknown, fallback = ""): string => {
  if (typeof value !== "string") return fallback;
  return recoverMojibake(value);
};

const asStringArray = (value: unknown): string[] =>
  Array.isArray(value) ? value.map((v) => String(v)) : [];

const asNumberLike = (value: unknown, fallback = 0): number => {
  if (typeof value === "number" && Number.isFinite(value)) return value;
  if (typeof value === "string" && value !== "") {
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : fallback;
  }
  return fallback;
};

const parseJsonStringArray = (value: unknown): string[] => {
  if (Array.isArray(value)) return value.map((v) => String(v));
  if (typeof value !== "string" || !value.trim()) return [];
  try {
    const parsed = JSON.parse(value);
    return Array.isArray(parsed) ? parsed.map((v) => String(v)) : [];
  } catch {
    return [];
  }
};

const mapCategoryCode = (value: unknown): string => {
  const v = asString(value).trim().toLowerCase();
  switch (v) {
    case "live": return "LIVE";
    case "sports": return "SPORTS";
    case "politics": return "POLITICS";
    case "economy": return "ECONOMY";
    case "tech": return "TECH";
    case "entertainment": return "ENTERTAINMENT";
    case "culture": return "CULTURE";
    case "international": return "INTERNATIONAL";
    case "general": return "GENERAL";
    case "crypto": return "GENERAL";
    default: return asString(value).toUpperCase() || "GENERAL";
  }
};

// ─── API Configuration ──────────────────────────────────────
const API_BASE = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

// ─── Token Management ───────────────────────────────────────
const safeStorage = typeof window !== "undefined" && window.localStorage
  ? window.localStorage
  : { getItem: () => null, setItem: () => {}, removeItem: () => {} };

const readStoredToken = () => safeStorage.getItem("predata_token");
const readStoredMemberId = () => safeStorage.getItem("predata_member_id");

let accessToken = readStoredToken();
let currentMemberId: string | null = readStoredMemberId();

const syncAuthCacheFromStorage = () => {
  accessToken = readStoredToken();
  currentMemberId = readStoredMemberId();
};

export const setToken = (token: string | null) => {
  accessToken = token;
  if (token) safeStorage.setItem("predata_token", token);
  else safeStorage.removeItem("predata_token");
};

export const setMemberId = (id: string | number | null) => {
  currentMemberId = id === null ? null : String(id);
  if (id !== null) safeStorage.setItem("predata_member_id", String(id));
  else safeStorage.removeItem("predata_member_id");
};

export const getToken = () => {
  if (!accessToken) accessToken = readStoredToken();
  return accessToken;
};
export const getMemberId = () => {
  if (!currentMemberId) currentMemberId = readStoredMemberId();
  return currentMemberId;
};
export const isLoggedIn = () => !!getToken();

const decodeJwtPayload = (token: string): ApiRecord | null => {
  try {
    const [, payload] = token.split(".");
    if (!payload) return null;
    const normalized = payload.replace(/-/g, "+").replace(/_/g, "/");
    const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, "=");
    const json = atob(padded);
    return JSON.parse(json) as ApiRecord;
  } catch {
    return null;
  }
};

export const getTokenExpiryMs = (): number | null => {
  const token = getToken();
  if (!token) return null;
  const payload = decodeJwtPayload(token);
  const exp = asNumber(payload?.exp, 0);
  return exp > 0 ? exp * 1000 : null;
};

// ─── FIX #3: Robust Fetch Wrapper ──────────────────────────
// Handles 410/empty body/HTML responses without crashing
const apiFetch = async <T = unknown>(path: string, options: RequestInit = {}): Promise<T> => {
  // Keep auth cache in sync after hard refresh / tab restore
  syncAuthCacheFromStorage();
  const tokenAtRequest = getToken();
  const headers = {
    "Content-Type": "application/json",
    ...(tokenAtRequest ? { Authorization: `Bearer ${tokenAtRequest}` } : {}),
    ...options.headers,
  };

  let res;
  try {
    res = await fetch(`${API_BASE}${path}`, { ...options, headers });
  } catch (networkErr: unknown) {
    const message = networkErr instanceof Error ? networkErr.message : "Unknown network error";
    throw new Error(`Network error: ${message}`);
  }

  // Handle empty / non-JSON responses
  const contentType = res.headers.get("content-type") || "";
  let json: ApiRecord;

  if (res.status === 204 || res.headers.get("content-length") === "0") {
    // No content — success with no body
    if (res.ok) return null as T;
    throw new Error(`HTTP ${res.status}`);
  }

  if (!contentType.includes("application/json")) {
    // Non-JSON response (HTML error pages, 410 Gone plain text, etc.)
    const text = await res.text().catch(() => "");
    if (!res.ok) {
      throw new Error(text.substring(0, 200) || `HTTP ${res.status}`);
    }
    return text;
  }

  try {
    json = await res.json();
  } catch (parseErr) {
    throw new Error(`Invalid JSON response (HTTP ${res.status})`);
  }

  // Clear auth only for authoritative session-check endpoints.
  // Avoid logging users out due to unrelated endpoint 401s (e.g. partial MyPage APIs).
  const shouldClearAuthOn401 =
    path.startsWith("/api/members/me") ||
    path.startsWith("/api/auth/refresh") ||
    path.startsWith("/api/auth/login");

  if (res.status === 401 && shouldClearAuthOn401 && tokenAtRequest && getToken() === tokenAtRequest) {
    setToken(null);
    setMemberId(null);
  }

  // ApiEnvelope: { success, data, error, timestamp }
  if (!res.ok || json.success === false) {
    const errorObj = asRecord(json.error);
    const errMsg = asString(errorObj.message) || asString(json.message) || `HTTP ${res.status}`;
    throw new Error(errMsg);
  }

  return json.data as T;
};

const get = <T = unknown>(path: string, extraHeaders?: HeadersInit) =>
  apiFetch<T>(path, { method: "GET", headers: extraHeaders });
const post = <T = unknown>(path: string, body?: unknown, extraHeaders?: HeadersInit) =>
  apiFetch<T>(path, { method: "POST", body: body ? JSON.stringify(body) : undefined, headers: extraHeaders });
const put = <T = unknown>(path: string, body: unknown) =>
  apiFetch<T>(path, { method: "PUT", body: JSON.stringify(body) });
const del = <T = unknown>(path: string) =>
  apiFetch<T>(path, { method: "DELETE" });

const generateIdempotencyKey = () =>
  `${Date.now()}-${Math.random().toString(36).substring(2, 10)}`;

// ─── FIX #4: Pagination Unwrapper ───────────────────────────
// Backend returns { items, page, size, totalElements, totalPages }
// Frontend expects arrays — this unwraps consistently
const unwrapPaginated = <T = unknown>(data: unknown): T[] => {
  const record = asRecord(data);
  if (!data) return [];
  if (Array.isArray(data)) return data as T[];
  if (Array.isArray(record.items)) return record.items as T[];
  if (Array.isArray(record.content)) return record.content as T[];
  return [];
};

// Keep full page info when needed
const unwrapPaginatedFull = <T = unknown>(data: unknown) => {
  const record = asRecord(data);
  if (!data) return { items: [], page: 0, totalPages: 0, totalElements: 0 };
  if (Array.isArray(data)) return { items: data as T[], page: 0, totalPages: 1, totalElements: data.length };
  return {
    items: (record.items || record.content || []) as T[],
    page: asNumber(record.page ?? record.number, 0),
    totalPages: asNumber(record.totalPages, 1),
    totalElements: asNumber(record.totalElements ?? record.totalCount, 0),
  };
};

// ═══════════════════════════════════════════════════════════════
// AUTH
// ═══════════════════════════════════════════════════════════════
export const authApi = {
  login: async (email: string, password: string): Promise<AuthResponseDto> => {
    const res = await post<AuthResponseDto>("/api/auth/login", { email, password });
    // Store memberId from login response for later use
    if (res?.memberId) setMemberId(res.memberId);
    return res;
  },

  refresh: async (): Promise<AuthResponseDto> => {
    const res = await post<AuthResponseDto>("/api/auth/refresh");
    if (res?.token) setToken(res.token);
    if (res?.memberId) setMemberId(res.memberId);
    return res;
  },

  sendCode: (email: string): Promise<AuthResponseDto> =>
    post("/api/auth/send-code", { email }),

  verifyCode: (email: string, code: string): Promise<AuthResponseDto> =>
    post("/api/auth/verify-code", { email, code }),

  // Backend requires: email, code, password, passwordConfirm
  completeSignup: async ({ email, code, password, passwordConfirm, nickname }: { email: string; code: string; password: string; passwordConfirm?: string; nickname?: string }): Promise<AuthResponseDto> => {
    const res = await post<AuthResponseDto>("/api/auth/complete-signup", {
      email,
      code,
      password,
      passwordConfirm: passwordConfirm || password,
      nickname: nickname || undefined,
    });
    if (res?.memberId) setMemberId(res.memberId);
    return res;
  },

  // Backend expects `googleToken`
  googleLogin: async (googleToken: string): Promise<AuthResponseDto> => {
    const res = await post<AuthResponseDto>("/api/auth/google", { googleToken });
    if (res?.memberId) setMemberId(res.memberId);
    return res;
  },

  googleCompleteRegistration: async (data: ApiRecord): Promise<AuthResponseDto> => {
    const res = await post<AuthResponseDto>("/api/auth/google/complete-registration", data);
    if (res?.memberId) setMemberId(res.memberId);
    return res;
  },

  walletNonce: (walletAddress: string): Promise<ApiRecord> =>
    post("/api/auth/wallet/nonce", { walletAddress }),

  walletLogin: async (payload: { walletAddress: string; nonce: string; message: string; signature: string }): Promise<AuthResponseDto> => {
    const res = await post<AuthResponseDto>("/api/auth/wallet", payload);
    if (res?.memberId) setMemberId(res.memberId);
    return res;
  },
};

// ═══════════════════════════════════════════════════════════════
// QUESTIONS
// ═══════════════════════════════════════════════════════════════
export const questionApi = {
  list: async (params: QueryParams = {}) => {
    const q = toQueryString(params);
    const data = await get(`/api/questions${q ? `?${q}` : ""}`);
    return unwrapPaginated<QuestionDto>(data);
  },
  detail: (id: number | string) => get<QuestionDto>(`/api/questions/${id}`),
  myCreated: async () => {
    const data = await get("/api/questions/me/created");
    return unwrapPaginated<CreatedQuestionItem>(data);
  },
  byStatus: async (status: string) => {
    const data = await get(`/api/questions/status/${status}`);
    return unwrapPaginated(data);
  },
  creditsStatus: () => get("/api/questions/credits/status"),

  // Draft open requires X-Idempotency-Key, returns { draftId, idempotencyKey }
  createDraft: () => {
    const key = generateIdempotencyKey();
    return post("/api/questions/drafts/open", null, { "X-Idempotency-Key": key });
  },

  // Draft submit: X-Idempotency-Key MUST come from createDraft response
  submitDraft: (draftId: number | string, data: ApiRecord, idempotencyKeyFromDraft?: string) => {
    const key = idempotencyKeyFromDraft || generateIdempotencyKey();
    const resolutionRule =
      asString(data.resolutionRule) ||
      asString(data.resolutionSource) ||
      asString(data.resolutionCriteria) ||
      undefined;
    return post(`/api/questions/drafts/${draftId}/submit`, {
      title: asString(data.title),
      description: asString(data.description),
      category: mapCategoryCode(data.category),
      voteWindowType: mapVoteWindowType(data.voteWindowType ?? data.endDate),
      settlementMode: mapSettlementMode(data.settlementMode ?? data.resolutionType),
      resolutionRule,
      resolutionSource: asString(data.resolutionSource) || undefined,
      tags: asStringArray(data.tags),
      creatorSplitInPool: asNumber(data.creatorSplitInPool, 50),
      submitIdempotencyKey: key,
    }, { "X-Idempotency-Key": key });
  },

  cancelDraft: (draftId: number | string) => post(`/api/questions/drafts/${draftId}/cancel`),

  getComments: async (questionId: number | string, params: QueryParams = {}) => {
    const q = toQueryString(params);
    const data = await get(`/api/questions/${questionId}/comments${q ? `?${q}` : ""}`);
    const raw = unwrapPaginated<Record<string, unknown>>(data);
    return raw.map(c => ({
      id: (c.commentId ?? c.id) as number,
      user: ((c.displayName ?? c.username ?? "Anonymous") as string),
      avatar: ((c.avatarUrl ?? "#7C3AED") as string),
      text: ((c.content ?? "") as string),
      time: ((c.createdAt ?? "") as string),
      vote: null,
    })) as CommentItem[];
  },
  postComment: (questionId: number | string, content: string) =>
    post(`/api/questions/${questionId}/comments`, { content }),
  deleteComment: (questionId: number | string, commentId: number | string) =>
    del(`/api/questions/${questionId}/comments/${commentId}`),
};

// ═══════════════════════════════════════════════════════════════
// VOTING
// ═══════════════════════════════════════════════════════════════
export const voteApi = {
  // Backend expects `choice`, not `side`
  vote: (questionId: number | string, choice: VoteChoice) =>
    post("/api/votes", { questionId, choice }),

  status: (questionId: number | string) => get(`/api/votes/status/${questionId}`),

  feed: async (params: QueryParams = {}) => {
    const normalized: QueryParams = { ...params };
    const normalizeWindow = (value: QueryValue) => {
      const raw = String(value ?? "").toUpperCase();
      if (raw === "6H" || raw === "H6") return "H6";
      if (raw === "1D" || raw === "D1") return "D1";
      if (raw === "3D" || raw === "D3") return "D3";
      return value;
    };
    // Backend pagination keys are page/size. Map legacy limit to size.
    if (normalized.limit !== undefined && normalized.size === undefined) {
      normalized.size = normalized.limit;
    }
    if (normalized.page === undefined) normalized.page = 0;
    if (normalized.size === undefined) normalized.size = 100;
    delete normalized.limit;
    if (normalized.timeFilter !== undefined && normalized.window === undefined) {
      normalized.window = normalizeWindow(normalized.timeFilter);
    } else if (normalized.window !== undefined) {
      normalized.window = normalizeWindow(normalized.window);
    }
    delete normalized.timeFilter;

    const q = toQueryString(normalized);
    const data = await get(`/api/votes/feed${q ? `?${q}` : ""}`);
    return unwrapPaginated<QuestionDto>(data);
  },

  dailyRemaining: () => get<{ remainingVotes: number }>("/api/votes/daily-remaining"),

  // VOTE_RESULT(OPINION) 질문 전용 commit/reveal 경로
  // 백엔드 feature flag: app.voting.commit-reveal-enabled=true 일 때만 동작
  commit: (request: { questionId: number | string; commitHash: string }) =>
    post("/api/votes/commit", request),
  reveal: (request: { questionId: number | string; choice: string; salt: string }) =>
    post("/api/votes/reveal", request),
};

// ═══════════════════════════════════════════════════════════════
// MARKET / AMM — FIX #1: swap/simulate params
// ═══════════════════════════════════════════════════════════════
export const marketApi = {
  // Swap execution: backend expects { questionId, action, outcome, usdcIn|sharesIn }
  // action = "BUY" | "SELL", outcome = "YES" | "NO"
  swap: ({ questionId, direction, side, amount, shares }: { questionId: number | string; direction?: string; side?: VoteChoice | string; amount?: number | string; shares?: number | string }) =>
    post("/api/swap", {
      questionId,
      action: (direction || "buy").toUpperCase(),
      outcome: (side || "YES").toUpperCase(),
      usdcIn: (direction || "buy").toLowerCase() === "buy" ? amount : undefined,
      sharesIn: (direction || "buy").toLowerCase() === "sell" ? shares : undefined,
    }),

  pool: (questionId: number | string) => get(`/api/pool/${questionId}`),

  // Simulate: backend expects { questionId, action, outcome, amount }
  simulate: ({ questionId, side, amount, direction }: { questionId: number | string; side?: VoteChoice | string; amount: number | string; direction?: string }) => {
    const action = (direction || "BUY").toUpperCase();
    const params = new URLSearchParams({
      questionId: String(questionId),
      action,
      outcome: (side || "YES").toUpperCase(),
      amount: String(amount),
    });
    return get(`/api/swap/simulate?${params}`);
  },

  priceHistory: (questionId: number | string, params: QueryParams = {}) => {
    const q = toQueryString(params);
    return get(`/api/swap/price-history/${questionId}${q ? `?${q}` : ""}`);
  },
  history: async (questionId: number | string) => {
    const data = await get(`/api/swap/history/${questionId}`);
    return unwrapPaginated(data);
  },
  myHistory: async (questionId: number | string) => {
    const data = await get(`/api/swap/my-history/${questionId}`);
    return unwrapPaginated(data);
  },
  myShares: (questionId: number | string) => get(`/api/swap/my-shares/${questionId}`),
};

// ═══════════════════════════════════════════════════════════════
// PORTFOLIO / SETTLEMENTS
// ═══════════════════════════════════════════════════════════════
export const portfolioApi = {
  summary: () => get("/api/portfolio/summary"),
  positions: async () => {
    const data = await get("/api/portfolio/positions");
    return unwrapPaginated<PositionDto>(data);
  },
  categoryBreakdown: () => get("/api/portfolio/category-breakdown"),
  accuracyTrend: () => get("/api/portfolio/accuracy-trend"),
  settlementHistory: async () => {
    const data = await get("/api/settlements/history/me");
    return unwrapPaginated<SettlementHistoryItem>(data);
  },
};

// ═══════════════════════════════════════════════════════════════
// ACTIVITY
// ═══════════════════════════════════════════════════════════════
export const activityApi = {
  myActivity: async (params: QueryParams = {}) => {
    const q = toQueryString(params);
    const data = await get(`/api/activities/me${q ? `?${q}` : ""}`);
    return unwrapPaginated(data);
  },
  byQuestion: async (questionId: number | string) => {
    const data = await get(`/api/activities/question/${questionId}`);
    return unwrapPaginated(data);
  },
  myByQuestion: async (questionId: number | string) => {
    const data = await get(`/api/activities/me/question/${questionId}`);
    return unwrapPaginated(data);
  },
};

// ═══════════════════════════════════════════════════════════════
// MEMBER / SOCIAL — FIX #4: paginated unwrap for followers/following
// ═══════════════════════════════════════════════════════════════
export const memberApi = {
  me: async () => {
    const data = await get<MemberDto>("/api/members/me");
    // Store memberId on first fetch
    if (data?.memberId !== undefined && data?.memberId !== null) {
      setMemberId(String(data.memberId));
    }
    return data;
  },
  dashboard: () => get<MemberDto>("/api/members/me/dashboard"),
  profile: (userId: number | string) => get(`/api/users/${userId}`),
  updateProfile: (data: ApiRecord) => put("/api/users/me/profile", data),
  uploadAvatar: async (file: File): Promise<{ avatarUrl: string }> => {
    const formData = new FormData();
    formData.append("file", file);
    const token = getToken();
    const res = await fetch(`${API_BASE}/api/users/me/avatar`, {
      method: "POST",
      headers: token ? { Authorization: `Bearer ${token}` } : {},
      body: formData,
    });
    const json = await res.json();
    if (!res.ok || json.success === false) {
      throw new Error(json.error?.message || json.message || `HTTP ${res.status}`);
    }
    return json.data;
  },
  follow: (userId: number | string) => post(`/api/users/${userId}/follow`),
  unfollow: (userId: number | string) => del(`/api/users/${userId}/follow`),
  followers: async (userId: number | string) => {
    const data = await get(`/api/users/${userId}/followers`);
    return unwrapPaginated(data);
  },
  following: async (userId: number | string) => {
    const data = await get(`/api/users/${userId}/following`);
    return unwrapPaginated(data);
  },
  requestWalletLinkNonce: (walletAddress: string): Promise<ApiRecord> =>
    post("/api/members/wallet/nonce", { walletAddress }),
  updateWalletAddress: (payload: { walletAddress: string | null; nonce?: string; message?: string; signature?: string }) =>
    put("/api/members/wallet", payload),
};

// ═══════════════════════════════════════════════════════════════
// PAYMENT / WALLET
// ═══════════════════════════════════════════════════════════════
export const paymentApi = {
  config: () => get<{ receiverWallet: string; usdcContract: string; chainId: number }>("/api/payments/config"),
  verifyDeposit: (txHash: string, amount: number, fromAddress: string) =>
    post("/api/payments/verify-deposit", { txHash, amount, fromAddress }),
  withdraw: (amount: number, walletAddress: string) =>
    post("/api/payments/withdraw", { amount, walletAddress }),
  myTransactions: async (params: QueryParams = {}) => {
    const q = toQueryString(params);
    const data = await get(`/api/transactions/my${q ? `?${q}` : ""}`);
    return unwrapPaginated(data);
  },
};

// ═══════════════════════════════════════════════════════════════
// LEADERBOARD
// ═══════════════════════════════════════════════════════════════
export const leaderboardApi = {
  top: (limit = 50) => get<LeaderboardEntryItem[]>(`/api/leaderboard/top?limit=${limit}`),
  member: (memberId: number | string) => get<{ entry: LeaderboardEntryItem | null; found: boolean }>(`/api/leaderboard/member/${memberId}`),
};

// ═══════════════════════════════════════════════════════════════
// FIX #5: Google OAuth Helper
// Returns googleToken from Google Sign-In SDK
// ═══════════════════════════════════════════════════════════════
export const googleSignIn = async () => {
  const googleClientId =
    process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID ||
    process.env.REACT_APP_GOOGLE_CLIENT_ID ||
    "";

  return new Promise((resolve, reject) => {
    if (!googleClientId) {
      reject(new Error("Google Client ID is missing. Set NEXT_PUBLIC_GOOGLE_CLIENT_ID."));
      return;
    }
    if (!window.google?.accounts?.id) {
      reject(new Error("Google Sign-In SDK not loaded."));
      return;
    }
    window.google.accounts.id.initialize({
      client_id: googleClientId,
      callback: (response) => {
        if (response.credential) {
          resolve(response.credential); // This is the googleToken
        } else {
          reject(new Error("Google sign-in failed"));
        }
      },
    });
    // Trigger One Tap or popup
    window.google.accounts.id.prompt((notification) => {
      if (notification.isNotDisplayed() || notification.isSkippedMoment()) {
        // Fallback: render button approach
        reject(new Error("Google sign-in was dismissed"));
      }
    });
  });
};

// ═══════════════════════════════════════════════════════════════
// RESPONSE MAPPERS — Match actual backend Dto field names
// ═══════════════════════════════════════════════════════════════

export const mapQuestionToMarket = (input: QuestionDto | unknown): MarketItem => {
  const q = asRecord(input) as QuestionDto & ApiRecord;
  const parsedTags = asStringArray(q.tags);
  const fallbackTags = parseJsonStringArray(q.tagsJson);
  const tags = parsedTags.length > 0 ? parsedTags : fallbackTags;
  const volumeRaw = q.totalVolume ?? q.totalBetPool;
  const yesTag = tags.find((tag) => /^(yes|y)$/i.test(tag));
  const noTag = tags.find((tag) => /^(no|n)$/i.test(tag));
  return {
    id: (q.id ?? q.questionId ?? 0) as number | string,
    title: asString(q.title),
    category: asString(q.category),
    volume: volumeRaw ? formatVolume(volumeRaw) : "0",
    voters: asNumber(q.totalVoteCount ?? q.totalVotes, 0),
    bettors: asNumber(q.bettorCount ?? q.totalBettors, 0),
    yesPrice: asNumber(q.yesPrice, 0.5),
    change24h: asNumber(q.priceChange24h, 0),
    timeLeft: asString(q.timeRemaining ?? q.timeLeft),
    bettingEndAt: asString(q.bettingEndAt),
    topDemographic: asString(q.topDemographic),
    sentiment: asString(q.sentiment, "neutral"),
    comments: asNumber(q.commentCount, 0),
    creator: asString(q.creatorNickname ?? q.creatorUsername),
    description: asString(q.description),
    status: asString(q.status),
    phase: asString((q as ApiRecord).phase),
    matchTime: asString((q as ApiRecord).matchTime),
    submitter: asString(q.creatorNickname ?? q.creatorUsername),
    avatar: asString(q.creatorAvatar, "#7C3AED"),
    tags,
    creatorSplitInPool: asNumber(q.creatorSplitInPool, 50),
    platformFeeShare: asNumberLike(q.platformFeeShare, 0.2),
    creatorFeeShare: asNumberLike(q.creatorFeeShare, 0),
    voterFeeShare: asNumberLike(q.voterFeeShare, 0),
    matchId: (q.matchId ?? undefined) as number | string | undefined,
    sport: asString(tags[0], asString(q.category) === "LIVE" ? "football" : ""),
    league: asString(tags[1]),
    homeTeam: yesTag,
    awayTeam: noTag,
    voteWindowType: asString(q.voteWindowType),
    settlementMode: resolveSettlementMode(q),
    votingPhase: asString(q.votingPhase) || null,
    voteVisibility: asString(q.voteVisibility) || null,
    hiddenVoteResults: shouldHideVoteBreakdown(q),
    totalVotes: asNumber(q.totalVoteCount ?? q.totalVotes, asNumber(q.yesVoteCount ?? q.yesVotes, 0) + asNumber(q.noVoteCount ?? q.noVotes, 0)),
  };
};

export const mapToVoteQuestion = (input: QuestionDto | unknown): VoteQuestion => {
  const q = asRecord(input) as QuestionDto & ApiRecord;
  const submittedAtRaw = asString(q.createdAtRelative ?? q.submittedAt ?? q.submitted);
  const submittedAtTs = submittedAtRaw ? new Date(submittedAtRaw).getTime() : NaN;
  const derivedAgeHours = Number.isNaN(submittedAtTs)
    ? 0
    : Math.max(0, (Date.now() - submittedAtTs) / 3600000);
  return {
    id: (q.questionId ?? q.id ?? 0) as number | string,
    title: asString(q.title),
    category: asString(q.category),
    sub: asString(q.subcategory ?? q.sub),
    submitterId: (q.submitterId ?? q.creatorMemberId) as number | string | undefined,
    yesVotes: asNumber(q.yesVoteCount ?? q.yesVotes, 0),
    noVotes: asNumber(q.noVoteCount ?? q.noVotes, 0),
    totalVotes: asNumber(q.totalVoteCount ?? q.totalVotes, asNumber(q.yesVoteCount ?? q.yesVotes, 0) + asNumber(q.noVoteCount ?? q.noVotes, 0)),
    submitter: asString(q.submitterDisplayName ?? q.submitterUsername ?? q.creatorNickname ?? q.creatorUsername ?? q.submitter),
    avatar: asString(q.submitterAvatarUrl ?? q.creatorAvatar ?? q.avatar, "#7C3AED"),
    following: Boolean(q.isFollowingSubmitter ?? q.isFollowing ?? q.following ?? false),
    submitted: submittedAtRaw,
    votingEndAt: asString(q.votingEndAt),
    age: asNumber(q.ageHours ?? q.age, derivedAgeHours),
    tags: asStringArray(q.tags),
    likes: asNumber(q.likeCount ?? q.likes, 0),
    commentCount: asNumber(q.commentCount, 0),
    description: asString(q.description),
    yesPrice: asNumber(q.yesPrice, 0.5),
    settlementMode: resolveSettlementMode(q),
    votingPhase: asString(q.votingPhase) || null,
    voteVisibility: asString(q.voteVisibility) || null,
    hiddenVoteResults: shouldHideVoteBreakdown(q),
  };
};

export const mapToPosition = (input: PositionDto | unknown): PositionItem => {
  const p = asRecord(input) as PositionDto & ApiRecord;
  return {
    id: (p.questionId ?? p.id ?? 0) as number | string,
    title: asString(p.questionTitle ?? p.title),
    side: asString(p.side ?? p.outcome, "YES") as VoteChoice,
    shares: asNumber(p.shareCount ?? p.shares, 0),
    avgPrice: asNumber(p.averageEntryPrice ?? p.averagePrice ?? p.avgPrice, 0),
    currentPrice: asNumber(p.currentPrice, 0),
    invested: asNumber(p.investedAmount ?? p.totalInvested ?? p.invested, 0),
  };
};

export const mapToUser = (input: MemberDto | unknown): UserProfile => {
  const m = asRecord(input) as MemberDto & ApiRecord;
  const nickname = asString(m.nickname);
  return {
    name: asString(m.nickname ?? m.displayName),
    username: asString(m.nickname ?? m.username),
    email: asString(m.email),
    avatar: asString(m.avatarUrl, nickname ? nickname.substring(0, 2).toUpperCase() : "??"),
    bio: asString(m.bio),
    balance: asNumber(m.balance, 0),
    totalProfit: asNumber(m.totalPnl ?? m.totalProfit, 0),
    totalBets: asNumber(m.betCount ?? m.totalBets, 0),
    winRate: asNumber(m.winRate, 0),
    voteCredits: asNumber(m.remainingCredits ?? m.remainingVotes, 0),
    memberSince: asString(m.createdAt),
    followers: asNumber(m.followerCount ?? (m as ApiRecord).followers, 0),
    following: asNumber(m.followingCount ?? (m as ApiRecord).following, 0),
    questionsCreated: asNumber(m.questionCreatedCount ?? m.questionCount, 0),
    totalVotes: asNumber(m.voteCount ?? m.totalVotes, 0),
    creatorEarnings: asNumber(m.creatorEarnings, 0),
  };
};

// ─── Field mapping helpers ──────────────────────────────────
const mapSettlementMode = (resolutionType: unknown): SettlementMode => {
  switch (resolutionType) {
    case "auto": return "OBJECTIVE_RULE";
    case "manual": return "VOTE_RESULT";
    case "OBJECTIVE": return "OBJECTIVE_RULE";
    case "OBJECTIVE_RULE": return "OBJECTIVE_RULE";
    case "SUBJECTIVE": return "VOTE_RESULT";
    case "VOTE_RESULT": return "VOTE_RESULT";
    default: return "OBJECTIVE_RULE";
  }
};

const isVoteResultQuestion = (q: QuestionDto): q is VoteResultQuestionDto => {
  if (q.settlementMode === "VOTE_RESULT") return true;
  if (typeof q.voteResultSettlement === "boolean") return q.voteResultSettlement;
  return false;
};

const resolveSettlementMode = (q: ApiRecord): SettlementMode => {
  const dto = q as unknown as QuestionDto;
  if (dto.settlementMode) return mapSettlementMode(dto.settlementMode);
  return isVoteResultQuestion(dto) ? "VOTE_RESULT" : "OBJECTIVE_RULE";
};

const shouldHideVoteBreakdown = (q: ApiRecord): boolean => {
  if (resolveSettlementMode(q) !== "VOTE_RESULT") return false;
  if (q?.voteVisibility) return q.voteVisibility !== "REVEALED";
  if (q?.votingPhase) return q.votingPhase !== "VOTING_REVEAL_CLOSED";
  if (q?.bettingEndAt) {
    const endTs = new Date(String(q.bettingEndAt)).getTime();
    if (!Number.isNaN(endTs)) return Date.now() < endTs;
  }
  return true;
};

const mapVoteWindowType = (input: unknown): "H6" | "D1" | "D3" => {
  if (!input) return "D1";
  if (typeof input === "string" && ["H6", "D1", "D3"].includes(input)) return input as "H6" | "D1" | "D3";
  const hours = (new Date(String(input)).getTime() - Date.now()) / 3600000;
  if (hours <= 6) return "H6";
  if (hours <= 24) return "D1";
  return "D3";
};

const formatVolume = (num: unknown): string => {
  const n = asNumber(num, 0);
  if (n >= 1_000_000) return (n / 1_000_000).toFixed(1) + "M";
  if (n >= 1_000) return (n / 1_000).toFixed(1) + "K";
  return n.toString();
};
