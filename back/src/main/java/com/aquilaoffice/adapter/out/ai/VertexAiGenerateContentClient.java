package com.aquilaoffice.adapter.out.ai;

import java.io.IOException;
import java.net.URI;

interface VertexAiGenerateContentClient {

    String generateContent(URI endpoint, String accessToken, String requestBody)
            throws IOException, InterruptedException;
}
