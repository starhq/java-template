package com.github.starhq.template.common.constant;

import com.github.starhq.template.config.cache.CacheType;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description: cache name constant
 * @date 2026/3/28 09:33
 */
public interface CacheConstant {

    String USER = CacheType.USER.getCacheName();

    String BUTTON = CacheType.BUTTON.getCacheName();

    String MENU = CacheType.MENU.getCacheName();

    String RESOURCE = CacheType.RESOURCE.getCacheName();

    String CAPTCHA = CacheType.CAPTCHA.getCacheName();

    String CAPTCHA_IP = CacheType.CAPTCHA_IP.getCacheName();

    String CAPTCHA_VERIFY = CacheType.CAPTCHA_VERIFY.getCacheName();

    String PERMISSION = CacheType.PERMISSION.getCacheName();

    String TOKEN = CacheType.TOKEN.getCacheName();

    String ADMIN_NAME = CacheType.ADMIN_NAME.getCacheName();

    String DICT_TYPE = CacheType.DICT_TYPE.getCacheName();

}
