import { chromium } from '@playwright/test';

const BASE_URL = process.env.QA_BASE_URL || 'http://127.0.0.1:3000';
const ROUTES = ['/', '/vote', '/explore', '/mypage', '/submit'];
const MAX_PER_ROUTE = 80;
const MAX_MS = 120000;

const startedAt = Date.now();
const timedOut = () => Date.now() - startedAt > MAX_MS;

function norm(v) {
  return (v || '').replace(/\s+/g, ' ').trim();
}

function keyFor(btn) {
  return `${norm(btn.text)}|${norm(btn.aria)}|${btn.index}`;
}

async function listButtons(page) {
  return page.evaluate(() => {
    const nodes = Array.from(document.querySelectorAll('button'));
    const vis = (el) => {
      const st = getComputedStyle(el);
      const r = el.getBoundingClientRect();
      return st.display !== 'none' && st.visibility !== 'hidden' && r.width > 0 && r.height > 0;
    };
    return nodes
      .map((el, i) => ({
        index: i,
        text: (el.textContent || '').trim(),
        aria: el.getAttribute('aria-label') || '',
        disabled: el.hasAttribute('disabled') || el.getAttribute('aria-disabled') === 'true',
        visible: vis(el),
      }))
      .filter((x) => x.visible);
  });
}

async function runRoute(page, route) {
  const result = { route, visible: 0, clicked: 0, skipped: 0, failed: [] };
  const seen = new Set();

  await page.goto(`${BASE_URL}${route}`, { waitUntil: 'domcontentloaded' });
  await page.waitForTimeout(500);

  while (!timedOut() && result.clicked + result.skipped + result.failed.length < MAX_PER_ROUTE) {
    const buttons = await listButtons(page);
    result.visible = Math.max(result.visible, buttons.length);

    let target = null;
    for (const b of buttons) {
      const k = keyFor(b);
      if (!seen.has(k)) {
        target = b;
        seen.add(k);
        break;
      }
    }
    if (!target) break;

    const label = norm(target.text) || norm(target.aria) || `(icon-only #${target.index})`;
    if (target.disabled) {
      result.skipped += 1;
      continue;
    }

    try {
      const loc = page.locator('button').nth(target.index);
      await loc.scrollIntoViewIfNeeded().catch(() => {});
      await loc.click({ timeout: 1200 }).catch(async () => {
        await loc.click({ timeout: 1200, force: true });
      });
      result.clicked += 1;
      await page.waitForTimeout(180);

      const cur = page.url();
      if (!cur.startsWith(BASE_URL) || !cur.startsWith(`${BASE_URL}${route}`)) {
        await page.goto(`${BASE_URL}${route}`, { waitUntil: 'domcontentloaded' });
        await page.waitForTimeout(250);
      }
    } catch (e) {
      result.failed.push({ label: label.slice(0, 80), error: String(e?.message || e).slice(0, 120) });
      await page.goto(`${BASE_URL}${route}`, { waitUntil: 'domcontentloaded' });
      await page.waitForTimeout(200);
    }
  }

  return result;
}

async function main() {
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext();
  const page = await context.newPage();
  const routes = [];

  for (const route of ROUTES) {
    if (timedOut()) break;
    routes.push(await runRoute(page, route));
  }

  await browser.close();

  const totals = routes.reduce((a, r) => ({
    visible: a.visible + r.visible,
    clicked: a.clicked + r.clicked,
    skipped: a.skipped + r.skipped,
    failed: a.failed + r.failed.length,
  }), { visible: 0, clicked: 0, skipped: 0, failed: 0 });

  console.log(JSON.stringify({ baseUrl: BASE_URL, timedOut: timedOut(), routes, totals }, null, 2));
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
