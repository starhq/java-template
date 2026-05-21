package com.github.starhq.template.config.mybatis.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.github.starhq.template.common.constant.ProfileConstants;
import com.github.starhq.template.common.util.SecurityContextUtils;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import java.time.OffsetDateTime;

/**
 * MyBatis-Plus automatic field population handler for base audit columns.
 *
 * <p>Intercepts entity insert and update operations to automatically populate
 * creation and modification timestamps, as well as the operator's user ID.
 * This eliminates the need to manually set these fields in the service layer.
 *
 * @author starhq
 */
@RequiredArgsConstructor
public class BaseEntityHandler implements MetaObjectHandler {

    /**
     * Spring Environment used to determine the active application profile.
     */
    private final Environment environment;

    /**
     * Populates audit fields before an INSERT operation.
     *
     * <p>Uses {@code strictInsertFill}, which means if the field already has a non-null value
     * explicitly set in the code, it will NOT be overwritten. This preserves the ability to
     * manually force specific timestamps or operator IDs if absolutely necessary.
     *
     * @param metaObject the MetaObject containing the entity's field values
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        OffsetDateTime now = OffsetDateTime.now();
        Long currentUserId = getCurrentUserId();

        // Only set if the field is currently null
        this.strictInsertFill(metaObject, "createdAt", OffsetDateTime.class, now);
        this.strictInsertFill(metaObject, "createdBy", Long.class, currentUserId);
    }

    /**
     * Populates audit fields before an UPDATE operation.
     *
     * <p>Uses {@code strictUpdateFill} to ensure that modification timestamps and operator IDs
     * are always updated on change, but only if the entity object holds a null value for these fields.
     *
     * @param metaObject the MetaObject containing the entity's field values
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        OffsetDateTime now = OffsetDateTime.now();
        Long currentUserId = getCurrentUserId();

        this.strictUpdateFill(metaObject, "updatedAt", OffsetDateTime.class, now);
        this.strictUpdateFill(metaObject, "updatedBy", Long.class, currentUserId);
    }

    /**
     * Retrieves the current operator's user ID.
     *
     * <p><b>⚠️ CRITICAL ARCHITECTURAL WARNING:</b> The current implementation hardcodes a fallback
     * user ID of {@code 1L} for non-production environments. While this makes local development and
     * writing unit tests easier (bypassing the need to mock the SecurityContext), it carries severe risks:
     * <ul>
     *   <li><b>Data Pollution on Staging:</b> If the staging/test environment uses real shared databases,
     *       all records created via test APIs will incorrectly show user ID 1 as the creator.</li>
     *   <li><b>Audit Log Integrity:</b> Audit trails will be fundamentally broken in non-prod environments,
     *       making it impossible to trace who actually performed a test action.</li>
     * </ul>
     *
     * <p><b>Recommended Alternatives:</b>
     * <ul>
     *   <li>If you need a valid user in tests, use MockMvc to log in and obtain a real token first.</li>
     *   <li>Keep this fallback only if the local environment connects to an isolated, disposable H2/in-memory DB.</li>
     * </ul>
     *
     * @return the current user's ID from the security context, or {@code 1L} if not in production
     */
    private Long getCurrentUserId() {
        boolean isProd = environment.acceptsProfiles(Profiles.of(ProfileConstants.PROD));
        return isProd ? SecurityContextUtils.getRequiredUserId() : 1L;
    }
}
