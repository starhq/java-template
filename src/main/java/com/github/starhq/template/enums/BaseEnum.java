package com.github.starhq.template.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
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

    /**
     * 根据存储值转换为对应的枚举常量
     * 反序列化时使用此方法
     * <p>
     * 注意：这是一个接口默认方法，实现类必须实现逻辑，
     * 或者你可以提供一个静态工具类来统一处理。
     *
     * @param value 存储值
     * @return 匹配的枚举常量
     */
    @JsonCreator
    E fromValue(T value);
}
