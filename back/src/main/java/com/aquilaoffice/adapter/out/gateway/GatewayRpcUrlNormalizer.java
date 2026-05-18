package com.aquilaoffice.adapter.out.gateway;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * 사용자가 저장한 Gateway URL을 WebSocket RPC endpoint로 변환한다.
 */
final class GatewayRpcUrlNormalizer {

    private static final String UNSUPPORTED_SCHEME_MESSAGE =
            "gatewayUrl must start with http://, https://, ws://, or wss://";

    private GatewayRpcUrlNormalizer() {}

    static URI normalize(String gatewayUrl, String rpcPath) {
        URI sourceUri = parseGatewayUrl(gatewayUrl);
        String scheme = normalizeScheme(sourceUri.getScheme());
        String host = sourceUri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("gatewayUrl host must not be blank");
        }
        String endpointPath = normalizeBasePath(sourceUri.getPath()) + normalizeRpcPath(rpcPath);
        try {
            return new URI(scheme, null, host, sourceUri.getPort(), endpointPath, null, null);
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("gatewayUrl is invalid", exception);
        }
    }

    private static URI parseGatewayUrl(String gatewayUrl) {
        String normalizedUrl = requireNotBlank(gatewayUrl, "gatewayUrl");
        try {
            return new URI(normalizedUrl);
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("gatewayUrl is invalid", exception);
        }
    }

    private static String normalizeScheme(String scheme) {
        if (scheme == null || scheme.isBlank()) {
            throw new IllegalArgumentException(UNSUPPORTED_SCHEME_MESSAGE);
        }
        return switch (scheme.toLowerCase()) {
            case "https" -> "wss";
            case "http" -> "ws";
            case "wss", "ws" -> scheme.toLowerCase();
            default -> throw new IllegalArgumentException(UNSUPPORTED_SCHEME_MESSAGE);
        };
    }

    private static String normalizeBasePath(String path) {
        if (path == null || path.isBlank() || "/".equals(path)) {
            return "";
        }
        String normalizedPath = path.trim();
        while (normalizedPath.length() > 1 && normalizedPath.endsWith("/")) {
            normalizedPath = normalizedPath.substring(0, normalizedPath.length() - 1);
        }
        if (!normalizedPath.startsWith("/")) {
            return "/" + normalizedPath;
        }
        return normalizedPath;
    }

    private static String normalizeRpcPath(String rpcPath) {
        String normalizedPath = requireNotBlank(rpcPath, "rpcPath");
        if (!normalizedPath.startsWith("/")) {
            normalizedPath = "/" + normalizedPath;
        }
        while (normalizedPath.length() > 1 && normalizedPath.endsWith("/")) {
            normalizedPath = normalizedPath.substring(0, normalizedPath.length() - 1);
        }
        return normalizedPath;
    }

    private static String requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
