import React, { useEffect, useMemo, useState } from "react";
import { COLORS } from "../theme";
import { EXPLORE_CATEGORIES } from "../data/exploreData";
import { useMarkets } from "../hooks/useApi";
import MarketCard from "../components/MarketCard";
import type { MarketItem } from "../types/domain";

type ExploreViewProps = {
  onBack?: () => void;
  onBet?: (...args: any[]) => void;
  liveOnlyMode?: boolean;
};

const SPORTS_MAP: Array<{ keys: string[]; label: string }> = [
  { keys: ["football", "soccer"], label: "Football" },
  { keys: ["baseball"], label: "Baseball" },
  { keys: ["basketball"], label: "Basketball" },
  { keys: ["mma", "boxing"], label: "MMA / Boxing" },
  { keys: ["esports"], label: "Esports" },
  { keys: ["f1", "formula"], label: "F1" },
];

const LEAGUE_MAP: Array<{ keys: string[]; label: string }> = [
  { keys: ["champions league", "ucl"], label: "UCL" },
  { keys: ["europa league", "uel"], label: "UEL" },
  { keys: ["conference league", "europa conference", "uecl"], label: "UECL" },
  { keys: ["premier league", "epl"], label: "EPL" },
  { keys: ["la liga"], label: "La Liga" },
  { keys: ["serie a"], label: "Serie A" },
  { keys: ["bundesliga"], label: "Bundesliga" },
  { keys: ["k league", "k-league", "kleague"], label: "K-League" },
];

const LIVE_SPORT_ORDER = ["Football", "Basketball", "Baseball", "MMA / Boxing", "Esports", "F1"];
const SPORT_LEAGUES: Record<string, string[]> = {
  Football: ["UCL", "UEL", "UECL", "EPL", "La Liga", "Serie A", "Bundesliga", "K-League"],
  Basketball: ["NBA", "KBL", "EuroLeague"],
  Baseball: ["MLB", "KBO", "NPB"],
  "MMA / Boxing": ["UFC", "Boxing"],
  Esports: ["LoL", "Valorant", "CS2"],
  F1: ["Formula 1"],
};

const normalize = (v: unknown) => String(v ?? "").toLowerCase();
const normalizeToken = (v: unknown) => normalize(v).replace(/[_-]+/g, " ").replace(/\s+/g, " ").trim();
const detect = (text: string, table: Array<{ keys: string[]; label: string }>, fallback: string) =>
  table.find((r) => r.keys.some((k) => text.includes(k)))?.label ?? fallback;
const toLiveSportLabel = (value: string) => detect(normalizeToken(value), SPORTS_MAP, "Football");
const toLiveLeagueLabel = (value: string) => detect(normalizeToken(value), LEAGUE_MAP, "All");

const extractTeams = (title: string) => {
  const src = title.replace(/\?/g, "").trim();
  for (const sep of [/\s+vs\.?\s+/i, /\s+v\s+/i, /\s+@\s+/]) {
    const parts = src.split(sep).map((p) => p.trim()).filter(Boolean);
    if (parts.length >= 2) return [parts[0], parts[1]];
  }
  return [] as string[];
};

function ExploreView({ onBack = () => {}, onBet = () => {}, liveOnlyMode = false }: ExploreViewProps = {}) {
  const [search, setSearch] = useState("");
  const [activeCat, setActiveCat] = useState(liveOnlyMode ? "live" : "trending");
  const [liveScope, setLiveScope] = useState<"live" | "all">(liveOnlyMode ? "live" : "all");
  const [activeSub, setActiveSub] = useState<string | null>(null);
  const [activeSub2, setActiveSub2] = useState<string | null>(null);
  const [sortBy, setSortBy] = useState("volume");
  const isLive = activeCat === "live";

  // API: fetch markets
  const { markets: MARKETS, loading, refresh } = useMarkets(activeCat, { includePrematchLive: isLive });

  const currentCat = EXPLORE_CATEGORIES.find(c => c.id === activeCat);
  const currentSub = currentCat?.subs?.find(s => s.id === activeSub);

  useEffect(() => {
    if (liveOnlyMode) {
      setActiveCat("live");
      setLiveScope("live");
      setActiveSub(null);
      setActiveSub2(null);
    }
  }, [liveOnlyMode]);
  useEffect(() => {
    if (!liveOnlyMode && activeCat === "live") {
      setActiveCat("trending");
      setActiveSub(null);
      setActiveSub2(null);
      setLiveScope("all");
    }
  }, [liveOnlyMode, activeCat]);

  useEffect(() => {
    if (!isLive) return;
    const interval = window.setInterval(() => {
      void refresh();
    }, 20000);
    const onFocus = () => { void refresh(); };
    window.addEventListener("focus", onFocus);
    return () => {
      window.clearInterval(interval);
      window.removeEventListener("focus", onFocus);
    };
  }, [isLive, refresh]);

  const parsedLive = useMemo(
    () =>
      MARKETS.map((m: any) => {
        const tags: string[] = Array.isArray(m?.tags) ? m.tags : [];
        const text = normalizeToken(`${m?.title ?? ""} ${tags.join(" ")}`);
        const sportRaw = m?.sport ?? tags[0] ?? m?.category;
        const leagueRaw = m?.league ?? tags[1] ?? "";
        const homeTeam = m?.homeTeam ?? "";
        const awayTeam = m?.awayTeam ?? "";
        const parsedTeams = extractTeams(String(m?.title ?? ""));
        const teams = (homeTeam && awayTeam) ? [String(homeTeam), String(awayTeam)] : parsedTeams;
        return {
          market: m,
          sport: sportRaw ? toLiveSportLabel(String(sportRaw)) : detect(text, SPORTS_MAP, "Football"),
          league: (() => {
            if (!leagueRaw) return detect(text, LEAGUE_MAP, "All");
            const mapped = toLiveLeagueLabel(String(leagueRaw));
            return mapped === "All" ? detect(text, LEAGUE_MAP, "All") : mapped;
          })(),
          teams,
        };
      }),
    [MARKETS]
  );

  const scopedLive = useMemo(
    () =>
      parsedLive.filter((x) =>
        liveScope === "live"
          ? normalize((x.market as any)?.category) === "live"
          : true
      ),
    [parsedLive, liveScope]
  );

  const liveSports = useMemo(() => {
    const fromData = Array.from(new Set(scopedLive.map((x) => x.sport)));
    return Array.from(new Set([...LIVE_SPORT_ORDER, ...fromData]));
  }, [scopedLive]);
  const liveLeagues = useMemo(
    () => {
      const defaultLeagues = activeSub ? (SPORT_LEAGUES[activeSub] || []) : [];
      const fromData = Array.from(new Set(scopedLive.filter((x) => !activeSub || x.sport === activeSub).map((x) => x.league)));
      return Array.from(new Set([...defaultLeagues, ...fromData]));
    },
    [scopedLive, activeSub]
  );
  useEffect(() => {
    setActiveSub2(null);
  }, [activeSub]);

  const sortOptions = [
    { id: "volume", label: "Trending" },
    { id: "newest", label: "Newest" },
    { id: "end", label: "End" },
  ];

  const isEndedMarket = (m: MarketItem) => {
    const status = String(m.status ?? "").toUpperCase();
    const phase = String((m as any).phase ?? "").toUpperCase();
    return phase === "FINISHED" || phase === "SETTLED" || status === "SETTLED" || status === "CANCELLED";
  };

  const isWithinEndedRetention = (m: MarketItem, days = 7) => {
    const base = (m as any).matchTime || m.bettingEndAt;
    if (!base) return false;
    const ts = new Date(String(base)).getTime();
    if (Number.isNaN(ts)) return false;
    return Date.now() - ts <= days * 24 * 60 * 60 * 1000;
  };

  // Filter markets
  let markets: MarketItem[] = [...MARKETS];
  if (search) {
    const q = search.toLowerCase();
    markets = markets.filter((m: MarketItem) => m.title.toLowerCase().includes(q));
  }
  if (!isLive && activeCat !== "trending") {
    markets = markets.filter((m: MarketItem) => normalize(m.category) === normalize(activeCat));
  } else {
    // LIVE data must not leak into non-LIVE buckets.
    if (!isLive) {
      markets = markets.filter((m: MarketItem) => normalize(m.category) !== "live");
    }
  }
  if (isLive) {
    const liveIds = scopedLive
      .filter((x) =>
        (!activeSub || x.sport === activeSub) &&
        (!activeSub2 || x.league === activeSub2)
      )
      .map((x) => x.market.id);
    markets = markets.filter((m) => liveIds.includes(m.id));
    // LIVE 기본 화면은 진행중/사전 경기만 노출
    if (sortBy !== "end") {
      markets = markets.filter((m) => !isEndedMarket(m));
    }
    // End 버튼: 종료 경기만 + 종료 후 7일 이내만 노출
    if (sortBy === "end") {
      markets = markets.filter((m) => isEndedMarket(m) && isWithinEndedRetention(m, 7));
    }
  }
  // Sort
  if (sortBy === "volume") {
    markets.sort((a: MarketItem, b: MarketItem) => parseFloat(b.volume.replace(/[k,]/g, "")) - parseFloat(a.volume.replace(/[k,]/g, "")));
  } else if (sortBy === "newest") {
    markets.sort((a: MarketItem, b: MarketItem) => {
      const ta = new Date(String((a as any).createdAt ?? 0)).getTime();
      const tb = new Date(String((b as any).createdAt ?? 0)).getTime();
      return tb - ta;
    });
  } else if (sortBy === "end") {
    markets.sort((a: MarketItem, b: MarketItem) => {
      const ta = new Date(String((a as any).matchTime || a.bettingEndAt || 0)).getTime();
      const tb = new Date(String((b as any).matchTime || b.bettingEndAt || 0)).getTime();
      return tb - ta;
    });
  }

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
          Home
        </button>

        {/* Inline search */}
        <div style={{
          flex: 1, maxWidth: 480,
          display: "flex", alignItems: "center", gap: 8,
          background: COLORS.surface, border: `1px solid ${COLORS.border}`,
          borderRadius: 10, padding: "7px 14px",
        }}>
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke={COLORS.textDim} strokeWidth="2"><circle cx="11" cy="11" r="8"/><path d="M21 21l-4.35-4.35"/></svg>
          <input
            type="text" placeholder="Trade on anything"
            value={search} onChange={e => setSearch(e.target.value)}
            style={{ flex: 1, background: "none", border: "none", outline: "none", color: COLORS.text, fontSize: 14, fontFamily: "'DM Sans', sans-serif" }}
          />
          {search && (
            <button onClick={() => setSearch("")} style={{ background: "none", border: "none", cursor: "pointer", color: COLORS.textDim, display: "flex", padding: 0 }}>
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M18 6L6 18M6 6l12 12"/></svg>
            </button>
          )}
        </div>
      </div>

      <div style={{ maxWidth: 1320, margin: "0 auto", padding: "20px 24px" }}>

        <div style={{
          display: "flex", gap: 6, marginBottom: 12, overflowX: "auto",
          paddingBottom: 4, msOverflowStyle: "none", scrollbarWidth: "none",
        }}>
          {(liveOnlyMode ? [{ id: "live", label: "LIVE" }] : EXPLORE_CATEGORIES.filter((cat) => cat.id !== "live")).map(cat => (
            <button key={cat.id} onClick={() => { if (!liveOnlyMode) { setActiveCat(cat.id); setActiveSub(null); setActiveSub2(null); } }} style={{
              padding: "8px 16px", borderRadius: 8, border: "none", cursor: liveOnlyMode ? "default" : "pointer",
              background: activeCat === cat.id ? COLORS.accentGlow : "transparent",
              color: activeCat === cat.id ? COLORS.accentLight : COLORS.textMuted,
              fontSize: 14, fontWeight: activeCat === cat.id ? 600 : 500,
              fontFamily: "'DM Sans', sans-serif", whiteSpace: "nowrap",
              transition: "all 0.15s",
            }}>{cat.label}</button>
          ))}
        </div>

        {/* Sub Categories */}
        {isLive && (
          <div style={{
            display: "flex", gap: 4, marginBottom: 12, overflowX: "auto",
            paddingBottom: 4, msOverflowStyle: "none", scrollbarWidth: "none",
          }}>
            <button onClick={() => { setLiveScope("live"); setActiveSub(null); setActiveSub2(null); }} style={{
              padding: "5px 12px", borderRadius: 6, border: "none", cursor: "pointer",
              background: liveScope === "live" ? "rgba(255,255,255,0.06)" : "transparent",
              color: liveScope === "live" ? COLORS.text : COLORS.textDim, fontSize: 12, fontWeight: 600,
              fontFamily: "'DM Sans', sans-serif", whiteSpace: "nowrap",
            }}>LIVE</button>
            <button onClick={() => { setLiveScope("all"); setActiveSub(null); setActiveSub2(null); }} style={{
              padding: "5px 12px", borderRadius: 6, border: "none", cursor: "pointer",
              background: liveScope === "all" && !activeSub ? "rgba(255,255,255,0.06)" : "transparent",
              color: liveScope === "all" && !activeSub ? COLORS.text : COLORS.textDim, fontSize: 12, fontWeight: 500,
              fontFamily: "'DM Sans', sans-serif", whiteSpace: "nowrap",
            }}>All Sports</button>
            {liveSports.map((s) => (
              <button key={s} onClick={() => { setLiveScope("all"); setActiveSub(s); }} style={{
                padding: "5px 12px", borderRadius: 6, border: "none", cursor: "pointer",
                background: activeSub === s ? "rgba(255,255,255,0.06)" : "transparent",
                color: activeSub === s ? COLORS.text : COLORS.textDim, fontSize: 12, fontWeight: 500,
                fontFamily: "'DM Sans', sans-serif", whiteSpace: "nowrap",
              }}>{s}</button>
            ))}
          </div>
        )}
        {!isLive && (currentCat?.subs?.length ?? 0) > 0 && (
          <div style={{
            display: "flex", gap: 4, marginBottom: 12, overflowX: "auto",
            paddingBottom: 4, msOverflowStyle: "none", scrollbarWidth: "none",
          }}>
            <button onClick={() => { setActiveSub(null); setActiveSub2(null); }} style={{
              padding: "5px 12px", borderRadius: 6, border: "none", cursor: "pointer",
              background: !activeSub ? "rgba(255,255,255,0.06)" : "transparent",
              color: !activeSub ? COLORS.text : COLORS.textDim,
              fontSize: 12, fontWeight: 500, fontFamily: "'DM Sans', sans-serif",
              whiteSpace: "nowrap", transition: "all 0.15s",
            }}>All {currentCat?.label ?? "Category"}</button>
            {(currentCat?.subs ?? []).map((sub) => (
              <button key={sub.id} onClick={() => { setActiveSub(sub.id); setActiveSub2(null); }} style={{
                padding: "5px 12px", borderRadius: 6, border: "none", cursor: "pointer",
                background: activeSub === sub.id ? "rgba(255,255,255,0.06)" : "transparent",
                color: activeSub === sub.id ? COLORS.text : COLORS.textDim,
                fontSize: 12, fontWeight: 500, fontFamily: "'DM Sans', sans-serif",
                whiteSpace: "nowrap", transition: "all 0.15s",
              }}>{sub.label}</button>
            ))}
          </div>
        )}

        {/* Sub2 Categories (e.g. EPL, La Liga under Football) */}
        {isLive && (
          <div style={{
            display: "flex", gap: 4, marginBottom: 12, overflowX: "auto",
            paddingBottom: 4,
          }}>
            <button onClick={() => setActiveSub2(null)} style={{
              padding: "4px 10px", borderRadius: 5, border: `1px solid ${!activeSub2 ? COLORS.accent : COLORS.border}`,
              cursor: "pointer", background: !activeSub2 ? COLORS.accentGlow : "transparent",
              color: !activeSub2 ? COLORS.accentLight : COLORS.textDim, fontSize: 11, fontWeight: 500,
              fontFamily: "'DM Sans', sans-serif", whiteSpace: "nowrap",
            }}>All</button>
            {liveLeagues.map((league) => (
              <button key={league} onClick={() => setActiveSub2(league)} style={{
                padding: "4px 10px", borderRadius: 5, border: `1px solid ${activeSub2 === league ? COLORS.accent : COLORS.border}`,
                cursor: "pointer", background: activeSub2 === league ? COLORS.accentGlow : "transparent",
                color: activeSub2 === league ? COLORS.accentLight : COLORS.textDim, fontSize: 11, fontWeight: 500,
                fontFamily: "'DM Sans', sans-serif", whiteSpace: "nowrap",
              }}>{league}</button>
            ))}
          </div>
        )}
        {!isLive && (currentSub?.subs2?.length ?? 0) > 0 && (
          <div style={{
            display: "flex", gap: 4, marginBottom: 12, overflowX: "auto",
            paddingBottom: 4,
          }}>
            <button onClick={() => setActiveSub2(null)} style={{
              padding: "4px 10px", borderRadius: 5, border: `1px solid ${!activeSub2 ? COLORS.accent : COLORS.border}`,
              cursor: "pointer",
              background: !activeSub2 ? COLORS.accentGlow : "transparent",
              color: !activeSub2 ? COLORS.accentLight : COLORS.textDim,
              fontSize: 11, fontWeight: 500, fontFamily: "'DM Sans', sans-serif",
              whiteSpace: "nowrap", transition: "all 0.15s",
            }}>All</button>
            {(currentSub?.subs2 ?? []).map((s2) => (
              <button key={s2} onClick={() => setActiveSub2(s2)} style={{
                padding: "4px 10px", borderRadius: 5, border: `1px solid ${activeSub2 === s2 ? COLORS.accent : COLORS.border}`,
                cursor: "pointer",
                background: activeSub2 === s2 ? COLORS.accentGlow : "transparent",
                color: activeSub2 === s2 ? COLORS.accentLight : COLORS.textDim,
                fontSize: 11, fontWeight: 500, fontFamily: "'DM Sans', sans-serif",
                whiteSpace: "nowrap", transition: "all 0.15s",
              }}>{s2}</button>
            ))}
          </div>
        )}

        {/* Title + Sort */}
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 20 }}>
          <div>
            <h1 style={{ fontSize: 22, fontWeight: 700, fontFamily: "'Outfit', sans-serif" }}>
              Markets
            </h1>
            <div style={{ fontSize: 13, color: COLORS.textDim, marginTop: 2 }}>
              {markets.length} market{markets.length !== 1 ? "s" : ""}
              {search && <> matching "<span style={{ color: COLORS.accentLight }}>{search}</span>"</>}
            </div>
          </div>
          <div style={{ display: "flex", gap: 6 }}>
            {sortOptions.map(s => (
              <button key={s.id} onClick={() => setSortBy(s.id)} style={{
                padding: "6px 14px", borderRadius: 8,
                background: sortBy === s.id ? COLORS.surface : "transparent",
                border: `1px solid ${sortBy === s.id ? COLORS.border : "transparent"}`,
                color: sortBy === s.id ? COLORS.text : COLORS.textDim,
                fontSize: 13, fontWeight: 500, fontFamily: "'DM Sans', sans-serif",
                cursor: "pointer", transition: "all 0.15s",
              }}>{s.label}</button>
            ))}
          </div>
        </div>

        {/* Market Card Grid - Home style */}
        <div style={{
          display: "grid", gridTemplateColumns: "repeat(3, 1fr)", gap: 14,
          maxHeight: "760px", overflowY: "auto", paddingRight: 4,
        }} className="market-grid-explore">
          {markets.map((market: MarketItem, i: number) => (
            <MarketCard key={market.id} market={market} index={i} onBet={onBet} />
          ))}
        </div>

        {markets.length === 0 && (
          <div style={{ textAlign: "center", padding: "60px 20px", color: COLORS.textDim }}>
            <div style={{ fontSize: 40, marginBottom: 12 }}>🔍</div>
            <div style={{ fontSize: 16, fontWeight: 600, marginBottom: 4, color: COLORS.textMuted }}>No markets found</div>
            <div style={{ fontSize: 13 }}>
              Try adjusting your search or filters
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

export default ExploreView;
