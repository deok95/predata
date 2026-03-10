export function GET() {
  return new Response(html, {
    headers: { 'Content-Type': 'text/html; charset=utf-8' },
  })
}

const html = `<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>PRE(D)ATA — Pitch Deck</title>
<link href="https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700;800;900&family=JetBrains+Mono:wght@400;500;600&display=swap" rel="stylesheet">
<style>
:root{--bg:#050505;--card:#0d0d0d;--cb:#1a1a1a;--accent:#7C3AED;--accent2:#A78BFA;--red:#FF4040;--orange:#FF8C00;--white:#FFF;--gray:#777;--light:#BBB;--dim:#444}
*{margin:0;padding:0;box-sizing:border-box}
html,body{height:100%;overflow:hidden;background:var(--bg);color:var(--white);font-family:'Outfit',sans-serif;-webkit-font-smoothing:antialiased}
.slide{position:absolute;top:0;left:0;right:0;bottom:0;display:flex;flex-direction:column;justify-content:center;padding:clamp(32px,5vh,64px) 10vw;opacity:0;transform:translateY(40px);transition:opacity .6s,transform .6s;pointer-events:none}
.slide.active{opacity:1;transform:translateY(0);pointer-events:all}
.label{font-family:'JetBrains Mono',monospace;font-size:12px;font-weight:600;letter-spacing:3px;text-transform:uppercase;color:var(--accent2);margin-bottom:clamp(10px,1.5vh,18px)}
h1{font-weight:800;font-size:clamp(28px,4.5vw,56px);line-height:1.1;margin-bottom:clamp(10px,1.5vh,20px);max-width:850px}
.a{color:var(--accent)}.a2{color:var(--accent2)}.r{color:var(--red)}.o{color:var(--orange)}
.sub{font-size:clamp(14px,1.6vw,18px);color:var(--gray);max-width:560px;line-height:1.6}
.cards{display:grid;gap:16px;margin-top:36px}.c2{grid-template-columns:repeat(2,1fr)}.c3{grid-template-columns:repeat(3,1fr)}
.card{background:var(--card);border:1px solid var(--cb);border-radius:14px;padding:28px;transition:border-color .3s}.card:hover{border-color:var(--accent)}
.card h3{font-weight:700;font-size:15px;margin-bottom:8px}.card p{color:var(--gray);font-size:13px;line-height:1.55}
.callout{background:var(--card);border-left:3px solid var(--accent);padding:16px 22px;margin-top:28px;font-size:15px;color:var(--light);max-width:600px;border-radius:0 10px 10px 0}
.flow{display:flex;align-items:center;gap:8px;margin-top:32px;flex-wrap:wrap}
.flow-s{background:var(--card);border:1px solid var(--cb);border-radius:10px;padding:12px 20px;font-family:'JetBrains Mono',monospace;font-size:12px;font-weight:500}
.flow-s.on{background:var(--accent);color:var(--bg);border-color:var(--accent);font-weight:700}
.flow-a{color:var(--accent2);font-size:16px}
.sol-grid{display:grid;grid-template-columns:1fr 1.2fr;gap:32px;margin-top:32px;align-items:center}
.sol-steps{display:flex;flex-direction:column;gap:16px}
.sol-step{display:flex;align-items:flex-start;gap:12px}
.sn{font-family:'JetBrains Mono',monospace;font-weight:700;font-size:18px;color:var(--accent);flex-shrink:0;width:28px}
.st{font-size:14px;color:var(--light);line-height:1.5}
.mock-mobile{width:min(220px,18vw);height:min(440px,46vh);border:2px solid var(--cb);border-radius:28px;overflow:hidden;box-shadow:0 0 40px rgba(124,58,237,.15);flex-shrink:0}
.mock-mobile img{width:100%;height:100%;object-fit:cover;object-position:top}
.screenshots{display:flex;gap:12px;margin-top:0;align-items:flex-start;justify-content:center}
.mock-desktop{width:100%;max-width:680px;height:360px;background:var(--card);border:2px solid var(--cb);border-radius:14px;display:flex;align-items:center;justify-content:center;flex-direction:column;gap:8px;overflow:hidden}
.prod-grid{display:grid;grid-template-columns:2fr 1fr;gap:24px;margin-top:32px;align-items:center}
.prod-links{display:flex;flex-direction:column;gap:14px}
.prod-link{background:var(--card);border:1px solid var(--cb);border-radius:14px;padding:22px;text-align:center}
.prod-link h3{font-size:14px;margin-bottom:4px}.prod-link a{color:var(--accent2);text-decoration:none;font-size:14px;font-weight:600}.prod-link p{color:var(--dim);font-size:11px;margin-top:4px}
.team-card{background:var(--card);border:1px solid var(--cb);border-radius:14px;padding:36px;display:flex;align-items:center;gap:28px;margin-top:32px;max-width:400px}
.avatar{width:72px;height:72px;border-radius:50%;background:var(--cb);display:flex;align-items:center;justify-content:center;color:var(--dim);font-size:11px;flex-shrink:0}
.tinfo h3{font-weight:700;font-size:17px;margin-bottom:2px}.tinfo .role{color:var(--accent2);font-size:12px;margin-bottom:8px}.tinfo a{color:var(--gray);font-size:12px;text-decoration:none}.tinfo a:hover{color:var(--accent)}
.contact{margin-top:40px;padding-top:28px;border-top:1px solid var(--cb)}.contact p{color:var(--dim);font-size:13px;margin-bottom:3px}.contact a{color:var(--accent2);text-decoration:none}
.flywheel{display:flex;align-items:center;justify-content:center;margin-top:40px;gap:0;flex-wrap:wrap}
.fw-step{background:var(--card);border:1px solid var(--cb);border-radius:12px;padding:16px 20px;text-align:center;width:140px}
.fw-step h4{font-size:13px;color:var(--white);margin-bottom:4px}.fw-step p{font-size:10px;color:var(--gray)}
.fw-arrow{color:var(--accent2);font-size:20px;margin:0 6px}
.fw-loop{color:var(--accent);font-size:11px;margin-top:16px;text-align:center;font-family:'JetBrains Mono',monospace}
.rev-flow{display:flex;align-items:center;justify-content:center;gap:12px;margin-top:32px;flex-wrap:wrap}
.rev-box{background:var(--card);border:1px solid var(--cb);border-radius:12px;padding:20px 24px;text-align:center}
.rev-box .pct{font-weight:800;font-size:28px;margin-bottom:4px}.rev-box .rlbl{font-size:11px;color:var(--gray)}
.rev-arrow{color:var(--accent2);font-size:20px}
.rm-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:16px;margin-top:32px}
.rm-card{background:var(--card);border:1px solid var(--cb);border-radius:14px;padding:24px}
.rm-card h3{font-size:14px;color:var(--accent2);margin-bottom:4px}.rm-card h4{font-size:13px;color:var(--white);margin-bottom:10px}
.rm-card p{font-size:11px;color:var(--gray);line-height:1.5}
.nav{position:fixed;right:28px;top:50%;transform:translateY(-50%);display:flex;flex-direction:column;gap:10px;z-index:100}
.dot{width:8px;height:8px;border-radius:50%;background:var(--dim);cursor:pointer;transition:all .3s}.dot.active{background:var(--accent);transform:scale(1.5)}
.counter{position:fixed;right:22px;bottom:28px;font-family:'JetBrains Mono',monospace;font-size:12px;color:var(--dim);z-index:100}
.topline{position:fixed;top:0;left:0;right:0;height:3px;background:linear-gradient(90deg,var(--accent),var(--accent2));z-index:200}
.cover h1{font-size:clamp(44px,6.5vw,80px)}.cover .tagline{font-size:clamp(16px,2vw,20px);color:var(--gray);margin-bottom:36px;max-width:520px}
.cover-meta{font-family:'JetBrains Mono',monospace;font-size:11px;letter-spacing:3px;color:var(--dim);text-transform:uppercase}
.hint{position:fixed;bottom:28px;left:50%;transform:translateX(-50%);font-family:'JetBrains Mono',monospace;font-size:11px;color:#FFF;z-index:100;opacity:.55}
.gtm-card{border:1px solid var(--cb);background:var(--card);transition:border-color .3s,box-shadow .3s,background .3s}.gtm-card:hover,.gtm-card.active{border-color:rgba(124,58,237,0.55);box-shadow:0 0 30px rgba(124,58,237,0.12);background:rgba(124,58,237,0.08)}
@media(max-width:768px){
.slide{padding:24px 5vw 80px;justify-content:flex-start;overflow-y:auto;-webkit-overflow-scrolling:touch}
.c2,.c3{grid-template-columns:1fr}.sol-grid{grid-template-columns:1fr}.prod-grid{grid-template-columns:1fr}.nav{display:none}.team-card{flex-direction:column;text-align:center}.rm-grid{grid-template-columns:1fr}.flywheel{flex-direction:column}.rev-flow{flex-direction:column}
.cover h1{font-size:clamp(32px,8vw,44px)!important}
.mock-mobile{width:min(42vw,180px)!important;height:min(84vw,360px)!important}
.screenshots{gap:8px!important}
.s03-grid{grid-template-columns:1fr 1fr!important;grid-template-rows:auto!important}
.s03-arrow{display:none!important}
.s05-outer{flex-direction:column!important;gap:20px!important}
.s05-left{width:100%!important}
.s06-outer{flex-direction:column!important;gap:16px!important}
.s06-bridge{display:none!important}
.s07-outer{flex-direction:column!important;gap:12px!important}
.s07-arrow{display:none!important}
.s09-outer{flex-direction:column!important;gap:12px!important}
.s09-cta{flex-direction:column!important;align-items:flex-start!important;gap:16px!important}
.s09-contacts{flex-wrap:wrap!important}
}
</style>
</head>
<body>
<div class="topline"></div>
<div class="nav" id="nav"></div>
<div class="counter" id="counter">1 / 9</div>
<div class="hint">↑ ↓ or scroll to navigate</div>

<!-- 01 TITLE -->
<div class="slide cover active" data-i="0" style="align-items:center;text-align:center">
<div style="display:flex;align-items:center;gap:20px;margin-bottom:36px">
<svg viewBox="0 0 40 40" width="80" height="80" fill="none"><rect width="40" height="40" rx="12" fill="#7C3AED" style="filter:drop-shadow(0 4px 16px rgba(124,58,237,0.4))"/><path d="M12 10V30H20C25.5228 30 30 25.5228 30 20C30 14.4772 25.5228 10 20 10H12Z" stroke="white" stroke-width="2.5" stroke-linejoin="round"/><circle cx="20" cy="20" r="3" fill="white"/><path d="M12 20H17" stroke="white" stroke-width="2" stroke-linecap="round"/></svg>
<div style="font-size:clamp(36px,4.5vw,56px);font-weight:800;letter-spacing:0.5px;line-height:1">PRE<span style="color:#A78BFA">(D)</span>ATA</div>
</div>
<p class="tagline" style="font-size:clamp(20px,2.4vw,28px);max-width:640px">If the market doesn't exist, <span class="a2">create it.</span></p>
<p style="font-family:'JetBrains Mono',monospace;font-size:clamp(13px,1.4vw,16px);letter-spacing:3px;color:var(--light);margin-top:16px">Submit. Vote. Trade. Earn.</p>
</div>

<!-- 02 PROBLEM -->
<div class="slide" data-i="1">
<div class="label">02 — Problem</div>
<h1>Current Prediction Markets<br>are <span class="r">Closed Systems.</span></h1>
<p class="sub" style="margin-bottom:24px">Demand is surging, but the gateway is too narrow.</p>
<div class="cards c2" style="max-width:720px;margin-top:18px">
<div class="card"><h3>Permissioned, Not Open.</h3><p>Users must <em>request</em> a market. The platform decides if it gets approved. User autonomy is zero.</p></div>
<div class="card"><h3>Guessing the Demand.</h3><p>Supply is dictated by platform logic, not actual demand. The topics users want to bet on are routinely ignored.</p></div>
<div class="card"><h3>Ignoring the Long-tail.</h3><p>Ops efficiency forces focus on mega-events. K-pop, local issues, niche communities — left with nothing.</p></div>
<div class="card"><h3>No Ownership, Just Betting.</h3><p>Users are passive consumers. No say in what trades, no sense of belonging, no reason to stay.</p></div>
</div>
<div style="margin-top:16px;border-top:1px solid var(--cb);padding-top:16px">
<div style="font-size:clamp(20px,2.4vw,32px);font-weight:800;letter-spacing:1px;color:var(--white);margin-bottom:6px">WE BREAK THE GATEWAY.</div>
<div style="font-size:clamp(13px,1.4vw,16px);color:var(--gray)">What if the market is chosen by the people, for the people? <span style="color:var(--accent2);font-weight:600">→ PRE(D)ATA: The Voting-Driven Market Engine.</span></div>
</div>
</div>

<!-- 03 SOLUTION -->
<div class="slide" data-i="2">
<div class="label">03 — Solution</div>
<h1>A Permissionless,<br><span class="a2">User-Driven Market Engine.</span></h1>
<div class="s03-grid" style="display:grid;grid-template-columns:1fr 44px 1fr;grid-template-rows:auto 44px auto;gap:10px;margin-top:28px;max-width:680px">
<div class="card" style="border-color:var(--accent);margin:0"><h3><span style="color:var(--accent2);margin-right:6px">01</span>Create</h3><p>Anyone can launch a market. No approval needed. No gatekeepers.</p></div>
<div class="s03-arrow" style="display:flex;align-items:center;justify-content:center;color:var(--accent2);font-size:22px">→</div>
<div class="card" style="margin:0"><h3><span style="color:var(--accent2);margin-right:6px">02</span>Vote</h3><p>The community filters the best. Crowd intelligence picks what trades.</p></div>
<div class="s03-arrow" style="display:flex;align-items:center;justify-content:center;color:var(--accent2);font-size:22px">↑</div>
<div class="s03-arrow" style="display:flex;align-items:center;justify-content:center;color:var(--accent);font-size:28px;opacity:0.5">↺</div>
<div class="s03-arrow" style="display:flex;align-items:center;justify-content:center;color:var(--accent2);font-size:22px">↓</div>
<div class="card" style="margin:0"><h3><span style="color:var(--accent2);margin-right:6px">04</span>Earn</h3><p>Platform fees redistributed to Creators and Voters. Value returns to the builders.</p></div>
<div class="s03-arrow" style="display:flex;align-items:center;justify-content:center;color:var(--accent2);font-size:22px">←</div>
<div class="card" style="margin:0"><h3><span style="color:var(--accent2);margin-right:6px">03</span>Trade</h3><p>Instant Listing for Top-Voted Markets. Liquidity flows from real demand.</p></div>
</div>
<div style="margin-top:28px;border-top:1px solid var(--cb);padding-top:20px">
<span style="font-size:clamp(15px,1.6vw,18px);font-weight:800;color:var(--white)">Create, Vote, and Earn.</span>
<span style="font-size:clamp(13px,1.3vw,15px);font-weight:400;color:var(--gray);margin-left:10px">We return the value to the people who build the market. <span style="color:var(--accent2);font-style:italic">Turn your insights into assets.</span></span>
</div>
</div>

<!-- 04 PRODUCT -->
<div class="slide" data-i="3">
<div class="label">04 — Product</div>
<h1>The Full Lifecycle<br>of <span class="a2">Prediction.</span></h1>
<div class="sol-grid" style="margin-top:20px">
<div class="sol-steps">
<p class="sub" style="margin-bottom:20px;margin-top:-8px">From Idea to Payout, all in your hands.</p>
<div class="sol-step"><div class="sn">01</div><div class="st"><strong>Curate with your Vote.</strong> Signal what the crowd cares about.</div></div>
<div class="sol-step"><div class="sn">02</div><div class="st"><strong>Trade with Instant Liquidity.</strong> Top-voted markets open immediately.</div></div>
<div class="sol-step"><div class="sn">03</div><div class="st"><strong>Automated Trustless Settlement.</strong> No human intervention. Transparent and instant.</div></div>
<div class="sol-step"><div class="sn">04</div><div class="st"><strong>Build Your Reputation.</strong> Proven forecasters earn authority — not just rank.</div></div>
</div>
<div class="screenshots" style="align-items:flex-start">
<div style="display:flex;flex-direction:column;align-items:center;gap:8px">
<div style="font-family:'JetBrains Mono',monospace;font-size:10px;letter-spacing:2px;color:var(--accent2);text-transform:uppercase">Vote</div>
<div class="mock-mobile"><img src="/screenshots/vote.png" alt="Vote"></div>
</div>
<div style="display:flex;flex-direction:column;align-items:center;gap:8px">
<div style="font-family:'JetBrains Mono',monospace;font-size:10px;letter-spacing:2px;color:var(--accent2);text-transform:uppercase">Trade</div>
<div class="mock-mobile"><img src="/screenshots/market.png" alt="Market"></div>
</div>
</div>
</div>
</div>

<!-- 05 MARKET OPPORTUNITY -->
<div class="slide" data-i="4">
<div class="label">05 — Market Opportunity</div>
<h1>A <span class="a2">$4B Wedge</span> Inside<br>a $150B Economy.</h1>
<div class="s05-outer" style="display:flex;gap:40px;align-items:flex-start;margin-top:22px;max-width:920px">
  <!-- LEFT: Nested TAM/SAM + 90/10 bar visual -->
  <div class="s05-left" style="flex-shrink:0;padding:18px;border:1px dashed rgba(107,114,128,0.3);border-radius:18px;background:rgba(107,114,128,0.04);width:310px">
    <div style="font-family:'JetBrains Mono',monospace;font-size:9px;color:#444;letter-spacing:2px;margin-bottom:3px">TAM</div>
    <div style="font-size:24px;font-weight:900;color:#4B5563;line-height:1">$150B+</div>
    <div style="font-size:10px;color:#3D4451;margin-top:2px;margin-bottom:14px">The Global Insight Economy</div>
    <!-- SAM inner box -->
    <div style="padding:14px;border:1px solid rgba(124,58,237,0.3);border-radius:12px;background:rgba(124,58,237,0.05)">
      <div style="font-family:'JetBrains Mono',monospace;font-size:9px;color:rgba(167,139,250,0.5);letter-spacing:2px;margin-bottom:3px">SAM</div>
      <div style="font-size:20px;font-weight:900;color:rgba(167,139,250,0.6);line-height:1;margin-bottom:12px">$40B</div>
      <!-- 90 / 10 segmented bar -->
      <div style="height:38px;border-radius:8px;overflow:hidden;display:flex;border:1px solid rgba(124,58,237,0.25)">
        <div style="width:82%;background:rgba(100,40,40,0.12);display:flex;align-items:center;padding-left:10px;border-right:2px dashed rgba(124,58,237,0.5)">
          <span style="font-family:'JetBrains Mono',monospace;font-size:9px;color:rgba(160,80,80,0.45);letter-spacing:1px">90% · Legacy</span>
        </div>
        <div style="width:18%;background:linear-gradient(135deg,rgba(167,139,250,0.9),rgba(124,58,237,0.7));display:flex;align-items:center;justify-content:center;box-shadow:inset 0 0 12px rgba(167,139,250,0.4)">
          <span style="font-family:'JetBrains Mono',monospace;font-size:9px;color:#FFF;font-weight:900;text-shadow:0 0 8px rgba(167,139,250,0.8)">10%</span>
        </div>
      </div>
      <!-- Below-bar labels -->
      <div style="display:flex;align-items:flex-start;gap:8px;margin-top:10px">
        <div style="flex:1">
          <div style="font-size:12px;font-weight:700;color:rgba(239,68,68,0.5)">$36B — Red Ocean</div>
          <div style="font-size:10px;color:#444;margin-top:3px;line-height:1.4">Politics · Sports · Macro<br><span style="color:#3D4451">Already dominated by giants</span></div>
        </div>
        <div style="flex-shrink:0;padding:8px 10px;border:1.5px solid rgba(167,139,250,0.8);border-radius:8px;background:rgba(124,58,237,0.22);text-align:center;min-width:80px;box-shadow:0 0 16px rgba(124,58,237,0.2)">
          <div style="font-size:18px;font-weight:900;color:#FFF;line-height:1">$4B</div>
          <div style="font-family:'JetBrains Mono',monospace;font-size:7px;color:var(--accent2);letter-spacing:1px;margin-top:3px">SOM · OUR WEDGE</div>
          <div style="font-size:8px;color:rgba(167,139,250,0.7);margin-top:2px">K-Pop · Niche · Trends</div>
        </div>
      </div>
    </div>
  </div>
  <!-- RIGHT: 3-row text breakdown -->
  <div style="flex:1;display:flex;flex-direction:column;padding-top:2px">
    <div style="padding:14px 0;border-bottom:1px solid var(--cb)">
      <div style="font-family:'JetBrains Mono',monospace;font-size:9px;color:#3D4451;letter-spacing:2px;margin-bottom:5px">TOTAL ADDRESSABLE MARKET</div>
      <div style="font-size:22px;font-weight:900;color:#4B5563;line-height:1">$150B+</div>
      <div style="font-size:12px;color:#444;margin-top:5px;line-height:1.55">Every market where information has a price — prediction, insight, and decision data across all industries.</div>
    </div>
    <div style="padding:14px 0;border-bottom:1px solid var(--cb)">
      <div style="font-family:'JetBrains Mono',monospace;font-size:9px;color:rgba(167,139,250,0.5);letter-spacing:2px;margin-bottom:5px">SERVICEABLE ADDRESSABLE MARKET</div>
      <div style="font-size:22px;font-weight:900;color:rgba(167,139,250,0.65);line-height:1">$40B</div>
      <div style="font-size:12px;color:#555;margin-top:5px;line-height:1.55">The proven prediction market pie. Polymarket and Kalshi dominate 90% with politics, sports, and macro-economics.</div>
    </div>
    <div style="padding:14px 0 0">
      <div style="font-family:'JetBrains Mono',monospace;font-size:9px;color:var(--accent2);letter-spacing:2px;margin-bottom:5px">SERVICEABLE OBTAINABLE MARKET ← OUR WEDGE</div>
      <div style="font-size:22px;font-weight:900;color:#FFF;line-height:1">$4B <span style="font-size:12px;color:var(--accent2);font-weight:600">— The 10% Beachhead</span> <span style="font-size:10px;background:rgba(124,58,237,0.3);border:1px solid rgba(167,139,250,0.5);color:var(--accent2);font-weight:700;padding:2px 7px;border-radius:4px;letter-spacing:0.5px;vertical-align:middle">Uncontested Market</span></div>
      <div style="font-size:12px;color:var(--light);margin-top:5px;line-height:1.55">The niche long-tail giants ignore. K-Pop, local debates, viral trends — an <span style="color:var(--accent2);font-weight:700">uncontested $4B gap</span> we dominate from day one, then expand into the full $150B.</div>
    </div>
  </div>
</div>
<div style="margin-top:16px;border-top:1px solid var(--cb);padding-top:12px;max-width:920px">
  <div style="font-size:clamp(12px,1.3vw,14px);color:var(--gray)">"We don't fight for the 90%. <span style="color:var(--white);font-weight:700">We dominate the 10% they left behind — and use it to expand the market to 100%.</span>"</div>
  <div style="font-size:10px;color:var(--dim);margin-top:4px">Source: Dune Analytics, Messari, Statista 2025/2026</div>
</div>
</div>

<!-- 06 BUSINESS MODEL -->
<div class="slide" data-i="5">
<div class="label">06 — Business Model</div>
<h1>From Transaction Fees<br>to <span class="a2">Data Intelligence.</span></h1>
<div class="s06-outer" style="display:flex;gap:16px;align-items:stretch;margin-top:16px;max-width:900px">
  <!-- Phase 1 -->
  <div style="flex:1;padding:22px 24px;border:1px solid rgba(124,58,237,0.45);border-radius:14px;background:rgba(20,14,36,0.9)">
    <div style="font-family:'JetBrains Mono',monospace;font-size:9px;color:rgba(167,139,250,0.6);letter-spacing:3px;margin-bottom:8px">PHASE 01 — NOW</div>
    <div style="font-size:18px;font-weight:800;color:#FFF;margin-bottom:2px">Transaction Revenue</div>
    <div style="font-size:11px;color:var(--accent2);font-style:italic;margin-bottom:20px">"Fueling the Ecosystem"</div>
    <!-- Reward highlight -->
    <div style="border:1px solid rgba(124,58,237,0.4);border-radius:10px;padding:14px 16px;background:rgba(124,58,237,0.1);margin-bottom:14px;text-align:center">
      <div style="font-size:36px;font-weight:900;color:#FFF;line-height:1;text-shadow:0 0 20px rgba(167,139,250,0.35)">Up to <span style="color:var(--accent2)">80%</span></div>
      <div style="font-size:12px;font-weight:700;color:#FFF;margin-top:4px">Reward Share</div>
      <div style="font-size:10px;color:rgba(167,139,250,0.6);margin-top:3px">Fee per Trade → Creator + Voter Pool</div>
    </div>
    <div style="font-size:11px;color:rgba(167,139,250,0.7);font-style:italic;margin-bottom:16px">Creators define their own distribution ratio.</div>
    <div style="border-top:1px solid rgba(124,58,237,0.25);padding-top:12px">
      <div style="font-size:9px;font-family:'JetBrains Mono',monospace;color:rgba(167,139,250,0.5);letter-spacing:1px;margin-bottom:4px">GOAL</div>
      <div style="font-size:12px;color:var(--light);line-height:1.5">Dominate niche markets. Accumulate <span style="color:#FFF;font-weight:700">high-resolution trend data</span> at scale.</div>
    </div>
  </div>
  <!-- Bridge arrow -->
  <div class="s06-bridge" style="display:flex;flex-direction:column;align-items:center;justify-content:center;gap:10px;padding:0 6px">
    <div style="font-family:'JetBrains Mono',monospace;font-size:8px;color:var(--accent2);text-align:center;line-height:1.6;max-width:72px;opacity:0.7">After Critical<br>Mass of Data</div>
    <div style="color:var(--accent2);font-size:32px;line-height:1;text-shadow:0 0 12px rgba(167,139,250,0.6)">→</div>
  </div>
  <!-- Phase 2 -->
  <div style="flex:1;padding:22px 24px;border:1px solid rgba(167,139,250,0.6);border-radius:14px;background:rgba(124,58,237,0.1);box-shadow:0 0 40px rgba(124,58,237,0.12)">
    <div style="font-family:'JetBrains Mono',monospace;font-size:9px;color:var(--accent2);letter-spacing:3px;margin-bottom:8px">PHASE 02 — SCALE</div>
    <div style="font-size:18px;font-weight:800;color:#FFF;margin-bottom:2px">Data-as-a-Service</div>
    <div style="font-size:11px;color:var(--accent2);font-style:italic;margin-bottom:20px">"Unlocking Proprietary Insight"</div>
    <!-- B2B targets with checkmarks -->
    <div style="display:flex;flex-direction:column;gap:12px;margin-bottom:18px">
      <div style="display:flex;gap:10px;align-items:flex-start">
        <span style="color:var(--accent2);font-size:14px;font-weight:900;flex-shrink:0;margin-top:1px">✓</span>
        <div>
          <div style="font-size:12px;font-weight:700;color:#FFF">Brands</div>
          <div style="font-size:11px;color:var(--gray);margin-top:2px">Real-time consumer sentiment &amp; trend signals</div>
        </div>
      </div>
      <div style="display:flex;gap:10px;align-items:flex-start">
        <span style="color:var(--accent2);font-size:14px;font-weight:900;flex-shrink:0;margin-top:1px">✓</span>
        <div>
          <div style="font-size:12px;font-weight:700;color:#FFF">Hedge Funds &amp; Analysts</div>
          <div style="font-size:11px;color:var(--gray);margin-top:2px">Niche prediction signals before they hit mainstream</div>
        </div>
      </div>
      <div style="display:flex;gap:10px;align-items:flex-start">
        <span style="color:var(--accent2);font-size:14px;font-weight:900;flex-shrink:0;margin-top:1px">✓</span>
        <div>
          <div style="font-size:12px;font-weight:700;color:#FFF">Enterprises</div>
          <div style="font-size:11px;color:var(--gray);margin-top:2px">B2B subscription dashboards for decision makers</div>
        </div>
      </div>
    </div>
    <div style="border-top:1px solid rgba(167,139,250,0.25);padding-top:12px">
      <div style="font-size:9px;font-family:'JetBrains Mono',monospace;color:var(--accent2);letter-spacing:1px;margin-bottom:4px">VALUE PROP</div>
      <div style="font-size:12px;color:var(--light);line-height:1.5"><span style="color:#FFF;font-weight:700">100x faster</span> than surveys. Sentiment backed by <span style="color:var(--accent2);font-weight:700">skin in the game.</span></div>
    </div>
  </div>
</div>
<div style="margin-top:18px;border-top:1px solid var(--cb);padding-top:14px;max-width:900px">
  <div style="font-size:clamp(12px,1.3vw,14px);color:var(--gray)">"Phase 1 funds growth. <span style="color:#FFF;font-weight:700">Phase 2 multiplies valuation.</span> The data moat is the endgame."</div>
</div>
</div>

<!-- 07 GO-TO-MARKET -->
<div class="slide" data-i="6">
<div class="label">07 — Go-To-Market</div>
<h1>Viral Expansion<br><span class="a2">by Nature.</span></h1>
<p class="sub" style="margin-top:-4px;margin-bottom:16px">Capturing global niche communities where engagement is highest.</p>
<div class="s07-outer" style="display:flex;align-items:stretch;gap:10px;max-width:920px">
  <!-- Phase 1 — always active -->
  <div class="gtm-card active" style="flex:1;padding:20px;border-radius:14px">
    <div style="font-family:'JetBrains Mono',monospace;font-size:9px;color:var(--accent2);letter-spacing:3px;margin-bottom:8px">PHASE 01 — INSERT</div>
    <div style="font-size:16px;font-weight:800;color:#FFF;margin-bottom:3px">Community Insertion</div>
    <div style="font-family:'JetBrains Mono',monospace;font-size:9px;color:rgba(167,139,250,0.6);letter-spacing:1px;margin-bottom:14px">INFILTRATING VIRAL SUB-CULTURES</div>
    <div style="font-size:12px;color:var(--light);line-height:1.6;margin-bottom:14px">Reddit, Discord, Twitter, Telegram sub-communities. Games, coins, creators, fandoms — wherever debate never stops, we plant the market.</div>
    <div style="border-top:1px solid rgba(124,58,237,0.25);padding-top:10px">
      <div style="font-size:9px;font-family:'JetBrains Mono',monospace;color:var(--accent2);letter-spacing:1px;margin-bottom:4px">INSIGHT</div>
      <div style="font-size:11px;color:var(--accent2);font-style:italic;">"We don't go to countries.</div>
      <div style="font-size:11px;color:var(--light);font-style:italic;">We go to sub-reddits."</div>
    </div>
  </div>
  <!-- arrow -->
  <div class="s07-arrow" style="display:flex;align-items:center;justify-content:center;flex-shrink:0;padding:0 2px">
    <span style="color:var(--accent2);font-size:20px;opacity:0.6">→</span>
  </div>
  <!-- Phase 2 -->
  <div class="gtm-card" style="flex:1;padding:20px;border-radius:14px">
    <div style="font-family:'JetBrains Mono',monospace;font-size:9px;color:var(--dim);letter-spacing:3px;margin-bottom:8px">PHASE 02 — LOOP</div>
    <div style="font-size:16px;font-weight:800;color:#FFF;margin-bottom:3px">Self-Spreading Markets</div>
    <div style="font-family:'JetBrains Mono',monospace;font-size:9px;color:rgba(167,139,250,0.4);letter-spacing:1px;margin-bottom:14px">THE "GUESS THE RESULT" LOOP</div>
    <div style="font-size:12px;color:var(--gray);line-height:1.6;margin-bottom:14px">User-created market links spread inside communities organically. Users share, friends join, new markets spawn. <span style="color:#FFF;font-weight:700">Zero CAC.</span> Users do the marketing.</div>
    <div style="border-top:1px solid var(--cb);padding-top:10px">
      <div style="font-size:9px;font-family:'JetBrains Mono',monospace;color:var(--dim);letter-spacing:1px;margin-bottom:3px">RESULT</div>
      <div style="font-size:11px;color:var(--gray)">Community-funded growth.<br>Sub-culture → mainstream.</div>
    </div>
  </div>
  <!-- arrow -->
  <div class="s07-arrow" style="display:flex;align-items:center;justify-content:center;flex-shrink:0;padding:0 2px">
    <span style="color:var(--accent2);font-size:20px;opacity:0.6">→</span>
  </div>
  <!-- Phase 3 -->
  <div class="gtm-card" style="flex:1;padding:20px;border-radius:14px">
    <div style="font-family:'JetBrains Mono',monospace;font-size:9px;color:var(--dim);letter-spacing:3px;margin-bottom:8px">PHASE 03 — SCALE</div>
    <div style="font-size:16px;font-weight:800;color:#FFF;margin-bottom:3px">Global Creator Protocol</div>
    <div style="font-family:'JetBrains Mono',monospace;font-size:9px;color:rgba(167,139,250,0.4);letter-spacing:1px;margin-bottom:14px">THE WORLD'S PREDICTION ENGINE</div>
    <div style="font-size:12px;color:var(--gray);line-height:1.6;margin-bottom:14px">Creators worldwide open their own prediction markets to earn. Every creator is a distribution channel. <span style="color:#FFF;font-weight:700">The platform scales itself.</span></div>
    <div style="border-top:1px solid var(--cb);padding-top:10px">
      <div style="font-size:9px;font-family:'JetBrains Mono',monospace;color:var(--dim);letter-spacing:1px;margin-bottom:3px">RESULT</div>
      <div style="font-size:11px;color:var(--gray)">Global unified prediction layer.<br>No local ceiling.</div>
    </div>
  </div>
</div>
<div style="margin-top:18px;border-top:1px solid var(--cb);padding-top:14px;max-width:920px">
  <div style="font-size:clamp(12px,1.3vw,14px);color:var(--gray)">"We don't buy users. <span style="color:#FFF;font-weight:700">The community creates them.</span>"</div>
</div>
</div>

<!-- 08 TEAM -->
<div class="slide" data-i="7">
<div class="label">08 — Team</div>
<h1>The <span class="a2">Founder.</span></h1>
<div style="display:flex;align-items:center;gap:24px;margin-top:32px;padding:28px 32px;background:var(--card);border:1px solid rgba(124,58,237,0.35);border-radius:16px;max-width:460px">
  <img src="/profile.png" alt="Deokjung Kim" style="width:72px;height:72px;border-radius:50%;border:2px solid rgba(124,58,237,0.45);object-fit:cover;object-position:top;flex-shrink:0;box-shadow:0 0 20px rgba(124,58,237,0.15)">
  <div>
    <div style="font-size:24px;font-weight:800;color:#FFF;line-height:1">Deokjung Kim</div>
    <div style="font-family:'JetBrains Mono',monospace;font-size:9px;color:var(--accent2);letter-spacing:2px;margin-top:7px">FOUNDER &amp; FULL-STACK DEVELOPER</div>
    <a href="https://www.linkedin.com/in/deokjung-kim-b80b012b8/" target="_blank" style="font-size:12px;color:var(--dim);text-decoration:none;margin-top:7px;display:inline-block">LinkedIn →</a>
  </div>
</div>
<div style="margin-top:36px;max-width:560px">
  <div style="font-size:clamp(14px,1.5vw,17px);color:var(--gray);line-height:1.6">"Executing the vision from <span style="color:#FFF;font-weight:700">core protocol</span> to <span style="color:var(--accent2);font-weight:700">user experience.</span>"</div>
</div>
</div>

<!-- 09 ASK -->
<div class="slide" data-i="8">
<div class="label">09 — Investment</div>
<h1>Seed Round.<br><span class="a2">$500K.</span></h1>
<div class="s09-outer" style="display:flex;gap:16px;margin-top:14px;max-width:880px;align-items:stretch">
  <!-- Left: Use of Funds -->
  <div style="flex:1;background:var(--card);border:1px solid rgba(124,58,237,0.4);border-radius:14px;padding:26px">
    <div style="font-family:'JetBrains Mono',monospace;font-size:9px;color:var(--accent2);letter-spacing:3px;margin-bottom:18px">USE OF FUNDS — 12-MONTH RUNWAY</div>
    <div style="display:flex;flex-direction:column;gap:13px">
      <div>
        <div style="display:flex;justify-content:space-between;align-items:baseline;margin-bottom:6px">
          <div style="font-size:13px;font-weight:700;color:#FFF">Marketing</div>
          <div style="font-family:'JetBrains Mono',monospace;font-size:12px;color:var(--accent2);font-weight:700">40%</div>
        </div>
        <div style="height:5px;background:var(--cb);border-radius:3px;overflow:hidden;margin-bottom:5px">
          <div style="width:40%;height:100%;background:linear-gradient(90deg,rgba(124,58,237,0.9),rgba(167,139,250,0.8));border-radius:3px"></div>
        </div>
        <div style="font-size:11px;color:var(--gray)">Creator acquisition, viral campaigns &amp; partnerships</div>
      </div>
      <div>
        <div style="display:flex;justify-content:space-between;align-items:baseline;margin-bottom:6px">
          <div style="font-size:13px;font-weight:700;color:#FFF">Operations</div>
          <div style="font-family:'JetBrains Mono',monospace;font-size:12px;color:var(--accent2);font-weight:700">30%</div>
        </div>
        <div style="height:5px;background:var(--cb);border-radius:3px;overflow:hidden;margin-bottom:5px">
          <div style="width:30%;height:100%;background:linear-gradient(90deg,rgba(124,58,237,0.75),rgba(167,139,250,0.65));border-radius:3px"></div>
        </div>
        <div style="font-size:11px;color:var(--gray)">Infrastructure, compliance &amp; global ops</div>
      </div>
      <div>
        <div style="display:flex;justify-content:space-between;align-items:baseline;margin-bottom:6px">
          <div style="font-size:13px;font-weight:700;color:#FFF">Market Liquidity</div>
          <div style="font-family:'JetBrains Mono',monospace;font-size:12px;color:var(--accent2);font-weight:700">20%</div>
        </div>
        <div style="height:5px;background:var(--cb);border-radius:3px;overflow:hidden;margin-bottom:5px">
          <div style="width:20%;height:100%;background:linear-gradient(90deg,rgba(124,58,237,0.55),rgba(167,139,250,0.45));border-radius:3px"></div>
        </div>
        <div style="font-size:11px;color:var(--gray)">Seeding liquidity for the first 1,000 niche markets</div>
      </div>
      <div>
        <div style="display:flex;justify-content:space-between;align-items:baseline;margin-bottom:6px">
          <div style="font-size:13px;font-weight:700;color:#FFF">Salary</div>
          <div style="font-family:'JetBrains Mono',monospace;font-size:12px;color:var(--accent2);font-weight:700">10%</div>
        </div>
        <div style="height:5px;background:var(--cb);border-radius:3px;overflow:hidden;margin-bottom:5px">
          <div style="width:10%;height:100%;background:linear-gradient(90deg,rgba(124,58,237,0.35),rgba(167,139,250,0.25));border-radius:3px"></div>
        </div>
        <div style="font-size:11px;color:var(--gray)">Lean founder runway — minimal burn, maximum output</div>
      </div>
    </div>
  </div>
  <!-- Right: 12-month milestones -->
  <div style="flex:1;background:var(--card);border:1px solid var(--cb);border-radius:14px;padding:26px">
    <div style="font-family:'JetBrains Mono',monospace;font-size:9px;color:var(--dim);letter-spacing:3px;margin-bottom:18px">12-MONTH MILESTONES</div>
    <div style="display:flex;flex-direction:column;gap:0">
      <div style="display:flex;align-items:center;gap:16px;padding:14px 0;border-bottom:1px solid var(--cb)">
        <div style="font-size:28px;font-weight:900;color:var(--accent2);line-height:1;min-width:88px">$5M+</div>
        <div>
          <div style="font-family:'JetBrains Mono',monospace;font-size:9px;color:var(--dim);letter-spacing:1px">MONTHLY TRADING VOLUME</div>
          <div style="font-size:11px;color:var(--gray);margin-top:3px">Proof of deep engagement and high-value niche markets</div>
        </div>
      </div>
      <div style="display:flex;align-items:center;gap:16px;padding:14px 0;border-bottom:1px solid var(--cb)">
        <div style="font-size:28px;font-weight:900;color:#FFF;line-height:1;min-width:88px">10K+</div>
        <div>
          <div style="font-family:'JetBrains Mono',monospace;font-size:9px;color:var(--dim);letter-spacing:1px">ACTIVE POWER USERS</div>
          <div style="font-size:11px;color:var(--gray);margin-top:3px">A loyal global community driving organic growth</div>
        </div>
      </div>
      <div style="display:flex;align-items:center;gap:16px;padding:14px 0">
        <div style="font-size:28px;font-weight:900;color:#FFF;line-height:1;min-width:88px">1,000+</div>
        <div>
          <div style="font-family:'JetBrains Mono',monospace;font-size:9px;color:var(--dim);letter-spacing:1px">UNIQUE NICHE MARKETS</div>
          <div style="font-size:11px;color:var(--gray);margin-top:3px">Dominating the long-tail territory where giants don't play</div>
        </div>
      </div>
    </div>
  </div>
</div>
<div class="s09-cta" style="margin-top:16px;border-top:1px solid var(--cb);padding-top:16px;max-width:880px;display:flex;justify-content:space-between;align-items:center;gap:20px">
  <div style="font-size:clamp(12px,1.3vw,14px);color:var(--gray)">Ready to unlock <span style="color:#FFF;font-weight:700">the 99% of human insight.</span></div>
  <div class="s09-contacts" style="flex-shrink:0;display:flex;align-items:center;gap:12px">
    <div style="text-align:right">
      <div style="font-size:13px;font-weight:700;color:#FFF">Deokjung Kim</div>
      <a href="mailto:deokjung95@gmail.com" style="font-size:13px;color:var(--light);text-decoration:none;font-weight:600">deokjung95@gmail.com</a>
    </div>
    <a href="https://t.me/deok1995" target="_blank" style="font-size:12px;color:var(--accent2);text-decoration:none;font-weight:600;border:1px solid rgba(167,139,250,0.35);padding:8px 14px;border-radius:8px;white-space:nowrap">Telegram →</a>
    <a href="https://www.linkedin.com/in/deokjung-kim-b80b012b8/" target="_blank" style="font-size:12px;color:var(--accent2);text-decoration:none;font-weight:600;border:1px solid rgba(167,139,250,0.35);padding:8px 14px;border-radius:8px;white-space:nowrap">LinkedIn →</a>
  </div>
</div>
</div>

<script>
(function(){
  const slides = document.querySelectorAll('.slide');
  const nav = document.getElementById('nav');
  const counter = document.getElementById('counter');
  const total = slides.length;
  let cur = 0;

  slides.forEach(function(_, i){
    const d = document.createElement('div');
    d.className = 'dot' + (i === 0 ? ' active' : '');
    d.addEventListener('click', function(){ go(i); });
    nav.appendChild(d);
  });

  function go(idx){
    if(idx < 0 || idx >= total) return;
    slides[cur].classList.remove('active');
    nav.children[cur].classList.remove('active');
    slides[cur].scrollTop = 0;
    cur = idx;
    slides[cur].classList.add('active');
    nav.children[cur].classList.add('active');
    counter.textContent = (cur + 1) + ' / ' + total;
  }

  document.addEventListener('keydown', function(e){
    if(e.key === 'ArrowDown' || e.key === 'ArrowRight') go(cur + 1);
    if(e.key === 'ArrowUp'   || e.key === 'ArrowLeft')  go(cur - 1);
  });

  var lastScroll = 0;
  document.addEventListener('wheel', function(e){
    if(Math.abs(e.deltaY) < 30) return;
    var now = Date.now();
    if(now - lastScroll < 1000) return;
    lastScroll = now;
    go(e.deltaY > 0 ? cur + 1 : cur - 1);
  }, { passive: true });

  var touchY = 0;
  document.addEventListener('touchstart', function(e){ touchY = e.touches[0].clientY; }, { passive: true });
  document.addEventListener('touchend', function(e){
    var diff = touchY - e.changedTouches[0].clientY;
    if(Math.abs(diff) < 40) return;
    var s = slides[cur];
    if(s.scrollHeight > s.clientHeight){
      if(diff > 0 && s.scrollTop + s.clientHeight < s.scrollHeight - 5) return;
      if(diff < 0 && s.scrollTop > 5) return;
    }
    go(diff > 0 ? cur + 1 : cur - 1);
  });
})();
</script>
</body>
</html>`
