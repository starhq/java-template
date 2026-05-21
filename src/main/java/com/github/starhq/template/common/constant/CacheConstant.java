package com.github.starhq.template.common.constant;

import com.github.starhq.template.config.cache.CacheType;

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
public interface CacheConstant {

    /**
     * Cache namespace used for storing user basic information profiles.
     */
    String USER = CacheType.USER.getCacheName();

    /**
     * Cache namespace dedicated to button-level permission metadata.
     */
    String BUTTON = CacheType.BUTTON.getCacheName();

    /**
     * Cache namespace for routing menu trees and hierarchical structures.
     */
    String MENU = CacheType.MENU.getCacheName();

    /**
     * Cache namespace for API resource definitions and permission mappings.
     */
    String RESOURCE = CacheType.RESOURCE.getCacheName();

    /**
     * Cache namespace used to store generated graphical captcha text or base64 data.
     */
    String CAPTCHA = CacheType.CAPTCHA.getCacheName();

    /**
     * Cache namespace for tracking and rate-limiting client IP addresses during captcha requests.
     */
    String CAPTCHA_IP = CacheType.CAPTCHA_IP.getCacheName();

    /**
     * Cache namespace for temporarily storing the correct captcha answer
     * pending user verification.
     */
    String CAPTCHA_VERIFY = CacheType.CAPTCHA_VERIFY.getCacheName();

    /**
     * Cache namespace for storing aggregated user permission strings (e.g., 'sys:user:add,sys:user:query').
     */
    String PERMISSION = CacheType.PERMISSION.getCacheName();

    /**
     * Cache namespace for persisting active user session tokens (e.g., JWT).
     */
    String TOKEN = CacheType.TOKEN.getCacheName();

    /**
     * Cache namespace used for caching the mapping between admin user IDs and their usernames.
     */
    String ADMIN_NAME = CacheType.ADMIN_NAME.getCacheName();

    /**
     * Cache namespace dedicated to system dictionary type definitions.
     */
    String DICT_TYPE = CacheType.DICT_TYPE.getCacheName();

}
