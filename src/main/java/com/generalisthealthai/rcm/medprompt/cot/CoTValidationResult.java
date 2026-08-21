package com.generalisthealthai.rcm.medprompt.cot;

import com.generalisthealthai.rcm.domain.enums.ReviewDecision;
import com.generalisthealthai.rcm.domain.model.RcmExemplar;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoTValidationResult {

    private boolean valid;
    private String generatedCoT;
    private ReviewDecision groundTruthDecision;
    private ReviewDecision validatedDecision;
    private RcmExemplar savedExemplar;
    private String statusMessage;
}
