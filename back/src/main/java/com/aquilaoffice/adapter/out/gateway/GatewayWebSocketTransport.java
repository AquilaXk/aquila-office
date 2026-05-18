package com.aquilaoffice.adapter.out.gateway;

import java.net.URI;

interface GatewayWebSocketTransport {

    GatewayWebSocketSession connect(URI uri, GatewayWebSocketListener listener);
}
