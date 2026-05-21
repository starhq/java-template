package com.github.starhq.template.common.constant;

/**
 * Constants specific to testing configurations.
 *
 * <p>Centralizes test environment variables to avoid hardcoding values
 * in unit and integration test cases, ensuring consistency across the test suite.
 *
 * @author wangjian
 */
public interface TestConstant {

    /**
     * Base API version path used in test requests.
     *
     * <p>Typically prepended to MockMvc request URIs (e.g., {@code /v1/users})
     * to verify version-specific controller endpoints.
     */
    String VERSION = "/v1";
}