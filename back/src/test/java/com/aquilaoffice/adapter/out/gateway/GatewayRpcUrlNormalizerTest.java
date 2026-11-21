package com.aquilaoffice.adapter.out.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import org.junit.jupiter.api.Test;

/**
 * Gateway HTTP 설정 URL을 WebSocket RPC endpoint로 정규화하는 규칙을 검증한다.
 */
class GatewayRpcUrlNormalizerTest {

    @Test
    void normalizesHttpsGatewayUrlToSecureWebSocketRpcEndpoint() {
        URI uri = GatewayRpcUrlNormalizer.normalize(" https://gateway.example.com/ ", "/rpc");

        assertThat(uri).isEqualTo(URI.create("wss://gateway.example.com/rpc"));
    }

    @Test
    void normalizesHttpGatewayUrlToWebSocketRpcEndpointAndPreservesPath() {
        URI uri = GatewayRpcUrlNormalizer.normalize("http://localhost:18080/gateway/", "rpc");

        assertThat(uri).isEqualTo(URI.create("ws://localhost:18080/gateway/rpc"));
    }

    @Test
    void preservesWebSocketSchemeAndDropsQueryAndFragment() {
        URI uri = GatewayRpcUrlNormalizer.normalize("wss://gateway.example.com/ws?token=secret#fragment", "/rpc");

        assertThat(uri).isEqualTo(URI.create("wss://gateway.example.com/ws/rpc"));
    }

    @Test
    void rejectsUnsupportedGatewayUrlScheme() {
        assertThatThrownBy(() -> GatewayRpcUrlNormalizer.normalize("ftp://gateway.example.com", "/rpc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("gatewayUrl must start with http://, https://, ws://, or wss://");
    }
}
