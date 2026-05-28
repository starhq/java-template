package com.github.starhq.template.common.constant;

import lombok.experimental.UtilityClass;

/**
 * Constants for Audit Log actions.
 *
 * <p>Defines standardized action descriptions for system operations.
 * These constants are primarily used in conjunction with the {@code @AuditLoggable} annotation
 * to ensure consistency in audit log records.
 *
 * @see com.github.starhq.template.aop.annotation.AuditLoggable#action()
 */
@UtilityClass
public class AuditLogConstant {

    /**
     * Action constant for updating a user.
     */
    public static final String USER_UPDATE = "UPDATE_USER";

    /**
     * Action constant for removing a user.
     */
    public static final String USER_REMOVE = "REMOVE_USER";

    /**
     * Action constant for inserting/creating a user.
     */
    public static final String USER_INSERT = "INSERT_USER";

    /**
     * Action constant for updating a role.
     */
    public static final String ROLE_UPDATE = "UPDATE_ROLE";

    /**
     * Action constant for removing a role.
     */
    public static final String ROLE_REMOVE = "REMOVE_ROLE";

    /**
     * Action constant for inserting/creating a role.
     */
    public static final String ROLE_INSERT = "INSERT_ROLE";

    /**
     * Action constant for updating a resource.
     */
    public static final String RESOURCE_UPDATE = "UPDATE_RESOURCE";

    /**
     * Action constant for removing a resource.
     */
    public static final String RESOURCE_REMOVE = "REMOVE_RESOURCE";

    /**
     * Action constant for inserting/creating a resource.
     */
    public static final String RESOURCE_INSERT = "INSERT_RESOURCE";

    /**
     * Action constant for updating a button.
     */
    public static final String BUTTON_UPDATE = "UPDATE_BUTTON";

    /**
     * Action constant for removing a button.
     */
    public static final String BUTTON_REMOVE = "REMOVE_BUTTON";

    /**
     * Action constant for inserting/creating a button.
     */
    public static final String BUTTON_INSERT = "INSERT_BUTTON";

    /**
     * Action constant for updating a menu.
     */
    public static final String MENU_UPDATE = "UPDATE_MENU";

    /**
     * Action constant for removing a menu.
     */
    public static final String MENU_REMOVE = "REMOVE_MENU";

    /**
     * Action constant for inserting/creating a menu.
     */
    public static final String MENU_INSERT = "INSERT_MENU";

    /**
     * Action constant for updating a dictionary type.
     */
    public static final String DICT_TYPE_UPDATE = "UPDATE_DICT_TYPE";

    /**
     * Action constant for removing a dictionary type.
     */
    public static final String DICT_TYPE_REMOVE = "REMOVE_DICT_TYPE";

    /**
     * Action constant for inserting/creating a dictionary type.
     */
    public static final String DICT_TYPE_INSERT = "INSERT_DICT_TYPE";

    /**
     * Action constant for updating a dictionary data entry.
     */
    public static final String DICT_DATA_UPDATE = "UPDATE_DICT_DATA";

    /**
     * Action constant for removing a dictionary data entry.
     */
    public static final String DICT_DATA_REMOVE = "REMOVE_DICT_DATA";

    /**
     * Action constant for inserting/creating a dictionary data entry.
     */
    public static final String DICT_DATA_INSERT = "INSERT_DICT_DATA";

}
