package com.github.starhq.template.service;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Service interface for CAPTCHA (Completely Automated Public Turing test to tell Computers and Humans Apart) generation and verification.
 * <p>
 * This service provides secure challenge-response mechanisms to prevent automated abuse of public endpoints
 * such as login, registration, password reset, and comment submission. It supports UUID-based session tracking,
 * image-based challenge generation, and case-insensitive verification with configurable complexity.
 * <p>
 * <strong>Primary Use Cases:</strong>
 * <ul>
 *     <li><strong>Login Protection</strong>: Require CAPTCHA after N failed login attempts to prevent brute-force attacks</li>
 *     <li><strong>Registration Throttling</strong>: Block automated account creation bots with visual challenges</li>
 *     <li><strong>Password Reset Security</strong>: Ensure password reset requests originate from human users</li>
 *     <li><strong>Comment/Feedback Spam Prevention</strong>: Filter automated spam submissions in public forms</li>
 * </ul>
 * <p>
 * <strong>Security Design Principles:</strong>
 * <ul>
 *     <li><strong>Stateless Session</strong>: Use UUID as session key stored in Redis with short TTL (5-10 min) to avoid server-side session bloat</li>
 *     <li><strong>One-Time Use</strong>: Each CAPTCHA code is invalidated immediately after successful verification to prevent replay attacks</li>
 *     <li><strong>Rate Limiting</strong>: Implement per-IP/per-UUID request limits to prevent enumeration or DoS attacks</li>
 *     <li><strong>Complexity Configuration</strong>: Support adjustable character set, length, and distortion level to balance security and usability</li>
 * </ul>
 * <p>
 * <strong>Integration Pattern:</strong>
 * <pre>
 * {@code
 * // Controller: Generate CAPTCHA image for login page
 * @GetMapping("/captcha")
 * public void getCaptcha(@RequestParam String uuid, HttpServletResponse response) {
 *     captchaService.generateCode(uuid, response);
 * }
 *
 * // Controller: Verify CAPTCHA during login
 * @PostMapping("/login")
 * public Result<Void> login(@RequestBody LoginDTO dto, @RequestParam String uuid, @RequestParam String captcha) {
 *     // Verify CAPTCHA before authentication
 *     captchaService.verify(uuid, captcha);
 *
 *     // Proceed with username/password authentication
 *     authService.authenticate(dto.getUsername(), dto.getPassword());
 *     return Result.success();
 * }
 *
 * // Frontend: Fetch and display CAPTCHA
 * const refreshCaptcha = () => {
 *   const uuid = generateUUID(); // Client-side UUID generation
 *   captchaImage.src = `/api/captcha?uuid=${uuid}&t=${Date.now()}`; // Cache busting
 * };
 *
 * // Submit login with CAPTCHA
 * const handleSubmit = async () => {
 *   await api.login({
 *     username: form.username,
 *     password: form.password,
 *     uuid: currentUuid,
 *     captcha: form.captcha
 *   });
 * };
 * }
 * </pre>
 *
 * @author wangjian
 * @author starhq (maintainer)
 * @version 1.0
 * @date 2026-05-08
 * @see HttpServletResponse
 * @see com.github.starhq.template.common.captcha.ICaptcha
 * @see com.github.starhq.template.common.constant.CacheConstant#CAPTCHA
 */
public interface CaptchaService {

    /**
     * Generates a CAPTCHA challenge image and writes it to the HTTP response.
     * <p>
     * This method creates a visual challenge (typically distorted text) and returns it
     * as a PNG/JPEG image stream. The generated code is stored server-side (Redis) with
     * the provided {@code uuid} as key for later verification.
     * <p>
     * <strong>Parameter Contracts:</strong>
     * <ul>
     *     <li>{@code uuid}: Must not be {@code null} or empty; serves as session key for code storage and verification</li>
     *     <li>{@code response}: Must not be {@code null}; HTTP response to write image stream and cache-control headers</li>
     * </ul>
     * <p>
     * <strong>Response Behavior:</strong>
     * <ul>
     *     <li><strong>Content-Type</strong>: Sets {@code image/png} or {@code image/jpeg} based on configuration</li>
     *     <li><strong>Cache Control</strong>: Adds {@code Cache-Control: no-store, no-cache, must-revalidate} to prevent browser caching</li>
     *     <li><strong>Image Stream</strong>: Writes binary image data directly to {@code response.getOutputStream()}</li>
     *     <li><strong>Error Handling</strong>: Throws {@link RuntimeException} if image generation fails; caller should handle gracefully</li>
     * </ul>
     * <p>
     * <strong>Server-Side Storage:</strong>
     * <p>
     * The generated CAPTCHA code is stored with the following properties:
     * <ul>
     *     <li><strong>Key</strong>: {@code "captcha:" + uuid} (e.g., {@code "captcha:550e8400-e29b-41d4-a716-446655440000"})</li>
     *     <li><strong>Value</strong>: Plain text code (e.g., {@code "aB3x"}) in uppercase for case-insensitive verification</li>
     *     <li><strong>TTL</strong>: Short expiration (typically 5-10 minutes) to balance security and user experience</li>
     *     <li><strong>One-Time Use</strong>: Code is deleted immediately after successful verification to prevent replay</li>
     * </ul>
     * <p>
     * <strong>Security Considerations:</strong>
     * <ul>
     *     <li><strong>UUID Entropy</strong>: Ensure {@code uuid} is cryptographically random (e.g., {@code UUID.randomUUID()}) to prevent prediction attacks</li>
     *     <li><strong>Image Distortion</strong>: Apply sufficient noise, warping, and character overlap to resist OCR attacks</li>
     *     <li><strong>Rate Limiting</strong>: Implement per-IP limits on CAPTCHA generation requests to prevent resource exhaustion</li>
     *     <li><strong>Accessibility</strong>: Consider providing audio CAPTCHA alternative for visually impaired users</li>
     * </ul>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Controller: Generate CAPTCHA for login page
     * @GetMapping("/captcha")
     * public void getCaptcha(@RequestParam String uuid, HttpServletResponse response) {
     *     try {
     *         // Validate UUID format
     *         if (!isValidUuid(uuid)) {
     *             response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid UUID");
     *             return;
     *         }
     *
     *         // Generate and write CAPTCHA image
     *         captchaService.generateCode(uuid, response);
     *
     *     } catch (IOException e) {
     *         log.error("Failed to write CAPTCHA image", e);
     *         response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
     *     }
     * }
     *
     * // Frontend: Fetch CAPTCHA with cache busting
     * const loadCaptcha = () => {
     *   const uuid = crypto.randomUUID();
     *   sessionStorage.setItem('captchaUuid', uuid);
     *
     *   // Add timestamp to bypass browser cache
     *   captchaImg.src = `/api/captcha?uuid=${uuid}&t=${Date.now()}`;
     * };
     * }
     * </pre>
     * <p>
     * <strong>Performance Considerations:</strong>
     * <ul>
     *     <li><strong>Image Generation</strong>: CAPTCHA generation is CPU-intensive; consider async generation or CDN caching for high-traffic scenarios</li>
     *     <li><strong>Redis Operations</strong>: Store code with {@code SETEX} for atomic set+expire to avoid race conditions</li>
     *     <li><strong>Memory Usage</strong>: Avoid storing large image buffers in memory; stream directly to response output</li>
     * </ul>
     *
     * @param uuid     the session identifier for storing/retrieving the CAPTCHA code; must not be {@code null} or empty
     * @param response the HTTP response to write the CAPTCHA image stream; must not be {@code null}
     * @throws IllegalArgumentException if {@code uuid} or {@code response} is {@code null}
     * @throws RuntimeException         if image generation or response writing fails
     * @see HttpServletResponse#setContentType(String)
     * @see HttpServletResponse#getOutputStream()
     * @see java.util.UUID#randomUUID()
     */
    void generateCode(String uuid, HttpServletResponse response);

    /**
     * Verifies a user-provided CAPTCHA code against the server-stored value.
     * <p>
     * This method retrieves the expected code using the provided {@code uuid},
     * performs case-insensitive comparison with the user input, and invalidates
     * the stored code after verification (one-time use policy).
     * <p>
     * <strong>Parameter Contracts:</strong>
     * <ul>
     *     <li>{@code uuid}: Must not be {@code null} or empty; the session key used during CAPTCHA generation</li>
     *     <li>{@code captcha}: Must not be {@code null} or empty; the user-provided code to verify (case-insensitive)</li>
     * </ul>
     * <p>
     * <strong>Verification Semantics:</strong>
     * <ul>
     *     <li><strong>Success</strong>: Returns normally if {@code captcha} matches stored code (case-insensitive)</li>
     *     <li><strong>Code Mismatch</strong>: Throws {@link com.github.starhq.template.common.exception.CaptchaException} with {@code ErrorCode.CAPTCHA_MISMATCH}</li>
     *     <li><strong>Expired/Used Code</strong>: Throws {@link com.github.starhq.template.common.exception.CaptchaException} with {@code ErrorCode.CAPTCHA_EXPIRED}</li>
     *     <li><strong>Invalid UUID</strong>: Throws {@link IllegalArgumentException} if {@code uuid} format is invalid</li>
     * </ul>
     * <p>
     * <strong>One-Time Use Policy:</strong>
     * <p>
     * After verification (regardless of success or failure), the stored CAPTCHA code is deleted
     * to prevent replay attacks. This means:
     * <ul>
     *     <li>Each CAPTCHA challenge can only be attempted once</li>
     *     <li>Failed attempts require fetching a new CAPTCHA image</li>
     *     <li>Successful verification automatically invalidates the code</li>
     * </ul>
     * <p>
     * <strong>Security Considerations:</strong>
     * <ul>
     *     <li><strong>Case-Insensitive Comparison</strong>: Convert both stored and input codes to uppercase before comparison to improve UX</li>
     *     <li><strong>Timing Attack Prevention</strong>: Use constant-time string comparison if storing codes in non-constant-time data structures</li>
     *     <li><strong>Rate Limiting</strong>: Implement per-IP/per-UUID verification attempt limits to prevent brute-force guessing</li>
     *     <li><strong>Logging</strong>: Log verification failures (without storing user input) for security monitoring</li>
     * </ul>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Controller: Verify CAPTCHA before login
     * @PostMapping("/login")
     * public Result<Void> login(@RequestBody LoginDTO dto,
     *                          @RequestParam String uuid,
     *                          @RequestParam String captcha) {
     *     try {
     *         // Verify CAPTCHA first (throws exception on failure)
     *         captchaService.verify(uuid, captcha);
     *
     *         // Proceed with authentication
     *         authService.authenticate(dto.getUsername(), dto.getPassword());
     *
     *         return Result.success();
     *
     *     } catch (CaptchaException e) {
     *         // Return CAPTCHA-specific error to frontend
     *         return Result.fail(e.getErrorCode());
     *     }
     * }
     *
     * // Frontend: Handle CAPTCHA verification error
     * const handleSubmit = async () => {
     *   try {
     *     await api.login({
     *       username: form.username,
     *       password: form.password,
     *       uuid: sessionStorage.getItem('captchaUuid'),
     *       captcha: form.captcha
     *     });
     *     router.push('/dashboard');
     *   } catch (error) {
     *     if (error.code === ErrorCode.CAPTCHA_MISMATCH.code) {
     *       message.error('Incorrect CAPTCHA');
     *       refreshCaptcha(); // Auto-refresh on failure
     *       form.captcha = ''; // Clear input field
     *     } else if (error.code === ErrorCode.CAPTCHA_EXPIRED.code) {
     *       message.error('CAPTCHA expired, please refresh');
     *       refreshCaptcha();
     *     }
     *   }
     * };
     * }
     * </pre>
     * <p>
     * <strong>Exception Handling Strategy:</strong>
     * <ul>
     *     <li><strong>Business Exceptions</strong>: Use typed {@link com.github.starhq.template.common.exception.BusinessException} for precise frontend error handling</li>
     *     <li><strong>Global Handler</strong>: Configure {@code @ControllerAdvice} to convert CAPTCHA exceptions to standardized API responses</li>
     *     <li><strong>Audit Logging</strong>: Log verification attempts (success/failure) with IP and timestamp for security analysis</li>
     * </ul>
     *
     * @param uuid    the session identifier used during CAPTCHA generation; must not be {@code null} or empty
     * @param captcha the user-provided code to verify; must not be {@code null} or empty
     * @throws IllegalArgumentException                                      if {@code uuid} or {@code captcha} is {@code null} or empty
     * @throws com.github.starhq.template.common.exception.BusinessException if verification fails (mismatch, expired, or used)
     * @see com.github.starhq.template.common.enums.ErrorCode#CAPTCHA_VERIFY
     * @see com.github.starhq.template.common.enums.ErrorCode#CAPTCHA_EXPIRED
     * @see com.github.starhq.template.common.constant.CacheConstant#CAPTCHA
     */
    void verify(String uuid, String captcha);

}