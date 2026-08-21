package com.generalisthealthai.rcm.validation;

import com.generalisthealthai.rcm.domain.enums.ReviewDecision;
import com.generalisthealthai.rcm.ingestion.dto.ClaimIngestRequestDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class SyntheticClaimDatasetGenerator {

    private static final String[] PAYERS = {"BCBS", "UnitedHealthcare", "Aetna", "Cigna"};

    /**
     * Generates a 200-claim standardized validation benchmark dataset.
     */
    public List<BenchmarkClaimRecord> generateBenchmarkDataset() {
        List<BenchmarkClaimRecord> dataset = new ArrayList<>(200);

        int id = 1;

        // 1. 50 Claims: Advanced Imaging / Procedure lacking Prior Auth (Expected: DENIED / CO-197)
        for (int i = 0; i < 50; i++) {
            String payer = PAYERS[i % PAYERS.length];
            dataset.add(BenchmarkClaimRecord.builder()
                    .testId(String.format("VAL-PA-%03d", id++))
                    .category("PRIOR_AUTH_ABSENT")
                    .expectedDecision(ReviewDecision.DENIED)
                    .expectedDenialCode("CO-197")
                    .claim(ClaimIngestRequestDto.builder()
                            .claimId(String.format("CLM-PA-%03d", i + 1))
                            .payerId(payer)
                            .patientId(String.format("PAT-PA-%03d", i + 1))
                            .cptCodes(List.of("73721"))
                            .primaryIcd10("M25.561")
                            .billedAmount(new BigDecimal("1250.00"))
                            .placeOfService("11")
                            .dateOfService("2026-08-21")
                            .build())
                    .build());
        }

        // 2. 50 Claims: Medical Necessity Mismatches (Expected: DENIED / CO-50)
        for (int i = 0; i < 50; i++) {
            String payer = PAYERS[i % PAYERS.length];
            dataset.add(BenchmarkClaimRecord.builder()
                    .testId(String.format("VAL-MN-%03d", id++))
                    .category("MEDICAL_NECESSITY_MISMATCH")
                    .expectedDecision(ReviewDecision.DENIED)
                    .expectedDenialCode("CO-50")
                    .claim(ClaimIngestRequestDto.builder()
                            .claimId(String.format("CLM-MN-%03d", i + 1))
                            .payerId(payer)
                            .patientId(String.format("PAT-MN-%03d", i + 1))
                            .cptCodes(List.of("93000"))
                            .primaryIcd10("M54.5")
                            .billedAmount(new BigDecimal("220.00"))
                            .placeOfService("11")
                            .dateOfService("2026-08-21")
                            .build())
                    .build());
        }

        // 3. 30 Claims: Physical Therapy Documentation Exceeded (Expected: DENIED / CO-16)
        for (int i = 0; i < 30; i++) {
            String payer = PAYERS[i % PAYERS.length];
            dataset.add(BenchmarkClaimRecord.builder()
                    .testId(String.format("VAL-DOC-%03d", id++))
                    .category("DOCUMENTATION_DEFICIT")
                    .expectedDecision(ReviewDecision.DENIED)
                    .expectedDenialCode("CO-16")
                    .claim(ClaimIngestRequestDto.builder()
                            .claimId(String.format("CLM-DOC-%03d", i + 1))
                            .payerId(payer)
                            .patientId(String.format("PAT-DOC-%03d", i + 1))
                            .cptCodes(List.of("97110"))
                            .primaryIcd10("M54.5")
                            .billedAmount(new BigDecimal("180.00"))
                            .placeOfService("11")
                            .dateOfService("2026-08-21")
                            .build())
                    .build());
        }

        // 4. 70 Claims: Clean Claims (Expected: PAID)
        for (int i = 0; i < 70; i++) {
            String payer = PAYERS[i % PAYERS.length];
            boolean isEkg = (i % 2 == 0);
            dataset.add(BenchmarkClaimRecord.builder()
                    .testId(String.format("VAL-CLEAN-%03d", id++))
                    .category("CLEAN_CLAIM")
                    .expectedDecision(ReviewDecision.PAID)
                    .expectedDenialCode(null)
                    .claim(ClaimIngestRequestDto.builder()
                            .claimId(String.format("CLM-CLN-%03d", i + 1))
                            .payerId(payer)
                            .patientId(String.format("PAT-CLN-%03d", i + 1))
                            .cptCodes(isEkg ? List.of("93000") : List.of("99214"))
                            .primaryIcd10(isEkg ? "R07.9" : "M54.5")
                            .billedAmount(isEkg ? new BigDecimal("250.00") : new BigDecimal("190.00"))
                            .placeOfService("11")
                            .dateOfService("2026-08-21")
                            .build())
                    .build());
        }

        return dataset;
    }
}
