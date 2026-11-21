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
        className="topbar"
        style={{
          height: 36,
          flexShrink: 0,
          display: "flex",
          alignItems: "center",
          gap: 8,
          padding: "0 12px",
          background: "var(--ui-bg-light, #18191d)",
          borderBottom: "1px solid var(--ui-border-light, rgba(230, 234, 240, 0.12))",
          fontSize: 12,
          zIndex: 5,
        }}
      >
        <span
          style={{
            color: "var(--ui-text-strong, #f8f9fc)",
            fontFamily: "var(--font-family-ui, var(--font-sans))",
            fontSize: 12,
            fontWeight: 600,
            letterSpacing: "-0.01em",
          }}
        >
          Aquila Office
        </span>
        <span
          aria-hidden="true"
          style={{
            color: "var(--ui-text-muted, #6e7681)",
            fontSize: 12,
          }}
        >
          /
        </span>
        <span
          style={{
            color: "var(--ui-text-secondary, #b2bbc5)",
            fontFamily: "var(--font-family-ui, var(--font-sans))",
            fontSize: 12,
            fontWeight: 500,
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
          className="statusbar"
          style={{
            height: 24,
            flexShrink: 0,
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
            padding: "0 10px",
            borderTop: "1px solid var(--ui-border-subtle, rgba(230, 234, 240, 0.08))",
            background: "var(--ui-bg-light, #18191d)",
            color: "var(--ui-text-muted, #6e7681)",
            fontSize: "var(--font-size-xs)",
          }}
        >
          <span>{footerLeft}</span>
          <span>{footerRight}</span>
        </footer>
      )}
    </div>
  );
}
