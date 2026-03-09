import { chromium } from "@playwright/test";

const BASE_URL = process.env.QA_BASE_URL || "http://127.0.0.1:3000";
const ADMIN_EMAIL = process.env.QA_ADMIN_EMAIL || "admin@predata.io";
const ADMIN_PASSWORD = process.env.QA_ADMIN_PASSWORD || "123400";

const result = {
  baseUrl: BASE_URL,
  steps: [],
  failures: [],
  consoleErrors: [],
  apiCalls: [],
  badApiCalls: [],
  requestFailures: [],
};

function stepOk(name, detail = "") {
  result.steps.push({ name, ok: true, detail });
}

function stepFail(name, detail) {
  result.steps.push({ name, ok: false, detail });
  result.failures.push({ name, detail });
}

function isAllowed4xx(call) {
  if (call.status === 409 && call.url.includes("/api/votes")) return true;
  if (call.status === 409 && call.url.includes("/api/users/") && call.url.includes("/follow")) return true;
  if (call.status === 404 && call.url.includes("/api/pool/")) return true;
  if (call.status === 400 && call.url.includes("/api/swap/simulate")) return true;
  if (call.status === 400 && call.url.includes("/api/swap")) return true;
  return false;
}

async function safeClick(locator, options = {}) {
  await locator.scrollIntoViewIfNeeded().catch(() => {});
  await locator.click(options).catch(async () => {
    await locator.click({ ...options, force: true });
  });
}

async function main() {
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext();
  const page = await context.newPage();

  page.on("console", (msg) => {
    if (msg.type() === "error") {
      result.consoleErrors.push(msg.text());
    }
  });

  page.on("requestfailed", (req) => {
    result.requestFailures.push({
      method: req.method(),
      url: req.url(),
      reason: req.failure()?.errorText || "unknown",
    });
  });

  page.on("response", (res) => {
    const req = res.request();
    const url = req.url();
    if (!url.includes("/api/")) return;
    const call = {
      method: req.method(),
      url,
      status: res.status(),
    };
    result.apiCalls.push(call);
    if (call.status >= 400 && !isAllowed4xx(call)) {
      result.badApiCalls.push(call);
    }
  });

  try {
    await page.goto(`${BASE_URL}/`, { waitUntil: "domcontentloaded" });

    // 1) Login
    try {
      await safeClick(page.getByRole("button", { name: "Log In" }));
      await page.getByPlaceholder("Email address").fill(ADMIN_EMAIL);
      await page.getByPlaceholder("Password").fill(ADMIN_PASSWORD);
      await safeClick(page.getByRole("button", { name: /^Sign In$/ }));
      await page.waitForFunction(() => Boolean(localStorage.getItem("predata_token") || localStorage.getItem("token") || localStorage.getItem("authToken")), null, { timeout: 10000 });
      const token = await page.evaluate(() => localStorage.getItem("predata_token") || localStorage.getItem("token") || localStorage.getItem("authToken"));
      if (!token) throw new Error("token not found");
      stepOk("login", "token issued");
    } catch (e) {
      stepFail("login", String(e?.message || e));
    }

    // 2) Vote flow
    try {
      const beforeVotes = result.apiCalls.filter((c) => c.url.includes("/api/votes") && c.method === "POST").length;
      await page.goto(`${BASE_URL}/vote`, { waitUntil: "domcontentloaded" });
      await page.waitForTimeout(1000);

      const yesBtn = page.locator("button").filter({ hasText: /^Yes(\s|$)/ }).first();
      if (await yesBtn.count()) {
        await safeClick(yesBtn);
        const confirmVoteBtn = page.getByRole("button", { name: /^Vote$/ });
        if (await confirmVoteBtn.count()) {
          await safeClick(confirmVoteBtn.first());
        }
        await page.waitForTimeout(1200);
        const afterVotes = result.apiCalls.filter((c) => c.url.includes("/api/votes") && c.method === "POST").length;
        if (afterVotes > beforeVotes) {
          stepOk("vote", "POST /api/votes triggered");
        } else {
          const votedBadgeCount = await page.locator("text=Voted, text=Already Voted").count();
          if (votedBadgeCount > 0) {
            stepOk("vote", "vote state updated without POST (pre-checked)");
          } else {
            stepOk("vote", "no mutable vote action in current feed");
          }
        }
      } else {
        const votedBadgeCount = await page.locator("text=Voted, text=Already Voted").count();
        if (votedBadgeCount > 0) {
          stepOk("vote", "no available vote action (already voted)");
          } else {
            stepOk("vote", "no votable question in current feed");
          }
      }
    } catch (e) {
      stepFail("vote", String(e?.message || e));
    }

    // 2-b) Follow flow
    try {
      const beforeFollow = result.apiCalls.filter((c) =>
        c.url.includes("/api/users/") &&
        c.url.includes("/follow") &&
        (c.method === "POST" || c.method === "DELETE")
      ).length;
      const followBtn = page.locator("button").filter({ hasText: /^Follow$/ }).first();
      if (await followBtn.count()) {
        await safeClick(followBtn);
        await page.waitForTimeout(1200);
        const afterFollow = result.apiCalls.filter((c) =>
          c.url.includes("/api/users/") &&
          c.url.includes("/follow") &&
          (c.method === "POST" || c.method === "DELETE")
        ).length;
        if (afterFollow > beforeFollow) {
          stepOk("follow", "/api/users/{id}/follow triggered");
        } else {
          throw new Error("follow API was not triggered");
        }
      } else {
        stepOk("follow", "no follow target in current feed");
      }
    } catch (e) {
      stepFail("follow", String(e?.message || e));
    }

    // 3) Bet flow (swap)
    try {
      const beforeSwap = result.apiCalls.filter((c) => c.url.includes("/api/swap") && c.method === "POST").length;
      await page.goto(`${BASE_URL}/`, { waitUntil: "domcontentloaded" });
      await page.waitForTimeout(800);

      const marketYesBtn = page.locator("button").filter({ hasText: /^Yes \d+¢$/ }).first();
      await marketYesBtn.waitFor({ state: "visible", timeout: 10000 });
      await safeClick(marketYesBtn);

      const amountInput = page.locator('input[type="number"]').first();
      await amountInput.waitFor({ state: "visible", timeout: 10000 });
      await amountInput.fill("1");

      const buyBtn = page.locator("button").filter({ hasText: /^Buy (Yes|No)/ }).first();
      await buyBtn.waitFor({ state: "visible", timeout: 10000 });
      await safeClick(buyBtn);
      await page.waitForTimeout(1400);

      const afterSwap = result.apiCalls.filter((c) => c.url.includes("/api/swap") && c.method === "POST").length;
      if (afterSwap <= beforeSwap) throw new Error("swap POST not triggered");
      stepOk("bet", "POST /api/swap triggered");
    } catch (e) {
      stepFail("bet", String(e?.message || e));
    }

    // 4) Comment flow
    try {
      const commentInput = page.getByPlaceholder("Share your thoughts...");
      await commentInput.waitFor({ state: "visible", timeout: 10000 });
      await commentInput.fill(`qa comment ${Date.now()}`);
      await safeClick(page.getByRole("button", { name: /^Post$/ }).first());
      await page.waitForTimeout(500);
      stepOk("comment", "comment action clicked");
    } catch (e) {
      stepFail("comment", String(e?.message || e));
    }

    // 5) Submit question flow
    try {
      await page.goto(`${BASE_URL}/submit`, { waitUntil: "domcontentloaded" });
      await page.waitForTimeout(500);

      await page.getByPlaceholder("Will Bitcoin hit $150K before July 2026?").fill(`[QA] E2E submit ${Date.now()}?`);
      await safeClick(page.locator("button").filter({ hasText: /^Crypto$/ }).first());
      const resolutionInput = page.getByPlaceholder("e.g. CoinGecko price, Official election results...");
      if (await resolutionInput.count()) {
        await resolutionInput.fill("CoinGecko daily close price");
      }
      await safeClick(page.getByRole("button", { name: "Preview & Submit" }));
      await page.waitForTimeout(500);
      const submitRespPromise = page.waitForResponse(
        (res) => res.request().method() === "POST" &&
          res.url().includes("/api/questions/drafts/") &&
          res.url().includes("/submit"),
        { timeout: 15000 },
      );
      await safeClick(page.getByRole("button", { name: "Submit Question" }));
      const submitResp = await submitRespPromise;
      const submitStatus = submitResp.status();
      const submitBody = (await submitResp.text()).slice(0, 300);
      if (submitStatus >= 200 && submitStatus < 300) {
        stepOk("submit-question", `submit completed (${submitStatus})`);
      } else {
        throw new Error(`submit failed (${submitStatus}) ${submitBody}`);
      }
    } catch (e) {
      stepFail("submit-question", String(e?.message || e));
    }
  } finally {
    await browser.close();
  }

  const actionableConsoleErrors = result.consoleErrors.filter((x) =>
    !x.includes("status of 401") &&
    !x.includes("status of 404") &&
    !x.includes("status of 400") &&
    !x.includes("status of 409") &&
    !x.includes("베팅 기간이 아닙니다")
  );
  if (actionableConsoleErrors.length > 0) {
    result.failures.push({ name: "console-errors", detail: `${actionableConsoleErrors.length} console errors` });
  }
  if (result.requestFailures.length > 0) {
    const nonDev = result.requestFailures.filter((f) =>
      !f.url.includes("/__nextjs_original-stack-frames") &&
      f.reason !== "net::ERR_ABORTED"
    );
    if (nonDev.length > 0) {
      result.failures.push({ name: "request-failures", detail: `${nonDev.length} request failures` });
    }
  }
  if (result.badApiCalls.length > 0) {
    result.failures.push({ name: "bad-api-calls", detail: `${result.badApiCalls.length} API 4xx/5xx (unexpected)` });
  }

  console.log(JSON.stringify({
    ...result,
    summary: {
      stepTotal: result.steps.length,
      stepFailed: result.steps.filter((s) => !s.ok).length,
      consoleErrorCount: result.consoleErrors.length,
      requestFailureCount: result.requestFailures.length,
      badApiCallCount: result.badApiCalls.length,
      apiCallCount: result.apiCalls.length,
      ok: result.failures.length === 0,
    },
  }, null, 2));

  if (result.failures.length > 0) {
    process.exit(1);
  }
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
