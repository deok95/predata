import React, { useState, useEffect } from "react";
import { COLORS } from "../theme";
import { useMyProfile, usePositions, useSettlementHistory, useMyActivity } from "../hooks/useApi";
import { questionApi, activityApi, isLoggedIn, leaderboardApi, memberApi, paymentApi, getMemberId } from "../services/api";
import { useWallet } from "../hooks/useWallet";
import type {
  PositionItem,
  ActivityItem,
  SettlementHistoryItem,
  CreatedQuestionItem,
  VoteActivityItem,
} from "../types/domain";

type MyPageViewProps = {
  onBack?: () => void;
  onBet?: (...args: unknown[]) => void;
  onNavigate?: (page: string) => void;
  isDark?: boolean;
  toggleTheme?: () => void;
};

function MyPageView({ onBack = () => {}, onBet, onNavigate, isDark, toggleTheme = () => {} }: MyPageViewProps = {}) {
  const [tab, setTab] = useState("activity");
  const [depositModal, setDepositModal] = useState(false);
  const [depositAmount, setDepositAmount] = useState("");
  const [withdrawModal, setWithdrawModal] = useState(false);
  const [withdrawAmount, setWithdrawAmount] = useState("");
  const [txStatus, setTxStatus] = useState<"idle" | "pending" | "verifying" | "success" | "error">("idle");
  const [txMessage, setTxMessage] = useState("");
  const [moreMenu, setMoreMenu] = useState(false);
  const wallet = useWallet();
  const [paymentConfig, setPaymentConfig] = useState<{ receiverWallet: string; usdcContract: string; chainId: number } | null>(null);
  const [linkedWalletAddress, setLinkedWalletAddress] = useState<string | null>(null);
  const [activityFeed, setActivityFeed] = useState<ActivityItem[]>([]);
  const [followingList, setFollowingList] = useState<Array<Record<string, unknown>>>([]);
  const [followersList, setFollowersList] = useState<Array<Record<string, unknown>>>([]);
  const [followModal, setFollowModal] = useState<null | "followers" | "following">(null);
  const [socialListError, setSocialListError] = useState("");

  useEffect(() => {
    paymentApi.config().then((res: any) => {
      const d = res?.data ?? res;
      if (d?.receiverWallet) setPaymentConfig(d);
    }).catch(() => {});
  }, []);

  // API data
  const { user: MOCK_USER, loading: userLoading, error: userError, refresh: refreshProfile } = useMyProfile();
  const [avatarUploading, setAvatarUploading] = useState(false);
  const { positions: MOCK_POSITIONS, loading: posLoading } = usePositions();
  const { data: MOCK_HISTORY, loading: histLoading } = useSettlementHistory();
  const { data: activityData, loading: actLoading } = useMyActivity();
  const [MOCK_CREATED_QUESTIONS, setCreatedQuestions] = useState<CreatedQuestionItem[]>([]);
  const [MOCK_VOTE_ACTIVITY, setVoteActivity] = useState<VoteActivityItem[]>([]);
  const [myRank, setMyRank] = useState<{ rank: number; tier: string; accuracy: number } | null>(null);
  const MOCK_ACTIVITY_FEED: ActivityItem[] = activityFeed;
  const historyList: SettlementHistoryItem[] = Array.isArray(MOCK_HISTORY) ? MOCK_HISTORY : [];

  const asString = (value: unknown, fallback = "") => (typeof value === "string" ? value : fallback);
  const asNumber = (value: unknown, fallback = 0) => {
    if (typeof value === "number") return value;
    if (typeof value === "string" && value.trim() !== "") {
      const n = Number(value);
      return Number.isFinite(n) ? n : fallback;
    }
    return fallback;
  };
  const toRelativeTime = (raw: string) => {
    const ts = new Date(raw).getTime();
    if (Number.isNaN(ts)) return raw;
    const diffMs = Date.now() - ts;
    if (diffMs < 60_000) return "just now";
    const mins = Math.floor(diffMs / 60_000);
    if (mins < 60) return `${mins}m ago`;
    const hours = Math.floor(mins / 60);
    if (hours < 24) return `${hours}h ago`;
    const days = Math.floor(hours / 24);
    return `${days}d ago`;
  };

  const loadFollowLists = async (memberIdInput?: string | number | null) => {
    const resolvedMemberId = memberIdInput ?? getMemberId();
    if (!resolvedMemberId) return;

    const [followingResult, followersResult] = await Promise.allSettled([
      memberApi.following(resolvedMemberId),
      memberApi.followers(resolvedMemberId),
    ]);

    if (followingResult.status === "fulfilled") {
      setFollowingList(Array.isArray(followingResult.value) ? followingResult.value as Array<Record<string, unknown>> : []);
    } else {
      setFollowingList([]);
    }

    if (followersResult.status === "fulfilled") {
      setFollowersList(Array.isArray(followersResult.value) ? followersResult.value as Array<Record<string, unknown>> : []);
    } else {
      setFollowersList([]);
    }

    if (followingResult.status === "rejected" && followersResult.status === "rejected") {
      setSocialListError("Failed to load follow lists.");
    } else {
      setSocialListError("");
    }
  };

  useEffect(() => {
    if (!isLoggedIn()) {
      setMyRank(null);
      setCreatedQuestions([]);
      setLinkedWalletAddress(null);
      return;
    }

    let active = true;
    (async () => {
      try {
        const me = await memberApi.me() as Record<string, unknown>;
        if (!active) return;

        const memberId = me.memberId ?? me.id;
        const walletAddress = typeof me.walletAddress === "string" ? me.walletAddress : null;
        setLinkedWalletAddress(walletAddress);

        const created = await questionApi.myCreated();
        if (active) {
          const mapped = (Array.isArray(created) ? created : []).map((item: unknown) => {
            const row = (item && typeof item === "object") ? item as Record<string, unknown> : {};
            return {
              id: row.id ?? row.questionId ?? 0,
              title: asString(row.title),
              category: asString(row.category, "GENERAL"),
              status: asString(row.status, "VOTING"),
              totalVotes: asNumber(row.totalVotes, 0),
              earnings: asNumber(row.earnings, 0),
            } as CreatedQuestionItem;
          });
          setCreatedQuestions(mapped);
        }

        const resolvedMemberId = memberId ?? getMemberId();
        if (resolvedMemberId) {
          await loadFollowLists(resolvedMemberId as string | number);
        }

        if (!memberId) return;
        const res = await leaderboardApi.member(memberId as string | number);
        const entry = res?.entry;
        if (!active || !entry) return;
        setMyRank({
          rank: entry.rank,
          tier: entry.tier,
          accuracy: entry.accuracyPercentage,
        });
      } catch {
        if (active) {
          setMyRank(null);
          setCreatedQuestions([]);
          setFollowingList([]);
          setFollowersList([]);
          setLinkedWalletAddress(null);
        }
      }
    })();

    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    if (!followModal) return;
    void loadFollowLists();
  }, [followModal]);

  useEffect(() => {
    let active = true;
    const rawItems = Array.isArray(activityData) ? activityData : [];
    if (rawItems.length === 0) {
      setActivityFeed([]);
      setVoteActivity([]);
      return;
    }

    (async () => {
      const questionIds = Array.from(new Set(rawItems.map((item: unknown) => {
        const row = (item && typeof item === "object") ? item as Record<string, unknown> : {};
        return asNumber(row.questionId, 0);
      }).filter((id) => id > 0)));

      const titleEntries = await Promise.all(
        questionIds.map(async (qid) => {
          try {
            const q = await questionApi.detail(qid);
            return [qid, asString((q as Record<string, unknown>).title, `Question #${qid}`)] as const;
          } catch {
            return [qid, `Question #${qid}`] as const;
          }
        })
      );
      if (!active) return;
      const titleMap = new Map<number, string>(titleEntries);

      const mappedActivity: ActivityItem[] = rawItems.map((item: unknown) => {
        const row = (item && typeof item === "object") ? item as Record<string, unknown> : {};
        const typeRaw = asString(row.activityType || row.type).toUpperCase();
        const qid = asNumber(row.questionId, 0);
        const choice = asString(row.choice || row.vote).toUpperCase();
        const amount = asNumber(row.amount, 0);
        const createdAt = asString(row.createdAt || row.time);
        const marketTitle = titleMap.get(qid) || `Question #${qid}`;
        if (typeRaw === "BET") {
          return {
            id: asString(row.id, `${qid}-${createdAt}`),
            type: "bet",
            text: `Placed ${choice || "BET"} bet`,
            market: marketTitle,
            detail: amount > 0 ? `$${amount.toLocaleString()}` : "",
            time: toRelativeTime(createdAt),
            color: COLORS.accentLight,
          };
        }
        if (typeRaw === "BET_SELL") {
          return {
            id: asString(row.id, `${qid}-${createdAt}`),
            type: "pnl",
            text: "Closed position",
            market: marketTitle,
            detail: amount > 0 ? `$${amount.toLocaleString()}` : "",
            time: toRelativeTime(createdAt),
            color: COLORS.gold,
          };
        }
        return {
          id: asString(row.id, `${qid}-${createdAt}`),
          type: "vote",
          text: `Voted ${choice || "-"}`,
          market: marketTitle,
          detail: "",
          time: toRelativeTime(createdAt),
          color: choice === "YES" ? COLORS.green : COLORS.red,
        };
      });

      const mappedVotes: VoteActivityItem[] = rawItems
        .map((item: unknown) => {
          const row = (item && typeof item === "object") ? item as Record<string, unknown> : {};
          const typeRaw = asString(row.activityType || row.type).toUpperCase();
          if (typeRaw !== "VOTE") return null;
          const qid = asNumber(row.questionId, 0);
          const choice = asString(row.choice || row.vote).toUpperCase();
          const createdAt = asString(row.createdAt || row.time);
          return {
            id: asNumber(row.id, qid),
            category: asString(row.category, "VOTE"),
            date: createdAt ? new Date(createdAt).toLocaleString() : "",
            title: titleMap.get(qid) || `Question #${qid}`,
            vote: choice === "NO" ? "NO" : "YES",
            totalVotes: asNumber(row.totalVotes, 0),
            status: "active",
          } as VoteActivityItem;
        })
        .filter((x): x is VoteActivityItem => Boolean(x));

      setActivityFeed(mappedActivity);
      setVoteActivity(mappedVotes);
    })();

    return () => {
      active = false;
    };
  }, [activityData]);

  // Loading state
  if (userLoading) {
    return <div style={{ minHeight: "100vh", background: COLORS.bg, display: "flex", alignItems: "center", justifyContent: "center", color: COLORS.textMuted }}>Loading...</div>;
  }

  if (!MOCK_USER) {
    return (
      <div style={{ minHeight: "100vh", background: COLORS.bg, display: "flex", alignItems: "center", justifyContent: "center", padding: 24 }}>
        <div style={{
          width: "100%", maxWidth: 420, textAlign: "center",
          background: COLORS.surface, border: `1px solid ${COLORS.border}`, borderRadius: 14, padding: 20,
        }}>
          <div style={{ fontSize: 16, fontWeight: 700, color: COLORS.text, marginBottom: 8 }}>Failed to load My Page</div>
          <div style={{ fontSize: 13, color: COLORS.textDim, marginBottom: 14 }}>
            {userError || "Session may be expired. Please log in again."}
          </div>
          <button
            onClick={() => onNavigate && onNavigate("home")}
            style={{
              padding: "9px 14px", borderRadius: 8, border: "none", cursor: "pointer",
              background: `linear-gradient(135deg, ${COLORS.accent}, #5B21B6)`, color: "white",
              fontSize: 13, fontWeight: 600,
            }}
          >
            Go Home
          </button>
        </div>
      </div>
    );
  }

  const totalValue = (MOCK_POSITIONS || []).reduce((sum: number, p: PositionItem) => sum + p.shares * p.currentPrice, 0);
  const totalInvested = (MOCK_POSITIONS || []).reduce((sum: number, p: PositionItem) => sum + p.invested, 0);
  const unrealizedPnl = totalValue - totalInvested;
  const normalizeWallet = (value: string) => value.trim().toLowerCase();

  const ensureWalletLinked = async (walletAddress: string) => {
    if (!isLoggedIn()) throw new Error("Please log in first.");
    if (!window.ethereum) throw new Error("MetaMask is not installed.");

    if (linkedWalletAddress && normalizeWallet(linkedWalletAddress) === normalizeWallet(walletAddress)) {
      return;
    }

    setTxStatus("pending");
    setTxMessage("Verifying wallet ownership...");

    const noncePayload = await memberApi.requestWalletLinkNonce(walletAddress);
    const nonce = typeof noncePayload?.nonce === "string" ? noncePayload.nonce : "";
    const message = typeof noncePayload?.message === "string" ? noncePayload.message : "";
    if (!nonce || !message) throw new Error("Failed to prepare wallet verification challenge.");

    const signature = await window.ethereum.request({
      method: "personal_sign",
      params: [message, walletAddress],
    });

    await memberApi.updateWalletAddress({ walletAddress, nonce, message, signature });
    setLinkedWalletAddress(walletAddress);
  };

  return (
    <div style={{ minHeight: "100vh", background: COLORS.bg }}>
      {/* Top bar */}
      <div className="mobile-only" style={{
        position: "sticky", top: 0, zIndex: 50,
        background: `${COLORS.bg}f2`, backdropFilter: "blur(12px)",
        borderBottom: `1px solid ${COLORS.border}`,
        padding: "12px 24px",
      }}>
        <div style={{ maxWidth: 900, margin: "0 auto", display: "flex", alignItems: "center" }}>
          <button onClick={onBack} style={{
            background: "none", border: "none", cursor: "pointer",
            color: COLORS.textMuted, display: "flex", alignItems: "center", gap: 6,
            fontSize: 14, fontWeight: 500, fontFamily: "'DM Sans', sans-serif",
          }}
          onMouseEnter={e => e.currentTarget.style.color = COLORS.text}
          onMouseLeave={e => e.currentTarget.style.color = COLORS.textMuted}
          >
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M19 12H5"/><polyline points="12 19 5 12 12 5"/></svg>
          </button>
          <span style={{ fontSize: 16, fontWeight: 600, marginLeft: 8 }}>My Page</span>
        </div>
      </div>

      <div style={{ maxWidth: 900, margin: "0 auto", padding: "20px 16px" }}>

        {/* Profile Card */}
        <div style={{
          background: COLORS.surface, border: `1px solid ${COLORS.border}`,
          borderRadius: 16, padding: "24px 20px", marginBottom: 16,
        }}>
          <div style={{ display: "flex", gap: 16, marginBottom: 16 }}>
            {/* 아바타: 클릭 시 파일 업로드 */}
            <div style={{ position: "relative", flexShrink: 0 }}>
              <input
                id="avatar-upload-input"
                type="file"
                accept="image/jpeg,image/png,image/webp,image/gif"
                style={{ display: "none" }}
                onChange={async (e) => {
                  const file = e.target.files?.[0];
                  if (!file) return;
                  setAvatarUploading(true);
                  try {
                    await memberApi.uploadAvatar(file);
                    await refreshProfile();
                  } catch (err) {
                    alert(err instanceof Error ? err.message : "Upload failed");
                  } finally {
                    setAvatarUploading(false);
                    e.target.value = "";
                  }
                }}
              />
              <div
                onClick={() => document.getElementById("avatar-upload-input")?.click()}
                style={{
                  width: 64, height: 64, borderRadius: 16,
                  background: MOCK_USER.avatar?.startsWith("http")
                    ? "transparent"
                    : `linear-gradient(135deg, ${COLORS.accent}, #5B21B6)`,
                  display: "flex", alignItems: "center", justifyContent: "center",
                  fontSize: 22, fontWeight: 800, color: "white",
                  boxShadow: "0 4px 20px rgba(124,58,237,0.3)",
                  cursor: "pointer", overflow: "hidden", position: "relative",
                  opacity: avatarUploading ? 0.6 : 1,
                }}
              >
                {MOCK_USER.avatar?.startsWith("http") ? (
                  <img
                    src={MOCK_USER.avatar}
                    alt="avatar"
                    style={{ width: "100%", height: "100%", objectFit: "cover" }}
                  />
                ) : (
                  MOCK_USER.avatar
                )}
                {/* 호버 오버레이 */}
                <div style={{
                  position: "absolute", inset: 0, background: "rgba(0,0,0,0.45)",
                  display: "flex", alignItems: "center", justifyContent: "center",
                  fontSize: 11, color: "white", fontWeight: 600, opacity: 0,
                  transition: "opacity 0.15s",
                }}
                  onMouseEnter={e => (e.currentTarget.style.opacity = "1")}
                  onMouseLeave={e => (e.currentTarget.style.opacity = "0")}
                >
                  {avatarUploading ? "..." : "Edit"}
                </div>
              </div>
            </div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ display: "flex", alignItems: "center", gap: 8, flexWrap: "wrap" }}>
                <span style={{ fontSize: 20, fontWeight: 700, fontFamily: "'Outfit', sans-serif" }}>{MOCK_USER.name}</span>
                <span style={{ fontSize: 13, color: COLORS.textDim }}>@{MOCK_USER.username}</span>
              </div>
              <div style={{ fontSize: 13, color: COLORS.textMuted, marginTop: 4, lineHeight: 1.5 }}>{MOCK_USER.bio}</div>
              <div style={{ fontSize: 11, color: COLORS.textDim, marginTop: 6 }}>Member since {MOCK_USER.memberSince} \u00b7 {MOCK_USER.voteCredits} vote credits</div>
            </div>
          </div>

          <div style={{ display: "flex", gap: 24, marginBottom: 16, paddingBottom: 16, borderBottom: `1px solid ${COLORS.border}` }}>
            {[
              { label: "Followers", value: MOCK_USER.followers, onClick: () => setFollowModal("followers") },
              { label: "Following", value: MOCK_USER.following, onClick: () => setFollowModal("following") },
              { label: "Questions", value: MOCK_USER.questionsCreated },
              { label: "Votes", value: MOCK_USER.totalVotes },
            ].map((s, i) => (
              <div key={i} style={{ cursor: "pointer" }} onClick={s.onClick}>
                <div style={{ fontSize: 17, fontWeight: 700, fontFamily: "'JetBrains Mono', monospace" }}>{s.value}</div>
                <div style={{ fontSize: 11, color: COLORS.textDim }}>{s.label}</div>
              </div>
            ))}
          </div>

          <div style={{ display: "flex", gap: 8 }}>
            <button style={{
              flex: 1, padding: "9px 0", borderRadius: 8,
              background: "rgba(255,255,255,0.04)", border: `1px solid ${COLORS.border}`,
              color: COLORS.textMuted, cursor: "pointer", fontSize: 13, fontWeight: 500,
            }}>Edit Profile</button>
            <div style={{ position: "relative" }}>
              <button onClick={() => setMoreMenu(!moreMenu)} style={{
                padding: "9px 14px", borderRadius: 8,
                background: "rgba(255,255,255,0.04)", border: `1px solid ${COLORS.border}`,
                color: COLORS.textMuted, cursor: "pointer", fontSize: 16, fontWeight: 700,
                display: "flex", alignItems: "center",
              }}>⋮</button>
              {moreMenu && (<>
                <div onClick={() => setMoreMenu(false)} style={{ position: "fixed", inset: 0, zIndex: 199 }} />
                <div style={{
                  position: "absolute", top: "100%", right: 0, marginTop: 6, zIndex: 200,
                  background: COLORS.surface, border: `1px solid ${COLORS.border}`,
                  borderRadius: 12, padding: 6, minWidth: 220,
                  boxShadow: "0 8px 32px rgba(0,0,0,0.4)",
                  animation: "fadeUp 0.15s ease",
                }}>
                  {/* Theme toggle */}
                  <div onClick={toggleTheme} style={{
                    display: "flex", alignItems: "center", gap: 10, padding: "10px 12px",
                    borderRadius: 8, cursor: "pointer", transition: "background 0.15s",
                  }}
                  onMouseEnter={e => e.currentTarget.style.background = COLORS.surfaceHover}
                  onMouseLeave={e => e.currentTarget.style.background = "transparent"}
                  >
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke={COLORS.textMuted} strokeWidth="2" strokeLinecap="round">
                      {isDark ? <><circle cx="12" cy="12" r="5"/><path d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42"/></> : <path d="M21 12.79A9 9 0 1111.21 3a7 7 0 009.79 9.79z"/>}
                    </svg>
                    <span style={{ fontSize: 13, color: COLORS.text }}>{isDark ? "Light Mode" : "Dark Mode"}</span>
                  </div>
                  <div style={{ height: 1, background: COLORS.border, margin: "4px 0" }} />
                  {/* Links */}
                  {[
                    { label: "Q&A / FAQ", icon: <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke={COLORS.textMuted} strokeWidth="2" strokeLinecap="round"><circle cx="12" cy="12" r="10"/><path d="M9.09 9a3 3 0 015.83 1c0 2-3 3-3 3"/><path d="M12 17h.01"/></svg> },
                    { label: "Terms of Service", icon: <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke={COLORS.textMuted} strokeWidth="2" strokeLinecap="round"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/></svg> },
                    { label: "Privacy Policy", icon: <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke={COLORS.textMuted} strokeWidth="2" strokeLinecap="round"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg> },
                  ].map((item, i) => (
                    <div key={i} style={{
                      display: "flex", alignItems: "center", gap: 10, padding: "10px 12px",
                      borderRadius: 8, cursor: "pointer", transition: "background 0.15s",
                    }}
                    onMouseEnter={e => e.currentTarget.style.background = COLORS.surfaceHover}
                    onMouseLeave={e => e.currentTarget.style.background = "transparent"}
                    >
                      {item.icon}
                      <span style={{ fontSize: 13, color: COLORS.text }}>{item.label}</span>
                    </div>
                  ))}
                  <div style={{ height: 1, background: COLORS.border, margin: "4px 0" }} />
                  {/* Social links */}
                  <div style={{ display: "flex", justifyContent: "center", gap: 12, padding: "10px 8px" }}>
                    {/* X */}
                    <a href="#" style={{ color: COLORS.textDim, display: "flex" }}>
                      <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M18.244 2.25h3.308l-7.227 8.26 8.502 11.24H16.17l-5.214-6.817L4.99 21.75H1.68l7.73-8.835L1.254 2.25H8.08l4.713 6.231zm-1.161 17.52h1.833L7.084 4.126H5.117z"/></svg>
                    </a>
                    {/* Discord */}
                    <a href="#" style={{ color: COLORS.textDim, display: "flex" }}>
                      <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M20.317 4.37a19.791 19.791 0 00-4.885-1.515.074.074 0 00-.079.037c-.21.375-.444.864-.608 1.25a18.27 18.27 0 00-5.487 0 12.64 12.64 0 00-.617-1.25.077.077 0 00-.079-.037A19.736 19.736 0 003.677 4.37a.07.07 0 00-.032.027C.533 9.046-.32 13.58.099 18.057a.082.082 0 00.031.057 19.9 19.9 0 005.993 3.03.078.078 0 00.084-.028c.462-.63.874-1.295 1.226-1.994a.076.076 0 00-.041-.106 13.107 13.107 0 01-1.872-.892.077.077 0 01-.008-.128 10.2 10.2 0 00.372-.292.074.074 0 01.077-.01c3.928 1.793 8.18 1.793 12.062 0a.074.074 0 01.078.01c.12.098.246.198.373.292a.077.077 0 01-.006.127 12.299 12.299 0 01-1.873.892.077.077 0 00-.041.107c.36.698.772 1.362 1.225 1.993a.076.076 0 00.084.028 19.839 19.839 0 006.002-3.03.077.077 0 00.032-.054c.5-5.177-.838-9.674-3.549-13.66a.061.061 0 00-.031-.03z"/></svg>
                    </a>
                    {/* Instagram */}
                    <a href="#" style={{ color: COLORS.textDim, display: "flex" }}>
                      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round"><rect x="2" y="2" width="20" height="20" rx="5"/><circle cx="12" cy="12" r="5"/><circle cx="17.5" cy="6.5" r="1.5" fill="currentColor" stroke="none"/></svg>
                    </a>
                    {/* TikTok */}
                    <a href="#" style={{ color: COLORS.textDim, display: "flex" }}>
                      <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M19.59 6.69a4.83 4.83 0 01-3.77-4.25V2h-3.45v13.67a2.89 2.89 0 01-2.88 2.5 2.89 2.89 0 01-2.89-2.89 2.89 2.89 0 012.89-2.89c.28 0 .54.04.79.1v-3.5a6.37 6.37 0 00-.79-.05A6.34 6.34 0 003.15 15.2a6.34 6.34 0 0010.86 4.43v-7.15a8.16 8.16 0 005.58 2.17V11.2a4.85 4.85 0 01-2.83-.9v5.18h1.84V6.69h-.01z"/></svg>
                    </a>
                  </div>
                </div>
              </>)}
            </div>
          </div>
        </div>

        {/* Balance Card */}
        <div style={{
          background: `linear-gradient(135deg, rgba(124,58,237,0.08), rgba(91,33,182,0.04))`,
          border: `1px solid rgba(124,58,237,0.2)`,
          borderRadius: 16, padding: "20px", marginBottom: 16,
        }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", flexWrap: "wrap", gap: 12 }}>
            <div>
              <div style={{ fontSize: 12, color: COLORS.textDim, marginBottom: 4 }}>Balance</div>
              <div style={{ fontSize: 28, fontWeight: 700, fontFamily: "'JetBrains Mono', monospace" }}>${MOCK_USER.balance.toLocaleString()}</div>
            </div>
            <div style={{ display: "flex", gap: 8 }}>
              <button onClick={() => { setDepositModal(true); setTxStatus("idle"); setTxMessage(""); }} style={{
                padding: "9px 20px", borderRadius: 8,
                background: `linear-gradient(135deg, ${COLORS.accent}, #5B21B6)`,
                border: "none", color: "white", cursor: "pointer",
                fontSize: 13, fontWeight: 600,
              }}>Deposit</button>
              <button onClick={() => { setWithdrawModal(true); setTxStatus("idle"); setTxMessage(""); }} style={{
                padding: "9px 20px", borderRadius: 8,
                background: "rgba(255,255,255,0.04)", border: `1px solid ${COLORS.border}`,
                color: COLORS.textMuted, cursor: "pointer", fontSize: 13, fontWeight: 600,
              }}>Withdraw</button>
            </div>
          </div>
          <div style={{ display: "flex", gap: 20, marginTop: 16, paddingTop: 14, borderTop: "1px solid rgba(124,58,237,0.15)", flexWrap: "wrap" }}>
            {[
              { label: "Total Profit", value: `+$${MOCK_USER.totalProfit.toFixed(2)}`, color: COLORS.green },
              { label: "Creator Earnings", value: `+$${MOCK_USER.creatorEarnings.toFixed(2)}`, color: COLORS.accentLight },
              { label: "Win Rate", value: `${MOCK_USER.winRate}%`, color: COLORS.gold },
              { label: "Total Bets", value: MOCK_USER.totalBets, color: COLORS.text },
            ].map((s, i) => (
              <div key={i} style={{ flex: 1, minWidth: 70 }}>
                <div style={{ fontSize: 14, fontWeight: 700, fontFamily: "'JetBrains Mono', monospace", color: s.color }}>{s.value}</div>
                <div style={{ fontSize: 10, color: COLORS.textDim }}>{s.label}</div>
              </div>
            ))}
          </div>
        </div>

        <div style={{
          background: COLORS.surface,
          border: `1px solid ${COLORS.border}`,
          borderRadius: 16,
          padding: "16px 20px",
          marginBottom: 16,
        }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 10 }}>
            <div style={{ fontSize: 14, fontWeight: 700 }}>My Rank</div>
            <button
              onClick={() => onNavigate && onNavigate("leaderboard")}
              style={{
                background: "none",
                border: "none",
                color: COLORS.accentLight,
                cursor: "pointer",
                fontSize: 12,
                fontWeight: 600,
              }}
            >
              View Board
            </button>
          </div>
          {myRank ? (
            <div style={{ display: "flex", gap: 24, flexWrap: "wrap" }}>
              <div>
                <div style={{ fontSize: 11, color: COLORS.textDim }}>Rank</div>
                <div style={{ fontSize: 20, fontWeight: 700, fontFamily: "'JetBrains Mono', monospace" }}>#{myRank.rank}</div>
              </div>
              <div>
                <div style={{ fontSize: 11, color: COLORS.textDim }}>Tier</div>
                <div style={{ fontSize: 20, fontWeight: 700, color: COLORS.accentLight }}>{myRank.tier}</div>
              </div>
              <div>
                <div style={{ fontSize: 11, color: COLORS.textDim }}>Accuracy</div>
                <div style={{ fontSize: 20, fontWeight: 700, fontFamily: "'JetBrains Mono', monospace" }}>{myRank.accuracy.toFixed(1)}%</div>
              </div>
            </div>
          ) : (
            <div style={{ fontSize: 13, color: COLORS.textDim }}>No ranking data available yet.</div>
          )}
        </div>

        {/* Portfolio Summary */}
        <div style={{
          background: COLORS.surface, border: `1px solid ${COLORS.border}`,
          borderRadius: 16, padding: "16px 20px", marginBottom: 20,
          display: "flex", justifyContent: "space-between", alignItems: "center", flexWrap: "wrap", gap: 12,
        }}>
          <div>
            <div style={{ fontSize: 11, color: COLORS.textDim }}>Portfolio Value</div>
            <div style={{ fontSize: 22, fontWeight: 700, fontFamily: "'JetBrains Mono', monospace" }}>${totalValue.toFixed(2)}</div>
          </div>
          <div>
            <div style={{ fontSize: 11, color: COLORS.textDim }}>Invested</div>
            <div style={{ fontSize: 16, fontWeight: 600, fontFamily: "'JetBrains Mono', monospace", color: COLORS.textMuted }}>${totalInvested.toFixed(2)}</div>
          </div>
          <div>
            <div style={{ fontSize: 11, color: COLORS.textDim }}>Unrealized P&L</div>
            <div style={{ fontSize: 16, fontWeight: 700, fontFamily: "'JetBrains Mono', monospace", color: unrealizedPnl >= 0 ? COLORS.green : COLORS.red }}>
              {unrealizedPnl >= 0 ? "+" : ""}${unrealizedPnl.toFixed(2)}
            </div>
          </div>
        </div>

        {/* Tabs */}
        <div style={{ display: "flex", gap: 0, marginBottom: 16, borderBottom: `1px solid ${COLORS.border}`, overflowX: "auto", msOverflowStyle: "none", scrollbarWidth: "none" }}>
            {[
              { id: "activity", label: "Activity" },
              { id: "positions", label: `Positions (${MOCK_POSITIONS.length})` },
              { id: "history", label: `History (${historyList.length})` },
              { id: "votes", label: `Votes (${MOCK_VOTE_ACTIVITY.length})` },
              { id: "created", label: `Created (${MOCK_CREATED_QUESTIONS.length})` },
              { id: "follows", label: `Follows (${followingList.length}/${followersList.length})` },
            ].map(t => (
            <button key={t.id} onClick={() => setTab(t.id)} style={{
              padding: "10px 16px", border: "none", cursor: "pointer",
              background: "transparent",
              color: tab === t.id ? COLORS.text : COLORS.textDim,
              fontSize: 13, fontWeight: tab === t.id ? 700 : 500,
              borderBottom: tab === t.id ? `2px solid ${COLORS.accent}` : "2px solid transparent",
              whiteSpace: "nowrap", fontFamily: "'DM Sans', sans-serif",
            }}>{t.label}</button>
          ))}
        </div>

        {/* Activity Feed */}
        {tab === "activity" && (
          <div style={{ display: "flex", flexDirection: "column", gap: 2 }}>
            {MOCK_ACTIVITY_FEED.map((a: ActivityItem, i: number) => (
              <div key={a.id} onClick={() => {
                if (a.type === "vote") onNavigate && onNavigate("vote");
                else if (a.type === "bet" || a.type === "pnl") onNavigate && onNavigate("explore");
                else if (a.type === "create") onNavigate && onNavigate("vote");
                else if (a.type === "comment") onNavigate && onNavigate("vote");
              }} style={{
                display: "flex", gap: 12, padding: "14px 4px", cursor: "pointer",
                borderBottom: i < MOCK_ACTIVITY_FEED.length - 1 ? `1px solid ${COLORS.border}` : "none",
                animation: `fadeUp 0.3s ease ${i * 0.03}s both`,
                borderRadius: 8, transition: "background 0.15s",
              }}
              onMouseEnter={e => e.currentTarget.style.background = COLORS.surfaceHover}
              onMouseLeave={e => e.currentTarget.style.background = "transparent"}
              >
                {/* Dot */}
                <div style={{
                  width: 8, height: 8, borderRadius: "50%", flexShrink: 0,
                  background: a.color, marginTop: 6,
                }} />
                {/* Content */}
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontSize: 13, fontWeight: 600, color: COLORS.text, marginBottom: 2 }}>{a.text}</div>
                  <div style={{ fontSize: 13, color: COLORS.textMuted, lineHeight: 1.4, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{a.market}</div>
                  <div style={{ fontSize: 12, color: a.type === "pnl" ? a.color : COLORS.textDim, marginTop: 3, fontFamily: a.type === "bet" || a.type === "pnl" || a.type === "earn" ? "'JetBrains Mono', monospace" : "inherit" }}>{a.detail}</div>
                </div>
                {/* Time */}
                <div style={{ fontSize: 11, color: COLORS.textDim, whiteSpace: "nowrap", flexShrink: 0, paddingTop: 2 }}>{a.time}</div>
              </div>
            ))}
          </div>
        )}

        {/* Positions */}
        {tab === "positions" && (
          <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
            {MOCK_POSITIONS.map((p: PositionItem) => {
              const currentValue = p.shares * p.currentPrice;
              const pnl = currentValue - p.invested;
              const pnlPct = ((pnl / p.invested) * 100).toFixed(1);
              return (
                <div key={p.id} onClick={() => onBet && onBet({ ...p, yesPrice: p.currentPrice, category: "position", timeLeft: "—", volume: "—", change24h: 0, voters: 0, bettors: 0, comments: 0, creator: "" }, p.side)} style={{
                  background: COLORS.surface, border: `1px solid ${COLORS.border}`,
                  borderRadius: 12, padding: 16, cursor: "pointer", transition: "border-color 0.15s",
                }}
                onMouseEnter={e => e.currentTarget.style.borderColor = "rgba(255,255,255,0.12)"}
                onMouseLeave={e => e.currentTarget.style.borderColor = COLORS.border}
                >
                  <div style={{ fontSize: 14, fontWeight: 600, color: COLORS.text, lineHeight: 1.4, marginBottom: 10 }}>{p.title}</div>
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", flexWrap: "wrap", gap: 8 }}>
                    <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
                      <span style={{
                        fontSize: 11, fontWeight: 700, padding: "2px 8px", borderRadius: 4,
                        background: p.side === "YES" ? COLORS.greenBg : COLORS.redBg,
                        color: p.side === "YES" ? COLORS.green : COLORS.red,
                      }}>{p.side}</span>
                      <span style={{ fontSize: 12, color: COLORS.textDim, fontFamily: "'JetBrains Mono', monospace" }}>{p.shares} shares</span>
                      <span style={{ fontSize: 12, color: COLORS.textDim, fontFamily: "'JetBrains Mono', monospace" }}>{Math.round(p.avgPrice * 100)}\u00a2 \u2192 {Math.round(p.currentPrice * 100)}\u00a2</span>
                    </div>
                    <div style={{ textAlign: "right" }}>
                      <span style={{ fontSize: 14, fontWeight: 700, fontFamily: "'JetBrains Mono', monospace", color: pnl >= 0 ? COLORS.green : COLORS.red }}>
                        {pnl >= 0 ? "+" : ""}${pnl.toFixed(2)}
                      </span>
                      <span style={{ fontSize: 11, color: pnl >= 0 ? COLORS.green : COLORS.red, marginLeft: 4 }}>({pnl >= 0 ? "+" : ""}{pnlPct}%)</span>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}

        {/* History */}
        {tab === "history" && (
          <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
            {historyList.map((h: SettlementHistoryItem, i: number) => {
              const invested = h.shares * h.priceBought;
              const returned = h.result === "WON" ? h.shares * h.priceSold : 0;
              const profit = returned - invested;
              const historyKey = `${String(h.id ?? "history")}-${String(h.date ?? "")}-${i}`;
              return (
                <div key={historyKey} style={{
                  background: COLORS.surface, border: `1px solid ${COLORS.border}`,
                  borderRadius: 12, padding: 16,
                }}>
                  <div style={{ fontSize: 14, fontWeight: 600, color: COLORS.text, lineHeight: 1.4, marginBottom: 10 }}>{h.title}</div>
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", flexWrap: "wrap", gap: 8 }}>
                    <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
                      <span style={{
                        fontSize: 11, fontWeight: 700, padding: "2px 8px", borderRadius: 4,
                        background: h.side === "YES" ? COLORS.greenBg : COLORS.redBg,
                        color: h.side === "YES" ? COLORS.green : COLORS.red,
                      }}>{h.side}</span>
                      <span style={{
                        fontSize: 11, fontWeight: 700, padding: "2px 8px", borderRadius: 4,
                        background: h.result === "WON" ? COLORS.greenBg : COLORS.redBg,
                        color: h.result === "WON" ? COLORS.green : COLORS.red,
                      }}>{h.result}</span>
                      <span style={{ fontSize: 12, color: COLORS.textDim }}>{h.date}</span>
                    </div>
                    <span style={{ fontSize: 14, fontWeight: 700, fontFamily: "'JetBrains Mono', monospace", color: profit >= 0 ? COLORS.green : COLORS.red }}>
                      {profit >= 0 ? "+" : ""}${profit.toFixed(2)}
                    </span>
                  </div>
                </div>
              );
            })}
          </div>
        )}

        {/* Vote Activity */}
        {tab === "votes" && (
          <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
            {MOCK_VOTE_ACTIVITY.map((v: VoteActivityItem) => (
              <div key={v.id} onClick={() => onNavigate && onNavigate("vote")} style={{
                background: COLORS.surface, border: `1px solid ${COLORS.border}`,
                borderRadius: 12, padding: 16, cursor: "pointer", transition: "border-color 0.15s",
              }}
              onMouseEnter={e => e.currentTarget.style.borderColor = "rgba(255,255,255,0.12)"}
              onMouseLeave={e => e.currentTarget.style.borderColor = COLORS.border}
              >
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 8 }}>
                  <span style={{ fontSize: 9, padding: "2px 6px", borderRadius: 3, background: COLORS.accentGlow, color: COLORS.accentLight, fontWeight: 600, textTransform: "uppercase" }}>{v.category}</span>
                  <span style={{ fontSize: 11, color: COLORS.textDim }}>{v.date}</span>
                </div>
                <div style={{ fontSize: 14, fontWeight: 600, color: COLORS.text, lineHeight: 1.4, marginBottom: 10 }}>{v.title}</div>
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                  <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
                    <span style={{ fontSize: 12, color: COLORS.textDim }}>Voted</span>
                    <span style={{
                      fontSize: 11, fontWeight: 700, padding: "2px 8px", borderRadius: 4,
                      background: v.vote === "YES" ? COLORS.greenBg : COLORS.redBg,
                      color: v.vote === "YES" ? COLORS.green : COLORS.red,
                    }}>{v.vote}</span>
                  </div>
                  <div style={{ display: "flex", alignItems: "center", gap: 6 }}>
                    <span style={{ fontSize: 11, color: COLORS.textDim, fontFamily: "'JetBrains Mono', monospace" }}>{v.totalVotes.toLocaleString()} votes</span>
                    {v.status === "active" && <span style={{ width: 6, height: 6, borderRadius: "50%", background: COLORS.green }} />}
                    {v.status === "resolved" && <span style={{ fontSize: 10, color: COLORS.textDim }}>Resolved</span>}
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}

        {/* Created Questions */}
        {tab === "created" && (
          <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
            {MOCK_CREATED_QUESTIONS.map((q: CreatedQuestionItem) => (
              <div key={q.id} onClick={() => onNavigate && onNavigate(q.status === "converted" ? "explore" : "vote")} style={{
                background: COLORS.surface, border: `1px solid ${COLORS.border}`,
                borderRadius: 12, padding: 16, cursor: "pointer", transition: "border-color 0.15s",
              }}
              onMouseEnter={e => e.currentTarget.style.borderColor = "rgba(255,255,255,0.12)"}
              onMouseLeave={e => e.currentTarget.style.borderColor = COLORS.border}
              >
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 8 }}>
                  <span style={{ fontSize: 9, padding: "2px 6px", borderRadius: 3, background: COLORS.accentGlow, color: COLORS.accentLight, fontWeight: 600, textTransform: "uppercase" }}>{q.category}</span>
                  <span style={{
                    fontSize: 10, fontWeight: 600, padding: "2px 8px", borderRadius: 4,
                    background: q.status === "converted" ? COLORS.greenBg : "rgba(255,255,255,0.03)",
                    color: q.status === "converted" ? COLORS.green : COLORS.textDim,
                  }}>{q.status === "converted" ? "Betting Active" : "Voting"}</span>
                </div>
                <div style={{ fontSize: 14, fontWeight: 600, color: COLORS.text, lineHeight: 1.4, marginBottom: 10 }}>{q.title}</div>
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                  <span style={{ fontSize: 12, color: COLORS.textDim, fontFamily: "'JetBrains Mono', monospace" }}>{q.totalVotes.toLocaleString()} votes</span>
                  <span style={{ fontSize: 13, fontWeight: 700, fontFamily: "'JetBrains Mono', monospace", color: COLORS.green }}>+${q.earnings.toFixed(2)}</span>
                </div>
              </div>
            ))}
            <div style={{
              background: "linear-gradient(135deg, rgba(124,58,237,0.06), rgba(91,33,182,0.04))",
              border: "1px solid rgba(124,58,237,0.2)",
              borderRadius: 12, padding: 16, textAlign: "center",
            }}>
              <div style={{ fontSize: 12, color: COLORS.textDim, marginBottom: 4 }}>Total Creator Earnings</div>
              <div style={{ fontSize: 22, fontWeight: 700, fontFamily: "'JetBrains Mono', monospace", color: COLORS.accentLight }}>${MOCK_USER.creatorEarnings.toFixed(2)}</div>
            </div>
          </div>
        )}

        {/* Follow Lists */}
        {tab === "follows" && (
          <div style={{ display: "grid", gap: 14 }}>
            <div style={{
              background: COLORS.surface, border: `1px solid ${COLORS.border}`,
              borderRadius: 12, padding: 14,
            }}>
              <div style={{ fontSize: 13, fontWeight: 700, color: COLORS.text, marginBottom: 10 }}>
                Following ({followingList.length})
              </div>
              {followingList.length === 0 ? (
                <div style={{ fontSize: 12, color: COLORS.textDim }}>No following users yet.</div>
              ) : followingList.map((u, i) => (
                <div key={`following-${asString(u.memberId, String(i))}`} style={{
                  display: "flex", justifyContent: "space-between", padding: "8px 0",
                  borderTop: i === 0 ? "none" : `1px solid ${COLORS.border}`,
                }}>
                  <div style={{ fontSize: 13, color: COLORS.text }}>
                    {asString(u.displayName) || asString(u.username) || `User #${asString(u.memberId)}`}
                  </div>
                  <div style={{ fontSize: 11, color: COLORS.textDim }}>{toRelativeTime(asString(u.followedAt))}</div>
                </div>
              ))}
            </div>

            <div style={{
              background: COLORS.surface, border: `1px solid ${COLORS.border}`,
              borderRadius: 12, padding: 14,
            }}>
              <div style={{ fontSize: 13, fontWeight: 700, color: COLORS.text, marginBottom: 10 }}>
                Followers ({followersList.length})
              </div>
              {followersList.length === 0 ? (
                <div style={{ fontSize: 12, color: COLORS.textDim }}>No followers yet.</div>
              ) : followersList.map((u, i) => (
                <div key={`followers-${asString(u.memberId, String(i))}`} style={{
                  display: "flex", justifyContent: "space-between", padding: "8px 0",
                  borderTop: i === 0 ? "none" : `1px solid ${COLORS.border}`,
                }}>
                  <div style={{ fontSize: 13, color: COLORS.text }}>
                    {asString(u.displayName) || asString(u.username) || `User #${asString(u.memberId)}`}
                  </div>
                  <div style={{ fontSize: 11, color: COLORS.textDim }}>{toRelativeTime(asString(u.followedAt))}</div>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>

      {/* Deposit Modal */}
      {depositModal && (
        <div onClick={() => { setDepositModal(false); setTxStatus("idle"); }} style={{
          position: "fixed", inset: 0, zIndex: 300,
          background: "rgba(0,0,0,0.6)", backdropFilter: "blur(4px)",
          display: "flex", alignItems: "center", justifyContent: "center",
          animation: "modalFadeIn 0.15s ease",
        }}>
          <div onClick={e => e.stopPropagation()} style={{
            background: COLORS.surface, borderRadius: 16, padding: 28,
            maxWidth: 400, width: "90%", border: `1px solid ${COLORS.border}`,
            animation: "modalSlideUp 0.2s ease both",
          }}>
            <div style={{ fontSize: 18, fontWeight: 700, marginBottom: 4 }}>Deposit USDC</div>
            <div style={{ fontSize: 13, color: COLORS.textDim, marginBottom: 20 }}>
              Send USDC via MetaMask on Polygon Amoy Testnet
            </div>

            {/* Wallet connect */}
            {!wallet.account ? (
              <button onClick={wallet.connect} disabled={wallet.connecting} style={{
                width: "100%", padding: "12px 0", borderRadius: 10, cursor: "pointer", marginBottom: 16,
                background: `linear-gradient(135deg, #F6851B, #E2761B)`,
                border: "none", color: "white", fontSize: 14, fontWeight: 700,
                display: "flex", alignItems: "center", justifyContent: "center", gap: 8,
              }}>
                🦊 {wallet.connecting ? "Connecting..." : wallet.isMetaMask() ? "Connect MetaMask" : "Install MetaMask"}
              </button>
            ) : (
              <div style={{
                padding: "8px 12px", borderRadius: 8, marginBottom: 16,
                background: "rgba(34,197,94,0.08)", border: "1px solid rgba(34,197,94,0.2)",
                display: "flex", alignItems: "center", justifyContent: "space-between",
              }}>
                <span style={{ fontSize: 12, color: COLORS.green }}>🦊 {wallet.shortAddress(wallet.account)}</span>
                {!wallet.isCorrectChain && (
                  <button onClick={wallet.switchToAmoy} style={{
                    padding: "3px 10px", borderRadius: 6, cursor: "pointer",
                    background: "rgba(239,68,68,0.1)", border: "1px solid rgba(239,68,68,0.3)",
                    color: COLORS.red, fontSize: 11, fontWeight: 600,
                  }}>Switch to Amoy</button>
                )}
              </div>
            )}

            {/* Amount */}
            <div style={{ display: "grid", gridTemplateColumns: "repeat(4, 1fr)", gap: 6, marginBottom: 12 }}>
              {["10", "50", "100", "500"].map(a => (
                <button key={a} onClick={() => setDepositAmount(a)} style={{
                  padding: "8px 0", borderRadius: 8, cursor: "pointer",
                  background: depositAmount === a ? COLORS.accentGlow : "rgba(255,255,255,0.02)",
                  border: `1px solid ${depositAmount === a ? COLORS.accent : COLORS.border}`,
                  color: depositAmount === a ? COLORS.accentLight : COLORS.textDim,
                  fontSize: 13, fontWeight: 600, fontFamily: "'JetBrains Mono', monospace",
                }}>${a}</button>
              ))}
            </div>
            <div style={{
              display: "flex", alignItems: "center", gap: 8, padding: "10px 14px",
              background: "rgba(255,255,255,0.02)", border: `1px solid ${COLORS.border}`,
              borderRadius: 10, marginBottom: 16,
            }}>
              <span style={{ fontSize: 16, fontWeight: 600, color: COLORS.textDim }}>$</span>
              <input value={depositAmount} onChange={e => setDepositAmount(e.target.value.replace(/[^0-9.]/g, ""))}
                placeholder="Enter USDC amount"
                style={{ flex: 1, background: "none", border: "none", outline: "none", color: COLORS.text, fontSize: 16, fontWeight: 600, fontFamily: "'JetBrains Mono', monospace" }}
              />
            </div>

            {/* Status */}
            {txStatus !== "idle" && (
              <div style={{
                padding: "10px 14px", borderRadius: 8, marginBottom: 16, fontSize: 13,
                background: txStatus === "success" ? "rgba(34,197,94,0.08)" : txStatus === "error" ? "rgba(239,68,68,0.08)" : "rgba(124,58,237,0.08)",
                border: `1px solid ${txStatus === "success" ? "rgba(34,197,94,0.2)" : txStatus === "error" ? "rgba(239,68,68,0.2)" : "rgba(124,58,237,0.2)"}`,
                color: txStatus === "success" ? COLORS.green : txStatus === "error" ? COLORS.red : COLORS.accentLight,
              }}>{txMessage}</div>
            )}

            <div style={{ display: "flex", gap: 10 }}>
              <button onClick={() => { setDepositModal(false); setTxStatus("idle"); }} style={{
                flex: 1, padding: "11px 0", borderRadius: 10, cursor: "pointer",
                background: "transparent", border: `1px solid ${COLORS.border}`,
                color: COLORS.textMuted, fontSize: 14, fontWeight: 600,
              }}>Cancel</button>
              <button
                disabled={!wallet.account || !depositAmount || txStatus === "pending" || txStatus === "verifying"}
                onClick={async () => {
                  if (!wallet.account || !depositAmount || !paymentConfig) return;
                  const amount = parseFloat(depositAmount);
                  if (!amount || amount <= 0) return;
                  try {
                    await ensureWalletLinked(wallet.account);
                    setTxStatus("pending");
                    setTxMessage("Waiting for MetaMask confirmation...");
                    const txHash = await wallet.sendUSDC(paymentConfig.receiverWallet, paymentConfig.usdcContract, amount);
                    setTxStatus("verifying");
                    setTxMessage(`TX submitted (${txHash.slice(0, 10)}...). Verifying on-chain...`);
                    await paymentApi.verifyDeposit(txHash, amount, wallet.account);
                    setTxStatus("success");
                    setTxMessage(`✓ $${amount} USDC deposited successfully!`);
                  } catch (e: any) {
                    setTxStatus("error");
                    setTxMessage(e?.message ?? "Transaction failed");
                  }
                }}
                style={{
                  flex: 1, padding: "11px 0", borderRadius: 10, cursor: "pointer",
                  background: `linear-gradient(135deg, ${COLORS.accent}, #5B21B6)`,
                  border: "none", color: "white", fontSize: 14, fontWeight: 700,
                  opacity: (!wallet.account || !depositAmount || txStatus === "pending" || txStatus === "verifying") ? 0.5 : 1,
                }}
              >
                {txStatus === "pending" ? "Confirming..." : txStatus === "verifying" ? "Verifying..." : `Deposit${depositAmount ? ` $${depositAmount}` : ""}`}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Withdraw Modal */}
      {withdrawModal && (
        <div onClick={() => { setWithdrawModal(false); setTxStatus("idle"); }} style={{
          position: "fixed", inset: 0, zIndex: 300,
          background: "rgba(0,0,0,0.6)", backdropFilter: "blur(4px)",
          display: "flex", alignItems: "center", justifyContent: "center",
          animation: "modalFadeIn 0.15s ease",
        }}>
          <div onClick={e => e.stopPropagation()} style={{
            background: COLORS.surface, borderRadius: 16, padding: 28,
            maxWidth: 400, width: "90%", border: `1px solid ${COLORS.border}`,
            animation: "modalSlideUp 0.2s ease both",
          }}>
            <div style={{ fontSize: 18, fontWeight: 700, marginBottom: 4 }}>Withdraw USDC</div>
            <div style={{ fontSize: 13, color: COLORS.textDim, marginBottom: 20 }}>
              Withdraw to your connected wallet
            </div>

            {/* Wallet connect */}
            {!wallet.account ? (
              <button onClick={wallet.connect} disabled={wallet.connecting} style={{
                width: "100%", padding: "12px 0", borderRadius: 10, cursor: "pointer", marginBottom: 16,
                background: `linear-gradient(135deg, #F6851B, #E2761B)`,
                border: "none", color: "white", fontSize: 14, fontWeight: 700,
                display: "flex", alignItems: "center", justifyContent: "center", gap: 8,
              }}>
                🦊 {wallet.connecting ? "Connecting..." : wallet.isMetaMask() ? "Connect MetaMask" : "Install MetaMask"}
              </button>
            ) : (
              <div style={{
                padding: "8px 12px", borderRadius: 8, marginBottom: 16,
                background: "rgba(34,197,94,0.08)", border: "1px solid rgba(34,197,94,0.2)",
                fontSize: 12, color: COLORS.green,
              }}>
                🦊 Withdraw to: {wallet.shortAddress(wallet.account)}
              </div>
            )}

            <div style={{
              display: "flex", alignItems: "center", gap: 8, padding: "10px 14px",
              background: "rgba(255,255,255,0.02)", border: `1px solid ${COLORS.border}`,
              borderRadius: 10, marginBottom: 16,
            }}>
              <span style={{ fontSize: 16, fontWeight: 600, color: COLORS.textDim }}>$</span>
              <input value={withdrawAmount} onChange={e => setWithdrawAmount(e.target.value.replace(/[^0-9.]/g, ""))}
                placeholder="Enter USDC amount"
                style={{ flex: 1, background: "none", border: "none", outline: "none", color: COLORS.text, fontSize: 16, fontWeight: 600, fontFamily: "'JetBrains Mono', monospace" }}
              />
            </div>

            {txStatus !== "idle" && (
              <div style={{
                padding: "10px 14px", borderRadius: 8, marginBottom: 16, fontSize: 13,
                background: txStatus === "success" ? "rgba(34,197,94,0.08)" : txStatus === "error" ? "rgba(239,68,68,0.08)" : "rgba(124,58,237,0.08)",
                border: `1px solid ${txStatus === "success" ? "rgba(34,197,94,0.2)" : txStatus === "error" ? "rgba(239,68,68,0.2)" : "rgba(124,58,237,0.2)"}`,
                color: txStatus === "success" ? COLORS.green : txStatus === "error" ? COLORS.red : COLORS.accentLight,
              }}>{txMessage}</div>
            )}

            <div style={{ display: "flex", gap: 10 }}>
              <button onClick={() => { setWithdrawModal(false); setTxStatus("idle"); }} style={{
                flex: 1, padding: "11px 0", borderRadius: 10, cursor: "pointer",
                background: "transparent", border: `1px solid ${COLORS.border}`,
                color: COLORS.textMuted, fontSize: 14, fontWeight: 600,
              }}>Cancel</button>
              <button
                disabled={!wallet.account || !withdrawAmount || txStatus === "pending"}
                onClick={async () => {
                  if (!wallet.account || !withdrawAmount) return;
                  const amount = parseFloat(withdrawAmount);
                  if (!amount || amount <= 0) return;
                  try {
                    await ensureWalletLinked(wallet.account);
                    setTxStatus("pending");
                    setTxMessage("Processing withdrawal...");
                    await paymentApi.withdraw(amount, wallet.account);
                    setTxStatus("success");
                    setTxMessage(`✓ $${amount} USDC withdrawal initiated!`);
                  } catch (e: any) {
                    setTxStatus("error");
                    setTxMessage(e?.message ?? "Withdrawal failed");
                  }
                }}
                style={{
                  flex: 1, padding: "11px 0", borderRadius: 10, cursor: "pointer",
                  background: `linear-gradient(135deg, ${COLORS.accent}, #5B21B6)`,
                  border: "none", color: "white", fontSize: 14, fontWeight: 700,
                  opacity: (!wallet.account || !withdrawAmount || txStatus === "pending") ? 0.5 : 1,
                }}
              >
                {txStatus === "pending" ? "Processing..." : `Withdraw${withdrawAmount ? ` $${withdrawAmount}` : ""}`}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Followers / Following Modal */}
      {followModal && (
        <div
          onClick={() => setFollowModal(null)}
          style={{
            position: "fixed", inset: 0, zIndex: 320,
            background: "rgba(0,0,0,0.6)", backdropFilter: "blur(4px)",
            display: "flex", alignItems: "center", justifyContent: "center",
            animation: "modalFadeIn 0.15s ease",
          }}
        >
          <div
            onClick={(e) => e.stopPropagation()}
            style={{
              background: COLORS.surface, borderRadius: 16, padding: 20,
              maxWidth: 460, width: "92%", maxHeight: "70vh", overflow: "auto",
              border: `1px solid ${COLORS.border}`,
            }}
          >
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 14 }}>
              <div style={{ fontSize: 16, fontWeight: 700, color: COLORS.text }}>
                {followModal === "followers" ? `Followers (${followersList.length})` : `Following (${followingList.length})`}
              </div>
              <button
                onClick={() => setFollowModal(null)}
                style={{
                  border: "none", background: "transparent", color: COLORS.textDim,
                  cursor: "pointer", fontSize: 18, lineHeight: 1,
                }}
              >
                ×
              </button>
            </div>

            {(followModal === "followers" ? followersList : followingList).length === 0 ? (
              <div style={{ fontSize: 13, color: COLORS.textDim, padding: "10px 4px" }}>
                {followModal === "followers" ? "No followers yet." : "No following users yet."}
              </div>
            ) : (
              (followModal === "followers" ? followersList : followingList).map((u, i) => {
                const name = asString(u.displayName) || asString(u.username) || `User #${asString(u.memberId, String(i))}`;
                const followedAt = asString(u.followedAt);
                return (
                  <div
                    key={`${followModal}-${asString(u.memberId, String(i))}-${i}`}
                    style={{
                      display: "flex", justifyContent: "space-between", alignItems: "center",
                      padding: "10px 4px", borderTop: i === 0 ? "none" : `1px solid ${COLORS.border}`,
                    }}
                  >
                    <div style={{ fontSize: 13, color: COLORS.text }}>{name}</div>
                    <div style={{ fontSize: 11, color: COLORS.textDim }}>{followedAt ? toRelativeTime(followedAt) : ""}</div>
                  </div>
                );
              })
            )}
            {socialListError && (
              <div style={{ marginTop: 10, fontSize: 12, color: COLORS.red }}>
                {socialListError}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

export default MyPageView;
