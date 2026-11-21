"use client";

import { GoogleLogin } from "@react-oauth/google";
import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useEffect, useState } from "react";
import { AlertCircle, Loader2, ShieldCheck } from "lucide-react";
import { AppTopChrome } from "@/components/desktop/AppTopChrome";
import type { ApiError } from "@/lib/api-client";
import { isAuthenticated, loginWithGoogle } from "@/lib/auth";

export default function AuthPage() {
  return (
    <Suspense fallback={null}>
      <AuthPageInner />
    </Suspense>
  );
}

function AuthPageInner() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const returnTo = safeReturnTo(searchParams.get("returnTo"));
  const [error, setError] = useState<string>("");
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (isAuthenticated()) {
      router.replace(nextStep(returnTo));
    }
  }, [router, returnTo]);

  async function handleSuccess(idToken?: string) {
    if (!idToken) {
      setError("Google 인증 토큰을 받지 못했습니다.");
      return;
    }
    setError("");
    setSubmitting(true);
    try {
      await loginWithGoogle(idToken);
      router.replace(nextStep(returnTo));
    } catch (err) {
      const apiErr = err as ApiError;
      setError(apiErr?.message ?? "로그인에 실패했습니다.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <AppTopChrome title="로그인" backHref="/" backLabel="시작 화면" showWorkspaceLinks={false}>
      <div className="ao-page-container ao-page-container-narrow">
        <div className="text-center">
          <p className="ao-page-eyebrow">계정 인증</p>
          <h1 className="ao-page-title">Google 계정으로 로그인</h1>
          <p className="ao-page-subtitle">
            워크스페이스 구성원 확인을 위해 Google 계정 인증을 진행합니다.
          </p>
        </div>

        <section className="ao-surface-panel flex flex-col gap-5" aria-label="Google 로그인">
          <div className="flex items-start gap-3">
            <div className="w-9 h-9 rounded-[var(--radius-md)] border border-[var(--ui-border)] bg-[var(--ui-surface-raised)] flex items-center justify-center shrink-0">
              <ShieldCheck size={18} aria-hidden="true" />
            </div>
            <div>
              <h2 className="m-0 text-[15px] font-semibold text-[var(--ui-text-strong)]">
                워크스페이스 계정 인증
              </h2>
              <p className="m-0 mt-1 text-[13px] text-[var(--ui-text-secondary)]">
                초대 링크를 받은 계정이거나 새 워크스페이스를 개설할 계정으로 로그인하세요.
              </p>
            </div>
          </div>

          <div className="flex flex-col items-center gap-3 py-2">
            <div className="auth-google-frame">
              <GoogleLogin
                onSuccess={(credResp) => void handleSuccess(credResp.credential)}
                onError={() => setError("Google 로그인이 취소되었거나 실패했습니다.")}
                theme="filled_black"
                size="large"
                shape="rectangular"
                text="continue_with"
                logo_alignment="left"
                width="280"
                useOneTap={false}
                containerProps={{
                  style: {
                    display: "inline-grid",
                    width: 284,
                    height: 48,
                    minHeight: 48,
                    placeItems: "center",
                    overflow: "hidden",
                    border: "1px solid rgba(230, 234, 240, 0.14)",
                    borderRadius: 8,
                    background: "#202124",
                    colorScheme: "dark",
                    lineHeight: 0,
                    padding: 0,
                  },
                }}
              />
              <span className="auth-google-visible-label" aria-hidden="true">
                Google 계정으로 계속하기
              </span>
            </div>
          </div>

          {error ? (
            <div className="ao-inline-notice ao-inline-notice-danger" role="alert">
              <AlertCircle size={15} aria-hidden="true" />
              <span>{error}</span>
            </div>
          ) : null}

          {submitting ? (
            <div className="ao-inline-notice" role="status">
              <Loader2 size={15} className="spin" aria-hidden="true" />
              <span>계정 정보를 확인하고 있습니다.</span>
            </div>
          ) : null}
        </section>
      </div>
    </AppTopChrome>
  );
}

function safeReturnTo(raw: string | null): string {
  if (!raw) return "/workspaces";
  if (!raw.startsWith("/") || raw.startsWith("//")) return "/workspaces";
  return raw;
}

function nextStep(returnTo: string): string {
  const params = new URLSearchParams({ returnTo });
  return `/auth/character?${params.toString()}`;
}
