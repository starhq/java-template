-- ENUM Types
CREATE TYPE user_status AS ENUM ('active', 'inactive', 'banned');
CREATE TYPE target_type AS ENUM ('user', 'role', 'menu', 'resource', 'button', 'dict_type', 'dict_data');

-- Users Table
CREATE TABLE sys_user
(
    id         BIGINT PRIMARY KEY,
    username   VARCHAR(50) UNIQUE                    NOT NULL,
    password   VARCHAR(70)                           NOT NULL, -- bcrypt hash
    status     user_status DEFAULT 'active'          NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by BIGINT,
    updated_at TIMESTAMPTZ,
    updated_by BIGINT,
    CONSTRAINT fk_user_created_by FOREIGN KEY (created_by) REFERENCES sys_user (id), -- DEFERRABLE INITIALLY DEFERRED,
    CONSTRAINT fk_user_updated_by FOREIGN KEY (updated_by) REFERENCES sys_user (id)
);

COMMENT ON TABLE sys_user IS 'Stores system users and their authentication details';
CREATE INDEX idx_user_username ON sys_user(username);

-- Roles Table
CREATE TABLE sys_role
(
    id          BIGINT PRIMARY KEY,
    code        VARCHAR(50) UNIQUE                    NOT NULL,
    name        VARCHAR(50)                           NOT NULL,
    description VARCHAR(255)  DEFAULT ''              NOT NULL,
    is_default  BOOLEAN DEFAULT FALSE,   -- 是否为默认角色额
    created_at  TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by  BIGINT                                NOT NULL,
    updated_at  TIMESTAMPTZ,
    updated_by  BIGINT,
    CONSTRAINT fk_role_created_by FOREIGN KEY (created_by) REFERENCES sys_user (id),
    CONSTRAINT fk_role_updated_by FOREIGN KEY (updated_by) REFERENCES sys_user (id)
);

COMMENT ON TABLE sys_role IS 'Stores roles in the system';
CREATE INDEX idx_sys_role_is_default ON sys_role(is_default) WHERE is_default = true;

-- User-Role Mapping Table (Many-to-Many)
CREATE TABLE sys_user_role
(
    user_id    BIGINT                                NOT NULL,
    role_id    BIGINT                                NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_role_user_id FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_user_role_role_id FOREIGN KEY (role_id) REFERENCES sys_role (id)
);

COMMENT ON TABLE sys_user_role IS 'Links users to roles in a many-to-many relationship';
CREATE INDEX idx_user_role_user_id ON sys_user_role(user_id);
CREATE INDEX idx_user_role_role_id ON sys_user_role(role_id);

-- Menu Table
CREATE TABLE sys_menu
(
    id         BIGINT PRIMARY KEY,
    parent_id  BIGINT,
    name       VARCHAR(30)                           NOT NULL,
    url        VARCHAR(100),
    icon       VARCHAR(50)                           NOT NULL ,
    sort_order INT         DEFAULT 0,
    open_style SMALLINT    DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by BIGINT                                NOT NULL,
    updated_at TIMESTAMPTZ,
    updated_by BIGINT,
    CONSTRAINT fk_menu_parent FOREIGN KEY (parent_id) REFERENCES sys_menu (id), -- DEFERRABLE INITIALLY DEFERRED,
    CONSTRAINT fk_menu_created_by FOREIGN KEY (created_by) REFERENCES sys_user (id),
    CONSTRAINT fk_menu_updated_by FOREIGN KEY (updated_by) REFERENCES sys_user (id)
);

COMMENT ON TABLE sys_menu IS 'Stores navigation menu items';
-- if menu's data is huge, use blew index to improve query performance
-- CREATE INDEX idx_menu_parent_sort ON sys_menu (parent_id, sort_order);   -- critical for tree rendering
CREATE INDEX idx_menu_sort_order ON sys_menu (sort_order);

-- Resources Table
CREATE TABLE sys_resource
(
    id          BIGINT PRIMARY KEY,
    name        VARCHAR(30)                           NOT NULL,
    url         VARCHAR(100)                          NOT NULL,
    method      SMALLINT DEFAULT 0                    NOT NULL,
    description VARCHAR(255) DEFAULT ''               NOT NULL,
    created_at  TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by  BIGINT                                NOT NULL,
    updated_at  TIMESTAMPTZ,
    updated_by  BIGINT,
    CONSTRAINT fk_resource_created_by FOREIGN KEY (created_by) REFERENCES sys_user (id),
    CONSTRAINT fk_resource_updated_by FOREIGN KEY (updated_by) REFERENCES sys_user (id)
);

COMMENT ON TABLE sys_resource IS 'Stores protected resources that require permissions';
CREATE UNIQUE INDEX uk_resource_url_method on sys_resource(url, method);

-- Buttons Table
CREATE TABLE sys_button
(
    id          BIGINT PRIMARY KEY,
    menu_id     BIGINT                                NOT NULL,
    name        VARCHAR(100)                          NOT NULL,
    code        VARCHAR(100) UNIQUE                   NOT NULL,
    description VARCHAR(255) DEFAULT ''               NOT NULL,
    created_at  TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by  BIGINT                                NOT NULL,
    updated_at  TIMESTAMPTZ,
    updated_by  BIGINT,
    CONSTRAINT fk_button_menu_id FOREIGN KEY (menu_id) REFERENCES sys_menu (id),
    CONSTRAINT fk_button_created_by FOREIGN KEY (created_by) REFERENCES sys_user (id),
    CONSTRAINT fk_button_updated_by FOREIGN KEY (updated_by) REFERENCES sys_user (id)
);

COMMENT ON TABLE sys_button IS 'Stores buttons that require permissions for user interaction';
CREATE INDEX idx_button_menu_id ON sys_button (menu_id);

-- Role-Menu Table
CREATE TABLE sys_role_menu
(
    role_id    BIGINT                                NOT NULL,
    menu_id    BIGINT                                NOT NULL,
    PRIMARY KEY (role_id, menu_id),
    CONSTRAINT fk_role_menu_role_id FOREIGN KEY (role_id) REFERENCES sys_role (id),
    CONSTRAINT fk_role_menu_menu_id FOREIGN KEY (menu_id) REFERENCES sys_menu (id)
);

COMMENT ON TABLE sys_role_menu IS 'Links roles to sys_menu for access control';
CREATE INDEX idx_role_menu_role_id ON sys_role_menu(role_id);
CREATE INDEX idx_role_menu_menu_id ON sys_role_menu(menu_id);

-- Role-Resource Table
CREATE TABLE sys_role_resource
(
    role_id     BIGINT                                NOT NULL,
    resource_id BIGINT                                NOT NULL,
    PRIMARY KEY (role_id, resource_id),
    CONSTRAINT fk_role_resource_role_id FOREIGN KEY (role_id) REFERENCES sys_role (id),
    CONSTRAINT fk_role_resource_resource_id FOREIGN KEY (resource_id) REFERENCES sys_resource (id)
);

COMMENT ON TABLE sys_role_resource IS 'Links roles to sys_resource for access control';
CREATE INDEX idx_role_resource_role_id ON sys_role_resource(role_id);
CREATE INDEX idx_role_resource_resource_id ON sys_role_resource(resource_id);

-- 按钮权限表
CREATE TABLE sys_role_button
(
    role_id    BIGINT                                NOT NULL,
    button_id  BIGINT                                NOT NULL,
    PRIMARY KEY (role_id, button_id),
    CONSTRAINT fk_role_button_role_id FOREIGN KEY (role_id) REFERENCES sys_role (id),
    CONSTRAINT fk_role_button_button_id FOREIGN KEY (button_id) REFERENCES sys_button (id)
);

COMMENT ON TABLE sys_role_button IS 'Links roles to buttons for access control';
CREATE INDEX idx_role_button_role_id ON sys_role_button(role_id);
CREATE INDEX idx_role_button_button_id ON sys_role_button(button_id);

-- Audit Log Table
CREATE TABLE sys_audit_log
(
    id          BIGINT PRIMARY KEY,
    action      VARCHAR(255)                          NOT NULL,
    target_id   BIGINT,
    target_type target_type                           NOT NULL,
    value       TEXT,
    created_at  TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by  BIGINT                                NOT NULL,
    CONSTRAINT fk_audit_log_created_by FOREIGN KEY (created_by) REFERENCES sys_user (id)
);

COMMENT ON TABLE sys_audit_log IS 'Stores system actions and security audit logs';
CREATE INDEX idx_audit_log_target_type_created_by ON sys_audit_log(target_type, created_by);

-- Default Roles Table
-- CREATE TABLE sys_default_role
-- (
--     id         BIGINT PRIMARY KEY,
--     role_id    BIGINT                                NOT NULL,
--     created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
--     created_by BIGINT                                NOT NULL,
--     updated_at TIMESTAMPTZ,
--     updated_by BIGINT,
--     CONSTRAINT fk_default_role FOREIGN KEY (role_id) REFERENCES sys_role (id)
-- );

-- COMMENT ON TABLE sys_default_role IS 'Defines default roles assigned to new users';

CREATE TABLE sys_token
(
    id                 BIGINT PRIMARY KEY,
    user_id            BIGINT                                NOT NULL,
    access_token       TEXT                                  NOT NULL,
    refresh_token      TEXT                                  NOT NULL,
    expired_at         TIMESTAMPTZ                           NOT NULL,
    created_at         TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    revoked            BOOLEAN DEFAULT FALSE                 NOT NULL,
    login_ip           VARCHAR(45)                           NOT NULL,
    device_fingerprint VARCHAR(64)                           NOT NULL,
    CONSTRAINT fk_token_user_id FOREIGN KEY (user_id) REFERENCES sys_user (id)
);

COMMENT ON TABLE sys_token IS 'Stores system access tokens';
CREATE INDEX idx_token_user_id ON sys_token(user_id);

create table sys_dict_type
(
    id          BIGINT PRIMARY KEY,
    type        VARCHAR(100) UNIQUE                   NOT NULL,
    name        VARCHAR(255)                          NOT NULL,
    description VARCHAR(255) DEFAULT ''               NOT NULL,
    created_at  TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by  BIGINT                                NOT NULL,
    updated_at  TIMESTAMPTZ,
    updated_by  BIGINT,
    CONSTRAINT fk_dict_type_created_by FOREIGN KEY (created_by) REFERENCES sys_user (id),
    CONSTRAINT fk_dict_type_updated_by FOREIGN KEY (updated_by) REFERENCES sys_user (id)
);

COMMENT ON TABLE sys_dict_type IS 'Stores system dictionary types';

create table sys_dict_data
(
    id          BIGINT PRIMARY KEY,
    type_id     BIGINT                                NOT NULL,
    label       VARCHAR(255)                          NOT NULL,
    value       VARCHAR(255)                          NOT NULL,
    description VARCHAR(255) DEFAULT ''               NOT NULL,
    created_at  TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by  BIGINT                                NOT NULL,
    updated_at  TIMESTAMPTZ,
    updated_by  BIGINT,
    CONSTRAINT fk_dict_type_type_id FOREIGN KEY (type_id) REFERENCES sys_dict_type (id),
    CONSTRAINT fk_dict_type_created_by FOREIGN KEY (created_by) REFERENCES sys_user (id),
    CONSTRAINT fk_dict_type_updated_by FOREIGN KEY (updated_by) REFERENCES sys_user (id)
);

CREATE UNIQUE INDEX uk_dict_type_value on sys_dict_data(type_id, value);
COMMENT ON TABLE sys_dict_data IS 'Stores system dictionary data';
CREATE INDEX idx_dict_data_type_id ON sys_dict_data(type_id);

INSERT INTO sys_user (id, username, password, status, created_at, created_by, updated_at, updated_by) VALUES (1, 'admin', '$2a$10$SirpyIjgbMRijCOYtcP0UOdnU6aT.Ar9IxLerWQSCD3JpVzfBzFNa', 'active', CURRENT_TIMESTAMP, null, null, null);
INSERT INTO sys_user (id, username, password, status, created_at, created_by, updated_at, updated_by) VALUES (2, 'starhq', '$2a$10$SirpyIjgbMRijCOYtcP0UOdnU6aT.Ar9IxLerWQSCD3JpVzfBzFNa', 'active', CURRENT_TIMESTAMP, null, null, null);

INSERT INTO sys_role (id, code, name, description, created_at, created_by, updated_at, updated_by) VALUES (1, 'T_ADMIN', 'Administrator', 'Full system access', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 1);
INSERT INTO sys_role (id, code, name, description, created_at, created_by, updated_at, updated_by) VALUES (2, 'T_USER', 'User', 'Basic user access', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 1);

INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1);
INSERT INTO sys_user_role (user_id, role_id) VALUES (2, 2);

INSERT INTO sys_menu (id, parent_id, name, url, icon, sort_order, open_style, created_at, created_by, updated_at, updated_by) VALUES (1, null, '权限管理', NULL, 'icon-safetycertificate', 99, 0, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 1);
INSERT INTO sys_menu (id, parent_id, name, url, icon, sort_order, open_style, created_at, created_by, updated_at, updated_by) VALUES (2, 1, '用户管理', 'sys/user', 'icon-user', 0, 0, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 1);
INSERT INTO sys_menu (id, parent_id, name, url, icon, sort_order, open_style, created_at, created_by, updated_at, updated_by) VALUES (3, 1, '角色管理', 'sys/role', 'icon-team', 1, 0, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 1);
INSERT INTO sys_menu (id, parent_id, name, url, icon, sort_order, open_style, created_at, created_by, updated_at, updated_by) VALUES (4, 1, '菜单管理', 'sys/menu', 'icon-unorderedlist', 2, 0, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 1);
INSERT INTO sys_menu (id, parent_id, name, url, icon, sort_order, open_style, created_at, created_by, updated_at, updated_by) VALUES (5, 1, '资源管理','sys/resource', 'icon-api', 3, 0, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 1);
INSERT INTO sys_menu (id, parent_id, name, url, icon, sort_order, open_style, created_at, created_by, updated_at, updated_by) VALUES (6, 1, '按钮管理','sys/button', 'icon-menu', 4, 0, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 1);
INSERT INTO sys_menu (id, parent_id, name, url, icon, sort_order, open_style, created_at, created_by, updated_at, updated_by) VALUES (7, null, '系统设置',NULL, 'icon-setting', 100, 0, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 1);
INSERT INTO sys_menu (id, parent_id, name, url, icon, sort_order, open_style, created_at, created_by, updated_at, updated_by) VALUES (8, 7, '字典管理','setting/dict-type', 'icon-golden-fill', 0, 0, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 1);
INSERT INTO sys_menu (id, parent_id, name, url, icon, sort_order, open_style, created_at, created_by, updated_at, updated_by) VALUES (9, 7, '活跃用户','setting/token', 'icon-user', 1, 0, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 1);
INSERT INTO sys_menu (id, parent_id, name, url, icon, sort_order, open_style, created_at, created_by, updated_at, updated_by) VALUES (10, 7, '审计日志','setting/audit-log', 'icon-solution', 2, 0, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 1);
INSERT INTO sys_menu (id, parent_id, name, url, icon, sort_order, open_style, created_at, created_by, updated_at, updated_by) VALUES (11, 7, '默认角色','setting/default-role', 'icon-team', 3, 0, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 1);

INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 1);
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 2);
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 3);
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 4);
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 5);
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 6);
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 7);
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 8);
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 9);
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 10);
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 11);


INSERT INTO sys_button (id, menu_id, name, code, description, created_at, created_by, updated_at, updated_by) VALUES (1, 2, '用户查询', 'sys:user:get', '', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 1);
INSERT INTO sys_button (id, menu_id, name, code, description, created_at, created_by, updated_at, updated_by) VALUES (2, 2, '用户修改','sys:user:edit', '', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 1);
INSERT INTO sys_button (id, menu_id, name, code, description, created_at, created_by, updated_at, updated_by) VALUES (3, 2, '用户删除','sys:user:delete', '', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 1);
INSERT INTO sys_button (id, menu_id, name, code, description, created_at, created_by, updated_at, updated_by) VALUES (4, 2, '用户导出','sys:user:export', '', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 1);
INSERT INTO sys_button (id, menu_id, name, code, description, created_at, created_by, updated_at, updated_by) VALUES (5, 3, '角色新增','sys:role:add', '', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 1);
INSERT INTO sys_button (id, menu_id, name, code, description, created_at, created_by, updated_at, updated_by) VALUES (6, 3, '角色修改','sys:role:edit', '', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 1);
INSERT INTO sys_button (id, menu_id, name, code, description, created_at, created_by, updated_at, updated_by) VALUES (7, 3, '角色删除','sys:role:delete', '', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 1);
INSERT INTO sys_button (id, menu_id, name, code, description, created_at, created_by, updated_at, updated_by) VALUES (8, 4, '菜单新增','sys:menu:add', '', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 1);
INSERT INTO sys_button (id, menu_id, name, code, description, created_at, created_by, updated_at, updated_by) VALUES (9, 4, '菜单修改','sys:menu:edit', '', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 1);
INSERT INTO sys_button (id, menu_id, name, code, description, created_at, created_by, updated_at, updated_by) VALUES (10, 4, '菜单删除','sys:menu:delete', '', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 1);
INSERT INTO sys_button (id, menu_id, name, code, description, created_at, created_by, updated_at, updated_by) VALUES (11, 5, '资源新增','sys:resource:add', '', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 1);
INSERT INTO sys_button (id, menu_id, name, code, description, created_at, created_by, updated_at, updated_by) VALUES (12, 5, '资源修改','sys:resource:edit', '', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 1);
INSERT INTO sys_button (id, menu_id, name, code, description, created_at, created_by, updated_at, updated_by) VALUES (13, 5, '资源删除','sys:resource:delete', '', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 1);
INSERT INTO sys_button (id, menu_id, name, code, description, created_at, created_by, updated_at, updated_by) VALUES (14, 6, '按钮新增','sys:button:add', '', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 1);
INSERT INTO sys_button (id, menu_id, name, code, description, created_at, created_by, updated_at, updated_by) VALUES (15, 6, '按钮修改','sys:button:edit', '', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 1);
INSERT INTO sys_button (id, menu_id, name, code, description, created_at, created_by, updated_at, updated_by) VALUES (16, 6, '按钮删除','sys:button:delete', '', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 1);
INSERT INTO sys_button (id, menu_id, name, code, description, created_at, created_by, updated_at, updated_by) VALUES (17, 6, '按钮查询','sys:button:get', '', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 1);
INSERT INTO sys_button (id, menu_id, name, code, description, created_at, created_by, updated_at, updated_by) VALUES (18, 8, '字典新增','sys:dict-type:add', '', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 1);
INSERT INTO sys_button (id, menu_id, name, code, description, created_at, created_by, updated_at, updated_by) VALUES (19, 8, '字典修改','sys:dict-type:edit', '', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 1);
INSERT INTO sys_button (id, menu_id, name, code, description, created_at, created_by, updated_at, updated_by) VALUES (20, 8, '字典删除','sys:dict-type:delete', '', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 1);
INSERT INTO sys_button (id, menu_id, name, code, description, created_at, created_by, updated_at, updated_by) VALUES (21, 8, '字典配置','sys_dict-type:dict-data', '', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 1);
INSERT INTO sys_button (id, menu_id, name, code, description, created_at, created_by, updated_at, updated_by) VALUES (22, 8, '字典数据新增','sys:dict-data:add', '', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 1);
INSERT INTO sys_button (id, menu_id, name, code, description, created_at, created_by, updated_at, updated_by) VALUES (23, 8, '字典数据修改','sys:dict-data:edit', '', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 1);
INSERT INTO sys_button (id, menu_id, name, code, description, created_at, created_by, updated_at, updated_by) VALUES (24, 8, '字典数据删除','sys:dict-data:delete', '', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 1);
INSERT INTO sys_button (id, menu_id, name, code, description, created_at, created_by, updated_at, updated_by) VALUES (25, 9, '活跃用户','sys:token:get', '', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 1);
INSERT INTO sys_button (id, menu_id, name, code, description, created_at, created_by, updated_at, updated_by) VALUES (26, 9, '踢出','sys:token:revoked', '', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 1);
INSERT INTO sys_button (id, menu_id, name, code, description, created_at, created_by, updated_at, updated_by) VALUES (27, 11, '默认角色修改','sys:default-role:update', '', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 1);


INSERT INTO sys_role_button (role_id, button_id) VALUES (1, 1);
INSERT INTO sys_role_button (role_id, button_id) VALUES (1, 2);
INSERT INTO sys_role_button (role_id, button_id) VALUES (1, 3);
INSERT INTO sys_role_button (role_id, button_id) VALUES (1, 4);
INSERT INTO sys_role_button (role_id, button_id) VALUES (1, 5);
INSERT INTO sys_role_button (role_id, button_id) VALUES (1, 6);
INSERT INTO sys_role_button (role_id, button_id) VALUES (1, 7);
INSERT INTO sys_role_button (role_id, button_id) VALUES (1, 8);
INSERT INTO sys_role_button (role_id, button_id) VALUES (1, 9);
INSERT INTO sys_role_button (role_id, button_id) VALUES (1, 10);
INSERT INTO sys_role_button (role_id, button_id) VALUES (1, 11);
INSERT INTO sys_role_button (role_id, button_id) VALUES (1, 12);
INSERT INTO sys_role_button (role_id, button_id) VALUES (1, 13);
INSERT INTO sys_role_button (role_id, button_id) VALUES (1, 14);
INSERT INTO sys_role_button (role_id, button_id) VALUES (1, 15);
INSERT INTO sys_role_button (role_id, button_id) VALUES (1, 16);
INSERT INTO sys_role_button (role_id, button_id) VALUES (1, 17);
INSERT INTO sys_role_button (role_id, button_id) VALUES (1, 18);
INSERT INTO sys_role_button (role_id, button_id) VALUES (1, 19);
INSERT INTO sys_role_button (role_id, button_id) VALUES (1, 20);
INSERT INTO sys_role_button (role_id, button_id) VALUES (1, 21);
INSERT INTO sys_role_button (role_id, button_id) VALUES (1, 22);
INSERT INTO sys_role_button (role_id, button_id) VALUES (1, 23);
INSERT INTO sys_role_button (role_id, button_id) VALUES (1, 24);
INSERT INTO sys_role_button (role_id, button_id) VALUES (1, 25);
INSERT INTO sys_role_button (role_id, button_id) VALUES (1, 26);
INSERT INTO sys_role_button (role_id, button_id) VALUES (1, 27);

INSERT INTO sys_default_role (id, role_id, created_at, created_by, updated_at, updated_by) VALUES (1, 2, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 1);

INSERT INTO sys_resource (id, name, url, method, description, created_at, created_by, updated_at, updated_by) VALUES (1, '用户api', '/users/**', 31, '用户模块管理', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 1);
INSERT INTO sys_resource (id, name, url, method, description, created_at, created_by, updated_at, updated_by) VALUES (2, '角色api', '/roles/**', 31, '角色模块管理', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 1);
INSERT INTO sys_resource (id, name, url, method, description, created_at, created_by, updated_at, updated_by) VALUES (3, '菜单api', '/menus/**', 31, '菜单模块管理', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 1);
INSERT INTO sys_resource (id, name, url, method, description, created_at, created_by, updated_at, updated_by) VALUES (4, '资源api', '/resources/**', 31, '资源模块管理', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 1);
INSERT INTO sys_resource (id, name, url, method, description, created_at, created_by, updated_at, updated_by) VALUES (5, '按钮api', '/buttons/**', 31, '按钮模块管理', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 1);
INSERT INTO sys_resource (id, name, url, method, description, created_at, created_by, updated_at, updated_by) VALUES (6, '字典模块api', '/dict-types/**', 31, '字典模块管理', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 1);
INSERT INTO sys_resource (id, name, url, method, description, created_at, created_by, updated_at, updated_by) VALUES (7, '字典数据api', '/dict-datas/**', 31, '字典数据管理', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 1);
INSERT INTO sys_resource (id, name, url, method, description, created_at, created_by, updated_at, updated_by) VALUES (9, '审核日志api', '/audit-logs/**', 31, '审核日志查询', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 1);
INSERT INTO sys_resource (id, name, url, method, description, created_at, created_by, updated_at, updated_by) VALUES (10, '默认角色api', '/default-roles/**', 31, '默认角色管理', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 1);
INSERT INTO sys_resource (id, name, url, method, description, created_at, created_by, updated_at, updated_by) VALUES (11, '令牌api', '/tokens/**', 31, '令牌管理', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 1);


INSERT INTO sys_role_resource (role_id, resource_id) VALUES (1, 1);
INSERT INTO sys_role_resource (role_id, resource_id) VALUES (1, 2);
INSERT INTO sys_role_resource (role_id, resource_id) VALUES (1, 3);
INSERT INTO sys_role_resource (role_id, resource_id) VALUES (1, 4);
INSERT INTO sys_role_resource (role_id, resource_id) VALUES (1, 5);
INSERT INTO sys_role_resource (role_id, resource_id) VALUES (1, 6);
INSERT INTO sys_role_resource (role_id, resource_id) VALUES (1, 7);
INSERT INTO sys_role_resource (role_id, resource_id) VALUES (1, 9);
INSERT INTO sys_role_resource (role_id, resource_id) VALUES (1, 10);
INSERT INTO sys_role_resource (role_id, resource_id) VALUES (1, 11);

INSERT INTO sys_dict_type (id, type, name, description, created_at, created_by, updated_at, updated_by) VALUES (1, 'user_status', '用户状态', '', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 1);
INSERT INTO sys_dict_type (id, type, name, description, created_at, created_by, updated_at, updated_by) VALUES (2, 'open_style', '打开方式', '', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 1);
INSERT INTO sys_dict_type (id, type, name, description, created_at, created_by, updated_at, updated_by) VALUES (3, 'target_type', '目标类型', '', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 1);

INSERT INTO sys_dict_data (id, type_id, label, value, description, created_at, created_by, updated_at, updated_by) VALUES (1, 1, '停用', 'inactive', '', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 1);
INSERT INTO sys_dict_data (id, type_id, label, value, description, created_at, created_by, updated_at, updated_by) VALUES (2, 1, '正常', 'active', '', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 1);
INSERT INTO sys_dict_data (id, type_id, label, value, description, created_at, created_by, updated_at, updated_by) VALUES (3, 1, '禁用', 'banned', '', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 1);
INSERT INTO sys_dict_data (id, type_id, label, value, description, created_at, created_by, updated_at, updated_by) VALUES (4, 2, '内部打开', '0', '', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 1);
INSERT INTO sys_dict_data (id, type_id, label, value, description, created_at, created_by, updated_at, updated_by) VALUES (5, 2, '外部打开', '1', '', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 1);
INSERT INTO sys_dict_data (id, type_id, label, value, description, created_at, created_by, updated_at, updated_by) VALUES (6, 3, '用户', 'user', '', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 1);
INSERT INTO sys_dict_data (id, type_id, label, value, description, created_at, created_by, updated_at, updated_by) VALUES (7, 3, '菜单', 'menu', '', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 1);
INSERT INTO sys_dict_data (id, type_id, label, value, description, created_at, created_by, updated_at, updated_by) VALUES (8, 3, '按钮', 'button', '', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 1);
INSERT INTO sys_dict_data (id, type_id, label, value, description, created_at, created_by, updated_at, updated_by) VALUES (9, 3, '角色', 'role', '', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 1);
INSERT INTO sys_dict_data (id, type_id, label, value, description, created_at, created_by, updated_at, updated_by) VALUES (10, 3, '字典类型', 'dict_type', '', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 1);
INSERT INTO sys_dict_data (id, type_id, label, value, description, created_at, created_by, updated_at, updated_by) VALUES (11, 3, '字典数据', 'dict_data', '', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 1);
INSERT INTO sys_dict_data (id, type_id, label, value, description, created_at, created_by, updated_at, updated_by) VALUES (12, 3, '资源', 'resource', '', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 1);
