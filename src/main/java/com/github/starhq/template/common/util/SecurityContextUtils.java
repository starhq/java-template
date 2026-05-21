package com.github.starhq.template.common.util;

import com.github.starhq.template.common.enums.ErrorCode;
import com.github.starhq.template.common.exception.CustomException;
import com.github.starhq.template.entity.SysUser;
import lombok.experimental.UtilityClass;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

/**
 * Utility class for interacting with the Spring Security {@link org.springframework.security.core.context.SecurityContext}.
 *
 * <p>Provides a type-safe and convenient API to extract the current user's identity, ID, and permissions,
 * abstracting away the raw Spring Security {@link Authentication} object from the business layer.
 *
 * <p><b>Design Philosophy:</b> Methods are split into two categories:
 * <ul>
 *   <li><b>Strict methods (e.g., {@link #getRequiredUserId()}):</b> Throw exceptions if the user is
 *       not authenticated. Used for core business operations where identity is mandatory.</li>
 *   <li><b>Safe methods (e.g., {@link #getUserIdOrNull()}):</b> Return {@code null} or {@code Optional}
 *       if the user is not authenticated. Used for non-critical paths like logging, optional UI enhancements,
 *       or async thread fallbacks.</li>
 * </ul>
 *
 * @author starhq
 */
@UtilityClass
public class SecurityContextUtils {

    /**
     * Retrieves the raw Spring Security {@link Authentication} object from the current thread.
     *
     * <p><b>Usage Warning:</b> This is the lowest-level extraction method. Directly using the returned
     * {@code Authentication} in business logic is highly discouraged, as it tightly couples your code
     * to Spring Security internals. Prefer using {@link #getCurrentUserDetails()} instead.
     *
     * <p><b>Security Filter:</b> Implicitly filters out {@link AnonymousAuthenticationToken}. Spring Security
     * populates the context with an anonymous token for unauthenticated requests to allow authorization
     * rules to evaluate safely. Excluding it here prevents the business layer from mistakenly treating
     * an anonymous user as an authenticated one.
     *
     * @return an {@link Optional} containing the authenticated {@link Authentication}, or empty if not logged in
     */
    public static Optional<Authentication> getCurrentAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return Optional.ofNullable(authentication)
                .filter(auth -> !(auth instanceof AnonymousAuthenticationToken));
    }

    /**
     * Retrieves the core {@link UserDetails} entity for the currently logged-in user.
     *
     * <p>This method performs strict validation: it ensures the authentication is not anonymous,
     * is explicitly marked as authenticated, and that the principal is actually an instance of
     * {@link UserDetails} before safely casting it.
     *
     * @return an {@link Optional} containing the {@link UserDetails}, or empty if not logged in or invalid
     */
    public static Optional<UserDetails> getCurrentUserDetails() {
        return getCurrentAuthentication()
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getPrincipal)
                .filter(UserDetails.class::isInstance)
                .map(UserDetails.class::cast);
    }

    /**
     * Retrieves the current user's username. Strictly requires the user to be logged in.
     *
     * @return the username string
     * @throws CustomException with {@link ErrorCode#UNAUTHORIZED} if no valid user context is found
     */
    public static String getCurrentUsername() {
        return getCurrentUserDetails()
                .map(UserDetails::getUsername)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED));
    }

    /**
     * Safely retrieves the current user's username without throwing exceptions.
     *
     * <p>Use this in scenarios where knowing the user is optional, such as recording non-critical
     * audit logs for public APIs, or rendering personalized UI components for guest users.
     *
     * @return the username string, or {@code null} if the user is not logged in
     */
    public static String getUsernameOrNull() {
        return getCurrentUserDetails()
                .map(UserDetails::getUsername)
                .orElse(null);
    }

    /**
     * Safely retrieves the current user's database ID, cast specifically to {@link SysUser}.
     *
     * <p><b>Design Note:</b> Because {@link UserDetails} is an interface, the actual principal
     * object might be a custom implementation (like {@code SysUser}). This method safely checks
     * the instance type before casting to avoid {@link ClassCastException}.
     *
     * <p><b>Typical Use Cases:</b>
     * <ul>
     *   <li>Fallback logic in unit tests when SecurityContext is not mocked.</li>
     *   <li>Extracting user ID in asynchronous threads (e.g., {@code @Async}) where the
     *       SecurityContext is typically not propagated automatically.</li>
     *   <li>Non-critical logging where a missing ID should not stop execution.</li>
     * </ul>
     *
     * @return the user's ID as a {@link Long}, or {@code null} if not logged in or if the principal is not a {@link SysUser}
     */
    public static Long getUserIdOrNull() {
        return getCurrentUserDetails()
                .filter(SysUser.class::isInstance)
                .map(SysUser.class::cast)
                .map(SysUser::getId)
                .orElse(null);
    }

    /**
     * Retrieves the current user's database ID. Strictly requires the user to be logged in.
     *
     * <p>Use this for core business logic where an authenticated identity is a hard prerequisite
     * (e.g., placing an order, changing a password, deleting personal data).
     *
     * @return the user's ID as a {@link Long}
     * @throws CustomException with {@link ErrorCode#UNAUTHORIZED} if no valid user context or ID is found
     */
    public static Long getRequiredUserId() {
        Long userId = getUserIdOrNull();
        if (userId == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED);
        }
        return userId;
    }

    /**
     * Retrieves the collection of authorities (roles/permissions) granted to the current user.
     *
     * @return a collection of {@link GrantedAuthority}
     * @throws CustomException with {@link ErrorCode#UNAUTHORIZED} if no valid user context is found
     */
    public static Collection<? extends GrantedAuthority> getCurrentAuthorities() {
        return getCurrentUserDetails()
                .map(UserDetails::getAuthorities)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED));
    }

    /**
     * Checks if the currently logged-in user holds a specific role or permission authority.
     *
     * <p>This provides a programmatic way to perform authorization checks directly in the code,
     * serving as an alternative to using {@code @PreAuthorize("hasRole('ROLE_ADMIN')")} annotations.
     * Useful when the permission string is dynamically determined at runtime.
     *
     * @param roleCode the exact string representation of the authority (e.g., "ROLE_ADMIN" or "sys:user:delete")
     * @return {@code true} if the user has the specified authority, {@code false} otherwise
     * @throws CustomException indirectly via {@link #getCurrentAuthorities()} if the user is not logged in
     */
    public static boolean hasRole(String roleCode) {
        return getCurrentAuthorities().stream()
                .anyMatch(authority -> Objects.equals(authority.getAuthority(), roleCode));
    }
}