/**
 * SPDX-License-Identifier: Apache-2.0
 *
 * 32-bit word-parallel Bitap fuzzy string matching with Hangul choseong support
 * and fail-closed length/item bounds.
 * Reference implementation: krisk/Fuse (Apache-2.0 License, https://github.com/krisk/Fuse)
 * Adapted with Uint32Array(128) ASCII mask table, Unicode/Hangul Map<number, number>,
 * and zero-allocation per-search R-bitmask state transitions: ((R[d] << 1) | 1) & charMask.
 */

export const MAX_QUERY_LENGTH = 128;
export const MAX_CANDIDATE_LENGTH = 2048;
export const MAX_ITEMS = 10000;
export const BITAP_WORD_BITS = 32;
export const DEFAULT_BITAP_MAX_DISTANCE = 2;

// 19 Korean compatibility Jamo choseong code points aligned with Hangul syllable block (U+AC00..U+D7A3)
const HANGUL_SYLLABLE_START = 0xac00;
const HANGUL_SYLLABLE_END = 0xd7a3;
const HANGUL_SYLLABLES_PER_CHOSEONG = 588;
const HANGUL_CHOSEONG_START = 0x3131;
const HANGUL_CHOSEONG_END = 0x314e;
const HANGUL_CHOSEONG_CODES = new Uint16Array([
  0x3131, // ㄱ
  0x3132, // ㄲ
  0x3134, // ㄴ
  0x3137, // ㄷ
  0x3138, // ㄸ
  0x3139, // ㄹ
  0x3141, // ㅁ
  0x3142, // ㅂ
  0x3143, // ㅃ
  0x3145, // ㅅ
  0x3146, // ㅆ
  0x3147, // ㅇ
  0x3148, // ㅈ
  0x3149, // ㅉ
  0x314a, // ㅊ
  0x314b, // ㅋ
  0x314c, // ㅌ
  0x314d, // ㅍ
  0x314e, // ㅎ
]);

export type BitapSearchOptions = {
  maxDistance?: number;
  caseSensitive?: boolean;
};

export type BitapMatchResult = {
  matched: boolean;
  distance: number;
  score: number;
  endIndex: number;
};

export type BitapSearchHit<T> = {
  item: T;
  index: number;
  distance: number;
  score: number;
  endIndex: number;
};

export type CompiledBitapPattern = {
  query: string;
  normalizedCodes: Uint32Array;
  patternLength: number;
  bitapLength: number;
  maxDistance: number;
  caseSensitive: boolean;
  hasChoseong: boolean;
  asciiMasks: Uint32Array;
  unicodeMasks: Map<number, number> | null;
  matchMask: number;
};

function normalizeCharCode(code: number, caseSensitive: boolean): number {
  if (!caseSensitive && code >= 65 && code <= 90) {
    return code | 32;
  }
  if (!caseSensitive && code > 127) {
    return String.fromCharCode(code).toLowerCase().charCodeAt(0);
  }
  return code;
}

function getHangulChoseongCode(code: number): number {
  if (code < HANGUL_SYLLABLE_START || code > HANGUL_SYLLABLE_END) {
    return 0;
  }
  const index = ((code - HANGUL_SYLLABLE_START) / HANGUL_SYLLABLES_PER_CHOSEONG) | 0;
  return HANGUL_CHOSEONG_CODES[index] ?? 0;
}

function isChoseongJamo(code: number): boolean {
  if (code < HANGUL_CHOSEONG_START || code > HANGUL_CHOSEONG_END) {
    return false;
  }
  for (let i = 0; i < HANGUL_CHOSEONG_CODES.length; i += 1) {
    if (HANGUL_CHOSEONG_CODES[i] === code) {
      return true;
    }
  }
  return false;
}

export function compileBitapPattern(
  query: string,
  options: BitapSearchOptions = {},
): CompiledBitapPattern {
  if (typeof query !== "string") {
    throw new TypeError("query must be a string");
  }
  if (query.length > MAX_QUERY_LENGTH) {
    throw new RangeError(
      `query length (${query.length}) exceeds MAX_QUERY_LENGTH (${MAX_QUERY_LENGTH})`,
    );
  }

  const caseSensitive = options.caseSensitive ?? false;
  const requestedDist = options.maxDistance ?? DEFAULT_BITAP_MAX_DISTANCE;
  const maxDistance = Number.isFinite(requestedDist)
    ? Math.max(0, Math.min(BITAP_WORD_BITS - 1, Math.floor(requestedDist)))
    : DEFAULT_BITAP_MAX_DISTANCE;

  const patternLength = query.length;
  const bitapLength = Math.min(BITAP_WORD_BITS, patternLength);
  const asciiMasks = new Uint32Array(128);
  let unicodeMasks: Map<number, number> | null = null;
  const normalizedCodes = new Uint32Array(patternLength);
  let hasChoseong = false;

  for (let i = 0; i < patternLength; i += 1) {
    const rawCode = query.charCodeAt(i);
    const normCode = normalizeCharCode(rawCode, caseSensitive);
    normalizedCodes[i] = normCode;
    if (isChoseongJamo(normCode)) {
      hasChoseong = true;
    }

    if (i < bitapLength) {
      const bit = (1 << i) >>> 0;
      if (normCode < 128) {
        asciiMasks[normCode] = (asciiMasks[normCode] | bit) >>> 0;
      } else {
        if (!unicodeMasks) {
          unicodeMasks = new Map<number, number>();
        }
        unicodeMasks.set(normCode, ((unicodeMasks.get(normCode) ?? 0) | bit) >>> 0);
      }
    }
  }

  const matchMask = bitapLength > 0 ? (1 << (bitapLength - 1)) >>> 0 : 0;

  return {
    query,
    normalizedCodes,
    patternLength,
    bitapLength,
    maxDistance,
    caseSensitive,
    hasChoseong,
    asciiMasks,
    unicodeMasks,
    matchMask,
  };
}

function getCandidateCharMask(
  compiled: CompiledBitapPattern,
  rawCode: number,
): number {
  const { asciiMasks, unicodeMasks, caseSensitive, hasChoseong } = compiled;
  if (rawCode < 128) {
    const code = !caseSensitive && rawCode >= 65 && rawCode <= 90 ? rawCode | 32 : rawCode;
    return asciiMasks[code];
  }
  if (!unicodeMasks) {
    return 0;
  }
  const normCode = normalizeCharCode(rawCode, caseSensitive);
  let mask = unicodeMasks.get(normCode) ?? 0;
  if (hasChoseong) {
    const choseong = getHangulChoseongCode(rawCode);
    if (choseong !== 0) {
      mask = (mask | (unicodeMasks.get(choseong) ?? 0)) >>> 0;
    }
  }
  return mask;
}

function charsMatchForFallback(
  textCode: number,
  patternNormCode: number,
  caseSensitive: boolean,
  hasChoseong: boolean,
): boolean {
  const normText =
    textCode < 128
      ? !caseSensitive && textCode >= 65 && textCode <= 90
        ? textCode | 32
        : textCode
      : normalizeCharCode(textCode, caseSensitive);
  if (normText === patternNormCode) {
    return true;
  }
  if (hasChoseong) {
    const choseong = getHangulChoseongCode(textCode);
    if (choseong !== 0 && choseong === patternNormCode) {
      return true;
    }
  }
  return false;
}

/**
 * Semi-global Levenshtein DP over a bounded slice [start, end) of text for patterns > 32 chars.
 */
function verifyLongPatternSlice(
  text: string,
  sliceStart: number,
  sliceEnd: number,
  compiled: CompiledBitapPattern,
  dpRow: Int32Array,
): { distance: number; endIndex: number } {
  const { normalizedCodes, patternLength, maxDistance, caseSensitive, hasChoseong } = compiled;
  for (let j = 0; j <= patternLength; j += 1) {
    dpRow[j] = j;
  }

  let bestDist = maxDistance + 1;
  let bestEnd = -1;

  for (let i = sliceStart; i < sliceEnd; i += 1) {
    const rawCode = text.charCodeAt(i);
    let prevDiag = 0; // Semi-global: alignment may start at any text index i with 0 cost
    dpRow[0] = 0;

    for (let j = 1; j <= patternLength; j += 1) {
      const oldAbove = dpRow[j];
      const eq = charsMatchForFallback(
        rawCode,
        normalizedCodes[j - 1],
        caseSensitive,
        hasChoseong,
      );
      const subCost = prevDiag + (eq ? 0 : 1);
      const delCost = oldAbove + 1;
      const insCost = dpRow[j - 1] + 1;
      let nextVal = subCost;
      if (delCost < nextVal) {
        nextVal = delCost;
      }
      if (insCost < nextVal) {
        nextVal = insCost;
      }
      dpRow[j] = nextVal;
      prevDiag = oldAbove;
    }

    const fullDist = dpRow[patternLength];
    if (fullDist < bestDist) {
      bestDist = fullDist;
      bestEnd = i;
      if (bestDist === 0) {
        break;
      }
    }
  }

  return { distance: bestDist, endIndex: bestEnd };
}

function matchWithCompiledPattern(
  text: string,
  compiled: CompiledBitapPattern,
  stateR: Uint32Array,
  longDpRow: Int32Array | null,
): BitapMatchResult {
  if (text.length > MAX_CANDIDATE_LENGTH) {
    throw new RangeError(
      `candidate length (${text.length}) exceeds MAX_CANDIDATE_LENGTH (${MAX_CANDIDATE_LENGTH})`,
    );
  }

  const { patternLength, bitapLength, maxDistance, matchMask } = compiled;
  if (patternLength === 0) {
    return { matched: true, distance: 0, score: 0, endIndex: 0 };
  }
  if (text.length === 0) {
    return { matched: false, distance: maxDistance + 1, score: 1, endIndex: -1 };
  }

  const maxDist = Math.min(maxDistance, Math.max(0, bitapLength - 1));
  stateR[0] = 0;
  for (let d = 1; d <= maxDist; d += 1) {
    stateR[d] = ((1 << d) - 1) >>> 0;
  }

  const isLongPattern = patternLength > BITAP_WORD_BITS && longDpRow !== null;
  let bestDistance = maxDistance + 1;
  let bestEndIndex = -1;

  for (let i = 0; i < text.length; i += 1) {
    const charMask = getCandidateCharMask(compiled, text.charCodeAt(i));

    let prevOld = stateR[0];
    let nextR = (((prevOld << 1) | 1) & charMask) >>> 0;
    stateR[0] = nextR;

    let candidateHitDist = maxDistance + 1;
    if ((nextR & matchMask) !== 0) {
      candidateHitDist = 0;
    }

    for (let d = 1; d <= maxDist; d += 1) {
      const curOld = stateR[d];
      const matchAdvance = ((curOld << 1) | 1) & charMask;
      const subAdvance = (prevOld << 1) | 1;
      const delAdvance = (nextR << 1) | 1;
      const insAdvance = prevOld;
      nextR = (matchAdvance | subAdvance | delAdvance | insAdvance) >>> 0;
      prevOld = curOld;
      stateR[d] = nextR;

      if (d < candidateHitDist && (nextR & matchMask) !== 0) {
        candidateHitDist = d;
      }
    }

    if (!isLongPattern) {
      if (candidateHitDist < bestDistance) {
        bestDistance = candidateHitDist;
        bestEndIndex = i;
        if (bestDistance === 0) {
          break;
        }
      }
    } else if (candidateHitDist <= maxDistance) {
      const sliceStart = Math.max(0, i - bitapLength - maxDistance);
      const sliceEnd = Math.min(text.length, i + (patternLength - bitapLength) + maxDistance + 2);
      const verified = verifyLongPatternSlice(text, sliceStart, sliceEnd, compiled, longDpRow);
      if (verified.distance < bestDistance) {
        bestDistance = verified.distance;
        bestEndIndex = verified.endIndex;
        if (bestDistance === 0) {
          break;
        }
      }
    }
  }

  if (bestDistance > maxDistance || bestEndIndex < 0) {
    return { matched: false, distance: maxDistance + 1, score: 1, endIndex: -1 };
  }

  const distancePenalty = bestDistance / Math.max(1, patternLength);
  const positionPenalty =
    (Math.max(0, bestEndIndex - patternLength + 1) / Math.max(1, text.length)) * 0.25;
  return {
    matched: true,
    distance: bestDistance,
    score: distancePenalty + positionPenalty,
    endIndex: bestEndIndex,
  };
}

export function bitapFuzzyMatch(
  candidate: string,
  query: string,
  options: BitapSearchOptions = {},
): BitapMatchResult {
  if (typeof candidate !== "string") {
    throw new TypeError("candidate must be a string");
  }
  const compiled = compileBitapPattern(query, options);
  const stateR = new Uint32Array(Math.max(1, compiled.maxDistance + 1));
  const longDpRow =
    compiled.patternLength > BITAP_WORD_BITS ? new Int32Array(compiled.patternLength + 1) : null;
  return matchWithCompiledPattern(candidate, compiled, stateR, longDpRow);
}

export function searchBitapItems<T>(
  items: readonly T[],
  query: string,
  getText: (item: T) => string,
  options: BitapSearchOptions = {},
): BitapSearchHit<T>[] {
  if (!Array.isArray(items)) {
    throw new TypeError("items must be an array");
  }
  if (items.length > MAX_ITEMS) {
    throw new RangeError(`items length (${items.length}) exceeds MAX_ITEMS (${MAX_ITEMS})`);
  }

  const compiled = compileBitapPattern(query, options);
  if (compiled.patternLength === 0) {
    const allHits: BitapSearchHit<T>[] = new Array(items.length);
    for (let i = 0; i < items.length; i += 1) {
      allHits[i] = { item: items[i], index: i, distance: 0, score: 0, endIndex: 0 };
    }
    return allHits;
  }

  const stateR = new Uint32Array(Math.max(1, compiled.maxDistance + 1));
  const longDpRow =
    compiled.patternLength > BITAP_WORD_BITS ? new Int32Array(compiled.patternLength + 1) : null;
  const hits: BitapSearchHit<T>[] = [];

  for (let i = 0; i < items.length; i += 1) {
    const candidate = getText(items[i]);
    if (typeof candidate !== "string") {
      continue;
    }
    const result = matchWithCompiledPattern(candidate, compiled, stateR, longDpRow);
    if (result.matched) {
      hits.push({
        item: items[i],
        index: i,
        distance: result.distance,
        score: result.score,
        endIndex: result.endIndex,
      });
    }
  }

  hits.sort((a, b) => {
    if (a.distance !== b.distance) {
      return a.distance - b.distance;
    }
    if (a.score !== b.score) {
      return a.score - b.score;
    }
    return a.index - b.index;
  });

  return hits;
}
