package com.github.starhq.template.config.mybatis.handler;

import com.github.starhq.template.common.enums.BaseEnum;
import com.github.starhq.template.common.enums.ErrorCode;
import com.github.starhq.template.common.enums.TargetType;
import com.github.starhq.template.common.enums.UserStatus;
import com.github.starhq.template.common.exception.DatabaseException;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;
import org.springframework.util.StringUtils;

import java.sql.*;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * MyBatis TypeHandler for transparently mapping between database string columns
 * and Java {@link BaseEnum} implementations storing String values (e.g., {@link UserStatus}, {@link TargetType}).
 *
 * <p>This handler ensures that when reading from the database, string values (like "active") are
 * automatically converted to their corresponding Enum instances, and vice versa when writing.
 *
 * <p>Note: For enums storing Integer values (like {@link com.github.starhq.template.common.enums.HttpMethod}),
 * a separate Integer-based TypeHandler is required.
 *
 * @param <E> the specific Enum type that implements {@link BaseEnum} with String generic type
 */
@MappedTypes({UserStatus.class, TargetType.class})
public class StringEnumTypeHandler<E extends Enum<E> & BaseEnum<E, String>> extends BaseTypeHandler<E> {

    /**
     * The concrete Enum class type this handler is managing.
     */
    private final Class<E> enumType;

    /**
     * Pre-computed lookup map for O(1) conversion from database string value to Java Enum.
     * <p>Prevents the performance overhead of iterating through {@code Enum.values()} on every row read.
     */
    private final Map<String, E> valueToEnumMap;

    /**
     * Initializes the type handler and pre-constructs the value-to-enum lookup map.
     *
     * @param enumType the Class object of the target Enum
     * @throws IllegalArgumentException if the provided class is null or not a valid Enum
     */
    public StringEnumTypeHandler(Class<E> enumType) {
        if (enumType == null) {
            throw new IllegalArgumentException("Type argument cannot be null");
        }
        this.enumType = enumType;

        E[] enumConstants = this.enumType.getEnumConstants();
        if (enumConstants == null || enumConstants.length == 0) {
            throw new IllegalArgumentException(this.enumType.getSimpleName() + " does not represent an enum type.");
        }

        // Pre-process all enum constants into a HashMap for fast O(1) lookups during deserialization
        this.valueToEnumMap = Arrays.stream(
                        enumConstants)
                .collect(Collectors.toMap(BaseEnum::getValue, e -> e));
    }

    /**
     * Sets a non-null enum parameter on a {@link PreparedStatement}.
     *
     * <p>Extracts the underlying string value from the Enum using {@link BaseEnum#getValue()}
     * and passes it to the JDBC driver.
     *
     * @param ps        The PreparedStatement to set the parameter on
     * @param i         The 1-based parameter index
     * @param parameter The enum value to set
     * @param jdbcType  The JDBC type (ignored here, we explicitly define Types.OTHER)
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, E parameter, JdbcType jdbcType) throws SQLException {
        // Using Types.OTHER ensures PostgreSQL (and other strict databases) treat the string unquoted.
        // Without it, Postgres might cast the string to an unknown type and fail on ENUM/varchar columns.
        ps.setObject(i, parameter.getValue(), Types.OTHER);
    }

    /**
     * Gets an enum value from a {@link ResultSet} using a column name.
     *
     * @param rs         The ResultSet to read from
     * @param columnName The name of the column to read
     * @return The corresponding enum value, or null if the SQL column value was NULL
     * @throws SQLException if a database access error occurs
     */
    @Override
    public E getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return toEnum(value);
    }

    /**
     * Gets an enum value from a {@link ResultSet} using a column index.
     *
     * @param rs          The ResultSet to read from
     * @param columnIndex The 1-based index of the column to read
     * @return The corresponding enum value, or null if the SQL column value was NULL
     * @throws SQLException if a database access error occurs
     */
    @Override
    public E getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return toEnum(value);
    }

    /**
     * Gets an enum value from a {@link CallableStatement} (stored procedure output).
     *
     * @param cs          The CallableStatement to read from
     * @param columnIndex The 1-based index of the output parameter to read
     * @return The corresponding enum value, or null if the output value was NULL
     * @throws SQLException if a database access error occurs
     */
    @Override
    public E getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return toEnum(value);
    }

    /**
     * Core conversion logic: Maps a raw database string to its corresponding Java Enum.
     *
     * <p><b>Error Handling Strategy:</b> If the database contains dirty data (a string that does not
     * match any defined Enum value), this method throws a {@link DatabaseException}.
     * <br><b>Why not BusinessException?</b> Because this error occurs during the MyBatis data access phase.
     * Throwing a 500-level DatabaseException clearly indicates that the source data is corrupt,
     * whereas a BusinessException implies the user sent a bad request (which is false in this context).
     *
     * @param value the raw string retrieved from the database
     * @return the mapped Enum constant, or {@code null} if the database value was null/empty
     * @throws DatabaseException if the string cannot be mapped to any known Enum constant
     */
    private E toEnum(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        // O(1) lookup via pre-computed map
        E result = this.valueToEnumMap.get(value);

        if (result == null) {
            // Includes the unexpected value in the exception arguments to aid in debugging data corruption
            throw new DatabaseException(ErrorCode.DB_MAPPING_ERROR,
                    this.enumType.getSimpleName(), value);
        }
        return result;
    }

}