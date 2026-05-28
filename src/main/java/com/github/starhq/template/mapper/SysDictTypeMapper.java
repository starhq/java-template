package com.github.starhq.template.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.starhq.template.entity.SysDictType;
import com.github.starhq.template.model.vo.dict.data.DictDataSimpleVO;
import com.github.starhq.template.model.vo.dict.type.DictTypeWithDataVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * MyBatis-Plus mapper interface for {@link SysDictType} entity with nested data loading support.
 * <p>
 * This interface extends {@link BaseMapper} to inherit standard CRUD operations for dictionary
 * type definitions, and provides a specialized method {@link #selectDictTypesWithData} for
 * efficiently fetching dictionary types along with their associated data entries in a single query.
 * <p>
 * <strong>Primary Use Cases:</strong>
 * <ul>
 *     <li><strong>Frontend Initialization</strong>: Load all dictionary types and data for dropdown/cascader components</li>
 *     <li><strong>Configuration Export</strong>: Export complete dictionary definitions for backup or migration</li>
 *     <li><strong>Cache Preloading</strong>: Populate application-level cache with full dictionary structure at startup</li>
 * </ul>
 * <p>
 * <strong>Design Principles:</strong>
 * <ul>
 *     <li><strong>Single Query Efficiency</strong>: Avoid N+1 problem by joining type and data tables in one SQL</li>
 *     <li><strong>Nested Result Mapping</strong>: Map flat SQL results to hierarchical {@link DictTypeWithDataVO} structure</li>
 *     <li><strong>Read-Optimized</strong>: Designed for infrequent writes but frequent reads; suitable for caching</li>
 * </ul>
 * <p>
 * <strong>Usage Example:</strong>
 * <pre>
 * {@code
 * @Service
 * public class DictTypeService {
 *     @Autowired private SysDictTypeMapper dictTypeMapper;
 *
 *     // Load all dictionary types with nested data for frontend initialization
 *     public List<DictTypeWithDataVO> getAllDictTypesWithData() {
 *         return dictTypeMapper.selectDictTypesWithData();
 *     }
 *
 *     // Preload into cache at application startup
 *     @PostConstruct
 *     public void preloadDictCache() {
 *         List<DictTypeWithDataVO> dictData = dictTypeMapper.selectDictTypesWithData();
 *         cacheHelper.put("dict:all", dictData, "dict:cache");
 *     }
 * }
 * }
 * </pre>
 *
 * @author starhq
 * @version 1.0
 * @date 2026-05-20
 * @see BaseMapper
 * @see SysDictType
 * @see DictTypeWithDataVO
 * @see com.github.starhq.template.service.DictTypeService
 */
@Mapper
public interface SysDictTypeMapper extends BaseMapper<SysDictType> {

    /**
     * Fetches all dictionary types with their associated data entries in a nested structure.
     * <p>
     * This method performs a single SQL query with {@code LEFT JOIN} between {@code sys_dict_type}
     * and {@code sys_dict_data} tables, then maps the flat result set to a hierarchical
     * {@link DictTypeWithDataVO} structure where each type contains a list of its data entries.
     * <p>
     * <strong>Query Behavior:</strong>
     * <ul>
     *     <li>Uses {@code LEFT JOIN} to include types even if they have no data entries</li>
     *     <li>Filters to active records only: {@code type.status = 1 AND (data.status = 1 OR data.id IS NULL)}</li>
     *     <li>Sorts by {@code type.sort_order}, {@code type.created_at}, then {@code data.sort_order} for consistent ordering</li>
     *     <li>MyBatis uses {@code <collection>} mapping to aggregate data entries under each type</li>
     * </ul>
     * <p>
     * <strong>Return Structure:</strong>
     * <pre>
     * {@code
     * [
     *   {
     *     "id": 1,
     *     "type": "user_status",
     *     "name": "User Status",
     *     "dataList": [
     *       {"id": 10, "label": "Enabled", "value": "1", "sortOrder": 1},
     *       {"id": 11, "label": "Disabled", "value": "0", "sortOrder": 2}
     *     ]
     *   },
     *   {
     *     "id": 2,
     *     "type": "order_type",
     *     "name": "Order Type",
     *     "dataList": []  // Empty if no data entries
     *   }
     * ]
     * }
     * </pre>
     * <p>
     * <strong>Performance Considerations:</strong>
     * <ul>
     *     <li>Add composite indexes: {@code sys_dict_data(type_id, status, sort_order)} for efficient join</li>
     *     <li>For large dictionary datasets (>100 types × 50 data each), consider pagination or lazy-loading</li>
     *     <li>Cache results with TTL (e.g., 30min) since dictionary definitions change infrequently</li>
     *     <li>Avoid calling this method in high-frequency request paths; prefer cached or preloaded data</li>
     * </ul>
     * <p>
     * <strong>XML Mapping Example:</strong>
     * <pre>
     * {@code
     * <!-- resources/mapper/SysDictTypeMapper.xml -->
     * <resultMap id="DictTypeWithDataVOMap" type="com.github.starhq.template.model.vo.dictType.DictTypeWithDataVO">
     *     <id property="id" column="type_id"/>
     *     <result property="type" column="type"/>
     *     <result property="name" column="type_name"/>
     *     <result property="description" column="type_description"/>
     *     <result property="sortOrder" column="type_sort_order"/>
     *     <result property="status" column="type_status"/>
     *     <collection property="dataList" ofType="com.github.starhq.template.model.vo.dictData.DictDataSimpleVO">
     *         <id property="id" column="data_id"/>
     *         <result property="label" column="data_label"/>
     *         <result property="value" column="data_value"/>
     *         <result property="sortOrder" column="data_sort_order"/>
     *         <result property="status" column="data_status"/>
     *     </collection>
     * </resultMap>
     *
     * <select id="selectDictTypesWithData" resultMap="DictTypeWithDataVOMap">
     *     SELECT
     *         t.id as type_id, t.type, t.name as type_name, t.description as type_description,
     *         t.sort_order as type_sort_order, t.status as type_status,
     *         d.id as data_id, d.label as data_label, d.value as data_value,
     *         d.sort_order as data_sort_order, d.status as data_status
     *     FROM sys_dict_type t
     *     LEFT JOIN sys_dict_data d ON t.id = d.type_id
     *         AND d.status = 1  -- Only join active data entries
     *     WHERE t.status = 1    -- Only active types
     *     ORDER BY t.sort_order ASC, t.created_at DESC, d.sort_order ASC
     * </select>
     * }
     * </pre>
     *
     * @return a list of {@link DictTypeWithDataVO} with nested {@code dataList}; empty list if no active types; never {@code null}
     * @see DictTypeWithDataVO
     * @see DictDataSimpleVO
     * @see <a href="https://mybatis.org/mybatis-3/sqlmap-xml.html#Result_Maps">MyBatis Collection Mapping Guide</a>
     */
    List<DictTypeWithDataVO> selectDictTypesWithData();
}