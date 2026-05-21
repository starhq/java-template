package com.github.starhq.template.common.constant;

/**
 * Constants for query parameters.
 *
 * <p>Defines standardized keys used for constructing database queries,
 * filtering conditions, and sorting criteria. Using these constants prevents
 * hardcoded strings in Mapper XML or QueryWrapper logic, ensuring type safety
 * and consistency across the data access layer.
 *
 * @author wangjian
 */
public interface QueryConstant {

    /**
     * Query parameter key mapped to the username field.
     */
    String USERNAME = "username";

    /**
     * Query parameter key mapped to the user's unique identifier.
     */
    String USER_ID = "user_id";

    /**
     * Query parameter key used to define the sorting order (e.g., 'asc' or 'desc').
     */
    String SORT = "sort_order";

    /**
     * Query parameter key mapped to a dictionary or category type identifier.
     */
    String TYPE_ID = "type_id";

    /**
     * Query parameter key mapped to the menu's unique identifier.
     */
    String MENU_ID = "menu_id";

    /**
     * Query parameter key used for filtering by the target entity type (e.g., in audit logs).
     */
    String TARGET_TYPE = "target_type";

    /**
     * Query parameter key for nested property access.
     *
     * <p>Specifically used in frameworks like MyBatis-Plus to resolve associated table
     * fields, translating to an SQL JOIN query like {@code creator.username}.
     */
    String CREATOR = "creator.username";
}