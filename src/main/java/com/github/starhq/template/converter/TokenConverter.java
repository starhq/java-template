package com.github.starhq.template.converter;

import com.github.starhq.template.entity.SysToken;
import com.github.starhq.template.model.dto.token.TokenSimpleDTO;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

/**
 * MapStruct converter interface for transforming {@link SysToken} entities
 * between persistence layer and internal DTO layers.
 * <p>
 * This converter supports unidirectional mapping for authentication token management:
 * <ul>
 *     <li>Entity → {@link TokenSimpleDTO}: For internal service communication, token audit logs,
 *     or lightweight token metadata exposure (excluding sensitive fields like raw token values)</li>
 * </ul>
 * <p>
 * <strong>Security Note:</strong>
 * <p>
 * This converter intentionally maps only non-sensitive fields. Sensitive data such as
 * {@code accessToken}, {@code refreshToken}, or {@code secretKey} should NEVER be exposed
 * via DTOs returned to clients. Always filter sensitive fields at the Service or Controller layer.
 * <p>
 * Configuration:
 * <ul>
 *     <li>{@code componentModel = "spring"}: Managed as a Spring bean for dependency injection</li>
 *     <li>{@code nullValuePropertyMappingStrategy = IGNORE}: Skip null source properties to preserve target defaults</li>
 *     <li>{@code unmappedTargetPolicy = WARN}: <strong>Development-time safeguard</strong> — emits compiler warnings
 *     if target fields are unmapped, helping catch missing field mappings early. Recommended to switch
 *     to {@code IGNORE} in production if partial mapping is intentional.</li>
 * </ul>
 *
 * @author starhq
 * @version 1.0
 * @date 2026-05-20
 * @see <a href="https://mapstruct.org/documentation/stable/reference/html/">MapStruct Reference Guide</a>
 */
@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface TokenConverter {

    /**
     * Converts a {@link SysToken} entity to a {@link TokenSimpleDTO} for internal use.
     * <p>
     * Typically used for:
     * <ul>
     *     <li>Token audit logging (recording token metadata without exposing secrets)</li>
     *     <li>Internal service-to-service communication where only token identifiers are needed</li>
     *     <li>Admin console token management views (displaying issuance time, expiry, device info)</li>
     * </ul>
     * <p>
     * <strong>Important:</strong> This method performs a shallow field-by-field mapping.
     * Ensure that sensitive fields (e.g., {@code accessToken}, {@code refreshToken}) are
     * explicitly excluded via {@code @Mapping(ignore = true)} or omitted from {@link TokenSimpleDTO}.
     *
     * @param entity the source {@link SysToken} entity from persistence layer
     * @return the target {@link TokenSimpleDTO} containing non-sensitive token metadata,
     * or {@code null} if the input entity is {@code null}
     */
    TokenSimpleDTO toSimpleDTO(SysToken entity);

}