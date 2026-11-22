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
  const rawLines = diff.replace(/\r\n/g, "\n").split("\n");
  const parsed: ParsedDiffLine[] = [];

  let oldLine = 0;
  let newLine = 0;
  let inHunk = false;
  let oldRemaining = 0;
  let newRemaining = 0;

  for (let i = 0; i < rawLines.length; i += 1) {
    const line = rawLines[i];
    if (line.startsWith("@@")) {
      const match = line.match(/@@ -(\d+)(?:,(\d+))? \+(\d+)(?:,(\d+))? @@/);
      if (match) {
        oldLine = parseInt(match[1], 10);
        oldRemaining = match[2] !== undefined ? parseInt(match[2], 10) : 1;
        newLine = parseInt(match[3], 10);
        newRemaining = match[4] !== undefined ? parseInt(match[4], 10) : 1;
      } else {
        oldRemaining = 0;
        newRemaining = 0;
      }
      inHunk = true;
      parsed.push({ type: "hunk", text: line });
    } else if (
      line.startsWith("diff --git") ||
      line.startsWith("index ") ||
      line.startsWith("--- ") ||
      line.startsWith("+++ ")
    ) {
      inHunk = false;
      oldRemaining = 0;
      newRemaining = 0;
      parsed.push({ type: "header", text: line });
    } else if (line.startsWith("+")) {
      adds++;
      if (newRemaining > 0) {
        newRemaining--;
      }
      parsed.push({ type: "add", text: line, newNum: newLine++ });
    } else if (line.startsWith("-")) {
      dels++;
      if (oldRemaining > 0) {
        oldRemaining--;
      }
      parsed.push({ type: "del", text: line, oldNum: oldLine++ });
    } else if (line.startsWith("\\")) {
      parsed.push({ type: "header", text: line });
    } else if (
      line.length > 0 ||
      (line === "" &&
        i < rawLines.length - 1 &&
        inHunk &&
        (oldRemaining > 0 || newRemaining > 0))
    ) {
      if (oldRemaining > 0) {
        oldRemaining--;
      }
      if (newRemaining > 0) {
        newRemaining--;
      }
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
