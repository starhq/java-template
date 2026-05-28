package com.github.starhq.template.helper;

import com.github.starhq.template.config.json.properties.SensitiveFieldProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoggerJsonSensitiveHelper 单元测试")
class LoggerJsonSensitiveHelperTest {

    @Mock
    private JsonMapper jsonMapper;
    @Mock
    private ObjectWriter objectWriter;

    @Mock
    private SensitiveFieldProperties sensitiveFieldProperties;

    @Mock
    private HttpServletRequest request;

    private LoggerJsonSensitiveHelper helper;

    @BeforeEach
    void setUp() {
        helper = new LoggerJsonSensitiveHelper(jsonMapper, sensitiveFieldProperties);
    }

    // ========================================
    // 1. mask(byte[], boolean) 字节流脱敏测试
    // ========================================
    @Nested
    @DisplayName("1. 字节流 JSON 脱敏测试")
    class ByteMaskTests {

        @Test
        @DisplayName("传入 null - 应返回 null")
        void mask_nullContent_shouldReturnNull() {
            assertThat(helper.mask(null, false)).isNull();
        }

        @Test
        @DisplayName("传入空数组 - 应返回 null")
        void mask_emptyContent_shouldReturnNull() {
            assertThat(helper.mask(new byte[0], false)).isNull();
        }

        @Test
        @DisplayName("非生产环境 - 应调用美化打印输出")
        void mask_devEnv_shouldUsePrettyPrinter() throws Exception {
            // given
            byte[] content = "{}".getBytes(StandardCharsets.UTF_8);
            ObjectNode rootNode = JsonNodeFactory.instance.objectNode();
            when(jsonMapper.readTree(content)).thenReturn(rootNode);
            when(jsonMapper.writerWithDefaultPrettyPrinter()).thenReturn(objectWriter);
            when(objectWriter.writeValueAsString(rootNode)).thenReturn("{\n  \"pretty\": true\n}");

            // when
            String result = helper.mask(content, false);

            // then
            assertThat(result).contains("pretty");
            verify(jsonMapper).writerWithDefaultPrettyPrinter();
            verify(jsonMapper, never()).writeValueAsString(any());
        }

        @Test
        @DisplayName("生产环境 - 应调用普通紧凑输出")
        void mask_prodEnv_shouldUseNormalWriter() throws Exception {
            // given
            byte[] content = "{}".getBytes(StandardCharsets.UTF_8);
            ObjectNode rootNode = JsonNodeFactory.instance.objectNode();
            when(jsonMapper.readTree(content)).thenReturn(rootNode);
            when(jsonMapper.writeValueAsString(rootNode)).thenReturn("{\"compact\":true}");

            // when
            String result = helper.mask(content, true);

            // then
            assertThat(result).isEqualTo("{\"compact\":true}");
            verify(jsonMapper).writeValueAsString(rootNode);
            verify(jsonMapper, never()).writerWithDefaultPrettyPrinter();
        }

        @Test
        @DisplayName("包含敏感字段 - 应递归替换为掩码值")
        void mask_withSensitiveKey_shouldReplaceValue() throws Exception {
            // given
            byte[] content = "{\"name\":\"张三\",\"idCard\":\"123456\"}".getBytes(StandardCharsets.UTF_8);
            ObjectNode rootNode = JsonNodeFactory.instance.objectNode();
            rootNode.put("name", "张三");
            rootNode.put("idCard", "123456");

            when(jsonMapper.readTree(content)).thenReturn(rootNode);
            when(sensitiveFieldProperties.isSensitive("name")).thenReturn(false);
            when(sensitiveFieldProperties.isSensitive("idCard")).thenReturn(true);
            when(sensitiveFieldProperties.getMaskValue()).thenReturn("*****");
            when(jsonMapper.writeValueAsString(any())).thenAnswer(invocation -> {
                ObjectNode arg = (ObjectNode) invocation.getArgument(0);
                return arg.toString(); // 方便断言
            });

            // when
            String result = helper.mask(content, true);

            // then
            assertThat(result).contains("\"name\":\"张三\"").contains("\"idCard\":\"*****\"");
        }

        @Test
        @DisplayName("嵌套对象和数组中的敏感字段 - 应深度递归脱敏")
        void mask_nestedStructure_shouldRecursivelyMask() throws Exception {
            // given
            byte[] content = "{}".getBytes();
            ObjectNode rootNode = JsonNodeFactory.instance.objectNode();
            ObjectNode innerObj = JsonNodeFactory.instance.objectNode();
            innerObj.put("secret", "hidden");
            rootNode.set("inner", innerObj);

            when(jsonMapper.readTree(content)).thenReturn(rootNode);

            // ✅ 关键修改：补充外层 key "inner" 的 Mock 行为，告诉它外层 key 不敏感
            when(sensitiveFieldProperties.isSensitive("inner")).thenReturn(false);
            when(sensitiveFieldProperties.isSensitive("secret")).thenReturn(true);

            when(sensitiveFieldProperties.getMaskValue()).thenReturn("🔲");
            when(jsonMapper.writeValueAsString(any())).thenAnswer(inv -> inv.getArgument(0).toString());

            // when
            String result = helper.mask(content, true);

            // then
            assertThat(result).contains("\"secret\":\"🔲\"");
            // 可选：验证两个 key 都被检查过了
            verify(sensitiveFieldProperties).isSensitive("inner");
            verify(sensitiveFieldProperties).isSensitive("secret");
        }

        @Test
        @DisplayName("JSON 解析失败 - 应捕获异常并返回原始字符串")
        void mask_jacksonException_shouldReturnRawString() throws Exception {
            // given
            byte[] content = "invalid json".getBytes();
            when(jsonMapper.readTree(content)).thenThrow(mock(JacksonException.class));

            // when
            String result = helper.mask(content, false);

            // then
            assertThat(result).isEqualTo("invalid json");
        }

        @Test
        @DisplayName("发生未知异常 (如 NullPointerException) - 应返回错误标识字符串")
        void mask_unknownException_shouldReturnErrorFlag() throws Exception {
            // given
            byte[] content = "{}".getBytes();
            when(jsonMapper.readTree(content)).thenThrow(new RuntimeException("Mocked NPE"));

            // when
            String result = helper.mask(content, false);

            // then
            assertThat(result).isEqualTo("[Binary Data Error]");
        }
    }

    // ========================================
    // 2. mask(HttpServletRequest, boolean, boolean) 请求参数脱敏测试
    // ========================================
    @Nested
    @DisplayName("2. 请求参数/Header 脱敏测试")
    class RequestMaskTests {

        @Test
        @DisplayName("提取 Header - 敏感 Header 替换，非敏感保留，空值显示 EMPTY")
        void mask_headers_shouldProcessCorrectly() throws Exception {
            // given
            // 1. 固定返回的 Header 列表
            when(sensitiveFieldProperties.getHeaders()).thenReturn(Set.of("Authorization", "Content-Type"));

            // 2. 【终极杀招】：拦截所有 isSensitive 调用，写出通用判断逻辑
            // 这样不管 Set 怎么乱序遍历，Mockito 都能完美应对，永远不会报错！
            when(sensitiveFieldProperties.isSensitive(anyString())).thenAnswer(invocation -> {
                String headerName = invocation.getArgument(0);
                // 模拟配置：只有 Authorization 是敏感的，其他都不是
                return "Authorization".equalsIgnoreCase(headerName);
            });

            when(sensitiveFieldProperties.getMaskValue()).thenReturn("******");

            when(request.getHeader("Content-Type")).thenReturn(null);

            when(jsonMapper.writeValueAsString(any())).thenAnswer(inv -> {
                Map<String, String> map = inv.getArgument(0);
                return map.toString();
            });

            // when
            String result = helper.mask(request, true, true);

            // then
            assertThat(result)
                    .contains("Authorization=******")
                    .contains("Content-Type=[EMPTY]");
        }

        @Test
        @DisplayName("提取 Params - 敏感参数脱敏，单值直接取，数组转 List")
        void mask_params_shouldProcessCorrectly() throws Exception {
            // given
            Map<String, String[]> paramMock = new LinkedHashMap<>();
            paramMock.put("password", new String[]{"123456"});
            paramMock.put("ids", new String[]{"1", "2", "3"});

            when(request.getParameterMap()).thenReturn(paramMock);
            when(sensitiveFieldProperties.isSensitive("password")).thenReturn(true);
            when(sensitiveFieldProperties.isSensitive("ids")).thenReturn(false);
            when(sensitiveFieldProperties.getMaskValue()).thenReturn("***");

            when(jsonMapper.writeValueAsString(any())).thenAnswer(inv -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) inv.getArgument(0);
                return map.toString();
            });

            // when
            String result = helper.mask(request, false, true);

            // then
            assertThat(result).contains("password=***")
                    .contains("ids=[1, 2, 3]"); // 数组转 List
        }

        @Test
        @DisplayName("请求中没有任何 Header 配置 - 返回 null")
        void mask_emptyHeadersConfig_shouldReturnNull() {
            when(sensitiveFieldProperties.getHeaders()).thenReturn(Collections.emptySet());
            String result = helper.mask(request, true, true);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("请求中没有任何 Parameter - 返回 null")
        void mask_emptyParams_shouldReturnNull() {
            when(request.getParameterMap()).thenReturn(Collections.emptyMap());
            String result = helper.mask(request, false, true);
            assertThat(result).isNull();
        }
    }
}