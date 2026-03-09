#!/usr/bin/env node

import { spawnSync } from "node:child_process";

const steps = [
  {
    name: "unit-api-integration-check",
    cmd: ["node", "scripts/qa/unit-api-integration-check.mjs"],
  },
  {
    name: "openapi-contract-match",
    cmd: ["node", "scripts/qa/check-openapi-contract-match.mjs"],
  },
];

for (const step of steps) {
  console.log(`\n[api-checklist] start: ${step.name}`);
  const result = spawnSync(step.cmd[0], step.cmd.slice(1), {
    stdio: "inherit",
    env: process.env,
  });
  if (result.status !== 0) {
    console.error(`[api-checklist] failed: ${step.name}`);
    process.exit(result.status ?? 1);
  }
  console.log(`[api-checklist] ok: ${step.name}`);
}

console.log("\n[api-checklist] all checks passed");
