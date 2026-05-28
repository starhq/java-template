package com.github.starhq.template.model.vo.dict.type;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * Hierarchical view object for dictionary type with its associated data entries.
 * <p>
 * This class encapsulates a dictionary type definition along with its child
 * key-value pairs ({@link DictDataVO}), enabling efficient one-shot retrieval
 * of complete dictionary structures for frontend initialization or configuration export.
 * Designed for scenarios where the full hierarchy is needed without multiple API calls.
 * <p>
 * <strong>Primary Use Cases:</strong>
 * <ul>
 *     <li><strong>Frontend Initialization</strong>: Load all dictionary types and data for dropdown/cascader components in a single request</li>
 *     <li><strong>Configuration Export</strong>: Export complete dictionary definitions for backup, migration, or documentation generation</li>
 *     <li><strong>Cache Preloading</strong>: Populate application-level cache with full dictionary structure at startup</li>
 *     <li><strong>Admin Console</strong>: Display dictionary types with expandable data lists for management</li>
 * </ul>
 * <p>
 * <strong>Hierarchical Structure:</strong>
 * <pre>
 * {@code
 * {
 *   "dictType": "user_status",
 *   "dataList": [
 *     { "label": "Enabled", "value": "1" },
 *     { "label": "Disabled", "value": "0" }
 *   ]
 * }
 * }
 * </pre>
 * <p>
 * <strong>Design Principles:</strong>
 * <ul>
 *     <li><strong>Minimalism</strong>: Only includes fields essential for dictionary hierarchy representation (type code + data list)</li>
 *     <li><strong>Immutability-Friendly</strong>: Stateless VO suitable for caching and concurrent access</li>
 *     <li><strong>Serialization-Ready</strong>: Implements {@link Serializable} with fixed {@code serialVersionUID} for cross-JVM compatibility</li>
 *     <li><strong>Framework-Neutral</strong>: No framework-specific annotations; usable in any Java context</li>
 * </ul>
 *
 * @author wangjian
 * @author starhq (maintainer)
 * @version 1.0
 * @date 2026-04-09
 * @see DictDataVO
 * @see com.github.starhq.template.entity.SysDictType
 * @see com.github.starhq.template.service.DictTypeService
 */
@Data
public class DictTypeWithDataVO implements Serializable {

    /**
     * Serial version UID for serialization compatibility.
     * <p>
     * Required when this VO is stored in distributed caches (Redis) or
     * transmitted across service boundaries (e.g., via RPC or messaging).
     * Update this value only if the class structure changes in a
     * backward-incompatible way (e.g., removing fields, changing types).
     *
     * @see java.io.Serializable
     */
    @Serial
    private static final long serialVersionUID = -5804201726027503401L;

    /**
     * The unique technical identifier/code for this dictionary type.
     * <p>
     * This field serves as the primary key for business logic to fetch associated
     * dictionary data entries. Typically follows {@code snake_case} or {@code camelCase}
     * conventions for readability and IDE auto-completion:
     * <ul>
     *     <li>{@code "user_status"} — Status codes for user accounts</li>
     *     <li>{@code "orderType"} — Classification of order types</li>
     *     <li>{@code "payment_method"} — Supported payment options</li>
     * </ul>
     * <p>
     * <strong>Usage Pattern:</strong>
     * <pre>
     * {@code
     * // Frontend: Use dictType as key to access data list
     * const dictionaries = {
     *   user_status: [
     *     { label: "Enabled", value: "1" },
     *     { label: "Disabled", value: "0" }
     *   ],
     *   order_type: [
     *     { label: "Standard", value: "STD" },
     *     { label: "Express", value: "EXP" }
     *   ]
     * };
     *
     * // Usage in form binding
     * <select v-model="form.status" :options="dictionaries.user_status" />
     * }
     * </pre>
     * <p>
     * <strong>Best Practices:</strong>
     * <ul>
     *     <li>Use lowercase with underscores ({@code snake_case}) for consistency: {@code "user_status"}</li>
     *     <li>Avoid special characters except underscores; restrict to {@code [a-z0-9_]} via validation if needed</li>
     *     <li>Prefix with module name for large systems: {@code "sys_user_status"}, {@code "biz_order_type"}</li>
     *     <li>Document type codes in a centralized registry for team reference</li>
     * </ul>
     *
     * @see com.github.starhq.template.entity.SysDictType#getType()
     */
    private String dictType;

    /**
     * The list of key-value pairs ({@link DictDataVO}) belonging to this dictionary type.
     * <p>
     * Each entry in the list represents a selectable option for UI components or a
     * configurable value for business logic. The list is typically sorted by
     * {@code sort_order} for consistent display across the application.
     * <p>
     * <strong>Data Structure:</strong>
     * <pre>
     * {@code
     * [
     *   { "label": "Enabled", "value": "1" },
     *   { "label": "Disabled", "value": "0" }
     * ]
     * }
     * </pre>
     * <p>
     * <strong>Frontend Integration:</strong>
     * <pre>
     * {@code
     * // Vue 3: Populate a cascader with dictionary types
     * <a-cascader :options="dictOptions" v-model="selectedValue" />
     *
     * <script setup>
     * const { data: dictTypes } = useRequest(() => api.getAllDictTypesWithData());
     *
     * const dictOptions = computed(() =>
     *   dictTypes.value?.map(type => ({
     *     label: type.dictType,
     *     value: type.dictType,
     *     children: type.dataList?.map(data => ({
     *       label: data.label,
     *       value: data.value
     *     }))
     *   })) || []
     * );
     * </script>
     * }
     * </pre>
     * <p>
     * <strong>Null-Safety:</strong>
     * <ul>
     *     <li>Should never be {@code null} in well-formed responses; return empty list {@code []} if no data entries exist</li>
     *     <li>Frontend should handle empty lists gracefully (e.g., show "No options" message)</li>
     * </ul>
     * <p>
     * <strong>Performance Considerations:</strong>
     * <ul>
     *     <li>For large dictionary types (>100 data entries), consider pagination or lazy-loading in UI</li>
     *     <li>Cache the entire hierarchy by {@code dictType} key for efficient repeated access</li>
     *     <li>Avoid including unused fields in {@code DictDataVO} to minimize payload size</li>
     * </ul>
     *
     * @see DictDataVO
     * @see com.github.starhq.template.entity.SysDictData
     */
    private List<DictDataVO> dataList;

}