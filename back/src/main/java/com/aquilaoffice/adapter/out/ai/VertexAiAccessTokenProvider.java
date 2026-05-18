package com.aquilaoffice.adapter.out.ai;

import java.io.IOException;

@FunctionalInterface
interface VertexAiAccessTokenProvider {

    String accessToken() throws IOException;
}
