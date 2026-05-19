package com.github.starhq.template.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.util.StringUtils;

@Getter
@AllArgsConstructor
public enum SortEnum implements BaseEnum<SortEnum, String> {
    ASC("asc"),
    DESC("desc");

    private final String value;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static SortEnum fromValue(String value) {
        if (!StringUtils.hasText(value)) {
            return DESC;
        }
        for (SortEnum order : values()) {
            if (order.value.equals(value.toLowerCase())) {
                return order;
            }
        }
        return DESC; // 默认倒序
    }
}
