package com.github.starhq.template.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.starhq.template.entity.SysDictData;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus mapper interface for {@link SysDictData} entity with custom pagination support.
 * <strong>Primary Use Cases:</strong>
 * <ul>
 *     <li><strong>Dictionary Management</strong>: Paginated listing of dictionary entries with filtering by type, label, value</li>
 *     <li><strong>Admin Console</strong>: Display dictionary data with extended fields (creator, timestamps, status) for management</li>
 *     <li><strong>Dynamic Configuration</strong>: Support real-time dictionary updates without code deployments</li>
 * </ul>
 * <p>
 * <strong>Custom Query Design:</strong>
 * <p>
 * The {@code selectDictDataPage} method leverages MyBatis-Plus's {@code Constants.WRAPPER}
 * parameter injection to support dynamic, type-safe query conditions while maintaining
 * separation between entity persistence ({@code SysDictData}) and presentation ({@code DictDataVO}).
 * <p>
 * <strong>Usage Example:</strong>
 * <pre>
 * {@code
 * @Service
 * public class DictDataService {
 *     @Autowired private SysDictDataMapper dictDataMapper;
 *
 *     public IPage<DictDataVO> queryDictData(PageRequest request) {
 *         Page<DictDataVO> page = new Page<>(request.getPage(), request.getSize());
 *
 *         QueryWrapper<DictDataVO> wrapper = new QueryWrapper<DictDataVO>()
 *             .eq("type_id", request.getTypeId())           // Filter by dictionary type
 *             .like(StringUtils.hasText(request.getLabel()), "label", request.getLabel())  // Fuzzy match label
 *             .eq("status", 1)                               // Only active entries
 *             .orderByDesc("sort_order", "created_at");     // Sort by order then time
 *
 *         return dictDataMapper.selectDictDataPage(page, wrapper);
 *     }
 * }
 * }
 * </pre>
 *
 * @author starhq
 * @version 1.0
 * @date 2026-05-20
 * @see BaseMapper
 * @see SysDictData
 * @see com.baomidou.mybatisplus.core.toolkit.Constants#WRAPPER
 */
@Mapper
public interface SysDictDataMapper extends BaseMapper<SysDictData> {

    // ========== Inherited Methods from BaseMapper<SysDictData> ==========
    //
    // The following standard CRUD methods are automatically provided by MyBatis-Plus:
    //
    // // Insert
    // int insert(SysDictData entity);
    //
    // // Select
    // SysDictData selectById(Serializable id);
    // List<SysDictData> selectBatchIds(Collection<? extends Serializable> idList);
    // List<SysDictData> selectByMap(Map<String, Object> columnMap);
    // SysDictData selectOne(LambdaQueryWrapper<SysDictData> queryWrapper);
    // List<SysDictData> selectList(LambdaQueryWrapper<SysDictData> queryWrapper);
    // <E extends IPage<SysDictData>> E selectPage(E page, LambdaQueryWrapper<SysDictData> queryWrapper);
    //
    // // Update
    // int updateById(SysDictData entity);
    // int update(SysDictData entity, LambdaUpdateWrapper<SysDictData> updateWrapper);
    //
    // // Delete
    // int deleteById(Serializable id);
    // int deleteByMap(Map<String, Object> columnMap);
    // int delete(LambdaQueryWrapper<SysDictData> queryWrapper);
    //
    // For detailed usage and advanced features, refer to:
    // @see <a href="https://baomidou.com/pages/49cc81/">MyBatis-Plus BaseMapper Guide</a>

}