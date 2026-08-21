package com.generalisthealthai.rcm.ingestion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimIngestRequestDto {

    private String claimId;

    @NotBlank(message = "Payer ID is required")
    private String payerId;

    private String patientId;

    private String dateOfService;

    private String placeOfService;

    @NotEmpty(message = "At least one CPT code is required")
    @Builder.Default
    private List<String> cptCodes = new ArrayList<>();

    @NotBlank(message = "Primary ICD-10 diagnosis code is required")
    private String primaryIcd10;

    @Builder.Default
    private List<String> secondaryIcd10s = new ArrayList<>();

    private BigDecimal billedAmount;
}
