import { chromium } from '@playwright/test';
import fs from 'node:fs';

const BASE = 'http://127.0.0.1:3000';
const LOGIN_JSON = '/tmp/qa_login.json';
let token = '';
let memberId = '';

if (fs.existsSync(LOGIN_JSON)) {
  const raw = fs.readFileSync(LOGIN_JSON, 'utf8');
  token = (raw.match(/"token":"([^"]+)"/) || [])[1] || '';
  memberId = String((raw.match(/"memberId":(\d+)/) || [])[1] || '');
}

async function main() {
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext();
  if (token) {
    await context.addInitScript(([t, m]) => {
      localStorage.setItem('token', t);
      localStorage.setItem('authToken', t);
      if (m) localStorage.setItem('memberId', m);
    }, [token, memberId]);
  }
  const page = await context.newPage();

  const calls = [];
  page.on('response', (res) => {
    const u = res.url();
    if (u.includes('127.0.0.1:8080/api/')) {
      calls.push({ method: res.request().method(), url: u, status: res.status() });
    }
  });

  // vote page action
  await page.goto(`${BASE}/vote`, { waitUntil: 'domcontentloaded' });
  await page.waitForTimeout(1400);
  const yesBtn = page.locator('button:has-text("Yes")').first();
  if (await yesBtn.isVisible().catch(() => false)) {
    await yesBtn.click().catch(() => {});
    await page.waitForTimeout(700);
    const confirmVote = page.locator('button:has-text("Vote")').first();
    if (await confirmVote.isVisible().catch(() => false)) {
      await confirmVote.click().catch(() => {});
      await page.waitForTimeout(800);
    }
  }

  // open first market detail from home and try buy
  await page.goto(`${BASE}/`, { waitUntil: 'domcontentloaded' });
  await page.waitForTimeout(1200);
  const firstMarket = page.locator('div').filter({ hasText: 'Yes' }).first();
  await page.locator('button:has-text("Yes")').first().click().catch(() => {});
  await page.waitForTimeout(1200);
  const buyBtn = page.locator('button').filter({ hasText: /Buy Yes|Buy No/ }).first();
  if (await buyBtn.isVisible().catch(() => false)) {
    await buyBtn.click().catch(() => {});
    await page.waitForTimeout(900);
  }

  await browser.close();

  const postVotes = calls.filter((c) => c.method === 'POST' && c.url.includes('/api/votes'));
  const postSwaps = calls.filter((c) => c.method === 'POST' && c.url.includes('/api/swap'));
  const bad = calls.filter((c) => c.status >= 400);

  console.log(JSON.stringify({
    totalApiCalls: calls.length,
    postVotes: postVotes.length,
    postVotesStatuses: postVotes.map((c) => c.status),
    postSwaps: postSwaps.length,
    postSwapStatuses: postSwaps.map((c) => c.status),
    badCount: bad.length,
    bad: bad.slice(0, 20),
  }, null, 2));
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
