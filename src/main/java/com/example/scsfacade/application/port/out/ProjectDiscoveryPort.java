package com.example.scsfacade.application.port.out;

import java.util.List;

/**
 * 納管專案探索出站埠 (ProjectDiscoveryPort)。
 * <p>
 * 遵循 ... + Port 命名規範。定義由基礎設施層探索需定期進行低頻對帳之專案清單合約。
 * </p>
 */
public interface ProjectDiscoveryPort {

    /**
     * 探索目前已設定納管之專案代碼清單。
     *
     * @return 專案代碼字串清單
     */
    List<String> discoverConfiguredProjectIds();
}
