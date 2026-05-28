package com.github.starhq.template.config.security;

import com.github.starhq.template.common.captcha.CircleCaptcha;
import com.github.starhq.template.common.captcha.ICaptcha;
import com.github.starhq.template.common.captcha.LineCaptcha;
import com.github.starhq.template.common.captcha.ShearCaptcha;
import com.github.starhq.template.common.constant.ProfileConstants;
import com.github.starhq.template.common.enums.ErrorCode;
import com.github.starhq.template.common.exception.BusinessException;
import com.github.starhq.template.config.messages.MessageUtils;
import com.github.starhq.template.config.security.filter.JwtAuthenticationFilter;
import com.github.starhq.template.config.security.filter.RequestResponseLoggingFilter;
import com.github.starhq.template.config.security.filter.ResourceFilter;
import com.github.starhq.template.config.security.handler.CustomAccessDeniedHandler;
import com.github.starhq.template.config.security.handler.CustomAuthenticationEntryPoint;
import com.github.starhq.template.config.security.handler.CustomLogoutHandler;
import com.github.starhq.template.config.security.jwt.JwtService;
import com.github.starhq.template.config.security.properties.CaptchaProperties;
import com.github.starhq.template.config.security.properties.CorsProperties;
import com.github.starhq.template.config.security.properties.JwtProperties;
import com.github.starhq.template.config.security.properties.WhiteListProperties;
import com.github.starhq.template.event.EventService;
import com.github.starhq.template.helper.CacheHelper;
import com.github.starhq.template.helper.LoggerJsonSensitiveHelper;
import com.github.starhq.template.service.ResourceService;
import com.github.starhq.template.service.TokenService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.json.JsonMapper;

/**
 * Main Spring Security configuration class.
 *
 * <p>This class acts as the central brain of the security architecture. It chains together custom filters,
 * configures stateless session management, defines endpoint permissions, and wires up cryptographic beans.
 *
 * <p><b>Architecture Paradigm:</b> This application implements a <b>Stateless JWT Architecture</b>. By disabling sessions
 * ({@code .sessionManagement(...)}), the server does not keep any in-memory state between requests.
 * Every request must carry a valid JWT. This is essential for horizontal scalability in microservices
 * and eliminates the need for session replication in clustered environments.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Builds the main HTTP Security filter chain.
     *
     * <p>This method assembles the exact sequence in which HTTP requests pass through our custom filters.
     * Order is critical:
     * {@code RequestResponseLoggingFilter -> JwtAuthenticationFilter -> UsernamePasswordAuthenticationFilter -> ResourceFilter -> Controller}.
     *
     * @param http                         The HTTP security builder.
     * @param corsConfigurationSource      Injected CORS config to prevent cross-origin issues.
     * @param whiteListProperties          Injected list of paths that skip authentication.
     * @param authenticationProvider       Injected provider delegating to our {@link UserDetailsService}.
     * @param entryPoint                   Injected handler for 401 Unauthorized responses.
     * @param accessDeniedHandler          Injected handler for 403 Forbidden responses.
     * @param logoutHandler                Injected handler to clear sessions and DB tokens.
     * @param jwtAuthenticationFilter      Injected filter for JWT validation.
     * @param requestResponseLoggingFilter Injected filter for audit logging.
     * @param resourceFilter               Injected filter for dynamic DB-driven authorization.
     * @return The fully constructed {@link SecurityFilterChain}.
     * @throws Exception if configuration fails.
     */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            CorsConfigurationSource corsConfigurationSource,
                                            WhiteListProperties whiteListProperties,
                                            AuthenticationProvider authenticationProvider,
                                            AuthenticationEntryPoint entryPoint,
                                            AccessDeniedHandler accessDeniedHandler,
                                            LogoutHandler logoutHandler,
                                            JwtAuthenticationFilter jwtAuthenticationFilter,
                                            RequestResponseLoggingFilter requestResponseLoggingFilter,
                                            ResourceFilter resourceFilter) throws IllegalStateException {
        http
                // Register CORS configuration (must be registered early, ideally via Nginx instead for pure backend setups)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                // Disable CSRF protection.
                // Since we use JWT tokens (stateless), there is no session cookie to steal. Enabling CSRF would
                // force the frontend to send CSRF tokens, which breaks stateless API clients like Postman or mobile apps.
                .csrf(AbstractHttpConfigurer::disable)
                // Disable "anonymousUser" autologin.
                // Ensures SecurityContext remains empty if no filter populates it.
                .anonymous(AbstractHttpConfigurer::disable)
                // Configure strict session management.
                // STATELESS means Spring will never use JSESSIONID cookies. This is mandatory for pure REST APIs.
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Define authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Spring Boot default error endpoint (usually forwards to /error)
                        .requestMatchers("/error").permitAll()
                        // Actuator health checks
                        .requestMatchers("/actuator/health").permitAll()
                        // Actuator all endpoints (requires ACTUATOR_ADMIN role to view raw metrics data)
                        .requestMatchers("/actuator").hasRole("ACTUATOR_ADMIN")
                        // Apply whitelist bypass
                        .requestMatchers(whiteListProperties.getWhiteList().toArray(new String[0])).permitAll()
                        // Catch-all: everything else requires authentication
                        .anyRequest().authenticated())
                // Plug our custom authentication provider into Spring Security's authentication mechanism
                .authenticationProvider(authenticationProvider)
                // Interceptor Chain Registration (Strict Order Matters!)
                // 1. Log Request/Response (must be first to capture raw data before security modifies it)
                .addFilterBefore(requestResponseLoggingFilter, UsernamePasswordAuthenticationFilter.class)
                // 2. Validate JWT and populate SecurityContext (must be before Spring Security's auth check)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // 3. Dynamic DB Authorization (must be after JWT so it can read SecurityContext)
                .addFilterAfter(resourceFilter, JwtAuthenticationFilter.class)
                // Configure custom exception handlers to return JSON instead of 302 redirects
                .exceptionHandling(e -> e.authenticationEntryPoint(entryPoint).accessDeniedHandler(accessDeniedHandler))
                // Configure logout behavior
                .logout(l -> l.logoutUrl("/api/v1/auth/logout").addLogoutHandler(logoutHandler)
                        // The default Spring Security logout handler clears the SecurityContext. We duplicate it here
                        // to ensure absolute cleanup in case custom handlers fail before execution.
                        .logoutSuccessHandler((req, resp, auth) -> SecurityContextHolder.clearContext()));

        return http.build();
    }

    // ==========================================
    // 1. Authentication Mechanism
    // ==========================================

    /**
     * Configures the provider responsible for verifying user credentials against the database.
     *
     * <p>We use {@link DaoAuthenticationProvider} because our architecture queries the database
     * via {@link UserDetailsService} to load the user object, rather than relying on an in-memory map.
     */
    @Bean
    AuthenticationProvider authenticationProvider(PasswordEncoder passwordEncoder, UserDetailsService userDetailsService) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    /**
     * Exposes the global {@link AuthenticationManager} bean.
     * This manager coordinates the configured {@link AuthenticationProvider} to perform actual
     * authentication logic.
     */
    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Provides the password hashing bean.
     *
     * <p><b>Dynamic Strength Logic:</b> Dynamically adjusts the BCrypt strength (rounds) based on server CPU cores.
     * High-core servers (e.g., 16 cores) can handle strength 12 securely without noticeable latency,
     * while low-core servers (e.g., 2 cores) default to strength 10 to prevent login API timeouts.
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        int strength = Runtime.getRuntime().availableProcessors() > 8 ? 12 : 10;
        return new BCryptPasswordEncoder(strength);
    }

    // ==========================================
    // 2.JWT & Filter Beans
    // ==========================================

    /**
     * Factory method for the JWT service.
     */
    @Bean
    JwtService jwtService(JwtProperties jwtProperties) {
        return new JwtService(jwtProperties);
    }

    /**
     * Factory method for the JWT authentication filter.
     */
    @Bean
    JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService, TokenService tokenService, WhiteListPathMatcher whiteListPathMatcher, JsonMapper jsonMapper, MessageUtils messageUtils) {
        return new JwtAuthenticationFilter(jwtService, userDetailsService, tokenService, whiteListPathMatcher, jsonMapper, messageUtils);
    }

    /**
     * Factory method for the request/response logging filter.
     */
    @Bean
    RequestResponseLoggingFilter requestResponseLoggingFilter(LoggerJsonSensitiveHelper sensitiveHelper, Environment environment, EventService eventService) {
        boolean isProd = environment.acceptsProfiles(Profiles.of(ProfileConstants.PROD));
        return new RequestResponseLoggingFilter(sensitiveHelper, eventService, isProd);
    }

    @Bean
    WhiteListPathMatcher whiteListPathMatcher(WhiteListProperties whiteListProperties) {
        return new WhiteListPathMatcher(whiteListProperties);
    }

    /**
     * Factory method for the dynamic resource authorization filter.
     */
    @Bean
    ResourceFilter resourceFilter(ResourceService resourceService, WhiteListPathMatcher whiteListPathMatcher, CacheHelper cacheHelper, JsonMapper jsonMapper) {
        return new ResourceFilter(resourceService, whiteListPathMatcher, cacheHelper, jsonMapper);
    }

    // ==========================================
    // 3. Custom Security Handlers(401 & 403)
    // ==========================================

    /**
     * Factory method for the logout handler.
     */
    @Bean
    LogoutHandler logoutService(TokenService tokenService) {
        return new CustomLogoutHandler(tokenService);
    }

    /**
     * Factory method for the 403 Forbidden handler.
     */
    @Bean
    AccessDeniedHandler accessDeniedService(MessageUtils messageUtils, JsonMapper jsonMapper) {
        return new CustomAccessDeniedHandler(messageUtils, jsonMapper);
    }

    /**
     * Factory method for the 401 Unauthorized entry point.
     */
    @Bean
    AuthenticationEntryPoint customAuthenticationEntryPoint(MessageUtils messageUtils, JsonMapper jsonMapper) {
        return new CustomAuthenticationEntryPoint(messageUtils, jsonMapper);
    }

    // ==========================================
    // 4. Infrastructure Beans(CORS &Captcha)
    // ==========================================

    /**
     * Dynamically constructs the CORS configuration based on properties.
     * Separating this into a bean makes it easier to manage multiple origin patterns
     * without cluttering the main security chain.
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource(CorsProperties corsProperties) {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOriginPatterns(corsProperties.getAllowedOriginPatterns());
        configuration.setAllowedMethods(corsProperties.getAllowedMethods());
        configuration.setAllowedHeaders(corsProperties.getAllowedHeaders());
        configuration.setAllowCredentials(corsProperties.isAllowCredentials());
        configuration.setMaxAge(corsProperties.getMaxAge());

        // Map the configuration to all endpoints
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Dynamic factory for the Captcha generator based on configuration properties.
     *
     * @param properties injected configuration dictating the visual style of the captcha
     * @return an implementation of {@link ICaptcha}
     * @throws BusinessException if an unsupported captcha type is configured
     */
    @Bean
    ICaptcha captcha(CaptchaProperties properties) {
        String type = properties.getType();

        return switch (type) {
            case "line" -> new LineCaptcha(properties.getWidth(), properties.getHeight(), properties.getCodeCount(), properties.getInterfereCount());
            case "circle" -> new CircleCaptcha(properties.getWidth(), properties.getHeight(), properties.getCodeCount(), properties.getInterfereCount());
            case "shear" -> new ShearCaptcha(properties.getWidth(), properties.getHeight(), properties.getCodeCount(), properties.getInterfereCount());
            default -> throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        };

    }
}