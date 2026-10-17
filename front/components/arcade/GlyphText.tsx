"use client";

import type { CSSProperties, ReactNode } from "react";

type GlyphTextProps = {
  glyph?: string;
  children: ReactNode;
  className?: string;
  style?: CSSProperties;
  truncate?: boolean;
  glyphPosition?: "start" | "end";
};

export default function GlyphText({
  children,
  className,
  style,
  truncate = false,
}: GlyphTextProps) {
  return (
    <span
      className={className}
      style={{
        display: "inline-flex",
        alignItems: "center",
        minWidth: 0,
        maxWidth: "100%",
        verticalAlign: "middle",
        ...style,
      }}
    >
      <span className={truncate ? "min-w-0 truncate" : "min-w-0"}>
        {children}
      </span>
    </span>
  );
}
