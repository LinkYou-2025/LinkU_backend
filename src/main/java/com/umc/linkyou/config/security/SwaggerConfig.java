package com.umc.linkyou.config.security;

import com.umc.linkyou.apiPayload.code.ErrorReasonDTO;
import com.umc.linkyou.validation.annotation.ApiErrorCode;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.web.method.HandlerMethod;
import java.util.Arrays;
import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.examples.Example;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI linkyouAPI() {
        Info info = new Info()
                .title("linkyou API")
                .description("linkyou API 명세서")
                .version("1.0.0");

        String jwtSchemeName = "JWT TOKEN";
        SecurityRequirement securityRequirement = new SecurityRequirement().addList(jwtSchemeName);
        Components components = new Components()
                .addSecuritySchemes(jwtSchemeName, new SecurityScheme()
                        .name(jwtSchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"));

        return new OpenAPI()
                .addServersItem(new Server().url("/"))
                .info(info)
                .addSecurityItem(securityRequirement)
                .components(components);
    }

    // @ApiErrorCode 어노테이션을 스캔하여 응답 예시를 자동 생성하는 커스텀 로직
    @Bean
    public OperationCustomizer customize() {
        return (Operation operation, HandlerMethod handlerMethod) -> {
            ApiErrorCode apiErrorCode = handlerMethod.getMethodAnnotation(ApiErrorCode.class);

            if (apiErrorCode != null) {
                generateErrorCodeResponseExample(operation, apiErrorCode.value());
            }

            return operation;
        };
    }

    private void generateErrorCodeResponseExample(Operation operation, ErrorStatus[] errorStatuses) {
        ApiResponses responses = operation.getResponses();

        Arrays.stream(errorStatuses).forEach(status -> {
            ErrorReasonDTO reason = status.getReasonHttpStatus();

            // 공통 응답 객체 ApiResponse 형식에 맞춰 예시 데이터 생성
            com.umc.linkyou.apiPayload.ApiResponse<Object> exampleResponse =
                    com.umc.linkyou.apiPayload.ApiResponse.onFailure(
                            reason.getCode(),
                            reason.getMessage(),
                            null
                    );

            Example example = new Example();
            example.setValue(exampleResponse);

            Content content = new Content();
            MediaType mediaType = new MediaType();
            mediaType.addExamples(status.name(), example);
            content.addMediaType("application/json", mediaType);

            io.swagger.v3.oas.models.responses.ApiResponse response =
                    new io.swagger.v3.oas.models.responses.ApiResponse();
            response.setContent(content);
            response.setDescription(reason.getMessage());

            // HTTP 상태 코드가 같으면 같은 응답 그룹 안에 Example이 추가
            responses.addApiResponse(String.valueOf(reason.getHttpStatus().value()), response);
        });
    }
}
