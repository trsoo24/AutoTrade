package trade.project.common.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import trade.project.common.exception.ApiException;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class BaseRestClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    /**
     * GET 요청을 수행합니다.
     */
    public <T> T get(String url, Class<T> responseType) {
        if (url == null) {
            throw new IllegalArgumentException("URL은 null일 수 없습니다");
        }
        return get(url, null, responseType);
    }

    /**
     * 헤더와 함께 GET 요청을 수행합니다.
     */
    public <T> T get(String url, Map<String, String> headers, Class<T> responseType) {
        if (url == null) {
            throw new IllegalArgumentException("URL은 null일 수 없습니다");
        }
        return executeRequest(url, HttpMethod.GET, headers, null, responseType);
    }

    /**
     * POST 요청을 수행합니다.
     */
    public <T> T post(String url, Object requestBody, Class<T> responseType) {
        if (url == null) {
            throw new IllegalArgumentException("URL은 null일 수 없습니다");
        }
        if (requestBody == null) {
            throw new IllegalArgumentException("요청 본문은 null일 수 없습니다");
        }
        return post(url, null, requestBody, responseType);
    }

    /**
     * 헤더와 함께 POST 요청을 수행합니다.
     */
    public <T> T post(String url, Map<String, String> headers, Object requestBody, Class<T> responseType) {
        if (url == null) {
            throw new IllegalArgumentException("URL은 null일 수 없습니다");
        }
        if (requestBody == null) {
            throw new IllegalArgumentException("요청 본문은 null일 수 없습니다");
        }
        return executeRequest(url, HttpMethod.POST, headers, requestBody, responseType);
    }

    /**
     * PUT 요청을 수행합니다.
     */
    public <T> T put(String url, Object requestBody, Class<T> responseType) {
        return put(url, null, requestBody, responseType);
    }

    /**
     * 헤더와 함께 PUT 요청을 수행합니다.
     */
    public <T> T put(String url, Map<String, String> headers, Object requestBody, Class<T> responseType) {
        return executeRequest(url, HttpMethod.PUT, headers, requestBody, responseType);
    }

    /**
     * DELETE 요청을 수행합니다.
     */
    public <T> T delete(String url, Class<T> responseType) {
        return delete(url, null, responseType);
    }

    /**
     * 헤더와 함께 DELETE 요청을 수행합니다.
     */
    public <T> T delete(String url, Map<String, String> headers, Class<T> responseType) {
        return executeRequest(url, HttpMethod.DELETE, headers, null, responseType);
    }

    /**
     * 비동기 GET 요청을 수행합니다.
     */
    public <T> Mono<T> getAsync(String url, Class<T> responseType) {
        return getAsync(url, null, responseType);
    }

    /**
     * 헤더와 함께 비동기 GET 요청을 수행합니다.
     */
    public <T> Mono<T> getAsync(String url, Map<String, String> headers, Class<T> responseType) {
        return executeRequestAsync(url, HttpMethod.GET, headers, null, responseType);
    }

    /**
     * 비동기 POST 요청을 수행합니다.
     */
    public <T> Mono<T> postAsync(String url, Object requestBody, Class<T> responseType) {
        return postAsync(url, null, requestBody, responseType);
    }

    /**
     * 헤더와 함께 비동기 POST 요청을 수행합니다.
     */
    public <T> Mono<T> postAsync(String url, Map<String, String> headers, Object requestBody, Class<T> responseType) {
        return executeRequestAsync(url, HttpMethod.POST, headers, requestBody, responseType);
    }

    /**
     * 실제 HTTP 요청을 수행하는 메서드
     */
    private <T> T executeRequest(String url, HttpMethod method, Map<String, String> headers, 
                                Object requestBody, Class<T> responseType) {
        try {
            return executeRequestAsync(url, method, headers, requestBody, responseType)
                    .timeout(DEFAULT_TIMEOUT)
                    .block();
        } catch (Exception e) {
            log.error("API 요청 실패: {} {} - {}", method, url, e.getMessage());
            throw new ApiException("API 요청 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 비동기 HTTP 요청을 수행하는 메서드
     */
    private <T> Mono<T> executeRequestAsync(String url, HttpMethod method, Map<String, String> headers, 
                                           Object requestBody, Class<T> responseType) {
        WebClient.RequestBodyUriSpec requestSpec = webClient.method(method);
        WebClient.RequestBodySpec bodySpec = requestSpec.uri(url);

        // 헤더 설정
        if (headers != null) {
            headers.forEach(bodySpec::header);
        }

        // 요청 본문 설정
        if (requestBody != null && (method == HttpMethod.POST || method == HttpMethod.PUT)) {
            bodySpec.contentType(MediaType.APPLICATION_JSON);
            bodySpec.bodyValue(requestBody);
        }

        return bodySpec
                .retrieve()
                .bodyToMono(String.class)
                .map(responseBody -> {
                    try {
                        return objectMapper.readValue(responseBody, responseType);
                    } catch (Exception e) {
                        log.error("응답 파싱 실패: {}", e.getMessage());
                        throw new ApiException("응답 파싱 실패: " + e.getMessage(), e);
                    }
                })
                .onErrorMap(WebClientResponseException.class, this::handleWebClientException)
                .onErrorMap(Exception.class, this::handleGenericException);
    }

    /**
     * WebClient 예외 처리
     */
    private ApiException handleWebClientException(WebClientResponseException ex) {
        log.error("WebClient 예외: {} - {}", ex.getStatusCode(), maskSensitiveInfo(ex.getResponseBodyAsString()));
        
        String errorMessage = String.format("API 호출 실패 (HTTP %d)", ex.getStatusCode().value());
        return new ApiException(errorMessage, "HTTP_ERROR", ex.getStatusCode().value(), ex);
    }

    /**
     * 일반 예외 처리
     */
    private ApiException handleGenericException(Exception ex) {
        log.error("일반 예외: {}", ex.getMessage());
        return new ApiException("API 호출 중 예외 발생", ex);
    }
    
    /**
     * 민감한 정보를 마스킹합니다.
     */
    private String maskSensitiveInfo(String responseBody) {
        if (responseBody == null || responseBody.length() < 100) {
            return responseBody;
        }
        
        // 응답이 너무 길면 앞부분만 표시하고 나머지는 마스킹
        return responseBody.substring(0, 100) + "... [마스킹됨]";
    }
} 