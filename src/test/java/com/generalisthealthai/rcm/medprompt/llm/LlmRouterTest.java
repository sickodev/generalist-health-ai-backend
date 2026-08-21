package com.generalisthealthai.rcm.medprompt.llm;

import com.generalisthealthai.rcm.domain.enums.ReviewDecision;
import com.generalisthealthai.rcm.medprompt.ensemble.dto.LlmAuditResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class LlmRouterTest {

    private GeminiClient geminiClient;
    private LlmRouter router;

    @BeforeEach
    void setUp() {
        geminiClient = Mockito.mock(GeminiClient.class);
        router = new LlmRouter(geminiClient);
    }

    @Test
    void testRouteCallsGeminiClient() {
        LlmAuditResponseDto mockResponse = LlmAuditResponseDto.builder()
                .decision(ReviewDecision.PAID)
                .build();
        when(geminiClient.generateAudit(anyString())).thenReturn(mockResponse);

        LlmAuditResponseDto result = router.route("test prompt");
        assertNotNull(result);
        assertEquals(ReviewDecision.PAID, result.getDecision());

        Function<String, LlmAuditResponseDto> inferenceFunc = router.getInferenceFunction();
        assertNotNull(inferenceFunc);
        assertEquals(ReviewDecision.PAID, inferenceFunc.apply("test").getDecision());
    }
}
