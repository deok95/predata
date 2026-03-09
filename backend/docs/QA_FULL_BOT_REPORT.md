# QA Full Bot Report

- Executed at: 2026-02-24T20:21:09.895Z
- Front runtime reachable: no (http://127.0.0.1:3000)
- Back runtime reachable: no (http://127.0.0.1:8080)
- Summary: PASS 2 / FAIL 0 / SKIP 4

## Results
- [PASS] unit-api-integration: API payload/query contract unit checks
  - timeMs: 77
  - exitCode: 0
- [PASS] openapi-contract-match: Contract docs vs OpenAPI path mismatch check
  - timeMs: 78
  - exitCode: 0
- [SKIP] ui-network-guard: UI API error-status guard check
  - timeMs: 0
  - reason: front/backend runtime not reachable
- [SKIP] vote-409-loop: Vote flow 409 loop regression check
  - timeMs: 0
  - reason: front/backend runtime not reachable
- [SKIP] console-errors-scan: Frontend route console error scan
  - timeMs: 0
  - reason: front/backend runtime not reachable
- [SKIP] e2e-user-flow: Login/vote/bet/comment/submit E2E flow
  - timeMs: 0
  - reason: front/backend runtime not reachable

## Output (tail)
### unit-api-integration
```text
PASS submitDraft settlementMode mapping
PASS simulate uses amount query only
PASS completeSignup sends passwordConfirm
Result: 3/3 passed
```
### openapi-contract-match
```text
[contract-check] openapi paths=150
[contract-check] checked paths=24
[contract-check] missing=0
[contract-check] report=backend/docs/FRONT_API_CONTRACT_MISMATCH_REPORT.md
```
### ui-network-guard
- skipped
### vote-409-loop
- skipped
### console-errors-scan
- skipped
### e2e-user-flow
- skipped
