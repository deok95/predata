import React from "react";
import { COLORS } from "../theme";

function Logo() {
  return (
    <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
      <div style={{ position: "relative", flexShrink: 0 }}>
        <svg viewBox="0 0 40 40" width="36" height="36" fill="none">
          <rect width="40" height="40" rx="12" fill="#7C3AED" style={{ filter: "drop-shadow(0 4px 6px rgba(124,58,237,0.3))" }} />
          <path d="M12 10V30H20C25.5228 30 30 25.5228 30 20C30 14.4772 25.5228 10 20 10H12Z" stroke="white" strokeWidth="2.5" strokeLinejoin="round" />
          <circle cx="20" cy="20" r="3" fill="white" style={{ animation: "dotBlink 2.5s ease-in-out infinite" }} />
          <path d="M12 20H17" stroke="white" strokeWidth="2" strokeLinecap="round" />
        </svg>
      </div>
      <div style={{ fontSize: 19, fontWeight: 700, letterSpacing: "0.5px", lineHeight: 1, fontFamily: "'Outfit', sans-serif" }}>
        PRE<span style={{ color: COLORS.accentLight }}>(D)</span>ATA
      </div>
    </div>
  );
}

export default Logo;
