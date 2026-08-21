package com.generalisthealthai.rcm.medprompt.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.generalisthealthai.rcm.domain.enums.DenialRisk;
import com.generalisthealthai.rcm.domain.enums.ReviewDecision;
import com.generalisthealthai.rcm.medprompt.ensemble.dto.LlmAuditResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeminiClientTest {

    private GeminiClient client;

    @BeforeEach
    void setUp() {
        client = new GeminiClient(
                RestClient.builder(),
                new ObjectMapper(),
                "demo-key",
                "https://generativelanguage.googleapis.com/v1beta",
                "gemini-1.5-pro",
                0.3
        );
    }

    @Test
    void testParseJsonAuditResponseClean() {
        String json = """
                {
                  "decision": "DENIED",
                  "denialRisk": "HIGH_RISK",
                  "riskScore": 0.89,
                  "predictedDenialCodes": ["CO-50"],
                  "stepByStepRationale": "1. Diagnosis does not support EKG.",
                  "appealLetterDraft": "To BCBS..."
                }
                """;

        LlmAuditResponseDto dto = client.parseJsonAuditResponse(json);

        assertNotNull(dto);
        assertEquals(ReviewDecision.DENIED, dto.getDecision());
        assertEquals(DenialRisk.HIGH_RISK, dto.getDenialRisk());
        assertEquals(0.89, dto.getRiskScore());
        assertEquals(1, dto.getPredictedDenialCodes().size());
        assertEquals("CO-50", dto.getPredictedDenialCodes().get(0));
    }

    @Test
    void testParseJsonWithMarkdownFences() {
        String markdown = """
                ```json
                {
                  "decision": "PAID",
                  "denialRisk": "LOW_RISK",
                  "riskScore": 0.05,
                  "predictedDenialCodes": [],
                  "stepByStepRationale": "Valid claim."
                }
                ```
                """;

        LlmAuditResponseDto dto = client.parseJsonAuditResponse(markdown);

        assertNotNull(dto);
        assertEquals(ReviewDecision.PAID, dto.getDecision());
        assertEquals(DenialRisk.LOW_RISK, dto.getDenialRisk());
    }

    @Test
    void testSyntheticAuditForEkgLowBackPain() {
        String prompt = "Payer: BCBS | CPT: 93000 | Diagnosis: M54.5";
        LlmAuditResponseDto dto = client.generateAudit(prompt);

        assertNotNull(dto);
        assertEquals(ReviewDecision.DENIED, dto.getDecision());
        assertEquals(DenialRisk.HIGH_RISK, dto.getDenialRisk());
        assertTrue(dto.getPredictedDenialCodes().contains("CO-50"));
    }
}
