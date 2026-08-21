package com.generalisthealthai.rcm.api.dto;

import com.generalisthealthai.rcm.ingestion.dto.ClaimIngestRequestDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppealDraftRequestDto {

    @Valid
    @NotNull(message = "Claim details must be provided")
    private ClaimIngestRequestDto claim;

    @Builder.Default
    private List<String> denialCodes = new ArrayList<>();

    private String auditRationale;
    private String customPayerPolicy;
}
