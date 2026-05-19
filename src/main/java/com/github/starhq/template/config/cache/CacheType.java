package com.github.starhq.template.config.cache;

import java.time.Duration;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CacheType {
    DICT_TYPE("dictTypes", Duration.ofHours(24), 500),

    USER("users", Duration.ofMinutes(30), 100_000),

    MENU("menus", Duration.ofHours(2), 50_000),

    BUTTON("buttons", Duration.ofHours(2), 50_000),

    RESOURCE("resources", Duration.ofHours(2), 50_000),

    CAPTCHA("captchas", Duration.ofMinutes(5), 2_000),

    CAPTCHA_IP("ips", Duration.ofSeconds(60), 1_000),

    CAPTCHA_VERIFY("verify_ips", Duration.ofMinutes(15), 1_000),

    TOKEN("tokens", Duration.ofMinutes(30), 50_000),

    PERMISSION("permissions", Duration.ofMinutes(30), 20_000),

    ADMIN_NAME("adminNames", Duration.ofHours(2), 1_000);

    private final String cacheName;
    private final Duration ttl;
    private final long maxSize;
}
