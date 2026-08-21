package com.generalisthealthai.rcm.api.dto;

import com.generalisthealthai.rcm.domain.enums.ReviewDecision;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExemplarResponseDto {

    private UUID id;
    private String payerId;
    private String cptCode;
    private String icd10Code;
    private String serviceDescription;
    private ReviewDecision groundTruthDecision;
    private String denialCode;
    private String validatedCoTRationale;
    private Instant createdAt;
}
