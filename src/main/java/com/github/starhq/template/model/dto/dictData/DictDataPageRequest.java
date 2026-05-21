package com.github.starhq.template.model.dto.dictData;

import com.github.starhq.template.model.dto.page.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * Pagination request parameters for querying dictionary data entries.
 * <p>
 * This class extends {@link PageRequest} to inherit standard pagination fields
 * ({@code page}, {@code size}, {@code sort}) and adds dictionary-specific filters
 * for targeted data retrieval in admin console or data management scenarios.
 * <p>
 * <strong>Primary Use Cases:</strong>
 * <ul>
 *     <li><strong>Dictionary Management Console</strong>: Browse and filter dictionary data by type</li>
 *     <li><strong>Data Audit</strong>: Review dictionary entries for compliance or data quality</li>
 *     <li><strong>Dynamic Configuration</strong>: Query dictionary values for application configuration</li>
 * </ul>
 * <p>
 * <strong>Query Behavior:</strong>
 * <p>
 * When used with {@link com.github.starhq.template.mapper.SysDictDataMapper#selectDictDataPage},
 * the filters are applied as follows:
 * <ul>
 *     <li>{@code dictTypeId}: Exact match on {@code sys_dict_data.type_id} foreign key</li>
 *     <li>Results are typically sorted by {@code sort_order} for consistent display order</li>
 * </ul>
 * <p>
 * <strong>Serialization:</strong>
 * <p>
 * This class implements {@link java.io.Serializable} with a fixed {@code serialVersionUID}
 * to ensure compatibility when caching request objects or transmitting across service boundaries.
 *
 * @author starhq
 * @version 1.0
 * @date 2026-05-20
 * @see PageRequest
 * @see com.github.starhq.template.service.DictDataService
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class DictDataPageRequest extends PageRequest {

    /**
     * Serial version UID for serialization compatibility.
     * <p>
     * Required when this DTO is stored in distributed caches (Redis) or
     * transmitted across service boundaries. Update this value only if the
     * class structure changes in a backward-incompatible way.
     *
     * @see java.io.Serializable
     */
    @Serial
    private static final long serialVersionUID = -2125107931331827571L;

    /**
     * Filter dictionary data entries by the associated dictionary type identifier.
     * <p>
     * <strong>Validation Rules:</strong>
     * <ul>
     *     <li>Optional: May be {@code null} for unfiltered queries (returns all dictionary data)</li>
     *     <li>Business Constraint: If provided, must reference an existing {@code SysDictType} record</li>
     * </ul>
     * <p>
     * <strong>Query Semantics:</strong>
     * <ul>
     *     <li>Exact match: {@code WHERE type_id = :dictTypeId}</li>
     *     <li>If {@code null}: No filtering by type (returns all dictionary data entries)</li>
     * </ul>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Fetch all dictionary data for "User Status" type (typeId=1)
     * DictDataPageRequest request = new DictDataPageRequest();
     * request.setDictTypeId(1L);
     * request.setPage(1);
     * request.setSize(20);
     *
     * IPage<DictDataPageVO> result = dictDataService.page(request);
     * }
     * </pre>
     *
     * @see com.github.starhq.template.entity.SysDictType
     */
    private Long dictTypeId;

}
