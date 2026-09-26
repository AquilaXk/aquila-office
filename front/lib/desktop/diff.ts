export type DiffLineType = "header" | "hunk" | "add" | "del" | "ctx";

export type ParsedDiffLine = {
  type: DiffLineType;
  text: string;
  oldNum?: number;
  newNum?: number;
};

export type ParsedUnifiedDiff = {
  lines: ParsedDiffLine[];
  additions: number;
  deletions: number;
};

/**
 * Parses unified diff text into structured lines with line numbers and change counts.
 */
export function parseUnifiedDiff(diff: string): ParsedUnifiedDiff {
  let adds = 0;
  let dels = 0;
  const rawLines = diff.split("\n");
  const parsed: ParsedDiffLine[] = [];

  let oldLine = 0;
  let newLine = 0;

  for (const line of rawLines) {
    if (line.startsWith("@@")) {
      const match = line.match(/@@ -(\d+)(?:,\d+)? \+(\d+)(?:,\d+)? @@/);
      if (match) {
        oldLine = parseInt(match[1], 10);
        newLine = parseInt(match[2], 10);
      }
      parsed.push({ type: "hunk", text: line });
    } else if (
      line.startsWith("diff --git") ||
      line.startsWith("index ") ||
      line.startsWith("--- ") ||
      line.startsWith("+++ ")
    ) {
      parsed.push({ type: "header", text: line });
    } else if (line.startsWith("+")) {
      adds++;
      parsed.push({ type: "add", text: line, newNum: newLine++ });
    } else if (line.startsWith("-")) {
      dels++;
      parsed.push({ type: "del", text: line, oldNum: oldLine++ });
    } else if (line.startsWith("\\")) {
      parsed.push({ type: "header", text: line });
    } else if (line.length > 0) {
      parsed.push({
        type: "ctx",
        text: line,
        oldNum: oldLine++,
        newNum: newLine++,
      });
    }
  }

  return { lines: parsed, additions: adds, deletions: dels };
}
