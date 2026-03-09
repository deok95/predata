#!/usr/bin/env node

import fs from "node:fs";
import path from "node:path";

const ROOT = process.cwd();
const OPENAPI_PATH = path.join(ROOT, "backend/docs/openapi/all.json");
const CONTRACT_SOURCES = [
  "backend/docs/v2-read-api-contract.md",
  "backend/docs/pagination-sort-api-contract.md",
  "scripts/qa/e2e-user-flow.mjs",
  "scripts/qa/ui-network-guard-check.mjs",
  "scripts/qa/check-vote-409-loop.mjs",
];
const REPORT_PATH = path.join(ROOT, "backend/docs/FRONT_API_CONTRACT_MISMATCH_REPORT.md");

function mustRead(filePath) {
  if (!fs.existsSync(filePath)) {
    throw new Error(`Required file not found: ${filePath}`);
  }
  return fs.readFileSync(filePath, "utf8");
}

function extractApiPaths(text) {
  const regex = /\/api\/[A-Za-z0-9_./{}:-]*/g;
  const found = text.match(regex) ?? [];
  return found.filter((p) => p.length > "/api/".length);
}

function normalize(pathValue) {
  let value = pathValue.trim();
  if (value.endsWith("/")) value = value.slice(0, -1);
  value = value.replace("/api/questions/status/BETTING", "/api/questions/status/{status}");
  value = value.replace(/\/\d+(?=\/|$)/g, "/{id}");
  return value;
}

function templateMatch(candidate, openapiPath) {
  const a = candidate.split("/").filter(Boolean);
  const b = openapiPath.split("/").filter(Boolean);
  if (a.length !== b.length) return false;
  for (let i = 0; i < a.length; i++) {
    const aa = a[i];
    const bb = b[i];
    const aVar = /^\{.+\}$/.test(aa);
    const bVar = /^\{.+\}$/.test(bb);
    if (aVar || bVar) continue;
    if (aa !== bb) return false;
  }
  return true;
}

// Paths that appear as URL substring checks (e.g. `url.includes("/api/pool/")`) rather
// than actual endpoints. They are valid if any child path exists in OpenAPI.
const PREFIX_ONLY_PATHS = [
  "/api/questions/drafts",
  "/api/users",
  "/api/pool",
];

function checkPath(candidate, openapiPaths) {
  if (openapiPaths.has(candidate)) return { ok: true, reason: "exact" };

  if (PREFIX_ONLY_PATHS.includes(candidate)) {
    const prefix = candidate + "/";
    const hasChild = [...openapiPaths].some((p) => p.startsWith(prefix));
    return hasChild
      ? { ok: true, reason: `prefix(${candidate}/*)` }
      : { ok: false, reason: `no child paths found under ${candidate}` };
  }

  for (const op of openapiPaths) {
    if (templateMatch(candidate, op)) {
      return { ok: true, reason: `template-match:${op}` };
    }
  }
  return { ok: false, reason: "not found in OpenAPI" };
}

function run() {
  const openapi = JSON.parse(mustRead(OPENAPI_PATH));
  const openapiPaths = new Set(Object.keys(openapi.paths ?? {}));

  const extracted = [];
  for (const rel of CONTRACT_SOURCES) {
    const abs = path.join(ROOT, rel);
    const text = mustRead(abs);
    for (const p of extractApiPaths(text)) {
      extracted.push({ source: rel, raw: p, normalized: normalize(p) });
    }
  }

  const dedup = new Map();
  for (const row of extracted) {
    if (!dedup.has(row.normalized)) dedup.set(row.normalized, row);
  }

  const checked = [];
  for (const row of dedup.values()) {
    const result = checkPath(row.normalized, openapiPaths);
    checked.push({ ...row, ...result });
  }

  const missing = checked.filter((x) => !x.ok);

  const lines = [];
  lines.push("# Front Contract vs OpenAPI Mismatch Report");
  lines.push("");
  lines.push(`- Checked at: ${new Date().toISOString()}`);
  lines.push(`- OpenAPI path count: ${openapiPaths.size}`);
  lines.push(`- Contract/QA distinct path count: ${checked.length}`);
  lines.push(`- Missing count: ${missing.length}`);
  lines.push("");
  lines.push("## Covered Paths");
  for (const c of checked.filter((x) => x.ok)) {
    lines.push(`- ${c.normalized} (${c.reason})`);
  }
  lines.push("");
  lines.push("## Missing Paths");
  if (missing.length === 0) {
    lines.push("- none");
  } else {
    for (const m of missing) {
      lines.push(`- ${m.normalized}`);
      lines.push(`  - source: ${m.source}`);
      lines.push(`  - reason: ${m.reason}`);
    }
  }

  fs.writeFileSync(REPORT_PATH, `${lines.join("\n")}\n`);

  console.log(`[contract-check] openapi paths=${openapiPaths.size}`);
  console.log(`[contract-check] checked paths=${checked.length}`);
  console.log(`[contract-check] missing=${missing.length}`);
  console.log(`[contract-check] report=${path.relative(ROOT, REPORT_PATH)}`);

  if (missing.length > 0) process.exit(1);
}

try {
  run();
} catch (err) {
  console.error(`[contract-check] failed: ${err.message}`);
  process.exit(1);
}
