import React from "react";
import { COLORS } from "../theme";
import { useLeaderboardTop } from "../hooks/useApi";
import type { LeaderboardEntryItem } from "../types/domain";

type LeaderboardViewProps = {
  onBack?: () => void;
};

function LeaderboardView({ onBack = () => {} }: LeaderboardViewProps = {}) {
  const { data, loading, error } = useLeaderboardTop(50);
  const rows: LeaderboardEntryItem[] = Array.isArray(data) ? data : [];

  return (
    <div style={{ minHeight: "100vh", background: COLORS.bg }}>
      <div
        className="mobile-only"
        style={{
          position: "sticky",
          top: 0,
          zIndex: 50,
          background: `${COLORS.bg}f2`,
          backdropFilter: "blur(12px)",
          borderBottom: `1px solid ${COLORS.border}`,
          padding: "12px 24px",
          display: "flex",
          alignItems: "center",
          gap: 10,
          maxWidth: 1100,
          margin: "0 auto",
        }}
      >
        <button
          onClick={onBack}
          style={{
            background: "none",
            border: "none",
            cursor: "pointer",
            color: COLORS.textMuted,
            display: "flex",
            alignItems: "center",
          }}
        >
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M19 12H5" />
            <polyline points="12 19 5 12 12 5" />
          </svg>
        </button>
        <h1 style={{ fontSize: 18, fontWeight: 700, fontFamily: "'Outfit', sans-serif", margin: 0 }}>Leaderboard</h1>
      </div>

      <div style={{ maxWidth: 1100, margin: "0 auto", padding: "20px 14px 90px" }}>
        <div
          style={{
            background: COLORS.surface,
            border: `1px solid ${COLORS.border}`,
            borderRadius: 14,
            overflow: "hidden",
          }}
        >
          <div
            className="desktop-only-grid"
            style={{
              display: "grid",
              gridTemplateColumns: "70px 1.5fr 90px 120px 120px 120px",
              gap: 8,
              padding: "12px 14px",
              borderBottom: `1px solid ${COLORS.border}`,
              fontSize: 12,
              color: COLORS.textDim,
              fontWeight: 600,
            }}
          >
            <span>Rank</span>
            <span>User</span>
            <span>Tier</span>
            <span>Accuracy</span>
            <span>Predictions</span>
            <span>Balance</span>
          </div>

          {loading && (
            <div style={{ padding: "22px 14px", color: COLORS.textDim, fontSize: 13 }}>Loading leaderboard...</div>
          )}
          {error && !loading && (
            <div style={{ padding: "22px 14px", color: COLORS.red, fontSize: 13 }}>{error}</div>
          )}
          {!loading && !error && rows.length === 0 && (
            <div style={{ padding: "22px 14px", color: COLORS.textDim, fontSize: 13 }}>No leaderboard data.</div>
          )}

          {!loading &&
            !error &&
            rows.map((row) => (
              <React.Fragment key={row.memberId}>
                <div
                  className="desktop-only-grid"
                  style={{
                    display: "grid",
                    gridTemplateColumns: "70px 1.5fr 90px 120px 120px 120px",
                    gap: 8,
                    padding: "12px 14px",
                    borderBottom: `1px solid ${COLORS.border}`,
                    fontSize: 13,
                    color: COLORS.text,
                  }}
                >
                  <span style={{ fontWeight: 700, color: row.rank <= 3 ? COLORS.accentLight : COLORS.text }}>{row.rank}</span>
                  <span>{row.email}</span>
                  <span>{row.tier}</span>
                  <span>{row.accuracyPercentage.toFixed(1)}%</span>
                  <span>
                    {row.correctPredictions}/{row.totalPredictions}
                  </span>
                  <span>${row.usdcBalance.toLocaleString()}</span>
                </div>

                <div
                  className="mobile-only"
                  style={{
                    padding: "12px 14px",
                    borderBottom: `1px solid ${COLORS.border}`,
                  }}
                >
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 6 }}>
                    <span style={{ fontSize: 16, fontWeight: 700, color: row.rank <= 3 ? COLORS.accentLight : COLORS.text }}>#{row.rank}</span>
                    <span style={{ fontSize: 12, color: COLORS.accentLight, fontWeight: 600 }}>{row.tier}</span>
                  </div>
                  <div style={{ fontSize: 13, color: COLORS.text, marginBottom: 6, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{row.email}</div>
                  <div style={{ display: "flex", justifyContent: "space-between", fontSize: 12, color: COLORS.textDim }}>
                    <span>{row.accuracyPercentage.toFixed(1)}%</span>
                    <span>{row.correctPredictions}/{row.totalPredictions}</span>
                    <span>${row.usdcBalance.toLocaleString()}</span>
                  </div>
                </div>
              </React.Fragment>
            ))}
        </div>
      </div>
    </div>
  );
}

export default LeaderboardView;
