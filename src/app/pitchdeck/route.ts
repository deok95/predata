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
.slide{position:absolute;top:0;left:0;right:0;bottom:0;display:flex;flex-direction:column;justify-content:center;padding:0 10vw;opacity:0;transform:translateY(40px);transition:opacity .6s,transform .6s;pointer-events:none}
.slide.active{opacity:1;transform:translateY(0);pointer-events:all}
.label{font-family:'JetBrains Mono',monospace;font-size:12px;font-weight:600;letter-spacing:3px;text-transform:uppercase;color:var(--accent2);margin-bottom:20px}
h1{font-weight:800;font-size:clamp(32px,5vw,62px);line-height:1.1;margin-bottom:20px;max-width:850px}
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
.sol-grid{display:grid;grid-template-columns:1fr 1fr;gap:40px;margin-top:32px;align-items:center}
.sol-steps{display:flex;flex-direction:column;gap:16px}
.sol-step{display:flex;align-items:flex-start;gap:12px}
.sn{font-family:'JetBrains Mono',monospace;font-weight:700;font-size:18px;color:var(--accent);flex-shrink:0;width:28px}
.st{font-size:14px;color:var(--light);line-height:1.5}
.mock-mobile{width:240px;height:440px;background:var(--card);border:2px solid var(--cb);border-radius:24px;display:flex;align-items:center;justify-content:center;justify-self:center;box-shadow:0 0 60px rgba(124,58,237,.08);flex-direction:column;gap:8px;overflow:hidden}
.mock-mobile-inner{display:flex;flex-direction:column;align-items:center;gap:12px;padding:24px}
.mock-bar{width:80px;height:5px;background:var(--cb);border-radius:3px}
.mock-row{width:100%;height:32px;background:var(--cb);border-radius:8px}
.mock-row.accent{background:rgba(124,58,237,.2);border:1px solid rgba(124,58,237,.3)}
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
.hint{position:fixed;bottom:28px;left:50%;transform:translateX(-50%);font-family:'JetBrains Mono',monospace;font-size:11px;color:var(--dim);z-index:100;opacity:.4}
@media(max-width:768px){.slide{padding:0 6vw}.c2,.c3{grid-template-columns:1fr}.sol-grid{grid-template-columns:1fr}.prod-grid{grid-template-columns:1fr}.nav{display:none}.team-card{flex-direction:column;text-align:center}.rm-grid{grid-template-columns:1fr}.flywheel{flex-direction:column}.rev-flow{flex-direction:column}}
</style>
</head>
<body>
<div class="topline"></div>
<div class="nav" id="nav"></div>
<div class="counter" id="counter">1 / 9</div>
<div class="hint">↑ ↓ or scroll to navigate</div>

<!-- 01 TITLE -->
<div class="slide cover active" data-i="0">
<div style="display:flex;align-items:center;gap:14px;margin-bottom:28px">
<svg viewBox="0 0 40 40" width="48" height="48" fill="none"><rect width="40" height="40" rx="12" fill="#7C3AED" style="filter:drop-shadow(0 4px 6px rgba(124,58,237,0.3))"/><path d="M12 10V30H20C25.5228 30 30 25.5228 30 20C30 14.4772 25.5228 10 20 10H12Z" stroke="white" stroke-width="2.5" stroke-linejoin="round"/><circle cx="20" cy="20" r="3" fill="white"/><path d="M12 20H17" stroke="white" stroke-width="2" stroke-linecap="round"/></svg>
<div style="font-size:20px;font-weight:700;letter-spacing:0.5px;line-height:1">PRE<span style="color:#A78BFA">(D)</span>ATA</div>
</div>
<h1>PRE<span style="color:#A78BFA">(D)</span>ATA</h1>
<p class="tagline">If the market doesn't exist, <span class="a2">create it.</span></p>
<div class="cover-meta">2026 &nbsp;·&nbsp; Confidential</div>
</div>

<!-- 02 PROBLEM -->
<div class="slide" data-i="1">
<div class="label">02 — Problem</div>
<h1>Prediction markets hit <span class="a2">$44B.</span><br>But users <span class="r">can't create their own.</span></h1>
<div class="cards c2" style="max-width:720px">
<div class="card"><h3>Supply is centralized</h3><p>Markets are created by a small group of curators. Long-tail topics — sports subcategories, K-pop, local issues — aren't covered.</p></div>
<div class="card"><h3>No reason to return</h3><p>Users consume and leave. There's no built-in loop that brings them back after a trade settles.</p></div>
<div class="card"><h3>Platform bottleneck</h3><p>Growth is gated by how fast the ops team can create markets. Demand outpaces supply.</p></div>
<div class="card"><h3>Weak signal</h3><p>Low-stakes participation produces noisy data. No skin in the game, no meaningful signal.</p></div>
</div>
</div>

<!-- 03 SOLUTION -->
<div class="slide" data-i="2">
<div class="label">03 — Solution</div>
<h1>Users create the market.<br>The crowd decides <span class="a2">what trades.</span></h1>
<div class="flow">
<div class="flow-s">Vote</div><div class="flow-a">→</div>
<div class="flow-s">Top 3 Selected</div><div class="flow-a">→</div>
<div class="flow-s">Betting Opens</div><div class="flow-a">→</div>
<div class="flow-s on">Settlement</div>
</div>
<div class="callout">80% of trading fees go back to the Creator + Voter Pool.<br>Create, vote, or bet — you earn either way.</div>
</div>

<!-- 04 PRODUCT -->
<div class="slide" data-i="3">
<div class="label">04 — Product</div>
<h1>Vote, trade, settle, compete.<br><span class="a2">One app.</span></h1>
<div class="sol-grid">
<div class="sol-steps">
<div class="sol-step"><div class="sn">01</div><div class="st">Browse trending questions and vote on what matters to you.</div></div>
<div class="sol-step"><div class="sn">02</div><div class="st">Top questions become live markets — place your bet.</div></div>
<div class="sol-step"><div class="sn">03</div><div class="st">Results come in. Payouts settle automatically.</div></div>
<div class="sol-step"><div class="sn">04</div><div class="st">Track your record on the leaderboard. Climb the ranks.</div></div>
</div>
<div class="mock-mobile">
<div class="mock-mobile-inner" style="width:100%">
<div class="mock-bar"></div>
<div class="mock-row accent"></div>
<div class="mock-row"></div>
<div class="mock-row accent"></div>
<div class="mock-row"></div>
<div class="mock-row"></div>
<div class="mock-row accent"></div>
<div class="mock-row"></div>
</div>
</div>
</div>
</div>

<!-- 05 RETENTION -->
<div class="slide" data-i="4">
<div class="label">05 — Why Users Stay</div>
<h1>Retention is <span class="a2">built into the loop.</span></h1>
<div class="flywheel">
<div class="fw-step"><h4>Vote</h4><p>Signal your opinion</p></div>
<div class="fw-arrow">→</div>
<div class="fw-step"><h4>Top 3</h4><p>Crowd picks what trades</p></div>
<div class="fw-arrow">→</div>
<div class="fw-step"><h4>Bet</h4><p>Put skin in the game</p></div>
<div class="fw-arrow">→</div>
<div class="fw-step"><h4>Settle</h4><p>Earn rewards</p></div>
</div>
<div class="fw-loop">↑ &nbsp; every settlement triggers the next question &nbsp; ↑</div>
<div class="cards c3" style="margin-top:32px;max-width:720px">
<div class="card"><h3>Immediate reward</h3><p>Vote, trade, and settle in one continuous feedback loop.</p></div>
<div class="card"><h3>Social motivation</h3><p>Leaderboard, follow, and activity feed keep users competitive.</p></div>
<div class="card"><h3>User-generated supply</h3><p>Anyone can submit questions — the community grows the content.</p></div>
</div>
</div>

<!-- 06 BUSINESS MODEL -->
<div class="slide" data-i="5">
<div class="label">06 — Business Model</div>
<h1>Everyone earns.<br><span class="a2">Every trade.</span></h1>
<div class="rev-flow">
<div class="rev-box"><div class="pct">Fee</div><div class="rlbl">per trade</div></div>
<div class="rev-arrow">→</div>
<div class="rev-box"><div class="pct a2">40%</div><div class="rlbl">Creator Pool</div></div>
<div class="rev-arrow">+</div>
<div class="rev-box"><div class="pct a2">40%</div><div class="rlbl">Voter Pool</div></div>
<div class="rev-arrow">+</div>
<div class="rev-box"><div class="pct a">20%</div><div class="rlbl">Platform</div></div>
</div>
<p class="sub" style="margin-top:32px">Growth lever: users × trade frequency × avg trade size.<br>Long-term: proprietary prediction data as a B2B product.</p>
</div>

<!-- 07 GO-TO-MARKET -->
<div class="slide" data-i="6">
<div class="label">07 — Go-To-Market</div>
<h1>Start where signal<br><span class="a2">is clearest.</span></h1>
<div class="rm-grid">
<div class="rm-card">
<h3>Now</h3>
<h4>Sports & Crypto</h4>
<p>Objective outcomes. Daily events. High re-engagement frequency. Validate the full loop here.</p>
</div>
<div class="rm-card">
<h3>Next</h3>
<h4>Issues & Culture</h4>
<p>Expand to trending topics, entertainment, and news. User-generated supply scales automatically.</p>
</div>
<div class="rm-card">
<h3>Scale</h3>
<h4>Creator Program</h4>
<p>Onboard influencers as market creators. Drive viral growth. Begin global expansion.</p>
</div>
</div>
<p class="sub" style="margin-top:28px">High-frequency categories first — then expand to adjacent long-tail topics.</p>
</div>

<!-- 08 TEAM -->
<div class="slide" data-i="7">
<div class="label">08 — Team</div>
<h1>Why <span class="a2">us.</span></h1>
<div class="team-card">
<div class="avatar">Photo</div>
<div class="tinfo">
<h3>[Founder Name]</h3>
<div class="role">CEO · Product</div>
<a href="#">[Previous Company / Role]</a>
</div>
</div>
<div class="team-card">
<div class="avatar">Photo</div>
<div class="tinfo">
<h3>[Founder Name]</h3>
<div class="role">CTO · Engineering</div>
<a href="#">[Previous Company / Role]</a>
</div>
</div>
</div>

<!-- 09 ASK -->
<div class="slide" data-i="8">
<div class="label">09 — Ask</div>
<h1>Seed round.<br><span class="a2">$[Amount].</span></h1>
<p class="sub">12-month targets: [DAU] DAU &nbsp;·&nbsp; [D30]% D30 retention &nbsp;·&nbsp; $[GMV] monthly GMV</p>
<div class="contact">
<p>Ready to build the prediction market for the next billion users.</p>
<a href="mailto:[email]">[email address]</a>
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
    var now = Date.now();
    if(now - lastScroll < 700) return;
    lastScroll = now;
    go(e.deltaY > 0 ? cur + 1 : cur - 1);
  }, { passive: true });

  var touchY = 0;
  document.addEventListener('touchstart', function(e){ touchY = e.touches[0].clientY; }, { passive: true });
  document.addEventListener('touchend', function(e){
    var diff = touchY - e.changedTouches[0].clientY;
    if(Math.abs(diff) > 40) go(diff > 0 ? cur + 1 : cur - 1);
  });
})();
</script>
</body>
</html>`
