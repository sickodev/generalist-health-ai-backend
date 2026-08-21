package com.generalisthealthai.rcm.domain.model;

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
public class ClaimMetadata {

    private String claimId;
    private String payerId;
    private String patientId;
    private String dateOfService;
    private String placeOfService;

    @Builder.Default
    private List<String> cptCodes = new ArrayList<>();

    private String primaryIcd10;

    @Builder.Default
    private List<String> secondaryIcd10s = new ArrayList<>();

    private BigDecimal billedAmount;
}
