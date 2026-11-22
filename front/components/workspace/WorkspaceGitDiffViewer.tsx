"use client";

import { useMemo, useState } from "react";
import { Check, Copy, FileCode2, ChevronDown, ChevronRight } from "lucide-react";

export type WorkspaceGitDiffViewerProps = {
  diff: string;
  filePath?: string;
  className?: string;
};

import { parseUnifiedDiff } from "@/lib/desktop/diff";
import { useCopyFeedback } from "@/lib/use-copy-feedback";

export type { DiffLineType, ParsedDiffLine } from "@/lib/desktop/diff";
export { parseUnifiedDiff };

export default function WorkspaceGitDiffViewer({
  diff,
  filePath,
  className = "",
}: WorkspaceGitDiffViewerProps) {
  const { copiedKey, copy } = useCopyFeedback<"diff">();
  const [collapsed, setCollapsed] = useState(false);
  const copied = copiedKey === "diff";

  const { lines, additions, deletions } = useMemo(
    () => parseUnifiedDiff(diff),
    [diff],
  );

  const handleCopy = () => {
    void copy(diff, "diff");
  };

  return (
    <div
      className={`ao-card ao-diff-viewer ${className}`.trim()}
      style={{
        overflow: "hidden",
        border: "1px solid var(--ui-border)",
        background: "var(--ui-surface)",
        borderRadius: 8,
      }}
    >
      <div
        className="flex items-center justify-between px-3 py-2"
        style={{
          background: "var(--ui-surface-raised)",
          borderBottom: "1px solid var(--ui-border-subtle)",
        }}
      >
        <button
          type="button"
          onClick={() => setCollapsed(!collapsed)}
          className="flex items-center gap-2 text-left"
          style={{
            fontFamily: "var(--font-mono)",
            fontSize: 12,
            color: "var(--ui-text)",
            background: "transparent",
            border: 0,
            cursor: "pointer",
          }}
          aria-expanded={!collapsed}
          aria-label={collapsed ? "Expand diff" : "Collapse diff"}
        >
          {collapsed ? <ChevronRight size={14} /> : <ChevronDown size={14} />}
          <FileCode2 size={14} style={{ color: "var(--accent-primary)" }} />
          <span style={{ fontWeight: 600 }}>{filePath || "Workspace Diff"}</span>
        </button>

        <div className="flex items-center gap-3">
          <div
            className="flex items-center gap-2"
            style={{ fontFamily: "var(--font-mono)", fontSize: 12 }}
          >
            <span style={{ color: "var(--status-success)", fontWeight: 600 }}>+{additions}</span>
            <span style={{ color: "var(--status-danger)", fontWeight: 600 }}>-{deletions}</span>
          </div>

          <button
            type="button"
            onClick={handleCopy}
            className="p-1 rounded"
            style={{
              color: "var(--ui-text-secondary)",
              background: "transparent",
              border: 0,
              cursor: "pointer",
            }}
            title="Diff 복사"
            aria-label="Copy diff"
          >
            {copied ? <Check size={14} style={{ color: "var(--status-success)" }} /> : <Copy size={14} />}
          </button>
        </div>
      </div>

      {!collapsed && (
        <div
          style={{
            overflowX: "auto",
            overflowY: "auto",
            maxHeight: 540,
            padding: 8,
            fontFamily: "var(--font-mono)",
            fontSize: 12,
            lineHeight: 1.55,
          }}
        >
          {lines.length === 0 ? (
            <div
              style={{
                padding: 16,
                textAlign: "center",
                color: "var(--ui-text-muted)",
              }}
            >
              변경된 내용이 없습니다.
            </div>
          ) : (
            lines.map((l, idx) => {
              if (l.type === "hunk") {
                return (
                  <div
                    key={idx}
                    style={{
                      padding: "4px 10px",
                      margin: "4px 0",
                      color: "var(--diff-hunk-text)",
                      background: "var(--diff-hunk-bg)",
                      borderRadius: 4,
                      fontSize: 12,
                      fontWeight: 600,
                      borderLeft: "2px solid var(--accent-primary)",
                      userSelect: "none",
                    }}
                  >
                    {l.text}
                  </div>
                );
              }

              if (l.type === "header") {
                return (
                  <div
                    key={idx}
                    style={{
                      padding: "2px 10px",
                      color: "var(--ui-text-muted)",
                      userSelect: "none",
                    }}
                  >
                    {l.text}
                  </div>
                );
              }

              const isAdd = l.type === "add";
              const isDel = l.type === "del";

              return (
                <div
                  key={idx}
                  className="flex items-start px-2 py-0.5 rounded-sm"
                  style={{
                    background: isAdd
                      ? "var(--diff-add-bg)"
                      : isDel
                        ? "var(--diff-del-bg)"
                        : "transparent",
                    color: isAdd
                      ? "var(--diff-add-text)"
                      : isDel
                        ? "var(--diff-del-text)"
                        : "var(--ui-text-secondary)",
                  }}
                >
                  <span
                    className="w-8 shrink-0 text-right pr-2 select-none"
                    style={{ color: "var(--ui-text-muted)" }}
                  >
                    {l.oldNum ?? ""}
                  </span>
                  <span
                    className="w-8 shrink-0 text-right pr-2 select-none"
                    style={{ color: "var(--ui-text-muted)" }}
                  >
                    {l.newNum ?? ""}
                  </span>
                  <span className="w-4 shrink-0 text-center select-none font-bold">
                    {isAdd ? "+" : isDel ? "-" : " "}
                  </span>
                  <span className="whitespace-pre-wrap break-all flex-1">
                    {l.text[0] === "+" || l.text[0] === "-" || l.text[0] === " "
                      ? l.text.slice(1)
                      : l.text}
                  </span>
                </div>
              );
            })
          )}
        </div>
      )}
    </div>
  );
}
