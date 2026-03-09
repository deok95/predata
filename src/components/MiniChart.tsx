import React from "react";
import { COLORS } from "../theme";

function MiniChart({ change }) {
  const positive = change >= 0;
  const points = positive
    ? "0,20 10,18 20,15 30,16 40,10 50,12 60,5 70,3"
    : "0,3 10,5 20,8 30,6 40,12 50,10 60,16 70,20";

  return (
    <svg width="70" height="24" viewBox="0 0 70 24" style={{ display: "block" }}>
      <defs>
        <linearGradient id={`grad-${change}`} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor={positive ? COLORS.green : COLORS.red} stopOpacity="0.3" />
          <stop offset="100%" stopColor={positive ? COLORS.green : COLORS.red} stopOpacity="0" />
        </linearGradient>
      </defs>
      <polygon
        points={`${points} 70,24 0,24`}
        fill={`url(#grad-${change})`}
      />
      <polyline
        points={points}
        fill="none"
        stroke={positive ? COLORS.green : COLORS.red}
        strokeWidth="1.5"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

export default MiniChart;
