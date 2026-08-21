package com.generalisthealthai.rcm.domain.model;

import com.generalisthealthai.rcm.domain.enums.CoverageStatus;
import com.generalisthealthai.rcm.domain.enums.NetworkStatus;
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
public class EligibilityRecord {

    private String patientId;
    private String payerId;
    private String dateOfService;
    private CoverageStatus coverageStatus;

    @Builder.Default
    private List<String> serviceTypeCodes = new ArrayList<>();

    private Boolean priorAuthRequired;
    private NetworkStatus networkStatus;
    private Boolean pcpReferralRequired;
    private BigDecimal copayAmount;
    private BigDecimal coinsurancePercent;

    @Builder.Default
    private List<String> rawSegments = new ArrayList<>();
}
