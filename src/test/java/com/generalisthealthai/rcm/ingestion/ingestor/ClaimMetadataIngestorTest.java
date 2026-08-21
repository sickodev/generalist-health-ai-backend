package com.generalisthealthai.rcm.ingestion.ingestor;

import com.generalisthealthai.rcm.domain.enums.CoverageStatus;
import com.generalisthealthai.rcm.domain.enums.NetworkStatus;
import com.generalisthealthai.rcm.domain.model.ClaimMetadata;
import com.generalisthealthai.rcm.domain.model.EligibilityRecord;
import com.generalisthealthai.rcm.ingestion.dto.ClaimIngestRequestDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClaimMetadataIngestorTest {

    private ClaimMetadataIngestor ingestor;

    @BeforeEach
    void setUp() {
        ingestor = new ClaimMetadataIngestor();
    }

    @Test
    void testIngestValidClaimRequest() {
        ClaimIngestRequestDto dto = ClaimIngestRequestDto.builder()
                .claimId("CLM-2026-001")
                .payerId("CIGNA")
                .patientId("PAT-5544")
                .dateOfService("2026-08-21")
                .placeOfService("22")
                .cptCodes(List.of("73721", "99214"))
                .primaryIcd10("M25.561")
                .secondaryIcd10s(List.of("M54.5"))
                .billedAmount(new BigDecimal("1250.00"))
                .build();

        ClaimMetadata result = ingestor.ingest(dto);

        assertNotNull(result);
        assertEquals("CLM-2026-001", result.getClaimId());
        assertEquals("CIGNA", result.getPayerId());
        assertEquals("PAT-5544", result.getPatientId());
        assertEquals("22", result.getPlaceOfService());
        assertEquals(2, result.getCptCodes().size());
        assertEquals("M25.561", result.getPrimaryIcd10());
        assertEquals(1, result.getSecondaryIcd10s().size());
    }

    @Test
    void testBuildCanonicalEmbeddingText() {
        ClaimMetadata claim = ClaimMetadata.builder()
                .payerId("BCBS")
                .cptCodes(List.of("99214"))
                .primaryIcd10("M54.5")
                .placeOfService("11")
                .build();

        EligibilityRecord eligibility = EligibilityRecord.builder()
                .coverageStatus(CoverageStatus.ACTIVE)
                .networkStatus(NetworkStatus.IN_NETWORK)
                .priorAuthRequired(true)
                .copayAmount(new BigDecimal("30.00"))
                .build();

        String canonical = ingestor.buildCanonicalEmbeddingText(claim, eligibility);

        assertTrue(canonical.contains("Payer: BCBS"));
        assertTrue(canonical.contains("CPT: 99214"));
        assertTrue(canonical.contains("Primary Diagnosis: M54.5"));
        assertTrue(canonical.contains("Coverage: ACTIVE"));
        assertTrue(canonical.contains("PA Required: true"));
        assertTrue(canonical.contains("Copay: $30.00"));
    }

    @Test
    void testIngestNullThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> ingestor.ingest(null));
    }
}
