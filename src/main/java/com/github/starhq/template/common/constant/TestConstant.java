package com.github.starhq.template.common.constant;

import lombok.experimental.UtilityClass;

/**
 * Constants specific to testing configurations.
 *
 * <p>Centralizes test environment variables to avoid hardcoding values
 * in unit and integration test cases, ensuring consistency across the test suite.
 *
 * @author wangjian
 */
@UtilityClass
public class TestConstant {

    /**
     * Base API version path used in test requests.
     *
     * <p>Typically prepended to MockMvc request URIs (e.g., {@code /v1/users})
     * to verify version-specific controller endpoints.
     */
    public static final String VERSION = "/v1";
}