package com.github.starhq.template.model.vo;

import java.io.Serial;
import java.io.Serializable;

import org.slf4j.MDC;

import com.github.starhq.template.common.enums.ErrorCode;

import lombok.Data;

/**
 * 统一响应结果
 *
 * @param <T> 数据类型
 * @author starhq
 */
@Data
public class Result<T> implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 业务错误码（业务层面的错误标识）
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 数据总数(分页用)
     */
    private Long total;
    /**
     * 链路追踪 ID
     */
    private String traceId;

    public Result() {
        this.traceId = MDC.get("trace_id");
    }

    public Result(T data) {
        this();
        this.data = data;
    }

    public Result(T data, Long total) {
        this();
        this.data = data;
        this.total = total;
    }

    public Result(String message) {
        this();
        this.message = message;
    }

    public Result(ErrorCode errorCode) {
        this();
        this.message = errorCode.getI18nKey();
        this.code = errorCode.getCode();
    }

    public Result(Integer code, String message) {
        this();
        this.message = message;
        this.code = code;
    }

    /**
     * 成功响应（带数据）
     *
     * @param data 数据
     * @param <T>  数据类型
     * @return 响应结果
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(data);
    }

    /**
     * 成功响应（带数据(分页)）
     *
     * @param data  数据
     * @param total 总数据数
     * @param <T>   数据类型
     * @return 响应结果
     */
    public static <T> Result<T> success(T data, Long total) {
        return new Result<>(data, total);
    }

    /**
     * 失败响应
     *
     * @param message 错误消息
     * @param <T>     数据类型
     * @return 响应结果
     */
    public static <T> Result<T> fail(String message) {
        return new Result<>(message);
    }

    /**
     * 失败响应
     *
     * @param errorCode 错误代码
     * @param <T>       数据类型
     * @return 响应结果
     */
    public static <T> Result<T> fail(ErrorCode errorCode) {
        return new Result<>(errorCode);
    }

    /**
     * 失败响应
     *
     * @param code    错误代码
     * @param message i18nkey
     * @param <T>     数据类型
     * @return 响应结果
     */
    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<>(code, message);
    }

}