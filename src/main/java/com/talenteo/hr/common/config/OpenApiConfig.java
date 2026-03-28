package com.talenteo.hr.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI hrApiOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Talenteo HR API")
                        .description("Employee Management and Payroll Processing")
                        .version("1.0.0"));
    }
}
