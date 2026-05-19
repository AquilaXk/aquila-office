"use client";

import { AlertCircle, ArrowLeft, KeyRound, Plus, ServerCog } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { type FormEvent, useEffect, useMemo, useState } from "react";
import { readAuthTokens } from "@/lib/auth";
import {
  maskGatewayToken,
  normalizeGatewayRegistryForm,
  OPENCLAW_GATEWAYS_STORAGE_KEY,
} from "@/lib/gateway-utils";

type GatewayRegistryEntry = {
  id: string;
  displayName: string;
  gatewayUrl: string;
  maskedToken: string;
  createdAt: string;
};

const DISPLAY_NAME_MAX_LENGTH = 80;
const URL_MAX_LENGTH = 300;

export default function GatewayRoutePage() {
  const router = useRouter();
  const [hydrated, setHydrated] = useState(false);
  const [gateways, setGateways] = useState<GatewayRegistryEntry[]>([]);
  const [selectedId, setSelectedId] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [gatewayUrl, setGatewayUrl] = useState("");
  const [token, setToken] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [createdMessage, setCreatedMessage] = useState("");

  useEffect(() => {
    let cancelled = false;
    queueMicrotask(() => {
      if (cancelled) {
        return;
      }

      if (!readAuthTokens()) {
        router.replace("/");
        return;
      }

      const storedGateways = readGatewayRegistry();
      setGateways(storedGateways);
      setSelectedId(storedGateways[0]?.id ?? "");
      setHydrated(true);
    });

    return () => {
      cancelled = true;
    };
  }, [router]);

  const selectedGateway = useMemo(
    () => gateways.find((gateway) => gateway.id === selectedId) ?? gateways[0] ?? null,
    [gateways, selectedId],
  );
  const normalizedForm = useMemo(
    () => normalizeGatewayRegistryForm({ displayName, gatewayUrl, token }),
    [displayName, gatewayUrl, token],
  );
  const canSubmit =
    !submitting &&
    displayName.trim().length > 0 &&
    gatewayUrl.trim().length > 0 &&
    token.trim().length > 0;

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (!normalizedForm.isValid) {
      setError(
        normalizedForm.displayNameError ??
          normalizedForm.urlError ??
          normalizedForm.tokenError ??
          "Gateway 입력값을 확인해 주세요.",
      );
      setCreatedMessage("");
      return;
    }

    setSubmitting(true);
    setError("");
    const nextGateway: GatewayRegistryEntry = {
      id: createGatewayId(),
      displayName: normalizedForm.displayName,
      gatewayUrl: normalizedForm.gatewayUrl,
      maskedToken: maskGatewayToken(normalizedForm.token),
      createdAt: new Date().toISOString(),
    };

    writeGatewayRegistry([nextGateway, ...gateways]);
    setGateways([nextGateway, ...gateways]);
    setSelectedId(nextGateway.id);
    setDisplayName("");
    setGatewayUrl("");
    setToken("");
    setCreatedMessage("WARP GATE LINKED.");
    setSubmitting(false);
  };

  if (!hydrated) {
    return null;
  }

  return (
    <main className="workspace-create-shell workspace-gateway-registry-shell">
      <section className="workspace-create-panel" aria-label="Gateway registry">
        <div className="workspace-create-header">
          <Link className="secondary-action" href="/workspaces">
            <ArrowLeft aria-hidden="true" />
            <span>BACK TO WORKSPACES</span>
          </Link>
          <div className="workspace-create-title">
            <div className="workspace-create-icon">
              <ServerCog aria-hidden="true" />
            </div>
            <div>
              <p className="eyebrow">OPENCLAW · WARP NET</p>
              <h1>REGISTER A GATEWAY</h1>
              <p className="topbar-summary">
                워크스페이스에서 에이전트를 소환할 수 있도록 OpenClaw gateway를 연결해 주세요.
              </p>
            </div>
          </div>
        </div>

        <div className="workspace-gateway-registry">
          <aside className="workspace-gateway-sidebar">
            <section className="workspace-section" aria-label="Linked gateways">
              <div className="section-header">
                <div>
                  <p className="eyebrow">Registry</p>
                  <h2>{`LINKED · ${String(gateways.length).padStart(2, "0")}`}</h2>
                </div>
              </div>

              {gateways.length === 0 ? (
                <div className="state-panel">
                  <ServerCog aria-hidden="true" />
                  <strong>아직 연결된 게이트웨이가 없습니다</strong>
                  <p>display name, URL, token을 입력해 첫 gateway를 등록하세요.</p>
                </div>
              ) : (
                <div className="workspace-list">
                  {gateways.map((gateway) => {
                    const selected = selectedGateway?.id === gateway.id;
                    return (
                      <button
                        aria-pressed={selected}
                        className="workspace-row workspace-row-link"
                        key={gateway.id}
                        onClick={() => setSelectedId(gateway.id)}
                        type="button"
                      >
                        <div className="workspace-row-icon">
                          <ServerCog aria-hidden="true" />
                        </div>
                        <div className="workspace-row-main">
                          <strong>{gateway.displayName.toUpperCase()}</strong>
                          <p>{gateway.gatewayUrl}</p>
                          <p>{formatDateTime(gateway.createdAt)}</p>
                        </div>
                        <div className="workspace-row-meta">
                          <span className="role-badge">ON</span>
                        </div>
                      </button>
                    );
                  })}
                </div>
              )}
            </section>

            {selectedGateway ? (
              <section className="workspace-section" aria-label="ACTIVE LINK">
                <div className="section-header">
                  <div>
                    <p className="eyebrow">ACTIVE LINK</p>
                    <h2>{selectedGateway.displayName.toUpperCase()}</h2>
                  </div>
                </div>
                <div className="workspace-gateway-active-link">
                  <span>TARGET</span>
                  <strong>{selectedGateway.displayName.toUpperCase()}</strong>
                  <p>{`TOKEN · ${selectedGateway.maskedToken}`}</p>
                </div>
              </section>
            ) : null}
          </aside>

          <section className="workspace-section" aria-label="GATE FORM">
            <div className="section-header">
              <div>
                <p className="eyebrow">Gate form</p>
                <h2>Gateway 등록</h2>
              </div>
            </div>
            <form className="workspace-create-form" onSubmit={handleSubmit}>
              <label className="form-field">
                <span>
                  <strong>DISPLAY NAME</strong>
                  <small>{displayName.length}/{DISPLAY_NAME_MAX_LENGTH}</small>
                </span>
                <input
                  autoFocus
                  disabled={submitting}
                  maxLength={DISPLAY_NAME_MAX_LENGTH}
                  onChange={(event) => {
                    setDisplayName(event.target.value);
                    setError("");
                    setCreatedMessage("");
                  }}
                  placeholder="예: 로컬 OpenClaw 게이트웨이"
                  value={displayName}
                />
              </label>

              <label className="form-field">
                <span>
                  <strong>GATEWAY URL</strong>
                  <small>{gatewayUrl.length}/{URL_MAX_LENGTH}</small>
                </span>
                <input
                  disabled={submitting}
                  maxLength={URL_MAX_LENGTH}
                  onChange={(event) => {
                    setGatewayUrl(event.target.value);
                    setError("");
                    setCreatedMessage("");
                  }}
                  placeholder="http://localhost:4317"
                  value={gatewayUrl}
                />
              </label>

              <label className="form-field">
                <span>
                  <strong>TOKEN</strong>
                  <small>저장 후 masked token만 표시됩니다</small>
                </span>
                <input
                  autoComplete="off"
                  disabled={submitting}
                  onChange={(event) => {
                    setToken(event.target.value);
                    setError("");
                    setCreatedMessage("");
                  }}
                  placeholder="OpenClaw gateway token"
                  type="password"
                  value={token}
                />
              </label>

              {error ? (
                <div className="form-notice form-notice-error" role="alert">
                  <AlertCircle aria-hidden="true" />
                  <span>{error}</span>
                </div>
              ) : null}

              {createdMessage ? (
                <div className="workspace-task-notice" role="status">
                  <Plus aria-hidden="true" />
                  <span>{createdMessage}</span>
                </div>
              ) : null}

              <div className="form-actions">
                <Link className="secondary-action" href="/workspaces">
                  CANCEL
                </Link>
                <button
                  className="primary-action primary-action-active"
                  disabled={!canSubmit}
                  type="submit"
                >
                  <KeyRound aria-hidden="true" />
                  <span>{submitting ? "LINKING" : "LINK GATE"}</span>
                </button>
              </div>
            </form>
          </section>
        </div>

        <div className="workspace-gateway-info-grid">
          <InfoTile title="REGISTER" value="라벨, gateway URL, token을 입력합니다." />
          <InfoTile title="VERIFY" value="linked list에서 등록된 gateway를 확인합니다." />
          <InfoTile title="BIND" value="워크스페이스별 binding은 workspace 내부에서 수행합니다." />
        </div>
      </section>
    </main>
  );
}

function InfoTile({ title, value }: { title: string; value: string }) {
  return (
    <div className="workspace-stat-card">
      <span>{title}</span>
      <strong>{value}</strong>
    </div>
  );
}

function readGatewayRegistry(): GatewayRegistryEntry[] {
  if (typeof window === "undefined") {
    return [];
  }

  const rawValue = window.localStorage.getItem(OPENCLAW_GATEWAYS_STORAGE_KEY);
  if (!rawValue) {
    return [];
  }

  try {
    const parsed = JSON.parse(rawValue);
    return Array.isArray(parsed) ? parsed.filter(isGatewayRegistryEntry) : [];
  } catch {
    return [];
  }
}

function writeGatewayRegistry(gateways: GatewayRegistryEntry[]): void {
  if (typeof window === "undefined") {
    return;
  }

  window.localStorage.setItem(OPENCLAW_GATEWAYS_STORAGE_KEY, JSON.stringify(gateways));
}

function isGatewayRegistryEntry(value: unknown): value is GatewayRegistryEntry {
  if (!value || typeof value !== "object") {
    return false;
  }

  const record = value as Record<string, unknown>;
  return (
    typeof record.id === "string" &&
    typeof record.displayName === "string" &&
    typeof record.gatewayUrl === "string" &&
    typeof record.maskedToken === "string" &&
    typeof record.createdAt === "string"
  );
}

function createGatewayId(): string {
  if (typeof window !== "undefined" && window.crypto?.randomUUID) {
    return window.crypto.randomUUID();
  }

  return `${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

function formatDateTime(value: string): string {
  try {
    return new Intl.DateTimeFormat("ko-KR", {
      dateStyle: "medium",
      timeStyle: "short",
    }).format(new Date(value));
  } catch {
    return value;
  }
}
