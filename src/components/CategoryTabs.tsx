import React from "react";
import { COLORS } from "../theme";
import { CATEGORIES } from "../data/mockMarkets";

function CategoryTabs({ active, onSelect }) {
  return (
    <div style={{
      display: "flex", gap: 4, padding: "0 0 12px 0", overflowX: "auto",
      borderBottom: `1px solid ${COLORS.border}`, marginBottom: 16,
    }}>
      {CATEGORIES.map((cat) => (
        <button key={cat.id} onClick={() => onSelect(cat.id)} style={{
          padding: "8px 16px", borderRadius: 8, border: "none", cursor: "pointer",
          background: active === cat.id ? COLORS.accent : "transparent",
          color: active === cat.id ? "white" : COLORS.textMuted,
          fontSize: 13, fontWeight: 500, whiteSpace: "nowrap",
          fontFamily: "'DM Sans', sans-serif",
          transition: "all 0.2s ease",
          display: "flex", alignItems: "center", gap: 6,
        }}
        onMouseEnter={e => { if (active !== cat.id) (e.currentTarget as HTMLElement).style.background = COLORS.surfaceHover; }}
        onMouseLeave={e => { if (active !== cat.id) (e.currentTarget as HTMLElement).style.background = "transparent"; }}
        >
          {cat.label}
        </button>
      ))}
    </div>
  );
}

export default CategoryTabs;
