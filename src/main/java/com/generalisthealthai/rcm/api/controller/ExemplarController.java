package com.generalisthealthai.rcm.api.controller;

import com.generalisthealthai.rcm.api.dto.ExemplarResponseDto;
import com.generalisthealthai.rcm.api.dto.ExemplarSearchRequestDto;
import com.generalisthealthai.rcm.domain.model.RcmExemplar;
import com.generalisthealthai.rcm.infrastructure.persistence.RcmExemplarRepository;
import com.generalisthealthai.rcm.medprompt.retrieval.VectorSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/rcm/exemplars")
@Tag(name = "Medprompt Exemplars", description = "Endpoints for managing and querying Medprompt few-shot historical exemplars")
public class ExemplarController {

    private final RcmExemplarRepository exemplarRepository;
    private final VectorSearchService vectorSearchService;

    public ExemplarController(RcmExemplarRepository exemplarRepository, VectorSearchService vectorSearchService) {
        this.exemplarRepository = exemplarRepository;
        this.vectorSearchService = vectorSearchService;
    }

    @GetMapping
    @Operation(summary = "List Historical Exemplars", description = "Retrieves all verified RCM historical exemplars with validated CoT rationale.")
    public ResponseEntity<List<ExemplarResponseDto>> listExemplars() {
        List<ExemplarResponseDto> dtos = exemplarRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/search")
    @Operation(summary = "Vector Search Exemplars", description = "Performs dynamic k-NN semantic search across historical exemplars.")
    public ResponseEntity<List<ExemplarResponseDto>> searchExemplars(@Valid @RequestBody ExemplarSearchRequestDto searchDto) {
        List<RcmExemplar> exemplars = vectorSearchService.findTopKExemplarsForText(searchDto.getQuery(), searchDto.getLimit());
        List<ExemplarResponseDto> dtos = exemplars.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    private ExemplarResponseDto mapToDto(RcmExemplar ex) {
        return ExemplarResponseDto.builder()
                .id(ex.getId())
                .payerId(ex.getPayerId())
                .cptCode(ex.getCptCode())
                .icd10Code(ex.getIcd10Code())
                .serviceDescription(ex.getServiceDescription())
                .groundTruthDecision(ex.getGroundTruthDecision())
                .denialCode(ex.getDenialCode())
                .validatedCoTRationale(ex.getValidatedCoTRationale())
                .createdAt(ex.getCreatedAt())
                .build();
    }
}
