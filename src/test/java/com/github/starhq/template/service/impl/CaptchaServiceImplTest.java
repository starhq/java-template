package com.github.starhq.template.service.impl;

import com.github.starhq.template.common.captcha.CaptchaResult;
import com.github.starhq.template.common.captcha.ICaptcha;
import com.github.starhq.template.common.exception.BadRequestException;
import com.github.starhq.template.common.exception.BusinessException;
import com.github.starhq.template.common.exception.CustomException;
import com.github.starhq.template.common.util.RequestContextUtil;
import com.github.starhq.template.helper.CacheHelper;
import com.github.starhq.template.model.dto.RequestContext;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked") // 抑制 Spring Cache 泛型警告
class CaptchaServiceImplTest {

    @Mock
    private ICaptcha captcha;
    @Mock
    private CacheHelper cacheHelper;
    @Mock
    private Cache cache; // Spring 的 Cache 对象
    @Mock
    private HttpServletResponse response;
    @Mock
    private ServletOutputStream outputStream;

    @InjectMocks
    private CaptchaServiceImpl captchaService;

    private static final String TEST_UUID = "test-uuid-123";
    private static final String TEST_CODE = "ABCD";

    // ==========================================
    // generateCode 测试
    // ==========================================

    @Test
    void generateCode_Success() throws IOException {
        // Given
        String deviceFingerprint = "device-123";
        String ip = "192.168.1.100";

        RequestContext context = new RequestContext(deviceFingerprint, ip);

        try (MockedStatic<RequestContextUtil> mockedStatic = mockStatic(RequestContextUtil.class)) {

            mockedStatic.when(RequestContextUtil::getContext).thenReturn(context);

            when(cacheHelper.getCache(anyString())).thenReturn(cache);

            // Mock IP 限制：返回 1 (小于 10)
            when(cache.get(anyString(), any(Callable.class))).thenReturn(new AtomicInteger(1));
            // Mock UUID 未被占用：返回 null
            when(cache.get(TEST_UUID, String.class)).thenReturn(null);

            when(response.getOutputStream()).thenReturn(outputStream);
            CaptchaResult mockResult = new CaptchaResult(TEST_CODE, null);
            when(captcha.generate()).thenReturn(mockResult);

            // When
            captchaService.generateCode(TEST_UUID, response);

            // Then
            verify(cache).put(TEST_UUID, TEST_CODE);
            verify(captcha).write(mockResult, outputStream);
            verify(response).setContentType(MediaType.IMAGE_PNG_VALUE);
        }
    }

    @Test
    void generateCode_Fail_InvalidUuid_Null() {
        assertThrows(BadRequestException.class, () -> captchaService.generateCode(null, response));
        assertThrows(BadRequestException.class, () -> captchaService.generateCode("", response));
    }

    @Test
    void generateCode_Fail_InvalidUuid_TooLong() {
        String longUuid = "a".repeat(65);
        assertThrows(BadRequestException.class, () -> captchaService.generateCode(longUuid, response));
    }

    @Test
    void generateCode_Fail_IpRateLimitExceeded() {
        // Given
        String deviceFingerprint = "device-123";
        String ip = "192.168.1.100";

        RequestContext context = new RequestContext(deviceFingerprint, ip);

        try (MockedStatic<RequestContextUtil> mockedStatic = mockStatic(RequestContextUtil.class)) {

            mockedStatic.when(RequestContextUtil::getContext).thenReturn(context);
            when(cacheHelper.getCache(anyString())).thenReturn(cache);
            // Mock IP 限制：返回 10 (达到限制)
            when(cache.get(anyString(), any(Callable.class))).thenReturn(new AtomicInteger(10));

            CustomException ex = assertThrows(CustomException.class, () ->
                    captchaService.generateCode(TEST_UUID, response)
            );
            assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatus());
        }
    }

    @Test
    void generateCode_Fail_UuidAlreadyExists() {
        // Given
        String deviceFingerprint = "device-123";
        String ip = "192.168.1.100";

        RequestContext context = new RequestContext(deviceFingerprint, ip);

        try (MockedStatic<RequestContextUtil> mockedStatic = mockStatic(RequestContextUtil.class)) {

            mockedStatic.when(RequestContextUtil::getContext).thenReturn(context);
            when(cacheHelper.getCache(anyString())).thenReturn(cache);
            when(cache.get(anyString(), any(Callable.class))).thenReturn(new AtomicInteger(1));
            // Mock UUID 已存在：返回 code
            when(cache.get(TEST_UUID, String.class)).thenReturn(TEST_CODE);

            CustomException ex = assertThrows(CustomException.class, () ->
                    captchaService.generateCode(TEST_UUID, response)
            );
            assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatus());
        }
    }

    @Test
    void generateCode_Fail_IOException() throws IOException {
        // Given
        String deviceFingerprint = "device-123";
        String ip = "192.168.1.100";

        RequestContext context = new RequestContext(deviceFingerprint, ip);

        try (MockedStatic<RequestContextUtil> mockedStatic = mockStatic(RequestContextUtil.class)) {

            mockedStatic.when(RequestContextUtil::getContext).thenReturn(context);
            when(cacheHelper.getCache(anyString())).thenReturn(cache);
            when(cache.get(anyString(), any(Callable.class))).thenReturn(new AtomicInteger(1));
            when(cache.get(TEST_UUID, String.class)).thenReturn(null);

            when(response.getOutputStream()).thenReturn(outputStream);
            when(captcha.generate()).thenReturn(new CaptchaResult(TEST_CODE, null));

            // Mock 输出流抛出异常
            doThrow(new IOException("Disk full")).when(outputStream).flush();

            assertThrows(BusinessException.class, () ->
                    captchaService.generateCode(TEST_UUID, response)
            );
        }
    }

    // ==========================================
    // verify 测试
    // ==========================================

    @Test
    void verify_Success() {
        // Given
        String deviceFingerprint = "device-123";
        String ip = "192.168.1.100";

        RequestContext context = new RequestContext(deviceFingerprint, ip);

        try (MockedStatic<RequestContextUtil> mockedStatic = mockStatic(RequestContextUtil.class)) {

            mockedStatic.when(RequestContextUtil::getContext).thenReturn(context);
            when(cacheHelper.getCache(anyString())).thenReturn(cache);

            // Mock 失败次数检查：未超限
            when(cache.get(contains("verify:fail"), eq(Integer.class))).thenReturn(null);
            // Mock 获取验证码：存在且匹配
            when(cache.get(TEST_UUID, String.class)).thenReturn(TEST_CODE);

            // When
            captchaService.verify(TEST_UUID, TEST_CODE);

            // Then
            // 验证执行了“一票否决”逻辑（不管对错，先删掉）
            verify(cache).evict(TEST_UUID);
            // 验证成功后，清除了失败计数器
            verify(cache).evict(contains("verify:fail"));
        }
    }

    @Test
    void verify_Fail_InputEmpty() {
        // Given
        String deviceFingerprint = "device-123";
        String ip = "192.168.1.100";

        RequestContext context = new RequestContext(deviceFingerprint, ip);

        try (MockedStatic<RequestContextUtil> mockedStatic = mockStatic(RequestContextUtil.class)) {

            mockedStatic.when(RequestContextUtil::getContext).thenReturn(context);
            assertThrows(BusinessException.class, () -> captchaService.verify(TEST_UUID, ""));
            assertThrows(BusinessException.class, () -> captchaService.verify(TEST_UUID, null));
        }
    }

    @Test
    void verify_Fail_IpBanned() {
        // Given
        String deviceFingerprint = "device-123";
        String ip = "192.168.1.100";

        RequestContext context = new RequestContext(deviceFingerprint, ip);

        try (MockedStatic<RequestContextUtil> mockedStatic = mockStatic(RequestContextUtil.class)) {

            mockedStatic.when(RequestContextUtil::getContext).thenReturn(context);
            when(cacheHelper.getCache(anyString())).thenReturn(cache);
            // Mock 失败次数：达到 5 次
            when(cache.get(contains("verify:fail"), eq(Integer.class))).thenReturn(5);

            assertThrows(BusinessException.class, () ->
                    captchaService.verify(TEST_UUID, "1234")
            );
        }
    }

    @Test
    void verify_Success_WhenFailCountUnderLimit() {
        // Given
        String deviceFingerprint = "device-123";
        String ip = "192.168.1.100";
        String correctCode = "ABCD"; // 假设这是正确的验证码

        RequestContext context = new RequestContext(deviceFingerprint, ip);

        try (MockedStatic<RequestContextUtil> mockedStatic = mockStatic(RequestContextUtil.class)) {
            mockedStatic.when(RequestContextUtil::getContext).thenReturn(context);
            when(cacheHelper.getCache(anyString())).thenReturn(cache);

            // 【关键修改 1】：Mock 失败次数为 2（小于上限 5），程序应该放行，不抛异常
            when(cache.get(contains("verify:fail"), eq(Integer.class))).thenReturn(2);

            // 【关键修改 2】：Mock 获取验证码成功
            when(cache.get(TEST_UUID, String.class)).thenReturn(correctCode);

            // When: 输入正确的验证码
            // 这里不应该抛出任何异常
            assertDoesNotThrow(() -> captchaService.verify(TEST_UUID, correctCode));

            // Then: 验证正常的清理逻辑被执行了
            verify(cache).evict(TEST_UUID);
            // 验证成功后，失败次数计数器被清除
            verify(cache).evict(contains("verify:fail"));
        }
    }

    @Test
    void verify_Success_WhenFailCountIsNull() {
        // Given
        String deviceFingerprint = "device-123";
        String ip = "192.168.1.100";
        String correctCode = "ABCD";

        RequestContext context = new RequestContext(deviceFingerprint, ip);

        try (MockedStatic<RequestContextUtil> mockedStatic = mockStatic(RequestContextUtil.class)) {
            mockedStatic.when(RequestContextUtil::getContext).thenReturn(context);
            when(cacheHelper.getCache(anyString())).thenReturn(cache);

            // 【关键修改】：Mock 失败次数为 null（第一次来验证，或者之前的记录过期了）
            when(cache.get(contains("verify:fail"), eq(Integer.class))).thenReturn(null);

            when(cache.get(TEST_UUID, String.class)).thenReturn(correctCode);

            // When
            assertDoesNotThrow(() -> captchaService.verify(TEST_UUID, correctCode));

            // Then
            verify(cache).evict(TEST_UUID);
            verify(cache).evict(contains("verify:fail"));
        }
    }

    @Test
    void verify_Fail_CaptchaExpired() {
        // Given
        String deviceFingerprint = "device-123";
        String ip = "192.168.1.100";

        RequestContext context = new RequestContext(deviceFingerprint, ip);

        try (MockedStatic<RequestContextUtil> mockedStatic = mockStatic(RequestContextUtil.class)) {

            mockedStatic.when(RequestContextUtil::getContext).thenReturn(context);
            when(cacheHelper.getCache(anyString())).thenReturn(cache);
            when(cache.get(contains("verify:fail"), eq(Integer.class))).thenReturn(null);
            // Mock 获取验证码：不存在 (返回 null)
            when(cache.get(TEST_UUID, String.class)).thenReturn(null);

            assertThrows(BusinessException.class, () ->
                    captchaService.verify(TEST_UUID, "1234")
            );
        }
    }

    @Test
    void verify_Fail_WrongCode() {
        // Given
        String deviceFingerprint = "device-123";
        String ip = "192.168.1.100";

        RequestContext context = new RequestContext(deviceFingerprint, ip);

        try (MockedStatic<RequestContextUtil> mockedStatic = mockStatic(RequestContextUtil.class)) {

            mockedStatic.when(RequestContextUtil::getContext).thenReturn(context);
            when(cacheHelper.getCache(anyString())).thenReturn(cache);
            when(cache.get(contains("verify:fail"), eq(Integer.class))).thenReturn(null);
            // Mock 获取验证码：存在
            when(cache.get(TEST_UUID, String.class)).thenReturn(TEST_CODE);

            // Mock 失败次数自增逻辑：模拟 cache.get 返回一个新的 AtomicInteger 并执行自增
            when(cache.get(contains("verify:fail"), any(Callable.class)))
                    .thenAnswer(invocation -> {
                        Callable<AtomicInteger> callable = invocation.getArgument(1);
                        return callable.call(); // 这会执行 () -> new AtomicInteger(0).incrementAndGet()，返回 1
                    });

            assertThrows(BusinessException.class, () ->
                    captchaService.verify(TEST_UUID, "wrong_code")
            );

            // 验证即使输错了，原验证码也被“一票否决”删除了
            verify(cache).evict(TEST_UUID);
            // 验证失败计数器并没有被清除
            verify(cache, never()).evict(contains("verify:fail"));
        }
    }
}
