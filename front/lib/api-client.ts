export const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

type ApiEnvelope<T> = {
  success: boolean;
  code: string;
  message: string;
  data: T;
};

type ApiFetchOptions = RequestInit & {
  accessToken?: string;
};

export class ApiError extends Error {
  public readonly status: number;
  public readonly details?: unknown;

  constructor(
    status: number,
    message: string,
    details?: unknown,
  ) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.details = details;
  }
}

export async function apiFetch<T>(
  path: string,
  options: ApiFetchOptions = {},
): Promise<T> {
  const { accessToken, headers, ...requestOptions } = options;
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...requestOptions,
    headers: buildHeaders(headers, accessToken),
  });
  const body = await readJson(response);

  if (!response.ok) {
    throw new ApiError(response.status, messageFrom(body, response.status), body);
  }

  if (isEnvelope<T>(body)) {
    if (!body.success) {
      throw new ApiError(response.status, body.message, body);
    }

    return body.data;
  }

  return body as T;
}

function buildHeaders(headers: HeadersInit | undefined, accessToken: string | undefined): Headers {
  const requestHeaders = new Headers(headers);
  if (!requestHeaders.has("Accept")) {
    requestHeaders.set("Accept", "application/json");
  }

  const normalizedAccessToken = accessToken?.trim();
  if (normalizedAccessToken) {
    requestHeaders.set("Authorization", `Bearer ${normalizedAccessToken}`);
  }

  return requestHeaders;
}

async function readJson(response: Response): Promise<unknown> {
  const text = await response.text();
  if (!text) {
    return null;
  }

  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}

function isEnvelope<T>(body: unknown): body is ApiEnvelope<T> {
  return (
    typeof body === "object" &&
    body !== null &&
    "success" in body &&
    "message" in body &&
    "data" in body
  );
}

function messageFrom(body: unknown, status: number): string {
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
