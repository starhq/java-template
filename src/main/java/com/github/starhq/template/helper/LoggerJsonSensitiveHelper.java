package com.github.starhq.template.helper;

import com.github.starhq.template.config.json.properties.SensitiveFieldProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.StringNode;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Helper utility for masking sensitive data in JSON logs and HTTP request metadata.
 * <p>
 * This class provides centralized, configurable desensitization for:
 * <ul>
 *     <li><strong>JSON Payloads</strong>: Recursively masks sensitive fields in request/response bodies</li>
 *     <li><strong>HTTP Headers</strong>: Redacts configured header names (e.g., {@code Authorization}, {@code Cookie})</li>
 *     <li><strong>Request Parameters</strong>: Masks sensitive query/form parameters (e.g., {@code password}, {@code token})</li>
 * </ul>
 * <p>
 * <strong>Configuration-Driven:</strong>
 * <p>
 * Sensitive field names are defined in {@link SensitiveFieldProperties}, enabling dynamic
 * updates without code changes. Typical configuration:
 * <pre>
 * {@code
 * template:
 *   sensitive:
 *     fields: [password, token, secret, idCard, phone, email]
 *     headers: [Authorization, Cookie, X-Api-Key]
 *     mask-value: "***"
 * }
 * </pre>
 * <p>
 * <strong>Privacy & Compliance:</strong>
 * <p>
 * This helper helps meet regulatory requirements (GDPR, PIPL, PCI-DSS) by preventing
 * sensitive data exposure in application logs. Always:
 * <ul>
 *     <li>Review and update {@code sensitiveFieldProperties} periodically</li>
 *     <li>Test desensitization logic with realistic payloads before production deployment</li>
 *     <li>Monitor logs for accidental PII leakage and adjust rules accordingly</li>
 * </ul>
 * <p>
 * <strong>Thread Safety:</strong>
 * <p>
 * This helper is stateless and safe for concurrent use. The underlying {@link JsonMapper}
 * must be configured as thread-safe (default behavior for Jackson 2.x+).
 *
 * @author wangjian
 * @author starhq (maintainer)
 * @version 1.0
 * @date 2026-05-04
 * @see SensitiveFieldProperties
 * @see JsonMapper
 * @see HttpServletRequest
 */
@Slf4j
@RequiredArgsConstructor
public class LoggerJsonSensitiveHelper {

    private final JsonMapper jsonMapper;
    private final SensitiveFieldProperties sensitiveFieldProperties;

    /**
     * Masks sensitive fields in a JSON byte array for safe logging.
     * <p>
     * This method performs a deep traversal of the JSON structure, replacing values
     * of configured sensitive keys with a mask placeholder (e.g., {@code "***"}).
     * Non-sensitive fields and structural elements (arrays, nested objects) are preserved.
     * <p>
     * <strong>Processing Flow:</strong>
     * <ol>
     *     <li>Parse input bytes into {@link JsonNode} tree</li>
     *     <li>Recursively traverse all object properties and array elements</li>
     *     <li>Replace values of sensitive keys (case-insensitive match) with mask value</li>
     *     <li>Serialize back to JSON string with environment-appropriate formatting</li>
     * </ol>
     * <p>
     * <strong>Environment Formatting:</strong>
     * <ul>
     *     <li>{@code isProd = true}: Compact JSON (minified) for efficient log storage</li>
     *     <li>{@code isProd = false}: Pretty-printed JSON for local debugging</li>
     * </ul>
     * <p>
     * <strong>Fallback Strategy:</strong>
     * <ul>
     *     <li>If JSON parsing fails: Return original UTF-8 string with WARN log</li>
     *     <li>If unknown error occurs: Return {@code "[Binary Data Error]"} with ERROR log</li>
     *     <li>Never throw exceptions to avoid breaking logging pipelines</li>
     * </ul>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * byte[] requestBody = ...; // from HTTP request
     * String safeLog = loggerJsonSensitiveHelper.mask(requestBody, true);
     * log.info("Received request body: {}", safeLog);
     * }
     * </pre>
     *
     * @param content the JSON content as UTF-8 bytes; may be {@code null} or empty
     * @param isProd  {@code true} for compact output (production), {@code false} for pretty-print (development)
     * @return the desensitized JSON string, or original content if parsing fails, or {@code null} if input is empty
     * @see JsonMapper#readTree(byte[])
     * @see SensitiveFieldProperties#getMaskValue()
     */
    public String mask(byte[] content, boolean isProd) {
        if (content == null || content.length == 0) {
            return null;
        }

        try {
            // 1. Parse JSON bytes into mutable tree model
            JsonNode rootNode = jsonMapper.readTree(content);

            // 2. Recursively mask sensitive fields throughout the tree
            maskNode(rootNode);

            // 3. Serialize back to string with environment-appropriate formatting
            return getString(isProd, rootNode);
        } catch (JacksonException e) {
            // Non-JSON content: log warning and return original string for debugging
            log.warn("JSON desensitization failed (invalid JSON), reason: {}", e.getMessage());
            return new String(content, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // Unexpected error: return safe placeholder to avoid log pollution
            log.error("An unknown error occurred during JSON desensitization", e);
            return "[Binary Data Error]";
        }
    }

    /**
     * Masks sensitive data in HTTP request headers or parameters for safe logging.
     * <p>
     * This method extracts either headers or parameters from the request, applies
     * configured sensitive field rules, and returns a sanitized map representation.
     * <p>
     * <strong>Extraction Strategy:</strong>
     * <ul>
     *     <li>{@code isHeader = true}: Extract configured header names only (not all headers)</li>
     *     <li>{@code isHeader = false}: Extract all request parameters (query + form)</li>
     * </ul>
     * <p>
     * <strong>Masking Rules:</strong>
     * <ul>
     *     <li>Keys matching {@code sensitiveFieldProperties.getFields()} are replaced with mask value</li>
     *     <li>Empty values are represented as {@code "[EMPTY]"} for clarity</li>
     *     <li>Multi-value parameters are preserved as lists unless masked</li>
     * </ul>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Log sensitive headers (e.g., for auth debugging)
     * String headers = loggerJsonSensitiveHelper.mask(request, true, true);
     * log.debug("Request headers: {}", headers);
     *
     * // Log request parameters (e.g., for API auditing)
     * String params = loggerJsonSensitiveHelper.mask(request, false, true);
     * log.info("Request params: {}", params);
     * }
     * </pre>
     *
     * @param request  the HTTP request to extract data from; must not be {@code null}
     * @param isHeader {@code true} to process headers, {@code false} to process parameters
     * @param isProd   {@code true} for compact JSON output, {@code false} for pretty-print
     * @return the desensitized map as JSON string, or {@code null} if no data to process
     * @see HttpServletRequest#getHeader(String)
     * @see HttpServletRequest#getParameterMap()
     * @see SensitiveFieldProperties#isSensitive(String)
     */
    public String mask(HttpServletRequest request, boolean isHeader, boolean isProd) {
        Map<?, ?> map = isHeader ? getHeadersMap(request) : getParamsMap(request);
        if (CollectionUtils.isEmpty(map)) {
            return null;
        }
        return getString(isProd, map);
    }

    // ==========================================
    // Private Helper Methods
    // ==========================================

    /**
     * Serializes an object to JSON string with environment-appropriate formatting.
     *
     * @param isProd {@code true} for compact output, {@code false} for pretty-print
     * @param object the object to serialize; may be {@link JsonNode} or {@link Map}
     * @return the JSON string representation
     * @throws JacksonException if serialization fails (should be caught by caller)
     */
    private String getString(boolean isProd, Object object) {
        return isProd ? jsonMapper.writeValueAsString(object) :
                jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
    }

    /**
     * Extracts and masks configured HTTP headers from the request.
     * <p>
     * Only headers listed in {@code sensitiveFieldProperties.getHeaders()} are included
     * in the result map. Each value is masked if the header name is sensitive.
     *
     * @param request the HTTP request to extract headers from
     * @return a map of header names to masked values; never {@code null}
     */
    private Map<?, ?> getHeadersMap(HttpServletRequest request) {
        Set<String> headers = sensitiveFieldProperties.getHeaders();
        Map<String, String> headerMap = new LinkedHashMap<>(headers.size());
        for (String header : headers) {
            String value = request.getHeader(header);
            // Mask sensitive headers; represent empty as "[EMPTY]" for audit clarity
            headerMap.put(header, StringUtils.hasText(value) ?
                    (sensitiveFieldProperties.isSensitive(header) ? sensitiveFieldProperties.getMaskValue() : value) :
                    "[EMPTY]");
        }
        return headerMap;
    }

    /**
     * Extracts and masks request parameters (query + form) from the request.
     * <p>
     * All parameters are included. Single-value parameters are stored as strings,
     * multi-value parameters as lists. Sensitive keys are replaced with mask value.
     *
     * @param request the HTTP request to extract parameters from
     * @return a map of parameter names to masked values; {@code null} if no parameters
     */
    private Map<?, ?> getParamsMap(HttpServletRequest request) {
        Map<String, String[]> params = request.getParameterMap();
        if (params.isEmpty()) {
            return null;
        }

        Map<String, Object> simpleParams = new LinkedHashMap<>(params.size());
        params.forEach((key, values) ->
                simpleParams.put(key, sensitiveFieldProperties.isSensitive(key) ?
                        sensitiveFieldProperties.getMaskValue() :
                        (values.length == 1 ? values[0] : Arrays.asList(values))));
        return simpleParams;
    }

    /**
     * Recursively traverses a JSON tree and masks sensitive field values.
     * <p>
     * <strong>Traversal Strategy:</strong>
     * <ul>
     *     <li><strong>Object Nodes</strong>: Iterate properties; mask values of sensitive keys, recurse otherwise</li>
     *     <li><strong>Array Nodes</strong>: Recurse into each element without masking array indices</li>
     *     <li><strong>Primitive Nodes</strong>: No action (values are only masked at parent object level)</li>
     * </ul>
     * <p>
     * <strong>Matching Rules:</strong>
     * <ul>
     *     <li>Key matching is case-insensitive via {@link SensitiveFieldProperties#isSensitive(String)}</li>
     *     <li>Only leaf values are masked; nested objects under sensitive keys are not traversed</li>
     *     <li>Masking is in-place on the mutable {@link ObjectNode} tree</li>
     * </ul>
     *
     * @param node the JSON node to process; {@code null} is safely ignored
     * @see JsonNode#isObject()
     * @see JsonNode#isArray()
     * @see #isSensitiveKey(String)
     */
    private void maskNode(JsonNode node) {
        if (Objects.isNull(node)) {
            return;
        }

        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;

            node.properties().forEach(entry -> {
                String key = entry.getKey();
                JsonNode value = entry.getValue();

                if (isSensitiveKey(key)) {
                    // Replace sensitive field value with mask placeholder
                    objectNode.set(key, new StringNode(sensitiveFieldProperties.getMaskValue()));
                } else {
                    // Recurse into non-sensitive nested structures
                    maskNode(value);
                }
            });
        } else if (node.isArray()) {
            // Recurse into array elements without masking indices
            for (JsonNode element : node) {
                maskNode(element);
            }
        }
        // Primitive values (string, number, boolean) are handled by parent object masking
    }

    /**
     * Checks if a field key should be masked based on configured sensitive fields.
     * <p>
     * Delegates to {@link SensitiveFieldProperties#isSensitive(String)} which typically
     * performs case-insensitive matching against a predefined set of field names.
     *
     * @param key the field name to check; may be {@code null}
     * @return {@code true} if the key is configured as sensitive, {@code false} otherwise
     * @see SensitiveFieldProperties#isSensitive(String)
     */
    private boolean isSensitiveKey(String key) {
        return sensitiveFieldProperties.isSensitive(key);
    }

}