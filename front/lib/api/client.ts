import { isApiEnvelope, messageFromApiBody } from "./envelope.ts";
import { ApiError } from "./errors.ts";
import type { ApiFetchOptions } from "./types.ts";

export const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

export async function apiFetch<T>(
  path: string,
  options: ApiFetchOptions = {},
): Promise<T> {
  const { accessToken, baseUrl = API_BASE_URL, headers, json, body, ...requestOptions } = options;
  const hasJsonBody = json !== undefined;
  const response = await fetch(apiUrl(baseUrl, path), {
    ...requestOptions,
    headers: buildApiHeaders(headers, accessToken, hasJsonBody),
    body: hasJsonBody ? JSON.stringify(json) : body,
  });
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
