export type GatewayBindingStatus = "UNBOUND" | "BOUND";

export type GatewayConnectionStatus =
  | "CONNECTED"
  | "UNREACHABLE"
  | "TOKEN_INVALID"
  | "TIMEOUT"
  | "PAIRING_REQUIRED"
  | "FORBIDDEN"
  | "FAILED";

export type GatewayFormValues = {
  gatewayUrl: string;
  token: string;
  validateConnection?: boolean;
};

export type NormalizedGatewayForm = {
  gatewayUrl: string;
  token: string;
  validateConnection: boolean;
  urlError: string | null;
  tokenError: string | null;
  isValid: boolean;
};

export function normalizeGatewayForm(values: GatewayFormValues): NormalizedGatewayForm {
  const gatewayUrl = values.gatewayUrl.trim();
  const token = values.token.trim();
  const urlError =
    gatewayUrl.length === 0
      ? "gateway URL을 입력해 주세요."
      : isGatewayUrl(gatewayUrl)
        ? null
        : "gateway URL은 http:// 또는 https:// 로 시작해야 합니다.";
  const tokenError = token.length === 0 ? "gateway token을 입력해 주세요." : null;

  return {
    gatewayUrl,
    token,
    validateConnection: values.validateConnection ?? true,
    urlError,
    tokenError,
    isValid: urlError === null && tokenError === null,
  };
}

export function isGatewayUrl(value: string): boolean {
  try {
    const url = new URL(value);
    return url.protocol === "http:" || url.protocol === "https:";
  } catch {
    return false;
  }
}

export function maskGatewayToken(token: string): string {
  if (token.length <= 8) {
    return "****";
  }

  return `${token.slice(0, 4)}****${token.slice(-4)}`;
}

export function gatewayStatusLabel(status: GatewayBindingStatus): string {
  return status === "BOUND" ? "연결됨" : "미연결";
}

export function gatewayConnectionStatusLabel(status: GatewayConnectionStatus): string {
  switch (status) {
    case "CONNECTED":
      return "연결 가능";
    case "UNREACHABLE":
      return "접속 불가";
    case "TOKEN_INVALID":
      return "토큰 오류";
    case "TIMEOUT":
      return "시간 초과";
    case "PAIRING_REQUIRED":
      return "페어링 필요";
    case "FORBIDDEN":
      return "권한 없음";
    case "FAILED":
      return "실패";
  }
}
