package com.generalisthealthai.rcm.api.controller;

import com.generalisthealthai.rcm.validation.BenchmarkEvaluationReport;
import com.generalisthealthai.rcm.validation.MedpromptValidationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rcm/benchmark")
@Tag(name = "Medprompt Benchmark", description = "Endpoints for running the automated 200-claim Medprompt validation and accuracy test suite")
public class BenchmarkController {

    private final MedpromptValidationService validationService;

    public BenchmarkController(MedpromptValidationService validationService) {
        this.validationService = validationService;
    }

    @PostMapping("/run")
    @Operation(summary = "Execute 200-Claim Validation Benchmark", description = "Runs the standardized 200-claim validation suite and returns statistical accuracy, precision, recall, and F1 scores.")
    public ResponseEntity<BenchmarkEvaluationReport> runBenchmark() {
        BenchmarkEvaluationReport report = validationService.runValidationSuite();
        return ResponseEntity.ok(report);
    }
}
