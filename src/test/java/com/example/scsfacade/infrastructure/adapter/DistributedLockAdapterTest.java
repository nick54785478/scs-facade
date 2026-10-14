package com.example.scsfacade.infrastructure.adapter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Redisson 分散式鎖外向適配器測試 (DistributedLockAdapterTest)。
 * <p>
 * 驗證 RLock 分散式鎖獲取、過期時間設定、線程所有權檢查與安全解鎖行為。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class DistributedLockAdapterTest {

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock rLock;

    private DistributedLockAdapter distributedLockAdapter;

    @BeforeEach
    void setUp() {
        distributedLockAdapter = new DistributedLockAdapter(redissonClient);
    }

    /**
     * Feature: 獲取 Redisson 分散式互斥鎖
     * Scenario: 當鎖未被他人佔用時成功獲取鎖
     * Given: Redisson RLock.tryLock 回傳 true
     * When: 調用 tryLock(lockKey, 30)
     * Then: 回傳 true 代表成功鎖定
     */
    @Test
    @DisplayName("獲取 Redisson 分散式鎖 - 未被佔用時應成功獲取並回傳 true")
    void shouldSuccessfullyAcquireLockWhenNotOccupied() throws Exception {
        // Given
        String lockKey = "lock:scan:task:123";
        when(redissonClient.getLock(lockKey)).thenReturn(rLock);
        when(rLock.tryLock(0L, 30L, TimeUnit.SECONDS)).thenReturn(true);

        // When
        boolean acquired = distributedLockAdapter.tryLock(lockKey, 30);

        // Then
        assertThat(acquired).isTrue();
        verify(rLock).tryLock(0L, 30L, TimeUnit.SECONDS);
    }

    /**
     * Feature: 獲取 Redisson 分散式互斥鎖
     * Scenario: 當鎖已被他人佔用時無法獲取鎖
     * Given: Redisson RLock.tryLock 回傳 false
     * When: 調用 tryLock(lockKey, 30)
     * Then: 回傳 false 代表被互斥阻擋
     */
    @Test
    @DisplayName("獲取 Redisson 分散式鎖 - 已被佔用時應回傳 false")
    void shouldFailToAcquireLockWhenAlreadyOccupied() throws Exception {
        // Given
        String lockKey = "lock:scan:task:123";
        when(redissonClient.getLock(lockKey)).thenReturn(rLock);
        when(rLock.tryLock(0L, 30L, TimeUnit.SECONDS)).thenReturn(false);

        // When
        boolean acquired = distributedLockAdapter.tryLock(lockKey, 30);

        // Then
        assertThat(acquired).isFalse();
    }

    /**
     * Feature: 釋放 Redisson 分散式互斥鎖 (所有權防誤刪)
     * Scenario: 當前線程持有鎖時成功釋放
     * Given: rLock.isHeldByCurrentThread() 為 true
     * When: 調用 releaseLock(lockKey)
     * Then: 呼叫 rLock.unlock() 安全釋放
     */
    @Test
    @DisplayName("釋放 Redisson 分散式鎖 - 當前線程持有時應呼叫 unlock 釋放")
    void shouldReleaseLockWhenHeldByCurrentThread() {
        // Given
        String lockKey = "lock:scan:task:123";
        when(redissonClient.getLock(lockKey)).thenReturn(rLock);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);

        // When
        distributedLockAdapter.releaseLock(lockKey);

        // Then
        verify(rLock).unlock();
    }

    /**
     * Feature: 釋放 Redisson 分散式互斥鎖 (防誤刪他人鎖)
     * Scenario: 當前線程未持有鎖時不觸發釋放
     * Given: rLock.isHeldByCurrentThread() 為 false
     * When: 調用 releaseLock(lockKey)
     * Then: 不呼叫 rLock.unlock()，杜絕誤刪他人的鎖
     */
    @Test
    @DisplayName("釋放 Redisson 分散式鎖 - 當前線程未持有時不應呼叫 unlock")
    void shouldNotReleaseLockWhenNotHeldByCurrentThread() {
        // Given
        String lockKey = "lock:scan:task:123";
        when(redissonClient.getLock(lockKey)).thenReturn(rLock);
        when(rLock.isHeldByCurrentThread()).thenReturn(false);

        // When
        distributedLockAdapter.releaseLock(lockKey);

        // Then
        verify(rLock, never()).unlock();
    }
}
