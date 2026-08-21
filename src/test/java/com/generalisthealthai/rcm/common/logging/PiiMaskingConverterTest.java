package com.generalisthealthai.rcm.common.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class PiiMaskingConverterTest {

    private PiiMaskingConverter converter;
    private ILoggingEvent mockEvent;

    @BeforeEach
    void setUp() {
        converter = new PiiMaskingConverter();
        mockEvent = Mockito.mock(ILoggingEvent.class);
    }

    @Test
    void testMasksPatientIdAndDob() {
        when(mockEvent.getFormattedMessage()).thenReturn("Processing audit for patientId: PAT-998877 and dob: 1985-04-12");

        String result = converter.convert(mockEvent);

        assertFalse(result.contains("PAT-998877"));
        assertFalse(result.contains("1985-04-12"));
        assertTrue(result.contains("patientId=[REDACTED]"));
        assertTrue(result.contains("dob=[REDACTED]"));
    }

    @Test
    void testMasksMemberIdAndEmail() {
        when(mockEvent.getFormattedMessage()).thenReturn("Verification payload for memberId=\"MBR-12345\" sent by auditor@healthplan.com");

        String result = converter.convert(mockEvent);

        assertFalse(result.contains("MBR-12345"));
        assertFalse(result.contains("auditor@healthplan.com"));
        assertTrue(result.contains("memberId=[REDACTED]"));
        assertTrue(result.contains("[REDACTED_EMAIL]"));
    }
}
