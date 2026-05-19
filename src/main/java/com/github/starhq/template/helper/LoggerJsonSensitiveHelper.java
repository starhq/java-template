package com.github.starhq.template.helper;

import com.github.starhq.template.config.json.properties.SensitiveFieldProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.StringNode;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description: helper for log sensitive data
 * @date 2026/5/4 20:54
 */
@Slf4j
@RequiredArgsConstructor
public class LoggerJsonSensitiveHelper {

    private final JsonMapper jsonMapper;
    private final SensitiveFieldProperties sensitiveFieldProperties;

    public String mask(byte[] content, boolean isProd) {
        if (content == null || content.length == 0) {
            return null;
        }

        try {
            // 1. 解析为 JsonNode
            JsonNode rootNode = jsonMapper.readTree(content);

            // 2. 递归脱敏
            maskNode(rootNode);

            // 3. 转回字符串
            return getString(isProd, rootNode);
        } catch (JacksonException e) {
            log.warn("JSON desensitization failed, reason: {}", e.getMessage());
            return new String(content, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("An unknown error occurred during JSON desensitization", e);
            return "[Binary Data Error]";
        }
    }

    public String mask(HttpServletRequest request, boolean isHeader, boolean isProd) {
        Map<?, ?> map = isHeader ? getHeadersMap(request) : getParamsMap(request);
        if (CollectionUtils.isEmpty(map)) {
            return null;
        }
        return getString(isProd, map);
    }

    // ==========================================
    // 私有辅助方法区
    // ==========================================

    private String getString(boolean isProd, Object object) {
        return isProd ? jsonMapper.writeValueAsString(object) :
                jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
    }

    private Map<?, ?> getHeadersMap(HttpServletRequest request) {
        Set<String> headers = sensitiveFieldProperties.getHeaders();
        Map<String, String> headerMap = new LinkedHashMap<>(headers.size());
        for (String header : headers) {
            String value = request.getHeader(header);
            headerMap.put(header, StringUtils.hasText(value) ? (sensitiveFieldProperties.isSensitive(header) ? sensitiveFieldProperties.getMaskValue() : value) : "[EMPTY]");
        }
        return headerMap;
    }

    private Map<?, ?> getParamsMap(HttpServletRequest request) {
        Map<String, String[]> params = request.getParameterMap();
        if (params.isEmpty()) {
            return null;
        }

        Map<String, Object> simpleParams = new LinkedHashMap<>(params.size());
        params.forEach((key, values) ->
                simpleParams.put(key, sensitiveFieldProperties.isSensitive(key) ? sensitiveFieldProperties.getMaskValue() : values.length == 1 ? values[0] : Arrays.asList(values)));
        return simpleParams;
    }

    private void maskNode(JsonNode node) {
        if (Objects.isNull(node)) {
            return;
        }

        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;

            node.properties().forEach(entry -> {
                String key = entry.getKey();
                JsonNode value = entry.getValue();

                if (isSensitiveKey(key)) {
                    objectNode.set(key, new StringNode(sensitiveFieldProperties.getMaskValue()));
                } else {
                    maskNode(value);
                }
            });
        } else if (node.isArray()) {
            for (JsonNode element : node) {
                maskNode(element);
            }
        }
    }

    private boolean isSensitiveKey(String key) {
        return sensitiveFieldProperties.isSensitive(key);
    }

}
