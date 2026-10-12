package com.example.scsfacade.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI (Swagger UI) 介面文件全域配置類別。
 * <p>
 * 遵循配置集中存放於 config/ 之規範。
 * </p>
 */
@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Source Code Scan Facade API")
                        .version("1.0.0")
                        .description("源碼掃描多來源整合門面服務 (SCS Facade) - 支援 SonarQube、多源標準化、指紋去重與 CI/CD 門禁審查")
                        .contact(new Contact().name("DevSecOps Team")));
    }
}
