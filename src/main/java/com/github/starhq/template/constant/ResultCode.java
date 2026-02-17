package com.github.starhq.template.constant;

/**
 * 响应结果码常量
 *
 * @author starhq
 */
public final class ResultCode {
    /**
     * 成功
     */
    public static final int SUCCESS = 200;

    /**
     * 参数错误
     */
    public static final int BAD_REQUEST = 400;

    /**
     * 未授权
     */
    public static final int UNAUTHORIZED = 401;

    /**
     * 禁止访问
     */
    public static final int FORBIDDEN = 403;

    /**
     * 资源不存在
     */
    public static final int NOT_FOUND = 404;

    /**
     * 服务器错误
     */
    public static final int INTERNAL_SERVER_ERROR = 500;

    private ResultCode() {
        // 工具类，禁止实例化
    }
}
