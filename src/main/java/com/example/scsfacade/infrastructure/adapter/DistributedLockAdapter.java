package com.example.scsfacade.infrastructure.adapter;

import com.example.scsfacade.application.port.out.DistributedLockPort;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redisson 分散式鎖外向適配器 (DistributedLockAdapter)。
 * <p>
 * 遵循 ... + Adapter 命名規範。實作應用層 DistributedLockPort 外向埠。
 * 採用 Redisson RLock 分散式鎖，具備以下生產級工業標準優勢：
 * 1. 看門狗自動續約 (Watchdog)：預設啟用後台守護線程自動定期續約，防止長任務執行中鎖被提前釋放。
 * 2. 安全原子解鎖 (Lock Ownership)：底層 Lua 腳本精準比對 Client UUID + Thread ID，絕不誤刪他人持有的鎖。
 * 3. 可重入性 (Reentrancy)：支援同線程嵌套重複加鎖。
 * </p>
 */
@Component
public class DistributedLockAdapter implements DistributedLockPort {

    private static final Logger log = LoggerFactory.getLogger(DistributedLockAdapter.class);

    private final RedissonClient redissonClient;

    public DistributedLockAdapter(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public boolean tryLock(String lockKey, long timeoutSeconds) {
        if (lockKey == null || lockKey.trim().isEmpty()) {
            return false;
        }
        RLock lock = redissonClient.getLock(lockKey.trim());
        try {
            // waitTime: 0 (立即嘗試獲取，不阻塞等待排隊), leaseTime: -1 (啟用 Redisson 看門狗自動續約機制)
            // 若調用端指定了正數 timeoutSeconds，則以 timeoutSeconds 作為 leaseTime
            long leaseTime = timeoutSeconds > 0 ? timeoutSeconds : -1;
            boolean acquired = lock.tryLock(0, leaseTime, TimeUnit.SECONDS);
            if (acquired) {
                log.debug("成功獲取 Redisson 分散式鎖 [key: {}, leaseTime: {}s]", lockKey, leaseTime);
            } else {
                log.warn("無法獲取 Redisson 分散式鎖，該資源已被並發佔用 [key: {}]", lockKey);
            }
            return acquired;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("獲取 Redisson 分散式鎖時線程中斷 [key: {}]: {}", lockKey, e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("存取 Redisson 分散式鎖時發生例外 [key: {}]: {}", lockKey, e.getMessage());
            return false;
        }
    }

    @Override
    public void releaseLock(String lockKey) {
        if (lockKey == null || lockKey.trim().isEmpty()) {
            return;
        }
        try {
            RLock lock = redissonClient.getLock(lockKey.trim());
            // 硬核防禦：僅在當前線程仍持有該鎖時釋放，徹底杜絕誤刪他人鎖之問題
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("成功安全釋放 Redisson 分散式鎖 [key: {}]", lockKey);
            } else {
                log.debug("當前線程未持有 Redisson 分散式鎖或已由逾時看門狗處理，跳過釋放 [key: {}]", lockKey);
            }
        } catch (Exception e) {
            log.error("釋放 Redisson 分散式鎖時發生例外 [key: {}]: {}", lockKey, e.getMessage());
        }
    }
}
