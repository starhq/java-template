package com.github.starhq.template.config.json.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Set;

/**
 * Configuration properties for data masking and sensitive field detection.
 *
 * <p>Binds to the {@code star.sensitive} block in application YAML files. This class provides
 * the rule engine for the logging framework to determine which JSON fields or HTTP headers
 * should be sanitized before being written to log files or monitoring systems.
 *
 * <p><b>YAML Binding Example:</b>
 * <pre>
 * star:
 *   sensitive:
 *     mask-value: "[MASKED]"
 *     fields:
 *       - password
 *       - idCard
 * </pre>
 *
 * @author starhq
 */
@Data
@ConfigurationProperties(prefix = "star.sensitive")
public class SensitiveFieldProperties {

    /**
     * A predefined set of standard field names that require data masking.
     * <p>This acts as a global baseline to prevent common credentials and tokens from leaking into logs.
     */
    private Set<String> fields = Set.of("password", "oldPassword", "newPassword", "confirmPassword", "token", "secret",
            "accessToken", "refreshToken", "authorization", "cookie", "set-cookie");

    /**
     * A predefined set of HTTP header names to be inspected during request logging.
     * <p>Used by AOP aspects to securely log headers without exposing sensitive tokens.
     */
    private Set<String> headers = Set.of(
            "Authorization", "Content-Type", "Accept", "User-Agent",
            "X-Device-Fingerprint", "Referer", "Origin", "Host");

    /**
     * The string value used to replace sensitive data in log outputs.
     * <p>Defaults to "*****". Can be customized via YAML if a different placeholder is preferred.
     */
    private String maskValue = "*****";

    /**
     * Evaluates whether a given field name belongs to the sensitive fields list.
     *
     * <p>This method is highly resilient to variations in JSON field naming conventions (e.g., Java's
     * camelCase "accessToken" vs JSON's snake_case "accesstoken") by performing case-insensitive matching.
     *
     * @param fieldName the name of the field to check (can be null)
     * @return {@code true} if the field is in the sensitive list, {@code false} otherwise
     */
    public boolean isSensitive(String fieldName) {
        // Guard clause against null fields or an unconfigured/empty field set
        if (fieldName == null || fields.isEmpty()) {
            return false;
        }
        // Case-insensitive stream matching
        return fields.stream()
                .anyMatch(sensitive -> sensitive.equalsIgnoreCase(fieldName));
    }
}
