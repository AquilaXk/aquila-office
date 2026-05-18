package com.aquilaoffice.application.port.out;

public interface AnalyzeChatIntentPort {

    ChatIntentAnalysisResult analyze(AnalyzeChatIntentCommand command);
}
