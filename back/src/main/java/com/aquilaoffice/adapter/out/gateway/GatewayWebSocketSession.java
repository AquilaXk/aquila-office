package com.aquilaoffice.adapter.out.gateway;

import java.util.concurrent.CompletableFuture;

interface GatewayWebSocketSession {

    CompletableFuture<Void> sendText(String message);

    CompletableFuture<Void> close();
}
