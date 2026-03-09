export const DARK_THEME = {
  bg: "#0B0E1A",
  surface: "#121729",
  surfaceHover: "#1A2038",
  border: "rgba(255,255,255,0.06)",
  borderHover: "rgba(139,92,246,0.3)",
  text: "#E8E8ED",
  textMuted: "#8B8FA3",
  textDim: "#5A5E72",
  accent: "#7C3AED",
  accentLight: "#A78BFA",
  accentGlow: "rgba(124,58,237,0.15)",
  green: "#22C55E",
  greenBg: "rgba(34,197,94,0.1)",
  red: "#EF4444",
  redBg: "rgba(239,68,68,0.1)",
  blue: "#3B82F6",
  blueBg: "rgba(59,130,246,0.1)",
  gold: "#F59E0B",
};

export const LIGHT_THEME = {
  bg: "#F5F5F7",
  surface: "#FFFFFF",
  surfaceHover: "#F0F0F2",
  border: "rgba(0,0,0,0.08)",
  borderHover: "rgba(139,92,246,0.3)",
  text: "#1A1A2E",
  textMuted: "#6B7085",
  textDim: "#9CA0B0",
  accent: "#7C3AED",
  accentLight: "#7C3AED",
  accentGlow: "rgba(124,58,237,0.1)",
  green: "#16A34A",
  greenBg: "rgba(22,163,74,0.08)",
  red: "#DC2626",
  redBg: "rgba(220,38,38,0.08)",
  blue: "#2563EB",
  blueBg: "rgba(37,99,235,0.08)",
  gold: "#D97706",
};

// Mutable reference for current theme
let COLORS = DARK_THEME;

export const getColors = () => COLORS;
export const setColors = (theme) => { COLORS = theme; };
export { COLORS };
