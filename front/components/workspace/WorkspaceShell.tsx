"use client";

import Link from "next/link";
import type { ReactNode } from "react";
import { Button } from "@/components/ui";
import { GlyphText } from "@/components/arcade";
import type { WorkspaceDetail } from "@/lib/api/workspaces";

export default function WorkspaceShell({
  children,
}: {
  workspace: WorkspaceDetail;
  children: ReactNode;
  sidebar?: ReactNode;
}) {
  return <>{children}</>;
}

export function WorkspaceErrorState({ message }: { message: string }) {
  return (
    <main
      className="theme-web flex min-h-screen items-center justify-center px-4"
      style={{ background: "var(--t4-bg)" }}
    >
      <section
        className="w-full max-w-md px-6 py-8 text-center"
        style={{
          border: "1px solid var(--t4-hp)",
          background: "rgba(31,35,39,0.92)",
          boxShadow: "0 0 24px rgba(208,106,94,0.24)",
        }}
      >
        <p
          style={{
            fontFamily: "var(--font-pixel)",
            fontSize: 10,
            letterSpacing: 2,
            color: "var(--t4-hp)",
            textShadow: "0 0 6px var(--t4-hp)",
            marginBottom: 16,
          }}
        >
          <GlyphText glyph="⚠">ERROR</GlyphText>
        </p>
        <p
          style={{
            fontFamily: "var(--font-mono-arcade)",
            fontSize: 12,
            color: "var(--t4-ink)",
          }}
        >
          {message}
        </p>
        <Link href="/workspaces" className="mt-5 inline-flex">
          <Button variant="secondary">
            <GlyphText glyph="◀">WORKSPACES</GlyphText>
          </Button>
        </Link>
      </section>
    </main>
  );
}

export function WorkspaceLoadingState() {
  return (
    <main
      className="theme-web flex min-h-screen items-center justify-center px-4"
      style={{ background: "var(--t4-bg)" }}
    >
      <div
        style={{
          fontFamily: "var(--font-pixel)",
          fontSize: 12,
          letterSpacing: 3,
          color: "var(--t4-pink)",
          textShadow: "0 0 8px var(--t4-pink)",
          animation: "t4-pulse 1.4s ease-in-out infinite",
        }}
      >
        <GlyphText glyph="◆">LOADING WORLD...</GlyphText>
      </div>
    </main>
  );
}
