package com.github.starhq.template.common.constant;

import com.github.starhq.template.config.cache.CacheType;
import lombok.experimental.UtilityClass;

/**
 * Cache name constants.
 *
 * <p>Centralizes all cache namespace definitions used throughout the application.
 * Values are dynamically retrieved from {@link CacheType} to ensure consistency
 * between cache constants and the underlying cache configuration (e.g., TTL).
 *
 * @author wangjian
 * @see CacheType
 */
@UtilityClass
public class CacheConstant {

    /**
     * Cache namespace used for storing user basic information profiles.
     */
    public static final String USER = CacheType.USER.getCacheName();

    /**
     * Cache namespace dedicated to button-level permission metadata.
     */
    public static final String BUTTON = CacheType.BUTTON.getCacheName();

    /**
     * Cache namespace for routing menu trees and hierarchical structures.
     */
    public static final String MENU = CacheType.MENU.getCacheName();

    /**
     * Cache namespace for API resource definitions and permission mappings.
     */
    public static final String RESOURCE = CacheType.RESOURCE.getCacheName();

    /**
     * Cache namespace used to store generated graphical captcha text or base64 data.
     */
    public static final String CAPTCHA = CacheType.CAPTCHA.getCacheName();

    /**
     * Cache namespace for tracking and rate-limiting client IP addresses during captcha requests.
     */
    public static final String CAPTCHA_IP = CacheType.CAPTCHA_IP.getCacheName();

    /**
     * Cache namespace for temporarily storing the correct captcha answer
     * pending user verification.
     */
    public static final String CAPTCHA_VERIFY = CacheType.CAPTCHA_VERIFY.getCacheName();

    /**
     * Cache namespace for storing aggregated user permission strings (e.g., 'sys:user:add,sys:user:query').
     */
    public static final String PERMISSION = CacheType.PERMISSION.getCacheName();

    /**
     * Cache namespace for persisting active user session tokens (e.g., JWT).
     */
    public static final String TOKEN = CacheType.TOKEN.getCacheName();

    /**
     * Cache namespace used for caching the mapping between admin user IDs and their usernames.
     */
    public static final String ADMIN_NAME = CacheType.ADMIN_NAME.getCacheName();

    /**
     * Cache namespace dedicated to system dictionary type definitions.
     */
    public static final String DICT_TYPE = CacheType.DICT_TYPE.getCacheName();

}
