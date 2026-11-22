export const MAX_HIGHLIGHT_CHARS = 65_536;
export const MAX_HIGHLIGHT_LINES = 2_000;
export const MAX_DISPLAY_CHARS = 262_144;

export type CodeLanguage =
  | "ts"
  | "js"
  | "java"
  | "rust"
  | "py"
  | "json"
  | "yaml"
  | "sql"
  | "sh"
  | "css"
  | "html"
  | "md"
  | "text";

export type CodeTokenKind = "plain" | "kw" | "str" | "num" | "com" | "fn" | "type" | "op";

export type CodeToken = {
  kind: CodeTokenKind;
  text: string;
};

export type HighlightedCodeResult = {
  language: CodeLanguage;
  lines: CodeToken[][];
  totalLines: number;
  highlighted: boolean;
  highlightSkipped: boolean;
  truncated: boolean;
};

const KEYWORDS_BY_FAMILY: Record<"c_like" | "py" | "sql" | "sh" | "json" | "yaml" | "css", ReadonlySet<string>> = {
  c_like: new Set([
    "abstract", "as", "async", "await", "bool", "boolean", "break", "case", "catch", "char",
    "class", "const", "continue", "crate", "default", "do", "double", "else", "enum", "export",
    "extends", "extern", "false", "final", "finally", "float", "fn", "for", "from", "function",
    "if", "impl", "implements", "import", "in", "instanceof", "int", "interface", "let", "long",
    "loop", "match", "mod", "mut", "new", "null", "of", "override", "package", "private",
    "protected", "pub", "public", "readonly", "record", "ref", "return", "self", "Self",
    "short", "static", "struct", "super", "switch", "this", "throw", "throws", "trait",
    "true", "try", "type", "typeof", "undefined", "unsafe", "use", "var", "void", "where",
    "while", "yield", "Some", "None", "Ok", "Err", "Result", "Option",
  ]),
  py: new Set([
    "and", "as", "assert", "async", "await", "break", "class", "continue", "def", "del",
    "elif", "else", "except", "False", "finally", "for", "from", "global", "if", "import",
    "in", "is", "lambda", "match", "None", "nonlocal", "not", "or", "pass", "raise",
    "return", "True", "try", "while", "with", "yield",
  ]),
  sql: new Set([
    "ADD", "ALL", "ALTER", "AND", "AS", "ASC", "BETWEEN", "BY", "CASE", "COALESCE", "CREATE",
    "DEFAULT", "DELETE", "DESC", "DISTINCT", "DROP", "ELSE", "END", "EXISTS", "FALSE", "FROM",
    "GROUP", "HAVING", "IN", "INDEX", "INNER", "INSERT", "INTO", "IS", "JOIN", "KEY", "LEFT",
    "LIKE", "LIMIT", "NOT", "NULL", "ON", "OR", "ORDER", "OUTER", "PRIMARY", "RIGHT", "SELECT",
    "SET", "TABLE", "THEN", "TRUE", "UNION", "UNIQUE", "UPDATE", "VALUES", "WHEN", "WHERE",
    "add", "all", "alter", "and", "as", "asc", "between", "by", "case", "coalesce", "create",
    "default", "delete", "desc", "distinct", "drop", "else", "end", "exists", "false", "from",
    "group", "having", "in", "index", "inner", "insert", "into", "is", "join", "key", "left",
    "like", "limit", "not", "null", "on", "or", "order", "outer", "primary", "right", "select",
    "set", "table", "then", "true", "union", "unique", "update", "values", "when", "where",
  ]),
  sh: new Set([
    "case", "do", "done", "elif", "else", "esac", "export", "false", "fi", "for", "function",
    "if", "in", "local", "readonly", "return", "set", "then", "true", "unset", "until", "while",
  ]),
  json: new Set(["true", "false", "null"]),
  yaml: new Set(["true", "false", "null", "yes", "no", "on", "off"]),
  css: new Set(["important", "inherit", "initial", "unset", "auto", "none", "block", "flex", "grid"]),
};

const EXT_TO_LANG: Record<string, CodeLanguage> = {
  ts: "ts",
  tsx: "ts",
  mts: "ts",
  cts: "ts",
  js: "js",
  jsx: "js",
  mjs: "js",
  cjs: "js",
  java: "java",
  kt: "java",
  rs: "rust",
  py: "py",
  json: "json",
  yml: "yaml",
  yaml: "yaml",
  sql: "sql",
  sh: "sh",
  bash: "sh",
  zsh: "sh",
  css: "css",
  scss: "css",
  html: "html",
  htm: "html",
  xml: "html",
  svg: "html",
  md: "md",
  markdown: "md",
  mdx: "md",
  txt: "text",
  log: "text",
  diff: "text",
  patch: "text",
};

export function inferCodeLanguage(fileName?: string | null, contentType?: string | null): CodeLanguage {
  const normalizedName = (fileName ?? "").trim().toLowerCase();
  const dotIdx = normalizedName.lastIndexOf(".");
  if (dotIdx >= 0 && dotIdx < normalizedName.length - 1) {
    const ext = normalizedName.slice(dotIdx + 1);
    const matched = EXT_TO_LANG[ext];
    if (matched) {
      return matched;
    }
  }

  const normalizedType = (contentType ?? "").trim().toLowerCase();
  if (normalizedType.includes("json")) return "json";
  if (normalizedType.includes("typescript")) return "ts";
  if (normalizedType.includes("javascript")) return "js";
  if (normalizedType.includes("yaml")) return "yaml";
  if (normalizedType.includes("sql")) return "sql";
  if (normalizedType.includes("css")) return "css";
  if (normalizedType.includes("html") || normalizedType.includes("xml")) return "html";
  if (normalizedType.includes("markdown")) return "md";
  return "text";
}

function isIdentStart(code: number): boolean {
  return (
    (code >= 65 && code <= 90) ||
    (code >= 97 && code <= 122) ||
    code === 95 ||
    code === 36
  );
}

function isIdentPart(code: number): boolean {
  return isIdentStart(code) || (code >= 48 && code <= 57);
}

function isDigit(code: number): boolean {
  return code >= 48 && code <= 57;
}

function isWhitespaceNoNewline(code: number): boolean {
  return code === 32 || code === 9 || code === 13;
}

function pushTokenAcrossLines(lines: CodeToken[][], kind: CodeTokenKind, slice: string): void {
  if (!slice) {
    return;
  }
  let start = 0;
  while (start <= slice.length) {
    const nl = slice.indexOf("\n", start);
    if (nl === -1) {
      const part = slice.slice(start);
      if (part.length > 0) {
        lines[lines.length - 1].push({ kind, text: part });
      }
      return;
    }
    const part = slice.slice(start, nl);
    if (part.length > 0) {
      lines[lines.length - 1].push({ kind, text: part });
    }
    lines.push([]);
    start = nl + 1;
  }
}

function resolveKeywordSet(language: CodeLanguage): ReadonlySet<string> | null {
  switch (language) {
    case "ts":
    case "js":
    case "java":
    case "rust":
      return KEYWORDS_BY_FAMILY.c_like;
    case "py":
      return KEYWORDS_BY_FAMILY.py;
    case "sql":
      return KEYWORDS_BY_FAMILY.sql;
    case "sh":
      return KEYWORDS_BY_FAMILY.sh;
    case "json":
      return KEYWORDS_BY_FAMILY.json;
    case "yaml":
      return KEYWORDS_BY_FAMILY.yaml;
    case "css":
      return KEYWORDS_BY_FAMILY.css;
    default:
      return null;
  }
}

function splitPlainLines(source: string): CodeToken[][] {
  const rawLines = source.split("\n");
  return rawLines.map((line) => (line.length > 0 ? [{ kind: "plain", text: line }] : []));
}

function peekNextNonSpaceChar(source: string, index: number): number {
  const len = source.length;
  let pos = index;
  while (pos < len && (source.charCodeAt(pos) === 32 || source.charCodeAt(pos) === 9)) {
    pos += 1;
  }
  return pos < len ? source.charCodeAt(pos) : -1;
}

/**
 * Single-pass O(n) syntax tokenizer with strict Fail-Closed char/line bounds.
 * Avoids regex backtracking so crafted or minified inputs render in linear time.
 */
export function tokenizeCodeLines(source: string, language: CodeLanguage): HighlightedCodeResult {
  const truncated = source.length > MAX_DISPLAY_CHARS;
  const boundedText = truncated ? source.slice(0, MAX_DISPLAY_CHARS) : source;

  let totalLines = 1;
  for (let i = 0; i < boundedText.length; i += 1) {
    if (boundedText.charCodeAt(i) === 10) {
      totalLines += 1;
    }
  }

  if (language === "text" || language === "md" || language === "html") {
    return {
      language,
      lines: splitPlainLines(boundedText),
      totalLines,
      highlighted: false,
      highlightSkipped: false,
      truncated,
    };
  }

  if (boundedText.length > MAX_HIGHLIGHT_CHARS || totalLines > MAX_HIGHLIGHT_LINES) {
    return {
      language,
      lines: splitPlainLines(boundedText),
      totalLines,
      highlighted: false,
      highlightSkipped: true,
      truncated,
    };
  }

  const keywords = resolveKeywordSet(language);
  const lines: CodeToken[][] = [[]];
  const n = boundedText.length;
  const supportsSlashLineComment =
    language === "ts" || language === "js" || language === "java" || language === "rust";
  const supportsBlockComment = supportsSlashLineComment || language === "css" || language === "sql";
  const supportsHashComment = language === "py" || language === "sh" || language === "yaml";
  const supportsSqlDashComment = language === "sql";
  const supportsBacktick = language === "ts" || language === "js" || language === "sh";

  let i = 0;
  while (i < n) {
    const ch = boundedText.charCodeAt(i);

    if (ch === 10) {
      lines.push([]);
      i += 1;
      continue;
    }

    if (isWhitespaceNoNewline(ch)) {
      const start = i;
      while (i < n && isWhitespaceNoNewline(boundedText.charCodeAt(i))) {
        i += 1;
      }
      lines[lines.length - 1].push({ kind: "plain", text: boundedText.slice(start, i) });
      continue;
    }

    // Line comments: //, #, --
    if (
      (supportsSlashLineComment && ch === 47 && i + 1 < n && boundedText.charCodeAt(i + 1) === 47) ||
      (supportsHashComment && ch === 35) ||
      (supportsSqlDashComment && ch === 45 && i + 1 < n && boundedText.charCodeAt(i + 1) === 45)
    ) {
      const start = i;
      const nl = boundedText.indexOf("\n", i);
      i = nl === -1 ? n : nl;
      lines[lines.length - 1].push({ kind: "com", text: boundedText.slice(start, i) });
      continue;
    }

    // Block comments: /* ... */
    if (supportsBlockComment && ch === 47 && i + 1 < n && boundedText.charCodeAt(i + 1) === 42) {
      const start = i;
      const end = boundedText.indexOf("*/", i + 2);
      i = end === -1 ? n : end + 2;
      pushTokenAcrossLines(lines, "com", boundedText.slice(start, i));
      continue;
    }

    // String literals: "...", '...', `...`
    if (ch === 34 || ch === 39 || (supportsBacktick && ch === 96)) {
      const quote = ch;
      const start = i;
      i += 1;
      while (i < n) {
        const cur = boundedText.charCodeAt(i);
        if (cur === 92) {
          i += 2;
          continue;
        }
        if (cur === quote) {
          i += 1;
          break;
        }
        if (quote !== 96 && cur === 10) {
          break;
        }
        i += 1;
      }
      const literal = boundedText.slice(start, i);
      const isJsonKey = language === "json" && quote === 34 && peekNextNonSpaceChar(boundedText, i) === 58;
      pushTokenAcrossLines(lines, isJsonKey ? "type" : "str", literal);
      continue;
    }

    // Numbers
    if (isDigit(ch)) {
      const start = i;
      i += 1;
      while (i < n) {
        const cur = boundedText.charCodeAt(i);
        if (
          isDigit(cur) ||
          cur === 46 ||
          cur === 95 ||
          cur === 120 ||
          cur === 88 ||
          (cur >= 97 && cur <= 102) ||
          (cur >= 65 && cur <= 70)
        ) {
          i += 1;
        } else {
          break;
        }
      }
      lines[lines.length - 1].push({ kind: "num", text: boundedText.slice(start, i) });
      continue;
    }

    // Identifiers & keywords
    if (isIdentStart(ch)) {
      const start = i;
      i += 1;
      while (i < n && isIdentPart(boundedText.charCodeAt(i))) {
        i += 1;
      }
      const word = boundedText.slice(start, i);
      let kind: CodeTokenKind = "plain";
      if (keywords?.has(word)) {
        kind = "kw";
      } else if (peekNextNonSpaceChar(boundedText, i) === 40) {
        kind = "fn";
      } else if (ch >= 65 && ch <= 90) {
        kind = "type";
      }
      lines[lines.length - 1].push({ kind, text: word });
      continue;
    }

    // Punctuation / operators
    if ("{}[]().,:;+-*/%=<>!&|^~?@#".includes(boundedText[i])) {
      lines[lines.length - 1].push({ kind: "op", text: boundedText[i] });
      i += 1;
      continue;
    }

    lines[lines.length - 1].push({ kind: "plain", text: boundedText[i] });
    i += 1;
  }

  return {
    language,
    lines,
    totalLines,
    highlighted: true,
    highlightSkipped: false,
    truncated,
  };
}

export async function copyTextSafe(text: string): Promise<boolean> {
  if (typeof navigator === "undefined" || !navigator.clipboard?.writeText) {
    return false;
  }
  try {
    await navigator.clipboard.writeText(text);
    return true;
  } catch {
    return false;
  }
}

export function sanitizeDownloadFilename(fileName?: string | null, fallback = "artifact.txt"): string {
  const trimmed = (fileName ?? "").trim();
  const baseName = trimmed.split(/[/\\]/).pop() ?? "";
  const cleaned = baseName.replace(/[^A-Za-z0-9._-]/g, "_").replace(/^\.+/, "");
  return cleaned.length > 0 ? cleaned.slice(0, 120) : fallback;
}

export function downloadTextArtifact(fileName: string | null | undefined, content: string): boolean {
  if (typeof document === "undefined" || typeof URL === "undefined" || typeof URL.createObjectURL !== "function") {
    return false;
  }
  const safeName = sanitizeDownloadFilename(fileName);
  const blob = new Blob([content], { type: "text/plain;charset=utf-8" });
  const objectUrl = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = objectUrl;
  anchor.download = safeName;
  anchor.style.display = "none";
  document.body.appendChild(anchor);
  anchor.click();
  document.body.removeChild(anchor);
  window.setTimeout(() => {
    URL.revokeObjectURL(objectUrl);
  }, 1_000);
  return true;
}
