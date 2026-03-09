import { chromium } from '@playwright/test';

const BASE = 'http://127.0.0.1:3000';
const ROUTES = ['/', '/vote', '/explore', '/mypage', '/submit'];

async function main() {
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext();
  const page = await context.newPage();

  const errors = [];
  page.on('console', (msg) => {
    if (msg.type() === 'error') {
      errors.push({ text: msg.text().slice(0, 300), url: page.url() });
    }
  });

  for (const r of ROUTES) {
    await page.goto(`${BASE}${r}`, { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(1200);
  }

  await browser.close();
  console.log(JSON.stringify({ routeCount: ROUTES.length, errorCount: errors.length, errors: errors.slice(0, 30) }, null, 2));
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
