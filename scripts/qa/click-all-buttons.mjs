import { chromium } from '@playwright/test';

const BASE_URL = process.env.QA_BASE_URL || 'http://127.0.0.1:3000';
const ROUTES = ['/', '/vote', '/explore', '/mypage', '/submit'];
const WAIT_MS = 300;
const MAX_ROUTE_PASSES = 3;

function textNorm(v) {
  return (v || '').replace(/\s+/g, ' ').trim();
}

function labelOf(text, aria, i) {
  const t = textNorm(text);
  const a = textNorm(aria);
  return (t || a || `(icon-only #${i})`).slice(0, 100);
}

async function closeTransientOverlays(page) {
  const closeCandidates = [
    'button:has-text("Cancel")',
    'button:has-text("Close")',
    'button:has-text("취소")',
    'button[aria-label*="close" i]',
  ];
  for (const sel of closeCandidates) {
    const btn = page.locator(sel).first();
    if (await btn.isVisible().catch(() => false)) {
      await btn.click({ timeout: 800 }).catch(() => {});
      await page.waitForTimeout(120);
    }
  }
}

async function ensureRoute(page, route) {
  const current = page.url();
  const expectedPrefix = `${BASE_URL}${route}`;
  if (!current.startsWith(expectedPrefix)) {
    await page.goto(expectedPrefix, { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(WAIT_MS);
  }
}

async function collectButtons(page) {
  return page.evaluate(() => {
    const nodes = Array.from(document.querySelectorAll('button'));
    const visible = (el) => {
      const style = window.getComputedStyle(el);
      const rect = el.getBoundingClientRect();
      return style.display !== 'none' && style.visibility !== 'hidden' && rect.width > 0 && rect.height > 0;
    };
    return nodes
      .map((el, idx) => ({
        idx,
        text: (el.textContent || '').replace(/\s+/g, ' ').trim(),
        aria: (el.getAttribute('aria-label') || '').trim(),
        disabled: el.hasAttribute('disabled') || el.getAttribute('aria-disabled') === 'true',
        visible: visible(el),
      }))
      .filter((b) => b.visible);
  });
}

async function runRoute(page, route) {
  const res = { route, totalVisible: 0, clicked: 0, skipped: 0, failed: [] };
  const clickedKeys = new Set();

  await ensureRoute(page, route);
  await page.waitForTimeout(700);

  for (let pass = 0; pass < MAX_ROUTE_PASSES; pass += 1) {
    const buttons = await collectButtons(page);
    res.totalVisible = Math.max(res.totalVisible, buttons.length);
    let progressed = false;

    for (let i = 0; i < buttons.length; i += 1) {
      const b = buttons[i];
      const key = `${b.text}|${b.aria}|${i}`;
      const label = labelOf(b.text, b.aria, i);
      if (clickedKeys.has(key)) continue;
      clickedKeys.add(key);

      if (b.disabled) {
        res.skipped += 1;
        continue;
      }

      const locator = page.locator('button:visible').nth(i);

      try {
        await locator.scrollIntoViewIfNeeded().catch(() => {});
        await locator.click({ timeout: 2200 }).catch(async () => {
          await locator.click({ timeout: 2200, force: true });
        });

        res.clicked += 1;
        progressed = true;
        await page.waitForTimeout(WAIT_MS);
        await closeTransientOverlays(page);
        await ensureRoute(page, route);
      } catch (e) {
        res.failed.push({ index: i, label, error: String(e?.message || e).slice(0, 160) });
        await ensureRoute(page, route);
      }
    }

    if (!progressed) break;
  }

  return res;
}

async function main() {
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext();
  const page = await context.newPage();

  const routes = [];
  for (const route of ROUTES) {
    routes.push(await runRoute(page, route));
  }

  await browser.close();

  const totals = routes.reduce(
    (acc, r) => {
      acc.totalVisible += r.totalVisible;
      acc.clicked += r.clicked;
      acc.skipped += r.skipped;
      acc.failed += r.failed.length;
      return acc;
    },
    { totalVisible: 0, clicked: 0, skipped: 0, failed: 0 },
  );

  console.log(JSON.stringify({ baseUrl: BASE_URL, routes, totals }, null, 2));
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
