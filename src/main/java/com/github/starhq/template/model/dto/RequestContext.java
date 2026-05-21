package com.github.starhq.template.model.dto;

import java.io.Serializable;

/**
 * Request context information captured at the start of an API request for audit and security purposes.
 * <p>
 * This record encapsulates client-side request metadata that is typically extracted from HTTP headers
 * and used for logging, security analysis, and audit trail generation. It is designed to be immutable
 * and thread-safe for use across the application layer.
 * <p>
 * <strong>Primary Use Cases:</strong>
 * <ul>
 *     <li><strong>Audit Logging</strong>: Record client context (IP, device) for security events</li>
 *     <li><strong>Security Analysis</strong>: Detect suspicious patterns (multiple IPs for same session)</li>
 *     <li><strong>Debugging</strong>: Correlate logs across distributed services using request context</li>
 * </ul>
 * <p>
 * <strong>Context Extraction:</strong>
 * <p>
 * Values are typically extracted from HTTP request attributes:
 * <ul>
 *     <li>{@code deviceFingerprint}: Extracted from request headers (e.g., {@code X-Device-Fingerprint}) or generated from browser fingerprinting</li>
 *     <li>{@code clientIp}: Extracted from {@code X-Forwarded-For}, {@code X-Real-IP}, or direct connection source</li>
 * </ul>
 * <p>
 * <strong>Usage Example:</strong>
 * <pre>
 * {@code
 * @Controller
 * public class AuditController {
 *     @PostMapping("/login")
 *     public Result<Void> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
 *         RequestContext context = new RequestContext(
 *             httpRequest.getHeader("X-Device-Fingerprint"),
 *             getClientIp(httpRequest)
 *         );
 *
 *         // Pass context to service layer for audit logging
 *         authService.login(request, context);
 *         return Result.success();
 *     }
 *
 *     private String getClientIp(HttpServletRequest request) {
 *         String ip = request.getHeader("X-Forwarded-For");
 *         if (ip == null || ip.isEmpty()) {
 *             ip = request.getRemoteAddr();
 *         }
 *         return ip;
 *     }
 * }
 * }
 * </pre>
 * <p>
 * <strong>Security Considerations:</strong>
 * <ul>
 *     <li>Client IP can be spoofed; use additional verification (e.g., geolocation, behavioral analysis)</li>
 *     <li>Device fingerprints should be validated against known patterns to prevent tampering</li>
 *     <li>Consider masking or hashing sensitive context data in logs for privacy compliance</li>
 *     <li>Implement rate-limiting based on device fingerprint to prevent abuse</li>
 * </ul>
 * <p>
 * <strong>Thread Safety:</strong>
 * <p>
 * This record is immutable and thread-safe. Instances can be safely shared across threads
 * without synchronization.
 *
 * @author starhq
 * @version 1.0
 * @date 2026-05-20
 */
public record RequestContext(
        String deviceFingerprint,
        String clientIp
) implements Serializable {
}
