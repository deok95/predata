import React, { useState, useEffect, useCallback, useMemo } from "react";
import { COLORS } from "../theme";
import AvatarCircle from "../components/AvatarCircle";
import { usePool, useSwapSimulation, useSwap, useMyShares, useSwapHistory } from "../hooks/useApi";
import { questionApi, isLoggedIn } from "../services/api";
import { getWsManager } from "../services/websocket";
import PriceBar from "../components/PriceBar";

const toNumber = (value: unknown, fallback = 0): number => {
  if (typeof value === "number" && Number.isFinite(value)) return value;
  if (typeof value === "string" && value !== "") {
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : fallback;
  }
  return fallback;
};

const relativeTime = (value: unknown): string => {
  const raw = String(value ?? "");
  if (!raw) return "now";
  const normalized = raw.includes("T") ? raw : raw.replace(" ", "T");
  const ts = new Date(normalized).getTime();
  if (Number.isNaN(ts)) return raw;
  const diffSec = Math.max(0, Math.floor((Date.now() - ts) / 1000));
  if (diffSec < 10) return "just now";
  if (diffSec < 60) return `${diffSec}s ago`;
  if (diffSec < 3600) return `${Math.floor(diffSec / 60)}m ago`;
  if (diffSec < 86400) return `${Math.floor(diffSec / 3600)}h ago`;
  return `${Math.floor(diffSec / 86400)}d ago`;
};

function BetDetailView({ market, initialChoice, onBack, onRequireAuth = (_message?: string) => {} }) {
  const [choice, setChoice] = useState(initialChoice);
  const [amount, setAmount] = useState("");
  const [activeTab, setActiveTab] = useState("comments");
  const [commentText, setCommentText] = useState("");
  const [betSheetOpen, setBetSheetOpen] = useState(false);
  const [nowMs, setNowMs] = useState(() => Date.now());

  useEffect(() => {
    const timer = window.setInterval(() => setNowMs(Date.now()), 60_000);
    return () => window.clearInterval(timer);
  }, []);

  // API: pool state for live prices
  const { data: poolData, execute: refreshPool, setData: setPoolData } = usePool(market.id);
  const { result: simResult, simulate } = useSwapSimulation();
  const { loading: swapLoading, executeSwap } = useSwap();
  const { data: myShares, execute: refreshMyShares } = useMyShares(market.id);
  const { data: swapHistory, execute: refreshSwapHistory } = useSwapHistory(market.id);

  // Subscribe to pool WebSocket updates for live prices
  useEffect(() => {
    let cancelled = false;
    const controller = new AbortController();
    let sub: { unsubscribe: () => void } | null = null;
    getWsManager()?.subscribe(`/topic/pool/${market.id}`, (msg) => {
      if (cancelled) return;
      const update = msg as { yesPrice: number; noPrice: number; yesShares: string; noShares: string };
      setPoolData((prev) => {
        const base = (prev ?? {}) as Record<string, unknown>;
        return {
          ...base,
          currentPrice: { yes: update.yesPrice, no: update.noPrice },
          yesShares: update.yesShares,
          noShares: update.noShares,
        } as typeof prev;
      });
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
  }, [market.id, setPoolData]);

  const [tradeMode, setTradeMode] = useState("buy"); // buy | sell
  const [sellShares, setSellShares] = useState("");
  const amountNum = parseFloat(amount) || 0;
  const sellSharesNum = parseInt(sellShares, 10) || 0;

  // Simulate when buy/sell amount changes
  useEffect(() => {
    const requestAmount = tradeMode === "buy" ? amountNum : sellSharesNum;
    if (requestAmount > 0) {
      simulate({ questionId: market.id, side: choice, amount: requestAmount, direction: tradeMode });
    }
  }, [amountNum, sellSharesNum, choice, market.id, tradeMode, simulate]);

  useEffect(() => {
    const timer = window.setInterval(() => {
      refreshSwapHistory().catch(() => {});
    }, 15000);
    return () => window.clearInterval(timer);
  }, [refreshSwapHistory]);

  const loading = swapLoading;
  const userBalance = 2847.50;
  const mySharesRecord = (myShares ?? {}) as Record<string, unknown>;
  const yesShares = toNumber(mySharesRecord.yesShares, 0);
  const noShares = toNumber(mySharesRecord.noShares, 0);
  const yesCostBasis = toNumber(mySharesRecord.yesCostBasis, 0);
  const noCostBasis = toNumber(mySharesRecord.noCostBasis, 0);
  const myHoldings = {
    YES: yesShares,
    NO: noShares,
    avgPriceYes: yesShares > 0 ? (yesCostBasis / yesShares) : 0,
    avgPriceNo: noShares > 0 ? (noCostBasis / noShares) : 0,
  };
  const holdingShares = choice === "YES" ? myHoldings.YES : myHoldings.NO;
  const holdingAvgPrice = choice === "YES" ? myHoldings.avgPriceYes : myHoldings.avgPriceNo;
  type BetComment = { id: number | string; user: string; avatar?: string; badge?: string | null; time: string; text: string; likes?: number; replies?: number; };
  const [comments, setComments] = useState<BetComment[]>([]);

  const loadBetComments = useCallback(async () => {
    try {
      const data = await questionApi.getComments(market.id);
      setComments((data || []) as BetComment[]);
    } catch { /* keep existing */ }
  }, [market.id]);

  useEffect(() => { loadBetComments(); }, [loadBetComments]);

  const liveTrades = useMemo(() => {
    const rows = (Array.isArray(swapHistory) ? swapHistory : []) as Record<string, unknown>[];
    return rows.map((row) => {
      const side = String(row.outcome ?? "YES").toUpperCase();
      const price = Math.round(toNumber(row.effectivePrice, 0.5) * 100);
      const email = String(row.memberEmail ?? "");
      const memberId = row.memberId != null ? String(row.memberId) : "";
      const user = email ? email.split("@")[0] : (memberId ? `user_${memberId}` : "trader");
      return {
        id: String(row.swapId ?? `${row.createdAt ?? ""}-${row.memberId ?? ""}`),
        user,
        side: side === "NO" ? "NO" : "YES",
        amount: toNumber(row.usdcAmount, 0),
        price,
        time: relativeTime(row.createdAt),
      };
    });
  }, [swapHistory]);

  // Use live pool price if available, otherwise fall back to market snapshot
  const poolRecord = poolData as Record<string, unknown> | null;
  const liveYesPrice = (poolRecord?.currentPrice as Record<string, number> | undefined)?.yes ?? market.yesPrice;

  const price = choice === "YES" ? liveYesPrice : (1 - liveYesPrice);
  const priceCents = Math.round(price * 100);
  const simRecord = (simResult ?? {}) as Record<string, unknown>;
  const configuredPlatformFeeRate = toNumber(market.platformFeeShare, 0.01);
  const feeRate = configuredPlatformFeeRate > 0 ? configuredPlatformFeeRate : 0.01;

  // Buy calculations (FPMM)
  const fee_buy = tradeMode === "buy"
    ? toNumber(simRecord.fee, amountNum * feeRate)
    : amountNum * feeRate;
  const potentialReturn = amountNum > 0 ? (amountNum / Math.max(price, 0.0001)).toFixed(2) : "0.00";
  const profitIfCorrect = amountNum > 0 ? (amountNum / Math.max(price, 0.0001) - amountNum).toFixed(2) : "0.00";
  const presets = [5, 10, 25, 50, 100];

  // Sell calculations (FPMM: shares → USDC)
  // FPMM sell gross: c_out_gross ≈ s_in * price (simplified for UI)
  const sellGross = toNumber(simRecord.usdcOut, (sellSharesNum * price));
  const sellFee = tradeMode === "sell"
    ? toNumber(simRecord.fee, sellGross * feeRate)
    : sellGross * feeRate;
  const sellNet = sellGross - sellFee; // user receives after fee
  const sellCostBasis = sellSharesNum * holdingAvgPrice;
  const sellRealizedPnl = sellNet - sellCostBasis;
  const sellPresets = [10, 25, 50, holdingShares].filter((v, i, a) => v > 0 && v <= holdingShares && a.indexOf(v) === i);
  const formatPercent = (value: number) => {
    const rounded = value >= 1 ? value.toFixed(1) : value.toFixed(2);
    return `${Number(rounded)}%`;
  };
  const buyFeeLabel = `Fee (${formatPercent(amountNum > 0 ? (fee_buy / amountNum) * 100 : feeRate * 100)})`;
  const sellFeeLabel = `Fee (${formatPercent(sellGross > 0 ? (sellFee / sellGross) * 100 : feeRate * 100)})`;

  const bettingEndMs = market?.bettingEndAt ? new Date(String(market.bettingEndAt)).getTime() : NaN;
  const bettingEndsAtLabel = Number.isNaN(bettingEndMs)
    ? null
    : new Date(bettingEndMs).toISOString().replace("T", " ").slice(0, 16) + " UTC";
  const bettingTimeLeftLabel = (() => {
    if (Number.isNaN(bettingEndMs)) return null;
    const diffMs = bettingEndMs - nowMs;
    if (diffMs <= 0) return "Betting closed";
    const totalMinutes = Math.floor(diffMs / 60000);
    const days = Math.floor(totalMinutes / (60 * 24));
    const hours = Math.floor((totalMinutes % (60 * 24)) / 60);
    const minutes = totalMinutes % 60;
    if (days > 0) return `${days}d ${hours}h left`;
    if (hours > 0) return `${hours}h ${minutes}m left`;
    return `${minutes}m left`;
  })();

  const handleBet = async () => {
    if (!isLoggedIn()) {
      onRequireAuth("Please log in or sign up to place a bet.");
      return;
    }
    if (tradeMode === "buy" && (!amount || amountNum <= 0)) return;
    if (tradeMode === "sell" && (!sellShares || sellSharesNum <= 0 || sellSharesNum > holdingShares)) return;
    try {
      await executeSwap({
        questionId: market.id,
        side: choice,
        direction: tradeMode, // "buy" or "sell"
        amount: tradeMode === "buy" ? amountNum : undefined,
        shares: tradeMode === "sell" ? sellSharesNum : undefined,
      });
      refreshPool(); // Refresh pool data after swap
      refreshSwapHistory().catch(() => {});
      refreshMyShares().catch(() => {});
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message.toLowerCase() : String(err || "").toLowerCase();
      if (message.includes("401") || message.includes("unauthorized") || message.includes("forbidden")) {
        onRequireAuth("Please log in or sign up to place a bet.");
        return;
      }
      console.error("Swap failed:", err);
    }
  };

  const handleComment = async () => {
    if (!isLoggedIn()) {
      onRequireAuth("Please log in or sign up to comment.");
      return;
    }
    if (!commentText.trim()) return;
    const text = commentText.trim();
    setCommentText("");
    try {
      await questionApi.postComment(market.id, text);
      await loadBetComments();
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message.toLowerCase() : String(err || "").toLowerCase();
      if (message.includes("401") || message.includes("unauthorized") || message.includes("forbidden")) {
        onRequireAuth("Please log in or sign up to comment.");
      }
      setCommentText(text); // restore on error
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
          Back
        </button>
        <span style={{
          fontSize: 10, padding: "3px 10px", borderRadius: 4,
          background: COLORS.accentGlow, color: COLORS.accentLight,
          fontWeight: 600, textTransform: "uppercase", letterSpacing: "0.5px",
        }}>{market.category}</span>
        {market.creator && <span style={{ fontSize: 11, color: COLORS.textDim }}>made by <span style={{ color: COLORS.accentLight, fontWeight: 600 }}>@{market.creator}</span></span>}
        <span style={{ fontSize: 12, color: COLORS.textDim }}>⏱ {market.timeLeft}</span>
      </div>

      <div className="bet-detail-grid" style={{ maxWidth: 1320, margin: "0 auto", padding: "24px 24px", display: "grid", gridTemplateColumns: "1fr 360px", gap: 28 }}>
        {/* ── Left Column ── */}
        <div>
          {/* Title */}
          <h1 style={{ fontSize: 24, fontWeight: 700, lineHeight: 1.35, color: COLORS.text, fontFamily: "'Outfit', sans-serif", marginBottom: 20 }}>
            {market.title}
          </h1>
          {(bettingTimeLeftLabel || bettingEndsAtLabel) && (
            <div style={{ marginTop: -12, marginBottom: 16 }}>
              {bettingTimeLeftLabel && (
                <div style={{ fontSize: 12, fontWeight: 600, color: COLORS.accentLight, fontFamily: "'JetBrains Mono', monospace" }}>
                  Betting ends in {bettingTimeLeftLabel}
                </div>
              )}
              {bettingEndsAtLabel && (
                <div style={{ fontSize: 11, color: COLORS.textDim, marginTop: 2, fontFamily: "'JetBrains Mono', monospace" }}>
                  Ends at {bettingEndsAtLabel}
                </div>
              )}
            </div>
          )}

          {/* Large Price Chart */}
          <div style={{
            background: COLORS.surface, border: `1px solid ${COLORS.border}`,
            borderRadius: 14, padding: 24, marginBottom: 20,
          }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: 16 }}>
              <div>
                <div style={{ fontSize: 32, fontWeight: 700, fontFamily: "'JetBrains Mono', monospace", color: COLORS.green }}>
                  {Math.round(liveYesPrice * 100)}%
                </div>
                <div style={{ fontSize: 12, color: COLORS.textDim }}>chance of Yes</div>
              </div>
              <div style={{ display: "flex", gap: 4 }}>
                {["LIVE", "1D", "1W", "1M", "ALL"].map(t => (
                  <button key={t} style={{
                    padding: "4px 10px", borderRadius: 5, border: "none", cursor: "pointer",
                    background: t === "1D" ? COLORS.accentGlow : "transparent",
                    color: t === "1D" ? COLORS.accentLight : COLORS.textDim,
                    fontSize: 11, fontWeight: 500, fontFamily: "'DM Sans', sans-serif",
                  }}>{t}</button>
                ))}
              </div>
            </div>
            {/* Chart */}
            <svg width="100%" height="180" viewBox="0 0 500 180" preserveAspectRatio="none">
              <defs>
                <linearGradient id="chartGrad" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stopColor={COLORS.green} stopOpacity="0.15" />
                  <stop offset="100%" stopColor={COLORS.green} stopOpacity="0" />
                </linearGradient>
              </defs>
              <polygon points="0,130 30,125 60,120 90,122 120,100 150,105 180,80 210,85 240,70 270,65 300,75 330,55 360,50 390,60 420,40 450,35 480,28 500,22 500,180 0,180" fill="url(#chartGrad)" />
              <polyline points="0,130 30,125 60,120 90,122 120,100 150,105 180,80 210,85 240,70 270,65 300,75 330,55 360,50 390,60 420,40 450,35 480,28 500,22" fill="none" stroke={COLORS.green} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
            </svg>
            <div style={{ display: "flex", justifyContent: "space-between", marginTop: 8 }}>
              <span style={{ fontSize: 11, color: COLORS.textDim, fontFamily: "'JetBrains Mono', monospace" }}>${market.volume} vol</span>
              <span style={{ fontSize: 11, color: COLORS.textDim }}>
                <span style={{ color: market.change24h >= 0 ? COLORS.green : COLORS.red, fontWeight: 600, fontFamily: "'JetBrains Mono', monospace" }}>
                  {market.change24h >= 0 ? "+" : ""}{market.change24h}%
                </span> 24h
              </span>
            </div>
          </div>

          {/* Price Bar */}
          <div style={{
            background: COLORS.surface, border: `1px solid ${COLORS.border}`,
            borderRadius: 14, padding: 20, marginBottom: 20,
          }}>
            <PriceBar yesPrice={liveYesPrice} />
          </div>

          {/* Live Trades Feed */}
          <div style={{
            background: COLORS.surface, border: `1px solid ${COLORS.border}`,
            borderRadius: 14, marginBottom: 20, overflow: "hidden",
          }}>
            <div style={{
              padding: "12px 18px", borderBottom: `1px solid ${COLORS.border}`,
              display: "flex", justifyContent: "space-between", alignItems: "center",
            }}>
              <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                <div style={{ width: 6, height: 6, borderRadius: "50%", background: COLORS.red, animation: "pulse 1.5s infinite" }} />
                <span style={{ fontSize: 13, fontWeight: 600 }}>Live Trades</span>
              </div>
              <span style={{ fontSize: 11, color: COLORS.textDim }}>{liveTrades.length} recent</span>
            </div>
            <div style={{ maxHeight: 240, overflowY: "auto" }}>
              {liveTrades.slice(0, 10).map((t, i) => (
                <div key={t.id} style={{
                  padding: "10px 18px", display: "flex", justifyContent: "space-between", alignItems: "center",
                  borderBottom: i < 9 ? `1px solid ${COLORS.border}` : "none",
                  animation: i === 0 ? "fadeUp 0.3s ease" : "none",
                }}>
                  <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                    <div style={{
                      width: 28, height: 28, borderRadius: 7,
                      background: t.side === "YES" ? COLORS.greenBg : COLORS.redBg,
                      display: "flex", alignItems: "center", justifyContent: "center",
                      fontSize: 10, fontWeight: 700, color: t.side === "YES" ? COLORS.green : COLORS.red,
                    }}>{t.side === "YES" ? "Y" : "N"}</div>
                    <div>
                      <div style={{ fontSize: 13, fontWeight: 500, color: COLORS.text }}>{t.user}</div>
                      <div style={{ fontSize: 11, color: COLORS.textDim }}>{t.time}</div>
                    </div>
                  </div>
                  <div style={{ textAlign: "right" }}>
                    <div style={{
                      fontSize: 13, fontWeight: 700, fontFamily: "'JetBrains Mono', monospace",
                      color: t.side === "YES" ? COLORS.green : COLORS.red,
                    }}>
                      ${t.amount.toLocaleString()}
                    </div>
                    <div style={{ fontSize: 10, color: COLORS.textDim, fontFamily: "'JetBrains Mono', monospace" }}>@ {t.price}¢</div>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Comments / Activity Tabs */}
          <div style={{
            background: COLORS.surface, border: `1px solid ${COLORS.border}`,
            borderRadius: 14, overflow: "hidden",
          }}>
            <div style={{ display: "flex", borderBottom: `1px solid ${COLORS.border}` }}>
              {[
                { id: "comments", label: "Ideas", count: comments.length },
                { id: "activity", label: "Activity" },
              ].map(t => (
                <button key={t.id} onClick={() => setActiveTab(t.id)} style={{
                  flex: 1, padding: "12px 0", border: "none", cursor: "pointer",
                  background: "transparent",
                  color: activeTab === t.id ? COLORS.text : COLORS.textDim,
                  fontSize: 14, fontWeight: activeTab === t.id ? 700 : 500,
                  fontFamily: "'DM Sans', sans-serif",
                  borderBottom: activeTab === t.id ? `2px solid ${COLORS.accent}` : "2px solid transparent",
                  transition: "all 0.15s",
                }}>{t.label}{t.count != null ? ` (${t.count})` : ""}</button>
              ))}
            </div>

            {activeTab === "comments" && (
              <div>
                {/* Comment input */}
                <div style={{ padding: "14px 18px", borderBottom: `1px solid ${COLORS.border}` }}>
                  <div style={{ fontSize: 13, color: COLORS.textDim, marginBottom: 8 }}>Share your opinion</div>
                  <div style={{ display: "flex", gap: 8, alignItems: "flex-end" }}>
                    <textarea
                      value={commentText} onChange={e => setCommentText(e.target.value)}
                      placeholder="Share your thoughts..."
                      rows={2}
                      style={{
                        flex: 1, padding: "10px 12px", borderRadius: 8,
                        background: "rgba(255,255,255,0.02)", border: `1px solid ${COLORS.border}`,
                        color: COLORS.text, fontSize: 13, fontFamily: "'DM Sans', sans-serif",
                        resize: "none", outline: "none",
                      }}
                    />
                    <button onClick={handleComment} disabled={!commentText.trim()} style={{
                      padding: "10px 18px", borderRadius: 8,
                      background: commentText.trim() ? COLORS.accent : COLORS.textDim,
                      border: "none", color: "white", cursor: commentText.trim() ? "pointer" : "not-allowed",
                      fontSize: 13, fontWeight: 600, fontFamily: "'DM Sans', sans-serif",
                      transition: "all 0.15s", whiteSpace: "nowrap",
                    }}>Post</button>
                  </div>
                  <div style={{ fontSize: 11, color: COLORS.textDim, marginTop: 6, textAlign: "right" }}>
                    {800 - commentText.length} left
                  </div>
                </div>

                {/* Comments list */}
                {comments.map((c, i) => (
                  <div key={c.id} style={{
                    padding: "14px 18px",
                    borderBottom: i < comments.length - 1 ? `1px solid ${COLORS.border}` : "none",
                  }}>
                    <div style={{ display: "flex", alignItems: "center", gap: 8, marginBottom: 6 }}>
                      <AvatarCircle avatar={c.avatar ?? COLORS.accent} name={c.user} size={28} />
                      <span style={{ fontSize: 13, fontWeight: 600, color: COLORS.text }}>{c.user}</span>
                      <span style={{ fontSize: 11, color: COLORS.textDim }}>{c.time}</span>
                      {c.badge && (
                        <span style={{
                          fontSize: 10, padding: "2px 6px", borderRadius: 4,
                          background: c.badge.startsWith("Yes") ? COLORS.greenBg : COLORS.redBg,
                          color: c.badge.startsWith("Yes") ? COLORS.green : COLORS.red,
                          fontWeight: 600,
                        }}>{c.badge}</span>
                      )}
                    </div>
                    <div style={{ fontSize: 14, color: COLORS.textMuted, lineHeight: 1.5, paddingLeft: 36 }}>
                      {c.text}
                    </div>
                    <div style={{ display: "flex", gap: 16, paddingLeft: 36, marginTop: 8 }}>
                      <button style={{
                        background: "none", border: "none", cursor: "pointer",
                        display: "flex", alignItems: "center", gap: 4,
                        color: COLORS.textDim, fontSize: 12, fontFamily: "'DM Sans', sans-serif",
                      }}>
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z"/></svg>
                        {(c.replies ?? 0) > 0 && c.replies}
                      </button>
                      <button style={{
                        background: "none", border: "none", cursor: "pointer",
                        display: "flex", alignItems: "center", gap: 4,
                        color: COLORS.textDim, fontSize: 12, fontFamily: "'DM Sans', sans-serif",
                      }}>
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M20.84 4.61a5.5 5.5 0 00-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 00-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 000-7.78z"/></svg>
                        {c.likes}
                      </button>
                      <button style={{
                        background: "none", border: "none", cursor: "pointer",
                        display: "flex", alignItems: "center", gap: 4,
                        color: COLORS.textDim, fontSize: 12, fontFamily: "'DM Sans', sans-serif",
                      }}>
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M4 12v8a2 2 0 002 2h12a2 2 0 002-2v-8M16 6l-4-4-4 4M12 2v13"/></svg>
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}

            {activeTab === "activity" && (
              liveTrades.length === 0 ? (
                <div style={{ padding: "20px 18px", textAlign: "center", color: COLORS.textDim }}>
                  <div style={{ fontSize: 14 }}>No recent trade activity</div>
                </div>
              ) : (
                <div style={{ maxHeight: 260, overflowY: "auto" }}>
                  {liveTrades.slice(0, 8).map((t, i) => (
                    <div key={`activity-${t.id}`} style={{
                      padding: "12px 18px",
                      display: "flex",
                      justifyContent: "space-between",
                      borderBottom: i < Math.min(liveTrades.length, 8) - 1 ? `1px solid ${COLORS.border}` : "none",
                    }}>
                      <div>
                        <div style={{ fontSize: 13, color: COLORS.text }}>
                          {t.user} {t.side === "YES" ? "bought Yes" : "bought No"}
                        </div>
                        <div style={{ fontSize: 11, color: COLORS.textDim }}>{t.time}</div>
                      </div>
                      <div style={{ textAlign: "right" }}>
                        <div style={{ fontSize: 13, fontWeight: 700, color: t.side === "YES" ? COLORS.green : COLORS.red }}>
                          ${t.amount.toLocaleString()}
                        </div>
                        <div style={{ fontSize: 11, color: COLORS.textDim }}>@ {t.price}¢</div>
                      </div>
                    </div>
                  ))}
                </div>
              )
            )}
          </div>
        </div>

        {/* ── Right Column - Bet Panel (Desktop) ── */}
        <div className="desktop-only" style={{ position: "sticky", top: 72, alignSelf: "start", display: "flex", flexDirection: "column" }}>
          {/* Market summary card */}
          <div style={{
            background: COLORS.surface, border: `1px solid ${COLORS.border}`,
            borderRadius: 16, overflow: "hidden", marginBottom: 14,
          }}>
            <div style={{ padding: "14px 18px", borderBottom: `1px solid ${COLORS.border}` }}>
              <div style={{ fontSize: 13, fontWeight: 500, color: COLORS.textMuted, lineHeight: 1.4, marginBottom: 4 }}>
                {market.title}
              </div>
              <div style={{
                fontSize: 12, fontWeight: 600,
                color: choice === "YES" ? COLORS.green : COLORS.red,
              }}>
                {tradeMode === "buy" ? "Buy" : "Sell"} {choice} · {market.title.split("?")[0].split(" ").slice(-2).join(" ")}
              </div>
            </div>

            {/* Buy / Sell toggle */}
            <div style={{ display: "flex", padding: "8px 14px", gap: 8, borderBottom: `1px solid ${COLORS.border}` }}>
              {["buy", "sell"].map(m => (
                <button key={m} onClick={() => setTradeMode(m)} style={{
                  flex: 1, padding: "7px 0", borderRadius: 6, border: "none", cursor: "pointer",
                  background: tradeMode === m ? COLORS.accentGlow : "transparent",
                  color: tradeMode === m ? COLORS.accentLight : COLORS.textDim,
                  fontSize: 13, fontWeight: tradeMode === m ? 600 : 500,
                  fontFamily: "'DM Sans', sans-serif", transition: "all 0.15s",
                }}>{m === "buy" ? "Buy" : "Sell"}</button>
              ))}
            </div>

            {/* Yes/No Toggle */}
            <div style={{ display: "flex", gap: 8, padding: "10px 14px" }}>
              {["YES", "NO"].map(c => (
                <button key={c} onClick={() => setChoice(c)} style={{
                  flex: 1, padding: "10px 0", borderRadius: 8, cursor: "pointer",
                  background: choice === c
                    ? (c === "YES" ? "rgba(34,197,94,0.1)" : "rgba(239,68,68,0.1)")
                    : "transparent",
                  border: `1.5px solid ${choice === c
                    ? (c === "YES" ? COLORS.green : COLORS.red)
                    : COLORS.border}`,
                  color: choice === c
                    ? (c === "YES" ? COLORS.green : COLORS.red)
                    : COLORS.textDim,
                  fontSize: 14, fontWeight: 700, fontFamily: "'JetBrains Mono', monospace",
                  transition: "all 0.15s",
                }}>
                  {c === "YES" ? "Yes" : "No"} {c === "YES" ? Math.round(liveYesPrice * 100) : Math.round((1 - liveYesPrice) * 100)}¢
                </button>
              ))}
            </div>

            <div style={{ padding: "0 18px 18px" }}>
              {tradeMode === "buy" ? (
                <>
                  {/* Amount */}
                  <div style={{ marginBottom: 14 }}>
                    <div style={{ fontSize: 13, fontWeight: 500, color: COLORS.textMuted, marginBottom: 6 }}>Amount</div>
                    <div style={{
                      display: "flex", alignItems: "center",
                      border: `1.5px solid ${COLORS.border}`, borderRadius: 10, overflow: "hidden",
                    }}>
                      <span style={{ padding: "0 12px", fontSize: 15, fontWeight: 600, color: COLORS.textDim }}>$</span>
                      <input type="number" placeholder="0" min="0" step="0.01"
                        value={amount} onChange={e => setAmount(e.target.value)}
                        style={{ flex: 1, padding: "12px 0", border: "none", outline: "none", fontSize: 20, fontWeight: 700, fontFamily: "'JetBrains Mono', monospace", color: COLORS.text, background: "transparent" }}
                      />
                    </div>
                  </div>
                  {/* Quick amounts */}
                  <div style={{ display: "flex", gap: 6, marginBottom: 16 }}>
                    {presets.map(p => (
                      <button key={p} onClick={() => setAmount(String(p))} style={{
                        flex: 1, padding: "6px 0", borderRadius: 6, cursor: "pointer",
                        background: amount === String(p) ? COLORS.accentGlow : "rgba(255,255,255,0.03)",
                        color: amount === String(p) ? COLORS.accentLight : COLORS.textDim,
                        border: `1px solid ${amount === String(p) ? "rgba(124,58,237,0.3)" : COLORS.border}`,
                        fontSize: 12, fontWeight: 600, fontFamily: "'JetBrains Mono', monospace",
                        transition: "all 0.15s",
                      }}>${p}</button>
                    ))}
                  </div>
                  {/* Balance */}
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 14, padding: "8px 12px", borderRadius: 8, background: "rgba(255,255,255,0.02)" }}>
                    <span style={{ fontSize: 12, color: COLORS.textDim }}>Balance</span>
                    <span style={{ fontSize: 13, fontWeight: 700, fontFamily: "'JetBrains Mono', monospace", color: COLORS.accentLight }}>${userBalance.toLocaleString()}</span>
                  </div>
                  {/* Summary */}
                  <div style={{ background: "rgba(255,255,255,0.02)", borderRadius: 10, padding: 12, marginBottom: 14 }}>
                    {[
                      { label: "Avg price", value: `${priceCents}¢` },
                      { label: "Est. shares", value: amountNum > 0 ? (amountNum / price).toFixed(1) : "0" },
                      { label: buyFeeLabel, value: `$${fee_buy.toFixed(2)}`, color: COLORS.textDim },
                      { label: "To win (if correct)", value: `$${potentialReturn}`, color: COLORS.green },
                      { label: "Profit if correct", value: `+$${profitIfCorrect}`, color: COLORS.green },
                    ].map((row, i) => (
                      <div key={i} style={{ display: "flex", justifyContent: "space-between", alignItems: "center", padding: "5px 0", borderBottom: i < 3 ? `1px solid ${COLORS.border}` : "none" }}>
                        <span style={{ fontSize: 12, color: COLORS.textDim }}>{row.label}</span>
                        <span style={{ fontSize: 12, fontWeight: 600, fontFamily: "'JetBrains Mono', monospace", color: row.color || COLORS.text }}>{row.value}</span>
                      </div>
                    ))}
                  </div>
                </>
              ) : (
                <>
                  {/* Holdings info */}
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 14, padding: "10px 12px", borderRadius: 8, background: "rgba(255,255,255,0.02)" }}>
                    <div>
                      <div style={{ fontSize: 11, color: COLORS.textDim, marginBottom: 2 }}>Your {choice} Shares</div>
                      <div style={{ fontSize: 18, fontWeight: 700, fontFamily: "'JetBrains Mono', monospace" }}>{holdingShares}</div>
                    </div>
                    <div style={{ textAlign: "right" }}>
                      <div style={{ fontSize: 11, color: COLORS.textDim, marginBottom: 2 }}>Avg Cost</div>
                      <div style={{ fontSize: 14, fontWeight: 600, fontFamily: "'JetBrains Mono', monospace", color: COLORS.textMuted }}>{Math.round(holdingAvgPrice * 100)}¢</div>
                    </div>
                  </div>
                  {/* Shares to sell */}
                  <div style={{ marginBottom: 14 }}>
                    <div style={{ fontSize: 13, fontWeight: 500, color: COLORS.textMuted, marginBottom: 6 }}>Shares to sell</div>
                    <div style={{
                      display: "flex", alignItems: "center",
                      border: `1.5px solid ${COLORS.border}`, borderRadius: 10, overflow: "hidden",
                    }}>
                      <input type="number" placeholder="0" min="0" max={holdingShares}
                        value={sellShares} onChange={e => setSellShares(e.target.value)}
                        style={{ flex: 1, padding: "12px 14px", border: "none", outline: "none", fontSize: 20, fontWeight: 700, fontFamily: "'JetBrains Mono', monospace", color: COLORS.text, background: "transparent" }}
                      />
                      <span style={{ padding: "0 12px", fontSize: 12, color: COLORS.textDim }}>/ {holdingShares}</span>
                    </div>
                  </div>
                  {/* Quick sell presets */}
                  <div style={{ display: "flex", gap: 6, marginBottom: 16 }}>
                    {sellPresets.map((p, i) => (
                      <button key={i} onClick={() => setSellShares(String(p))} style={{
                        flex: 1, padding: "6px 0", borderRadius: 6, cursor: "pointer",
                        background: sellShares === String(p) ? COLORS.accentGlow : "rgba(255,255,255,0.03)",
                        color: sellShares === String(p) ? COLORS.accentLight : COLORS.textDim,
                        border: `1px solid ${sellShares === String(p) ? "rgba(124,58,237,0.3)" : COLORS.border}`,
                        fontSize: 12, fontWeight: 600, fontFamily: "'JetBrains Mono', monospace",
                        transition: "all 0.15s",
                      }}>{p === holdingShares ? "MAX" : p}</button>
                    ))}
                  </div>
                  {/* Sell Summary */}
                  <div style={{ background: "rgba(255,255,255,0.02)", borderRadius: 10, padding: 12, marginBottom: 14 }}>
                    {[
                      { label: "Current price", value: `${priceCents}¢` },
                      { label: "Gross proceeds", value: `$${sellGross.toFixed(2)}` },
                      { label: sellFeeLabel, value: `-$${sellFee.toFixed(2)}`, color: COLORS.textDim },
                      { label: "You receive", value: `$${sellNet.toFixed(2)}`, color: COLORS.text },
                      { label: "Cost basis", value: `$${sellCostBasis.toFixed(2)}`, color: COLORS.textDim },
                      { label: "Realized P&L", value: `${sellRealizedPnl >= 0 ? "+" : ""}$${sellRealizedPnl.toFixed(2)}`, color: sellRealizedPnl >= 0 ? COLORS.green : COLORS.red },
                    ].map((row, i) => (
                      <div key={i} style={{ display: "flex", justifyContent: "space-between", alignItems: "center", padding: "5px 0", borderBottom: i < 5 ? `1px solid ${COLORS.border}` : "none" }}>
                        <span style={{ fontSize: 12, color: COLORS.textDim }}>{row.label}</span>
                        <span style={{ fontSize: 12, fontWeight: 600, fontFamily: "'JetBrains Mono', monospace", color: row.color || COLORS.text }}>{row.value}</span>
                      </div>
                    ))}
                  </div>
                </>
              )}

              {/* Action Button */}
              <button onClick={handleBet} disabled={loading || (tradeMode === "buy" ? !amount : !sellShares || sellSharesNum > holdingShares)} style={{
                width: "100%", padding: "14px 0", borderRadius: 10,
                background: (tradeMode === "buy" ? !amount : !sellShares) ? COLORS.textDim
                  : tradeMode === "sell" ? `linear-gradient(135deg, ${COLORS.red}, #DC2626)`
                  : choice === "YES"
                    ? `linear-gradient(135deg, ${COLORS.green}, #16A34A)`
                    : `linear-gradient(135deg, ${COLORS.red}, #DC2626)`,
                border: "none", color: "white", cursor: (tradeMode === "buy" ? !amount : !sellShares) ? "not-allowed" : "pointer",
                fontSize: 15, fontWeight: 700, fontFamily: "'DM Sans', sans-serif",
                transition: "all 0.15s", opacity: loading ? 0.7 : 1,
                boxShadow: (tradeMode === "buy" && amount) || (tradeMode === "sell" && sellShares) ? "0 4px 16px rgba(0,0,0,0.3)" : "none",
                display: "flex", alignItems: "center", justifyContent: "center", gap: 8,
              }}>
                {loading && <div style={{ width: 16, height: 16, border: "2px solid rgba(255,255,255,0.3)", borderTopColor: "white", borderRadius: "50%", animation: "spin 0.6s linear infinite" }} />}
                {loading
                  ? (tradeMode === "buy" ? "Placing bet..." : "Selling...")
                  : tradeMode === "buy"
                    ? `Buy ${choice === "YES" ? "Yes" : "No"} ${amount ? `$${amount}` : ""}`
                    : `Sell ${sellSharesNum} ${choice} Share${sellSharesNum !== 1 ? "s" : ""}`
                }
              </button>
            </div>
          </div>

          {/* Stats */}
          <div style={{
            background: COLORS.surface, border: `1px solid ${COLORS.border}`,
            borderRadius: 14, padding: 16,
            display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12,
          }}>
            {[
              { label: "Volume", value: `$${market.volume}` },
              { label: "Voters", value: (market.voters || 0).toLocaleString() },
              { label: "Comments", value: comments.length },
              { label: "24h", value: `${market.change24h > 0 ? "+" : ""}${market.change24h}%`, color: market.change24h >= 0 ? COLORS.green : COLORS.red },
            ].map((s, i) => (
              <div key={i}>
                <div style={{ fontSize: 10, color: COLORS.textDim, marginBottom: 2, textTransform: "uppercase" }}>{s.label}</div>
                <div style={{ fontSize: 14, fontWeight: 700, fontFamily: "'JetBrains Mono', monospace", color: s.color || COLORS.text }}>{s.value}</div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* ── Mobile Fixed Bottom Bar ── */}
      <div className="mobile-only-flex" style={{
        position: "fixed", bottom: 0, left: 0, right: 0, zIndex: 200,
        background: `${COLORS.bg}f7`, backdropFilter: "blur(12px)",
        borderTop: `1px solid ${COLORS.border}`,
        padding: "10px 16px calc(10px + env(safe-area-inset-bottom, 0px))",
        gap: 10,
      }}>
        <button onClick={() => { setChoice("YES"); setBetSheetOpen(true); }} style={{
          flex: 1, padding: "14px 0", borderRadius: 10, cursor: "pointer",
          background: "rgba(34,197,94,0.08)", border: "1.5px solid rgba(34,197,94,0.3)",
          color: COLORS.green, fontSize: 16, fontWeight: 700, fontFamily: "'DM Sans', sans-serif",
          transition: "all 0.15s",
        }}>
          Buy Yes {Math.round(liveYesPrice * 100)}%
        </button>
        <button onClick={() => { setChoice("NO"); setBetSheetOpen(true); }} style={{
          flex: 1, padding: "14px 0", borderRadius: 10, cursor: "pointer",
          background: "rgba(239,68,68,0.08)", border: "1.5px solid rgba(239,68,68,0.3)",
          color: COLORS.red, fontSize: 16, fontWeight: 700, fontFamily: "'DM Sans', sans-serif",
          transition: "all 0.15s",
        }}>
          Buy No {Math.round((1 - liveYesPrice) * 100)}%
        </button>
      </div>

      {/* ── Mobile Bottom Sheet ── */}
      {betSheetOpen && (
        <div onClick={() => setBetSheetOpen(false)} style={{
          position: "fixed", inset: 0, zIndex: 300,
          background: "rgba(0,0,0,0.6)", backdropFilter: "blur(4px)",
          animation: "modalFadeIn 0.15s ease",
        }}>
          <div onClick={e => e.stopPropagation()} style={{
            position: "absolute", bottom: 0, left: 0, right: 0,
            background: COLORS.surface,
            borderTopLeftRadius: 20, borderTopRightRadius: 20,
            padding: "0 20px calc(20px + env(safe-area-inset-bottom, 0px))",
            animation: "sheetSlideUp 0.25s ease both",
            maxHeight: "85vh", overflowY: "auto",
          }}>
            {/* Handle */}
            <div style={{ display: "flex", justifyContent: "center", padding: "10px 0" }}>
              <div style={{ width: 36, height: 4, borderRadius: 2, background: "rgba(255,255,255,0.15)" }} />
            </div>

            {/* Title */}
            <div style={{ fontSize: 13, color: COLORS.textDim, marginBottom: 4, lineHeight: 1.4 }}>{market.title}</div>

            {/* Buy/Sell Toggle */}
            <div style={{ display: "flex", gap: 8, marginBottom: 12 }}>
              {["buy", "sell"].map(m => (
                <button key={m} onClick={() => setTradeMode(m)} style={{
                  flex: 1, padding: "9px 0", borderRadius: 8, border: "none", cursor: "pointer",
                  background: tradeMode === m ? COLORS.accentGlow : "transparent",
                  color: tradeMode === m ? COLORS.accentLight : COLORS.textDim,
                  fontSize: 14, fontWeight: tradeMode === m ? 600 : 500,
                  fontFamily: "'DM Sans', sans-serif", transition: "all 0.15s",
                }}>{m === "buy" ? "Buy" : "Sell"}</button>
              ))}
            </div>

            {/* Balance / Holdings */}
            <div style={{
              display: "flex", justifyContent: "space-between", alignItems: "center",
              padding: "10px 0", marginBottom: 12,
              borderBottom: `1px solid ${COLORS.border}`,
            }}>
              {tradeMode === "buy" ? (
                <>
                  <span style={{ fontSize: 13, color: COLORS.textDim }}>Available Balance</span>
                  <span style={{ fontSize: 16, fontWeight: 700, fontFamily: "'JetBrains Mono', monospace", color: COLORS.accentLight }}>
                    ${userBalance.toLocaleString()}
                  </span>
                </>
              ) : (
                <>
                  <div>
                    <div style={{ fontSize: 11, color: COLORS.textDim }}>Your {choice} Shares</div>
                    <div style={{ fontSize: 18, fontWeight: 700, fontFamily: "'JetBrains Mono', monospace" }}>{holdingShares}</div>
                  </div>
                  <div style={{ textAlign: "right" }}>
                    <div style={{ fontSize: 11, color: COLORS.textDim }}>Avg Cost</div>
                    <div style={{ fontSize: 14, fontWeight: 600, fontFamily: "'JetBrains Mono', monospace", color: COLORS.textMuted }}>{Math.round(holdingAvgPrice * 100)}¢</div>
                  </div>
                </>
              )}
            </div>

            {/* Buy/Sell Toggle (Mobile) */}
            <div style={{ display: "flex", gap: 8, marginBottom: 12 }}>
              {["buy", "sell"].map(m => (
                <button key={m} onClick={() => setTradeMode(m)} style={{
                  flex: 1, padding: "9px 0", borderRadius: 8, border: "none", cursor: "pointer",
                  background: tradeMode === m ? COLORS.accentGlow : "transparent",
                  color: tradeMode === m ? COLORS.accentLight : COLORS.textDim,
                  fontSize: 14, fontWeight: tradeMode === m ? 600 : 500,
                  fontFamily: "'DM Sans', sans-serif", transition: "all 0.15s",
                }}>{m === "buy" ? "Buy" : "Sell"}</button>
              ))}
            </div>

            {/* Yes/No Toggle */}
            <div style={{ display: "flex", gap: 8, marginBottom: 16 }}>
              {["YES", "NO"].map(c => (
                <button key={c} onClick={() => setChoice(c)} style={{
                  flex: 1, padding: "12px 0", borderRadius: 10, cursor: "pointer",
                  background: choice === c
                    ? (c === "YES" ? "rgba(34,197,94,0.12)" : "rgba(239,68,68,0.12)")
                    : "transparent",
                  border: `2px solid ${choice === c
                    ? (c === "YES" ? COLORS.green : COLORS.red)
                    : COLORS.border}`,
                  color: choice === c
                    ? (c === "YES" ? COLORS.green : COLORS.red)
                    : COLORS.textDim,
                  fontSize: 16, fontWeight: 700, fontFamily: "'JetBrains Mono', monospace",
                  transition: "all 0.15s",
                }}>
                  {c === "YES" ? "Yes" : "No"} {c === "YES" ? Math.round(liveYesPrice * 100) : Math.round((1 - liveYesPrice) * 100)}¢
                </button>
              ))}
            </div>

            {tradeMode === "buy" ? (
              <>
                {/* Amount */}
                <div style={{
                  display: "flex", alignItems: "center",
                  border: `1.5px solid ${COLORS.border}`, borderRadius: 12,
                  overflow: "hidden", marginBottom: 12,
                }}>
                  <span style={{ padding: "0 14px", fontSize: 18, fontWeight: 600, color: COLORS.textDim }}>$</span>
                  <input type="number" placeholder="0" min="0" step="0.01"
                    value={amount} onChange={e => setAmount(e.target.value)}
                    style={{ flex: 1, padding: "14px 0", border: "none", outline: "none", fontSize: 24, fontWeight: 700, fontFamily: "'JetBrains Mono', monospace", color: COLORS.text, background: "transparent" }}
                  />
                </div>
                {/* Quick amounts */}
                <div style={{ display: "flex", gap: 8, marginBottom: 16 }}>
                  {presets.map(p => (
                    <button key={p} onClick={() => setAmount(String(p))} style={{
                      flex: 1, padding: "10px 0", borderRadius: 8, cursor: "pointer",
                      background: amount === String(p) ? COLORS.accentGlow : "rgba(255,255,255,0.03)",
                      color: amount === String(p) ? COLORS.accentLight : COLORS.textDim,
                      border: `1px solid ${amount === String(p) ? "rgba(124,58,237,0.3)" : COLORS.border}`,
                      fontSize: 14, fontWeight: 600, fontFamily: "'JetBrains Mono', monospace",
                    }}>${p}</button>
                  ))}
                </div>
                {/* Buy Summary */}
                <div style={{ background: "rgba(255,255,255,0.02)", borderRadius: 10, padding: 14, marginBottom: 16 }}>
                  {[
                    { label: "Avg price", value: `${priceCents}¢` },
                    { label: "Est. shares", value: amountNum > 0 ? (amountNum / price).toFixed(1) : "0" },
                    { label: buyFeeLabel, value: `$${fee_buy.toFixed(2)}`, color: COLORS.textDim },
                    { label: "To win (if correct)", value: `$${potentialReturn}`, color: COLORS.green },
                    { label: "Profit if correct", value: `+$${profitIfCorrect}`, color: COLORS.green },
                  ].map((row, i) => (
                    <div key={i} style={{ display: "flex", justifyContent: "space-between", alignItems: "center", padding: "6px 0", borderBottom: i < 4 ? `1px solid ${COLORS.border}` : "none" }}>
                      <span style={{ fontSize: 13, color: COLORS.textDim }}>{row.label}</span>
                      <span style={{ fontSize: 13, fontWeight: 600, fontFamily: "'JetBrains Mono', monospace", color: row.color || COLORS.text }}>{row.value}</span>
                    </div>
                  ))}
                </div>
              </>
            ) : (
              <>
                {/* Shares to sell */}
                <div style={{
                  display: "flex", alignItems: "center",
                  border: `1.5px solid ${COLORS.border}`, borderRadius: 12,
                  overflow: "hidden", marginBottom: 12,
                }}>
                  <input type="number" placeholder="0" min="0" max={holdingShares}
                    value={sellShares} onChange={e => setSellShares(e.target.value)}
                    style={{ flex: 1, padding: "14px", border: "none", outline: "none", fontSize: 24, fontWeight: 700, fontFamily: "'JetBrains Mono', monospace", color: COLORS.text, background: "transparent" }}
                  />
                  <span style={{ padding: "0 14px", fontSize: 13, color: COLORS.textDim }}>/ {holdingShares} shares</span>
                </div>
                {/* Quick sell presets */}
                <div style={{ display: "flex", gap: 8, marginBottom: 16 }}>
                  {sellPresets.map((p, i) => (
                    <button key={i} onClick={() => setSellShares(String(p))} style={{
                      flex: 1, padding: "10px 0", borderRadius: 8, cursor: "pointer",
                      background: sellShares === String(p) ? COLORS.accentGlow : "rgba(255,255,255,0.03)",
                      color: sellShares === String(p) ? COLORS.accentLight : COLORS.textDim,
                      border: `1px solid ${sellShares === String(p) ? "rgba(124,58,237,0.3)" : COLORS.border}`,
                      fontSize: 14, fontWeight: 600, fontFamily: "'JetBrains Mono', monospace",
                    }}>{p === holdingShares ? "MAX" : p}</button>
                  ))}
                </div>
                {/* Sell Summary */}
                <div style={{ background: "rgba(255,255,255,0.02)", borderRadius: 10, padding: 14, marginBottom: 16 }}>
                  {[
                    { label: "Current price", value: `${priceCents}¢` },
                    { label: "Gross proceeds", value: `$${sellGross.toFixed(2)}` },
                    { label: sellFeeLabel, value: `-$${sellFee.toFixed(2)}`, color: COLORS.textDim },
                    { label: "You receive", value: `$${sellNet.toFixed(2)}`, color: COLORS.text },
                    { label: "Cost basis", value: `$${sellCostBasis.toFixed(2)}`, color: COLORS.textDim },
                    { label: "Realized P&L", value: `${sellRealizedPnl >= 0 ? "+" : ""}$${sellRealizedPnl.toFixed(2)}`, color: sellRealizedPnl >= 0 ? COLORS.green : COLORS.red },
                  ].map((row, i) => (
                    <div key={i} style={{ display: "flex", justifyContent: "space-between", alignItems: "center", padding: "6px 0", borderBottom: i < 5 ? `1px solid ${COLORS.border}` : "none" }}>
                      <span style={{ fontSize: 13, color: COLORS.textDim }}>{row.label}</span>
                      <span style={{ fontSize: 13, fontWeight: 600, fontFamily: "'JetBrains Mono', monospace", color: row.color || COLORS.text }}>{row.value}</span>
                    </div>
                  ))}
                </div>
              </>
            )}

            {/* Action Button */}
            <button onClick={() => { handleBet(); const hasInput = tradeMode === "buy" ? amount : sellShares; if (hasInput) setTimeout(() => setBetSheetOpen(false), 1600); }} disabled={loading || (tradeMode === "buy" ? !amount : !sellShares || sellSharesNum > holdingShares)} style={{
              width: "100%", padding: "16px 0", borderRadius: 12,
              background: (tradeMode === "buy" ? !amount : !sellShares) ? COLORS.textDim
                : tradeMode === "sell" ? `linear-gradient(135deg, ${COLORS.red}, #DC2626)`
                : choice === "YES"
                  ? `linear-gradient(135deg, ${COLORS.green}, #16A34A)`
                  : `linear-gradient(135deg, ${COLORS.red}, #DC2626)`,
              border: "none", color: "white", cursor: (tradeMode === "buy" ? !amount : !sellShares) ? "not-allowed" : "pointer",
              fontSize: 17, fontWeight: 700, fontFamily: "'DM Sans', sans-serif",
              opacity: loading ? 0.7 : 1,
              boxShadow: "0 4px 20px rgba(0,0,0,0.3)",
              display: "flex", alignItems: "center", justifyContent: "center", gap: 8,
            }}>
              {loading && <div style={{ width: 18, height: 18, border: "2px solid rgba(255,255,255,0.3)", borderTopColor: "white", borderRadius: "50%", animation: "spin 0.6s linear infinite" }} />}
              {loading
                ? (tradeMode === "buy" ? "Placing bet..." : "Selling...")
                : tradeMode === "buy"
                  ? `Buy ${choice === "YES" ? "Yes" : "No"} ${amount ? `$${amount}` : ""}`
                  : `Sell ${sellSharesNum} ${choice} Share${sellSharesNum !== 1 ? "s" : ""}`
              }
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

export default BetDetailView;
