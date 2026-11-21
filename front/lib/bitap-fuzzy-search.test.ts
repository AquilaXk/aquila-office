import assert from "node:assert/strict";
import { performance } from "node:perf_hooks";
import test from "node:test";
import {
  MAX_CANDIDATE_LENGTH,
  MAX_ITEMS,
  MAX_QUERY_LENGTH,
  bitapFuzzyMatch,
  compileBitapPattern,
  searchBitapItems,
} from "./bitap-fuzzy-search.ts";

test("bitap-fuzzy-search matches exact, case-insensitive, and 1..2 typo edits", () => {
  const exact = bitapFuzzyMatch("Workspace Controller", "workspace");
  assert.equal(exact.matched, true);
  assert.equal(exact.distance, 0);

  // 1 substitution typo: "workspaxe" vs "workspace"
  const sub = bitapFuzzyMatch("Workspace Controller", "workspaxe", { maxDistance: 1 });
  assert.equal(sub.matched, true);
  assert.equal(sub.distance, 1);

  // 1 insertion typo: "worksspace" vs "workspace"
  const ins = bitapFuzzyMatch("Workspace Controller", "worksspace", { maxDistance: 1 });
  assert.equal(ins.matched, true);
  assert.equal(ins.distance, 1);

  // 1 deletion typo: "wrkspace" vs "workspace"
  const del = bitapFuzzyMatch("Workspace Controller", "wrkspace", { maxDistance: 1 });
  assert.equal(del.matched, true);
  assert.equal(del.distance, 1);

  // Too many edits must fail when maxDistance=1
  const tooFar = bitapFuzzyMatch("Workspace Controller", "wxxkspace", { maxDistance: 1 });
  assert.equal(tooFar.matched, false);
});

test("bitap-fuzzy-search supports Hangul choseong and mixed Hangul queries", () => {
  const fullChoseong = bitapFuzzyMatch("워크스페이스 아티팩트", "ㅇㅋㅅㅍㅇㅅ");
  assert.equal(fullChoseong.matched, true);
  assert.equal(fullChoseong.distance, 0);

  const taskChoseong = bitapFuzzyMatch("태스크 대시보드", "ㅌㅅㅋ");
  assert.equal(taskChoseong.matched, true);
  assert.equal(taskChoseong.distance, 0);

  const mixed = bitapFuzzyMatch("오케스트레이션 플랜", "오ㅋㅅ트", { maxDistance: 0 });
  assert.equal(mixed.matched, true);

  const fuzzyChoseong = bitapFuzzyMatch("태스크 대시보드", "ㅌㅅㅍ", { maxDistance: 1 });
  assert.equal(fuzzyChoseong.matched, true);
  assert.equal(fuzzyChoseong.distance, 1);
});

test("bitap-fuzzy-search handles patterns longer than 32 chars via safe fallback", () => {
  const longQuery = "workspace-agent-orchestration-pipeline-plan";
  assert.ok(longQuery.length > 32 && longQuery.length <= MAX_QUERY_LENGTH);

  const match = bitapFuzzyMatch(
    "Automated workspace-agent-orchestration-pipeline-plan execution report",
    longQuery,
    { maxDistance: 2 },
  );
  assert.equal(match.matched, true);

  const typoMatch = bitapFuzzyMatch(
    "Automated workspace-agent-orchestration-pipeline-plan execution report",
    "workspace-agent-orchestraton-pipeline-plan",
    { maxDistance: 2 },
  );
  assert.equal(typoMatch.matched, true);

  const miss = bitapFuzzyMatch("Completely unrelated agent note", longQuery, { maxDistance: 2 });
  assert.equal(miss.matched, false);
});

test("searchBitapItems ranks exact matches before fuzzy matches", () => {
  const items = [
    { id: 1, label: "Deploy Production Cluster" },
    { id: 2, label: "Task Dashboard Modal" },
    { id: 3, label: "Tusk Dashbard Review" },
    { id: 4, label: "Artifact File Explorer" },
  ];

  const results = searchBitapItems(items, "task dash", (it) => it.label, { maxDistance: 2 });
  assert.ok(results.length >= 2);
  assert.equal(results[0].item.id, 2);
  assert.equal(results[0].distance, 0);
});

test("Fail-Closed limits reject queries >128, candidates >2048, and item lists >10000", () => {
  assert.equal(MAX_QUERY_LENGTH, 128);
  assert.equal(MAX_CANDIDATE_LENGTH, 2048);
  assert.equal(MAX_ITEMS, 10000);

  assert.throws(() => compileBitapPattern("a".repeat(MAX_QUERY_LENGTH + 1)), RangeError);
  assert.throws(
    () => bitapFuzzyMatch("x".repeat(MAX_CANDIDATE_LENGTH + 1), "abc"),
    RangeError,
  );

  const tooManyItems = new Array(MAX_ITEMS + 1).fill("workspace task");
  assert.throws(
    () => searchBitapItems(tooManyItems, "task", (s) => s),
    RangeError,
  );
});

function naiveSlidingLevenshteinContains(text: string, query: string, maxDist: number): boolean {
  const t = text.toLowerCase();
  const q = query.toLowerCase();
  const m = q.length;
  const n = t.length;
  const minW = Math.max(1, m - maxDist);
  const maxW = Math.min(n, m + maxDist);
  for (let start = 0; start < n; start += 1) {
    for (let len = minW; len <= maxW && start + len <= n; len += 1) {
      const sub = t.slice(start, start + len);
      const dp = new Array<number>(len + 1);
      for (let j = 0; j <= len; j += 1) dp[j] = j;
      for (let i = 1; i <= m; i += 1) {
        let prev = dp[0];
        dp[0] = i;
        for (let j = 1; j <= len; j += 1) {
          const cur = dp[j];
          const cost = q.charCodeAt(i - 1) === sub.charCodeAt(j - 1) ? 0 : 1;
          dp[j] = Math.min(dp[j] + 1, dp[j - 1] + 1, prev + cost);
          prev = cur;
        }
      }
      if (dp[len] <= maxDist) return true;
    }
  }
  return false;
}

test("Benchmark: 32-bit word Bitap search beats naive DP Levenshtein scan", () => {
  const itemCount = 3000;
  const candidates: string[] = new Array(itemCount);
  for (let i = 0; i < itemCount; i += 1) {
    candidates[i] =
      i % 97 === 0
        ? `Workspace Task Orchestraton Module ${i}`
        : `Frontend Component Module Record Number ${i} Metadata`;
  }
  const query = "orchestration";

  // Warmup
  for (let i = 0; i < 100; i += 1) {
    bitapFuzzyMatch(candidates[i], query, { maxDistance: 2 });
    naiveSlidingLevenshteinContains(candidates[i], query, 2);
  }

  const t0 = performance.now();
  const bitapHits = searchBitapItems(candidates, query, (s) => s, { maxDistance: 2 });
  const bitapMs = performance.now() - t0;

  const t1 = performance.now();
  let naiveHits = 0;
  for (let i = 0; i < itemCount; i += 1) {
    if (naiveSlidingLevenshteinContains(candidates[i], query, 2)) {
      naiveHits += 1;
    }
  }
  const naiveMs = performance.now() - t1;

  assert.equal(bitapHits.length, naiveHits);
  console.log(
    `[bench][bitap-fuzzy-search] ${itemCount} items: bitap=${bitapMs.toFixed(2)}ms, naiveDP=${naiveMs.toFixed(2)}ms, speedup=${(naiveMs / Math.max(0.01, bitapMs)).toFixed(1)}x`,
  );
  assert.ok(
    bitapMs * 5 < naiveMs,
    `expected Bitap search to be >=5x faster than naive DP scan (bitap=${bitapMs.toFixed(2)}ms, naiveDP=${naiveMs.toFixed(2)}ms)`,
  );
});
