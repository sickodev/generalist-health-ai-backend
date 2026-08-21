package com.generalisthealthai.rcm.ingestion.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.generalisthealthai.rcm.domain.enums.CoverageStatus;
import com.generalisthealthai.rcm.domain.enums.NetworkStatus;
import com.generalisthealthai.rcm.domain.model.EligibilityRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Edi271ParserTest {

    private Edi271Parser parser;

    @BeforeEach
    void setUp() {
        parser = new Edi271Parser(new ObjectMapper());
    }

    @Test
    void testParseRawX12Edi271ActiveCoverageWithCopayAndAuth() {
        String sampleX12 =
                "ISA*00*          *00*          *ZZ*SUBMITTER      *ZZ*RECEIVER       *260821*1200*^*00501*000000001*0*T*:~" +
                "GS*HB*SUBMITTER*RECEIVER*20260821*1200*1*X*005010X279A1~" +
                "ST*271*0001*005010X279A1~" +
                "BHT*0022*11*REQ001*20260821*1200~" +
                "HL*1**20*1~" +
                "NM1*PR*2*UNITEDHEALTHCARE*****PI*UHC9988~" +
                "HL*2*1*21*1~" +
                "NM1*1P*1*DOE*JOHN****XX*1234567890~" +
                "HL*3*2*22*0~" +
                "NM1*IL*1*SMITH*JANE****MI*PAT-778899~" +
                "DTP*291*D8*20260821~" +
                "EB*1**30***27*25.00*0.20**Y*Y~" +
                "EB*1**98***27*40.00***Y*Y~" +
                "REF*9F*REFERRAL-REQ~" +
                "SE*13*0001~" +
                "GE*1*1~" +
                "IEA*1*000000001~";

        EligibilityRecord record = parser.parse(sampleX12);

        assertNotNull(record);
        assertEquals("UHC9988", record.getPayerId());
        assertEquals("PAT-778899", record.getPatientId());
        assertEquals("2026-08-21", record.getDateOfService());
        assertEquals(CoverageStatus.ACTIVE, record.getCoverageStatus());
        assertEquals(NetworkStatus.IN_NETWORK, record.getNetworkStatus());
        assertTrue(record.getPriorAuthRequired());
        assertTrue(record.getPcpReferralRequired());
        assertEquals(new BigDecimal("25.00"), record.getCopayAmount());
        assertEquals(new BigDecimal("0.20"), record.getCoinsurancePercent());
        assertTrue(record.getServiceTypeCodes().contains("30"));
        assertTrue(record.getServiceTypeCodes().contains("98"));
    }

    @Test
    void testParseRawX12Edi271InactiveCoverage() {
        String sampleX12 =
                "ISA*00*          *00*          *ZZ*SENDER         *ZZ*RECEIVER       *260821*1200*^*00501*000000002*0*T*:~" +
                "NM1*PR*2*AETNA*****PI*AETNA01~" +
                "NM1*IL*1*WILLIAMS*BOB****MI*PAT-112233~" +
                "EB*6**30~" +
                "SE*4*0001~";

        EligibilityRecord record = parser.parse(sampleX12);

        assertEquals("AETNA01", record.getPayerId());
        assertEquals("PAT-112233", record.getPatientId());
        assertEquals(CoverageStatus.INACTIVE, record.getCoverageStatus());
        assertFalse(record.getPriorAuthRequired());
    }

    @Test
    void testParseStructuredJsonEdi271() {
        String jsonPayload = """
                {
                    "payerId": "BCBS-IL",
                    "memberId": "MBR-9988",
                    "dateOfService": "2026-08-21",
                    "coverageStatus": "ACTIVE",
                    "networkStatus": "IN_NETWORK",
                    "serviceTypeCodes": ["30", "1"],
                    "priorAuthRequired": true,
                    "copayAmount": 35.00,
                    "coinsurancePercent": 0.15
                }
                """;

        EligibilityRecord record = parser.parse(jsonPayload);

        assertNotNull(record);
        assertEquals("BCBS-IL", record.getPayerId());
        assertEquals("MBR-9988", record.getPatientId());
        assertEquals("2026-08-21", record.getDateOfService());
        assertEquals(CoverageStatus.ACTIVE, record.getCoverageStatus());
        assertEquals(NetworkStatus.IN_NETWORK, record.getNetworkStatus());
        assertTrue(record.getPriorAuthRequired());
        assertEquals(0, new BigDecimal("35.00").compareTo(record.getCopayAmount()));
        assertEquals(0, new BigDecimal("0.15").compareTo(record.getCoinsurancePercent()));
    }

    @Test
    void testParseEmptyOrNullThrowsException() {
        assertThrows(EdiParseException.class, () -> parser.parse(""));
        assertThrows(EdiParseException.class, () -> parser.parse(null));
        assertThrows(EdiParseException.class, () -> parser.parse("   "));
    }
}
