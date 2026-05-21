package com.github.starhq.template.converter;

import com.github.starhq.template.entity.SysAuditLog;
import com.github.starhq.template.model.vo.auditlog.AuditLogPageVO;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

/**
 * MapStruct converter interface for transforming {@link SysAuditLog} entities
 * to {@link AuditLogPageVO} view objects.
 * <p>
 * This converter is used in audit log pagination scenarios to map database
 * entity fields to frontend-displayable view objects, ensuring separation
 * between persistence layer and presentation layer.
 * <p>
 * Configuration:
 * <ul>
 *     <li>{@code componentModel = "spring"}: Managed as a Spring bean for dependency injection</li>
 *     <li>{@code nullValuePropertyMappingStrategy = IGNORE}: Skip null source properties to preserve target defaults</li>
 *     <li>{@code unmappedTargetPolicy = IGNORE}: Allow partial field mapping for flexible VO evolution</li>
 * </ul>
 *
 * @author wangjian
 * @author starhq (maintainer)
 * @version 1.0
 * @date 2026-04-10
 * @see <a href="https://mapstruct.org/documentation/stable/reference/html/">MapStruct Reference Guide</a>
 */
@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface AuditLogConverter {

    /**
     * Converts a {@link SysAuditLog} entity to a {@link AuditLogPageVO} for pagination display.
     * <p>
     * This method performs a shallow field-by-field mapping. Nested objects or complex
     * transformations should be handled via custom {@code @Mapping} expressions or
     * {@code default} methods in this interface.
     *
     * @param entity the source {@link SysAuditLog} entity from persistence layer
     * @return the target {@link AuditLogPageVO} for admin console pagination display,
     *         or {@code null} if the input entity is {@code null}
     */
    AuditLogPageVO toPageVO(SysAuditLog entity);

}