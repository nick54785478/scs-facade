package com.example.scsfacade.domain.scanexecution.exception;

import com.example.scsfacade.domain.scanexecution.aggregate.vo.ScanExecutionId;

/**
 * 掃描執行紀錄未找到領域例外 (ScanExecutionNotFoundException)。
 */
public class ScanExecutionNotFoundException extends RuntimeException {

    public ScanExecutionNotFoundException(ScanExecutionId id) {
        super(String.format("找不到指定的掃描執行記錄: %s", id != null ? id.getValue() : "null"));
    }

    public ScanExecutionNotFoundException(String message) {
        super(message);
    }
}
