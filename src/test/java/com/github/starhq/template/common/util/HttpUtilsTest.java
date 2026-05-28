package com.github.starhq.template.common.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HttpUtilsTest {

    @Mock
    private HttpServletRequest request;
    private MockedStatic<RequestContextHolder> mockedStatic;
    ServletRequestAttributes attributes;

    @BeforeEach
    void setUp() {
        // 模拟有活跃的 HTTP 请求上下文
        mockedStatic = mockStatic(RequestContextHolder.class);
        attributes = mock(ServletRequestAttributes.class);
        mockedStatic.when(RequestContextHolder::getRequestAttributes).thenReturn(attributes);
    }

    @AfterEach
    void tearDown() {
        if (mockedStatic != null) {
            mockedStatic.close();
        }
    }

    // ==========================================
    // 1. 测试 IP 提取逻辑 (重点测试多代理头和未知 IP 防御)
    // ==========================================

    @Test
    void getClientIp_FromCurrentRequest_ShouldReturnFirstIp() {
        when(attributes.getRequest()).thenReturn(request);

        when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1");

        String ip = HttpUtils.getClientIp();

        assertEquals("10.0.0.1", ip);
        // 验证只解析了第一个头，没有去解析后续的无用头
        verify(request, never()).getHeader("Proxy-Client-IP");
    }

    @Test
    void getClientIp_FromXForwardedFor_ShouldReturnFirstIp() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1, 192.168.1.1, 172.16.0.1");

        String ip = HttpUtils.getClientIp(request);

        assertEquals("10.0.0.1", ip);
        // 验证只解析了第一个头，没有去解析后续的无用头
        verify(request, never()).getHeader("Proxy-Client-IP");
    }

    @Test
    void getClientIp_WithUnknownIdentifier_ShouldFallbackToRemoteAddr() {
        // 模拟所有 Header 都是 unknown
        when(request.getHeader(anyString())).thenReturn("unknown");
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");

        assertEquals("192.168.1.100", HttpUtils.getClientIp(request));
    }

    @Test
    void getClientIp_WithEmptyHeaders_ShouldFallbackToRemoteAddr() {
        when(request.getHeader(anyString())).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("192.168.1.200");

        assertEquals("192.168.1.200", HttpUtils.getClientIp(request));
    }

    @Test
    void getClientIp_WithEmptyRemoteAddr_ShouldReturnNull() {
        when(request.getHeader(anyString())).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn(""); // 模拟空字符串

        assertNull(HttpUtils.getClientIp(request));
    }

    @Test
    void getClientIp_WithoutRequestContext_ShouldThrowException() {
        // 清除 Mock，模拟没有 HTTP 上下文的情况
        mockedStatic.when(RequestContextHolder::getRequestAttributes).thenReturn(null);

        // 不传 request 调用静态方法
        IllegalStateException ex = assertThrows(IllegalStateException.class, HttpUtils::getClientIp);
        assertTrue(ex.getMessage().contains("No current HTTP request context found"));
    }

    // ==========================================
    // 2. 测试 Token 提取逻辑
    // ==========================================
    @Test
    void extractToken_FromBearerHeader_ShouldExtractCorrectly() {
        when(request.getHeader("Authorization")).thenReturn("Bearer my-jwt-token-123");

        String token = HttpUtils.extractToken(request);

        assertEquals("my-jwt-token-123", token);
    }

    @Test
    void extractToken_InvalidBearerHeader_ShouldFallbackToCookie() {
        when(request.getHeader("Authorization")).thenReturn("Basic invalidFormat");

        // 模拟 Cookie 中有 token
        Cookie mockCookie = new Cookie("jwt_token", "cookie-token-456");
        when(request.getCookies()).thenReturn(new Cookie[]{mockCookie});

        assertEquals("cookie-token-456", HttpUtils.extractToken(request));
    }

    @Test
    void extractToken_NoHeaderNoCookie_ShouldFallbackToQueryParam() {
        when(request.getHeader("Authorization")).thenReturn("");
        when(request.getCookies()).thenReturn(null); // 没有对应 Cookie
        when(request.getParameter("token")).thenReturn("query-token-789");

        assertEquals("query-token-789", HttpUtils.extractToken(request));
    }

    @Test
    void extractToken_NothingFound_ShouldReturnNull() {
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getCookies()).thenReturn(null);
        when(request.getParameter("token")).thenReturn(null);

        assertNull(HttpUtils.extractToken(request));
    }

    // ==========================================
    // 3. 测试 Fingerprint 提取逻辑 (逻辑与 Token 类似，简写覆盖分支)
    // ==========================================
    @Test
    void extractDeviceFingerprint_FromHeader() {
        when(request.getHeader("X-Device-Fingerprint")).thenReturn("fingerprint-abc-123");
        assertEquals("fingerprint-abc-123", HttpUtils.extractDeviceFingerprint(request));
    }

    @Test
    void extractDeviceFingerprint_FromCookie() {
        Cookie mockCookie = new Cookie("X-Device-Fingerprint", "fingerprint-abc-123");
        when(request.getCookies()).thenReturn(new Cookie[]{mockCookie});
        when(request.getHeader("X-Device-Fingerprint")).thenReturn("");
        assertEquals("fingerprint-abc-123", HttpUtils.extractDeviceFingerprint(request));
    }

    @Test
    void extractDeviceFingerprint_FromEmptyCookie() {
        when(request.getCookies()).thenReturn(new Cookie[]{});
        when(request.getHeader("X-Device-Fingerprint")).thenReturn("");
        assertNull(HttpUtils.extractDeviceFingerprint(request));
    }

    @Test
    void extractDeviceFingerprint_FromWrongCookieName() {
        Cookie mockCookie = new Cookie("X-Device-Fingerprint1", "fingerprint-abc-123");
        when(request.getCookies()).thenReturn(new Cookie[]{mockCookie});
        when(request.getHeader("X-Device-Fingerprint")).thenReturn("");
        assertNull(HttpUtils.extractDeviceFingerprint(request));
    }

    @Test
    void extractDeviceFingerprint_FromQueryParam() {
        when(request.getHeader("X-Device-Fingerprint")).thenReturn(null);
        when(request.getCookies()).thenReturn(null);
        when(request.getParameter("X-Device-Fingerprint")).thenReturn("query-fingerprint-xyz");
        assertEquals("query-fingerprint-xyz", HttpUtils.extractDeviceFingerprint(request));
    }

    // ==========================================
    // 5. 测试 Response 写入
    // ==========================================
    @Test
    void write_ShouldWriteJsonResponse() throws IOException {
        MockHttpServletResponse response = new MockHttpServletResponse();
        String expectedJson = "{\"code\": 401, \"msg\": \"Unauthorized\"}";

        HttpUtils.write(response, 401, expectedJson);

        assertEquals(401, response.getStatus());
        assertEquals("application/json;charset=UTF-8", response.getContentType());
        assertEquals(expectedJson, response.getContentAsString());
    }

    // ==========================================
    // 6. 读取 HEADER 参数
    // ==========================================

    @Test
    void getHeader_FromCurrentRequest_ShouldReturnHeaderValue() {
        when(attributes.getRequest()).thenReturn(request);

        when(request.getHeader("hello")).thenReturn("world");

        String value = HttpUtils.getHeader("hello");

        assertEquals("world", value);
    }
}