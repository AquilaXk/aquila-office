package com.aquilaoffice.domain.model;

/**
 * 공급자 HTTP 호출 시 사용할 wire protocol 구분이다.
 */
public enum ModelWireProtocol {
    OPENAI_CHAT,
    OPENAI_RESPONSES,
    ANTHROPIC_MESSAGES,
    GOOGLE_GEMINI,
    OLLAMA_NATIVE
}
