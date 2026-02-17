package com.github.starhq.template.config.handler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;
import org.springframework.util.StringUtils;

import com.github.starhq.template.enums.BaseEnum;
import com.github.starhq.template.enums.TargetType;
import com.github.starhq.template.enums.UserStatus;
import com.github.starhq.template.exception.BusinessException;

@MappedTypes({ UserStatus.class, TargetType.class })
public class StringEnumTypeHandler<E extends Enum<E> & BaseEnum<E, String>> extends BaseTypeHandler<E> {

    private final Class<E> type;
    private final Map<String, E> enumMap;

    public StringEnumTypeHandler(Class<E> type) {
        if (type == null) {
            throw new BusinessException("validation.enum.null_type");
        }
        this.type = type;

        E[] enums = type.getEnumConstants();
        if (enums == null) {
            throw new BusinessException("validation.enum.invalid", new Object[] { type.getSimpleName() });
        }

        this.enumMap = Arrays.stream(enums)
                .collect(Collectors.toMap(BaseEnum::getValue, e -> e));

    }

    /**
     * Sets a non-null enum parameter on a PreparedStatement.
     *
     * @param ps        The PreparedStatement to set the parameter on
     * @param i         The parameter index
     * @param parameter The enum value to set
     * @param jdbcType  The JDBC type (not used in this implementation)
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, E parameter, JdbcType jdbcType) throws SQLException {
        ps.setObject(i, parameter.getValue(), Types.OTHER);
    }

    /**
     * Gets an enum value from a ResultSet using a column name.
     *
     * @param rs         The ResultSet to read from
     * @param columnName The name of the column to read
     * @return The enum value, or null if the column value was null
     * @throws SQLException if a database access error occurs
     */
    @Override
    public E getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return toEnum(value);
    }

    /**
     * Gets an enum value from a ResultSet using a column index.
     *
     * @param rs          The ResultSet to read from
     * @param columnIndex The index of the column to read
     * @return The enum value, or null if the column value was null
     * @throws SQLException if a database access error occurs
     */
    @Override
    public E getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return toEnum(value);
    }

    /**
     * Gets an enum value from a CallableStatement using a column index.
     *
     * @param cs          The CallableStatement to read from
     * @param columnIndex The index of the column to read
     * @return The enum value, or null if the column value was null
     * @throws SQLException if a database access error occurs
     */
    @Override
    public E getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return toEnum(value);
    }

    /**
     * Converts a string value to its corresponding enum constant.
     * Uses a ConcurrentHashMap for efficient lookup instead of linear search.
     *
     * @param value The string value to convert
     * @return The corresponding enum constant, or null if the value is null
     * @throws BusinessException if the value cannot be converted to an enum
     *                           constant
     */
    private E toEnum(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        E result = enumMap.get(value);
        if (result == null) {
            throw new BusinessException("validation.enum.invalid", new Object[] { type.getSimpleName() });
        }
        return result;
    }

}
