import { getColors } from "../theme";

export const getStyles = (colors = getColors()) => ({
  global: `
    @import url('https://fonts.googleapis.com/css2?family=DM+Sans:ital,opsz,wght@0,9..40,300;0,9..40,400;0,9..40,500;0,9..40,600;0,9..40,700&family=JetBrains+Mono:wght@400;500&family=Outfit:wght@500;600;700;800&display=swap');

    * { margin: 0; padding: 0; box-sizing: border-box; }
    html, body {
      width: 100%;
      max-width: 100%;
      overflow-x: hidden;
    }
    body { background: ${colors.bg}; color: ${colors.text}; font-family: 'DM Sans', sans-serif; }

    ::-webkit-scrollbar { width: 6px; }
    ::-webkit-scrollbar-track { background: transparent; }
    ::-webkit-scrollbar-thumb { background: ${colors.border}; border-radius: 3px; }

    @keyframes fadeUp {
      from { opacity: 0; transform: translateY(12px); }
      to { opacity: 1; transform: translateY(0); }
    }
    @keyframes slideIn {
      from { opacity: 0; transform: translateX(-8px); }
      to { opacity: 1; transform: translateX(0); }
    }
    @keyframes pulse {
      0%, 100% { opacity: 1; }
      50% { opacity: 0.5; }
    }
    @keyframes shimmer {
      0% { background-position: -200% 0; }
      100% { background-position: 200% 0; }
    }
    @keyframes glow {
      0%, 100% { box-shadow: 0 0 20px rgba(124,58,237,0.1); }
      50% { box-shadow: 0 0 30px rgba(124,58,237,0.2); }
    }
    @keyframes modalFadeIn {
      from { opacity: 0; }
      to { opacity: 1; }
    }
    @keyframes modalSlideUp {
      from { opacity: 0; transform: translateY(20px) scale(0.97); }
      to { opacity: 1; transform: translateY(0) scale(1); }
    }
    @keyframes spin {
      to { transform: rotate(360deg); }
    }
    @keyframes sheetSlideUp {
      from { transform: translateY(100%); }
      to { transform: translateY(0); }
    }
    @keyframes dotBlink {
      0%, 100% { fill: white; opacity: 1; }
      50% { fill: #7C3AED; opacity: 0.3; }
    }

    /* ── Responsive ── */
    .desktop-only { display: block; }
    .desktop-only-flex { display: flex; }
    .desktop-only-grid { display: grid; }
    .mobile-only { display: none; }
    .mobile-only-flex { display: none; }
    .main-grid { display: grid; grid-template-columns: 1fr 320px; gap: 24px; }
    .market-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; }
    .market-grid-explore { }
    .bet-detail-grid { }
    .vote-detail-grid { display: grid; grid-template-columns: 1fr 400px; gap: 32px; }
    .vote-detail-stats { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; }
    .vote-detail-panel { position: sticky; top: 72px; align-self: start; }

    @media (max-width: 1024px) {
      .main-grid { grid-template-columns: 1fr 280px; }
      .market-grid { grid-template-columns: 1fr; }
      .market-grid-explore { grid-template-columns: repeat(2, 1fr) !important; }
    }

    @media (max-width: 768px) {
      .desktop-only { display: none !important; }
      .desktop-only-flex { display: none !important; }
      .desktop-only-grid { display: none !important; }
      .mobile-only { display: block !important; }
      .mobile-only-flex { display: flex !important; }
      .mobile-only-flex { min-width: 0 !important; }
      .main-grid { grid-template-columns: 1fr; }
      .market-grid { grid-template-columns: 1fr; }
      .market-grid-explore { grid-template-columns: 1fr !important; }
      .bet-detail-grid { grid-template-columns: 1fr !important; padding-bottom: 80px !important; }
      .vote-detail-grid { grid-template-columns: 1fr !important; gap: 16px !important; padding: 16px 14px 84px !important; }
      .vote-detail-stats { grid-template-columns: 1fr !important; gap: 10px !important; }
      .vote-detail-panel { position: static !important; top: auto !important; }
    }

    .mobile-tab-bar {
      display: none; position: fixed; bottom: 0; left: 0; right: 0; z-index: 100;
      background: ${colors.bg}ee; backdrop-filter: blur(12px);
      border-top: 1px solid ${colors.border};
      padding: 8px 0 calc(8px + env(safe-area-inset-bottom, 0px));
      justify-content: space-around; align-items: center;
    }
    @media (max-width: 768px) {
      .mobile-tab-bar { display: flex; }
    }
  `,
});
