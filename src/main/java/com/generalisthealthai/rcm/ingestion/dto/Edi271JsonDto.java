package com.generalisthealthai.rcm.ingestion.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonIgnoreProperties(ignoreUnknown = true)
public class Edi271JsonDto {

    private String patientId;
    private String memberId;
    private String payerId;
    private String payerName;
    private String dateOfService;
    private String coverageStatus; // "ACTIVE", "INACTIVE", "1", "6", etc.
    
    @Builder.Default
    private List<String> serviceTypeCodes = new ArrayList<>();
    
    private Boolean priorAuthRequired;
    private String networkStatus; // "IN_NETWORK", "OUT_OF_NETWORK", "Y", "N"
    private Boolean pcpReferralRequired;
    private BigDecimal copayAmount;
    private BigDecimal coinsurancePercent;
    
    @Builder.Default
    private List<String> notes = new ArrayList<>();
}
