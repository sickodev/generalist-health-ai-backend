package com.generalisthealthai.rcm.ingestion.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.generalisthealthai.rcm.domain.enums.CoverageStatus;
import com.generalisthealthai.rcm.domain.enums.NetworkStatus;
import com.generalisthealthai.rcm.domain.model.EligibilityRecord;
import com.generalisthealthai.rcm.ingestion.dto.Edi271JsonDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class Edi271Parser {

    private static final Logger log = LoggerFactory.getLogger(Edi271Parser.class);
    private final ObjectMapper objectMapper;

    public Edi271Parser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Parses input string automatically detecting whether it is raw X12 EDI 271 or structured JSON.
     */
    public EligibilityRecord parse(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new EdiParseException("EDI input cannot be null or empty");
        }

        String trimmed = input.trim();
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            return parseJson(trimmed);
        } else {
            return parseX12(trimmed);
        }
    }

    /**
     * Parses raw ANSI ASC X12 EDI 271 transaction.
     */
    public EligibilityRecord parseX12(String rawX12) {
        if (rawX12 == null || rawX12.trim().isEmpty()) {
            throw new EdiParseException("Raw X12 content cannot be empty");
        }

        try {
            String[] segments;
            if (rawX12.contains("~")) {
                segments = rawX12.split("~");
            } else {
                segments = rawX12.split("\\r?\\n");
            }

            EligibilityRecord.EligibilityRecordBuilder builder = EligibilityRecord.builder();
            List<String> serviceTypeCodes = new ArrayList<>();
            List<String> rawSegmentsList = new ArrayList<>();
            CoverageStatus coverageStatus = CoverageStatus.UNKNOWN;
            NetworkStatus networkStatus = NetworkStatus.UNKNOWN;
            Boolean priorAuthRequired = null;
            Boolean pcpReferralRequired = false;
            BigDecimal copayAmount = null;
            BigDecimal coinsurancePercent = null;
            String payerId = null;
            String patientId = null;
            String dateOfService = null;

            for (String segment : segments) {
                String cleanSegment = segment.trim();
                if (cleanSegment.isEmpty()) {
                    continue;
                }
                rawSegmentsList.add(cleanSegment);

                String[] elements = cleanSegment.split("\\*", -1);
                if (elements.length == 0) {
                    continue;
                }

                String tag = elements[0].trim().toUpperCase();

                switch (tag) {
                    case "NM1":
                        // NM1*PR = Payer, NM1*IL = Insured / Subscriber, NM1*QC = Patient
                        if (elements.length > 1) {
                            String entityId = elements[1].trim();
                            if ("PR".equalsIgnoreCase(entityId) && elements.length > 9) {
                                payerId = elements[9].trim();
                            } else if (("IL".equalsIgnoreCase(entityId) || "QC".equalsIgnoreCase(entityId)) && elements.length > 9) {
                                patientId = elements[9].trim();
                            }
                        }
                        break;

                    case "DTP":
                        // DTP*291*D8*20260821 or DTP*472*D8*20260821
                        if (elements.length >= 4) {
                            dateOfService = formatDateOfService(elements[3].trim());
                        }
                        break;

                    case "EB":
                        String currentServiceCode = (elements.length > 3) ? elements[3].trim() : "";

                        // EB01 = Eligibility/Benefit Code
                        if (elements.length > 1) {
                            String eb01 = elements[1].trim();
                            if ("1".equals(eb01)) {
                                coverageStatus = CoverageStatus.ACTIVE;
                            } else if ("6".equals(eb01)) {
                                coverageStatus = CoverageStatus.INACTIVE;
                            } else if ("7".equals(eb01)) {
                                coverageStatus = CoverageStatus.TERMINATED;
                            }
                        }

                        // EB03 = Service Type Code
                        if (!currentServiceCode.isEmpty() && !serviceTypeCodes.contains(currentServiceCode)) {
                            serviceTypeCodes.add(currentServiceCode);
                        }

                        // EB07 = Monetary Amount (Copay / Deductible)
                        if (elements.length > 7 && !elements[7].trim().isEmpty()) {
                            try {
                                BigDecimal val = new BigDecimal(elements[7].trim());
                                if (copayAmount == null || "30".equals(currentServiceCode)) {
                                    copayAmount = val;
                                }
                            } catch (NumberFormatException e) {
                                log.warn("Could not parse EB07 copay amount: {}", elements[7]);
                            }
                        }

                        // EB08 = Coinsurance Percent
                        if (elements.length > 8 && !elements[8].trim().isEmpty()) {
                            try {
                                BigDecimal val = new BigDecimal(elements[8].trim());
                                if (coinsurancePercent == null || "30".equals(currentServiceCode)) {
                                    coinsurancePercent = val;
                                }
                            } catch (NumberFormatException e) {
                                log.warn("Could not parse EB08 coinsurance percent: {}", elements[8]);
                            }
                        }

                        // Check authorization indicator (EB11 or second-to-last/last flags)
                        for (int i = 9; i < elements.length; i++) {
                            String val = elements[i].trim().toUpperCase();
                            if (i == 11 || (i == 10 && elements.length == 12)) {
                                if ("Y".equals(val)) {
                                    priorAuthRequired = true;
                                } else if ("N".equals(val) && priorAuthRequired == null) {
                                    priorAuthRequired = false;
                                }
                            }
                            if (i == 12 || (i == 11 && elements.length == 12)) {
                                if ("Y".equals(val)) {
                                    networkStatus = NetworkStatus.IN_NETWORK;
                                } else if ("N".equals(val)) {
                                    networkStatus = NetworkStatus.OUT_OF_NETWORK;
                                }
                            }
                        }
                        break;

                    case "REF":
                        // Pre-auth or referral numbers: REF*BB (Prior Auth), REF*9F (Referral)
                        if (elements.length > 1 && "9F".equalsIgnoreCase(elements[1].trim())) {
                            pcpReferralRequired = true;
                        }
                        break;

                    default:
                        break;
                }
            }

            return builder
                    .payerId(payerId)
                    .patientId(patientId)
                    .dateOfService(dateOfService)
                    .coverageStatus(coverageStatus)
                    .serviceTypeCodes(serviceTypeCodes)
                    .priorAuthRequired(priorAuthRequired != null ? priorAuthRequired : false)
                    .networkStatus(networkStatus)
                    .pcpReferralRequired(pcpReferralRequired)
                    .copayAmount(copayAmount)
                    .coinsurancePercent(coinsurancePercent)
                    .rawSegments(rawSegmentsList)
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse X12 EDI 271 payload", e);
            throw new EdiParseException("Invalid X12 EDI 271 format: " + e.getMessage(), e);
        }
    }

    /**
     * Parses structured JSON eligibility payload.
     */
    public EligibilityRecord parseJson(String json) {
        try {
            Edi271JsonDto dto = objectMapper.readValue(json, Edi271JsonDto.class);

            CoverageStatus coverageStatus = CoverageStatus.UNKNOWN;
            if (dto.getCoverageStatus() != null) {
                String statusStr = dto.getCoverageStatus().trim().toUpperCase();
                if ("ACTIVE".equals(statusStr) || "1".equals(statusStr)) {
                    coverageStatus = CoverageStatus.ACTIVE;
                } else if ("INACTIVE".equals(statusStr) || "6".equals(statusStr)) {
                    coverageStatus = CoverageStatus.INACTIVE;
                } else if ("TERMINATED".equals(statusStr) || "7".equals(statusStr)) {
                    coverageStatus = CoverageStatus.TERMINATED;
                }
            }

            NetworkStatus networkStatus = NetworkStatus.UNKNOWN;
            if (dto.getNetworkStatus() != null) {
                String netStr = dto.getNetworkStatus().trim().toUpperCase();
                if ("IN_NETWORK".equals(netStr) || "Y".equals(netStr)) {
                    networkStatus = NetworkStatus.IN_NETWORK;
                } else if ("OUT_OF_NETWORK".equals(netStr) || "N".equals(netStr)) {
                    networkStatus = NetworkStatus.OUT_OF_NETWORK;
                }
            }

            String patientId = dto.getPatientId() != null ? dto.getPatientId() : dto.getMemberId();

            return EligibilityRecord.builder()
                    .payerId(dto.getPayerId() != null ? dto.getPayerId() : dto.getPayerName())
                    .patientId(patientId)
                    .dateOfService(dto.getDateOfService())
                    .coverageStatus(coverageStatus)
                    .serviceTypeCodes(dto.getServiceTypeCodes() != null ? dto.getServiceTypeCodes() : new ArrayList<>())
                    .priorAuthRequired(dto.getPriorAuthRequired() != null ? dto.getPriorAuthRequired() : false)
                    .networkStatus(networkStatus)
                    .pcpReferralRequired(dto.getPcpReferralRequired() != null ? dto.getPcpReferralRequired() : false)
                    .copayAmount(dto.getCopayAmount())
                    .coinsurancePercent(dto.getCoinsurancePercent())
                    .rawSegments(dto.getNotes() != null ? dto.getNotes() : new ArrayList<>())
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse JSON EDI 271 payload", e);
            throw new EdiParseException("Invalid JSON EDI 271 payload: " + e.getMessage(), e);
        }
    }

    private String formatDateOfService(String rawDate) {
        if (rawDate == null || rawDate.length() != 8) {
            return rawDate;
        }
        // YYYYMMDD -> YYYY-MM-DD
        return rawDate.substring(0, 4) + "-" + rawDate.substring(4, 6) + "-" + rawDate.substring(6, 8);
    }
}
