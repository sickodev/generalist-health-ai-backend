package com.generalisthealthai.rcm.common.logging;

import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Logback message converter that redacts sensitive PII/PHI fields for HIPAA compliance.
 */
public class PiiMaskingConverter extends MessageConverter {

    private static final Pattern[] PII_PATTERNS = new Pattern[]{
            // JSON-style or key-value patterns: "patientId": "12345" or patientId=12345
            Pattern.compile("(?i)(patientId|memberId|subscriberId|ssn|dob|dateOfBirth)\\s*[:=]\\s*[\"']?([A-Za-z0-9\\-]+)[\"']?"),
            // Email address pattern
            Pattern.compile("(?i)[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}")
    };

    @Override
    public String convert(ILoggingEvent event) {
        String message = super.convert(event);
        if (message == null || message.isEmpty()) {
            return message;
        }

        String maskedMessage = message;
        for (Pattern pattern : PII_PATTERNS) {
            Matcher matcher = pattern.matcher(maskedMessage);
            if (matcher.find()) {
                maskedMessage = matcher.replaceAll(matchResult -> {
                    String group = matchResult.group();
                    if (group.contains(":") || group.contains("=")) {
                        String key = matchResult.group(1);
                        return key + "=[REDACTED]";
                    }
                    return "[REDACTED_EMAIL]";
                });
            }
        }
        return maskedMessage;
    }
}
