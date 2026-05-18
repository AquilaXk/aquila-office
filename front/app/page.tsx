"use client";

import Image from "next/image";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { BRAND_MASCOT_PATH, SERVICE_NAME } from "@/lib/branding";
import { isAuthenticated } from "@/lib/auth";

const MENU = [
  { label: "LOGIN", href: "/auth", action: "login" as const },
  { label: "CONTINUE", href: "/workspaces", action: "continue" as const },
];

export default function Home() {
  const router = useRouter();
  const [selected, setSelected] = useState(0);
  const [notice, setNotice] = useState("");

  const activate = useCallback((item: (typeof MENU)[number]) => {
    if (item.action === "continue" && !isAuthenticated()) {
      setNotice("로그인이 필요합니다. 인증 화면으로 이동합니다.");
      router.push("/auth?returnTo=/workspaces");
      return;
    }

    router.push(item.href);
  }, [router]);

  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "ArrowDown" || event.key === "ArrowRight") {
        event.preventDefault();
        setSelected((current) => (current + 1) % MENU.length);
        setNotice("");
      }
      if (event.key === "ArrowUp" || event.key === "ArrowLeft") {
        event.preventDefault();
        setSelected((current) => (current - 1 + MENU.length) % MENU.length);
        setNotice("");
      }
      if (event.key === "Enter") {
        event.preventDefault();
        activate(MENU[selected]);
      }
    };

    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [activate, selected]);

  return (
    <main className="title-screen" aria-label="타이틀 화면">
      <section className="title-screen-panel">
        <div className="title-crest">
          <Image
            src={BRAND_MASCOT_PATH}
            alt={`${SERVICE_NAME} crest`}
            width={88}
            height={88}
            priority
          />
        </div>
        <p className="eyebrow">PRESS START</p>
        <h1>{SERVICE_NAME.toUpperCase()}</h1>
        <p className="title-screen-subtitle">cooperative workspace operations</p>

        <div className="title-menu" role="menu" aria-label="시작 메뉴">
          {MENU.map((item, index) => {
            const active = selected === index;
            return (
              <button
                key={item.label}
                type="button"
                role="menuitem"
                className={active ? "title-menu-item title-menu-item-active" : "title-menu-item"}
                onMouseEnter={() => {
                  setSelected(index);
                  setNotice("");
                }}
                onClick={() => activate(item)}
              >
                <span aria-hidden="true">{active ? ">" : ""}</span>
                <strong>{item.label}</strong>
              </button>
            );
          })}
        </div>

        {notice ? (
          <p className="title-screen-notice" role="status">
            {notice}
          </p>
        ) : null}
        <p className="title-screen-hint">[ ↑↓ SELECT ] [ ENTER CONFIRM ]</p>
      </section>
    </main>
  );
}
