package com.github.starhq.template.config.security;

import com.github.starhq.template.config.security.properties.WhiteListProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;

/**
 * Utility class for evaluating whether an incoming HTTP request path should bypass security checks.
 *
 * <p>This class is typically used in custom filters (like {@link com.github.starhq.template.config.security.filter.JwtAuthenticationFilter})
 * to short-circuit the authentication process for public APIs (e.g., login, public downloads)
 * before hitting the database or JWT parser.
 *
 * @author starhq
 */
@RequiredArgsConstructor
public class WhiteListPathMatcher {

    /**
     * Injected configuration containing the list of allowed public paths.
     */
    private final WhiteListProperties whiteListProperties;

    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    /**
     * Evaluates if a given request path should be considered public (whitelisted).
     *
     * <p><b>Algorithm Choice:</b> This method uses a simple {@code String.contains()} check against the configured list.
     * <b>Trade-offs:</b>
     * <ul>
     * <li><b>Performance:</b> {@code anyMatch + contains} is extremely fast and has zero overhead for typical white lists.
     * <li><b>Strictness:</b> It uses prefix matching (e.g., "/api/public/" matches "/api/public/user"). It does <b>NOT</b> support
     * deep AntPath matching (e.g., it will incorrectly match "/api/public-test" against "/api/public/**").
     * This is generally acceptable for security filters because we only use it for explicitly defined public endpoints,
     * not for wild-card-based route patterns.</li>
     * </ul>
     *
     * @param requestPath the URI path of the incoming HTTP request (e.g., "/api/public/captcha")
     * @return {@code true} if the path starts with any string in the whitelist, {@code false} otherwise
     */
    public boolean isWhiteListPath(String requestPath) {
        // Guard clause: If no whitelist is configured at all, reject everything to fail-safe
        if (CollectionUtils.isEmpty(whiteListProperties.getWhiteList())) {
            return false;
        }

        // Perform prefix matching against the YAML list
        return whiteListProperties.getWhiteList().stream()
                .anyMatch((white) -> MATCHER.match(white, requestPath));
    }
}