import { isApiEnvelope, messageFromApiBody } from "./envelope.ts";
import { ApiError } from "./errors.ts";
import type { ApiFetchOptions } from "./types.ts";

export const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";
export const MAX_JSON_RESPONSE_CHARS = 8 * 1024 * 1024;

export type UnauthorizedRecoveryHandler = (failedAccessToken: string) => Promise<string | null>;

let unauthorizedRecoveryHandler: UnauthorizedRecoveryHandler | null = null;

export function configureUnauthorizedRecovery(handler: UnauthorizedRecoveryHandler | null): void {
  unauthorizedRecoveryHandler = handler;
}

export async function apiFetch<T>(
  path: string,
  options: ApiFetchOptions = {},
): Promise<T> {
  const {
    accessToken,
    baseUrl = API_BASE_URL,
    headers,
    json,
    body,
    skipAuthRecovery = false,
    ...requestOptions
  } = options;
  const normalizedAccessToken = accessToken?.trim();
  const response = await sendApiRequest(path, {
    accessToken: normalizedAccessToken,
    baseUrl,
    headers,
    json,
    body,
    requestOptions,
  });

  if (response.status === 401 && normalizedAccessToken && !skipAuthRecovery && unauthorizedRecoveryHandler) {
    const recoveredAccessToken = await unauthorizedRecoveryHandler(normalizedAccessToken);
    if (recoveredAccessToken) {
      return parseApiResponse(await sendApiRequest(path, {
        accessToken: recoveredAccessToken,
        baseUrl,
        headers,
        json,
        body,
        requestOptions,
      }));
    }
  }

  return parseApiResponse(response);
}

type SendApiRequestOptions = {
  accessToken?: string;
  baseUrl: string;
  headers?: HeadersInit;
  json?: unknown;
  body?: BodyInit | null;
  requestOptions: RequestInit;
};

async function sendApiRequest(
  path: string,
  options: SendApiRequestOptions,
): Promise<Response> {
  const hasJsonBody = options.json !== undefined;
  return fetch(apiUrl(options.baseUrl, path), {
    ...options.requestOptions,
    headers: buildApiHeaders(options.headers, options.accessToken, hasJsonBody),
    body: hasJsonBody ? JSON.stringify(options.json) : options.body,
  });
}

async function parseApiResponse<T>(response: Response): Promise<T> {
  const responseBody = await readJson(response);

  if (!response.ok) {
    throw new ApiError(response.status, messageFromApiBody(responseBody, response.status), responseBody);
  }

  if (isApiEnvelope<T>(responseBody)) {
    if (!responseBody.success) {
      throw new ApiError(response.status, responseBody.message, responseBody);
    }

    return responseBody.data;
  }

  return responseBody as T;
}

export function apiUrl(baseUrl: string, path: string): string {
  if (path.startsWith("http://") || path.startsWith("https://")) {
    return path;
  }

  const normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.slice(0, -1) : baseUrl;
  const normalizedPath = path.startsWith("/") ? path : `/${path}`;
  return `${normalizedBaseUrl}${normalizedPath}`;
}

export function buildApiHeaders(
  headers: HeadersInit | undefined,
  accessToken: string | undefined,
  hasJsonBody = false,
): Headers {
  const requestHeaders = new Headers(headers);
  if (!requestHeaders.has("Accept")) {
    requestHeaders.set("Accept", "application/json");
  }
  if (hasJsonBody && !requestHeaders.has("Content-Type")) {
    requestHeaders.set("Content-Type", "application/json");
  }

  const normalizedAccessToken = accessToken?.trim();
  if (normalizedAccessToken) {
    requestHeaders.set("Authorization", `Bearer ${normalizedAccessToken}`);
  }

  return requestHeaders;
}

async function readBoundedResponseText(response: Response): Promise<string> {
  const contentLengthHeader = response.headers.get("Content-Length");
  if (contentLengthHeader) {
    const declaredBytes = Number(contentLengthHeader);
    // Reject immediately when Content-Length exceeds worst-case UTF-8 byte budget for MAX_JSON_RESPONSE_CHARS
    if (Number.isFinite(declaredBytes) && declaredBytes > MAX_JSON_RESPONSE_CHARS * 4) {
      throw new ApiError(
        response.status || 413,
        "응답 크기가 허용 상한(8MB)을 초과했습니다.",
        null,
      );
    }
  }

  if (!response.body || typeof response.body.getReader !== "function") {
    const text = await response.text();
    if (text.length > MAX_JSON_RESPONSE_CHARS) {
      throw new ApiError(
        response.status || 413,
        "응답 크기가 허용 상한(8MB)을 초과했습니다.",
        null,
      );
    }
    return text;
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder("utf-8", { fatal: false });
  const chunks: string[] = [];
  let totalChars = 0;

  try {
    while (true) {
      const { done, value } = await reader.read();
      if (done) {
        break;
      }
      if (value && value.byteLength > 0) {
        const piece = decoder.decode(value, { stream: true });
        totalChars += piece.length;
        if (totalChars > MAX_JSON_RESPONSE_CHARS) {
          await reader.cancel().catch(() => {});
          throw new ApiError(
            response.status || 413,
            "응답 크기가 허용 상한(8MB)을 초과했습니다.",
            null,
          );
        }
        chunks.push(piece);
      }
    }
    const tail = decoder.decode();
    if (tail) {
      totalChars += tail.length;
      if (totalChars > MAX_JSON_RESPONSE_CHARS) {
        throw new ApiError(
          response.status || 413,
          "응답 크기가 허용 상한(8MB)을 초과했습니다.",
          null,
        );
      }
      chunks.push(tail);
    }
  } finally {
    reader.releaseLock();
  }

  return chunks.join("");
}

async function readJson(response: Response): Promise<unknown> {
  const text = await readBoundedResponseText(response);
  if (!text) {
    return null;
  }

  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}
