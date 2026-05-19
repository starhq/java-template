package com.github.starhq.template.common.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 通用枚举接口
 * 支持 T 类型 (如 String, Integer, Long 等)
 *
 * @param <E> 枚举类型本身
 * @param <T> 数据库中存储的类型 (如 String, Integer)
 */
public interface BaseEnum<E extends Enum<E> & BaseEnum<E, T>, T> {

    /**
     * 获取枚举对应的存储值
     * 序列化时使用此方法
     *
     * @return 存储值
     */
    @JsonValue
    T getValue();
}
