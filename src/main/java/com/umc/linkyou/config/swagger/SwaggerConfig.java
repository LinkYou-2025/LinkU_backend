package com.umc.linkyou.config.swagger;

import com.umc.linkyou.apiPayload.code.BaseErrorCode;
import com.umc.linkyou.apiPayload.code.ErrorReasonDTO;
import com.umc.linkyou.apiPayload.code.ReasonDTO;
import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.code.status.SuccessStatus;
import com.umc.linkyou.apiPayload.code.SuccessReasonDTO;
import com.umc.linkyou.apiPayload.code.status.CommonErrorStatus;
import com.umc.linkyou.apiPayload.code.status.aiarticle.AiArticleErrorStatus;
import com.umc.linkyou.apiPayload.code.status.alarm.AlarmErrorStatus;
import com.umc.linkyou.apiPayload.code.status.auth.AuthErrorStatus;
import com.umc.linkyou.apiPayload.code.status.auth.AuthSuccessStatus;
import com.umc.linkyou.apiPayload.code.status.category.CategoryErrorStatus;
import com.umc.linkyou.apiPayload.code.status.curation.CurationErrorStatus;
import com.umc.linkyou.apiPayload.code.status.folder.FolderErrorStatus;
import com.umc.linkyou.apiPayload.code.status.folder.InvitationErrorStatus;
import com.umc.linkyou.apiPayload.code.status.folder.ShareFolderErrorStatus;
import com.umc.linkyou.apiPayload.code.status.gemini.GeminiErrorStatus;
import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.apiPayload.code.status.linku.LinkuErrorStatus;
import com.umc.linkyou.validation.annotation.swagger.ApiAuthSuccessCode;
import com.umc.linkyou.validation.annotation.swagger.ApiDomainErrorCodes;
import com.umc.linkyou.validation.annotation.swagger.ApiErrorCode;
import com.umc.linkyou.validation.annotation.swagger.ApiErrorCodes;
import com.umc.linkyou.validation.annotation.swagger.ApiSuccessCode;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI linkyouAPI() {
        Info info = new Info()
                .title("linkyou API")
                .description("linkyou API 명세서\n\n" + buildErrorCodeReference())
                .version("1.0.0");

        String jwtSchemeName = "JWT TOKEN";
        SecurityRequirement securityRequirement = new SecurityRequirement().addList(jwtSchemeName);

        io.swagger.v3.core.converter.ResolvedSchema resolvedSchema =
                io.swagger.v3.core.converter.ModelConverters.getInstance()
                        .resolveAsResolvedSchema(new io.swagger.v3.core.converter
                                .AnnotatedType(com.umc.linkyou.apiPayload.ApiResponse.class));


        Components components = new Components()
                .addSecuritySchemes(jwtSchemeName, new SecurityScheme()
                        .name(jwtSchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"))
                .addSchemas("ApiResponse", resolvedSchema.schema);

        return new OpenAPI()
                .addServersItem(new Server().url("/"))
                .info(info)
                .addSecurityItem(securityRequirement)
                .components(components);
    }

    @Bean
    public OperationCustomizer customize() {
        return (Operation operation, HandlerMethod handlerMethod) -> {
            // 인증이 명시된 엔드포인트에만 401 예시를 추가한다.
            if (requiresAuthentication(operation)) {
                addErrorCodeExample(operation.getResponses(), AuthErrorStatus.UNAUTHORIZED);
            }

            // 1. 성공 응답 처리
            ApiSuccessCode successAnnotation = handlerMethod.getMethodAnnotation(ApiSuccessCode.class);
            if (successAnnotation != null) {
                generateSuccessResponseExample(operation, successAnnotation.value());
            }
            ApiAuthSuccessCode authSuccessAnnotation = handlerMethod.getMethodAnnotation(ApiAuthSuccessCode.class);
            if (authSuccessAnnotation != null) {
                generateAuthSuccessResponseExample(operation, authSuccessAnnotation.value());
            }

            // 2. 에러 응답 처리 (Repeatable 컨테이너와 단일 어노테이션 모두 상속 관계 포함 스캔)
            List<ApiErrorCode> errorCodes = new ArrayList<>();

            // 단일로 붙었을 때
            ApiErrorCode single = handlerMethod.getMethodAnnotation(ApiErrorCode.class);
            if (single != null) errorCodes.add(single);

            // 여러 개 붙었을 때 (바구니 어노테이션 탐색)
            ApiErrorCodes multiple = handlerMethod.getMethodAnnotation(ApiErrorCodes.class);
            if (multiple != null) {
                errorCodes.addAll(Arrays.asList(multiple.value()));
            }

            if (!errorCodes.isEmpty()) {
                generateErrorCodeResponseExample(operation, errorCodes.toArray(new ApiErrorCode[0]));
            }

            // 3. 클래스/인터페이스 단위 도메인 에러 전체 주입
            ApiDomainErrorCodes domainCodes = handlerMethod.getBeanType().getAnnotation(ApiDomainErrorCodes.class);
            if (domainCodes == null) {
                for (Class<?> iface : handlerMethod.getBeanType().getInterfaces()) {
                    domainCodes = iface.getAnnotation(ApiDomainErrorCodes.class);
                    if (domainCodes != null) break;
                }
            }
            if (domainCodes != null) {
                ApiResponses responses = operation.getResponses();
                for (Class<? extends BaseErrorCode> enumClass : domainCodes.value()) {
                    for (BaseErrorCode code : enumClass.getEnumConstants()) {
                        addErrorCodeExample(responses, code);
                    }
                }
            }

            return operation;
        };
    }

    private boolean requiresAuthentication(Operation operation) {
        return operation.getSecurity() != null
                && operation.getSecurity().stream()
                .anyMatch(requirement -> requirement != null && !requirement.isEmpty());
    }

    // addExample 메서드 수정 (Schema 주입 필수)
    private void addExample(ApiResponses responses, int httpStatus, String name, String desc, Object value) {
        String statusKey = String.valueOf(httpStatus);

        ApiResponse response = responses.computeIfAbsent(statusKey, k -> new ApiResponse().description(desc));

        if (response.getContent() == null) {
            response.setContent(new Content());
        }

        Content content = response.getContent();
        if (content.get("application/json") == null) {
            content.addMediaType("application/json", new MediaType());
        }

        MediaType mediaType = content.get("application/json");

        // [중요] Schema가 없으면 Swagger UI가 예시를 렌더링하지 못함
        if (mediaType.getSchema() == null) {
            mediaType.setSchema(new io.swagger.v3.oas.models.media.Schema<>().$ref("#/components/schemas/ApiResponse"));
        }

        Example example = new Example();
        example.setValue(value);
        mediaType.addExamples(name, example);

        // Swagger UI의 Example Value가 공통 스키마 예시 대신 실제 응답 예시를 우선 표시하도록 맞춘다.
        if (mediaType.getExample() == null) {
            mediaType.setExample(value);
        }
    }

    // 성공 예시 생성
    private void generateSuccessResponseExample(Operation operation, SuccessStatus status) {
        ApiResponses responses = operation.getResponses();
        ReasonDTO reason = status.getReasonHttpStatus();

        com.umc.linkyou.apiPayload.ApiResponse<Object> exampleResponse =
                com.umc.linkyou.apiPayload.ApiResponse.onSuccess(status);

        addExample(responses, reason.getHttpStatus().value(), status.name(), reason.getMessage(), exampleResponse);
    }

    private void generateAuthSuccessResponseExample(Operation operation, AuthSuccessStatus status) {
        ApiResponses responses = operation.getResponses();
        SuccessReasonDTO reason = status.getReasonHttpStatus();

        com.umc.linkyou.apiPayload.ApiResponse<Object> exampleResponse =
                com.umc.linkyou.apiPayload.ApiResponse.onSuccess(status, null);

        addExample(responses, reason.getHttpStatus().value(), status.name(), reason.getMessage(), exampleResponse);
    }

    // 에러 예시 생성
    private void generateErrorCodeResponseExample(Operation operation, ApiErrorCode[] annotations) {
        ApiResponses responses = operation.getResponses();

        for (ApiErrorCode annotation : annotations) {
            for (ErrorStatus status : annotation.errorStatus()) {
                addErrorCodeExample(responses, status);
            }
            for (UserErrorStatus status : annotation.userErrorStatus()) {
                addErrorCodeExample(responses, status);
            }
            for (AuthErrorStatus status : annotation.authErrorStatus()) {
                addErrorCodeExample(responses, status);
            }
            for (CurationErrorStatus status : annotation.curationErrorStatus()) {
                addErrorCodeExample(responses, status);
            }
            for (FolderErrorStatus status : annotation.folderErrorStatus()) {
                addErrorCodeExample(responses, status);
            }
            for (ShareFolderErrorStatus status : annotation.shareFolderErrorStatus()) {
                addErrorCodeExample(responses, status);
            }
            for (InvitationErrorStatus status : annotation.invitationErrorStatus()) {
                addErrorCodeExample(responses, status);
            }
            for (CategoryErrorStatus status : annotation.categoryErrorStatus()) {
                addErrorCodeExample(responses, status);
            }
        }
    }

    private void addErrorCodeExample(ApiResponses responses, BaseErrorCode status) {
        ErrorReasonDTO reason = status.getReasonHttpStatus();
        com.umc.linkyou.apiPayload.ApiResponse<Object> exampleResponse =
                com.umc.linkyou.apiPayload.ApiResponse.onFailure(reason.getCode(), reason.getMessage(), null);

        addExample(responses, reason.getHttpStatus().value(), ((Enum<?>)status).name(), reason.getMessage(), exampleResponse);
    }

    private String buildErrorCodeReference() {
        List<Class<? extends BaseErrorCode>> errorEnums = List.of(
                AuthErrorStatus.class,
                UserErrorStatus.class,
                ErrorStatus.class,
                AlarmErrorStatus.class,
                AiArticleErrorStatus.class,
                CurationErrorStatus.class,
                FolderErrorStatus.class,
                ShareFolderErrorStatus.class,
                InvitationErrorStatus.class,
                CategoryErrorStatus.class,
                GeminiErrorStatus.class,
                LinkuErrorStatus.class,
                CommonErrorStatus.class
        );

        StringBuilder sb = new StringBuilder("---\n## 에러 코드 레퍼런스\n\n");

        for (Class<? extends BaseErrorCode> errorEnum : errorEnums) {
            sb.append("<details>\n");
            sb.append("<summary><b>").append(errorEnum.getSimpleName()).append("</b></summary>\n\n");
            sb.append("| Code | HTTP | Message |\n");
            sb.append("|------|:----:|---------|\n");

            for (BaseErrorCode code : errorEnum.getEnumConstants()) {
                ErrorReasonDTO reason = code.getReasonHttpStatus();
                sb.append("| `").append(reason.getCode()).append("` | ")
                        .append(reason.getHttpStatus().value()).append(" | ")
                        .append(reason.getMessage()).append(" |\n");
            }
            sb.append("\n</details>\n\n");
        }

        return sb.toString();
    }

}
