package cn.nuaa.jensonxu.fairy.integration.agent.handler;

import cn.nuaa.jensonxu.fairy.integration.agent.AgentProperties;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.redisson.api.RPermitExpirableSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Agent 并发控制组件
 * 基于 Semaphore 限制同时执行的 Agent 请求数量
 * 超过上限的请求将等待指定时间，等待超时后抛出异常，由 AgentHandler 映射为 429 错误事件
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentConcurrencyLimiter {

    private static final String SEMAPHORE_KEY = "agent:concurrency:semaphore";

    private final AgentProperties agentProperties;
    private final RedissonClient redissonClient;
    private final ThreadLocal<String> permitIdHolder = new ThreadLocal<>();

    // private Semaphore semaphore;
    private RPermitExpirableSemaphore semaphore;

    @PostConstruct
    public void init() {
        int maxConcurrent = agentProperties.getConcurrency().getMaxConcurrent();  // 最大并发数
        semaphore = redissonClient.getPermitExpirableSemaphore(SEMAPHORE_KEY);
        semaphore.trySetPermits(maxConcurrent);
        //this.semaphore = new Semaphore(maxConcurrent, true);  // 第二个参数 true：公平模式，先到先得，避免某些请求长期等不到许可
        log.info("[agent] 并发限流初始化完成, maxConcurrent={}, acquireTimeoutMs={}", maxConcurrent, agentProperties.getConcurrency().getAcquireTimeoutMs());
    }

    /**
     * 尝试获取一个执行许可
     * 在 acquireTimeoutMs 内未获取到许可则抛出异常
     *
     * @throws AgentConcurrencyException 等待超时或线程被中断时抛出
     */
    public void acquire() throws AgentConcurrencyException {
        // long timeoutMs = agentProperties.getConcurrency().getAcquireTimeoutMs();
        AgentProperties.Concurrency config = agentProperties.getConcurrency();
        try {
            String permitId = semaphore.tryAcquire(config.getAcquireTimeoutMs(), config.getPermitTtlMs(), TimeUnit.MILLISECONDS);
            if(permitId == null) {
                log.warn("[agent] 获取许可超时, key={}", SEMAPHORE_KEY);
                throw new AgentConcurrencyException("Agent 服务繁忙, 请求排队超时(等待超过 " + config.getAcquireTimeoutMs() + "ms), 请稍后重试");
            }
            permitIdHolder.set(permitId);
            log.debug("[agent] 许可获取成功, permitId={}", permitId);
            /*
            boolean acquired = semaphore.tryAcquire(timeoutMs, TimeUnit.MILLISECONDS);
            if (!acquired) {
                log.warn("[agent] 获取许可超时, 当前可用许可数: {}", semaphore.availablePermits());
                throw new AgentConcurrencyException("Agent 服务繁忙, 请求排队超时(等待超过 " + timeoutMs + "ms), 请稍后重试");
            }
            log.debug("[agent] 许可获取成功, 剩余可用许可数: {}", semaphore.availablePermits());
            */
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AgentConcurrencyException("获取 Agent 执行许可时线程被中断");
        }
    }

    /**
     * 释放一个执行许可，必须在 finally 块中调用，确保不泄漏许可
     */
    public void release() {
        // semaphore.release();
        // log.debug("[agent] 许可已释放, 当前可用许可数: {}", semaphore.availablePermits());

        String permitId = permitIdHolder.get();
        if (permitId == null) {
            log.warn("[agent] release() 调用时 permitId 为空，跳过释放");
            return;
        }
        try {
            semaphore.release(permitId);
            log.debug("[agent] 许可已释放, permitId={}", permitId);
        } catch (Exception e) {
            // 许可可能已因 TTL 到期被自动回收，记录日志即可，不向上抛出
            log.warn("[agent] 释放许可时发生异常, permitId={}, error={}", permitId, e.getMessage());
        } finally {
            permitIdHolder.remove();
        }
    }

    /**
     * 返回当前可用许可数（用于监控/日志）
     */
    public int getAvailablePermits() {
        // return semaphore.getQueueLength();
        return semaphore.availablePermits();
    }
}
