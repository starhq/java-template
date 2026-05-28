package com.github.starhq.template.common.constant;

import lombok.experimental.UtilityClass;

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
@UtilityClass
public class QueryConstant {

    /**
     * Query parameter key mapped to the username field.
     */
    public static final String USERNAME = "username";

    /**
     * Query parameter key mapped to the user's unique identifier.
     */
    public static final String USER_ID = "user_id";

    /**
     * Query parameter key used to define the sorting order (e.g., 'asc' or 'desc').
     */
    public static final String SORT = "sort_order";

    /**
     * Query parameter key mapped to a dictionary or category type identifier.
     */
    public static final String TYPE_ID = "type_id";

    /**
     * Query parameter key mapped to the menu's unique identifier.
     */
    public static final String MENU_ID = "menu_id";

    /**
     * Query parameter key used for filtering by the target entity type (e.g., in audit logs).
     */
    public static final String TARGET_TYPE = "target_type";

    /**
     * Query parameter key for nested property access.
     *
     * <p>Specifically used in frameworks like MyBatis-Plus to resolve associated table
     * fields, translating to an SQL JOIN query like {@code creator.username}.
     */
    public static final String CREATOR = "creator.username";
}