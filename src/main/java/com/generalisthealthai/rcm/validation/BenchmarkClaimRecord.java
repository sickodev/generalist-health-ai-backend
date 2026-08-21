package com.generalisthealthai.rcm.validation;

import com.generalisthealthai.rcm.domain.enums.ReviewDecision;
import com.generalisthealthai.rcm.ingestion.dto.ClaimIngestRequestDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BenchmarkClaimRecord {

    private String testId;
    private ClaimIngestRequestDto claim;
    private ReviewDecision expectedDecision;
    private String expectedDenialCode;
    private String category;
}
