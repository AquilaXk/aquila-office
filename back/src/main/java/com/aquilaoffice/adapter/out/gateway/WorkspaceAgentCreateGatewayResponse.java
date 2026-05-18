package com.aquilaoffice.adapter.out.gateway;

record WorkspaceAgentCreateGatewayResponse(
        boolean success,
        String openClawAgentId,
        String failureReason) {}
