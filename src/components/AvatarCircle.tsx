import React from "react";
import { COLORS } from "../theme";

type Props = {
  avatar: string;
  name: string;
  size?: number;
  fontSize?: number;
  style?: React.CSSProperties;
};

export default function AvatarCircle({ avatar, name, size = 32, fontSize, style }: Props) {
  const isUrl = avatar?.startsWith("http");
  const initials = (name || "?").slice(0, 2).toUpperCase();
  const fSize = fontSize ?? Math.max(8, Math.floor(size * 0.35));

  const base: React.CSSProperties = {
    width: size,
    height: size,
    borderRadius: "50%",
    flexShrink: 0,
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    overflow: "hidden",
    ...style,
  };

  if (isUrl) {
    return (
      <div style={base}>
        <img
          src={avatar}
          alt={name}
          style={{ width: "100%", height: "100%", objectFit: "cover" }}
          onError={(e) => {
            // Fallback to initials on load error
            (e.currentTarget.parentElement as HTMLElement).style.background =
              `linear-gradient(135deg, ${COLORS.accent}, #5B21B6)`;
            e.currentTarget.style.display = "none";
            const span = document.createElement("span");
            span.textContent = initials;
            span.style.cssText = `font-size:${fSize}px;font-weight:700;color:white`;
            e.currentTarget.parentElement?.appendChild(span);
          }}
        />
      </div>
    );
  }

  const bgColor = avatar?.startsWith("#") ? avatar : COLORS.accent;
  return (
    <div style={{ ...base, background: `linear-gradient(135deg, ${bgColor}, ${bgColor}88)` }}>
      <span style={{ fontSize: fSize, fontWeight: 700, color: "white" }}>{initials}</span>
    </div>
  );
}
