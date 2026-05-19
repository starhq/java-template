package com.github.starhq.template.service.impl;

import com.github.starhq.template.common.captcha.CaptchaResult;
import com.github.starhq.template.common.captcha.ICaptcha;
import com.github.starhq.template.common.constant.CacheConstant;
import com.github.starhq.template.common.enums.ErrorCode;
import com.github.starhq.template.common.exception.BadRequestException;
import com.github.starhq.template.common.exception.BusinessException;
import com.github.starhq.template.common.exception.CustomException;
import com.github.starhq.template.common.util.RequestContextUtil;
import com.github.starhq.template.helper.CacheHelper;
import com.github.starhq.template.service.CaptchaService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description:
 * @date 2026/5/8 13:37
 */
@Service("captchaService")
@RequiredArgsConstructor
public class CaptchaServiceImpl implements CaptchaService {

    private final ICaptcha captcha;
    private final CacheHelper cacheHelper;

    private static final String IP_LIMIT_PREFIX = "captcha:ip:limit:";
    private static final String FAIL_LIMIT_PREFIX = "captcha:verify:fail:";

    private static final int IP_LIMIT_MAX = 10;
    private static final int FAIL_LIMIT_MAX = 5;

    @Override
    public void generateCode(String uuid,  HttpServletResponse response) {
        validateUuid(uuid);

        String clientIp = RequestContextUtil.getContext().clientIp();
        int times = checkIpRateLimit(IP_LIMIT_PREFIX + clientIp, CacheConstant.CAPTCHA_IP);
        if (times >= IP_LIMIT_MAX) {
            throw new CustomException(ErrorCode.CAPTCHA_REQUEST_TOO_OFTEN, HttpStatus.TOO_MANY_REQUESTS);
        }

        Cache cache = cacheHelper.getCache(CacheConstant.CAPTCHA);
        String code = cache.get(uuid, String.class);
        if (StringUtils.hasText(code)) {
            throw new CustomException(ErrorCode.CAPTCHA_REQUEST_TOO_OFTEN, HttpStatus.TOO_MANY_REQUESTS);
        }

        setNoCacheHeaders(response);

        try (OutputStream outputStream = response.getOutputStream()) {
            CaptchaResult result = captcha.generate();
            cache.put(uuid, result.code());
            captcha.write(result, outputStream);
            outputStream.flush();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.CAPTCHA_FAILED, e);
        }
    }

    @Override
    public void verify(String uuid, String userInputCode) {
        String clientIp = RequestContextUtil.getContext().clientIp();

        if (!StringUtils.hasText(userInputCode)) {
            throw new BusinessException(ErrorCode.CAPTCHA_VERIFY);
        }

        checkVerifyFailLimit(clientIp);

        Cache cache = cacheHelper.getCache(CacheConstant.CAPTCHA);
        String code = cache.get(uuid, String.class);
        if (!StringUtils.hasText(code)) {
            throw new BusinessException(ErrorCode.CAPTCHA_EXPIRED);
        }
        cache.evict(uuid);

        if (!code.equalsIgnoreCase(userInputCode)) {
            checkIpRateLimit(FAIL_LIMIT_PREFIX + clientIp, CacheConstant.CAPTCHA_VERIFY);
            throw new BusinessException(ErrorCode.CAPTCHA_VERIFY);
        }

        cache = cacheHelper.getCache(CacheConstant.CAPTCHA_VERIFY);
        cache.evict(FAIL_LIMIT_PREFIX + clientIp);
    }

    /**
     * 检查 IP 验证失败次数是否超限
     */
    private void checkVerifyFailLimit(String clientIp) {
        Cache cache = cacheHelper.getCache(CacheConstant.CAPTCHA_VERIFY);
        Integer failCount = cache.get(FAIL_LIMIT_PREFIX + clientIp, Integer.class);

        if (failCount != null && failCount >= FAIL_LIMIT_MAX) {
            throw new BusinessException(ErrorCode.CAPTCHA_VERIFY_BANNED);
        }
    }

    private int checkIpRateLimit(String key, String cacheKey) {
        Cache cache = cacheHelper.getCache(cacheKey);

        AtomicInteger counter = cache.get(key, () -> new AtomicInteger(0));
        return Objects.requireNonNull(counter).incrementAndGet();
    }

    private void setNoCacheHeaders(HttpServletResponse response) {
        response.setContentType(MediaType.IMAGE_PNG_VALUE);
        response.setHeader(HttpHeaders.PRAGMA, "no-cache");
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, max-age=0");
        response.setDateHeader(HttpHeaders.EXPIRES, 0);
    }

    private void validateUuid(String uuid) {
        if (!StringUtils.hasText(uuid) || uuid.length() > 64) {
            throw new BadRequestException(ErrorCode.QUERY_FORMAT, uuid);
        }
    }
}
