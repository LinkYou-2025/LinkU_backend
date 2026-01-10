package com.umc.linkyou.config.security.oauth;

import com.umc.linkyou.domain.enums.Provider;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.*;

@Component
@RequiredArgsConstructor
public class OAuth2TokenClient implements OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> {

    private static final String GRANT_TYPE = "grant_type";
    private static final String AUTHORIZATION_CODE = "authorization_code";
    private static final String CLIENT_ID = "client_id";
    private static final String REDIRECT_URI = "redirect_uri";
    private static final String CODE = "code";
    private static final String CLIENT_SECRET = "client_secret";

    private static final String ACCESS_TOKEN = "access_token";
    private static final String TOKEN_TYPE = "token_type";
    private static final String EXPIRES_IN = "expires_in";
    private static final String REFRESH_TOKEN = "refresh_token";
    private static final String SCOPE = "scope";

    private final OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> delegateForDefault =
            new DefaultAuthorizationCodeTokenResponseClient();

    private final RestTemplate restTemplate;

    @Override
    public OAuth2AccessTokenResponse getTokenResponse(OAuth2AuthorizationCodeGrantRequest request) {
        String registrationId = request.getClientRegistration().getRegistrationId();

        if (Provider.KAKAO.name().equalsIgnoreCase(registrationId)) {
            return requestKakaoToken(request);
        }

        return delegateForDefault.getTokenResponse(request);
    }

    private OAuth2AccessTokenResponse requestKakaoToken(OAuth2AuthorizationCodeGrantRequest request) {
        var clientRegistration = request.getClientRegistration();
        var authorizationResponse = request.getAuthorizationExchange().getAuthorizationResponse();

        String tokenUri = clientRegistration.getProviderDetails().getTokenUri();
        String clientId = clientRegistration.getClientId();
        String clientSecret = clientRegistration.getClientSecret();
        String code = authorizationResponse.getCode();
        String redirectUri = authorizationResponse.getRedirectUri();

        MultiValueMap<String, String> formParameters = new LinkedMultiValueMap<>();
        formParameters.add(GRANT_TYPE, AUTHORIZATION_CODE);
        formParameters.add(CLIENT_ID, clientId);
        formParameters.add(REDIRECT_URI, redirectUri);
        formParameters.add(CODE, code);

        if (clientSecret != null && !clientSecret.isBlank()) {
            formParameters.add(CLIENT_SECRET, clientSecret);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        RequestEntity<MultiValueMap<String, String>> requestEntity =
                new RequestEntity<>(formParameters, headers,
                        org.springframework.http.HttpMethod.POST, URI.create(tokenUri));

        ResponseEntity<Map<String, Object>> responseEntity =
                restTemplate.exchange(requestEntity, new ParameterizedTypeReference<>() {});

        Map<String, Object> body = responseEntity.getBody();
        if (body == null || !body.containsKey(ACCESS_TOKEN)) {
            throw new IllegalStateException("Invalid Kakao token response: " + body);
        }

        return convertToAccessTokenResponse(body);
    }

    private OAuth2AccessTokenResponse convertToAccessTokenResponse(Map<String, Object> body) {
        OAuth2AccessTokenResponse.Builder builder = OAuth2AccessTokenResponse
                .withToken((String) body.get(ACCESS_TOKEN))
                .tokenType(OAuth2AccessToken.TokenType.BEARER);

        Object expiresIn = body.get(EXPIRES_IN);
        if (expiresIn != null) {
            if (expiresIn instanceof Number number) {
                builder.expiresIn(number.intValue());
            } else {
                builder.expiresIn(Integer.parseInt(expiresIn.toString()));
            }
        }

        if (body.containsKey(REFRESH_TOKEN)) {
            builder.refreshToken((String) body.get(REFRESH_TOKEN));
        }

        if (body.containsKey(SCOPE)) {
            Object scopeObj = body.get(SCOPE);
            Set<String> scopes;
            if (scopeObj instanceof String scopeString) {
                scopes = new HashSet<>(Arrays.asList(scopeString.split(" ")));
            } else {
                scopes = new HashSet<>((Collection<String>) scopeObj);
            }
            builder.scopes(scopes);
        }

        Map<String, Object> additional = new HashMap<>();
        body.forEach((key, value) -> {
            if (!Set.of(ACCESS_TOKEN, TOKEN_TYPE, EXPIRES_IN, REFRESH_TOKEN, SCOPE).contains(key)) {
                additional.put(key, value);
            }
        });
        if (!additional.isEmpty()) {
            builder.additionalParameters(additional);
        }

        return builder.build();
    }
}
