package cn.nuaa.jensonxu.fairy.web;

import cn.nuaa.jensonxu.fairy.integration.agent.handler.AgentConcurrencyException;
import cn.nuaa.jensonxu.fairy.integration.agent.handler.AgentConcurrencyLimiter;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
class AgentConcurrencyLimiterTest {

    @Autowired
    private AgentConcurrencyLimiter limiter;

    /**
     * 每个用例执行后恢复许可，避免用例间相互干扰
     */
    @AfterEach
    void cleanUp() {
        log.info("[test] 测试结束，当前可用许可数: {}", limiter.getAvailablePermits());
    }

    /**
     * 用例1：基本 acquire / release 流程
     * 验证：acquire 成功后可用许可数减少，release 后恢复
     */
    @Test
    void testBasicAcquireAndRelease() throws AgentConcurrencyException {
        int before = limiter.getAvailablePermits();
        log.info("[test] acquire 前可用许可数: {}", before);

        limiter.acquire();
        int afterAcquire = limiter.getAvailablePermits();
        log.info("[test] acquire 后可用许可数: {}", afterAcquire);
        assertEquals(before - 1, afterAcquire, "acquire 后可用许可数应减少 1");

        limiter.release();
        int afterRelease = limiter.getAvailablePermits();
        log.info("[test] release 后可用许可数: {}", afterRelease);
        assertEquals(before, afterRelease, "release 后可用许可数应恢复");
    }

    /**
     * 用例2：并发上限控制
     * 场景：启动 maxConcurrent + 5 个线程同时抢许可，持有 2 秒后释放
     * 验证：成功获取的线程数 <= maxConcurrent，超出的线程超时后抛异常（429 场景）
     */
    @Test
    void testConcurrencyLimit() throws InterruptedException {
        int maxConcurrent = limiter.getAvailablePermits();  // 20
        int extraThreads = 5;
        int totalThreads = maxConcurrent + extraThreads;    // 25

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rejectCount = new AtomicInteger(0);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch acquireDoneLatch = new CountDownLatch(totalThreads);
        CountDownLatch holdLatch = new CountDownLatch(1);  // 主线程控制释放时机

        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);

        for (int i = 0; i < totalThreads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    limiter.acquire();
                    successCount.incrementAndGet();
                    holdLatch.await();  // 持有许可，等主线程放行后才释放
                    limiter.release();
                } catch (AgentConcurrencyException e) {
                    rejectCount.incrementAndGet();
                    log.info("[test] 请求被拒绝（429）: {}", e.getMessage());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    acquireDoneLatch.countDown();
                }
            });
        }

        startLatch.countDown();

        // 等待所有线程完成 acquire 阶段（成功或超时拒绝）
        // 超时时间 = acquireTimeoutMs(5s) + 缓冲(2s)
        acquireDoneLatch.await(7, TimeUnit.SECONDS);
        holdLatch.countDown();  // 通知持有许可的线程释放
        executor.shutdown();

        log.info("[test] 成功数: {}, 拒绝数: {}, 合计: {}", successCount.get(), rejectCount.get(), totalThreads);

        assertEquals(maxConcurrent, successCount.get(), "成功数应恰好等于 maxConcurrent");
        assertEquals(extraThreads, rejectCount.get(), "超出上限的请求应全部被拒绝");
    }

    /**
     * 用例3：release 幂等性保护
     * 场景：未 acquire 直接调用 release
     * 验证：不抛异常，不影响许可计数
     */
    @Test
    void testReleaseWithoutAcquire() {
        int before = limiter.getAvailablePermits();
        assertDoesNotThrow(() -> limiter.release(), "未 acquire 时调用 release 不应抛出异常");
        assertEquals(before, limiter.getAvailablePermits(), "空 release 不应改变许可数");
    }
}