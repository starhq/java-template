package com.github.starhq.template.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 系统错误码枚举
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    // ========== 1. 通用错误 (1xxxx) ==========
    // 400,参数格式错误(body)
    PARAM_FORMAT(10000, "error.param.format", "error.external"),
    // 400,参数格式错误(query)
    QUERY_FORMAT(10001, "error.query.format", "error.external"),
    // 400,参数超出范围
    ENUM_FORMAT(10002, "error.enum.format", "error.external"),
    // 400,校验失败
    VALIDATION_FAILED(10003, "error.validation", "error.external"),
    // 404,资源不存在(一般请求的路径错误)
    NOT_FOUND(10004, "error.not_found", "error.external"),
    // 405,请求方法不允许
    NOT_SUPPORT(10005, "error.method.not_support", "error.external"),

    // ========== 2. 权限模块 (2xxxx) ==========
    // 401, 未经授权
    UNAUTHORIZED(20000, "error.auth.unauthorized", "error.auth.unauthorized"),
    // 401, 请求缺少令牌
    TOKEN_MISSING(20001, "error.auth.token.missing", "error.auth.unauthorized"),
    // 401, 令牌无效
    TOKEN_INVALID(20002, "error.auth.token.invalid", "error.auth.unauthorized"),
    // 401, 令牌过期
    TOKEN_EXPIRED(20003, "error.auth.token.expired", "error.auth.unauthorized"),
    // 401, 刷新令牌无效
    TOKEN_REFRESH_INVALID(20004, "error.auth.token.refresh.invalid", "error.auth.unauthorized"),
    // 401, 访问令牌无效
    TOKEN_ACCESS_INVALID(20005, "error.auth.token.access.invalid", "error.auth.unauthorized"),
    // 401, 会话无效
    SESSION_INVALID(20006, "error.auth.session.invalid", "error.auth.unauthorized"),
    // 401, 会话过期
    SESSION_EXPIRED(20007, "error.auth.session.expired", "error.auth.unauthorized"),
    // 401, 请求缺少指纹
    FINGERPRINT_MISSING(20008, "error.auth.fingerprint.missing", "error.auth.unauthorized"),
    // 401, 客户端环境发生变化(更换设备,ip等,需要重新认证)
    DEVICE_MISMATCH(20009, "error.auth.device.mismatch", "error.auth.unauthorized"),
    // 401, 用户名或者密码错误
    CREDENTIALS(20010, "error.auth.credentials", "error.auth.unauthorized"),
    // 401, 账号不可用
    DISABLED(20011, "error.auth.disabled", "error.auth.unauthorized"),
    // 401,尚未分配角色
    NO_ROLES(20012, "error.auth.no_roles", "error.auth.unauthorized"),
    // 401, 原密码错误
    MISMATCH_PASSWORD(20013, "error.password.mismatch", "error.auth.unauthorized"),
    // 401, 新旧密码不能相同
    SAME_PASSWORD(20014, "error.password.same", "error.auth.unauthorized"),
    // 401, 重置密码失败
    RESET_PASSWORD(20015, "error.auth.password.failed", "error.auth.unauthorized"),
    // 500, 校验码生成失败
    CAPTCHA_FAILED(20016, "error.auth.captcha.failed", "error.internal"),
    // 400, 校验码验证失败
    CAPTCHA_VERIFY(20017, "error.auth.captcha.verify", "error.internal"),
    // 429, 校验码请求过于频繁
    CAPTCHA_REQUEST_TOO_OFTEN(20018, "error.auth.captcha.often", "error.internal"),
    // 500, 错误次数过多,暂时禁止登陆
    CAPTCHA_VERIFY_BANNED(20019, "error.auth.captcha.banned", "error.internal"),
    // 500, captcha过期
    CAPTCHA_EXPIRED(20019, "error.auth.captcha.expired", "error.internal"),

    // 403, 禁止访问
    FORBIDDEN(21000, "error.auth.forbidden", "error.auth.forbidden"),

    // ========== 3. 业务模块 (3xxxx) ==========
    // 404, 用户不存在
    USER_NOT_FOUND(30000, "error.user.not_found", "error.not_found"),
    // 500, 用户分配却色失败
    USER_ASSIGN_FAILED(30001, "error.user.assign.role.fail", "error.business"),
    // 429, 重复用户名
    USER_DUPLICATE_USERNAME(30002, "error.user.duplicate.username", "error.external"),
    // 500, 更新用户失败
    USER_UPDATE_FAILED(
            30003, "error.user.update.failed", "error.business"),
    // 500, 删除用户失败
    USER_DELETE_FAILED(
            30004, "error.user.delete.failed", "error.business"),
    // 500, 添加用户失败
    USER_INSERT_FAILED(
            30005, "error.user.insert.failed", "error.business"),
    // 500, 因为关联关系删除用户失败
    USER_CONSTRAINT(
            30006, "error.user.delete.constraint", "error.business"),

    // 500, 令牌撤销失败
    TOKEN_REVOKED(31000, "error.token.revoked", "error.business"),
    // 500, 令牌签发失败
    TOKEN_ISSUED(31001, "error.token.issued", "error.business"),

    // 404,角色不存在
    ROLE_NOT_FOUND(32000, "error.role.not_found", "error.not_found"),
    // 429,角色的code已经被占用
    ROLE_DUPLICATE_CODE(32001, "error.role.duplicate_code", "error.external"),
    // 500,角色的保存失败
    ROLE_INSERT_FAILED(32002, "error.role.insert.failed", "error.business"),
    // 500,角色的更新失败
    ROLE_UPDATE_FAILED(32003, "error.role.update.failed", "error.business"),
    // 500,角色的删除失败
    ROLE_DELETE_FAILED(32004, "error.role.delete.failed", "error.business"),
    // 500, 角色分配菜单失败
    ROLE_ASSIGN_MENUS_FAILED(32005, "error.role.assign.menus.fail", "error.business"),
    // 500, 角色分配按钮失败
    ROLE_ASSIGN_BUTTONS_FAILED(32006, "error.role.assign.buttons.fail", "error.business"),
    // 500, 角色分配资源失败
    ROLE_ASSIGN_RESOURCES_FAILED(32007, "error.role.assign.resources.fail", "error.business"),

    // 404,菜单不存在
    MENU_NOT_FOUND(33000, "error.menu.not_found", "error.not_found"),
    // 500,菜单的保存失败
    MENU_INSERT_FAILED(33001, "error.menu.insert.failed", "error.business"),
    // 500, 菜单删除失败
    MENU_DELETE_FAILED(33002, "error.menu.delete.failed", "error.business"),
    // 500, 菜单更新失败
    MENU_UPDATE_FAILED(33003, "error.menu.update.failed", "error.business"),
    // 500, 菜单包含子菜单
    MENU_HAS_CHILD(33004, "error.menu.delete.constraint", "error.business"),

    // 404,按钮不存在
    BUTTON_NOT_FOUND(34000, "error.button.not_found", "error.not_found"),
    // 429, code 已存在
    BUTTON_DUPLICATE_CODE(34001, "error.button.duplicate_code", "error.external"),
    // 500, 按钮的保存失败
    BUTTON_INSERT_FAILED(34002, "error.button.insert.failed", "error.business"),
    // 500, 按钮的更新失败
    BUTTON_UPDATE_FAILED(34003, "error.button.update.failed", "error.business"),
    // 500, 按钮的删除失败
    BUTTON_DELETE_FAILED(34004, "error.button.delete.failed", "error.business"),

    // 404, resource不存在
    RESOURCE_NOT_FOUND(35000, "error.resource.not_found", "error.not_found"),
    // 429, resource的url和method已存在
    RESOURCE_DUPLICATE_URL_METHOD(35001, "error.resource.duplicate_url_method", "error.external"),
    // 500, resource的保存失败
    RESOURCE_INSERT_FAILED(35002, "error.resource.insert.failed", "error.business"),
    // 500, resource的更新失败
    RESOURCE_UPDATE_FAILED(35003, "error.resource.update.failed", "error.business"),
    // 500, resource的删除失败
    RESOURCE_DELETE_FAILED(35004, "error.resource.delete.failed", "error.business"),

    // 404, dict-type不存在
    DICT_TYPE_NOT_FOUND(36000, "error.dict_type.not_found", "error.not_found"),
    // 429, dict-type的type 已存在
    DICT_TYPE_DUPLICATE_TYPE(36001, "error.dict_type.duplicate_type", "error.external"),
    // 500, dict-type的保存失败
    DICT_TYPE_INSERT_FAILED(36002, "error.dict_type.insert.failed", "error.business"),
    // 500, dict-type的更新失败
    DICT_TYPE_UPDATE_FAILED(36003, "error.dict_type.update.failed", "error.business"),
    // 500, dict-type的删除失败
    DICT_TYPE_DELETE_FAILED(36004, "error.dict_type.delete.failed", "error.business"),

    // 404, dict-data不存在
    DICT_DATA_NOT_FOUND(37000, "error.dict_data.not_found", "error.not_found"),
    // 429, dict-data的value 已存在
    DICT_DATA_DUPLICATE_VALUE(37001, "error.dict_data.duplicate_value", "error.external"),
    // 500, dict-data的保存失败
    DICT_DATA_INSERT_FAILED(37002, "error.dict_data.insert.failed", "error.business"),
    // 500, dict-data的更新失败
    DICT_DATA_UPDATE_FAILED(37003, "error.dict_data.update.failed", "error.business"),
    // 500, dict-data的删除失败
    DICT_DATA_DELETE_FAILED(37004, "error.dict_data.delete.failed", "error.business"),

    // ========== 9. 系统异常 (9xxxx) ==========
    // 500, 数据库查询异常
    DB_QUERY_ERROR(90000, "error.db.execute", "error.internal"),
    // 500, 已经从数据库中读取数据,映射对象失败
    DB_MAPPING_ERROR(90001, "error.db.mapping", "error.internal"),
    // 500, 系统内部错误
    INTERNAL_ERROR(99999, "error.internal", "error.internal");

    /**
     * 错误码
     */
    private final Integer code;

    /**
     * i18n 消息 key
     */
    private final String i18nKey;

    /**
     * 默认英文消息（用于 i18n 缺失时的 fallback）
     */
    private final String defaultMessage;
}
