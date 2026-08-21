package com.generalisthealthai.rcm.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SyntheticClaimDatasetGeneratorTest {

    private SyntheticClaimDatasetGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new SyntheticClaimDatasetGenerator();
    }

    @Test
    void testGenerateBenchmarkDatasetGenerates200Claims() {
        List<BenchmarkClaimRecord> dataset = generator.generateBenchmarkDataset();

        assertNotNull(dataset);
        assertEquals(200, dataset.size());

        long paClaims = dataset.stream().filter(c -> "PRIOR_AUTH_ABSENT".equals(c.getCategory())).count();
        long mnClaims = dataset.stream().filter(c -> "MEDICAL_NECESSITY_MISMATCH".equals(c.getCategory())).count();
        long docClaims = dataset.stream().filter(c -> "DOCUMENTATION_DEFICIT".equals(c.getCategory())).count();
        long cleanClaims = dataset.stream().filter(c -> "CLEAN_CLAIM".equals(c.getCategory())).count();

        assertEquals(50, paClaims);
        assertEquals(50, mnClaims);
        assertEquals(30, docClaims);
        assertEquals(70, cleanClaims);
    }
}
