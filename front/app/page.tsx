"use client";

import Image from "next/image";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { BRAND_MASCOT_PATH, SERVICE_NAME } from "@/lib/branding";
import { AUTH_STORAGE_KEYS, isAuthenticated, readAuthUser, type AuthUser } from "@/lib/auth";

const MENU = [
  { label: "LOGIN", href: "/auth", action: "login" as const },
  { label: "CONTINUE", href: "/workspaces", action: "continue" as const },
];

export default function Home() {
  const router = useRouter();
  const [selected, setSelected] = useState(0);
  const [user, setUser] = useState<AuthUser | null>(null);

  const activate = useCallback((item: (typeof MENU)[number]) => {
    if (item.action === "continue" && !isAuthenticated()) {
      router.push("/");
      return;
    }

    router.push(item.href);
  }, [router]);

  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "ArrowDown" || event.key === "ArrowRight") {
        event.preventDefault();
        setSelected((current) => (current + 1) % MENU.length);
      }
      if (event.key === "ArrowUp" || event.key === "ArrowLeft") {
        event.preventDefault();
        setSelected((current) => (current - 1 + MENU.length) % MENU.length);
      }
      if (event.key === "Enter") {
        event.preventDefault();
        activate(MENU[selected]);
      }
    };

    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [activate, selected]);

  useEffect(() => {
    const refreshUser = () => setUser(readAuthUser());
    const onStorage = (event: StorageEvent) => {
      if (event.key === null || AUTH_STORAGE_KEYS.includes(event.key)) {
        refreshUser();
      }
    };

    refreshUser();
    window.addEventListener("storage", onStorage);
    return () => window.removeEventListener("storage", onStorage);
  }, []);

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
        <p className="title-screen-subtitle">USER {user?.name ?? "MEMBER"}</p>

        <div className="title-menu" role="menu" aria-label="시작 메뉴">
          {MENU.map((item, index) => {
            const active = selected === index;
            return (
              <button
                key={item.label}
                type="button"
                role="menuitem"
                className={active ? "title-menu-item title-menu-item-active" : "title-menu-item"}
                onMouseEnter={() => setSelected(index)}
                onClick={() => activate(item)}
              >
                <span aria-hidden="true">{active ? ">" : ""}</span>
                <strong>{item.label}</strong>
              </button>
            );
          })}
        </div>

        <p className="title-screen-hint">[ ↑↓ SELECT ] [ ENTER CONFIRM ]</p>
      </section>
    </main>
  );
}
