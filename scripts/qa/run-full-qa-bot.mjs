#!/usr/bin/env node

import fs from "node:fs";
import path from "node:path";
import { spawnSync } from "node:child_process";

const ROOT = process.cwd();
const FRONT_BASE = process.env.QA_BASE_URL || "http://127.0.0.1:3000";
const BACK_BASE = process.env.QA_API_URL || "http://127.0.0.1:8080";
const REPORT_PATH = path.join(ROOT, "backend/docs/QA_FULL_BOT_REPORT.md");

const TESTS = [
  {
    id: "unit-api-integration",
    desc: "API payload/query contract unit checks",
    cmd: ["node", "scripts/qa/unit-api-integration-check.mjs"],
    requiresRuntime: false,
  },
  {
    id: "openapi-contract-match",
    desc: "Contract docs vs OpenAPI path mismatch check",
    cmd: ["node", "scripts/qa/check-openapi-contract-match.mjs"],
    requiresRuntime: false,
  },
  {
    id: "bot-100-load-check",
    desc: "Initialize 100 independent bots and run vote/trade rounds",
    cmd: ["node", "scripts/qa/bot-100-load-check.mjs"],
    requiresRuntime: true,
  },
  {
    id: "ui-network-guard",
    desc: "UI API error-status guard check",
    cmd: ["node", "scripts/qa/ui-network-guard-check.mjs"],
    requiresRuntime: true,
  },
  {
    id: "vote-409-loop",
    desc: "Vote flow 409 loop regression check",
    cmd: ["node", "scripts/qa/check-vote-409-loop.mjs"],
    requiresRuntime: true,
  },
  {
    id: "console-errors-scan",
    desc: "Frontend route console error scan",
    cmd: ["node", "scripts/qa/console-errors-scan.mjs"],
    requiresRuntime: true,
  },
  {
    id: "e2e-user-flow",
    desc: "Login/vote/bet/comment/submit E2E flow",
    cmd: ["node", "scripts/qa/e2e-user-flow.mjs"],
    requiresRuntime: true,
  },
];

async function ping(url, ms = 2200) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), ms);
  try {
    const res = await fetch(url, { method: "GET", signal: controller.signal });
    return res.status > 0;
  } catch {
    return false;
  } finally {
    clearTimeout(timer);
  }
}

function runCommand(cmd) {
  const started = Date.now();
  const out = spawnSync(cmd[0], cmd.slice(1), {
    cwd: ROOT,
    env: process.env,
    encoding: "utf8",
    maxBuffer: 10 * 1024 * 1024,
  });
  const elapsedMs = Date.now() - started;
  return {
    ok: out.status === 0,
    code: out.status ?? -1,
    elapsedMs,
    stdout: (out.stdout || "").trim(),
    stderr: (out.stderr || "").trim(),
  };
}

function toReport(results, runtimeReady) {
  const passed = results.filter((r) => r.status === "PASS").length;
  const failed = results.filter((r) => r.status === "FAIL").length;
  const skipped = results.filter((r) => r.status === "SKIP").length;

  const lines = [];
  lines.push("# QA Full Bot Report");
  lines.push("");
  lines.push(`- Executed at: ${new Date().toISOString()}`);
  lines.push(`- Front runtime reachable: ${runtimeReady.front ? "yes" : "no"} (${FRONT_BASE})`);
  lines.push(`- Back runtime reachable: ${runtimeReady.back ? "yes" : "no"} (${BACK_BASE})`);
  lines.push(`- Summary: PASS ${passed} / FAIL ${failed} / SKIP ${skipped}`);
  lines.push("");
  lines.push("## Results");
  for (const r of results) {
    lines.push(`- [${r.status}] ${r.id}: ${r.desc}`);
    lines.push(`  - timeMs: ${r.elapsedMs}`);
    if (r.reason) lines.push(`  - reason: ${r.reason}`);
    if (r.code !== undefined) lines.push(`  - exitCode: ${r.code}`);
  }
  lines.push("");
  lines.push("## Output (tail)");
  for (const r of results) {
    lines.push(`### ${r.id}`);
    if (r.status === "SKIP") {
      lines.push("- skipped");
      continue;
    }
    const out = (r.stdout || r.stderr || "").split("\n").slice(-12).join("\n");
    lines.push("```text");
    lines.push(out || "(no output)");
    lines.push("```");
  }
  return `${lines.join("\n")}\n`;
}

async function main() {
  const runtimeReady = {
    front: await ping(`${FRONT_BASE}/`),
    back: await ping(`${BACK_BASE}/api/health`),
  };
  const canRunRuntime = runtimeReady.front && runtimeReady.back;

  const results = [];
  for (const t of TESTS) {
    if (t.requiresRuntime && !canRunRuntime) {
      results.push({
        id: t.id,
        desc: t.desc,
        status: "SKIP",
        reason: "front/backend runtime not reachable",
        elapsedMs: 0,
      });
      console.log(`SKIP ${t.id} (runtime unavailable)`);
      continue;
    }

    console.log(`RUN ${t.id}`);
    const r = runCommand(t.cmd);
    results.push({
      id: t.id,
      desc: t.desc,
      status: r.ok ? "PASS" : "FAIL",
      code: r.code,
      elapsedMs: r.elapsedMs,
      stdout: r.stdout,
      stderr: r.stderr,
    });
    console.log(`${r.ok ? "PASS" : "FAIL"} ${t.id} (${r.elapsedMs}ms)`);
  }

  fs.writeFileSync(REPORT_PATH, toReport(results, runtimeReady), "utf8");
  console.log(`REPORT ${path.relative(ROOT, REPORT_PATH)}`);

  const hasFail = results.some((r) => r.status === "FAIL");
  if (hasFail) process.exit(1);
}

main().catch((err) => {
  console.error(`FAIL qa-bot bootstrap: ${err.message}`);
  process.exit(1);
});
