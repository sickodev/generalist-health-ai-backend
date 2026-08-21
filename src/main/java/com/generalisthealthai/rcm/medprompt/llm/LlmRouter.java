package com.generalisthealthai.rcm.medprompt.llm;

import com.generalisthealthai.rcm.medprompt.ensemble.dto.LlmAuditResponseDto;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
public class LlmRouter {

    private final GeminiClient geminiClient;

    public LlmRouter(GeminiClient geminiClient) {
        this.geminiClient = geminiClient;
    }

    /**
     * Routes the prompt to the configured LLM client.
     */
    public LlmAuditResponseDto route(String prompt) {
        return geminiClient.generateAudit(prompt);
    }

    /**
     * Returns an inference function reference suitable for EnsembleOrchestrator.
     */
    public Function<String, LlmAuditResponseDto> getInferenceFunction() {
        return this::route;
    }
}
