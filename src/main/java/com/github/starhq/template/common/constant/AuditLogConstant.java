package com.github.starhq.template.common.constant;

/**
 * Constants for Audit Log actions.
 *
 * <p>Defines standardized action descriptions for system operations.
 * These constants are primarily used in conjunction with the {@code @AuditLoggable} annotation
 * to ensure consistency in audit log records.
 *
 * @see com.github.starhq.template.aop.annotation.AuditLoggable#action()
 */
public interface AuditLogConstant {

    /**
     * Action constant for updating a user.
     */
    String USER_UPDATE = "UPDATE_USER";

    /**
     * Action constant for removing a user.
     */
    String USER_REMOVE = "REMOVE_USER";

    /**
     * Action constant for inserting/creating a user.
     */
    String USER_INSERT = "INSERT_USER";

    /**
     * Action constant for updating a role.
     */
    String ROLE_UPDATE = "UPDATE_ROLE";

    /**
     * Action constant for removing a role.
     */
    String ROLE_REMOVE = "REMOVE_ROLE";

    /**
     * Action constant for inserting/creating a role.
     */
    String ROLE_INSERT = "INSERT_ROLE";

    /**
     * Action constant for updating a resource.
     */
    String RESOURCE_UPDATE = "UPDATE_RESOURCE";

    /**
     * Action constant for removing a resource.
     */
    String RESOURCE_REMOVE = "REMOVE_RESOURCE";

    /**
     * Action constant for inserting/creating a resource.
     */
    String RESOURCE_INSERT = "INSERT_RESOURCE";

    /**
     * Action constant for updating a button.
     */
    String BUTTON_UPDATE = "UPDATE_BUTTON";

    /**
     * Action constant for removing a button.
     */
    String BUTTON_REMOVE = "REMOVE_BUTTON";

    /**
     * Action constant for inserting/creating a button.
     */
    String BUTTON_INSERT = "INSERT_BUTTON";

    /**
     * Action constant for updating a menu.
     */
    String MENU_UPDATE = "UPDATE_MENU";

    /**
     * Action constant for removing a menu.
     */
    String MENU_REMOVE = "REMOVE_MENU";

    /**
     * Action constant for inserting/creating a menu.
     */
    String MENU_INSERT = "INSERT_MENU";

    /**
     * Action constant for updating a dictionary type.
     */
    String DICT_TYPE_UPDATE = "UPDATE_DICT_TYPE";

    /**
     * Action constant for removing a dictionary type.
     */
    String DICT_TYPE_REMOVE = "REMOVE_DICT_TYPE";

    /**
     * Action constant for inserting/creating a dictionary type.
     */
    String DICT_TYPE_INSERT = "INSERT_DICT_TYPE";

    /**
     * Action constant for updating a dictionary data entry.
     */
    String DICT_DATA_UPDATE = "UPDATE_DICT_DATA";

    /**
     * Action constant for removing a dictionary data entry.
     */
    String DICT_DATA_REMOVE = "REMOVE_DICT_DATA";

    /**
     * Action constant for inserting/creating a dictionary data entry.
     */
    String DICT_DATA_INSERT = "INSERT_DICT_DATA";

}
