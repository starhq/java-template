package com.github.starhq.template.config.security.properties;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "star")
public class WhiteListProperties {
    private List<String> whiteList = new ArrayList<>();
}
