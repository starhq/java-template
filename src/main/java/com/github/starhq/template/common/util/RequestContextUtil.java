package com.github.starhq.template.common.util;

import com.github.starhq.template.model.dto.RequestContext;
import lombok.experimental.UtilityClass;

/**
 * Thread-local utility for managing request-scoped context data.
 *
 * <p>In a typical layered architecture (Controller -> Service -> DAO), passing contextual data
 * (like UserId, TenantId, or TraceId) through method signatures creates boilerplate and tightly
 * couples layers. This utility leverages {@link ThreadLocal} to bind a {@link RequestContext} to
 * the current executing thread, allowing any downstream class to access request metadata implicitly.
 *
 * <p><b>Thread Safety:</b> Because it uses {@code ThreadLocal}, data stored here is inherently
 * isolated to the thread processing the specific HTTP request. Thread A cannot see Thread B's context.
 *
 * @author wangjian
 */
@UtilityClass
public class RequestContextUtil {

    /**
     * Thread-local storage for the request context.
     */
    private static final ThreadLocal<RequestContext> CONTEXT = new ThreadLocal<>();

    /**
     * Binds a request context to the current thread.
     *
     * <p>Typically invoked at the very beginning of a request lifecycle, such as in a
     * Servlet {@link jakarta.servlet.Filter} or a Spring {@code HandlerInterceptor.preHandle()} method.
     *
     * @param context the request context object containing metadata for the current request
     */
    public static void setContext(RequestContext context) {
        CONTEXT.set(context);
    }

    /**
     * Retrieves the request context bound to the current thread.
     *
     * <p>Can be safely called from any layer (Service, Mapper, etc.) without needing to pass
     * the context as a method parameter.
     *
     * @return the current thread's {@link RequestContext}, or {@code null} if no context has been set
     * or if called outside the scope of an HTTP request
     */
    public static RequestContext getContext() {
        return CONTEXT.get();
    }

    /**
     * Clears the request context from the current thread.
     *
     * <p><b>⚠️ CRITICAL REQUIREMENT:</b> This method <b>MUST</b> be invoked when the request
     * finishes processing (e.g., in a Filter's {@code finally} block or Spring Interceptor's
     * {@code afterCompletion}). Failing to clear the {@code ThreadLocal} in a web server environment
     * (like Tomcat) that uses a thread pool will cause severe issues:
     * <ul>
     *   <li><b>Memory Leaks:</b> The context object and its references cannot be garbage collected.</li>
     *   <li><b>Data Corruption:</b> The next HTTP request reused from the thread pool will inherit
     *       the previous request's context, leading to cross-user data leaks or security vulnerabilities.</li>
     * </ul>
     */
    public static void clear() {
        CONTEXT.remove();
    }
}