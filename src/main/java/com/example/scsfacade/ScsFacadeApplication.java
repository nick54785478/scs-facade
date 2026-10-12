package com.example.scsfacade;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SCS Facade (源碼掃描門面服務) 啟動進入點。
 * <p>
 * 負責引導 Spring Boot 容器與自動配置。
 * 遵循 DDD 與六角形架構，業務核心與領域邏輯保持純粹與低耦合。
 * </p>
 */
@SpringBootApplication
public class ScsFacadeApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScsFacadeApplication.class, args);
    }
}
