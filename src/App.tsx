 "use client";

import React, { useState, useEffect, useRef } from "react";
import { DARK_THEME, LIGHT_THEME, COLORS, setColors } from "./theme";
import { getStyles } from "./styles/globalStyles";
import { CATEGORIES } from "./data/mockMarkets";
import { useMarkets, useVoteFeed } from "./hooks/useApi";
import { authApi, getTokenExpiryMs, isLoggedIn, memberApi, mapToUser, setMemberId, setToken } from "./services/api";
import Logo from "./components/Logo";
import AvatarCircle from "./components/AvatarCircle";
import CategoryTabs from "./components/CategoryTabs";
import MarketCard from "./components/MarketCard";
import TopQuestionsSidebar from "./components/TopQuestionsSidebar";
import { AuthModal, LoginModal, SignupModal } from "./components/AuthModals";
import VoteDetailView from "./views/VoteDetailView";
import BetDetailView from "./views/BetDetailView";
import MyPageView from "./views/MyPageView";
import ExploreView from "./views/ExploreView";
import VotePageView from "./views/VotePageView";
import QuestionSubmitView from "./views/QuestionSubmitView";
import LeaderboardView from "./views/LeaderboardView";

export default function PredataHome({ initialPage = "home" }: { initialPage?: string }) {
  type AppPage = "home" | "vote" | "explore" | "leaderboard" | "submit" | "mypage";
  type NavSnapshot = { page: AppPage; betView: any; voteView: any; showMyPage: boolean; exploreLiveOnly: boolean };
  const [activeCategory, setActiveCategory] = useState("trending");
  const [searchQuery, setSearchQuery] = useState("");
  const [scrolled, setScrolled] = useState(false);
  const [authModal, setAuthModal] = useState<any>(null);
  const [loggedInUser, setLoggedInUser] = useState<any>(null);
  const [profileDropdown, setProfileDropdown] = useState(false);
  const [showMyPage, setShowMyPage] = useState(false);
  const [betView, setBetView] = useState<any>(null);
  const [voteView, setVoteView] = useState<any>(null);
  const [toast, setToast] = useState<any>(null);
  const [page, setPage] = useState<AppPage>((initialPage as AppPage) || "home");
  const [exploreLiveOnly, setExploreLiveOnly] = useState(false);
  const [voteFocusQuestionId, setVoteFocusQuestionId] = useState<string | number | null>(null);
  const [navHistory, setNavHistory] = useState<NavSnapshot[]>([]);
  const [isMobile, setIsMobile] = useState(false);
  const [isDark, setIsDark] = useState(true);
  const [nowMs, setNowMs] = useState(() => Date.now());
  const inactivityTimerRef = useRef<number | null>(null);
  const refreshInFlightRef = useRef(false);
  const lastRefreshAttemptMsRef = useRef(0);
  const lastActivityStorageKey = "predata_last_activity_ms";

  setColors(isDark ? DARK_THEME : LIGHT_THEME);

  const toggleTheme = () => setIsDark(!isDark);

  // API: fetch markets by category
  const { markets: filteredMarkets, loading: marketsLoading, refresh: refreshMarkets } = useMarkets(activeCategory) as any;

  // API: fetch top votes for mobile carousel
  const { questions: topVoteQuestions } = useVoteFeed({ sort: "top" }) as any;

  const showToast = (message: string, type = "error") => {
    setToast({ message, type });
    setTimeout(() => setToast(null), 3000);
  };

  const requireLogin = (message = "Please log in or sign up to continue.") => {
    if (isLoggedIn()) return true;
    showToast(message, "error");
    setAuthModal("login");
    return false;
  };

  const getVotingTimeLeftLabel = (endAtRaw?: string | null) => {
    if (!endAtRaw) return null;
    const endAt = new Date(endAtRaw).getTime();
    if (Number.isNaN(endAt)) return null;
    const diffMs = endAt - nowMs;
    if (diffMs <= 0) return "Voting closed";

    const totalMinutes = Math.floor(diffMs / 60000);
    const days = Math.floor(totalMinutes / (60 * 24));
    const hours = Math.floor((totalMinutes % (60 * 24)) / 60);
    const minutes = totalMinutes % 60;

    if (days > 0) return `${days}d ${hours}h left`;
    if (hours > 0) return `${hours}h ${minutes}m left`;
    return `${minutes}m left`;
  };

  const getBettingTimeLeftLabel = (endAtRaw?: string | null, market?: any) => {
    const status = String(market?.status ?? "").toUpperCase();
    const phase = String(market?.phase ?? "").toUpperCase();
    const isLiveCategory = String(market?.category ?? "").toUpperCase() === "LIVE";
    const isFinished = phase === "FINISHED" || phase === "SETTLED" || status === "SETTLED" || status === "CANCELLED";
    if (isLiveCategory && !isFinished) return "Live betting open";
    if (!endAtRaw) return null;
    const endAt = new Date(endAtRaw).getTime();
    if (Number.isNaN(endAt)) return null;
    const diffMs = endAt - nowMs;
    if (diffMs <= 0) return "Betting closed";

    const totalMinutes = Math.floor(diffMs / 60000);
    const days = Math.floor(totalMinutes / (60 * 24));
    const hours = Math.floor((totalMinutes % (60 * 24)) / 60);
    const minutes = totalMinutes % 60;

    if (days > 0) return `${days}d ${hours}h left`;
    if (hours > 0) return `${hours}h ${minutes}m left`;
    return `${minutes}m left`;
  };

  const clearSession = () => {
    setToken(null);
    setMemberId(null);
    setLoggedInUser(null);
    setProfileDropdown(false);
    if (typeof window !== "undefined") {
      window.localStorage.removeItem(lastActivityStorageKey);
    }
  };

  const refreshLoggedInUser = () => {
    if (!isLoggedIn()) {
      setLoggedInUser(null);
      return;
    }
    memberApi.me()
      .then((u) => setLoggedInUser(mapToUser(u)))
      .catch((err: unknown) => {
        const message = err instanceof Error ? err.message.toLowerCase() : String(err || "").toLowerCase();
        if (message.includes("401") || message.includes("unauthorized") || message.includes("authentication")) {
          clearSession();
        }
      });
  };

  useEffect(() => {
    const handleScroll = () => setScrolled(window.scrollY > 10);
    window.addEventListener("scroll", handleScroll);
    return () => window.removeEventListener("scroll", handleScroll);
  }, []);

  useEffect(() => {
    if (initialPage) setPage(initialPage as AppPage);
  }, [initialPage]);

  useEffect(() => {
    if (typeof window === "undefined") return;
    const path = window.location.pathname;
    if (path !== "/explore") return;
    const mode = new URLSearchParams(window.location.search).get("mode");
    setPage("explore");
    setExploreLiveOnly(mode === "live");
  }, []);

  useEffect(() => {
    if ((page === "submit" || page === "mypage") && !isLoggedIn()) {
      showToast("Please log in or sign up to continue.", "error");
      setAuthModal("login");
      setPage("home");
    }
  }, [page]);

  useEffect(() => {
    const syncMobile = () => setIsMobile(window.innerWidth <= 768);
    syncMobile();
    window.addEventListener("resize", syncMobile);
    return () => window.removeEventListener("resize", syncMobile);
  }, []);

  useEffect(() => {
    const timer = window.setInterval(() => setNowMs(Date.now()), 60_000);
    return () => window.clearInterval(timer);
  }, []);

  useEffect(() => {
    const initialSnapshot: NavSnapshot = { page, betView, voteView, showMyPage, exploreLiveOnly };
    window.history.replaceState({ __predata: true, ...initialSnapshot }, "", window.location.href);

    const onPopState = (event: PopStateEvent) => {
      const state = event.state as (NavSnapshot & { __predata?: boolean }) | null;
      if (!state || state.__predata !== true) return;
      setPage(state.page);
      setBetView(state.betView ?? null);
      setVoteView(state.voteView ?? null);
      setShowMyPage(Boolean(state.showMyPage));
      setExploreLiveOnly(Boolean(state.exploreLiveOnly));
      setNavHistory(prev => (prev.length > 0 ? prev.slice(0, -1) : prev));
      window.scrollTo(0, 0);
    };

    window.addEventListener("popstate", onPopState);
    return () => window.removeEventListener("popstate", onPopState);
  }, []);

  useEffect(() => {
    if (page === "mypage") {
      setShowMyPage(true);
      setPage("home");
    }
  }, [page]);

  // 로그인 상태 복원
  useEffect(() => {
    refreshLoggedInUser();
  }, []);

  useEffect(() => {
    const onFocus = () => {
      if (isLoggedIn() && !loggedInUser) refreshLoggedInUser();
    };
    window.addEventListener("focus", onFocus);
    return () => window.removeEventListener("focus", onFocus);
  }, [loggedInUser]);

  // 사용자 활동 기반 세션 관리:
  // - 30분 무활동이면 자동 로그아웃
  // - 활동이 있고 토큰 만료 1분 이내면 refresh
  useEffect(() => {
    if (!loggedInUser || !isLoggedIn()) return;

    const INACTIVITY_LIMIT_MS = 30 * 60 * 1000;
    const REFRESH_LEAD_MS = 60 * 1000;
    const MIN_REFRESH_RETRY_GAP_MS = 15 * 1000;

    const getLastActivityMs = () => {
      const raw = window.localStorage.getItem(lastActivityStorageKey);
      const parsed = raw ? Number(raw) : NaN;
      if (Number.isFinite(parsed) && parsed > 0) return parsed;
      const now = Date.now();
      window.localStorage.setItem(lastActivityStorageKey, String(now));
      return now;
    };

    const setLastActivityNow = () => {
      const now = Date.now();
      window.localStorage.setItem(lastActivityStorageKey, String(now));
      return now;
    };

    const scheduleInactivityLogout = (baseMs: number) => {
      const remaining = Math.max(1_000, INACTIVITY_LIMIT_MS - (Date.now() - baseMs));
      if (inactivityTimerRef.current !== null) {
        window.clearTimeout(inactivityTimerRef.current);
      }
      inactivityTimerRef.current = window.setTimeout(() => {
        if (isLoggedIn()) clearSession();
      }, remaining);
    };

    const tryRefreshIfNeeded = async () => {
      const expiryMs = getTokenExpiryMs();
      if (!expiryMs) return;
      const now = Date.now();
      const expiresSoon = expiryMs - now <= REFRESH_LEAD_MS;
      if (!expiresSoon) return;
      if (refreshInFlightRef.current) return;
      if (now - lastRefreshAttemptMsRef.current < MIN_REFRESH_RETRY_GAP_MS) return;

      refreshInFlightRef.current = true;
      lastRefreshAttemptMsRef.current = now;
      try {
        await authApi.refresh();
      } catch {
        clearSession();
      } finally {
        refreshInFlightRef.current = false;
      }
    };

    const handleUserActivity = () => {
      if (!isLoggedIn()) return;
      const at = setLastActivityNow();
      scheduleInactivityLogout(at);
      void tryRefreshIfNeeded();
    };

    const initialActivityMs = getLastActivityMs();
    if (Date.now() - initialActivityMs >= INACTIVITY_LIMIT_MS) {
      clearSession();
      return;
    }
    scheduleInactivityLogout(initialActivityMs);

    const events: Array<keyof WindowEventMap> = [
      "click", "keydown", "mousemove", "scroll", "touchstart"
    ];
    events.forEach((evt) => window.addEventListener(evt, handleUserActivity, { passive: true }));

    return () => {
      if (inactivityTimerRef.current !== null) {
        window.clearTimeout(inactivityTimerRef.current);
      }
      events.forEach((evt) => window.removeEventListener(evt, handleUserActivity));
    };
  }, [loggedInUser]);

  const PAGE_PATHS: Record<AppPage, string> = {
    home: "/", vote: "/vote", explore: "/explore",
    leaderboard: "/leaderboard", submit: "/submit", mypage: "/mypage",
  };
  const buildPath = (targetPage: AppPage, liveOnly = false) => {
    const base = PAGE_PATHS[targetPage] ?? "/";
    if (targetPage === "explore" && liveOnly) return `${base}?mode=live`;
    return base;
  };

  const pushHistory = () => {
    setNavHistory(prev => [...prev, { page, betView, voteView, showMyPage, exploreLiveOnly }]);
  };

  const pushBrowserState = (snapshot: NavSnapshot) => {
    const path = buildPath(snapshot.page, snapshot.exploreLiveOnly);
    window.history.pushState({ __predata: true, ...snapshot }, "", path);
  };

  const navigate = (nextPage: AppPage) => {
    if (nextPage === "submit" && !requireLogin("Please log in or sign up to submit a question.")) {
      return;
    }
    if (nextPage === "mypage" && !requireLogin("Please log in or sign up to access My Page.")) {
      return;
    }

    const nextExploreLiveOnly = nextPage === "explore" ? false : false;
    const path = buildPath(nextPage, nextExploreLiveOnly);
    if (page === "mypage") {
      window.history.replaceState(
        { __predata: true, page: nextPage, betView: null, voteView: null, showMyPage: false, exploreLiveOnly: nextExploreLiveOnly },
        "",
        path
      );
    } else {
      pushHistory();
      pushBrowserState({ page: nextPage, betView: null, voteView: null, showMyPage: false, exploreLiveOnly: nextExploreLiveOnly });
    }
    setBetView(null);
    setVoteView(null);
    setShowMyPage(false);
    if (nextPage !== "vote") setVoteFocusQuestionId(null);
    if (nextPage !== "explore") setExploreLiveOnly(false);
    setPage(nextPage);
    window.scrollTo(0, 0);
  };

  const openLiveHome = () => {
    if (page !== "explore" || !exploreLiveOnly) {
      pushHistory();
      pushBrowserState({ page: "explore", betView: null, voteView: null, showMyPage: false, exploreLiveOnly: true });
    }
    setPage("explore");
    setExploreLiveOnly(true);
    setBetView(null);
    setVoteView(null);
    setShowMyPage(false);
    window.scrollTo(0, 0);
  };

  const openExploreGeneral = () => {
    setExploreLiveOnly(false);
    navigate("explore");
  };

  const openMyPage = () => {
    if (!requireLogin("Please log in or sign up to access My Page.")) return;
    if (showMyPage) return;
    pushHistory();
    pushBrowserState({ page, betView, voteView, showMyPage: true, exploreLiveOnly });
    setShowMyPage(true);
    window.scrollTo(0, 0);
  };

  const closeMyPageModal = () => {
    setShowMyPage(false);
  };

  const goBack = () => {
    if (navHistory.length > 0) {
      window.history.back();
      return;
    }

    setNavHistory(prev => {
      if (prev.length === 0) {
        if (betView) {
          setBetView(null);
        } else if (voteView) {
          setVoteView(null);
        } else if (showMyPage) {
          setShowMyPage(false);
        } else if (page !== "home") {
          setPage("home");
        }
        return prev;
      }
      const last = prev[prev.length - 1];
      setPage(last.page);
      setBetView(last.betView);
      setVoteView(last.voteView);
      setShowMyPage(last.showMyPage);
      setExploreLiveOnly(last.exploreLiveOnly);
      return prev.slice(0, -1);
    });
    window.scrollTo(0, 0);
  };

  const handleBet = (market: any, choice: any) => {
    if (!requireLogin("Please log in or sign up to place a bet.")) return;
    pushHistory();
    pushBrowserState({ page, betView: { market, choice }, voteView: null, showMyPage: false, exploreLiveOnly });
    setShowMyPage(false);
    setVoteView(null);
    setBetView({ market, choice });
    window.scrollTo(0, 0);
  };

  const handleOpenVoteQuestion = (question: any) => {
    setVoteFocusQuestionId(question?.id ?? null);
    navigate("vote");
  };

  // ── Sub-page Global Header ──
  const desktopGlobalHeader = (
    <header className="desktop-only" style={{
      position: "sticky", top: 0, zIndex: 120,
      background: `${COLORS.bg}f2`,
      backdropFilter: "blur(12px)",
      borderBottom: `1px solid ${COLORS.border}`,
      transition: "all 0.3s ease",
    }}>
      <div style={{
        maxWidth: 1320, margin: "0 auto", padding: "12px 24px",
        display: "flex", alignItems: "center", justifyContent: "space-between",
      }}>
        <button onClick={() => navigate("home")} style={{ background: "none", border: "none", cursor: "pointer", padding: 0, display: "flex", color: "inherit" }}>
          <Logo />
        </button>
        <div style={{ display: "flex", alignItems: "center", gap: 16 }}>
          <button onClick={openLiveHome} style={{
            background: "none", border: "none", color: COLORS.accentLight, fontSize: 13, fontWeight: 700, cursor: "pointer", fontFamily: "'DM Sans', sans-serif",
          }}>LIVE</button>
          <button onClick={() => navigate("vote")} style={{
            background: "none", border: "none", color: COLORS.textMuted, fontSize: 13, fontWeight: 500, cursor: "pointer", fontFamily: "'DM Sans', sans-serif",
          }}>Vote</button>
          <button onClick={openExploreGeneral} style={{
            background: "none", border: "none", color: COLORS.textMuted, fontSize: 13, fontWeight: 500, cursor: "pointer", fontFamily: "'DM Sans', sans-serif",
          }}>Explore</button>
          <button onClick={() => navigate("leaderboard")} style={{
            background: "none", border: "none", color: COLORS.textMuted, fontSize: 13, fontWeight: 500, cursor: "pointer", fontFamily: "'DM Sans', sans-serif",
          }}>Leaderboard</button>

          <div style={{ width: 1, height: 20, background: COLORS.border }} />

          <button style={{
            position: "relative", background: "none", border: "none",
            color: COLORS.textMuted, fontSize: 16, cursor: "pointer",
          }}>
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 01-3.46 0"/></svg>
            <span style={{
              position: "absolute", top: -2, right: -4,
              width: 8, height: 8, borderRadius: "50%",
              background: COLORS.accent, border: `2px solid ${COLORS.bg}`,
            }} />
          </button>

          {loggedInUser ? (
            <div style={{ position: "relative" }}>
              <button
                onClick={() => setProfileDropdown(prev => !prev)}
                style={{
                  background: "none", border: "none", padding: 0,
                  cursor: "pointer", borderRadius: "50%", transition: "all 0.2s",
                }}
              >
                <AvatarCircle
                  avatar={loggedInUser.avatar}
                  name={loggedInUser.name || "ME"}
                  size={36}
                  style={{ border: "2px solid rgba(124,58,237,0.3)" }}
                />
              </button>
              {profileDropdown && (
                <>
                  <div onClick={() => setProfileDropdown(false)} style={{ position: "fixed", inset: 0, zIndex: 199 }} />
                  <div style={{
                    position: "absolute", right: 0, top: "calc(100% + 8px)",
                    width: 200, background: COLORS.surface,
                    border: `1px solid ${COLORS.border}`,
                    borderRadius: 12, padding: "6px 0",
                    boxShadow: "0 8px 32px rgba(0,0,0,0.4)",
                    zIndex: 200,
                  }}>
                    <div style={{ padding: "12px 16px", borderBottom: `1px solid ${COLORS.border}` }}>
                      <div style={{ fontSize: 14, fontWeight: 600, color: COLORS.text }}>{loggedInUser.name}</div>
                      <div style={{ fontSize: 12, color: COLORS.textDim, marginTop: 2 }}>{loggedInUser.email}</div>
                    </div>
                    {[
                      { label: "My Page", icon: "👤", action: () => { setProfileDropdown(false); openMyPage(); } },
                      { label: "How to Use", icon: "📖", action: () => { setProfileDropdown(false); } },
                    ].map(item => (
                      <button
                        key={item.label}
                        onClick={item.action}
                        style={{
                          width: "100%", padding: "10px 16px",
                          background: "none", border: "none",
                          color: COLORS.text, fontSize: 13, fontWeight: 500,
                          cursor: "pointer", textAlign: "left",
                          display: "flex", alignItems: "center", gap: 10,
                          fontFamily: "'DM Sans', sans-serif", transition: "background 0.15s",
                        }}
                        onMouseEnter={e => e.currentTarget.style.background = "rgba(255,255,255,0.05)"}
                        onMouseLeave={e => e.currentTarget.style.background = "none"}
                      >
                        <span style={{ fontSize: 15 }}>{item.icon}</span>
                        {item.label}
                      </button>
                    ))}
                    <div style={{ borderTop: `1px solid ${COLORS.border}`, marginTop: 4 }}>
                      <button
                        onClick={clearSession}
                        style={{
                          width: "100%", padding: "10px 16px",
                          background: "none", border: "none",
                          color: "#EF4444", fontSize: 13, fontWeight: 500,
                          cursor: "pointer", textAlign: "left",
                          display: "flex", alignItems: "center", gap: 10,
                          fontFamily: "'DM Sans', sans-serif", transition: "background 0.15s",
                        }}
                        onMouseEnter={e => e.currentTarget.style.background = "rgba(239,68,68,0.08)"}
                        onMouseLeave={e => e.currentTarget.style.background = "none"}
                      >
                        <span style={{ fontSize: 15 }}>🚪</span>
                        Log Out
                      </button>
                    </div>
                  </div>
                </>
              )}
            </div>
          ) : (
            <>
              <button onClick={() => setAuthModal("login")} style={{
                padding: "8px 16px", borderRadius: 8, background: "transparent",
                color: COLORS.textMuted, border: `1px solid ${COLORS.border}`,
                cursor: "pointer", fontSize: 13, fontWeight: 500, fontFamily: "'DM Sans', sans-serif",
                transition: "all 0.2s",
              }}>Log In</button>
              <button onClick={() => setAuthModal("signup")} style={{
                padding: "8px 18px", borderRadius: 8,
                background: "linear-gradient(135deg, #7C3AED, #5B21B6)",
                color: "white", border: "none", cursor: "pointer",
                fontSize: 13, fontWeight: 600, fontFamily: "'DM Sans', sans-serif",
                boxShadow: "0 2px 8px rgba(124,58,237,0.3)", transition: "all 0.2s",
              }}>Sign Up</button>
            </>
          )}
        </div>
      </div>
    </header>
  );

  // ── Mobile Tab Bar (shared across all pages) ──
  const mobileTabBar = (
    <nav className="mobile-tab-bar">
      {[
        { id: "home", label: "Home", icon: <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg> },
        { id: "vote", label: "Vote", icon: <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect x="3" y="3" width="18" height="18" rx="2"/><path d="M9 12l2 2 4-4"/></svg> },
        { id: "explore", label: "Explore", icon: <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><circle cx="12" cy="12" r="10"/><polygon points="16.24 7.76 14.12 14.12 7.76 16.24 9.88 9.88 16.24 7.76"/></svg> },
        { id: "mypage", label: "My Page", icon: <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/><circle cx="12" cy="7" r="4"/></svg> },
      ].map(tab => (
        <button key={tab.id} onClick={() => { if (tab.id === "mypage") openMyPage(); else if (tab.id === "explore") openExploreGeneral(); else if (tab.id === "vote") navigate("vote"); else if (tab.id === "home") navigate("home"); }} style={{
          background: "none", border: "none", cursor: "pointer",
          display: "flex", flexDirection: "column", alignItems: "center", gap: 2,
          color: tab.id === page || (tab.id === "home" && page === "home" && !betView) ? COLORS.accentLight : COLORS.textDim,
          padding: "4px 12px", transition: "color 0.15s", fontFamily: "'DM Sans', sans-serif",
        }}>
          {tab.icon}
          <span style={{ fontSize: 10, fontWeight: 500 }}>{tab.label}</span>
        </button>
      ))}
    </nav>
  );

  const authModalsJSX = (
    <>
      {authModal === "login" && (
        <LoginModal
          onClose={() => {
            setAuthModal(null);
            refreshLoggedInUser();
          }}
          onSwitchToSignup={() => setAuthModal("signup")}
        />
      )}
      {authModal === "signup" && (
        <SignupModal
          onClose={() => {
            setAuthModal(null);
            refreshLoggedInUser();
          }}
          onSwitchToLogin={() => setAuthModal("login")}
        />
      )}
    </>
  );

  const myPageModalJSX = showMyPage ? (
    <div
      onClick={closeMyPageModal}
      style={{
        position: "fixed", inset: 0, zIndex: 300,
        background: "rgba(0,0,0,0.6)", backdropFilter: "blur(4px)",
        display: "flex", alignItems: "center", justifyContent: "center",
      }}
    >
      <div
        onClick={e => e.stopPropagation()}
        style={{
          width: "90%", maxWidth: 800, maxHeight: "85vh",
          background: COLORS.bg, borderRadius: 16,
          border: `1px solid ${COLORS.border}`,
          overflow: "auto", position: "relative",
        }}
      >
        <button
          onClick={closeMyPageModal}
          style={{
            position: "sticky", top: 12, float: "right",
            marginRight: 12, zIndex: 10,
            width: 32, height: 32, borderRadius: "50%",
            background: "rgba(255,255,255,0.1)", border: "none",
            color: COLORS.text, fontSize: 18, cursor: "pointer",
            display: "flex", alignItems: "center", justifyContent: "center",
          }}
        >×</button>
        <MyPageView
          onBack={closeMyPageModal}
          onBet={handleBet}
          onNavigate={(next: string) => {
            if (["home", "vote", "explore", "leaderboard", "submit"].includes(next)) {
              closeMyPageModal();
              navigate(next as AppPage);
            }
          }}
          isDark={isDark}
          toggleTheme={toggleTheme}
        />
      </div>
    </div>
  ) : null;

  if (betView) {
    return (
      <div style={{ minHeight: "100vh", background: COLORS.bg, paddingBottom: 70 }}>
        <style>{getStyles().global}</style>
        {desktopGlobalHeader}
        <BetDetailView
          market={betView.market}
          initialChoice={betView.choice}
          onBack={goBack}
          onRequireAuth={(message?: string) => requireLogin(message || "Please log in or sign up to continue.")}
        />
        {mobileTabBar}
        {myPageModalJSX}
        {authModalsJSX}
      </div>
    );
  }

  if (voteView?.question) {
    return (
      <div style={{ minHeight: "100vh", background: COLORS.bg, paddingBottom: 70 }}>
        <style>{getStyles().global}</style>
        {desktopGlobalHeader}
        <VoteDetailView
          question={voteView.question}
          initialChoice={null}
          onBack={goBack}
        />
        {mobileTabBar}
        {myPageModalJSX}
        {authModalsJSX}
      </div>
    );
  }

  // ── Explore View ──
  if (page === "explore") {
    return (
      <div style={{ minHeight: "100vh", background: COLORS.bg, paddingBottom: 70 }}>
        <style>{getStyles().global}</style>
        {desktopGlobalHeader}
        <ExploreView onBack={goBack} onBet={handleBet} liveOnlyMode={exploreLiveOnly} />
        {mobileTabBar}
        {myPageModalJSX}
        {authModalsJSX}
      </div>
    );
  }

  // ── Submit View ──
  if (page === "submit") {
    return (
      <div style={{ minHeight: "100vh", background: COLORS.bg, paddingBottom: 70 }}>
        <style>{getStyles().global}</style>
        {desktopGlobalHeader}
        <div
          onClick={goBack}
          style={{
            position: "fixed", inset: 0, zIndex: 320,
            background: "rgba(0,0,0,0.62)", backdropFilter: "blur(4px)",
            display: "flex", alignItems: "center", justifyContent: "center",
            padding: "24px",
          }}
        >
          <div
            onClick={(e) => e.stopPropagation()}
            style={{
              width: "min(980px, 100%)",
              maxHeight: "82vh",
              overflow: "auto",
              borderRadius: 16,
              border: `1px solid ${COLORS.border}`,
              background: COLORS.bg,
              boxShadow: "0 24px 80px rgba(0,0,0,0.45)",
              position: "relative",
            }}
          >
            <button
              onClick={goBack}
              style={{
                position: "sticky", top: 12, float: "right",
                marginRight: 12, zIndex: 10,
                width: 32, height: 32, borderRadius: "50%",
                background: "rgba(255,255,255,0.1)", border: "none",
                color: COLORS.text, fontSize: 18, cursor: "pointer",
                display: "flex", alignItems: "center", justifyContent: "center",
              }}
            >
              ×
            </button>
            <QuestionSubmitView onBack={goBack} />
          </div>
        </div>
        {mobileTabBar}
        {myPageModalJSX}
        {authModalsJSX}
      </div>
    );
  }

  // ── Vote Page View ──
  if (page === "vote") {
    return (
      <div style={{ minHeight: "100vh", background: COLORS.bg, paddingBottom: 70 }}>
        <style>{getStyles().global}</style>
        {desktopGlobalHeader}
        <VotePageView
          onBack={goBack}
          onSubmit={() => navigate("submit")}
          showToast={showToast}
          focusQuestionId={voteFocusQuestionId}
          onFocusHandled={() => setVoteFocusQuestionId(null)}
          onRequireAuth={(message) => {
            showToast(message || "Please log in or sign up to continue.", "error");
            setAuthModal("login");
          }}
        />
        {mobileTabBar}
        {myPageModalJSX}
        {authModalsJSX}
      </div>
    );
  }

  // ── Leaderboard View ──
  if (page === "leaderboard") {
    return (
      <div style={{ minHeight: "100vh", background: COLORS.bg, paddingBottom: 70 }}>
        <style>{getStyles().global}</style>
        {desktopGlobalHeader}
        <LeaderboardView onBack={goBack} />
        {mobileTabBar}
        {myPageModalJSX}
        {authModalsJSX}
      </div>
    );
  }

  return (
    <div style={{ minHeight: "100vh", background: COLORS.bg, paddingBottom: 70 }}>
      <style>{getStyles().global}</style>

      {/* ── Navbar ─── */}
      <header style={{
        position: "sticky", top: 0, zIndex: 100,
        background: scrolled ? `${COLORS.bg}f2` : COLORS.bg,
        backdropFilter: scrolled ? "blur(12px)" : "none",
        borderBottom: `1px solid ${scrolled ? COLORS.border : "transparent"}`,
        transition: "all 0.3s ease",
      }}>
        <div style={{
          maxWidth: 1320, margin: "0 auto", padding: isMobile ? "10px 14px" : "12px 24px",
          display: "flex", alignItems: "center", justifyContent: "space-between", gap: isMobile ? 8 : 0,
        }}>
          <button onClick={() => navigate("home")} style={{ background: "none", border: "none", cursor: "pointer", padding: 0, display: "flex", color: "inherit" }}>
            <Logo />
          </button>

          {/* Search - Desktop */}
          <div className="desktop-only" style={{
            flex: 1, maxWidth: 460, margin: "0 32px",
            position: "relative",
          }}>
            <div style={{
              display: "flex", alignItems: "center", gap: 8,
              background: COLORS.surface, border: `1px solid ${COLORS.border}`,
              borderRadius: 10, padding: "8px 14px",
              transition: "border-color 0.2s",
            }}>
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke={COLORS.textDim} strokeWidth="2"><circle cx="11" cy="11" r="8"/><path d="M21 21l-4.35-4.35"/></svg>
              <input
                type="text" placeholder="Search markets..."
                value={searchQuery} onChange={e => setSearchQuery(e.target.value)}
                style={{
                  flex: 1, background: "none", border: "none", outline: "none",
                  color: COLORS.text, fontSize: 13, fontFamily: "'DM Sans', sans-serif",
                }}
              />
              <kbd style={{
                fontSize: 10, padding: "2px 6px", borderRadius: 4,
                background: "rgba(255,255,255,0.05)", color: COLORS.textDim,
                border: `1px solid ${COLORS.border}`,
              }}>⌘K</kbd>
            </div>
          </div>

          {/* Nav Actions - Desktop */}
          <div className="desktop-only" style={{ display: "flex", alignItems: "center", gap: 16 }}>
            <button onClick={openLiveHome} style={{
              background: "none", border: "none", color: COLORS.accentLight,
              fontSize: 13, fontWeight: 700, cursor: "pointer",
              fontFamily: "'DM Sans', sans-serif", transition: "opacity 0.2s",
            }}
            onMouseEnter={e => (e.currentTarget as HTMLElement).style.opacity = "0.8"}
            onMouseLeave={e => (e.currentTarget as HTMLElement).style.opacity = "1"}
            >LIVE</button>
            <button onClick={() => navigate("vote")} style={{
              background: "none", border: "none", color: COLORS.textMuted,
              fontSize: 13, fontWeight: 500, cursor: "pointer",
              fontFamily: "'DM Sans', sans-serif", transition: "color 0.2s",
            }}
            onMouseEnter={e => (e.currentTarget as HTMLElement).style.color = COLORS.text}
            onMouseLeave={e => (e.currentTarget as HTMLElement).style.color = COLORS.textMuted}
            >Vote</button>

            <button onClick={openExploreGeneral} style={{
              background: "none", border: "none", color: COLORS.textMuted,
              fontSize: 13, fontWeight: 500, cursor: "pointer",
              fontFamily: "'DM Sans', sans-serif", transition: "color 0.2s",
            }}
            onMouseEnter={e => (e.currentTarget as HTMLElement).style.color = COLORS.text}
            onMouseLeave={e => (e.currentTarget as HTMLElement).style.color = COLORS.textMuted}
            >Explore</button>

            <button onClick={() => navigate("leaderboard")} style={{
              background: "none", border: "none", color: COLORS.textMuted,
              fontSize: 13, fontWeight: 500, cursor: "pointer",
              fontFamily: "'DM Sans', sans-serif", transition: "color 0.2s",
            }}
            onMouseEnter={e => (e.currentTarget as HTMLElement).style.color = COLORS.text}
            onMouseLeave={e => (e.currentTarget as HTMLElement).style.color = COLORS.textMuted}
            >Leaderboard</button>

            <div style={{ width: 1, height: 20, background: COLORS.border }} />

            <button style={{
              position: "relative", background: "none", border: "none",
              color: COLORS.textMuted, fontSize: 16, cursor: "pointer",
            }}>
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 01-3.46 0"/></svg>
              <span style={{
                position: "absolute", top: -2, right: -4,
                width: 8, height: 8, borderRadius: "50%",
                background: COLORS.accent, border: `2px solid ${COLORS.bg}`,
              }} />
            </button>

            {loggedInUser ? (
              /* ── 로그인 상태: 프로필 아바타 + 드롭다운 ── */
              <div style={{ position: "relative" }}>
                <button
                  onClick={() => setProfileDropdown(prev => !prev)}
                  style={{
                    background: "none", border: "none", padding: 0,
                    cursor: "pointer", borderRadius: "50%", transition: "all 0.2s",
                  }}
                >
                  <AvatarCircle
                    avatar={loggedInUser.avatar}
                    name={loggedInUser.name || "ME"}
                    size={36}
                    style={{ border: "2px solid rgba(124,58,237,0.3)" }}
                  />
                </button>

                {profileDropdown && (
                  <>
                    <div
                      onClick={() => setProfileDropdown(false)}
                      style={{ position: "fixed", inset: 0, zIndex: 199 }}
                    />
                    <div style={{
                      position: "absolute", right: 0, top: "calc(100% + 8px)",
                      width: 200, background: COLORS.surface,
                      border: `1px solid ${COLORS.border}`,
                      borderRadius: 12, padding: "6px 0",
                      boxShadow: "0 8px 32px rgba(0,0,0,0.4)",
                      zIndex: 200,
                    }}>
                      <div style={{ padding: "12px 16px", borderBottom: `1px solid ${COLORS.border}` }}>
                        <div style={{ fontSize: 14, fontWeight: 600, color: COLORS.text }}>
                          {loggedInUser.name}
                        </div>
                        <div style={{ fontSize: 12, color: COLORS.textDim, marginTop: 2 }}>
                          {loggedInUser.email}
                        </div>
                      </div>

                      {[
                        { label: "My Page", icon: "👤", action: () => { setProfileDropdown(false); openMyPage(); } },
                        { label: "How to Use", icon: "📖", action: () => { setProfileDropdown(false); }},
                      ].map(item => (
                        <button
                          key={item.label}
                          onClick={item.action}
                          style={{
                            width: "100%", padding: "10px 16px",
                            background: "none", border: "none",
                            color: COLORS.text, fontSize: 13, fontWeight: 500,
                            cursor: "pointer", textAlign: "left",
                            display: "flex", alignItems: "center", gap: 10,
                            fontFamily: "'DM Sans', sans-serif",
                            transition: "background 0.15s",
                          }}
                          onMouseEnter={e => e.currentTarget.style.background = "rgba(255,255,255,0.05)"}
                          onMouseLeave={e => e.currentTarget.style.background = "none"}
                        >
                          <span style={{ fontSize: 15 }}>{item.icon}</span>
                          {item.label}
                        </button>
                      ))}

                      <div style={{ borderTop: `1px solid ${COLORS.border}`, marginTop: 4 }}>
                        <button
                          onClick={clearSession}
                          style={{
                            width: "100%", padding: "10px 16px",
                            background: "none", border: "none",
                            color: "#EF4444", fontSize: 13, fontWeight: 500,
                            cursor: "pointer", textAlign: "left",
                            display: "flex", alignItems: "center", gap: 10,
                            fontFamily: "'DM Sans', sans-serif",
                            transition: "background 0.15s",
                          }}
                          onMouseEnter={e => e.currentTarget.style.background = "rgba(239,68,68,0.08)"}
                          onMouseLeave={e => e.currentTarget.style.background = "none"}
                        >
                          <span style={{ fontSize: 15 }}>🚪</span>
                          Log Out
                        </button>
                      </div>
                    </div>
                  </>
                )}
              </div>
            ) : (
              <>
                <button onClick={() => setAuthModal("login")} style={{
                  padding: "8px 16px", borderRadius: 8,
                  background: "transparent",
                  color: COLORS.textMuted, border: `1px solid ${COLORS.border}`,
                  cursor: "pointer",
                  fontSize: 13, fontWeight: 500, fontFamily: "'DM Sans', sans-serif",
                  transition: "all 0.2s",
                }}
                onMouseEnter={e => { (e.currentTarget as HTMLElement).style.borderColor = "rgba(255,255,255,0.15)"; (e.currentTarget as HTMLElement).style.color = COLORS.text; }}
                onMouseLeave={e => { (e.currentTarget as HTMLElement).style.borderColor = COLORS.border; (e.currentTarget as HTMLElement).style.color = COLORS.textMuted; }}
                >
                  Log In
                </button>

                <button onClick={() => setAuthModal("signup")} style={{
                  padding: "8px 18px", borderRadius: 8,
                  background: "linear-gradient(135deg, #7C3AED, #5B21B6)",
                  color: "white", border: "none", cursor: "pointer",
                  fontSize: 13, fontWeight: 600, fontFamily: "'DM Sans', sans-serif",
                  boxShadow: "0 2px 8px rgba(124,58,237,0.3)",
                  transition: "all 0.2s",
                }}
                onMouseEnter={e => (e.currentTarget as HTMLElement).style.boxShadow = "0 4px 16px rgba(124,58,237,0.5)"}
                onMouseLeave={e => (e.currentTarget as HTMLElement).style.boxShadow = "0 2px 8px rgba(124,58,237,0.3)"}
                >
                  Sign Up
                </button>
              </>
            )}
          </div>

          {/* Nav Actions - Mobile: search bar + bell */}
          <div className="mobile-only-flex" style={{ flex: 1, minWidth: 0, alignItems: "center", gap: 8, marginLeft: 10 }}>
            <div style={{
              flex: 1, minWidth: 0, display: "flex", alignItems: "center", gap: 6,
              background: COLORS.surface, border: `1px solid ${COLORS.border}`,
              borderRadius: 8, padding: "7px 10px",
            }}>
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke={COLORS.textDim} strokeWidth="2"><circle cx="11" cy="11" r="8"/><path d="M21 21l-4.35-4.35"/></svg>
              <input
                type="text" placeholder="Search..."
                value={searchQuery} onChange={e => setSearchQuery(e.target.value)}
                style={{ flex: 1, minWidth: 0, background: "none", border: "none", outline: "none", color: COLORS.text, fontSize: 13, fontFamily: "'DM Sans', sans-serif" }}
              />
            </div>
            <button style={{ background: "none", border: "none", color: COLORS.textMuted, cursor: "pointer", padding: 4, display: "flex", position: "relative", flexShrink: 0 }}>
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round"><path d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 01-3.46 0"/></svg>
              <span style={{ position: "absolute", top: 2, right: 2, width: 7, height: 7, borderRadius: "50%", background: COLORS.accent, border: `2px solid ${COLORS.bg}` }} />
            </button>
          </div>
        </div>
      </header>

      {/* ═══════════════════════════════════════════
           DESKTOP Main Content
         ═══════════════════════════════════════════ */}
      <main className="desktop-only" style={{ maxWidth: 1320, margin: "0 auto", padding: "20px 24px" }}>
        <div className="main-grid">
          <div>
            <CategoryTabs active={activeCategory} onSelect={setActiveCategory} />
            <div className="market-grid" style={{ maxHeight: "764px", overflowY: "auto", paddingRight: 4 }}>
              {filteredMarkets.map((market: any, i: number) => (
                <MarketCard key={market.id} market={market} index={i} onBet={handleBet} />
              ))}
            </div>
            {filteredMarkets.length === 0 && (
              <div style={{ textAlign: "center", padding: "60px 20px", color: COLORS.textDim }}>
                <div style={{ fontSize: 15, fontWeight: 500 }}>No markets in this category yet</div>
                <div style={{ fontSize: 13, marginTop: 4 }}>Check back soon or explore other categories</div>
              </div>
            )}
          </div>

          <aside style={{ display: "flex", flexDirection: "column", gap: 16 }}>
            <TopQuestionsSidebar
              onNoCredits={() => showToast("No votes remaining for today.")}
              onSubmit={() => {
                if (!requireLogin("Please log in or sign up to submit a question.")) return;
                navigate("submit");
              }}
              onRequireAuth={(message) => requireLogin(message || "Please log in or sign up to continue.")}
              onOpenQuestion={handleOpenVoteQuestion}
            />
            <div style={{
              background: "linear-gradient(135deg, #7C3AED, #4C1D95)",
              borderRadius: 14, padding: 20, textAlign: "center",
            }}>
              <div style={{ fontSize: 15, fontWeight: 700, marginBottom: 4 }}>Vote & Earn</div>
              <div style={{ fontSize: 12, color: "rgba(255,255,255,0.7)", marginBottom: 14, lineHeight: 1.5 }}>
                Share your opinion, earn rewards,<br />and generate valuable data insights.
              </div>
              <button style={{
                width: "100%", padding: "10px 0", borderRadius: 8,
                background: "rgba(255,255,255,0.15)", color: "white",
                border: "1px solid rgba(255,255,255,0.2)", cursor: "pointer",
                fontSize: 13, fontWeight: 600, fontFamily: "'DM Sans', sans-serif",
                backdropFilter: "blur(4px)", transition: "background 0.2s",
              }}
              onClick={() => navigate("vote")}
              onMouseEnter={e => (e.currentTarget as HTMLElement).style.background = "rgba(255,255,255,0.25)"}
              onMouseLeave={e => (e.currentTarget as HTMLElement).style.background = "rgba(255,255,255,0.15)"}
              >Start Voting →</button>
            </div>
          </aside>
        </div>
      </main>

      {/* ═══════════════════════════════════════════
           MOBILE Main Content
         ═══════════════════════════════════════════ */}
      <div className="mobile-only" style={{ padding: "0 0 20px" }}>

        {/* Top Votes Carousel */}
        <div style={{ marginBottom: 16 }}>
          <div style={{ padding: "0 14px", marginBottom: 10, display: "flex", justifyContent: "space-between", alignItems: "center" }}>
            <span style={{ fontSize: 15, fontWeight: 700, fontFamily: "'Outfit', sans-serif" }}>Top Votes</span>
            <button onClick={() => navigate("vote")} style={{ background: "none", border: "none", color: COLORS.accentLight, fontSize: 12, fontWeight: 600, cursor: "pointer" }}>See all →</button>
          </div>
          <div style={{ display: "flex", gap: 10, overflowX: "auto", padding: "0 14px", scrollSnapType: "x mandatory", msOverflowStyle: "none", scrollbarWidth: "none" }}>
            {[...topVoteQuestions]
              .filter((q: any) => {
                if (!q?.votingEndAt) return true;
                const ts = new Date(q.votingEndAt).getTime();
                if (Number.isNaN(ts)) return true;
                return ts > nowMs;
              })
              .sort((a: any, b: any) => (b.totalVotes ?? (b.yesVotes + b.noVotes)) - (a.totalVotes ?? (a.yesVotes + a.noVotes)))
              .slice(0, 5)
              .map((q: any, i: number) => {
              const total = q.totalVotes ?? (q.yesVotes + q.noVotes);
              const hiddenVoteResults = q.hiddenVoteResults === true;
              const yesPct = Math.round(((q.yesVotes ?? 0) / Math.max(total, 1)) * 100);
              const votingTimeLeft = getVotingTimeLeftLabel(q.votingEndAt);
              return (
                <div key={q.id} onClick={() => navigate("vote")} style={{
                  minWidth: 240, scrollSnapAlign: "start",
                  background: COLORS.surface,
                  border: `1px solid ${COLORS.border}`, borderRadius: 14, padding: 14,
                  cursor: "pointer", transition: "all 0.2s",
                  animation: `fadeUp 0.3s ease ${i * 0.05}s both`,
                }}>
                  <div style={{ display: "flex", alignItems: "center", gap: 6, marginBottom: 8 }}>
                    <AvatarCircle avatar={q.avatar} name={q.submitter} size={22} />
                    <span style={{ fontSize: 11, color: COLORS.textDim }}>{q.submitter}</span>
                    <span style={{ fontSize: 9, padding: "1px 5px", borderRadius: 3, background: COLORS.accentGlow, color: COLORS.accentLight, fontWeight: 600, textTransform: "uppercase", marginLeft: "auto" }}>{q.category}</span>
                  </div>
                  <div style={{ fontSize: 13, fontWeight: 600, color: COLORS.text, lineHeight: 1.4, marginBottom: 10, display: "-webkit-box", WebkitLineClamp: 2, WebkitBoxOrient: "vertical", overflow: "hidden" }}>{q.title}</div>
                  {votingTimeLeft && (
                    <div style={{ fontSize: 10, color: COLORS.textDim, fontFamily: "'JetBrains Mono', monospace", marginTop: -4, marginBottom: 8 }}>
                      {votingTimeLeft}
                    </div>
                  )}
                  {hiddenVoteResults ? (
                    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                      <span style={{ fontSize: 11, color: COLORS.textDim }}>Hidden until betting close</span>
                      <span style={{ fontSize: 11, color: COLORS.textDim, fontFamily: "'JetBrains Mono', monospace" }}>{total.toLocaleString()} votes</span>
                    </div>
                  ) : (
                    <>
                      {/* Vote bar mini */}
                      <div style={{ height: 4, borderRadius: 2, background: "rgba(239,68,68,0.15)", overflow: "hidden", marginBottom: 6 }}>
                        <div style={{ height: "100%", width: `${yesPct}%`, borderRadius: 2, background: COLORS.green }} />
                      </div>
                      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                        <span style={{ fontSize: 11, fontWeight: 600, fontFamily: "'JetBrains Mono', monospace", color: COLORS.green }}>Yes {yesPct}%</span>
                        <span style={{ fontSize: 11, color: COLORS.textDim, fontFamily: "'JetBrains Mono', monospace" }}>{total.toLocaleString()} votes</span>
                      </div>
                    </>
                  )}
                </div>
              );
            })}
          </div>
        </div>

        {/* Category Tabs (horizontal scroll) */}
        <div style={{ padding: "0 14px", marginBottom: 8 }}>
          <div style={{ display: "flex", gap: 6, overflowX: "auto", msOverflowStyle: "none", scrollbarWidth: "none" }}>
            {[{ id: "live", label: "LIVE" }, { id: "trending", label: "Trending" }, { id: "crypto", label: "Crypto" }, { id: "tech", label: "Tech" }, { id: "politics", label: "Politics" }, { id: "economy", label: "Economy" }, { id: "science", label: "Science" }, { id: "expired", label: "Expired" }].map(cat => (
              <button key={cat.id} onClick={() => setActiveCategory(cat.id)} style={{
                padding: "7px 16px", borderRadius: 20, border: "none", cursor: "pointer",
                background: activeCategory === cat.id ? (cat.id === "expired" ? "rgba(156,163,175,0.15)" : COLORS.accentGlow) : "rgba(255,255,255,0.03)",
                color: activeCategory === cat.id ? (cat.id === "expired" ? "#9CA3AF" : COLORS.accentLight) : COLORS.textDim,
                fontSize: 13, fontWeight: activeCategory === cat.id ? 600 : 450,
                fontFamily: "'DM Sans', sans-serif", whiteSpace: "nowrap", transition: "all 0.15s",
              }}>{cat.label}</button>
            ))}
          </div>
        </div>

        {/* Hot Markets Header */}
        <div style={{ padding: "8px 14px 10px", display: "flex", justifyContent: "space-between", alignItems: "center" }}>
          <span style={{ fontSize: 15, fontWeight: 700, fontFamily: "'Outfit', sans-serif" }}>Hot Markets</span>
          <button onClick={openExploreGeneral} style={{ background: "none", border: "none", color: COLORS.accentLight, fontSize: 12, fontWeight: 600, cursor: "pointer" }}>See all →</button>
        </div>

        {/* Market Cards Feed (1-col) - sorted by volume */}
        <div style={{ padding: "0 14px", display: "flex", flexDirection: "column", gap: 10, maxHeight: "740px", overflowY: "auto" }}>
          {[...filteredMarkets].sort((a: any, b: any) => parseFloat(b.volume) - parseFloat(a.volume)).map((market: any, i: number) => {
            const isUp = market.change24h >= 0;
            const yesPct = Math.round(market.yesPrice * 100);
            const status = String(market.status ?? "").toUpperCase();
            const phase = String((market as any).phase ?? "").toUpperCase();
            const isLiveCategory = String(market.category ?? "").toUpperCase() === "LIVE";
            const isLiveOpen = isLiveCategory && phase !== "FINISHED" && phase !== "SETTLED" && status !== "SETTLED" && status !== "CANCELLED";
            const isClosed = !isLiveOpen && !!market.status && market.status !== "BETTING";
            const bettingTimeLeft = getBettingTimeLeftLabel(market.bettingEndAt, market);
            const bettingEndsAtLabel = market?.bettingEndAt
              ? new Date(String(market.bettingEndAt)).toISOString().replace("T", " ").slice(5, 16)
              : null;
            const closedLabel =
              market.status === "VOTING" ? "Voting" :
              market.status === "BREAK"  ? "Preparing" :
              "Closed";
            return (
              <div key={market.id} onClick={() => !isClosed && handleBet(market, isUp ? "YES" : "NO")} style={{
                background: COLORS.surface, border: `1px solid ${COLORS.border}`,
                borderRadius: 14, padding: 16, cursor: isClosed ? "default" : "pointer",
                transition: "all 0.2s", opacity: isClosed ? 0.7 : 1,
                animation: `fadeUp 0.3s ease ${i * 0.03}s both`,
              }}>
                {/* Header */}
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 8 }}>
                  <div style={{ display: "flex", alignItems: "center", gap: 6 }}>
                    <span style={{ fontSize: 9, padding: "2px 7px", borderRadius: 4, background: COLORS.accentGlow, color: COLORS.accentLight, fontWeight: 600, textTransform: "uppercase" }}>{market.category}</span>
                    {isClosed && <span style={{ fontSize: 9, padding: "2px 7px", borderRadius: 4, background: "rgba(156,163,175,0.15)", color: "#9CA3AF", fontWeight: 600, textTransform: "uppercase" }}>Closed</span>}
                    {market.creator && <span style={{ fontSize: 10, color: COLORS.textDim }}>@{market.creator}</span>}
                  </div>
                  <span style={{ fontSize: 11, color: COLORS.textDim }}>⏱ {market.timeLeft}</span>
                </div>
                {/* Title */}
                <div style={{ fontSize: 15, fontWeight: 600, color: COLORS.text, lineHeight: 1.4, marginBottom: 12 }}>{market.title}</div>
                {(bettingTimeLeft || bettingEndsAtLabel) && (
                  <div style={{
                    marginTop: -6,
                    marginBottom: 10,
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                    gap: 8,
                    background: "rgba(255,255,255,0.03)",
                    border: `1px solid ${COLORS.border}`,
                    borderRadius: 8,
                    padding: "6px 8px",
                  }}>
                    <span style={{ fontSize: 10, color: COLORS.accentLight, fontFamily: "'JetBrains Mono', monospace", fontWeight: 700 }}>
                      {bettingTimeLeft || "Betting closed"}
                    </span>
                    {bettingEndsAtLabel && (
                      <span style={{ fontSize: 10, color: COLORS.textDim, fontFamily: "'JetBrains Mono', monospace" }}>
                        {bettingEndsAtLabel}
                      </span>
                    )}
                  </div>
                )}
                {/* Price + Change + Volume */}
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-end" }}>
                  <div style={{ display: "flex", alignItems: "baseline", gap: 8 }}>
                    <span style={{ fontSize: 26, fontWeight: 700, fontFamily: "'JetBrains Mono', monospace", color: COLORS.green }}>{yesPct}%</span>
                    <span style={{ fontSize: 12, fontWeight: 600, color: isUp ? COLORS.green : COLORS.red, fontFamily: "'JetBrains Mono', monospace" }}>{isUp ? "+" : ""}{market.change24h}%</span>
                  </div>
                  <div style={{ textAlign: "right" }}>
                    <div style={{ fontSize: 12, fontWeight: 600, fontFamily: "'JetBrains Mono', monospace", color: COLORS.textMuted }}>${market.volume}</div>
                    <div style={{ fontSize: 10, color: COLORS.textDim }}>{market.bettors?.toLocaleString()} bettors</div>
                  </div>
                </div>
                {/* Quick bet buttons or closed */}
                {isClosed ? (
                  <div style={{
                    marginTop: 12, padding: "9px 0", borderRadius: 10, textAlign: "center",
                    background: "rgba(156,163,175,0.08)", border: "1.5px solid rgba(156,163,175,0.2)",
                    fontSize: 13, fontWeight: 600, color: "#6B7280",
                  }}>{closedLabel}</div>
                ) : (
                  <div style={{ display: "flex", gap: 8, marginTop: 12 }}>
                    <button onClick={e => { e.stopPropagation(); handleBet(market, "YES"); }} style={{
                      flex: 1, padding: "9px 0", borderRadius: 10, cursor: "pointer",
                      background: "rgba(34,197,94,0.06)", border: "1.5px solid rgba(34,197,94,0.2)",
                      color: COLORS.green, fontSize: 13, fontWeight: 700,
                    }}>Yes {yesPct}¢</button>
                    <button onClick={e => { e.stopPropagation(); handleBet(market, "NO"); }} style={{
                      flex: 1, padding: "9px 0", borderRadius: 10, cursor: "pointer",
                      background: "rgba(239,68,68,0.06)", border: "1.5px solid rgba(239,68,68,0.2)",
                      color: COLORS.red, fontSize: 13, fontWeight: 700,
                    }}>No {100 - yesPct}¢</button>
                  </div>
                )}
              </div>
            );
          })}

          {filteredMarkets.length === 0 && (
            <div style={{ textAlign: "center", padding: "40px 20px", color: COLORS.textDim }}>
              <div style={{ fontSize: 15, fontWeight: 500 }}>No markets in this category yet</div>
            </div>
          )}
        </div>
      </div>

      {/* ── Footer - Desktop ─── */}
      <footer className="desktop-only" style={{
        borderTop: `1px solid ${COLORS.border}`,
        padding: "32px 24px", marginTop: 40, display: "flex",
      }}>
        <div style={{
          maxWidth: 1320, margin: "0 auto", width: "100%",
          display: "flex", justifyContent: "space-between", alignItems: "center",
          flexWrap: "wrap", gap: 16,
        }}>
          <div>
            <Logo />
            <div style={{ fontSize: 12, color: COLORS.textDim, marginTop: 8 }}>
              Separating signal from noise in prediction markets.
            </div>
          </div>
          <div style={{ display: "flex", gap: 24 }}>
            {["About", "Docs", "API", "Careers", "Blog"].map(link => (
              <a key={link} href="#" style={{
                fontSize: 12, color: COLORS.textDim, textDecoration: "none",
                transition: "color 0.2s",
              }}
              onMouseEnter={e => (e.currentTarget as HTMLElement).style.color = COLORS.text}
              onMouseLeave={e => (e.currentTarget as HTMLElement).style.color = COLORS.textDim}
              >{link}</a>
            ))}
          </div>
          <div style={{ display: "flex", gap: 10, alignItems: "center" }}>
            {/* X */}
            <a href="#" style={{ width: 32, height: 32, borderRadius: 8, background: COLORS.surfaceHover, border: `1px solid ${COLORS.border}`, display: "flex", alignItems: "center", justifyContent: "center", color: COLORS.textDim, cursor: "pointer", transition: "border-color 0.2s" }}>
              <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor"><path d="M18.244 2.25h3.308l-7.227 8.26 8.502 11.24H16.17l-5.214-6.817L4.99 21.75H1.68l7.73-8.835L1.254 2.25H8.08l4.713 6.231zm-1.161 17.52h1.833L7.084 4.126H5.117z"/></svg>
            </a>
            {/* Discord */}
            <a href="#" style={{ width: 32, height: 32, borderRadius: 8, background: COLORS.surfaceHover, border: `1px solid ${COLORS.border}`, display: "flex", alignItems: "center", justifyContent: "center", color: COLORS.textDim, cursor: "pointer" }}>
              <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor"><path d="M20.317 4.37a19.791 19.791 0 00-4.885-1.515.074.074 0 00-.079.037c-.21.375-.444.864-.608 1.25a18.27 18.27 0 00-5.487 0 12.64 12.64 0 00-.617-1.25.077.077 0 00-.079-.037A19.736 19.736 0 003.677 4.37a.07.07 0 00-.032.027C.533 9.046-.32 13.58.099 18.057a.082.082 0 00.031.057 19.9 19.9 0 005.993 3.03.078.078 0 00.084-.028c.462-.63.874-1.295 1.226-1.994a.076.076 0 00-.041-.106 13.107 13.107 0 01-1.872-.892.077.077 0 01-.008-.128 10.2 10.2 0 00.372-.292.074.074 0 01.077-.01c3.928 1.793 8.18 1.793 12.062 0a.074.074 0 01.078.01c.12.098.246.198.373.292a.077.077 0 01-.006.127 12.299 12.299 0 01-1.873.892.077.077 0 00-.041.107c.36.698.772 1.362 1.225 1.993a.076.076 0 00.084.028 19.839 19.839 0 006.002-3.03.077.077 0 00.032-.054c.5-5.177-.838-9.674-3.549-13.66a.061.061 0 00-.031-.03z"/></svg>
            </a>
            {/* Instagram */}
            <a href="#" style={{ width: 32, height: 32, borderRadius: 8, background: COLORS.surfaceHover, border: `1px solid ${COLORS.border}`, display: "flex", alignItems: "center", justifyContent: "center", color: COLORS.textDim, cursor: "pointer" }}>
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round"><rect x="2" y="2" width="20" height="20" rx="5"/><circle cx="12" cy="12" r="5"/></svg>
            </a>
            {/* TikTok */}
            <a href="#" style={{ width: 32, height: 32, borderRadius: 8, background: COLORS.surfaceHover, border: `1px solid ${COLORS.border}`, display: "flex", alignItems: "center", justifyContent: "center", color: COLORS.textDim, cursor: "pointer" }}>
              <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor"><path d="M19.59 6.69a4.83 4.83 0 01-3.77-4.25V2h-3.45v13.67a2.89 2.89 0 01-2.88 2.5 2.89 2.89 0 01-2.89-2.89 2.89 2.89 0 012.89-2.89c.28 0 .54.04.79.1v-3.5a6.37 6.37 0 00-.79-.05A6.34 6.34 0 003.15 15.2a6.34 6.34 0 0010.86 4.43v-7.15a8.16 8.16 0 005.58 2.17V11.2a4.85 4.85 0 01-2.83-.9v5.18h1.84V6.69h-.01z"/></svg>
            </a>
            {/* Theme toggle */}
            <button onClick={toggleTheme} style={{
              width: 32, height: 32, borderRadius: 8,
              background: COLORS.surfaceHover, border: `1px solid ${COLORS.border}`,
              display: "flex", alignItems: "center", justifyContent: "center",
              color: COLORS.textDim, cursor: "pointer", transition: "all 0.2s",
            }}>
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
                {isDark ? <><circle cx="12" cy="12" r="5"/><path d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42"/></> : <path d="M21 12.79A9 9 0 1111.21 3a7 7 0 009.79 9.79z"/>}
              </svg>
            </button>
          </div>
        </div>
      </footer>

      {/* ── Mobile Bottom Tab Bar ─── */}
      {mobileTabBar}

      {/* Toast Popup */}
      {toast && (
        <div style={{
          position: "fixed", top: 24, left: "50%", transform: "translateX(-50%)",
          zIndex: 1100, animation: "modalSlideUp 0.25s ease both",
        }}>
          <div style={{
            background: toast.type === "error" ? "rgba(239,68,68,0.95)" : "rgba(34,197,94,0.95)",
            color: "white", padding: "12px 24px", borderRadius: 12,
            fontSize: 14, fontWeight: 600, fontFamily: "'DM Sans', sans-serif",
            boxShadow: "0 8px 32px rgba(0,0,0,0.4)",
            backdropFilter: "blur(8px)",
            display: "flex", alignItems: "center", gap: 10,
            whiteSpace: "nowrap",
          }}>
            {toast.type === "error" ? (
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="2" strokeLinecap="round"><circle cx="12" cy="12" r="10"/><path d="M15 9l-6 6M9 9l6 6"/></svg>
            ) : (
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="2" strokeLinecap="round"><circle cx="12" cy="12" r="10"/><polyline points="16 10 11 15 8 12"/></svg>
            )}
            {toast.message}
          </div>
        </div>
      )}

      {/* Auth Modals */}
      {authModalsJSX}

      {/* MyPage Modal */}
      {myPageModalJSX}
    </div>
  );
}
