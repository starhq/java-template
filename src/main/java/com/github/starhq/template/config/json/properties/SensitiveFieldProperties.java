package com.github.starhq.template.config.json.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Set;

@Data
@ConfigurationProperties(prefix = "star.sensitive")
public class SensitiveFieldProperties {

    /**
     * 需要脱敏的字段名列表（不区分大小写匹配）
     */
    private Set<String> fields = Set.of("password", "oldPassword", "newPassword", "confirmPassword", "token", "secret",
            "accessToken", "refreshToken", "authorization", "cookie", "set-cookie");

    private Set<String> headers = Set.of(
            "Authorization", "Content-Type", "Accept", "User-Agent",
            "X-Device-Fingerprint", "Referer", "Origin", "Host");

    /**
     * 脱敏后的占位符
     */
    private String maskValue = "*****";

    /**
     * 判断字段名是否需要脱敏
     */
    public boolean isSensitive(String fieldName) {
        if (fieldName == null || fields.isEmpty()) {
            return false;
        }
        // ✅ 不区分大小写匹配
        return fields.stream()
                .anyMatch(sensitive -> sensitive.equalsIgnoreCase(fieldName));
    }
}
