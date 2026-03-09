import React, { useState } from "react";
import { COLORS } from "../theme";
import PriceBar from "./PriceBar";
import MiniChart from "./MiniChart";

function MarketCard({ market, index, onBet }) {
  const [hovered, setHovered] = useState(false);
  const positive = market.change24h >= 0;
  const bettingEndTs = market?.bettingEndAt ? new Date(String(market.bettingEndAt)).getTime() : NaN;
  const bettingEndsAtLabel = Number.isNaN(bettingEndTs)
    ? null
    : new Date(bettingEndTs).toISOString().replace("T", " ").slice(0, 16) + " UTC";
  const bettingTimeLeftLabel = (() => {
    const status = String(market?.status ?? "").toUpperCase();
    const phase = String(market?.phase ?? "").toUpperCase();
    const isLiveCategory = String(market?.category ?? "").toUpperCase() === "LIVE";
    const isFinished = phase === "FINISHED" || phase === "SETTLED" || status === "SETTLED" || status === "CANCELLED";
    if (isLiveCategory && !isFinished) return "Live betting open";
    if (Number.isNaN(bettingEndTs)) return null;
    const diffMs = bettingEndTs - Date.now();
    if (diffMs <= 0) return "Betting closed";
    const totalMinutes = Math.floor(diffMs / 60000);
    const days = Math.floor(totalMinutes / (60 * 24));
    const hours = Math.floor((totalMinutes % (60 * 24)) / 60);
    const minutes = totalMinutes % 60;
    if (days > 0) return `${days}d ${hours}h left`;
    if (hours > 0) return `${hours}h ${minutes}m left`;
    return `${minutes}m left`;
  })();
  const status = String(market?.status ?? "").toUpperCase();
  const phase = String(market?.phase ?? "").toUpperCase();
  const isLiveCategory = String(market?.category ?? "").toUpperCase() === "LIVE";
  const isLiveOpen = isLiveCategory && phase !== "FINISHED" && phase !== "SETTLED" && status !== "SETTLED" && status !== "CANCELLED";
  // 일반 마켓은 BETTING 상태에서만 허용, LIVE 마켓은 경기 미종료면 허용
  const isClosed = !isLiveOpen && !!market.status && market.status !== "BETTING";
  const closedLabel =
    market.status === "VOTING" ? "Voting" :
    market.status === "BREAK"  ? "Preparing" :
    "Closed";

  const compactEndsAtLabel = bettingEndsAtLabel
    ? bettingEndsAtLabel.replace(" UTC", "").slice(5)
    : null;

  return (
    <div
      onClick={() => !isClosed && onBet(market, "YES")}
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      style={{
        background: hovered && !isClosed ? COLORS.surfaceHover : COLORS.surface,
        border: `1px solid ${hovered && !isClosed ? COLORS.borderHover : COLORS.border}`,
        borderRadius: 14, padding: 20, cursor: isClosed ? "default" : "pointer",
        transition: "all 0.25s ease",
        animation: `fadeUp 0.4s ease ${index * 0.06}s both`,
        transform: hovered && !isClosed ? "translateY(-2px)" : "none",
        boxShadow: hovered && !isClosed ? "0 8px 32px rgba(0,0,0,0.3), 0 0 0 1px rgba(124,58,237,0.1)" : "0 2px 8px rgba(0,0,0,0.1)",
        display: "flex", flexDirection: "column", gap: 14,
        opacity: isClosed ? 0.7 : 1,
      }}
    >
      {/* Header */}
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start" }}>
        <div style={{ display: "flex", gap: 10, flex: 1 }}>
          <div style={{ flex: 1 }}>
            <h3 style={{ fontSize: 14, fontWeight: 600, lineHeight: 1.4, marginBottom: 4, color: COLORS.text }}>
              {market.title}
            </h3>
            <div style={{ display: "flex", alignItems: "center", gap: 8, flexWrap: "wrap" }}>
              <span style={{
                fontSize: 10, padding: "2px 8px", borderRadius: 4,
                background: COLORS.accentGlow, color: COLORS.accentLight,
                fontWeight: 600, textTransform: "uppercase", letterSpacing: "0.5px",
              }}>
                {market.category}
              </span>
              {isClosed && (
                <span style={{
                  fontSize: 10, padding: "2px 8px", borderRadius: 4,
                  background: "rgba(156,163,175,0.15)", color: "#9CA3AF",
                  fontWeight: 600, textTransform: "uppercase", letterSpacing: "0.5px",
                }}>
                  Closed
                </span>
              )}
              {market.creator && <span style={{ fontSize: 10, color: COLORS.textDim }}>made by <span style={{ color: COLORS.accentLight, fontWeight: 600 }}>@{market.creator}</span></span>}
              <span style={{ fontSize: 11, color: COLORS.textDim }}>⏱ {market.timeLeft}</span>
            </div>
          </div>
        </div>
        <MiniChart change={market.change24h} />
      </div>

      {(bettingTimeLeftLabel || compactEndsAtLabel) && (
        <div style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          gap: 8,
          background: "rgba(255,255,255,0.03)",
          border: `1px solid ${COLORS.border}`,
          borderRadius: 8,
          padding: "6px 8px",
        }}>
          <div style={{ display: "flex", alignItems: "center", gap: 6, minWidth: 0 }}>
            <span style={{
              fontSize: 10,
              fontWeight: 700,
              color: COLORS.accentLight,
              fontFamily: "'JetBrains Mono', monospace",
              whiteSpace: "nowrap",
            }}>
              {bettingTimeLeftLabel || "Betting closed"}
            </span>
          </div>
          {compactEndsAtLabel && (
            <span style={{
              fontSize: 10,
              color: COLORS.textDim,
              fontFamily: "'JetBrains Mono', monospace",
              whiteSpace: "nowrap",
            }}>
              {compactEndsAtLabel}
            </span>
          )}
        </div>
      )}

      {/* Price Bar */}
      <PriceBar yesPrice={market.yesPrice} />

      {/* Stats Row */}
      <div style={{
        display: "flex", justifyContent: "space-between", alignItems: "center",
        paddingTop: 10, borderTop: `1px solid ${COLORS.border}`,
      }}>
        <div style={{ display: "flex", gap: 14 }}>
          <div style={{ fontSize: 11, color: COLORS.textDim }}>
            <span style={{ color: COLORS.textMuted, fontWeight: 600, fontFamily: "'JetBrains Mono', monospace" }}>
              ${market.volume}
            </span> vol
          </div>
          <div style={{ fontSize: 11, color: COLORS.textDim }}>
            <span style={{ color: COLORS.textMuted, fontWeight: 500 }}>{(market.voters || 0).toLocaleString()}</span> voters
          </div>
          <div style={{ fontSize: 11, color: COLORS.textDim }}>
            {market.comments} comments
          </div>
        </div>
        <div style={{
          fontSize: 11, fontWeight: 600,
          color: positive ? COLORS.green : COLORS.red,
          fontFamily: "'JetBrains Mono', monospace",
        }}>
          {positive ? "+" : ""}{market.change24h}%
        </div>
      </div>

      {/* Yes / No Buttons or Closed */}
      {isClosed ? (
        <div style={{
          padding: "9px 0", borderRadius: 8, textAlign: "center",
          background: "rgba(156,163,175,0.08)", border: "1px solid rgba(156,163,175,0.2)",
          fontSize: 13, fontWeight: 600, color: "#6B7280",
          fontFamily: "'DM Sans', sans-serif",
        }}>
          {closedLabel}
        </div>
      ) : (
        <div style={{ display: "flex", gap: 8 }}>
          <button onClick={(e) => { e.stopPropagation(); onBet(market, "YES"); }} style={{
            flex: 1, padding: "9px 0", borderRadius: 8, cursor: "pointer",
            background: "rgba(34,197,94,0.06)",
            color: COLORS.green,
            border: `1px solid rgba(34,197,94,0.2)`,
            fontSize: 13, fontWeight: 600, fontFamily: "'DM Sans', sans-serif",
            transition: "all 0.15s",
          }}
          onMouseEnter={e => { (e.currentTarget as HTMLElement).style.background = COLORS.green; (e.currentTarget as HTMLElement).style.color = "white"; (e.currentTarget as HTMLElement).style.borderColor = COLORS.green; }}
          onMouseLeave={e => { (e.currentTarget as HTMLElement).style.background = "rgba(34,197,94,0.06)"; (e.currentTarget as HTMLElement).style.color = COLORS.green; (e.currentTarget as HTMLElement).style.borderColor = "rgba(34,197,94,0.2)"; }}
          >
            Yes {Math.round(market.yesPrice * 100)}¢
          </button>
          <button onClick={(e) => { e.stopPropagation(); onBet(market, "NO"); }} style={{
            flex: 1, padding: "9px 0", borderRadius: 8, cursor: "pointer",
            background: "rgba(239,68,68,0.06)",
            color: COLORS.red,
            border: `1px solid rgba(239,68,68,0.2)`,
            fontSize: 13, fontWeight: 600, fontFamily: "'DM Sans', sans-serif",
            transition: "all 0.15s",
          }}
          onMouseEnter={e => { (e.currentTarget as HTMLElement).style.background = COLORS.red; (e.currentTarget as HTMLElement).style.color = "white"; (e.currentTarget as HTMLElement).style.borderColor = COLORS.red; }}
          onMouseLeave={e => { (e.currentTarget as HTMLElement).style.background = "rgba(239,68,68,0.06)"; (e.currentTarget as HTMLElement).style.color = COLORS.red; (e.currentTarget as HTMLElement).style.borderColor = "rgba(239,68,68,0.2)"; }}
          >
            No {Math.round((1 - market.yesPrice) * 100)}¢
          </button>
        </div>
      )}
    </div>
  );
}

export default MarketCard;
