import React, { useState, useEffect } from "react";
import { COLORS } from "../theme";
import { voteApi, isLoggedIn } from "../services/api";
import { useVoteFeed } from "../hooks/useApi";
import type { VoteQuestion, VoteChoice } from "../types/domain";

type TopQuestionsSidebarProps = {
  onNoCredits?: () => void;
  onSubmit?: () => void;
  onRequireAuth?: (message?: string) => void;
  onOpenQuestion?: (question: VoteQuestion) => void;
};

function TopQuestionsSidebar({ onNoCredits = () => {}, onSubmit = () => {}, onRequireAuth = () => {}, onOpenQuestion = () => {} }: TopQuestionsSidebarProps = {}) {
  const [selectedCat, setSelectedCat] = useState("");
  const [timeFilter, setTimeFilter] = useState("D1");
  const [votes, setVotes] = useState<Record<string | number, VoteChoice>>({});
  const [voteCredits, setVoteCredits] = useState<number | null>(null);
  const [nowMs, setNowMs] = useState(() => Date.now());
  const isVotingOpen = (q: VoteQuestion) => {
    if (!q.votingEndAt) return true;
    const ts = new Date(q.votingEndAt).getTime();
    if (Number.isNaN(ts)) return true;
    return ts > nowMs;
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

  const sidebarCategories = [    { id: "", label: "Trending" },
    { id: "crypto", label: "Crypto" },
    { id: "tech", label: "Tech & AI" },
    { id: "politics", label: "Politics" },
    { id: "economy", label: "Economy" },
    { id: "culture", label: "Culture" },
    { id: "sports", label: "Sports" },
  ];

  const timeOptions = [
    { id: "H6", label: "6H" },
    { id: "D1", label: "1D" },
    { id: "D3", label: "3D" },
  ];

  const { questions: apiQuestions, loading, refresh } = useVoteFeed({
    mode: "top10",
    category: selectedCat || undefined,
    window: timeFilter,
    limit: 50,
  });

  const [confirmPopup, setConfirmPopup] = useState<{ qId: string | number; title: string; choice: VoteChoice } | null>(null); // { qId, title, choice }
  const visibleQuestions = [...apiQuestions]
    .filter(isVotingOpen)
    .sort((a, b) => (b.totalVotes ?? (b.yesVotes + b.noVotes)) - (a.totalVotes ?? (a.yesVotes + a.noVotes)))
    .slice(0, 3);

  const handleVoteClick = (q: VoteQuestion, choice: VoteChoice) => {
    if (!isLoggedIn()) {
      onRequireAuth("Please log in or sign up to vote.");
      return;
    }
    if (votes[q.id]) return; // already voted
    if (voteCredits !== null && voteCredits <= 0) { onNoCredits(); return; }
    setConfirmPopup({ qId: q.id, title: q.title, choice });
  };

  const confirmVote = async () => {
    if (!confirmPopup) return;
    if (!isLoggedIn()) {
      onRequireAuth("Please log in or sign up to vote.");
      setConfirmPopup(null);
      return;
    }
    try {
      const res: any = await voteApi.vote(confirmPopup.qId, confirmPopup.choice);
      setVotes((prev) => ({ ...prev, [confirmPopup.qId]: confirmPopup.choice }));
      const remaining = res?.data?.remainingDailyVotes ?? res?.remainingDailyVotes;
      if (typeof remaining === "number") setVoteCredits(remaining);
      else setVoteCredits(prev => (prev !== null ? prev - 1 : null));
      await refresh();
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message.toLowerCase() : String(err || "").toLowerCase();
      if (message.includes("401") || message.includes("unauthorized") || message.includes("forbidden")) {
        onRequireAuth("Please log in or sign up to vote.");
      }
      console.error("Vote failed:", err);
    }
    setConfirmPopup(null);
  };

  return (
    <div style={{
      background: COLORS.surface, border: `1px solid ${COLORS.border}`,
      borderRadius: 14, overflow: "hidden",
    }}>
      {/* Header */}
      <div style={{
        padding: "14px 18px", borderBottom: `1px solid ${COLORS.border}`,
      }}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 10 }}>
          <div style={{ fontSize: 14, fontWeight: 600 }}>Top 3 Questions</div>
          <div style={{ display: "flex", gap: 6, alignItems: "center" }}>
            {voteCredits !== null && (
              <div style={{
                fontSize: 11, fontWeight: 600, padding: "2px 8px", borderRadius: 4,
                background: voteCredits > 0 ? COLORS.accentGlow : "rgba(239,68,68,0.1)",
                color: voteCredits > 0 ? COLORS.accentLight : COLORS.red,
                fontFamily: "'JetBrains Mono', monospace",
              }}>
                {voteCredits} vote{voteCredits !== 1 ? "s" : ""} left
              </div>
            )}
          </div>
        </div>
        {/* Submit Question Button */}
        <button onClick={onSubmit} style={{
          width: "100%", padding: "9px 0", borderRadius: 8, cursor: "pointer",
          background: `linear-gradient(135deg, ${COLORS.accent}, #5B21B6)`,
          border: "none", color: "white",
          fontSize: 12, fontWeight: 600, fontFamily: "'DM Sans', sans-serif",
          display: "flex", alignItems: "center", justifyContent: "center", gap: 6,
          boxShadow: "0 2px 8px rgba(124,58,237,0.25)",
          marginBottom: 10,
        }}>
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5"><path d="M12 5v14M5 12h14"/></svg>
          Submit Question
        </button>

        {/* Category selector - horizontal scroll */}
        <div style={{ 
          display: "flex", gap: 4, overflowX: "auto", marginBottom: 10,
          msOverflowStyle: "none", scrollbarWidth: "none",
          WebkitOverflowScrolling: "touch",
        }}>
          {sidebarCategories.map(cat => (
            <button key={cat.id} onClick={() => setSelectedCat(cat.id)} style={{
              padding: "4px 10px", borderRadius: 5, border: "none", cursor: "pointer",
              background: selectedCat === cat.id ? COLORS.accentGlow : "transparent",
              color: selectedCat === cat.id ? COLORS.accentLight : COLORS.textDim,
              fontSize: 11, fontWeight: selectedCat === cat.id ? 600 : 450,
              fontFamily: "'DM Sans', sans-serif",
              transition: "all 0.15s",
              whiteSpace: "nowrap", flexShrink: 0,
            }}>{cat.label}</button>
          ))}
        </div>

        {/* Time filter */}
        <div style={{ display: "flex", gap: 4 }}>
          {timeOptions.map(t => (
            <button key={t.id} onClick={() => setTimeFilter(t.id)} style={{
              flex: 1, padding: "5px 0", borderRadius: 5, cursor: "pointer",
              background: timeFilter === t.id ? COLORS.accent : "rgba(255,255,255,0.03)",
              color: timeFilter === t.id ? "white" : COLORS.textDim,
              border: `1px solid ${timeFilter === t.id ? COLORS.accent : COLORS.border}`,
              fontSize: 11, fontWeight: 600, fontFamily: "'DM Sans', sans-serif",
              transition: "all 0.15s",
            }}>{t.label}</button>
          ))}
        </div>
      </div>

      {/* Empty state */}
      {!loading && visibleQuestions.length === 0 && (
        <div style={{ padding: "24px 18px", textAlign: "center", color: COLORS.textDim }}>
          <div style={{ fontSize: 13 }}>No active questions in this category.</div>
        </div>
      )}

      {/* Loading state */}
      {loading && (
        <div style={{ padding: "24px 18px", textAlign: "center", color: COLORS.textDim }}>
          <div style={{ fontSize: 13 }}>Loading...</div>
        </div>
      )}

      {/* Questions */}
      {!loading && visibleQuestions.map((q: VoteQuestion, i: number) => {
        const totalVotes = q.totalVotes ?? (q.yesVotes + q.noVotes);
        const hiddenVoteResults = q.hiddenVoteResults === true;
        const yesPct = Math.round(((q.yesVotes ?? 0) / Math.max(totalVotes, 1)) * 100);
        const myVote = votes[q.id];
        const votingTimeLeft = getVotingTimeLeft(q);

        return (
          <div key={q.id} style={{
            padding: "14px 18px", borderBottom: `1px solid ${COLORS.border}`,
            transition: "background 0.1s", cursor: "pointer",
          }}
          onClick={() => onOpenQuestion(q)}
          onMouseEnter={e => e.currentTarget.style.background = COLORS.surfaceHover}
          onMouseLeave={e => e.currentTarget.style.background = "transparent"}
          >
            {/* Rank + Title */}
            <div style={{ display: "flex", gap: 8, marginBottom: 10 }}>
              <span style={{
                fontSize: 11, fontWeight: 700, color: COLORS.accent,
                fontFamily: "'JetBrains Mono', monospace",
                minWidth: 16,
              }}>#{i + 1}</span>
              <div style={{ flex: 1 }}>
                <div style={{ fontSize: 13, fontWeight: 500, color: COLORS.text, lineHeight: 1.4 }}>
                  {q.title}
                </div>
                {votingTimeLeft && (
                  <div style={{ marginTop: 4, fontSize: 10, color: COLORS.textDim, fontFamily: "'JetBrains Mono', monospace" }}>
                    {votingTimeLeft}
                  </div>
                )}
              </div>
            </div>

            {/* Vote bar */}
            <div style={{ marginBottom: 10 }}>
              {hiddenVoteResults ? (
                <div style={{ fontSize: 11, color: COLORS.textDim, textAlign: "center" }}>
                  Hidden until betting close
                </div>
              ) : (
                <>
                  <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 3 }}>
                    <span style={{ fontSize: 10, color: COLORS.green, fontWeight: 600, fontFamily: "'JetBrains Mono', monospace" }}>
                      Yes {yesPct}%
                    </span>
                    <span style={{ fontSize: 10, color: COLORS.red, fontWeight: 600, fontFamily: "'JetBrains Mono', monospace" }}>
                      No {100 - yesPct}%
                    </span>
                  </div>
                  <div style={{ height: 4, borderRadius: 2, background: COLORS.redBg, overflow: "hidden" }}>
                    <div style={{
                      height: "100%", width: `${yesPct}%`, borderRadius: 2,
                      background: `linear-gradient(90deg, ${COLORS.green}, #4ADE80)`,
                      transition: "width 0.4s ease",
                    }} />
                  </div>
                </>
              )}
            </div>

            {/* Vote buttons or Complete */}
            {myVote ? (
              <div style={{
                display: "flex", alignItems: "center", justifyContent: "center", gap: 6,
                padding: "8px 0", borderRadius: 6,
                background: myVote === "YES" ? "rgba(34,197,94,0.08)" : "rgba(239,68,68,0.08)",
                border: `1px solid ${myVote === "YES" ? "rgba(34,197,94,0.2)" : "rgba(239,68,68,0.2)"}`,
              }}>
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke={myVote === "YES" ? COLORS.green : COLORS.red} strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
                <span style={{ fontSize: 12, fontWeight: 600, color: myVote === "YES" ? COLORS.green : COLORS.red }}>
                  Voted {myVote === "YES" ? "Yes" : "No"}
                </span>
              </div>
            ) : (
              <div style={{ display: "flex", gap: 6 }}>
                <button onClick={(e) => { e.stopPropagation(); handleVoteClick(q, "YES"); }} style={{
                  flex: 1, padding: "7px 0", borderRadius: 6, cursor: "pointer",
                  background: "rgba(34,197,94,0.06)",
                  color: COLORS.green,
                  border: "1px solid rgba(34,197,94,0.2)",
                  fontSize: 12, fontWeight: 600, fontFamily: "'DM Sans', sans-serif",
                  transition: "all 0.15s",
                }}>
                  Yes
                </button>
                <button onClick={(e) => { e.stopPropagation(); handleVoteClick(q, "NO"); }} style={{
                  flex: 1, padding: "7px 0", borderRadius: 6, cursor: "pointer",
                  background: "rgba(239,68,68,0.06)",
                  color: COLORS.red,
                  border: "1px solid rgba(239,68,68,0.2)",
                  fontSize: 12, fontWeight: 600, fontFamily: "'DM Sans', sans-serif",
                  transition: "all 0.15s",
                }}>
                  No
                </button>
              </div>
            )}

            {/* Meta */}
            <div style={{ display: "flex", justifyContent: "space-between", marginTop: 8 }}>
              <span style={{ fontSize: 10, color: COLORS.textDim }}>
                {totalVotes.toLocaleString()} votes
              </span>
              <span style={{ fontSize: 10, color: COLORS.textDim, fontFamily: "'JetBrains Mono', monospace" }}>
                {Math.round(q.yesPrice * 100)}¢ / {Math.round((1 - q.yesPrice) * 100)}¢
              </span>
            </div>
          </div>
        );
      })}

      {/* Vote Confirm Popup */}
      {confirmPopup && (
        <div onClick={() => setConfirmPopup(null)} style={{
          position: "fixed", inset: 0, zIndex: 1100,
          background: "rgba(0,0,0,0.6)", backdropFilter: "blur(6px)",
          display: "flex", alignItems: "center", justifyContent: "center",
          padding: 24, animation: "modalFadeIn 0.15s ease both",
        }}>
          <div onClick={e => e.stopPropagation()} style={{
            width: "100%", maxWidth: 360,
            background: COLORS.surface, border: `1px solid ${COLORS.border}`,
            borderRadius: 16, padding: "28px 24px",
            animation: "modalSlideUp 0.2s ease both",
            textAlign: "center",
          }}>
            {/* Icon */}
            <div style={{
              width: 48, height: 48, borderRadius: "50%", margin: "0 auto 16px",
              background: confirmPopup.choice === "YES" ? "rgba(34,197,94,0.1)" : "rgba(239,68,68,0.1)",
              display: "flex", alignItems: "center", justifyContent: "center",
            }}>
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke={confirmPopup.choice === "YES" ? COLORS.green : COLORS.red} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <rect x="3" y="3" width="18" height="18" rx="2"/><path d="M9 12l2 2 4-4"/>
              </svg>
            </div>

            <div style={{ fontSize: 16, fontWeight: 700, color: COLORS.text, fontFamily: "'Outfit', sans-serif", marginBottom: 8 }}>
              Confirm Your Vote
            </div>
            <div style={{ fontSize: 14, color: COLORS.textMuted, lineHeight: 1.5, marginBottom: 6 }}>
              {confirmPopup.title}
            </div>
            <div style={{ fontSize: 15, fontWeight: 700, marginBottom: 20, color: confirmPopup.choice === "YES" ? COLORS.green : COLORS.red }}>
              → {confirmPopup.choice === "YES" ? "Yes" : "No"}
            </div>

            <div style={{ display: "flex", gap: 10 }}>
              <button onClick={() => setConfirmPopup(null)} style={{
                flex: 1, padding: "11px 0", borderRadius: 10,
                background: "rgba(255,255,255,0.04)", border: `1px solid ${COLORS.border}`,
                color: COLORS.textMuted, cursor: "pointer",
                fontSize: 14, fontWeight: 600, fontFamily: "'DM Sans', sans-serif",
                transition: "all 0.15s",
              }}
              onMouseEnter={e => { (e.currentTarget as HTMLElement).style.background = "rgba(255,255,255,0.08)"; (e.currentTarget as HTMLElement).style.color = COLORS.text; }}
              onMouseLeave={e => { (e.currentTarget as HTMLElement).style.background = "rgba(255,255,255,0.04)"; (e.currentTarget as HTMLElement).style.color = COLORS.textMuted; }}
              >Cancel</button>
              <button onClick={confirmVote} style={{
                flex: 1, padding: "11px 0", borderRadius: 10,
                background: confirmPopup.choice === "YES"
                  ? `linear-gradient(135deg, ${COLORS.green}, #16A34A)`
                  : `linear-gradient(135deg, ${COLORS.red}, #DC2626)`,
                border: "none", color: "white", cursor: "pointer",
                fontSize: 14, fontWeight: 700, fontFamily: "'DM Sans', sans-serif",
                boxShadow: confirmPopup.choice === "YES" ? "0 4px 16px rgba(34,197,94,0.3)" : "0 4px 16px rgba(239,68,68,0.3)",
                transition: "all 0.15s",
              }}>Vote</button>
            </div>

            {voteCredits !== null && (
              <div style={{ fontSize: 11, color: COLORS.textDim, marginTop: 14 }}>
                {voteCredits} vote{voteCredits !== 1 ? "s" : ""} remaining today
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

export default TopQuestionsSidebar;
