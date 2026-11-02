package com.backend.orderflow.utils.SwaggerConfig;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI orderFlowOpenAPI() {

        return new OpenAPI()
                .info(new Info()
                        .title("Order Flow API")
                        .description("API para gerenciamento de pedidos e produtos")
                        .version("1.0.0"));
    }
}
