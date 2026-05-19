package com.github.starhq.template.config.security;

import com.github.starhq.template.config.security.properties.WhiteListProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.util.CollectionUtils;

@RequiredArgsConstructor
public class WhiteListPathMatcher {

    private final WhiteListProperties whiteListProperties;

    /**
     * 检查是否在白名单中
     */
    public boolean isWhiteListPath(String requestPath) {
        if (CollectionUtils.isEmpty(whiteListProperties.getWhiteList())) {
            return false;
        }
        return whiteListProperties.getWhiteList().stream()
                .anyMatch(requestPath::contains);
    }
}
