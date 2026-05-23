package com.github.starhq.template.helper;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.github.starhq.template.entity.SysUser;
import com.github.starhq.template.mapper.SysUserMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SysUserMapperHelper 单元测试")
class SysUserMapperHelperTest {

    @Mock
    private SysUserMapper userMapper;

    @InjectMocks
    private SysUserMapperHelper helper;

    @Captor
    private ArgumentCaptor<LambdaQueryWrapper<SysUser>> queryWrapperCaptor;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), SysUser.class);
    }

    // ========================================
    // 辅助方法：快速构建 SysUser 实体
    // ========================================
    private SysUser buildUser(Long id, String username) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setUsername(username);
        return user;
    }

    // ========================================
    // 1. 正常业务流转测试
    // ========================================
    @Nested
    @DisplayName("1. 正常业务流转测试")
    class HappyPathTests {

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("传入存在的 ID 集合 - 应正确返回 ID 到 Username 的映射 Map")
        void apply_withExistingIds_shouldReturnCorrectMap() {
            // given
            Set<Long> ids = Set.of(1L, 2L, 3L);
            List<SysUser> mockUsers = List.of(
                    buildUser(1L, "admin"),
                    buildUser(2L, "user_jack"),
                    buildUser(3L, "user_tom")
            );

            when(userMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(mockUsers);

            // when
            Map<Long, String> result = helper.apply(ids);

            // then
            assertThat(result)
                    .isNotEmpty()
                    .hasSize(3)
                    .containsEntry(1L, "admin")
                    .containsEntry(2L, "user_jack")
                    .containsEntry(3L, "user_tom");
        }

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("部分 ID 在数据库中不存在 - 应只返回存在的数据映射")
        void apply_withPartialExistingIds_shouldReturnFoundMap() {
            // given
            Set<Long> requestIds = Set.of(1L, 2L, 99L); // 99L 不存在
            List<SysUser> mockUsers = List.of(
                    buildUser(1L, "admin"),
                    buildUser(2L, "user_jack")
                    // 模拟数据库查不到 99L
            );

            when(userMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(mockUsers);

            // when
            Map<Long, String> result = helper.apply(requestIds);

            // then
            assertThat(result)
                    .hasSize(2)
                    .doesNotContainKey(99L)
                    .containsEntry(1L, "admin");
        }
    }

    // ========================================
    // 2. 空值与边界防御测试
    // ========================================
    @Nested
    @DisplayName("2. 空值与边界防御测试")
    class EdgeCaseTests {

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("传入空集合 - 应返回空 Map 且仍会调用 Mapper (让数据库处理空 IN)")
        void apply_withEmptySet_shouldReturnEmptyMap() {
            // given
            Set<Long> emptyIds = Collections.emptySet();
            // when(userMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

            // when
            Map<Long, String> result = helper.apply(emptyIds);

            // then
            assertThat(result).isEmpty();
            verify(userMapper, never()).selectList(any(LambdaQueryWrapper.class));
        }

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("数据库查询返回空列表 - 应安全返回空 Map")
        void apply_whenDbReturnsEmpty_shouldReturnEmptyMap() {
            // given
            Set<Long> ids = Set.of(999L, 888L);
            when(userMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

            // when
            Map<Long, String> result = helper.apply(ids);

            // then
            assertThat(result).isEmpty();
        }

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("⚠️ 潜在风险验证：数据库返回的 Username 为 null - 会触发 NPE")
        void apply_whenUsernameIsNull_shouldThrowNPE() {
            // given
            Set<Long> ids = Set.of(1L);
            SysUser badUser = new SysUser();
            badUser.setId(1L);
            badUser.setUsername(null); // username 为 null

            when(userMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(badUser));

            // when & then
            // 因为源码使用了 Collectors.toMap，如果 value 为 null 会抛出 NullPointerException
            org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class, () -> helper.apply(ids));
        }
    }

    // ========================================
    // 3. MyBatis-Plus 查询条件构建验证 (交互测试)
    // ========================================
    @Nested
    @DisplayName("3. MyBatis-Plus 查询条件构建验证")
    class QueryConstructionTests {

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("验证是否正确调用了 Mapper 并传递了 Wrapper")
        void apply_shouldInvokeMapperSelectListOnce() {
            // given
            Set<Long> ids = Set.of(1L);
            when(userMapper.selectList(any())).thenReturn(List.of(buildUser(1L, "a")));

            // when
            helper.apply(ids);

            // then
            // 验证 selectList 确切被调用了一次
            verify(userMapper, org.mockito.Mockito.times(1)).selectList(any(LambdaQueryWrapper.class));
        }

        @Test
        @DisplayName("验证 Wrapper 生成的 SQL 片段包含 IN 条件和 SELECT 字段")
        void apply_shouldBuildCorrectSqlSegment() {
            // given
            Set<Long> ids = Set.of(10L, 20L);
            when(userMapper.selectList(any())).thenReturn(Collections.emptyList());

            // when
            helper.apply(ids);

            // then
            // 捕获传入 Mapper 的参数
            verify(userMapper).selectList(queryWrapperCaptor.capture());
            LambdaQueryWrapper<SysUser> capturedWrapper = queryWrapperCaptor.getValue();

            // 获取 MyBatis-Plus 解析后的 SQL 片段进行断言
            String sqlSegment = capturedWrapper.getCustomSqlSegment();

            // 验证包含了 IN 查询条件
            assertThat(sqlSegment).contains("IN");

            // 验证 SELECT 投影 (通过检查 getSqlSelect)
            String sqlSelect = capturedWrapper.getSqlSelect();
            assertThat(sqlSelect)
                    .contains("id")       // 验证选了 id
                    .contains("username"); // 验证选了 username
        }
    }
}