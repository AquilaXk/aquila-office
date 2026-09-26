"use client";

import { useMemo, useState } from "react";
import { Check, Copy, FileCode2, ChevronDown, ChevronRight } from "lucide-react";

export type WorkspaceGitDiffViewerProps = {
  diff: string;
  filePath?: string;
  className?: string;
};

import { parseUnifiedDiff } from "@/lib/desktop/diff";

export type { DiffLineType, ParsedDiffLine } from "@/lib/desktop/diff";
export { parseUnifiedDiff };

export default function WorkspaceGitDiffViewer({
  diff,
  filePath,
  className = "",
}: WorkspaceGitDiffViewerProps) {
  const [copied, setCopied] = useState(false);
  const [collapsed, setCollapsed] = useState(false);

  const { lines, additions, deletions } = useMemo(
    () => parseUnifiedDiff(diff),
    [diff],
  );

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(diff);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // fallback
    }
  };

  return (
    <div
      className={`ao-card overflow-hidden border border-[var(--border-subtle,#343A40)] bg-[var(--surface-card,#212529)] rounded-xl shadow-lg ${className}`}
    >
      {/* Header bar */}
      <div className="flex items-center justify-between px-4 py-2.5 bg-[var(--surface-subtle,#2A2E33)] border-b border-[var(--border-subtle,#343A40)]">
        <button
          type="button"
          onClick={() => setCollapsed(!collapsed)}
          className="flex items-center gap-2 text-left font-mono text-xs text-[var(--ink-primary,#F8F9FA)] hover:text-white transition-colors"
          aria-expanded={!collapsed}
          aria-label={collapsed ? "Expand diff" : "Collapse diff"}
        >
          {collapsed ? <ChevronRight size={14} /> : <ChevronDown size={14} />}
          <FileCode2 size={14} className="text-[var(--brand-400,#8E9CE6)]" />
          <span className="font-semibold">{filePath || "Workspace Diff"}</span>
        </button>

        <div className="flex items-center gap-3">
          <div className="flex items-center gap-1.5 text-xs font-mono">
            <span className="text-emerald-400 font-medium">+{additions}</span>
            <span className="text-rose-400 font-medium">-{deletions}</span>
          </div>

          <button
            type="button"
            onClick={handleCopy}
            className="p-1 rounded text-[var(--ink-muted,#9CA3AF)] hover:text-white hover:bg-[var(--surface-card,#212529)] transition-colors"
            title="Copy diff"
            aria-label="Copy diff"
          >
            {copied ? <Check size={14} className="text-emerald-400" /> : <Copy size={14} />}
          </button>
        </div>
      </div>

      {/* Diff body */}
      {!collapsed && (
        <div className="overflow-x-auto p-2 font-mono text-xs leading-relaxed max-h-[600px] overflow-y-auto">
          {lines.length === 0 ? (
            <div className="p-4 text-center text-[var(--ink-muted,#9CA3AF)]">
              No changes in this file
            </div>
          ) : (
            lines.map((l, idx) => {
              if (l.type === "hunk") {
                return (
                  <div
                    key={idx}
                    className="px-3 py-1 my-1 text-[var(--brand-400,#8E9CE6)] bg-[rgba(92,107,192,0.12)] rounded text-[11px] font-semibold select-none border-l-2 border-[var(--brand-500,#5C6BC0)]"
                  >
                    {l.text}
                  </div>
                );
              }

              if (l.type === "header") {
                return (
                  <div
                    key={idx}
                    className="px-3 py-0.5 text-[var(--ink-muted,#9CA3AF)] select-none"
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
                  className={`flex items-start px-2 py-0.5 rounded-sm transition-colors ${
                    isAdd
                      ? "bg-[rgba(92,107,192,0.18)] text-emerald-300"
                      : isDel
                        ? "bg-[rgba(208,106,94,0.16)] text-rose-300"
                        : "text-[var(--ink-secondary,#E5E7EB)] hover:bg-[rgba(255,255,255,0.03)]"
                  }`}
                >
                  <span className="w-8 shrink-0 text-right pr-2 select-none text-[var(--ink-muted,#697089)]">
                    {l.oldNum ?? ""}
                  </span>
                  <span className="w-8 shrink-0 text-right pr-2 select-none text-[var(--ink-muted,#697089)]">
                    {l.newNum ?? ""}
                  </span>
                  <span className="w-4 shrink-0 text-center select-none font-bold">
                    {isAdd ? "+" : isDel ? "-" : " "}
                  </span>
                  <span className="whitespace-pre-wrap break-all flex-1">
                    {l.text.replace(/^[+ -]/, "")}
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
