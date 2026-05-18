import type { ApiEnvelope } from "./types.ts";

export function isApiEnvelope<T>(body: unknown): body is ApiEnvelope<T> {
  return (
    typeof body === "object" &&
    body !== null &&
    "success" in body &&
    "message" in body &&
    "data" in body
  );
}

export function messageFromApiBody(body: unknown, status: number): string {
  if (typeof body === "object" && body !== null && "message" in body) {
    const message = (body as { message?: unknown }).message;
    if (typeof message === "string" && message.length > 0) {
      return message;
    }
  }

  if (status === 401) {
    return "로그인이 필요합니다.";
  }

  if (status >= 500) {
    return "서버 오류가 발생했습니다.";
  }

  return "요청을 처리하지 못했습니다.";
}
