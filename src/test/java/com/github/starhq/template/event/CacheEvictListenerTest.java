package com.github.starhq.template.event;

import com.github.starhq.template.event.listener.CacheEvictListener;
import com.github.starhq.template.helper.CacheHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CacheEvictListenerTest {

    @Mock
    private CacheHelper cacheHelper;

    @InjectMocks
    private CacheEvictListener cacheEvictListener;

    @Test
    void handleEvictEvent_Success() {
        // Given: 准备正常的 keys 和 cacheNames
        Set<Long> keys = Set.of(1L, 2L);
        List<String> cacheNames = List.of("user", "token");
        CacheEvictEvent<Long> event = new CacheEvictEvent<>(keys, cacheNames);

        // When
        cacheEvictListener.handleEvictEvent(event);

        // Then: 验证底层的 cacheHelper 被正确调用，且参数匹配
        verify(cacheHelper, times(1)).evict(keys, cacheNames);
    }

    @Test
    void handleEvictEvent_Fail_EventIsNull() {
        // Given: 传入 null 事件
        // When
        cacheEvictListener.handleEvictEvent(null);

        // Then: 验证第一层防御生效，后续逻辑未执行
        verify(cacheHelper, never()).evict(any(), any());
    }

    @Test
    void handleEvictEvent_Fail_CacheNamesIsEmpty() {
        // Given: cacheNames 为空集合（模拟 CollectionUtils.isEmpty 判断为 true）
        Set<Long> keys = Set.of(1L);
        List<String> emptyCacheNames = Collections.emptyList(); // 或者直接传 null
        CacheEvictEvent<Long> event = new CacheEvictEvent<>(keys, emptyCacheNames);

        // When
        cacheEvictListener.handleEvictEvent(event);

        // Then: 验证第二层防御生效
        verify(cacheHelper, never()).evict(any(), any());
    }

    @Test
    void handleEvictEvent_Fail_KeysIsEmpty() {
        // Given: keys 为空集合，但 cacheNames 正常
        List<Long> emptyKeys = Collections.emptyList();
        List<String> cacheNames = List.of("user");
        CacheEvictEvent<Long> event = new CacheEvictEvent<>(emptyKeys, cacheNames);

        // When
        cacheEvictListener.handleEvictEvent(event);

        // Then: 验证第三层防御生效
        verify(cacheHelper, never()).evict(any(), any());
    }

    @Test
    void handleEvictEvent_Fail_Exception() {
        // Given: keys 为空集合，但 cacheNames 正常
        List<Long> emptyKeys = List.of(1L);
        List<String> cacheNames = List.of("user");
        CacheEvictEvent<Long> event = new CacheEvictEvent<>(emptyKeys, cacheNames);

        doThrow(new RuntimeException("Evict cache failure")).when(cacheHelper).evict(anyList(), anyList());

        // When
        assertDoesNotThrow(() -> cacheEvictListener.handleEvictEvent(event));

        // Then: 验证第三层防御生效
        verify(cacheHelper).evict(any(), any());
    }
}
