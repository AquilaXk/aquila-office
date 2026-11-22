package com.aquilaoffice.domain.model;

/**
 * 워크스페이스에서 등록 가능한 AI 모델 공급자 유형이다.
 */
public enum ModelProviderKind {
    OPENAI(true, false, ModelWireProtocol.OPENAI_RESPONSES),
    ANTHROPIC(true, false, ModelWireProtocol.ANTHROPIC_MESSAGES),
    GOOGLE(true, false, ModelWireProtocol.GOOGLE_GEMINI),
    OPENROUTER(true, false, ModelWireProtocol.OPENAI_CHAT),
    DEEPSEEK(true, false, ModelWireProtocol.OPENAI_CHAT),
    XAI(true, false, ModelWireProtocol.OPENAI_CHAT),
    GROQ(true, false, ModelWireProtocol.OPENAI_CHAT),
    OLLAMA(false, true, ModelWireProtocol.OLLAMA_NATIVE),
    CUSTOM_OPENAI(false, false, ModelWireProtocol.OPENAI_CHAT);

    private final boolean apiKeyRequired;
    private final boolean localHostProvider;
    private final ModelWireProtocol defaultProtocol;

    ModelProviderKind(boolean apiKeyRequired, boolean localHostProvider, ModelWireProtocol defaultProtocol) {
        this.apiKeyRequired = apiKeyRequired;
        this.localHostProvider = localHostProvider;
        this.defaultProtocol = defaultProtocol;
    }

    public boolean requiresApiKey() {
        return apiKeyRequired;
    }

    public boolean isLocalHostProvider() {
        return localHostProvider;
    }

    public ModelWireProtocol defaultWireProtocol() {
        return defaultProtocol;
    }
}
