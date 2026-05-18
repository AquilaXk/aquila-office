package com.aquilaoffice.adapter.out.gateway;

interface GatewayWebSocketListener {

    void onText(String message);

    void onError(Throwable error);
}
