import React, { useState, useEffect, useRef } from "react";
import { COLORS } from "../theme";
import { voteApi } from "../services/api";

function VoteDetailView({ question, initialChoice, onBack }) {
  const [choice, setChoice] = useState(initialChoice);
  const [submitted, setSubmitted] = useState(false);
  const [loading, setLoading] = useState(false);

  const totalVotes = question.totalVotes ?? (question.yesVotes + question.noVotes);
  const hiddenVoteResults = question.hiddenVoteResults === true;
  const yesPct = Math.round(((question.yesVotes ?? 0) / Math.max(totalVotes, 1)) * 100);

  const handleSubmit = async () => {
    if (!choice) return;
    setLoading(true);
    try {
      await voteApi.vote(question.id, choice);
      setSubmitted(true);
    } catch (err) {
      console.error("Vote failed:", err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ minHeight: "100vh", background: COLORS.bg }}>
      {/* Top bar */}
      <div className="mobile-only" style={{
        position: "sticky", top: 0, zIndex: 50,
        background: `${COLORS.bg}f2`, backdropFilter: "blur(12px)",
        borderBottom: `1px solid ${COLORS.border}`,
        padding: "12px 24px",
        display: "flex", alignItems: "center", gap: 16,
        maxWidth: 1320, margin: "0 auto",
      }}>
        <button onClick={onBack} style={{
          background: "none", border: "none", cursor: "pointer",
          color: COLORS.textMuted, display: "flex", alignItems: "center", gap: 6,
          fontSize: 14, fontWeight: 500, fontFamily: "'DM Sans', sans-serif",
          transition: "color 0.15s",
        }}
        onMouseEnter={e => e.currentTarget.style.color = COLORS.text}
        onMouseLeave={e => e.currentTarget.style.color = COLORS.textMuted}
        >
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M19 12H5"/><polyline points="12 19 5 12 12 5"/></svg>
          Back to Markets
        </button>
        <span style={{
          fontSize: 11, padding: "3px 10px", borderRadius: 4,
          background: COLORS.accentGlow, color: COLORS.accentLight,
          fontWeight: 600, textTransform: "uppercase", letterSpacing: "0.5px",
        }}>Voting Phase</span>
      </div>

      <div className="vote-detail-grid" style={{ maxWidth: 1320, margin: "0 auto", padding: "24px 24px" }}>
        {/* Left - Question Info */}
        <div>
          <h1 style={{ fontSize: 24, fontWeight: 700, lineHeight: 1.35, color: COLORS.text, fontFamily: "'Outfit', sans-serif", marginBottom: 24 }}>
            {question.title}
          </h1>

          {/* Vote Distribution */}
          <div style={{
            background: COLORS.surface, border: `1px solid ${COLORS.border}`,
            borderRadius: 14, padding: 24, marginBottom: 20,
          }}>
            <div style={{ fontSize: 14, fontWeight: 600, marginBottom: 16 }}>Vote Distribution</div>
            {hiddenVoteResults ? (
              <div style={{ fontSize: 14, color: COLORS.textDim, textAlign: "center", marginBottom: 8 }}>
                Vote breakdown hidden until betting close
              </div>
            ) : (
              <>
                <div style={{ display: "flex", gap: 24, marginBottom: 20 }}>
                  <div style={{ flex: 1 }}>
                    <div style={{ fontSize: 36, fontWeight: 700, fontFamily: "'JetBrains Mono', monospace", color: COLORS.green }}>{yesPct}%</div>
                    <div style={{ fontSize: 13, color: COLORS.textDim }}>Yes · {question.yesVotes.toLocaleString()} votes</div>
                  </div>
                  <div style={{ flex: 1, textAlign: "right" }}>
                    <div style={{ fontSize: 36, fontWeight: 700, fontFamily: "'JetBrains Mono', monospace", color: COLORS.red }}>{100 - yesPct}%</div>
                    <div style={{ fontSize: 13, color: COLORS.textDim }}>No · {question.noVotes.toLocaleString()} votes</div>
                  </div>
                </div>
                {/* Large bar */}
                <div style={{ height: 12, borderRadius: 6, background: COLORS.redBg, overflow: "hidden" }}>
                  <div style={{
                    height: "100%", width: `${yesPct}%`, borderRadius: 6,
                    background: `linear-gradient(90deg, ${COLORS.green}, #4ADE80)`,
                    transition: "width 0.6s ease",
                  }} />
                </div>
              </>
            )}
          </div>

          {/* Stats */}
          <div className="vote-detail-stats" style={{
            background: COLORS.surface, border: `1px solid ${COLORS.border}`,
            borderRadius: 14, padding: 20,
            display: "grid",
          }}>
            {[
              { label: "Total Votes", value: totalVotes.toLocaleString() },
              { label: "Yes Price", value: `${Math.round(question.yesPrice * 100)}¢` },
              { label: "No Price", value: `${Math.round((1 - question.yesPrice) * 100)}¢` },
            ].map((s, i) => (
              <div key={i}>
                <div style={{ fontSize: 11, color: COLORS.textDim, marginBottom: 4 }}>{s.label}</div>
                <div style={{ fontSize: 18, fontWeight: 700, fontFamily: "'JetBrains Mono', monospace", color: COLORS.text }}>{s.value}</div>
              </div>
            ))}
          </div>
        </div>

        {/* Right - Vote Panel */}
        <div className="vote-detail-panel">
          <div style={{
            background: COLORS.surface, border: `1px solid ${COLORS.border}`,
            borderRadius: 16, overflow: "hidden",
          }}>
            <div style={{ padding: 20 }}>
              <div style={{ fontSize: 16, fontWeight: 700, marginBottom: 4, fontFamily: "'Outfit', sans-serif" }}>Cast Your Vote</div>
              <div style={{ fontSize: 13, color: COLORS.textDim, marginBottom: 20 }}>
                Your vote helps determine market direction
              </div>

              {submitted ? (
                <div style={{ textAlign: "center", padding: "20px 0" }}>
                  <div style={{
                    width: 56, height: 56, borderRadius: "50%",
                    background: choice === "YES" ? COLORS.greenBg : COLORS.redBg,
                    display: "inline-flex", alignItems: "center", justifyContent: "center",
                    marginBottom: 14,
                  }}>
                    <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke={choice === "YES" ? COLORS.green : COLORS.red} strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
                  </div>
                  <div style={{ fontSize: 18, fontWeight: 700, color: COLORS.text, marginBottom: 4 }}>
                    Vote submitted!
                  </div>
                  <div style={{ fontSize: 14, color: COLORS.textDim }}>
                    You voted <span style={{ color: choice === "YES" ? COLORS.green : COLORS.red, fontWeight: 600 }}>{choice}</span>
                  </div>
                  <button onClick={onBack} style={{
                    marginTop: 20, padding: "10px 24px", borderRadius: 8,
                    background: "rgba(255,255,255,0.04)", border: `1px solid ${COLORS.border}`,
                    color: COLORS.textMuted, cursor: "pointer", fontSize: 13, fontWeight: 500,
                    fontFamily: "'DM Sans', sans-serif", transition: "all 0.15s",
                  }}
                  onMouseEnter={e => { (e.currentTarget as HTMLElement).style.background = "rgba(255,255,255,0.08)"; (e.currentTarget as HTMLElement).style.color = COLORS.text; }}
                  onMouseLeave={e => { (e.currentTarget as HTMLElement).style.background = "rgba(255,255,255,0.04)"; (e.currentTarget as HTMLElement).style.color = COLORS.textMuted; }}
                  >Back to Markets</button>
                </div>
              ) : (
                <>
                  {/* Yes/No Selection */}
                  <div style={{ display: "flex", flexDirection: "column", gap: 10, marginBottom: 20 }}>
                    {["YES", "NO"].map(c => (
                      <button key={c} onClick={() => setChoice(c)} style={{
                        padding: "16px 20px", borderRadius: 12, cursor: "pointer",
                        background: choice === c
                          ? (c === "YES" ? "rgba(34,197,94,0.1)" : "rgba(239,68,68,0.1)")
                          : "rgba(255,255,255,0.02)",
                        border: `2px solid ${choice === c
                          ? (c === "YES" ? COLORS.green : COLORS.red)
                          : COLORS.border}`,
                        display: "flex", alignItems: "center", justifyContent: "space-between",
                        transition: "all 0.15s",
                      }}>
                        <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
                          <div style={{
                            width: 20, height: 20, borderRadius: "50%",
                            border: `2px solid ${choice === c ? (c === "YES" ? COLORS.green : COLORS.red) : COLORS.textDim}`,
                            display: "flex", alignItems: "center", justifyContent: "center",
                            transition: "all 0.15s",
                          }}>
                            {choice === c && <div style={{ width: 10, height: 10, borderRadius: "50%", background: c === "YES" ? COLORS.green : COLORS.red }} />}
                          </div>
                          <span style={{
                            fontSize: 16, fontWeight: 600, fontFamily: "'DM Sans', sans-serif",
                            color: choice === c ? COLORS.text : COLORS.textMuted,
                          }}>{c === "YES" ? "Yes" : "No"}</span>
                        </div>
                        <span style={{
                          fontSize: 14, fontWeight: 600, fontFamily: "'JetBrains Mono', monospace",
                          color: c === "YES" ? COLORS.green : COLORS.red,
                        }}>{hiddenVoteResults ? "Hidden" : `${c === "YES" ? yesPct : 100 - yesPct}%`}</span>
                      </button>
                    ))}
                  </div>

                  {/* Submit */}
                  <button onClick={handleSubmit} disabled={loading || !choice} style={{
                    width: "100%", padding: "14px 0", borderRadius: 10,
                    background: !choice ? COLORS.textDim
                      : choice === "YES"
                        ? `linear-gradient(135deg, ${COLORS.green}, #16A34A)`
                        : `linear-gradient(135deg, ${COLORS.red}, #DC2626)`,
                    border: "none", color: "white", cursor: !choice ? "not-allowed" : "pointer",
                    fontSize: 15, fontWeight: 700, fontFamily: "'DM Sans', sans-serif",
                    transition: "all 0.15s", opacity: loading ? 0.7 : 1,
                    boxShadow: choice ? (choice === "YES" ? "0 4px 16px rgba(34,197,94,0.3)" : "0 4px 16px rgba(239,68,68,0.3)") : "none",
                    display: "flex", alignItems: "center", justifyContent: "center", gap: 8,
                  }}>
                    {loading && <div style={{ width: 16, height: 16, border: "2px solid rgba(255,255,255,0.3)", borderTopColor: "white", borderRadius: "50%", animation: "spin 0.6s linear infinite" }} />}
                    {loading ? "Submitting..." : `Vote ${choice === "YES" ? "Yes" : "No"}`}
                  </button>
                </>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export default VoteDetailView;
