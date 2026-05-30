package com.github.starhq.template.helper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CacheHelper 单元测试")
class CacheHelperTest {

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    private CacheHelper cacheHelper;

    @BeforeEach
    void setUp() {
        cacheHelper = new CacheHelper(cacheManager);
    }

    @Nested
    @DisplayName("1. 基础操作测试")
    class BasicOperationTests {

        @Test
        @DisplayName("Cache 存在 - 应成功写入值")
        void put_whenCacheExists_shouldPutValue() {
            when(cacheManager.getCache("user")).thenReturn(cache);
            cacheHelper.put(1L, "admin", "user");
            verify(cache).put(1L, "admin");
        }

        @Test
        @DisplayName("Cache 不存在 - 应安全跳过不抛异常")
        void put_whenCacheIsNull_shouldDoNothing() {
            when(cacheManager.getCache("user")).thenReturn(null);
            cacheHelper.put(1L, "admin", "user");
            verify(cache, never()).put(any(), any());
        }

        @Test
        @DisplayName("Cache 存在且命中 - 应返回对应的值")
        void get_whenCacheExists_shouldReturnValue() {
            when(cacheManager.getCache("user")).thenReturn(cache);
            when(cache.get(1L, String.class)).thenReturn("admin");

            String result = cacheHelper.get(1L, String.class, "user");
            assertThat(result).isEqualTo("admin");
        }

        @Test
        @DisplayName("Cache 不存在 - 应返回 null")
        void get_whenCacheIsNull_shouldReturnNull() {
            when(cacheManager.getCache("user")).thenReturn(null);
            String result = cacheHelper.get(1L, String.class, "user");
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("2. evict 删除操作测试")
    class EvictTests {

        @Test
        @DisplayName("cacheNames 为空 - 应直接返回")
        void evict_whenCacheNamesEmpty_shouldDoNothing() {
            cacheHelper.evict(List.of(1L), Collections.emptyList());
            verify(cacheManager, never()).getCache(anyString());
        }

        @Test
        @DisplayName("ids 为空 - 应调用每个 cache 的 clear 方法")
        void evict_whenIdsEmpty_shouldClearCaches() {
            when(cacheManager.getCache("c1")).thenReturn(cache);
            when(cacheManager.getCache("c2")).thenReturn(cache);

            cacheHelper.evict(Collections.emptyList(), List.of("c1", "c2"));

            verify(cache, times(2)).clear();
        }

        @Test
        @DisplayName("ids 不为空 - 应遍历 ids 调用每个 cache 的 evict 方法")
        void evict_whenIdsNotEmpty_shouldEvictSpecificKeys() {
            when(cacheManager.getCache("c1")).thenReturn(cache);
            Set<Long> ids = Set.of(1L, 2L, 3L);

            cacheHelper.evict(ids, List.of("c1"));

            verify(cache, times(3)).evict(anyLong());
            verify(cache).evict(1L);
            verify(cache, never()).clear();
        }

        @Test
        @DisplayName("某个 Cache 实例为 null - 应安全跳过该 Cache")
        void evict_whenCacheInstanceIsNull_shouldSkip() {
            when(cacheManager.getCache("c1")).thenReturn(null);
            when(cacheManager.getCache("c2")).thenReturn(cache);

            cacheHelper.evict(List.of(1L), List.of("c1", "c2"));

            verify(cache, times(1)).evict(1L);
        }
    }

    @Nested
    @DisplayName("3. clear 清空操作测试")
    class ClearTests {

        @Test
        @DisplayName("Cache 存在 - 应执行 clear")
        void clear_whenCacheExists_shouldCallClear() {
            when(cacheManager.getCache("user")).thenReturn(cache);
            cacheHelper.clear("user");
            verify(cache).clear();
        }

        @Test
        @DisplayName("Cache 不存在 - 应安全跳过")
        void clear_whenCacheIsNull_shouldDoNothing() {
            when(cacheManager.getCache("user")).thenReturn(null);
            cacheHelper.clear("user");
            verify(cache, never()).clear();
        }
    }

    @Nested
    @DisplayName("4. 核心逻辑 getBatchWithCache 批量获取测试")
    class GetBatchWithCacheTests {

        @SuppressWarnings("unchecked")
        private Function<Set<Long>, Map<Long, String>> mockDbLoader(Map<Long, String> dbData) {
            Function<Set<Long>, Map<Long, String>> loader = mock(Function.class);
            if (!dbData.isEmpty()) {
                when(loader.apply(anySet())).thenReturn(dbData);
            }
            return loader;
        }

        @Test
        @DisplayName("传入的 ids 为空 - 应直接返回空 Map")
        void getBatch_idsEmpty_shouldReturnEmptyMap() {
            Function<Set<Long>, Map<Long, String>> loader = mockDbLoader(Map.of());
            Map<Long, String> result = cacheHelper.getBatchWithCache(Collections.emptySet(), "user", loader);
            assertThat(result).isEmpty();
            verify(cacheManager, never()).getCache(anyString());
        }

        @Test
        @DisplayName("完全命中缓存 - 不应调用 DB Loader")
        void getBatch_allCacheHit_shouldNotCallDb() {
            when(cacheManager.getCache("user")).thenReturn(cache);
            when(cache.get(1L, String.class)).thenReturn("admin");
            when(cache.get(2L, String.class)).thenReturn("jack");

            Function<Set<Long>, Map<Long, String>> loader = mockDbLoader(Map.of());
            Map<Long, String> result = cacheHelper.getBatchWithCache(Set.of(1L, 2L), "user", loader);

            assertThat(result).hasSize(2).containsEntry(1L, "admin").containsEntry(2L, "jack");
            verify(loader, never()).apply(anySet());
            verify(cache, never()).put(any(), any());
        }

        @Test
        @DisplayName("完全未命中缓存 - 应调用 DB Loader 并回写缓存")
        void getBatch_allCacheMiss_shouldCallDbAndWriteBack() {
            when(cacheManager.getCache("user")).thenReturn(cache);
            when(cache.get(anyLong(), eq(String.class))).thenReturn(null);

            Map<Long, String> mockDbData = Map.of(1L, "admin", 2L, "jack");
            Function<Set<Long>, Map<Long, String>> loader = mockDbLoader(mockDbData);

            Map<Long, String> result = cacheHelper.getBatchWithCache(Set.of(1L, 2L), "user", loader);

            assertThat(result).hasSize(2).isEqualTo(mockDbData);
            verify(loader).apply(Set.of(1L, 2L));
            verify(cache).put(1L, "admin");
            verify(cache).put(2L, "jack");
        }

        @Test
        @DisplayName("部分命中缓存 - 只应将未命中的 ID 传给 DB 并只回写 DB 查出的数据")
        void getBatch_partialCacheMiss_shouldCallDbWithMissIdsOnly() {
            when(cacheManager.getCache("user")).thenReturn(cache);
            when(cache.get(1L, String.class)).thenReturn("admin");
            when(cache.get(2L, String.class)).thenReturn(null);

            Map<Long, String> mockDbData = Map.of(2L, "jack");
            Function<Set<Long>, Map<Long, String>> loader = mockDbLoader(mockDbData);

            Map<Long, String> result = cacheHelper.getBatchWithCache(Set.of(1L, 2L), "user", loader);

            assertThat(result).hasSize(2);
            verify(loader).apply(Set.of(2L));
            verify(cache).put(2L, "jack");
            verify(cache, never()).put(eq(1L), anyString());
        }

        @Test
        @DisplayName("Cache 实例为 null - 应跳过缓存读写直接全量查 DB")
        void getBatch_cacheIsNull_shouldFallbackToDb() {
            when(cacheManager.getCache("user")).thenReturn(null);

            Map<Long, String> mockDbData = Map.of(1L, "admin");
            Function<Set<Long>, Map<Long, String>> loader = mockDbLoader(mockDbData);

            Map<Long, String> result = cacheHelper.getBatchWithCache(Set.of(1L), "user", loader);

            assertThat(result).isEqualTo(mockDbData);
            verify(loader).apply(Set.of(1L));
            verify(cache, never()).get(any());
            verify(cache, never()).put(any(), any());
        }

        @Test
        @DisplayName("DB Loader 抛出异常 - 应捕获异常并返回 unknown，且不回写缓存")
        @SuppressWarnings("unchecked")
        void getBatch_dbLoaderThrowsException_shouldReturnUnknown() {
            // given: 模拟缓存未命中
            when(cacheManager.getCache("user")).thenReturn(cache);
            when(cache.get(anyLong(), eq(String.class))).thenReturn(null);

            // 【关键点】：创建一个 Mock 的 loader，并指定它在被调用时抛出 RuntimeException
            Function<Set<Long>, Map<Long, String>> failLoader = mock(Function.class);
            when(failLoader.apply(anySet())).thenThrow(new RuntimeException("Database connection failed"));

            // when: 调用方法
            Map<Long, String> result = cacheHelper.getBatchWithCache(Set.of(1L, 2L), "user", failLoader);

            // then: 验证结果
            // 1. 返回的数据大小应该是 2
            // 2. 因为 DB 挂了，原本缓存未命中的 1 和 2，都应该被赋值为 "unknown"
            assertThat(result).hasSize(2)
                    .containsEntry(1L, "unknown")
                    .containsEntry(2L, "unknown");

            // 3. 验证 DB Loader 确实被触发过
            verify(failLoader).apply(Set.of(1L, 2L));

            // 4. 【核心业务验证】：因为拿到的 dbData 是异常降级数据，所以绝对不能回写到缓存里，否则会污染缓存！
            verify(cache, times(2)).put(any(), any());
        }
    }

    @Nested
    @DisplayName("5. 严格获取 Cache 实例测试")
    class GetCacheStrictTests {

        @Test
        @DisplayName("Cache 存在 - 应正常返回实例")
        void getCache_whenExists_shouldReturnCache() {
            when(cacheManager.getCache("user")).thenReturn(cache);
            Cache result = cacheHelper.getCache("user");
            assertThat(result).isEqualTo(cache);
        }

        @Test
        @DisplayName("Cache 不存在 - 应抛出 BusinessException 异常")
        void getCache_whenNull_shouldThrowBusinessException() {
            when(cacheManager.getCache("invalid_cache")).thenReturn(null);
            assertThatThrownBy(() -> cacheHelper.getCache("invalid_cache"))
                    .isInstanceOf(RuntimeException.class);
        }
    }
}