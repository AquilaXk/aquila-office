"use client";

import Image from "next/image";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { ArrowRight } from "lucide-react";
import PixelAvatar from "@/components/arcade/PixelAvatar";
import { AppTopChrome } from "@/components/desktop/AppTopChrome";
import { BRAND_MASCOT_PATH, SERVICE_NAME } from "@/lib/branding";
import {
  AUTH_STORAGE_KEYS,
  hydrateDesktopAccessToken,
  isAuthenticated,
  readAuthUser,
  type AuthUser,
} from "@/lib/auth";
import { isEditableEventTarget } from "@/lib/workspace-shortcuts";

const MENU = [
  { label: "로그인", href: "/auth", action: "login" as const },
  { label: "워크스페이스로 이동", href: "/workspaces", action: "continue" as const },
];

export default function Home() {
  const router = useRouter();
  const [selected, setSelected] = useState(0);
  const [user, setUser] = useState<AuthUser | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const activate = useCallback(
    (item: (typeof MENU)[number]) => {
      if (item.action === "continue" && !isAuthenticated()) {
        setNotice("먼저 로그인을 완료해 주세요.");
        router.push("/");
        return;
      }
      setNotice(null);
      router.push(item.href);
    },
    [router],
  );

  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.defaultPrevented || event.metaKey || event.ctrlKey || event.altKey) {
        return;
      }
      if (isEditableEventTarget(event.target)) {
        return;
      }
      if (event.target instanceof Element) {
        const interactive = event.target.closest("header, nav, a, button");
        if (interactive && !interactive.classList.contains("title-menu-item")) {
          return;
        }
      }
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
    let ignore = false;
    const refreshUser = () => {
      if (!ignore) {
        setUser(readAuthUser());
      }
    };
    const onStorage = (event: StorageEvent) => {
      if (event.key === null || AUTH_STORAGE_KEYS.includes(event.key)) {
        refreshUser();
      }
    };

    void hydrateDesktopAccessToken().then(() => {
      refreshUser();
    });
    window.addEventListener("storage", onStorage);
    return () => {
      ignore = true;
      window.removeEventListener("storage", onStorage);
    };
  }, []);

  return (
    <AppTopChrome title="시작" showWorkspaceLinks={Boolean(user)}>
      <div className="title-screen flex-1">
        <section className="title-screen-panel">
          <div className="title-crest">
            <Image
              src={BRAND_MASCOT_PATH}
              alt={`${SERVICE_NAME} 로고`}
              width={88}
              height={88}
              priority
            />
          </div>
          <p className="eyebrow">협업형 AI 에이전트 워크스페이스</p>
          <h1>{SERVICE_NAME}</h1>
          <p className="title-screen-subtitle">
            에이전트 작업, 승인 검토, 산출물과 실시간 오피스를 한 화면에서 운영합니다.
          </p>
          <p className="title-screen-subtitle">
            현재 계정: {user ? `${user.name} (${user.email})` : "게스트 (로그인 필요)"}
          </p>

          <div className="ao-crew-stage-strip my-3" aria-label="오피스 크루 미리보기">
            <div className="ao-crew-tile">
              <PixelAvatar
                kind="alex"
                activity="idle"
                paletteOverride={{ gear: "headphones" }}
                size={2.2}
              />
              <span className="text-[12px] text-[var(--ui-text-muted)]">Lead</span>
            </div>
            <div className="ao-crew-tile">
              <PixelAvatar
                kind="mira"
                activity="typing"
                paletteOverride={{ gear: "beret" }}
                size={2.2}
              />
              <span className="text-[12px] text-[var(--ui-text-muted)]">Design</span>
            </div>
            <div className="ao-crew-tile">
              <PixelAvatar
                kind="kenji"
                activity="reading"
                paletteOverride={{ gear: "glasses" }}
                size={2.2}
              />
              <span className="text-[12px] text-[var(--ui-text-muted)]">Backend</span>
            </div>
            <div className="ao-crew-tile">
              <PixelAvatar
                kind="agent"
                activity="walk"
                variantKey="Ops Agent"
                agentCategory="ORCHESTRATOR"
                size={2.2}
              />
              <span className="text-[12px] text-[var(--ui-text-muted)]">Ops Agent</span>
            </div>
          </div>

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
                  <ArrowRight
                    size={15}
                    aria-hidden="true"
                    style={{ opacity: active ? 1 : 0 }}
                  />
                  <strong>{item.label}</strong>
                </button>
              );
            })}
          </div>

          {notice ? (
            <p className="title-screen-hint text-[var(--status-warning)]" role="status">
              {notice}
            </p>
          ) : (
            <p className="title-screen-hint">
              방향키로 메뉴를 이동하고 Enter 키로 선택할 수 있습니다.
            </p>
          )}
        </section>
      </div>
    </AppTopChrome>
  );
}
