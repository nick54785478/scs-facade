package com.example.scsfacade.domain.finding.exception;

import com.example.scsfacade.domain.finding.aggregate.vo.FindingId;

/**
 * 找不到指定弱點之領域例外 (FindingNotFoundException)。
 * <p>
 * 當依據識別碼搜尋弱點但該弱點不存在於領域中時拋出。
 * </p>
 */
public class FindingNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final FindingId findingId;

    public FindingNotFoundException(FindingId findingId) {
        super(String.format("找不到指定之弱點: %s", findingId != null ? findingId.getValue() : "null"));
        this.findingId = findingId;
    }

    public FindingId getFindingId() {
        return findingId;
    }
}
