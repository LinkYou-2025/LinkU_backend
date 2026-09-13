package com.umc.linkyou.support.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.List;

// MockMvc 응답의 ApiResponse.result를 ObjectMapper로 역직렬화하기 위한 테스트 유틸
public class ApiResponseTestUtils {

    private ApiResponseTestUtils() {}

    public static <T> T readResult(MvcResult mvcResult, ObjectMapper objectMapper, Class<T> resultType) throws Exception {
        return objectMapper.treeToValue(resultNode(mvcResult, objectMapper), resultType);
    }

    public static <T> List<T> readResultList(MvcResult mvcResult, ObjectMapper objectMapper, Class<T> itemType) throws Exception {
        return objectMapper.readerForListOf(itemType).readValue(resultNode(mvcResult, objectMapper));
    }

    private static JsonNode resultNode(MvcResult mvcResult, ObjectMapper objectMapper) throws Exception {
        String content = mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(content).get("result");
    }
}
