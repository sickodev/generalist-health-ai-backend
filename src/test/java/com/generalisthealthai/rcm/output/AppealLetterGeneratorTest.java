package com.generalisthealthai.rcm.output;

import com.generalisthealthai.rcm.domain.model.ClaimMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppealLetterGeneratorTest {

    private AppealLetterGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new AppealLetterGenerator();
    }

    @Test
    void testGenerateAppealLetter() {
        ClaimMetadata claim = ClaimMetadata.builder()
                .claimId("CLM-9988")
                .payerId("UnitedHealthcare")
                .patientId("PAT-1122")
                .dateOfService("2026-08-21")
                .cptCodes(List.of("73721"))
                .primaryIcd10("M25.561")
                .build();

        String letter = generator.generateAppealLetter(
                claim,
                List.of("CO-197"),
                "Urgent diagnostic MRI required for acute knee joint trauma.",
                "RAD012 - Radiology Prior Authorization Guidelines"
        );

        assertNotNull(letter);
        assertTrue(letter.contains("UnitedHealthcare"));
        assertTrue(letter.contains("CLM-9988"));
        assertTrue(letter.contains("PAT-1122"));
        assertTrue(letter.contains("CO-197"));
        assertTrue(letter.contains("73721"));
        assertTrue(letter.contains("M25.561"));
        assertTrue(letter.contains("RAD012"));
    }
}
