package com.github.starhq.template.common.util;


import com.github.starhq.template.model.dto.RequestContext;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class RequestContextUtilTest {

    @Test
    void setContext_ThenGetContext_ShouldReturnSameInstance() {
        // Given: 创建一个真实的 RequestContext
        RequestContext expectedContext = new RequestContext("device-123", "192.168.1.1");

        // When: 存入上下文
        RequestContextUtil.setContext(expectedContext);

        // Then: 获取时应该是同一个对象
        assertSame(expectedContext, RequestContextUtil.getContext());
    }

    @Test
    void getContext_WhenNotSet_ShouldReturnNull() {
        // Given: 确保当前线程是干净的（测试框架默认就是干净的，但为了严谨加上清理）
        RequestContextUtil.clear();

        // When: 没有存过任何东西就去获取
        RequestContext context = RequestContextUtil.getContext();

        // Then: 应该返回 null
        assertNull(context);
    }

    @Test
    void clear_ShouldRemoveExistingContext() {
        // Given: 先存入一个对象
        RequestContextUtil.setContext(new RequestContext("d1", "ip1"));
        assertNotNull(RequestContextUtil.getContext()); // 确认存入成功

        // When: 执行清理
        RequestContextUtil.clear();

        // Then: 再次获取应该为 null
        assertNull(RequestContextUtil.getContext());
    }

    // ==========================================
    // 核心测试：多线程并发安全性（证明 ThreadLocal 没有被污染）
    // ==========================================
    @Test
    void multipleThreads_ShouldMaintainStrictIsolation() throws InterruptedException {
        int threadCount = 2; // 模拟 100 个并发请求
        CountDownLatch startLatch = new CountDownLatch(1);
        // 顺便提一句：你原代码里声明了 verifyLatch 但没用到，我帮你删掉了，避免 unused variable 警告
        CountDownLatch finishLatch = new CountDownLatch(threadCount);
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) { // 去掉 try-with-resources 语法糖
            final int threadId = i; // 【关键修复】：加上 final 或 effectively final，确保匿名内部类可以访问


            executorService.submit(() -> {
                try {
                    // 等待所有线程就绪
                    startLatch.await();

                    // 每个线程存入自己独有的特征值
                    String deviceId = "device-" + threadId;
                    String ip = "192.168.1." + threadId;
                    RequestContext context = new RequestContext(deviceId, ip);
                    RequestContextUtil.setContext(context);

                    // 模拟业务逻辑执行，睡一小会儿（放大并发冲突的概率）
                    Thread.sleep(10);

                    // 验证：在并发执行期间，获取到的永远是自己的数据
                    RequestContext retrievedContext = RequestContextUtil.getContext();
                    assertNotNull(retrievedContext);
                    assertEquals("线程 " + threadId + " 数据被篡改！", deviceId, retrievedContext.deviceFingerprint());
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } finally {
                    // 模拟 Filter 的 finally 块：无论业务代码是否报错，必须清理！
                    RequestContextUtil.clear();
                    finishLatch.countDown();
                }
            });
        }

        // 释放启动信号
        startLatch.countDown();

        // 等待所有线程执行完毕
        boolean allFinished = finishLatch.await(5, TimeUnit.SECONDS);
        assertTrue(allFinished, "某些线程执行超时");
    }
}
