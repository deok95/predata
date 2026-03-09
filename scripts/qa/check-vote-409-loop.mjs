import { chromium } from '@playwright/test';

const BASE = 'http://127.0.0.1:3000';

async function main() {
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext();
  const page = await context.newPage();

  const calls = [];
  page.on('response', async (res) => {
    const url = res.url();
    if (url.includes('/api/votes')) {
      const req = res.request();
      calls.push({ method: req.method(), url, status: res.status() });
    }
  });

  await page.goto(`${BASE}/vote`, { waitUntil: 'domcontentloaded' });
  await page.waitForTimeout(1200);

  const firstYes = page.locator('button:has-text("Yes")').first();
  if (await firstYes.isVisible().catch(() => false)) {
    await firstYes.click({ timeout: 2000 }).catch(() => {});
    await page.waitForTimeout(1200);
    const confirm = page.locator('button:has-text("Vote")').first();
    if (await confirm.isVisible().catch(() => false)) {
      await confirm.click({ timeout: 2000 }).catch(() => {});
    }
  }

  await page.waitForTimeout(1500);
  await browser.close();

  const postVotes = calls.filter((c) => c.method === 'POST' && c.url.includes('/api/votes'));
  const status409 = postVotes.filter((c) => c.status === 409);

  console.log(JSON.stringify({ totalVoteCalls: calls.length, postVotes: postVotes.length, post409: status409.length, calls }, null, 2));
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
