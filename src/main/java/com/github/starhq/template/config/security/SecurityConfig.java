package com.github.starhq.template.config.security;

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

import tools.jackson.databind.json.JsonMapper;

/**
 * Main Spring Security configuration for the application.
 * Configures authentication, authorization, session management, and custom
 * filters.
 */
@Configuration
@EnableWebSecurity // Enables Spring Security's web security features
public class SecurityConfig {

    /**
     * Defines the security filter chain. This is the core of Spring Security
     * configuration.
     *
     * @param http The HttpSecurity object to configure web security.
     * @return The configured SecurityFilterChain.
     * @throws Exception If an error occurs during configuration.
     */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, CorsConfigurationSource corsConfigurationSource,
                                            WhiteListProperties whiteListProperties, // Injected from context
                                            AuthenticationProvider authenticationProvider, // Injected from @Bean below
                                            AuthenticationEntryPoint entryPoint, // Injected from context
                                            AccessDeniedHandler accessDeniedHandler, // Injected from context
                                            LogoutHandler logoutHandler, // Injected from context
                                            JwtAuthenticationFilter jwtAuthenticationFilter, // Injected from context
                                            RequestResponseLoggingFilter requestResponseLoggingFilter, // Injected from context
                                            ResourceFilter resourceFilter // Injected from context
    ) throws Exception {
        http
                // Disable CORS (if handled externally or not needed for this API).
                // For production, consider a proper CORS configuration using
                // CorsConfigurationSource.
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                // Disable CSRF (Cross-Site Request Forgery) for stateless APIs.
                // This is common for REST APIs that rely on tokens (like JWTs) rather than
                // sessions.
                .csrf(AbstractHttpConfigurer::disable)
                // Disable anonymous authentication. All requests will either be explicitly
                // authenticated
                // by a filter, or explicitly permitted by authorizeHttpRequests.
                .anonymous(AbstractHttpConfigurer::disable)
                // Configure stateless session management.
                // JWTs are stateless, so no session should be created or managed by Spring
                // Security.
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Configure authorization rules for HTTP requests.
                .authorizeHttpRequests(auth -> auth
                        // Permit access to the Spring Boot default error page.
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/actuator").hasRole("ACTUATOR_ADMIN")
                        // Permit access to paths defined in WhiteListProperties.
                        // These paths bypass Spring Security's authentication checks.
                        .requestMatchers(whiteListProperties.getWhiteList().toArray(new String[0])).permitAll()
                        // All other requests require authentication.
                        .anyRequest().authenticated())
                // Set the custom authentication provider that handles user login logic.
                .authenticationProvider(authenticationProvider)
                // Add the RequestResponseLoggingFilter to the Spring Security filter chain.
                // This filter logs incoming and outgoing requests and responses.
                // This is useful for debugging and monitoring.
                // It's placed before UsernamePasswordAuthenticationFilter to log before
                // authentication.
                .addFilterBefore(requestResponseLoggingFilter, UsernamePasswordAuthenticationFilter.class)
                // Add the JWT authentication filter to the Spring Security filter chain.
                // It's placed before UsernamePasswordAuthenticationFilter to process JWTs
                // first.
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // Add the Resource filter after the JWT filter.
                // This ensures that authentication context is available for resource-level
                // authorization checks.
                .addFilterAfter(resourceFilter, JwtAuthenticationFilter.class)
                // Configure exception handling for authentication and access denied scenarios.
                // Custom entry points and handlers provide specific JSON responses.
                .exceptionHandling(e -> e.authenticationEntryPoint(entryPoint).accessDeniedHandler(accessDeniedHandler))
                // Configure logout handling.
                // The specified logout URL triggers the CustomLogoutHandler.
                .logout(l -> l.logoutUrl("/api/v1/auth/logout").addLogoutHandler(logoutHandler)
                        .logoutSuccessHandler((req, resp, auth) -> SecurityContextHolder.clearContext()));

        return http.build();
    }

    // --- Authentication and Authorization Beans ---

    /**
     * Configures the AuthenticationProvider which is responsible for fetching user
     * details
     * and performing password verification.
     * Uses DaoAuthenticationProvider with the custom UserDetailsService and
     * PasswordEncoder.
     *
     * @param passwordEncoder    The PasswordEncoder bean for password verification.
     * @param userDetailsService The UserDetailsService bean for loading user
     *                           details.
     * @return A configured AuthenticationProvider.
     */
    @Bean
    AuthenticationProvider authenticationProvider(PasswordEncoder passwordEncoder,
                                                  UserDetailsService userDetailsService) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    /**
     * Exposes the AuthenticationManager bean.
     * The AuthenticationManager coordinates with AuthenticationProviders to
     * authenticate users.
     *
     * @param config The AuthenticationConfiguration to get the manager from.
     * @return The configured AuthenticationManager.
     * @throws Exception If an error occurs retrieving the manager.
     */
    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Provides a BCryptPasswordEncoder bean for password hashing.
     * The strength of the encoder is dynamically set based on available CPU cores.
     *
     * @return A BCryptPasswordEncoder instance.
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        // Adjust BCrypt strength based on CPU cores for performance vs. security
        // balance.
        // A strength of 10-12 is generally recommended.
        int strength = Runtime.getRuntime().availableProcessors() > 8 ? 12 : 10;
        return new BCryptPasswordEncoder(strength);
    }

    // --- JWT Related Beans ---

    /**
     * Provides the JwtService bean for handling JWT token creation and parsing.
     *
     * @param jwtProperties The JwtProperties bean containing JWT configuration
     *                      (e.g., secret key, expiration times).
     * @return A JwtService instance.
     */
    @Bean
    JwtService jwtService(JwtProperties jwtProperties) {
        return new JwtService(jwtProperties);
    }

    /**
     * Provides the JwtAuthenticationFilter bean.
     * This filter intercepts requests to validate JWT tokens and set up the
     * SecurityContext.
     *
     * @param jwtService         The JwtService for token operations.
     * @param userDetailsService The UserDetailsService for loading user details
     *                           from the token's subject.
     * @param tokenService       The TokenService for database-side token
     *                           validation (e.g., revocation, session expiry).
     * @return A JwtAuthenticationFilter instance.
     */
    @Bean
    JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService,
                                                    TokenService tokenService, WhiteListPathMatcher whiteListPathMatcher, JsonMapper jsonMapper, MessageUtils messageUtils) {
        return new JwtAuthenticationFilter(jwtService, userDetailsService, tokenService, whiteListPathMatcher,
                jsonMapper, messageUtils);
    }

    /**
     * Provides the RequestResponseLoggingFilter bean.
     * This filter logs incoming and outgoing HTTP requests and responses.
     *
     * @param environment The Environment for application profiles.
     * @return A RequestResponseLoggingFilter instance.
     */
    @Bean
    RequestResponseLoggingFilter requestResponseLoggingFilter(LoggerJsonSensitiveHelper sensitiveHelper,
                                                              Environment environment, EventService eventService) {
        boolean isProd = environment.acceptsProfiles(Profiles.of(ProfileConstants.PROD));
        return new RequestResponseLoggingFilter(sensitiveHelper, eventService, isProd);
    }

    @Bean
    WhiteListPathMatcher whiteListPathMatcher(WhiteListProperties whiteListProperties) {
        return new WhiteListPathMatcher(whiteListProperties);
    }

    /**
     * Provides the ResourceFilter bean.
     * This filter performs fine-grained resource-level authorization based on
     * stored resource definitions
     * and user permissions, typically after initial authentication.
     *
     * @param resourceService The ResourceService to fetch resource definitions.
     * @return A ResourceFilter instance.
     */
    @Bean
    ResourceFilter resourceFilter(ResourceService resourceService, WhiteListPathMatcher whiteListPathMatcher,
                                  CacheHelper cacheHelper, JsonMapper jsonMapper) {
        return new ResourceFilter(resourceService, whiteListPathMatcher, cacheHelper, jsonMapper);
    }

    // --- Custom Handler Beans ---

    /**
     * Provides the CustomLogoutHandler bean.
     * This handler is responsible for invalidating tokens in the database and
     * clearing
     * the security context during the logout process.
     *
     * @param tokenService The TokenService for managing database tokens.
     * @return A CustomLogoutHandler instance.
     */
    @Bean
    LogoutHandler logoutService(TokenService tokenService) {
        return new CustomLogoutHandler(tokenService);
    }

    /**
     * Provides the CustomAccessDeniedHandler bean.
     * This handler is invoked by Spring Security when an authenticated user tries
     * to access
     * a resource they are not authorized for (resulting in a 403 Forbidden
     * response).
     * It formats a custom JSON error response.
     *
     * @param messageUtils The MessageUtils for internationalized error messages.
     * @param jsonMapper   The Environment for application profiles.
     * @return A CustomAccessDeniedHandler instance.
     */
    @Bean
    AccessDeniedHandler accessDeniedService(MessageUtils messageUtils, JsonMapper jsonMapper) {
        return new CustomAccessDeniedHandler(messageUtils, jsonMapper);
    }

    /**
     * Provides the CustomAuthenticationEntryPoint bean.
     * This entry point is invoked by Spring Security when an unauthenticated user
     * attempts
     * to access a protected resource (resulting in a 401 Unauthorized response).
     * It formats a custom JSON error response.
     *
     * @param messageUtils The MessageUtils for internationalized error messages.
     * @param jsonMapper   The Environment for application profiles.
     * @return A CustomAuthenticationEntryPoint instance.
     */
    @Bean
    AuthenticationEntryPoint customAuthenticationEntryPoint(MessageUtils messageUtils, JsonMapper jsonMapper) {
        return new CustomAuthenticationEntryPoint(messageUtils, jsonMapper);
    }

    // ✅ 3. 动态构建 CorsConfigurationSource
    @Bean
    CorsConfigurationSource corsConfigurationSource(CorsProperties corsProperties) {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOriginPatterns(corsProperties.getAllowedOriginPatterns());
        configuration.setAllowedMethods(corsProperties.getAllowedMethods());
        configuration.setAllowedHeaders(corsProperties.getAllowedHeaders());
        configuration.setAllowCredentials(corsProperties.isAllowCredentials());
        configuration.setMaxAge(corsProperties.getMaxAge());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    ICaptcha captcha(CaptchaProperties properties) {
        String type = properties.getType();

        return switch (type) {
            case "line" -> new LineCaptcha(properties.getWidth(), properties.getHeight(), properties.getCodeCount(),
                    properties.getInterfereCount());
            case "circle" -> new CircleCaptcha(properties.getWidth(), properties.getHeight(), properties.getCodeCount(),
                    properties.getInterfereCount());
            case "shear" -> new ShearCaptcha(properties.getWidth(), properties.getHeight(), properties.getCodeCount(),
                    properties.getInterfereCount());
            default -> throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        };

    }
}