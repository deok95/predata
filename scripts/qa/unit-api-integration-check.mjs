#!/usr/bin/env node

import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import os from "node:os";
import { pathToFileURL } from "node:url";
import ts from "typescript";

const ROOT = process.cwd();
const API_TS_PATH = path.join(ROOT, "src/services/api.ts");

function createLocalStorage() {
  const store = new Map();
  return {
    getItem(key) {
      return store.has(key) ? store.get(key) : null;
    },
    setItem(key, value) {
      store.set(key, String(value));
    },
    removeItem(key) {
      store.delete(key);
    },
    clear() {
      store.clear();
    },
  };
}

function createHeaders(map) {
  return {
    get(name) {
      return map[String(name).toLowerCase()] ?? null;
    },
  };
}

function createJsonResponse(data, status = 200) {
  const body = { success: true, data, error: null };
  return {
    ok: status >= 200 && status < 300,
    status,
    headers: createHeaders({
      "content-type": "application/json",
      "content-length": "1",
    }),
    async json() {
      return body;
    },
    async text() {
      return JSON.stringify(body);
    },
  };
}

async function loadApiModule() {
  if (!fs.existsSync(API_TS_PATH)) {
    throw new Error(`File not found: ${API_TS_PATH}`);
  }

  globalThis.localStorage = createLocalStorage();
  globalThis.window = {};

  const source = fs.readFileSync(API_TS_PATH, "utf8");
  const transpiled = ts.transpileModule(source, {
    compilerOptions: {
      module: ts.ModuleKind.ESNext,
      target: ts.ScriptTarget.ES2022,
    },
  }).outputText;
  const tempPath = path.join(os.tmpdir(), `predata_api_${Date.now()}.mjs`);
  fs.writeFileSync(tempPath, transpiled, "utf8");
  return import(pathToFileURL(tempPath).href);
}

async function testSettlementModeMapping(mod) {
  let capturedBody = null;
  globalThis.fetch = async (_url, options = {}) => {
    capturedBody = JSON.parse(options.body);
    return createJsonResponse({ questionId: 101 });
  };

  await mod.questionApi.submitDraft(
    99,
    {
      title: "Bitcoin closes above 100k by March?",
      description: "desc",
      category: "CRYPTO",
      voteWindowType: "D1",
      settlementMode: "OBJECTIVE",
      creatorSplitInPool: 60,
    },
    "idem-key-1"
  );

  assert.equal(capturedBody.settlementMode, "OBJECTIVE_RULE");

  await mod.questionApi.submitDraft(
    100,
    {
      title: "ETH ETF approved this week?",
      description: "desc",
      category: "CRYPTO",
      voteWindowType: "D1",
      resolutionType: "manual",
      creatorSplitInPool: 60,
    },
    "idem-key-2"
  );

  assert.equal(capturedBody.settlementMode, "VOTE_RESULT");
}

async function testSwapSimulateAmountParam(mod) {
  let capturedUrl = "";
  globalThis.fetch = async (url) => {
    capturedUrl = String(url);
    return createJsonResponse({ preview: true });
  };

  await mod.marketApi.simulate({
    questionId: 123,
    side: "YES",
    direction: "BUY",
    amount: 25.5,
  });

  const parsed = new URL(capturedUrl);
  assert.equal(parsed.pathname, "/api/swap/simulate");
  assert.equal(parsed.searchParams.get("questionId"), "123");
  assert.equal(parsed.searchParams.get("action"), "BUY");
  assert.equal(parsed.searchParams.get("outcome"), "YES");
  assert.equal(parsed.searchParams.get("amount"), "25.5");
  assert.equal(parsed.searchParams.has("usdcIn"), false);
  assert.equal(parsed.searchParams.has("sharesIn"), false);
}

async function testCompleteSignupPasswordConfirm(mod) {
  let capturedBody = null;
  globalThis.fetch = async (_url, options = {}) => {
    capturedBody = JSON.parse(options.body);
    return createJsonResponse({ token: "token", memberId: 77 });
  };

  await mod.authApi.completeSignup({
    email: "qa@example.com",
    code: "123456",
    password: "password123!",
    passwordConfirm: "password123!",
    nickname: "qa-user",
  });

  assert.equal(capturedBody.email, "qa@example.com");
  assert.equal(capturedBody.code, "123456");
  assert.equal(capturedBody.password, "password123!");
  assert.equal(capturedBody.passwordConfirm, "password123!");
}

async function run() {
  const mod = await loadApiModule();
  const tests = [
    { name: "submitDraft settlementMode mapping", fn: () => testSettlementModeMapping(mod) },
    { name: "simulate uses amount query only", fn: () => testSwapSimulateAmountParam(mod) },
    { name: "completeSignup sends passwordConfirm", fn: () => testCompleteSignupPasswordConfirm(mod) },
  ];

  let passed = 0;
  for (const t of tests) {
    try {
      await t.fn();
      passed += 1;
      console.log(`PASS ${t.name}`);
    } catch (err) {
      console.error(`FAIL ${t.name}`);
      console.error(`  ${err.message}`);
      process.exitCode = 1;
    }
  }

  console.log(`Result: ${passed}/${tests.length} passed`);
}

run().catch((err) => {
  console.error(`FAIL bootstrap`);
  console.error(`  ${err.message}`);
  process.exit(1);
});
