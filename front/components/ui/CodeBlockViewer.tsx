"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { Check, Copy, Download } from "lucide-react";

import {
  copyTextSafe,
  downloadTextArtifact,
  inferCodeLanguage,
  tokenizeCodeLines,
} from "@/lib/code-highlight";

type CodeBlockViewerProps = {
  code: string;
  fileName?: string | null;
  contentType?: string | null;
  className?: string;
  maxHeightClassName?: string;
  showLineNumbers?: boolean;
};

function formatCharSize(bytes: number): string {
  if (bytes < 1024) {
    return `${bytes} B`;
  }
  return `${(bytes / 1024).toFixed(1)} KB`;
}

export default function CodeBlockViewer({
  code,
  fileName,
  contentType,
  className = "",
  maxHeightClassName = "max-h-[520px]",
  showLineNumbers = true,
}: CodeBlockViewerProps) {
  const [copied, setCopied] = useState(false);
  const resetTimerRef = useRef<number | null>(null);

  useEffect(() => {
    return () => {
      if (resetTimerRef.current != null) {
        window.clearTimeout(resetTimerRef.current);
      }
    };
  }, []);

  const result = useMemo(() => {
    const language = inferCodeLanguage(fileName, contentType);
    return tokenizeCodeLines(code, language);
  }, [code, fileName, contentType]);

  const handleCopy = async () => {
    const ok = await copyTextSafe(code);
    if (!ok) {
      return;
    }
    setCopied(true);
    if (resetTimerRef.current != null) {
      window.clearTimeout(resetTimerRef.current);
    }
    resetTimerRef.current = window.setTimeout(() => {
      setCopied(false);
      resetTimerRef.current = null;
    }, 1500);
  };

  return (
    <div className={`ao-code-viewer ${className}`.trim()}>
      <div className="ao-code-viewer-header">
        <div className="ao-code-viewer-meta">
          <span className="ao-code-viewer-lang">{result.language}</span>
          <span>{result.totalLines}줄</span>
          <span>{formatCharSize(code.length)}</span>
        </div>
        <div className="ao-code-viewer-actions">
          <button
            type="button"
            className="tb-btn"
            onClick={() => downloadTextArtifact(fileName, code)}
          >
            <Download size={12} aria-hidden="true" />
            <span>다운로드</span>
          </button>
          <button
            type="button"
            className="tb-btn"
            onClick={() => void handleCopy()}
          >
            {copied ? (
              <Check size={12} aria-hidden="true" />
            ) : (
              <Copy size={12} aria-hidden="true" />
            )}
            <span>{copied ? "복사됨" : "코드 복사"}</span>
          </button>
        </div>
      </div>

      {result.highlightSkipped || result.truncated ? (
        <div className="ao-code-viewer-notice">
          {result.truncated
            ? "대용량 파일(>256KB)은 앞부분만 미리 표시한다. 전체 내용은 다운로드로 확인할 수 있다."
            : "대용량 파일(>64KB 또는 2,000줄)로 구문 강조를 생략하고 일반 텍스트로 표시한다."}
        </div>
      ) : null}

      <div className={`ao-code-viewer-body ${maxHeightClassName}`.trim()}>
        <div className="ao-code-viewer-lines" role="region" aria-label={fileName ?? "코드 미리보기"}>
          {result.lines.map((tokens, lineIndex) => (
            <div key={lineIndex} className="ao-code-viewer-row">
              {showLineNumbers ? (
                <span className="ao-code-viewer-gutter" aria-hidden="true">
                  {lineIndex + 1}
                </span>
              ) : null}
              <code className="ao-code-viewer-code">
                {tokens.length === 0
                  ? "\n"
                  : tokens.map((token, tokenIndex) =>
                      token.kind === "plain" ? (
                        token.text
                      ) : (
                        <span key={tokenIndex} className={`ao-tok-${token.kind}`}>
                          {token.text}
                        </span>
                      ),
                    )}
              </code>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
