"use client";

import { GoogleLogin } from "@react-oauth/google";
import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useEffect, useState } from "react";
import { isAuthenticated, loginWithGoogle } from "@/lib/auth";
import type { ApiError } from "@/lib/api-client";
import { GlyphText, T4Screen, T4Panel } from "@/components/arcade";
import { t4 } from "@/components/arcade/tokens";

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
    if (isAuthenticated()) router.replace(nextStep(returnTo));
  }, [router, returnTo]);

  async function handleSuccess(idToken?: string) {
    if (!idToken) {
      setError("Google 토큰을 받지 못했습니다.");
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
    <T4Screen title="SIGN IN">
      <div
        style={{
          padding: "24px 28px",
          height: "100%",
          boxSizing: "border-box",
          display: "flex",
          flexDirection: "column",
          gap: 18,
          justifyContent: "center",
        }}
      >
        <div style={{ textAlign: "center", marginBottom: 8 }}>
          <div
            style={{
              fontFamily: "var(--font-sans)",
              fontSize: 18,
              fontWeight: 600,
              color: t4.ink,
              letterSpacing: "-0.01em",
              marginBottom: 6,
            }}
          >
            <GlyphText glyph="▶">SIGN IN TO CONTINUE</GlyphText>
          </div>
          <div
            style={{
              fontFamily: "var(--font-sans)",
              fontSize: 13,
              color: t4.dim,
            }}
          >
            Authenticate with your workspace Google account
          </div>
        </div>

        <div style={{ maxWidth: 460, width: "100%", margin: "0 auto" }}>
          <T4Panel
            label="SIGN IN"
            style={{ padding: "22px 24px", position: "relative" }}
          >
            <div
              style={{
                fontFamily: "var(--font-sans)",
                fontSize: 13,
                fontWeight: 600,
                color: t4.ink,
                marginBottom: 6,
              }}
            >
              <GlyphText glyph="◆">WORKSPACE MEMBER VERIFICATION</GlyphText>
            </div>
            <div
              style={{
                fontFamily: "var(--font-sans)",
                fontSize: 13,
                color: t4.dim,
                lineHeight: 1.55,
                marginBottom: 20,
              }}
            >
              Sign in with your work Google account.
              <br />
              Only invited members may enter.
            </div>

            <div
              style={{
                display: "flex",
                flexDirection: "column",
                alignItems: "center",
                gap: 10,
              }}
            >
              <div className="auth-google-frame">
                <GoogleLogin
                  onSuccess={(credResp) => handleSuccess(credResp.credential)}
                  onError={() =>
                    setError("Google 로그인이 취소되었거나 실패했습니다.")
                  }
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
                  CONTINUE WITH GOOGLE
                </span>
              </div>
            </div>

            {error && (
              <div
                style={{
                  marginTop: 14,
                  fontFamily: "var(--font-sans)",
                  fontSize: 12,
                  color: t4.hp,
                  textAlign: "center",
                }}
              >
                <GlyphText glyph="⚠">{error}</GlyphText>
              </div>
            )}
            {submitting && (
              <div
                style={{
                  marginTop: 14,
                  fontFamily: "var(--font-sans)",
                  fontSize: 12,
                  color: t4.mp,
                  textAlign: "center",
                }}
              >
                LOADING ACCOUNT...
              </div>
            )}
          </T4Panel>
        </div>

        <div
          style={{
            position: "absolute",
            bottom: 16,
            left: 0,
            right: 0,
            textAlign: "center",
            fontFamily: "var(--font-sans)",
            fontSize: 12,
            color: t4.mute,
          }}
        >
          Aquila Office
        </div>
      </div>
    </T4Screen>
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
