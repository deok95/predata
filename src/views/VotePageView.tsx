import React, { useState, useEffect } from "react";
import { COLORS } from "../theme";
import AvatarCircle from "../components/AvatarCircle";
import { EXPLORE_CATEGORIES } from "../data/exploreData";
import { useVoteFeed, useFollow } from "../hooks/useApi";
import { voteApi, questionApi, isLoggedIn, getMemberId } from "../services/api";
import type { VoteQuestion, VoteChoice, CommentItem } from "../types/domain";

type ExploreCategory = (typeof EXPLORE_CATEGORIES)[number];
type ExploreSubCategory = NonNullable<ExploreCategory["subs"]>[number];

type VotePageViewProps = {
  onBack?: () => void;
  onSubmit?: () => void;
  showToast?: (message: string, type?: string) => void;
  onRequireAuth?: (message?: string) => void;
  focusQuestionId?: string | number | null;
  onFocusHandled?: () => void;
};

type VoteResultQuestion = VoteQuestion & {
  settlementMode: "VOTE_RESULT";
  voteVisibility?: string | null;
  votingPhase?: string | null;
};

const isVoteResultQuestion = (q: VoteQuestion): q is VoteResultQuestion =>
  q.settlementMode === "VOTE_RESULT";

const isVoteBreakdownHidden = (q: VoteQuestion): boolean => {
  if (!isVoteResultQuestion(q)) return false;
  if (q.voteVisibility) return q.voteVisibility !== "REVEALED";
  if (q.votingPhase) return q.votingPhase !== "VOTING_REVEAL_CLOSED";
  return q.hiddenVoteResults === true;
};

function VotePageView({
  onBack = () => {},
  onSubmit = () => {},
  showToast = () => {},
  onRequireAuth,
  focusQuestionId = null,
  onFocusHandled = () => {},
}: VotePageViewProps = {}) {
  const [activeCat, setActiveCat] = useState("trending");
  const [activeSub, setActiveSub] = useState<string | null>(null);
  const [activeSub2, setActiveSub2] = useState<string | null>(null);
  const [timeFilter, setTimeFilter] = useState("1D");
  const [feedMode, setFeedMode] = useState("foryou");
  const [search, setSearch] = useState("");
  const [sortBy, setSortBy] = useState("hot");
  const [votes, setVotes] = useState<Record<string | number, VoteChoice>>({});
  const [voteCredits, setVoteCredits] = useState<number | null>(null);
  const [confirmPopup, setConfirmPopup] = useState<{ question: VoteQuestion; choice: VoteChoice } | null>(null);
  const [followState, setFollowState] = useState<Record<string, boolean>>({});
  const [followLoading, setFollowLoading] = useState<Record<string, boolean>>({});
  const [commentOpen, setCommentOpen] = useState<string | number | null>(null);
  const [commentText, setCommentText] = useState("");
  const [allComments, setAllComments] = useState<Record<string | number, CommentItem[]>>({});
  const { follow, unfollow } = useFollow();
  const selectedVoteWindowType = timeFilter === "6H" ? "H6" : timeFilter === "1D" ? "D1" : "D3";
  const [nowMs, setNowMs] = useState(() => Date.now());

  const promptAuth = (message = "Please log in or sign up to continue.") => {
    if (onRequireAuth) onRequireAuth(message);
    else showToast(message, "error");
  };
  const isAuthRequiredError = (err: unknown) => {
    const message = err instanceof Error ? err.message : String(err || "");
    const m = message.toLowerCase();
    return m.includes("401") || m.includes("unauthorized") || m.includes("authentication");
  };
  const extractHttpStatus = (err: unknown): string | null => {
    const message = err instanceof Error ? err.message : String(err || "");
    const m = message.match(/\b(4\d{2}|5\d{2})\b/);
    return m ? m[1] : null;
  };

  // Fetch remaining daily votes from backend on mount
  useEffect(() => {
    if (!isLoggedIn()) return;
    voteApi.dailyRemaining()
      .then((res: any) => {
        const val = res?.data?.remainingVotes ?? res?.remainingVotes;
        if (typeof val === "number") setVoteCredits(val);
      })
      .catch(() => {});
  }, []);

  useEffect(() => {
    const timer = window.setInterval(() => setNowMs(Date.now()), 60_000);
    return () => window.clearInterval(timer);
  }, []);

  // API: fetch vote feed
  const feedModeParam = feedMode === "following" ? "following" : feedMode === "top10" ? "top10" : "foryou";
  const { questions: apiQuestions, loading, castVote, refresh } = useVoteFeed({
    category: activeCat !== "trending" ? activeCat : undefined,
    sort: sortBy,
    window: selectedVoteWindowType,
    mode: feedModeParam,
    limit: feedMode === "top10" ? 10 : 50,
  });

  // Use API data, fallback gracefully
  const ALL_VOTE_QUESTIONS: VoteQuestion[] = apiQuestions.length > 0 ? apiQuestions : [];

  const followKey = (q: VoteQuestion) => String(q.submitterId ?? q.submitter);
  const isFollowingSubmitter = (q: VoteQuestion) => {
    const key = followKey(q);
    return followState[key] ?? Boolean(q.following);
  };

  useEffect(() => {
    if (ALL_VOTE_QUESTIONS.length === 0) return;
    setFollowState((prev) => {
      const next = { ...prev };
      ALL_VOTE_QUESTIONS.forEach((q) => {
        const key = followKey(q);
        if (next[key] === undefined) next[key] = Boolean(q.following);
      });
      return next;
    });
  }, [ALL_VOTE_QUESTIONS]);

  // Load comments from API when opening
  const loadComments = async (questionId: string | number) => {
    try {
      const comments = await questionApi.getComments(questionId);
      setAllComments(prev => ({ ...prev, [questionId]: comments || [] }));
    } catch (e) {
      // Keep existing comments on error
    }
  };
  const commentQuestion = commentOpen ? ALL_VOTE_QUESTIONS.find((q: VoteQuestion) => q.id === commentOpen) : null;
  const currentComments = commentOpen ? (allComments[commentOpen] || []) : [];

  const handleAddComment = async () => {
    if (!commentText.trim() || !commentOpen) return;
    if (!isLoggedIn()) {
      promptAuth("Please log in or sign up to comment.");
      return;
    }
    const text = commentText.trim();
    setCommentText("");
    try {
      await questionApi.postComment(commentOpen, text);
      await loadComments(commentOpen);
    } catch (e: unknown) {
      if (isAuthRequiredError(e)) {
        promptAuth("Please log in or sign up to comment.");
      }
      setCommentText(text); // restore on error
    }
  };

  const mainCategories = [{ id: "trending", label: "Trending", subs: [] }, ...EXPLORE_CATEGORIES.filter(c => c.id !== "trending" && c.id !== "live")];
  const currentCat: ExploreCategory | undefined = EXPLORE_CATEGORIES.find(c => c.id === activeCat);
  const currentSub = currentCat?.subs?.find(s => s.id === activeSub);
  const timeOptions = [{ id: "6H", label: "6H", hours: 6 }, { id: "1D", label: "1D", hours: 24 }, { id: "3D", label: "3D", hours: 72 }];

  let questions: VoteQuestion[] = [...ALL_VOTE_QUESTIONS];
  questions = questions.filter((q: VoteQuestion) => {
    if (!q.votingEndAt) return true;
    const ts = new Date(q.votingEndAt).getTime();
    if (Number.isNaN(ts)) return true;
    return ts > nowMs;
  });
  if (activeCat !== "trending") {
    questions = questions.filter((q: VoteQuestion) =>
      String(q.category || "").toLowerCase() === activeCat.toLowerCase()
    );
  }
  if (activeSub) questions = questions.filter((q: VoteQuestion) => q.sub === activeSub);
  if (activeSub2) questions = questions.filter((q: VoteQuestion) => q.sub2 === activeSub2);
  if (search) { const s = search.toLowerCase(); questions = questions.filter((q: VoteQuestion) => q.title.toLowerCase().includes(s) || q.tags.some((t: string) => t.includes(s))); }

  // Desktop sort
  if (sortBy === "hot" || activeCat === "trending") questions.sort((a: VoteQuestion, b: VoteQuestion) => (b.totalVotes ?? (b.yesVotes + b.noVotes)) - (a.totalVotes ?? (a.yesVotes + a.noVotes)));
  else if (sortBy === "new") questions.sort((a: VoteQuestion, b: VoteQuestion) => a.age - b.age);
  else if (sortBy === "controversial") questions.sort((a: VoteQuestion, b: VoteQuestion) => {
    const aYes = isVoteBreakdownHidden(a) ? 0 : a.yesVotes;
    const aNo = isVoteBreakdownHidden(a) ? 0 : a.noVotes;
    const bYes = isVoteBreakdownHidden(b) ? 0 : b.yesVotes;
    const bNo = isVoteBreakdownHidden(b) ? 0 : b.noVotes;
    const rA = Math.min(aYes, aNo) / Math.max(aYes || 1, aNo || 1);
    const rB = Math.min(bYes, bNo) / Math.max(bYes || 1, bNo || 1);
    return rB - rA;
  });

  // Mobile feed mode override
  const mobileQuestions = (() => {
    let mq = [...questions];
    if (feedMode === "following") {
      mq = mq.filter((q: VoteQuestion) => isFollowingSubmitter(q));
      mq.sort((a: VoteQuestion, b: VoteQuestion) => a.age - b.age);
    } else if (feedMode === "top10") {
      mq.sort((a: VoteQuestion, b: VoteQuestion) => (b.totalVotes ?? (b.yesVotes + b.noVotes)) - (a.totalVotes ?? (a.yesVotes + a.noVotes)));
      mq = mq.slice(0, 10);
    } else {
      mq.sort((a: VoteQuestion, b: VoteQuestion) => {
        const af = isFollowingSubmitter(a) ? 1 : 0;
        const bf = isFollowingSubmitter(b) ? 1 : 0;
        if (af !== bf) return bf - af;
        return (b.likes + (b.totalVotes ?? (b.yesVotes + b.noVotes))) - (a.likes + (a.totalVotes ?? (a.yesVotes + a.noVotes)));
      });
    }
    return mq;
  })();

  const top10 = questions.slice(0, 10);

  useEffect(() => {
    if (!focusQuestionId) return;
    const id = `vote-question-${focusQuestionId}`;
    const timer = window.setTimeout(() => {
      const target = document.getElementById(id);
      if (target) {
        target.scrollIntoView({ behavior: "smooth", block: "center" });
        (target as HTMLElement).style.outline = `2px solid ${COLORS.accent}`;
        (target as HTMLElement).style.outlineOffset = "2px";
        window.setTimeout(() => {
          (target as HTMLElement).style.outline = "";
          (target as HTMLElement).style.outlineOffset = "";
        }, 1200);
      }
      onFocusHandled();
    }, 120);
    return () => window.clearTimeout(timer);
  }, [focusQuestionId, mobileQuestions.length, onFocusHandled]);

  const getTotalVotes = (q: VoteQuestion) => q.totalVotes ?? (q.yesVotes + q.noVotes);
  const getYesPct = (q: VoteQuestion) => {
    const total = Math.max(getTotalVotes(q), 1);
    return Math.round(((q.yesVotes ?? 0) / total) * 100);
  };

  const getVotingTimeLeft = (q: VoteQuestion) => {
    if (!q.votingEndAt) return null;
    const endAt = new Date(q.votingEndAt).getTime();
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

  const handleVoteClick = (q: VoteQuestion, choice: VoteChoice) => {
    if (votes[q.id]) return;
    if (!isLoggedIn()) {
      promptAuth("Please log in or sign up to vote.");
      return;
    }
    if (voteCredits !== null && voteCredits <= 0) { showToast("No votes remaining for today."); return; }
    setConfirmPopup({ question: q, choice });
  };
  const confirmVote = async () => {
    if (!confirmPopup) return;
    if (!isLoggedIn()) {
      promptAuth("Please log in or sign up to vote.");
      setConfirmPopup(null);
      return;
    }
    try {
      const res: any = await castVote(confirmPopup.question.id, confirmPopup.choice);
      setVotes(prev => ({ ...prev, [confirmPopup.question.id]: confirmPopup.choice }));
      const remaining = res?.data?.remainingDailyVotes ?? res?.remainingDailyVotes;
      if (typeof remaining === "number") setVoteCredits(remaining);
      else setVoteCredits(prev => (prev !== null ? prev - 1 : null));
    } catch (err: unknown) {
      if (isAuthRequiredError(err)) {
        promptAuth("Please log in or sign up to vote.");
        setConfirmPopup(null);
        return;
      }
      const message = err instanceof Error ? err.message : "Vote failed";
      if (showToast) showToast(message, "error");
    }
    setConfirmPopup(null);
  };
  const toggleFollow = async (q: VoteQuestion) => {
    if (!isLoggedIn()) {
      promptAuth("Please log in or sign up to follow.");
      return;
    }
    if (!q.submitterId) {
      showToast("Cannot follow: submitter ID not found.", "error");
      return;
    }
    const myId = getMemberId();
    if (myId && String(myId) === String(q.submitterId)) {
      showToast("You cannot follow yourself.", "error");
      return;
    }

    const key = followKey(q);
    const prev = isFollowingSubmitter(q);
    setFollowLoading((s) => ({ ...s, [key]: true }));
    setFollowState((s) => ({ ...s, [key]: !prev }));
    try {
      if (prev) await unfollow(q.submitterId);
      else await follow(q.submitterId);
      await refresh();
      showToast(prev ? "Unfollowed successfully." : "Followed successfully.", "success");
    } catch (err: unknown) {
      setFollowState((s) => ({ ...s, [key]: prev }));
      if (isAuthRequiredError(err)) {
        promptAuth("Please log in or sign up to follow.");
        return;
      }
      const status = extractHttpStatus(err);
      const message = err instanceof Error ? err.message : "Follow failed";
      showToast(status ? `Follow failed (${status}): ${message}` : `Follow failed: ${message}`, "error");
    } finally {
      setFollowLoading((s) => ({ ...s, [key]: false }));
    }
  };

  // ── Category rows (rendered as JSX, not a component) ──
  const catRowsJSX = (
    <div style={{ padding: "8px 0" }}>
      <div style={{ display: "flex", gap: 4, overflowX: "auto", paddingBottom: 4, msOverflowStyle: "none", scrollbarWidth: "none" }}>
        {mainCategories.map(cat => (
          <button key={cat.id} onClick={() => { setActiveCat(cat.id); setActiveSub(null); setActiveSub2(null); }} style={{
            padding: "6px 14px", borderRadius: 20, border: "none", cursor: "pointer",
            background: activeCat === cat.id ? COLORS.accentGlow : "rgba(255,255,255,0.03)",
            color: activeCat === cat.id ? COLORS.accentLight : COLORS.textDim,
            fontSize: 12, fontWeight: activeCat === cat.id ? 600 : 450,
            fontFamily: "'DM Sans', sans-serif", whiteSpace: "nowrap", transition: "all 0.15s",
          }}>{cat.label}</button>
        ))}
      </div>
      {(currentCat?.subs?.length ?? 0) > 0 && (
        <div style={{ display: "flex", gap: 4, overflowX: "auto", paddingTop: 4, paddingBottom: 2 }}>
          <button onClick={() => { setActiveSub(null); setActiveSub2(null); }} style={{
            padding: "4px 10px", borderRadius: 5, border: "none", cursor: "pointer",
            background: !activeSub ? "rgba(255,255,255,0.06)" : "transparent",
            color: !activeSub ? COLORS.text : COLORS.textDim,
            fontSize: 11, fontWeight: 500, fontFamily: "'DM Sans', sans-serif", whiteSpace: "nowrap",
          }}>All</button>
          {(currentCat?.subs ?? []).map((sub: ExploreSubCategory) => (
            <button key={sub.id} onClick={() => { setActiveSub(sub.id); setActiveSub2(null); }} style={{
              padding: "4px 10px", borderRadius: 5, border: "none", cursor: "pointer",
              background: activeSub === sub.id ? "rgba(255,255,255,0.06)" : "transparent",
              color: activeSub === sub.id ? COLORS.text : COLORS.textDim,
              fontSize: 11, fontWeight: 500, fontFamily: "'DM Sans', sans-serif", whiteSpace: "nowrap",
            }}>{sub.label}</button>
          ))}
        </div>
      )}
      {(currentSub?.subs2?.length ?? 0) > 0 && (
        <div style={{ display: "flex", gap: 4, overflowX: "auto", paddingTop: 2 }}>
          <button onClick={() => setActiveSub2(null)} style={{
            padding: "3px 8px", borderRadius: 4, border: `1px solid ${!activeSub2 ? COLORS.accent : COLORS.border}`,
            cursor: "pointer", background: !activeSub2 ? COLORS.accentGlow : "transparent",
            color: !activeSub2 ? COLORS.accentLight : COLORS.textDim,
            fontSize: 10, fontWeight: 500, whiteSpace: "nowrap",
          }}>All</button>
          {(currentSub?.subs2 ?? []).map((s2: string) => (
            <button key={s2} onClick={() => setActiveSub2(s2)} style={{
              padding: "3px 8px", borderRadius: 4, border: `1px solid ${activeSub2 === s2 ? COLORS.accent : COLORS.border}`,
              cursor: "pointer", background: activeSub2 === s2 ? COLORS.accentGlow : "transparent",
              color: activeSub2 === s2 ? COLORS.accentLight : COLORS.textDim,
              fontSize: 10, fontWeight: 500, whiteSpace: "nowrap",
            }}>{s2}</button>
          ))}
        </div>
      )}
    </div>
  );

  // ── Vote bar render helper (not a component) ──
  const renderVoteBar = (q: VoteQuestion) => {
    const total = getTotalVotes(q);
    const hidden = isVoteBreakdownHidden(q);
    const yesPct = hidden ? 50 : getYesPct(q);
    const voted = votes[q.id];
    return (
      <div>
        {hidden ? (
          <div style={{ fontSize: 12, color: COLORS.textDim, marginBottom: 8, textAlign: "center" }}>
            Vote breakdown hidden until betting close
          </div>
        ) : (
          <>
            <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 4 }}>
              <span style={{ fontSize: 12, fontWeight: 600, color: COLORS.green, fontFamily: "'JetBrains Mono', monospace" }}>Yes {yesPct}%</span>
              <span style={{ fontSize: 12, fontWeight: 600, color: COLORS.red, fontFamily: "'JetBrains Mono', monospace" }}>No {100 - yesPct}%</span>
            </div>
            <div style={{ height: 6, borderRadius: 3, background: COLORS.redBg, overflow: "hidden", marginBottom: 8 }}>
              <div style={{ height: "100%", width: `${yesPct}%`, borderRadius: 3, background: `linear-gradient(90deg, ${COLORS.green}, rgba(34,197,94,0.6))`, transition: "width 0.3s" }} />
            </div>
          </>
        )}
        {voted ? (
          <div style={{
            display: "flex", alignItems: "center", justifyContent: "center", gap: 6,
            padding: "9px 0", borderRadius: 10,
            background: voted === "YES" ? COLORS.greenBg : COLORS.redBg,
          }}>
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke={voted === "YES" ? COLORS.green : COLORS.red} strokeWidth="2.5"><polyline points="20 6 9 17 4 12"/></svg>
            <span style={{ fontSize: 13, fontWeight: 600, color: voted === "YES" ? COLORS.green : COLORS.red }}>Voted {voted}</span>
          </div>
        ) : (
          <div style={{ display: "flex", gap: 8 }}>
            {(["YES", "NO"] as const).map((c: VoteChoice) => (
              <button key={c} onClick={() => handleVoteClick(q, c)} style={{
                flex: 1, padding: "9px 0", borderRadius: 10, cursor: "pointer",
                background: c === "YES" ? "rgba(34,197,94,0.06)" : "rgba(239,68,68,0.06)",
                border: `1.5px solid ${c === "YES" ? "rgba(34,197,94,0.2)" : "rgba(239,68,68,0.2)"}`,
                color: c === "YES" ? COLORS.green : COLORS.red,
                fontSize: 14, fontWeight: 700, fontFamily: "'DM Sans', sans-serif", transition: "all 0.15s",
              }}
              onMouseEnter={e => { (e.currentTarget as HTMLElement).style.background = c === "YES" ? COLORS.green : COLORS.red; (e.currentTarget as HTMLElement).style.color = "white"; }}
              onMouseLeave={e => { (e.currentTarget as HTMLElement).style.background = c === "YES" ? "rgba(34,197,94,0.06)" : "rgba(239,68,68,0.06)"; (e.currentTarget as HTMLElement).style.color = c === "YES" ? COLORS.green : COLORS.red; }}
              >{c === "YES" ? "Yes" : "No"}</button>
            ))}
          </div>
        )}
        <div style={{ fontSize: 11, color: COLORS.textDim, marginTop: 5, fontFamily: "'JetBrains Mono', monospace", textAlign: "center" }}>
          {total.toLocaleString()} votes
        </div>
      </div>
    );
  };

  return (
    <div style={{ minHeight: "100vh", background: COLORS.bg }}>
      {/* Top bar */}
      <div className="mobile-only" style={{
        position: "sticky", top: 0, zIndex: 50,
        background: `${COLORS.bg}f2`, backdropFilter: "blur(12px)",
        borderBottom: `1px solid ${COLORS.border}`,
        padding: "10px 24px", maxWidth: 1320, margin: "0 auto",
      }}>
        <div style={{ display: "flex", alignItems: "center", gap: 14 }}>
          <button onClick={onBack} style={{
            background: "none", border: "none", cursor: "pointer",
            color: COLORS.textMuted, display: "flex", alignItems: "center",
          }}
          onMouseEnter={e => e.currentTarget.style.color = COLORS.text}
          onMouseLeave={e => e.currentTarget.style.color = COLORS.textMuted}
          >
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M19 12H5"/><polyline points="12 19 5 12 12 5"/></svg>
          </button>
          <div style={{
            flex: 1, maxWidth: 420, display: "flex", alignItems: "center", gap: 8,
            background: COLORS.surface, border: `1px solid ${COLORS.border}`, borderRadius: 10, padding: "6px 12px",
          }}>
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke={COLORS.textDim} strokeWidth="2"><circle cx="11" cy="11" r="8"/><path d="M21 21l-4.35-4.35"/></svg>
            <input type="text" placeholder="Search questions..." value={search} onChange={e => setSearch(e.target.value)}
              style={{ flex: 1, background: "none", border: "none", outline: "none", color: COLORS.text, fontSize: 13, fontFamily: "'DM Sans', sans-serif" }} />
          </div>
          <div style={{ display: "flex", alignItems: "center", gap: 10, marginLeft: "auto" }}>
            {voteCredits !== null && (
              <div style={{
                fontSize: 11, fontWeight: 600, padding: "3px 8px", borderRadius: 5,
                background: voteCredits > 0 ? COLORS.accentGlow : "rgba(239,68,68,0.1)",
                color: voteCredits > 0 ? COLORS.accentLight : COLORS.red,
                fontFamily: "'JetBrains Mono', monospace",
              }}>{voteCredits} vote{voteCredits !== 1 ? "s" : ""}</div>
            )}
            <button onClick={onSubmit} style={{
              padding: "6px 12px", borderRadius: 8,
              background: `linear-gradient(135deg, ${COLORS.accent}, #5B21B6)`,
              border: "none", color: "white", cursor: "pointer",
              fontSize: 11, fontWeight: 600, display: "flex", alignItems: "center", gap: 4,
            }}>
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5"><path d="M12 5v14M5 12h14"/></svg>
              Submit
            </button>
          </div>
        </div>
      </div>

      {/* ════════════════════════════════════════════
           UNIFIED FEED LAYOUT
         ════════════════════════════════════════════ */}
      <div style={{ maxWidth: 680, margin: "0 auto" }}>
        {/* Feed Mode Tabs */}
        <div style={{
          display: "flex", borderBottom: `1px solid ${COLORS.border}`,
          position: "sticky", top: 49, zIndex: 40, background: COLORS.bg,
        }}>
          {[{ id: "foryou", label: "For You" }, { id: "following", label: "Following" }, { id: "top10", label: "Top 10" }].map(m => (
            <button key={m.id} onClick={() => setFeedMode(m.id)} style={{
              flex: 1, padding: "11px 0", border: "none", cursor: "pointer", background: "transparent",
              color: feedMode === m.id ? COLORS.text : COLORS.textDim,
              fontSize: 13, fontWeight: feedMode === m.id ? 700 : 500,
              borderBottom: feedMode === m.id ? `2px solid ${COLORS.accent}` : "2px solid transparent",
            }}>{m.label}</button>
          ))}
        </div>

        <div style={{ padding: "0 14px" }}>
          {catRowsJSX}
          {/* Time pills */}
          <div style={{ display: "flex", gap: 4, paddingBottom: 8 }}>
            {timeOptions.map(t => (
              <button key={t.id} onClick={() => setTimeFilter(t.id)} style={{
                padding: "4px 10px", borderRadius: 5, border: `1px solid ${timeFilter === t.id ? COLORS.border : "transparent"}`,
                cursor: "pointer", background: timeFilter === t.id ? COLORS.surface : "transparent",
                color: timeFilter === t.id ? COLORS.text : COLORS.textDim,
                fontSize: 11, fontWeight: timeFilter === t.id ? 600 : 450, fontFamily: "'JetBrains Mono', monospace",
              }}>{t.label}</button>
            ))}
          </div>
        </div>

        {/* Feed */}
        <div style={{ display: "flex", flexDirection: "column", maxHeight: "960px", overflowY: "auto" }}>
          {mobileQuestions.map((q: VoteQuestion, i: number) => {
            const key = followKey(q);
            const isFollowing = isFollowingSubmitter(q);
            const isFollowBusy = followLoading[key] === true;
            const votingTimeLeft = getVotingTimeLeft(q);
            const myMemberId = getMemberId();
            const isOwnQuestion = myMemberId !== null && q.submitterId !== undefined && String(q.submitterId) === String(myMemberId);
            const canFollowSubmitter = Boolean(q.submitterId) && !isOwnQuestion;
            return (
              <div id={`vote-question-${q.id}`} key={q.id} style={{
                borderBottom: `1px solid ${COLORS.border}`, padding: "14px 14px",
                animation: `fadeUp 0.3s ease ${i * 0.04}s both`,
              }}>
                {/* IG-style header */}
                <div style={{ display: "flex", alignItems: "center", gap: 10, marginBottom: 10 }}>
                  <AvatarCircle
                    avatar={q.avatar}
                    name={q.submitter || "predata"}
                    size={34}
                    style={{ border: isFollowing ? `2px solid ${COLORS.accent}` : "2px solid transparent" }}
                  />
                  <div style={{ flex: 1 }}>
                    <div style={{ display: "flex", alignItems: "center", gap: 5 }}>
                      <span style={{ fontSize: 13, fontWeight: 600, color: COLORS.text }}>{q.submitter || "predata"}</span>
                      <span style={{ fontSize: 9, padding: "1px 5px", borderRadius: 3, background: COLORS.accentGlow, color: COLORS.accentLight, fontWeight: 600, textTransform: "uppercase" }}>{q.category}</span>
                    </div>
                    <span style={{ fontSize: 11, color: COLORS.textDim }}>
                      {q.submitted}
                      {votingTimeLeft ? ` · ${votingTimeLeft}` : ""}
                    </span>
                  </div>
                  <button
                    onClick={() => !isOwnQuestion && toggleFollow(q)}
                    disabled={isFollowBusy || isOwnQuestion}
                    style={{
                    padding: "4px 12px", borderRadius: 7,
                    cursor: isOwnQuestion ? "default" : "pointer",
                    background: isOwnQuestion ? COLORS.accentGlow : (isFollowing ? "transparent" : COLORS.accent),
                    border: isOwnQuestion ? `1px solid ${COLORS.accent}` : `1px solid ${isFollowing ? COLORS.border : "transparent"}`,
                    color: isOwnQuestion ? COLORS.accentLight : (isFollowing ? COLORS.textDim : "white"),
                    fontSize: 11, fontWeight: 600,
                    opacity: isFollowBusy ? 0.7 : 1,
                  }}
                  >
                    {isFollowBusy ? "..." : isOwnQuestion ? "Mine" : (isFollowing ? "Following" : "Follow")}
                  </button>
                </div>

                {/* Title */}
                <div style={{ fontSize: 16, fontWeight: 700, color: COLORS.text, lineHeight: 1.4, marginBottom: q.description ? 4 : 8, fontFamily: "'Outfit', sans-serif" }}>{q.title}</div>
                {q.description && <div style={{ fontSize: 12, color: COLORS.textMuted, lineHeight: 1.4, marginBottom: 8 }}>{q.description}</div>}

                {/* Tags */}
                <div style={{ display: "flex", gap: 4, marginBottom: 10, flexWrap: "wrap" }}>
                  {q.tags.map(t => <span key={t} style={{ fontSize: 10, padding: "2px 6px", borderRadius: 4, background: "rgba(255,255,255,0.03)", color: COLORS.accentLight, fontWeight: 500 }}>#{t}</span>)}
                </div>

                {/* Vote area */}
                <div style={{ background: "rgba(255,255,255,0.015)", borderRadius: 12, padding: 12, marginBottom: 10 }}>
                  {renderVoteBar(q)}
                </div>

                {/* Action bar */}
                <div style={{ display: "flex", alignItems: "center", gap: 16 }}>
                  <div style={{ display: "flex", alignItems: "center", gap: 4, color: COLORS.textMuted }}>
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M22 12h-4l-3 9L9 3l-3 9H2"/></svg>
                    <span style={{ fontSize: 12, fontWeight: 600, fontFamily: "'JetBrains Mono', monospace" }}>{getTotalVotes(q).toLocaleString()}</span>
                    <span style={{ fontSize: 11, color: COLORS.textDim }}>votes</span>
                  </div>
                  <button onClick={() => { setCommentOpen(q.id); loadComments(q.id); }} style={{ background: "none", border: "none", cursor: "pointer", display: "flex", alignItems: "center", gap: 4, padding: 0, color: COLORS.textDim }}>
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z"/></svg>
                    <span style={{ fontSize: 12, fontWeight: 500 }}>{(allComments[q.id] || []).length || q.commentCount}</span>
                  </button>
                  {feedMode === "top10" && (
                    <span style={{
                      marginLeft: "auto", fontSize: 10, fontWeight: 700, padding: "2px 7px", borderRadius: 4,
                      background: i < 3 ? `linear-gradient(135deg, ${COLORS.accent}, #5B21B6)` : COLORS.surface,
                      color: i < 3 ? "white" : COLORS.textDim,
                      border: i >= 3 ? `1px solid ${COLORS.border}` : "none",
                      fontFamily: "'JetBrains Mono', monospace",
                    }}>#{i + 1}</span>
                  )}
                </div>
              </div>
            );
          })}
        </div>

        {mobileQuestions.length === 0 && (
          <div style={{ textAlign: "center", padding: "60px 20px", color: COLORS.textDim }}>
            <div style={{ fontSize: 15, fontWeight: 600, color: COLORS.textMuted }}>No questions found</div>
            <div style={{ fontSize: 12, marginTop: 4 }}>Try a different tab or category</div>
          </div>
        )}
      </div>

      {/* Vote Confirm Popup */}
      {confirmPopup && (
        <div onClick={() => setConfirmPopup(null)} style={{
          position: "fixed", inset: 0, zIndex: 300,
          background: "rgba(0,0,0,0.6)", backdropFilter: "blur(4px)",
          display: "flex", alignItems: "center", justifyContent: "center",
          animation: "modalFadeIn 0.15s ease",
        }}>
          <div onClick={e => e.stopPropagation()} style={{
            background: COLORS.surface, borderRadius: 16, padding: 28,
            maxWidth: 360, width: "90%", border: `1px solid ${COLORS.border}`,
            animation: "modalSlideUp 0.2s ease both",
          }}>
            <div style={{ width: 48, height: 48, borderRadius: "50%", margin: "0 auto 14px",
              background: confirmPopup.choice === "YES" ? COLORS.greenBg : COLORS.redBg,
              display: "flex", alignItems: "center", justifyContent: "center",
            }}>
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke={confirmPopup.choice === "YES" ? COLORS.green : COLORS.red} strokeWidth="2.5"><polyline points="20 6 9 17 4 12"/></svg>
            </div>
            <div style={{ fontSize: 16, fontWeight: 700, textAlign: "center", marginBottom: 8 }}>Confirm Your Vote</div>
            <div style={{ fontSize: 13, color: COLORS.textMuted, textAlign: "center", lineHeight: 1.5, marginBottom: 6 }}>{confirmPopup.question.title}</div>
            <div style={{ textAlign: "center", marginBottom: 20 }}>
              <span style={{ fontSize: 13, fontWeight: 700, color: confirmPopup.choice === "YES" ? COLORS.green : COLORS.red }}>→ {confirmPopup.choice}</span>
            </div>
            <div style={{ display: "flex", gap: 10 }}>
              <button onClick={() => setConfirmPopup(null)} style={{ flex: 1, padding: "10px 0", borderRadius: 10, cursor: "pointer", background: "transparent", border: `1px solid ${COLORS.border}`, color: COLORS.textMuted, fontSize: 14, fontWeight: 600 }}>Cancel</button>
              <button onClick={confirmVote} style={{ flex: 1, padding: "10px 0", borderRadius: 10, cursor: "pointer", background: confirmPopup.choice === "YES" ? `linear-gradient(135deg, ${COLORS.green}, #16A34A)` : `linear-gradient(135deg, ${COLORS.red}, #DC2626)`, border: "none", color: "white", fontSize: 14, fontWeight: 700 }}>Vote</button>
            </div>
            {voteCredits !== null && (
              <div style={{ fontSize: 11, color: COLORS.textDim, textAlign: "center", marginTop: 10 }}>{voteCredits} vote{voteCredits !== 1 ? "s" : ""} remaining today</div>
            )}
          </div>
        </div>
      )}

      {/* ════ Comment UI ════ */}
      {commentOpen && commentQuestion && (
        <>
          {/* ── Desktop: Side Panel ── */}
          <div className="desktop-only" style={{
            position: "fixed", top: 0, right: 0, bottom: 0, width: 400, zIndex: 300,
            background: COLORS.surface, borderLeft: `1px solid ${COLORS.border}`,
            display: "flex", flexDirection: "column",
            animation: "sheetSlideUp 0.2s ease both",
          }}>
            {/* Header */}
            <div style={{
              padding: "16px 20px", borderBottom: `1px solid ${COLORS.border}`,
              display: "flex", justifyContent: "space-between", alignItems: "center",
            }}>
              <div>
                <div style={{ fontSize: 15, fontWeight: 700 }}>Comments</div>
                <div style={{ fontSize: 12, color: COLORS.textDim, marginTop: 2 }}>{currentComments.length} comment{currentComments.length !== 1 ? "s" : ""}</div>
              </div>
              <button onClick={() => setCommentOpen(null)} style={{
                background: "none", border: "none", cursor: "pointer", color: COLORS.textMuted, padding: 4,
              }}>
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M18 6L6 18M6 6l12 12"/></svg>
              </button>
            </div>
            {/* Question context */}
            <div style={{ padding: "12px 20px", borderBottom: `1px solid ${COLORS.border}`, background: "rgba(255,255,255,0.01)" }}>
              <div style={{ fontSize: 13, fontWeight: 600, color: COLORS.text, lineHeight: 1.4 }}>{commentQuestion.title}</div>
            </div>
            {/* Comments list */}
            <div style={{ flex: 1, overflowY: "auto", padding: "12px 20px" }}>
              {currentComments.length === 0 && (
                <div style={{ textAlign: "center", padding: "40px 0", color: COLORS.textDim, fontSize: 13 }}>No comments yet. Be the first!</div>
              )}
              {currentComments.map((c: CommentItem) => (
                <div key={c.id} style={{ marginBottom: 16 }}>
                  <div style={{ display: "flex", alignItems: "center", gap: 8, marginBottom: 4 }}>
                    <AvatarCircle avatar={c.avatar} name={c.user} size={28} />
                    <span style={{ fontSize: 13, fontWeight: 600, color: COLORS.text }}>{c.user}</span>
                    {c.vote && (
                      <span style={{
                        fontSize: 10, fontWeight: 600, padding: "1px 6px", borderRadius: 3,
                        background: c.vote === "YES" ? COLORS.greenBg : COLORS.redBg,
                        color: c.vote === "YES" ? COLORS.green : COLORS.red,
                      }}>{c.vote}</span>
                    )}
                    <span style={{ fontSize: 11, color: COLORS.textDim, marginLeft: "auto" }}>{c.time}</span>
                  </div>
                  <div style={{ fontSize: 13, color: COLORS.textMuted, lineHeight: 1.5, paddingLeft: 36 }}>{c.text}</div>
                </div>
              ))}
            </div>
            {/* Input */}
            <div style={{ padding: "12px 20px", borderTop: `1px solid ${COLORS.border}`, display: "flex", gap: 8 }}>
              <input value={commentText} onChange={e => setCommentText(e.target.value)}
                onKeyDown={e => { if (e.key === "Enter") handleAddComment(); }}
                placeholder="Add a comment..."
                style={{
                  flex: 1, padding: "10px 14px", borderRadius: 10,
                  background: "rgba(255,255,255,0.02)", border: `1px solid ${COLORS.border}`,
                  color: COLORS.text, fontSize: 13, fontFamily: "'DM Sans', sans-serif", outline: "none",
                }}
              />
              <button onClick={handleAddComment} disabled={!commentText.trim()} style={{
                padding: "0 16px", borderRadius: 10,
                background: commentText.trim() ? COLORS.accent : "rgba(255,255,255,0.03)",
                border: "none", color: commentText.trim() ? "white" : COLORS.textDim,
                cursor: commentText.trim() ? "pointer" : "not-allowed",
                fontSize: 13, fontWeight: 600,
              }}>Post</button>
            </div>
          </div>
          {/* Desktop backdrop */}
          <div className="desktop-only" onClick={() => setCommentOpen(null)} style={{
            position: "fixed", inset: 0, zIndex: 299,
            background: "rgba(0,0,0,0.4)",
          }} />

          {/* ── Mobile: Bottom Sheet ── */}
          <div className="mobile-only" style={{ position: "fixed", inset: 0, zIndex: 300 }}>
            <div onClick={() => setCommentOpen(null)} style={{ position: "absolute", inset: 0, background: "rgba(0,0,0,0.5)" }} />
            <div style={{
              position: "absolute", bottom: 0, left: 0, right: 0,
              background: COLORS.surface, borderRadius: "16px 16px 0 0",
              maxHeight: "75vh", display: "flex", flexDirection: "column",
              animation: "sheetSlideUp 0.25s ease both",
            }}>
              {/* Drag handle */}
              <div style={{ display: "flex", justifyContent: "center", padding: "10px 0 4px" }}>
                <div style={{ width: 36, height: 4, borderRadius: 2, background: COLORS.border }} />
              </div>
              {/* Header */}
              <div style={{ padding: "8px 16px 12px", borderBottom: `1px solid ${COLORS.border}` }}>
                <div style={{ fontSize: 15, fontWeight: 700, textAlign: "center" }}>Comments</div>
                <div style={{ fontSize: 12, color: COLORS.textDim, textAlign: "center", marginTop: 2 }}>{currentComments.length} comment{currentComments.length !== 1 ? "s" : ""}</div>
              </div>
              {/* Comments list */}
              <div style={{ flex: 1, overflowY: "auto", padding: "12px 16px" }}>
                {currentComments.length === 0 && (
                  <div style={{ textAlign: "center", padding: "30px 0", color: COLORS.textDim, fontSize: 13 }}>No comments yet. Be the first!</div>
                )}
                {currentComments.map((c: CommentItem) => (
                  <div key={c.id} style={{ marginBottom: 14 }}>
                    <div style={{ display: "flex", alignItems: "center", gap: 8, marginBottom: 4 }}>
                      <AvatarCircle avatar={c.avatar} name={c.user} size={26} />
                      <span style={{ fontSize: 13, fontWeight: 600, color: COLORS.text }}>{c.user}</span>
                      {c.vote && (
                        <span style={{
                          fontSize: 10, fontWeight: 600, padding: "1px 6px", borderRadius: 3,
                          background: c.vote === "YES" ? COLORS.greenBg : COLORS.redBg,
                          color: c.vote === "YES" ? COLORS.green : COLORS.red,
                        }}>{c.vote}</span>
                      )}
                      <span style={{ fontSize: 11, color: COLORS.textDim, marginLeft: "auto" }}>{c.time}</span>
                    </div>
                    <div style={{ fontSize: 13, color: COLORS.textMuted, lineHeight: 1.5, paddingLeft: 34 }}>{c.text}</div>
                  </div>
                ))}
              </div>
              {/* Input */}
              <div style={{
                padding: "10px 16px calc(10px + env(safe-area-inset-bottom, 0px))",
                borderTop: `1px solid ${COLORS.border}`, display: "flex", gap: 8,
              }}>
                <input value={commentText} onChange={e => setCommentText(e.target.value)}
                  onKeyDown={e => { if (e.key === "Enter") handleAddComment(); }}
                  placeholder="Add a comment..."
                  style={{
                    flex: 1, padding: "10px 14px", borderRadius: 10,
                    background: "rgba(255,255,255,0.02)", border: `1px solid ${COLORS.border}`,
                    color: COLORS.text, fontSize: 13, fontFamily: "'DM Sans', sans-serif", outline: "none",
                  }}
                />
                <button onClick={handleAddComment} disabled={!commentText.trim()} style={{
                  padding: "0 14px", borderRadius: 10,
                  background: commentText.trim() ? COLORS.accent : "rgba(255,255,255,0.03)",
                  border: "none", color: commentText.trim() ? "white" : COLORS.textDim,
                  cursor: commentText.trim() ? "pointer" : "not-allowed",
                  fontSize: 13, fontWeight: 600,
                }}>Post</button>
              </div>
            </div>
          </div>
        </>
      )}
    </div>
  );
} 

export default VotePageView;
