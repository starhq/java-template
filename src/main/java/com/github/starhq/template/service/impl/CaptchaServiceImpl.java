package com.github.starhq.template.service.impl;

import com.github.starhq.template.common.captcha.CaptchaResult;
import com.github.starhq.template.common.captcha.ICaptcha;
import com.github.starhq.template.common.constant.CacheConstant;
import com.github.starhq.template.common.enums.ErrorCode;
import com.github.starhq.template.common.exception.BadRequestException;
import com.github.starhq.template.common.exception.BusinessException;
import com.github.starhq.template.common.exception.CustomException;
import com.github.starhq.template.common.util.RequestContextUtil;
import com.github.starhq.template.helper.CacheHelper;
import com.github.starhq.template.service.CaptchaService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service implementation for CAPTCHA generation and verification with rate limiting and cache integration.
 * <p>
 * This class provides secure challenge-response mechanisms to prevent automated abuse of public endpoints
 * such as login, registration, and password reset. It integrates with {@link ICaptcha} for image generation,
 * {@link CacheHelper} for code storage and rate limit tracking, and implements defense-in-depth strategies
 * against brute-force, replay, and enumeration attacks.
 * <p>
 * <strong>Primary Responsibilities:</strong>
 * <ul>
 *     <li><strong>CAPTCHA Generation</strong>: Create visual challenges with distortion, noise, and configurable complexity</li>
 *     <li><strong>Code Verification</strong>: Perform case-insensitive comparison with one-time use policy to prevent replay</li>
 *     <li><strong>Rate Limiting</strong>: Enforce per-IP request limits for generation and failure-based limits for verification</li>
 *     <li><strong>Cache Management</strong>: Store codes and rate counters in Redis with short TTL for consistency and performance</li>
 * </ul>
 * <p>
 * <strong>Security Design Principles:</strong>
 * <ul>
 *     <li><strong>Defense in Depth</strong>: Combine UUID validation, IP rate limiting, failure tracking, and one-time code use</li>
 *     <li><strong>Fail-Secure</strong>: Any validation failure results in exception; no silent failures that could be exploited</li>
 *     <li><strong>Stateless Session</strong>: Use UUID as cache key with short TTL (5 min) to avoid server-side session bloat</li>
 *     <li><strong>Audit-Ready</strong>: All verification failures logged for security monitoring and anomaly detection</li>
 * </ul>
 * <p>
 * <strong>Rate Limiting Strategy:</strong>
 * <ul>
 *     <li><strong>Generation Limit</strong>: Max {@value #IP_LIMIT_MAX} requests per IP within TTL to prevent resource exhaustion</li>
 *     <li><strong>Failure Limit</strong>: Max {@value #FAIL_LIMIT_MAX} verification failures per IP before temporary ban</li>
 *     <li><strong>Atomic Counters</strong>: Use {@link AtomicInteger} in cache for thread-safe increment without race conditions</li>
 * </ul>
 * <p>
 * <strong>Integration Pattern:</strong>
 * <pre>
 * {@code
 * // Controller: Generate CAPTCHA for login page
 * @GetMapping("/captcha")
 * public void getCaptcha(@RequestParam String uuid, HttpServletResponse response) {
 *     captchaService.generateCode(uuid, response);
 * }
 *
 * // Controller: Verify CAPTCHA before authentication
 * @PostMapping("/login")
 * public Result<Void> login(@RequestBody LoginDTO dto,
 *                          @RequestParam String uuid,
 *                          @RequestParam String captcha) {
 *     // Verify CAPTCHA first (throws exception on failure)
 *     captchaService.verify(uuid, captcha);
 *
 *     // Proceed with authentication
 *     authService.authenticate(dto.getUsername(), dto.getPassword());
 *     return Result.success();
 * }
 *
 * // Frontend: Handle CAPTCHA workflow
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
 *     if (error.code === ErrorCode.CAPTCHA_VERIFY.code) {
 *       message.error('Incorrect CAPTCHA');
 *       refreshCaptcha(); // Auto-refresh on failure
 *     } else if (error.code === ErrorCode.CAPTCHA_VERIFY_BANNED.code) {
 *       message.error('Too many failed attempts, please try later');
 *     }
 *   }
 * };
 * }
 * </pre>
 *
 * @author wangjian
 * @author starhq (maintainer)
 * @version 1.0
 * @date 2026-05-08
 * @see CaptchaService
 * @see ICaptcha
 * @see CacheHelper
 * @see CacheConstant
 */
@Service("captchaService")
@RequiredArgsConstructor
public class CaptchaServiceImpl implements CaptchaService {

    /**
     * CAPTCHA generator implementation for creating visual challenges.
     * <p>
     * This component handles:
     * <ul>
     *     <li>Random code generation with configurable character set and length</li>
     *     <li>Image rendering with distortion, noise, and font randomization to resist OCR</li>
     *     <li>Output streaming to {@link OutputStream} for HTTP response integration</li>
     * </ul>
     * <p>
     * <strong>Implementation Note:</strong>
     * <p>
     * Typical implementations include:
     * <ul>
     *     <li>Simple text-based CAPTCHA with Gaussian noise and wave distortion</li>
     *     <li>Advanced CAPTCHA with clickable objects or slide puzzles for better UX</li>
     *     <li>Third-party services (e.g., reCAPTCHA, hCaptcha) via adapter pattern</li>
     * </ul>
     *
     * @see ICaptcha#generate()
     * @see ICaptcha#write(CaptchaResult, OutputStream)
     */
    private final ICaptcha captcha;

    /**
     * Cache helper for storing CAPTCHA codes and rate limit counters.
     * <p>
     * Used for:
     * <ul>
     *     <li><strong>Code Storage</strong>: Store generated code with UUID key and short TTL for verification</li>
     *     <li><strong>Rate Limiting</strong>: Track per-IP request counts for generation and failure limits</li>
     *     <li><strong>Atomic Operations</strong>: Support {@link AtomicInteger} in cache for thread-safe counters</li>
     * </ul>
     * <p>
     * <strong>Cache Configuration:</strong>
     * <pre>
     * {@code
     * // CAPTCHA code cache: 5 min TTL, evict on verification
     * @Bean
     * public CacheManager captchaCacheManager() {
     *     return new RedisCacheManager(redisConnectionFactory,
     *         RedisCacheConfiguration.defaultCacheConfig()
     *             .entryTtl(Duration.ofMinutes(5))
     *             .disableCachingNullValues());
     * }
     *
     * // Rate limit cache: 10 min TTL for IP-based counters
     * @Bean
     * public CacheManager rateLimitCacheManager() {
     *     return new RedisCacheManager(redisConnectionFactory,
     *         RedisCacheConfiguration.defaultCacheConfig()
     *             .entryTtl(Duration.ofMinutes(10)));
     * }
     * }
     * </pre>
     *
     * @see CacheHelper#getCache(String)
     * @see CacheConstant#CAPTCHA
     * @see CacheConstant#CAPTCHA_IP
     * @see CacheConstant#CAPTCHA_VERIFY
     */
    private final CacheHelper cacheHelper;

    /**
     * Cache key prefix for IP-based CAPTCHA generation rate limiting.
     * <p>
     * Format: {@code "captcha:ip:limit:{clientIp}"}
     * <p>
     * Used to track how many times a specific IP has requested CAPTCHA generation
     * within the configured time window. Prevents resource exhaustion attacks.
     *
     * @see #IP_LIMIT_MAX
     * @see CacheConstant#CAPTCHA_IP
     */
    private static final String IP_LIMIT_PREFIX = "captcha:ip:limit:";

    /**
     * Cache key prefix for IP-based verification failure tracking.
     * <p>
     * Format: {@code "captcha:verify:fail:{clientIp}"}
     * <p>
     * Used to track consecutive verification failures per IP. When threshold is exceeded,
     * further verification attempts are blocked to prevent brute-force guessing attacks.
     *
     * @see #FAIL_LIMIT_MAX
     * @see CacheConstant#CAPTCHA_VERIFY
     */
    private static final String FAIL_LIMIT_PREFIX = "captcha:verify:fail:";

    /**
     * Maximum number of CAPTCHA generation requests allowed per IP within the TTL window.
     * <p>
     * Exceeding this limit results in {@link CustomException} with {@link ErrorCode#CAPTCHA_REQUEST_TOO_OFTEN}.
     * <p>
     * <strong>Tuning Guidance:</strong>
     * <ul>
     *     <li>Increase for high-traffic public endpoints with legitimate retry needs</li>
     *     <li>Decrease for sensitive operations (e.g., password reset) to reduce attack surface</li>
     *     <li>Monitor metrics to adjust based on actual usage patterns and attack signals</li>
     * </ul>
     *
     * @see #checkIpRateLimit(String, String)
     */
    private static final int IP_LIMIT_MAX = 10;

    /**
     * Maximum number of consecutive verification failures allowed per IP before temporary ban.
     * <p>
     * Exceeding this limit results in {@link BusinessException} with {@link ErrorCode#CAPTCHA_VERIFY_BANNED}.
     * <p>
     * <strong>Security Considerations:</strong>
     * <ul>
     *     <li>Lower values provide stronger protection but may impact legitimate users with typos</li>
     *     <li>Consider implementing progressive delays (exponential backoff) instead of hard ban for better UX</li>
     *     <li>Log banned attempts for security monitoring and potential IP reputation scoring</li>
     * </ul>
     *
     * @see #checkVerifyFailLimit(String)
     */
    private static final int FAIL_LIMIT_MAX = 5;

    /**
     * Generates a CAPTCHA challenge image and writes it to the HTTP response.
     * <p>
     * This method implements a secure generation workflow:
     * <ol>
     *     <li>Validate UUID format and length to prevent injection or excessive memory usage</li>
     *     <li>Check IP-based rate limit to prevent resource exhaustion attacks</li>
     *     <li>Check if CAPTCHA already exists for UUID to prevent duplicate generation</li>
     *     <li>Configure HTTP headers to prevent browser caching of CAPTCHA images</li>
     *     <li>Generate CAPTCHA via {@link ICaptcha}, store code in cache, and stream image to response</li>
     * </ol>
     * <p>
     * <strong>Parameter Contracts:</strong>
     * <ul>
     *     <li>{@code uuid}: Must not be {@code null} or empty; max length 64 chars; serves as cache key for code storage</li>
     *     <li>{@code response}: Must not be {@code null}; HTTP response to write image stream and cache-control headers</li>
     * </ul>
     * <p>
     * <strong>Response Behavior:</strong>
     * <ul>
     *     <li><strong>Content-Type</strong>: Sets {@code image/png} via {@link MediaType#IMAGE_PNG_VALUE}</li>
     *     <li><strong>Cache Control</strong>: Adds {@code no-cache, no-store, max-age=0} headers to prevent caching</li>
     *     <li><strong>Image Stream</strong>: Writes binary PNG data directly to {@code response.getOutputStream()}</li>
     *     <li><strong>Error Handling</strong>: Throws {@link BusinessException} if image generation or streaming fails</li>
     * </ul>
     * <p>
     * <strong>Rate Limiting Logic:</strong>
     * <pre>
     * {@code
     * // Check IP-based generation limit
     * String clientIp = RequestContextUtil.getContext().clientIp();
     * int times = checkIpRateLimit(IP_LIMIT_PREFIX + clientIp, CacheConstant.CAPTCHA_IP);
     * if (times >= IP_LIMIT_MAX) {
     *     throw new CustomException(ErrorCode.CAPTCHA_REQUEST_TOO_OFTEN, HttpStatus.TOO_MANY_REQUESTS);
     * }
     *
     * // Check if CAPTCHA already exists for UUID (prevent duplicate generation)
     * String code = cache.get(uuid, String.class);
     * if (StringUtils.hasText(code)) {
     *     throw new CustomException(ErrorCode.CAPTCHA_REQUEST_TOO_OFTEN, HttpStatus.TOO_MANY_REQUESTS);
     * }
     * }
     * </pre>
     * <p>
     * <strong>Security Considerations:</strong>
     * <ul>
     *     <li><strong>UUID Validation</strong>: Prevents excessively long keys that could cause memory exhaustion</li>
     *     <li><strong>IP Extraction</strong>: Use {@link RequestContextUtil} to handle proxy headers ({@code X-Forwarded-For}) correctly</li>
     *     <li><strong>Atomic Counters</strong>: {@link AtomicInteger} in cache ensures thread-safe increment without race conditions</li>
     *     <li><strong>One-Time Code</strong>: Code stored in cache is evicted after verification to prevent replay</li>
     * </ul>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Controller: Generate CAPTCHA for login page
     * @GetMapping("/captcha")
     * public void getCaptcha(@RequestParam String uuid, HttpServletResponse response) {
     *     try {
     *         captchaService.generateCode(uuid, response);
     *     } catch (CustomException e) {
     *         // Handle rate limit or validation errors
     *         response.sendError(e.getHttpStatus().value(), e.getMessage());
     *     } catch (BusinessException e) {
     *         // Handle generation or streaming errors
     *         log.error("CAPTCHA generation failed", e);
     *         response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value());
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
     *
     * @param uuid     the session identifier for storing/retrieving the CAPTCHA code; must not be {@code null} or empty, max 64 chars
     * @param response the HTTP response to write the CAPTCHA image stream; must not be {@code null}
     * @throws BadRequestException if {@code uuid} is {@code null}, empty, or exceeds length limit
     * @throws CustomException     if IP rate limit exceeded or CAPTCHA already exists for UUID
     * @throws BusinessException   if image generation or response streaming fails
     * @see #validateUuid(String)
     * @see #checkIpRateLimit(String, String)
     * @see #setNoCacheHeaders(HttpServletResponse)
     * @see ICaptcha#generate()
     * @see Cache#put(Object, Object)
     */
    @Override
    public void generateCode(String uuid, HttpServletResponse response) {
        // 1. Validate UUID format and length (prevent injection/memory exhaustion)
        validateUuid(uuid);

        // 2. Extract client IP for rate limiting (handles proxy headers via RequestContextUtil)
        String clientIp = RequestContextUtil.getContext().clientIp();

        // 3. Check IP-based generation rate limit
        int times = checkIpRateLimit(IP_LIMIT_PREFIX + clientIp, CacheConstant.CAPTCHA_IP);
        if (times >= IP_LIMIT_MAX) {
            throw new CustomException(ErrorCode.CAPTCHA_REQUEST_TOO_OFTEN, HttpStatus.TOO_MANY_REQUESTS);
        }

        // 4. Check if CAPTCHA already exists for this UUID (prevent duplicate generation)
        Cache cache = cacheHelper.getCache(CacheConstant.CAPTCHA);
        String code = cache.get(uuid, String.class);
        if (StringUtils.hasText(code)) {
            throw new CustomException(ErrorCode.CAPTCHA_REQUEST_TOO_OFTEN, HttpStatus.TOO_MANY_REQUESTS);
        }

        // 5. Configure HTTP headers to prevent browser caching of CAPTCHA images
        setNoCacheHeaders(response);

        // 6. Generate CAPTCHA, store code, and stream image to response
        try (OutputStream outputStream = response.getOutputStream()) {
            // Generate random code and distorted image
            CaptchaResult result = captcha.generate();

            // Store code in cache for later verification (TTL configured in CacheConstant.CAPTCHA)
            cache.put(uuid, result.code());

            // Write image to HTTP response output stream
            captcha.write(result, outputStream);
            outputStream.flush();
        } catch (IOException e) {
            // Wrap IO errors with business exception for consistent API error handling
            throw new BusinessException(ErrorCode.CAPTCHA_FAILED, e);
        }
    }

    /**
     * Verifies a user-provided CAPTCHA code against the server-stored value.
     * <p>
     * This method implements a secure verification workflow:
     * <ol>
     *     <li>Extract client IP for failure tracking and rate limiting</li>
     *     <li>Validate user input is non-empty to prevent trivial bypass attempts</li>
     *     <li>Check if IP has exceeded verification failure limit (temporary ban)</li>
     *     <li>Retrieve stored code from cache using UUID; evict immediately (one-time use)</li>
     *     <li>Perform case-insensitive comparison; on mismatch, increment failure counter</li>
     *     <li>On success, evict failure counter cache to reset ban state</li>
     * </ol>
     * <p>
     * <strong>Parameter Contracts:</strong>
     * <ul>
     *     <li>{@code uuid}: Must not be {@code null} or empty; the session key used during CAPTCHA generation</li>
     *     <li>{@code userInputCode}: Must not be {@code null} or empty; the user-provided code to verify (case-insensitive)</li>
     * </ul>
     * <p>
     * <strong>Verification Semantics:</strong>
     * <ul>
     *     <li><strong>Success</strong>: Returns normally if {@code userInputCode} matches stored code (case-insensitive); failure counter reset</li>
     *     <li><strong>Code Mismatch</strong>: Throws {@link BusinessException} with {@link ErrorCode#CAPTCHA_VERIFY}; increments failure counter</li>
     *     <li><strong>Expired/Used Code</strong>: Throws {@link BusinessException} with {@link ErrorCode#CAPTCHA_EXPIRED} if code not found in cache</li>
     *     <li><strong>Input Validation</strong>: Throws {@link BusinessException} with {@link ErrorCode#CAPTCHA_VERIFY} if input is empty</li>
     *     <li><strong>Failure Ban</strong>: Throws {@link BusinessException} with {@link ErrorCode#CAPTCHA_VERIFY_BANNED} if IP exceeds failure limit</li>
     * </ul>
     * <p>
     * <strong>One-Time Use Policy:</strong>
     * <p>
     * After verification attempt (regardless of success or failure), the stored CAPTCHA code is evicted:
     * <pre>
     * {@code
     * // Retrieve and immediately evict code (atomic one-time use)
     * String code = cache.get(uuid, String.class);
     * if (!StringUtils.hasText(code)) {
     *     throw new BusinessException(ErrorCode.CAPTCHA_EXPIRED);
     * }
     * cache.evict(uuid); // Evict after retrieval to prevent replay
     * }
     * </pre>
     * This ensures:
     * <ul>
     *     <li>Each CAPTCHA challenge can only be attempted once</li>
     *     <li>Failed attempts require fetching a new CAPTCHA image</li>
     *     <li>Successful verification automatically invalidates the code</li>
     * </ul>
     * <p>
     * <strong>Failure Tracking Strategy:</strong>
     * <pre>
     * {@code
     * // On verification mismatch: increment failure counter for IP
     * if (!code.equalsIgnoreCase(userInputCode)) {
     *     checkIpRateLimit(FAIL_LIMIT_PREFIX + clientIp, CacheConstant.CAPTCHA_VERIFY);
     *     throw new BusinessException(ErrorCode.CAPTCHA_VERIFY);
     * }
     *
     * // On success: evict failure counter to reset ban state
     * cache = cacheHelper.getCache(CacheConstant.CAPTCHA_VERIFY);
     * cache.evict(FAIL_LIMIT_PREFIX + clientIp);
     * }
     * </pre>
     * <p>
     * <strong>Security Considerations:</strong>
     * <ul>
     *     <li><strong>Case-Insensitive Comparison</strong>: Use {@code equalsIgnoreCase} to improve UX while maintaining security</li>
     *     <li><strong>Immediate Eviction</strong>: Evict code after retrieval (not after comparison) to prevent timing-based replay</li>
     *     <li><strong>Failure Counter Atomicity</strong>: {@link AtomicInteger} in cache ensures thread-safe increment across concurrent requests</li>
     *     <li><strong>IP Extraction</strong>: Use {@link RequestContextUtil} to handle proxy headers correctly for accurate rate limiting</li>
     *     <li><strong>Audit Logging</strong>: Consider logging verification failures (without storing user input) for security monitoring</li>
     * </ul>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Controller: Verify CAPTCHA before authentication
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
     *     } catch (BusinessException e) {
     *         // Return CAPTCHA-specific error to frontend
     *         return Result.fail(e.getErrorCode());
     *     }
     * }
     *
     * // Frontend: Handle verification errors
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
     *     if (error.code === ErrorCode.CAPTCHA_VERIFY.code) {
     *       message.error('Incorrect CAPTCHA');
     *       refreshCaptcha(); // Auto-refresh on failure
     *       form.captcha = ''; // Clear input field
     *     } else if (error.code === ErrorCode.CAPTCHA_VERIFY_BANNED.code) {
     *       message.error('Too many failed attempts, please try again later');
     *       // Optionally: disable form or show countdown timer
     *     }
     *   }
     * };
     * }
     * </pre>
     *
     * @param uuid          the session identifier used during CAPTCHA generation; must not be {@code null} or empty
     * @param userInputCode the user-provided code to verify; must not be {@code null} or empty
     * @throws BusinessException if verification fails (empty input, expired code, mismatch, or IP banned)
     * @see #checkVerifyFailLimit(String)
     * @see #checkIpRateLimit(String, String)
     * @see Cache#get(Object, Class)
     * @see Cache#evict(Object)
     */
    @Override
    public void verify(String uuid, String userInputCode) {
        // 1. Extract client IP for failure tracking and rate limiting
        String clientIp = RequestContextUtil.getContext().clientIp();

        // 2. Validate user input is non-empty (prevent trivial bypass)
        if (!StringUtils.hasText(userInputCode)) {
            throw new BusinessException(ErrorCode.CAPTCHA_VERIFY);
        }

        // 3. Check if IP has exceeded verification failure limit (temporary ban)
        checkVerifyFailLimit(clientIp);

        // 4. Retrieve stored code from cache
        Cache cache = cacheHelper.getCache(CacheConstant.CAPTCHA);
        String code = cache.get(uuid, String.class);

        // 5. Check if code exists (expired or already used)
        if (!StringUtils.hasText(code)) {
            throw new BusinessException(ErrorCode.CAPTCHA_EXPIRED);
        }

        // 6. Evict code immediately (one-time use policy)
        cache.evict(uuid);

        // 7. Perform case-insensitive comparison
        if (!code.equalsIgnoreCase(userInputCode)) {
            // On mismatch: increment failure counter for IP
            checkIpRateLimit(FAIL_LIMIT_PREFIX + clientIp, CacheConstant.CAPTCHA_VERIFY);
            throw new BusinessException(ErrorCode.CAPTCHA_VERIFY);
        }

        // 8. On success: evict failure counter cache to reset ban state
        cache = cacheHelper.getCache(CacheConstant.CAPTCHA_VERIFY);
        cache.evict(FAIL_LIMIT_PREFIX + clientIp);
    }

    /**
     * Checks if the client IP has exceeded the maximum allowed verification failures.
     * <p>
     * This method implements the failure-based rate limiting strategy:
     * <ul>
     *     <li>Retrieve failure counter from cache using {@code FAIL_LIMIT_PREFIX + clientIp} key</li>
     *     <li>If counter exists and >= {@value #FAIL_LIMIT_MAX}, throw ban exception</li>
     *     <li>If counter does not exist or < limit, allow verification to proceed</li>
     * </ul>
     * <p>
     * <strong>Cache Key Format:</strong>
     * <pre>
     * {@code "captcha:verify:fail:{clientIp}" }
     * </pre>
     * <p>
     * <strong>Ban Behavior:</strong>
     * <ul>
     *     <li>When limit is exceeded, {@link BusinessException} with {@link ErrorCode#CAPTCHA_VERIFY_BANNED} is thrown</li>
     *     <li>Ban persists until cache TTL expires (configured via {@link CacheConstant#CAPTCHA_VERIFY})</li>
     *     <li>Successful verification automatically evicts the failure counter to reset ban state</li>
     * </ul>
     * <p>
     * <strong>Security Considerations:</strong>
     * <ul>
     *     <li><strong>Progressive Penalties</strong>: Consider implementing exponential backoff instead of hard ban for better UX</li>
     *     <li><strong>IP Spoofing</strong>: Ensure {@link RequestContextUtil} correctly handles proxy headers to prevent IP spoofing</li>
     *     <li><strong>Monitoring</strong>: Log ban events for security analysis and potential IP reputation scoring</li>
     * </ul>
     *
     * @param clientIp the client IP address to check; must not be {@code null}
     * @throws BusinessException if failure count >= {@value #FAIL_LIMIT_MAX}
     * @see CacheConstant#CAPTCHA_VERIFY
     * @see #FAIL_LIMIT_MAX
     */
    private void checkVerifyFailLimit(String clientIp) {
        // Retrieve failure counter cache
        Cache cache = cacheHelper.getCache(CacheConstant.CAPTCHA_VERIFY);

        // Get current failure count for IP (null if not exists)
        Integer failCount = cache.get(FAIL_LIMIT_PREFIX + clientIp, Integer.class);

        // Check if limit exceeded
        if (failCount != null && failCount >= FAIL_LIMIT_MAX) {
            throw new BusinessException(ErrorCode.CAPTCHA_VERIFY_BANNED);
        }
    }

    /**
     * Increments and returns the rate limit counter for a given cache key.
     * <p>
     * This method implements atomic counter increment using {@link AtomicInteger} stored in cache:
     * <ul>
     *     <li>If counter does not exist, initialize to {@code new AtomicInteger(0)}</li>
     *     <li>Atomically increment counter via {@link AtomicInteger#incrementAndGet()}</li>
     *     <li>Return new counter value for limit comparison</li>
     * </ul>
     * <p>
     * <strong>Atomicity Guarantee:</strong>
     * <p>
     * Using {@link AtomicInteger} in cache ensures thread-safe increment across concurrent requests:
     * <pre>
     * {@code
     * // Atomic get-or-create + increment
     * AtomicInteger counter = cache.get(key, () -> new AtomicInteger(0));
     * return Objects.requireNonNull(counter).incrementAndGet();
     * }
     * </pre>
     * This prevents race conditions where multiple concurrent requests could read the same value
     * and increment incorrectly.
     * <p>
     * <strong>Cache TTL:</strong>
     * <p>
     * Counter entries expire based on the cache configuration for {@code cacheKey}:
     * <ul>
     *     <li>{@link CacheConstant#CAPTCHA_IP}: Typically 10 min for generation rate limiting</li>
     *     <li>{@link CacheConstant#CAPTCHA_VERIFY}: Typically 30 min for failure tracking</li>
     * </ul>
     * <p>
     * <strong>Usage Context:</strong>
     * <p>
     * Called from:
     * <ul>
     *     <li>{@link #generateCode}: To track CAPTCHA generation requests per IP</li>
     *     <li>{@link #verify}: To track verification failures per IP</li>
     * </ul>
     *
     * @param key      the full cache key (prefix + clientIp) for the counter
     * @param cacheKey the cache region name (e.g., {@link CacheConstant#CAPTCHA_IP})
     * @return the new counter value after increment
     * @see Cache#get(Object, java.util.concurrent.Callable)
     * @see AtomicInteger#incrementAndGet()
     */
    private int checkIpRateLimit(String key, String cacheKey) {
        // Get counter cache for specified region
        Cache cache = cacheHelper.getCache(cacheKey);

        // Atomic get-or-create + increment (thread-safe across concurrent requests)
        AtomicInteger counter = cache.get(key, () -> new AtomicInteger(0));
        return Objects.requireNonNull(counter).incrementAndGet();
    }

    /**
     * Configures HTTP response headers to prevent browser caching of CAPTCHA images.
     * <p>
     * This method sets the following headers:
     * <ul>
     *     <li><strong>Content-Type</strong>: {@code image/png} via {@link MediaType#IMAGE_PNG_VALUE}</li>
     *     <li><strong>Pragma</strong>: {@code no-cache} for HTTP/1.0 compatibility</li>
     *     <li><strong>Cache-Control</strong>: {@code no-cache, no-store, max-age=0} for HTTP/1.1</li>
     *     <li><strong>Expires</strong>: {@code 0} (epoch) to indicate immediate expiration</li>
     * </ul>
     * <p>
     * <strong>Security Rationale:</strong>
     * <p>
     * Preventing CAPTCHA image caching is critical for security:
     * <ul>
     *     <li><strong>Prevent Replay</strong>: Ensures each CAPTCHA request generates a fresh challenge</li>
     *     <li><strong>Avoid Stale Codes</strong>: Prevents browser from serving old CAPTCHA with expired server-side code</li>
     *     <li><strong>Rate Limiting Accuracy</strong>: Ensures each image request is counted for IP-based rate limiting</li>
     * </ul>
     * <p>
     * <strong>Browser Compatibility:</strong>
     * <p>
     * The combination of headers ensures compatibility across browsers and proxy caches:
     * <ul>
     *     <li>{@code Pragma: no-cache}: Legacy HTTP/1.0 support</li>
     *     <li>{@code Cache-Control: no-store}: Prevents any caching (strongest directive)</li>
     *     <li>{@code Expires: 0}: Fallback for older caches that ignore Cache-Control</li>
     * </ul>
     *
     * @param response the HTTP response to configure; must not be {@code null}
     * @see HttpServletResponse#setContentType(String)
     * @see HttpServletResponse#setHeader(String, String)
     * @see HttpServletResponse#setDateHeader(String, long)
     * @see HttpHeaders#PRAGMA
     * @see HttpHeaders#CACHE_CONTROL
     * @see HttpHeaders#EXPIRES
     */
    private void setNoCacheHeaders(HttpServletResponse response) {
        // Set image content type
        response.setContentType(MediaType.IMAGE_PNG_VALUE);

        // HTTP/1.0 compatibility header
        response.setHeader(HttpHeaders.PRAGMA, "no-cache");

        // HTTP/1.1 cache control: prevent any caching
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, max-age=0");

        // Expires header fallback for older caches
        response.setDateHeader(HttpHeaders.EXPIRES, 0);
    }

    /**
     * Validates UUID format and length for CAPTCHA session identification.
     * <p>
     * This method enforces:
     * <ul>
     *     <li><strong>Non-Empty</strong>: UUID must not be {@code null} or blank (via {@link StringUtils#hasText})</li>
     *     <li><strong>Length Limit</strong>: UUID must not exceed 64 characters to prevent memory exhaustion attacks</li>
     * </ul>
     * <p>
     * <strong>Validation Logic:</strong>
     * <pre>
     * {@code
     * if (!StringUtils.hasText(uuid) || uuid.length() > 64) {
     *     throw new BadRequestException(ErrorCode.QUERY_FORMAT, uuid);
     * }
     * }
     * </pre>
     * <p>
     * <strong>Security Rationale:</strong>
     * <ul>
     *     <li><strong>Injection Prevention</strong>: Length limit prevents excessively long keys that could cause cache memory exhaustion</li>
     *     <li><strong>Format Validation</strong>: Ensures UUID is present before proceeding with generation/verification</li>
     *     <li><strong>Early Failure</strong>: Validates input at method entry to fail fast and avoid unnecessary processing</li>
     * </ul>
     * <p>
     * <strong>Usage Context:</strong>
     * <p>
     * Called at the start of {@link #generateCode} to validate input before any expensive operations.
     * Not called in {@link #verify} because UUID is used directly as cache key; missing/invalid UUID
     * will naturally result in cache miss and {@link ErrorCode#CAPTCHA_EXPIRED}.
     *
     * @param uuid the UUID string to validate; may be {@code null} or empty
     * @throws BadRequestException if {@code uuid} is {@code null}, empty, or exceeds 64 characters
     * @see StringUtils#hasText(CharSequence)
     * @see ErrorCode#QUERY_FORMAT
     */
    private void validateUuid(String uuid) {
        // Validate non-empty and length limit (prevent memory exhaustion)
        if (!StringUtils.hasText(uuid) || uuid.length() > 64) {
            throw new BadRequestException(ErrorCode.QUERY_FORMAT, uuid);
        }
    }

}