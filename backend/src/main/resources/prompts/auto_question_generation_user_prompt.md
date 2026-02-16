Generate questions using this runtime input.

Input:
- subcategory: {{subcategory}}
- region: {{region}}
- targetDate: {{targetDate}}
- trendSignals: {{trendSignalsJson}}
- breakMinutes: {{breakMinutes}}
- votingHours: {{votingHours}}
- bettingHours: {{bettingHours}}

Output format:
- Must follow the JSON schema exactly.
- Do not include any text outside JSON.

Additional generation rules:
- 3 questions exactly.
- 2 VERIFIABLE + 1 OPINION.
- OPINION 문구 템플릿 고정:
  "시장은 ...라고 생각할까요?"
- OPINION questions settle from vote result.
- VERIFIABLE questions settle from external sources.
- resolveAt must be realistically settle-able for the question.
- confidence is between 0 and 1.
