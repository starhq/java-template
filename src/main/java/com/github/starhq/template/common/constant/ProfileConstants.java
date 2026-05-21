package com.github.starhq.template.common.constant;

/**
 * Constants representing active Spring profiles.
 *
 * <p>Used primarily in conditional logic (e.g., {@code @Profile} annotations,
 * configuration properties, or custom deployment strategies) to determine
 * the current runtime environment and load environment-specific beans.
 *
 * @author wangjian
 * @see org.springframework.context.annotation.Profile
 */
public interface ProfileConstants {

    /**
     * Profile expression indicating any environment <b>except</b> the development environment.
     *
     * <p>Commonly used in Spring's {@code @Profile} annotation to exclude beans
     * from local development, such as {@code @Profile(NON_DEV)}.
     */
    String NON_DEV = "!dev";

    /**
     * Profile identifier for the local development environment.
     */
    String DEV = "dev";

    /**
     * Profile identifier for the testing/staging environment.
     */
    String TEST = "test";

    /**
     * Profile identifier for the production environment.
     */
    String PROD = "prod";
}
