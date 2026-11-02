package com.backend.orderflow.utils.SwaggerConfig;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.method.HandlerMethod;

import java.util.Arrays;
import java.util.stream.Collectors;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI orderFlowOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Order Flow API")
                        .description("API REST para orquestração e gerenciamento de fluxo de pedidos e produtos.")
                        .version("1.0.0"));
    }

    @Bean
    public OperationCustomizer globalOperationCustomizer() {
        return (operation, handlerMethod) -> {
            generateAutoSummary(operation, handlerMethod);
            configureStandardResponses(operation, handlerMethod);
            return operation;
        };
    }

    private void generateAutoSummary(io.swagger.v3.oas.models.Operation operation, HandlerMethod handlerMethod) {
        if (operation.getSummary() == null || operation.getSummary().isBlank()) {
            String methodName = handlerMethod.getMethod().getName();
            String readableSummary = splitCamelCase(methodName);
            operation.setSummary(readableSummary);
        }
    }

    private void configureStandardResponses(io.swagger.v3.oas.models.Operation operation, HandlerMethod handlerMethod) {
        ApiResponses responses = operation.getResponses();
        if (responses == null) {
            responses = new ApiResponses();
            operation.setResponses(responses);
        }

        if (!responses.containsKey("500")) {
            responses.addApiResponse("500", new ApiResponse().description("Erro interno inesperado no processamento da solicitação"));
        }

        boolean hasRequestBody = Arrays.stream(handlerMethod.getMethodParameters())
                .anyMatch(param -> param.hasParameterAnnotation(RequestBody.class));

        if (hasRequestBody && !responses.containsKey("400")) {
            responses.addApiResponse("400", new ApiResponse().description("Dados da requisição inválidos ou payload mal formatado"));
        }
    }

    private String splitCamelCase(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        String formatted = s.replaceAll("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])", " ");
        return Character.toUpperCase(formatted.charAt(0)) + formatted.substring(1);
    }
}
