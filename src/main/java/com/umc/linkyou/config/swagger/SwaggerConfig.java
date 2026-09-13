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
import com.umc.linkyou.validation.annotation.ApiAdmin;
import com.umc.linkyou.validation.annotation.ApiManager;
import com.umc.linkyou.validation.annotation.swagger.ApiAuthSuccessCode;
import com.umc.linkyou.validation.annotation.swagger.ApiDomainErrorCodes;
import com.umc.linkyou.validation.annotation.swagger.ApiErrorCode;
import com.umc.linkyou.validation.annotation.swagger.ApiErrorCodes;
import com.umc.linkyou.validation.annotation.swagger.ApiNoContentCode;
import com.umc.linkyou.validation.annotation.swagger.ApiSuccessCode;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.converter.ResolvedSchema;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

@Slf4j
@Configuration
public class SwaggerConfig {

    private static final String BASE_PACKAGE = "com.umc.linkyou";

    private enum ApiGroup { USER, MANAGER, ADMIN }

    // 그룹별로 실제 @ApiErrorCode/@ApiDomainErrorCodes에 등장하는 에러 enum 집합 (지연 계산 후 캐시)
    private Map<ApiGroup, Set<Class<? extends BaseErrorCode>>> errorEnumsByGroupCache;

    @Bean
    public OpenAPI linkyouAPI() {
        Info info = new Info()
                .title("linkyou API")
                .description("linkyou API 명세서\n\n" + buildErrorCodeReference())
                .version("1.0.0");

        String jwtSchemeName = "JWT TOKEN";
        SecurityRequirement securityRequirement = new SecurityRequirement().addList(jwtSchemeName);

        ResolvedSchema resolvedSchema = ModelConverters.getInstance()
                .resolveAsResolvedSchema(new AnnotatedType(com.umc.linkyou.apiPayload.ApiResponse.class));


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
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
                .group("user")
                .pathsToMatch("/api/v1/**", "/api/v2/**")
                .pathsToExclude("/api/v1/admin/**", "/api/v1/manage/**")
                .addOperationCustomizer(customize())
                .addOpenApiCustomizer(openApi -> openApi.getInfo().setDescription(
                        "linkyou API 명세서 (User)\n\n" + buildErrorCodeReference(errorEnumsForGroup(ApiGroup.USER))))
                .build();
    }

    @Bean
    public GroupedOpenApi managerApi() {
        return GroupedOpenApi.builder()
                .group("manager")
                .pathsToMatch("/api/v1/manage/**")
                .addOperationCustomizer(customize())
                .addOpenApiCustomizer(openApi -> openApi.getInfo().setDescription(
                        "linkyou API 명세서 (Manager)\n\n" + buildErrorCodeReference(errorEnumsForGroup(ApiGroup.MANAGER))))
                .build();
    }

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("admin")
                .pathsToMatch("/api/v1/admin/**")
                .addOperationCustomizer(customize())
                .addOpenApiCustomizer(openApi -> openApi.getInfo().setDescription(
                        "linkyou API 명세서 (Admin)\n\n" + buildErrorCodeReference(errorEnumsForGroup(ApiGroup.ADMIN))))
                .build();
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
            ApiNoContentCode noContentAnnotation = handlerMethod.getMethodAnnotation(ApiNoContentCode.class);
            if (noContentAnnotation != null) {
                operation.getResponses().addApiResponse("204",
                        new ApiResponse().description(noContentAnnotation.description()));
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
            mediaType.setSchema(new Schema<>().$ref("#/components/schemas/ApiResponse"));
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

    private record ErrorCategory(
            Function<ApiErrorCode, ? extends BaseErrorCode[]> extractor,
            Class<? extends BaseErrorCode> type
    ) {}

    private static final List<ErrorCategory> ERROR_CATEGORIES = List.of(
            new ErrorCategory(ApiErrorCode::errorStatus, ErrorStatus.class),
            new ErrorCategory(ApiErrorCode::userErrorStatus, UserErrorStatus.class),
            new ErrorCategory(ApiErrorCode::authErrorStatus, AuthErrorStatus.class),
            new ErrorCategory(ApiErrorCode::alarmErrorStatus, AlarmErrorStatus.class),
            new ErrorCategory(ApiErrorCode::aiArticleErrorStatus, AiArticleErrorStatus.class),
            new ErrorCategory(ApiErrorCode::curationErrorStatus, CurationErrorStatus.class),
            new ErrorCategory(ApiErrorCode::linkuErrorStatus, LinkuErrorStatus.class),
            new ErrorCategory(ApiErrorCode::folderErrorStatus, FolderErrorStatus.class),
            new ErrorCategory(ApiErrorCode::shareFolderErrorStatus, ShareFolderErrorStatus.class),
            new ErrorCategory(ApiErrorCode::invitationErrorStatus, InvitationErrorStatus.class),
            new ErrorCategory(ApiErrorCode::categoryErrorStatus, CategoryErrorStatus.class),
            new ErrorCategory(ApiErrorCode::commonErrorStatus, CommonErrorStatus.class)
    );

    // 에러 예시 생성
    private void generateErrorCodeResponseExample(Operation operation, ApiErrorCode[] annotations) {
        ApiResponses responses = operation.getResponses();

        for (ApiErrorCode annotation : annotations) {
            for (ErrorCategory category : ERROR_CATEGORIES) {
                for (BaseErrorCode status : category.extractor().apply(annotation)) {
                    addErrorCodeExample(responses, status);
                }
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
        return buildErrorCodeReference(allErrorEnums());
    }

    private List<Class<? extends BaseErrorCode>> allErrorEnums() {
        return List.of(
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
    }

    private String buildErrorCodeReference(Collection<Class<? extends BaseErrorCode>> errorEnums) {
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

    // 그룹(user/manager/admin)에 속한 컨트롤러가 실제로 선언한 에러 enum 목록을 구한다.
    private Set<Class<? extends BaseErrorCode>> errorEnumsForGroup(ApiGroup group) {
        return errorEnumsByGroup().getOrDefault(group, Set.of());
    }

    private Map<ApiGroup, Set<Class<? extends BaseErrorCode>>> errorEnumsByGroup() {
        if (errorEnumsByGroupCache == null) {
            errorEnumsByGroupCache = scanErrorEnumsByGroup();
        }
        return errorEnumsByGroupCache;
    }

    private Map<ApiGroup, Set<Class<? extends BaseErrorCode>>> scanErrorEnumsByGroup() {
        Map<ApiGroup, Set<Class<? extends BaseErrorCode>>> result = new EnumMap<>(ApiGroup.class);
        for (ApiGroup group : ApiGroup.values()) {
            // 인증이 필요한 엔드포인트에는 401 예시가 자동으로 붙으므로 AuthErrorStatus는 모든 그룹에 기본 포함
            Set<Class<? extends BaseErrorCode>> bucket = new LinkedHashSet<>();
            bucket.add(AuthErrorStatus.class);
            result.put(group, bucket);
        }

        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));

        for (org.springframework.beans.factory.config.BeanDefinition bd : scanner.findCandidateComponents(BASE_PACKAGE)) {
            try {
                Class<?> controllerClass = Class.forName(bd.getBeanClassName());
                ApiGroup group = controllerClass.isAnnotationPresent(ApiAdmin.class) ? ApiGroup.ADMIN
                        : controllerClass.isAnnotationPresent(ApiManager.class) ? ApiGroup.MANAGER
                        : ApiGroup.USER;

                Set<Class<? extends BaseErrorCode>> bucket = result.get(group);

                collectDomainErrorCodes(controllerClass, bucket);
                collectMethodErrorCodes(controllerClass, bucket);
                for (Class<?> iface : controllerClass.getInterfaces()) {
                    collectDomainErrorCodes(iface, bucket);
                    collectMethodErrorCodes(iface, bucket);
                }
            } catch (ClassNotFoundException | LinkageError e) {
                // NoClassDefFoundError 등 복구 가능한 클래스 로딩 실패만 스킵하고 원인을 남긴다.
                // OutOfMemoryError 등 치명적 오류(Error 중 LinkageError가 아닌 것)는 여기서 잡히지 않고 그대로 전파된다.
                log.warn("Swagger 에러코드 문서 생성 중 컨트롤러 클래스 로딩 실패: {} - {}",
                        bd.getBeanClassName(), e.toString());
            }
        }

        return result;
    }

    private void collectDomainErrorCodes(Class<?> type, Set<Class<? extends BaseErrorCode>> bucket) {
        ApiDomainErrorCodes domainCodes = type.getAnnotation(ApiDomainErrorCodes.class);
        if (domainCodes != null) {
            bucket.addAll(Arrays.asList(domainCodes.value()));
        }
    }

    private void collectMethodErrorCodes(Class<?> type, Set<Class<? extends BaseErrorCode>> bucket) {
        for (Method method : type.getMethods()) {
            List<ApiErrorCode> codes = new ArrayList<>();
            ApiErrorCode single = method.getAnnotation(ApiErrorCode.class);
            if (single != null) codes.add(single);
            ApiErrorCodes multiple = method.getAnnotation(ApiErrorCodes.class);
            if (multiple != null) codes.addAll(Arrays.asList(multiple.value()));

            for (ApiErrorCode code : codes) {
                for (ErrorCategory category : ERROR_CATEGORIES) {
                    if (category.extractor().apply(code).length > 0) {
                        bucket.add(category.type());
                    }
                }
            }
        }
    }

}
