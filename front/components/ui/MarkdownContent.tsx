"use client";

import { Check, Copy } from "lucide-react";
import { isValidElement, useCallback, useEffect, useRef, useState, type ReactNode } from "react";
import ReactMarkdown from "react-markdown";
import type { Components } from "react-markdown";
import remarkGfm from "remark-gfm";

type MarkdownContentProps = {
  content: string;
  compact?: boolean;
};

type MarkdownCodeBlockProps = {
  lang: string;
  children: ReactNode;
};

export async function copyMarkdownCodeBlock(text: string): Promise<void> {
  if (typeof navigator === "undefined" || !navigator.clipboard?.writeText) {
    return;
  }
  await navigator.clipboard.writeText(text);
}

export default function MarkdownContent({ content, compact = false }: MarkdownContentProps) {
  return (
    <div className={compact ? "markdown-artifact-content markdown-compact-content" : "markdown-artifact-content"}>
      <ReactMarkdown remarkPlugins={[remarkGfm]} components={markdownComponents}>
        {content}
      </ReactMarkdown>
    </div>
  );
}

function MarkdownCodeBlock({ lang, children }: MarkdownCodeBlockProps) {
  const [copied, setCopied] = useState(false);
  const resetTimerRef = useRef<number | null>(null);
  const code = extractText(children);

  useEffect(() => {
    return () => {
      if (resetTimerRef.current !== null) {
        window.clearTimeout(resetTimerRef.current);
      }
    };
  }, []);

  const handleCopy = useCallback(() => {
    void copyMarkdownCodeBlock(code)
      .then(() => {
        setCopied(true);
        if (resetTimerRef.current !== null) {
          window.clearTimeout(resetTimerRef.current);
        }
        resetTimerRef.current = window.setTimeout(() => {
          setCopied(false);
          resetTimerRef.current = null;
        }, 1500);
      })
      .catch(() => {});
  }, [code]);

  return (
    <div className="markdown-artifact-code">
      <div className="markdown-artifact-code-header">
        <span>{lang || "code"}</span>
        <button type="button" onClick={handleCopy} aria-label="code block 복사">
          {copied ? <Check aria-hidden="true" /> : <Copy aria-hidden="true" />}
        </button>
      </div>
      <pre>
        <code>{children}</code>
      </pre>
    </div>
  );
}

function extractText(node: ReactNode): string {
  if (typeof node === "string" || typeof node === "number") {
    return String(node);
  }

  if (Array.isArray(node)) {
    return node.map(extractText).join("");
  }

  if (node && typeof node === "object" && "props" in node) {
    const props = node.props as { children?: ReactNode };
    return extractText(props.children);
  }

  return "";
}

function sanitizeMarkdownHref(href?: string): string | undefined {
  if (!href) {
    return undefined;
  }
  const trimmed = href.trim();
  if (trimmed.startsWith("#") || (trimmed.startsWith("/") && !trimmed.startsWith("//"))) {
    return trimmed;
  }
  try {
    const parsed = new URL(trimmed);
    if (parsed.protocol === "http:" || parsed.protocol === "https:" || parsed.protocol === "mailto:") {
      return parsed.toString();
    }
  } catch {
    return undefined;
  }
  return undefined;
}

const markdownComponents: Components = {
  h1: ({ children }) => <h1>{children}</h1>,
  h2: ({ children }) => <h2>{children}</h2>,
  h3: ({ children }) => <h3>{children}</h3>,
  p: ({ children }) => <p>{children}</p>,
  ul: ({ children }) => <ul>{children}</ul>,
  ol: ({ children }) => <ol>{children}</ol>,
  li: ({ children }) => <li>{children}</li>,
  blockquote: ({ children }) => <blockquote>{children}</blockquote>,
  a: ({ href, children }) => {
    const safeHref = sanitizeMarkdownHref(href);
    if (!safeHref) {
      return <span>{children}</span>;
    }
    return (
      <a href={safeHref} target="_blank" rel="noreferrer noopener">
        {children}
      </a>
    );
  },
  code: ({ className, children }) => (
    <code
      className={
        className ? `markdown-artifact-inline-code ${className}` : "markdown-artifact-inline-code"
      }
    >
      {children}
    </code>
  ),
  pre: ({ children }) => {
    if (isValidElement<{ className?: string; children?: ReactNode }>(children)) {
      const childClassName = children.props.className ?? "";
      const language = childClassName.startsWith("language-")
        ? childClassName.replace("language-", "")
        : "";
      return <MarkdownCodeBlock lang={language}>{children.props.children}</MarkdownCodeBlock>;
    }
    return <MarkdownCodeBlock lang="">{children}</MarkdownCodeBlock>;
  },
  table: ({ children }) => (
    <div className="markdown-artifact-table-scroll">
      <table>{children}</table>
    </div>
  ),
  th: ({ children }) => <th>{children}</th>,
  td: ({ children }) => <td>{children}</td>,
};
