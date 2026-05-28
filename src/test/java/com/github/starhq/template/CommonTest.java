package com.github.starhq.template;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description:
 * @date 2026/5/9 12:49
 */
class CommonTest {

    @Test
    void test() {
        String key = "3R+Xiy55zNKk9jncK1nKlHYElLlylh1lRETufC8aYaMmo+ZDk9o0edXlWHoXneA/JX/D58VpuWoxUEVKZZfhxA==";
        byte[] keys = key.getBytes();
        Assertions.assertEquals(88, keys.length);
    }
}
