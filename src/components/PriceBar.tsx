import React from "react";
import { COLORS } from "../theme";

function PriceBar({ yesPrice }) {
  const noPrice = 1 - yesPrice;
  const yesPct = Math.round(yesPrice * 100);
  const noPct = Math.round(noPrice * 100);

  return (
    <div>
      <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 6 }}>
        <span style={{ fontSize: 12, fontWeight: 600, color: COLORS.green }}>Yes {yesPct}¢</span>
        <span style={{ fontSize: 12, fontWeight: 600, color: COLORS.red }}>No {noPct}¢</span>
      </div>
      <div style={{
        display: "flex", height: 6, borderRadius: 3, overflow: "hidden",
        background: COLORS.redBg,
      }}>
        <div style={{
          width: `${yesPct}%`, background: `linear-gradient(90deg, ${COLORS.green}, #4ADE80)`,
          borderRadius: 3, transition: "width 0.6s ease",
        }} />
      </div>
    </div>
  );
}

export default PriceBar;
