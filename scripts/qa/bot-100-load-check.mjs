#!/usr/bin/env node

const API_BASE = process.env.QA_API_URL || "http://127.0.0.1:8080";
const ADMIN_EMAIL = process.env.QA_ADMIN_EMAIL || "admin@predata.io";
const ADMIN_PASSWORD = process.env.QA_ADMIN_PASSWORD || "123400";
const VOTE_ROUNDS = Number(process.env.QA_BOT_VOTE_ROUNDS || 3);
const TRADE_ROUNDS = Number(process.env.QA_BOT_TRADE_ROUNDS || 3);

function parseEnvelope(json) {
  if (json && typeof json === "object" && "data" in json) return json.data;
  return json;
}

async function api(path, method = "GET", body, token) {
  const res = await fetch(`${API_BASE}${path}`, {
    method,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: body ? JSON.stringify(body) : undefined,
  });
  const text = await res.text();
  let json = null;
  try {
    json = text ? JSON.parse(text) : null;
  } catch {
    json = { raw: text };
  }
  if (!res.ok) {
    throw new Error(`${method} ${path} failed (${res.status}): ${JSON.stringify(json).slice(0, 300)}`);
  }
  return parseEnvelope(json);
}

async function loginAdmin() {
  const data = await api("/api/auth/login", "POST", {
    email: ADMIN_EMAIL,
    password: ADMIN_PASSWORD,
  });
  return data?.token || null;
}

async function main() {
  const startedAt = new Date().toISOString();
  const report = {
    startedAt,
    apiBase: API_BASE,
    targetBots: 100,
    voteRounds: VOTE_ROUNDS,
    tradeRounds: TRADE_ROUNDS,
    steps: [],
  };

  let token = null;
  try {
    token = await loginAdmin();
    report.steps.push({ step: "admin-login", ok: true });
  } catch (e) {
    report.steps.push({ step: "admin-login", ok: false, error: String(e.message || e) });
  }

  const init = await api("/api/admin/bot/init", "POST", null, token);
  const botCount = Number(init?.botCount || 0);
  report.steps.push({
    step: "bot-init",
    ok: botCount >= 100,
    botCount,
    message: init?.message || "",
  });

  for (let i = 0; i < VOTE_ROUNDS; i++) {
    try {
      const res = await api("/api/admin/bot/vote", "POST", null, token);
      report.steps.push({ step: `vote-round-${i + 1}`, ok: true, message: res?.message || "" });
    } catch (e) {
      report.steps.push({ step: `vote-round-${i + 1}`, ok: false, error: String(e.message || e) });
    }
  }

  for (let i = 0; i < TRADE_ROUNDS; i++) {
    try {
      const res = await api("/api/admin/bot/trade", "POST", null, token);
      report.steps.push({ step: `trade-round-${i + 1}`, ok: true, message: res?.message || "" });
    } catch (e) {
      report.steps.push({ step: `trade-round-${i + 1}`, ok: false, error: String(e.message || e) });
    }
  }

  const failed = report.steps.filter((s) => !s.ok);
  report.summary = {
    total: report.steps.length,
    failed: failed.length,
    ok: failed.length === 0,
  };

  console.log(JSON.stringify(report, null, 2));
  if (!report.summary.ok) process.exit(1);
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
