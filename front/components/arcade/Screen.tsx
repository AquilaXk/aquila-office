"use client";

import type { CSSProperties, ReactNode } from "react";

interface T4ScreenProps {
  children: ReactNode;
  title?: string;
  footerLeft?: ReactNode;
  footerRight?: ReactNode;
  className?: string;
  style?: CSSProperties;
}

export default function T4Screen({
  children,
  title = "Workspace",
  footerLeft,
  footerRight,
  className,
  style,
}: T4ScreenProps) {
  return (
    <div
      className={className}
      style={{
        position: "relative",
        width: "100%",
        minHeight: "100dvh",
        height: "100dvh",
        display: "flex",
        flexDirection: "column",
        background: "var(--surface-canvas, #121317)",
        boxSizing: "border-box",
        fontFamily: "var(--font-sans)",
        color: "var(--text-primary, #f8f9fc)",
        overflow: "hidden",
        ...style,
      }}
    >
      <header
        style={{
          height: 52,
          flexShrink: 0,
          display: "flex",
          alignItems: "center",
          gap: 12,
          padding: "0 20px",
          background: "rgba(18, 19, 23, 0.84)",
          backdropFilter: "blur(8px)",
          borderBottom: "1px solid var(--border-subtle)",
          zIndex: 5,
        }}
      >
        <span
          style={{
            color: "var(--text-primary, #f8f9fc)",
            fontFamily: "var(--font-sans)",
            fontSize: 13,
            fontWeight: 600,
            letterSpacing: "-0.01em",
          }}
        >
          Aquila Office
        </span>
        <span
          aria-hidden="true"
          style={{
            color: "var(--text-muted, #6e7681)",
            fontSize: 12,
          }}
        >
          /
        </span>
        <span
          style={{
            color: "var(--text-secondary, #b2bbc5)",
            fontFamily: "var(--font-sans)",
            fontSize: 13,
            fontWeight: 400,
          }}
        >
          {title}
        </span>
      </header>
      <div
        style={{
          flex: 1,
          minHeight: 0,
          overflow: "auto",
          position: "relative",
        }}
      >
        {children}
      </div>
      {(footerLeft || footerRight) && (
        <footer
          style={{
            height: 36,
            flexShrink: 0,
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
            padding: "0 20px",
            borderTop: "1px solid var(--border-subtle)",
            background: "var(--surface-1, #18191d)",
            color: "var(--text-muted, #6e7681)",
            fontSize: 12,
          }}
        >
          <span>{footerLeft}</span>
          <span>{footerRight}</span>
        </footer>
      )}
    </div>
  );
}
