package com.generalisthealthai.rcm.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExemplarSearchRequestDto {

    @NotBlank(message = "Search query text cannot be blank")
    private String query;

    @Builder.Default
    private int limit = 4;
}
