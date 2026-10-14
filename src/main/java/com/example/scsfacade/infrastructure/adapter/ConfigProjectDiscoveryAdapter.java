package com.example.scsfacade.infrastructure.adapter;

import com.example.scsfacade.application.port.out.ProjectDiscoveryPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 組態納管專案探索外向適配器 (ConfigProjectDiscoveryAdapter)。
 * <p>
 * 遵循 ... + Adapter 命名規範。實作應用層 ProjectDiscoveryPort 外向埠。
 * 負責自應用程式環境組態 (application.yml 之 scs.reconciliation.managed-projects) 讀取並解析納管專案清單。
 * </p>
 */
@Component
public class ConfigProjectDiscoveryAdapter implements ProjectDiscoveryPort {

    private final List<String> configuredProjects;

    public ConfigProjectDiscoveryAdapter(
            @Value("${scs.reconciliation.managed-projects:scs-facade}") String managedProjects) {
        if (managedProjects == null || managedProjects.trim().isEmpty()) {
            this.configuredProjects = Collections.emptyList();
        } else {
            this.configuredProjects = Arrays.stream(managedProjects.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .distinct()
                    .toList();
        }
    }

    @Override
    public List<String> discoverConfiguredProjectIds() {
        return configuredProjects;
    }
}
