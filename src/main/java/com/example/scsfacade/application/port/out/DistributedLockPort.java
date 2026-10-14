package com.example.scsfacade.application.port.out;

/**
 * 分散式鎖外向埠契約 (DistributedLockPort)。
 * <p>
 * 遵循 ... + Port 命名規範。定義應用層對分散式並發鎖定與冪等互斥存取之合約。
 * 遵守 Clean Architecture 規範：介面方法傳參與回傳型別嚴禁使用任何技術類或環境相依物件。
 * </p>
 */
public interface DistributedLockPort {

    /**
     * 嘗試獲取指定識別鍵之分散式互斥鎖。
     *
     * @param lockKey 鎖識別碼鍵值 (例如 "lock:scan:project-a:main" 或 "lock:scan:task:task-123")
     * @param timeoutSeconds 鎖持有效時（秒），超時自動釋放以防止分散式死鎖
     * @return true 代表獲取鎖成功，false 代表已被其他並發程序持有
     */
    boolean tryLock(String lockKey, long timeoutSeconds);

    /**
     * 釋放指定識別鍵之分散式鎖。
     *
     * @param lockKey 鎖識別碼鍵值
     */
    void releaseLock(String lockKey);
}
