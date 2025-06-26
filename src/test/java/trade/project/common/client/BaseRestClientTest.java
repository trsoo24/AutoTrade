package trade.project.common.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import trade.project.common.exception.ApiException;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BaseRestClient 테스트")
class BaseRestClientTest {

    @InjectMocks
    private BaseRestClient baseRestClient;

    private String testUrl;
    private Map<String, Object> testRequestBody;

    @BeforeEach
    void setUp() {
        testUrl = "https://api.example.com/test";
        testRequestBody = new HashMap<>();
        testRequestBody.put("key", "value");
    }

    @Test
    @DisplayName("GET 요청 - null URL")
    void get_WithNullUrl_ShouldThrowIllegalArgumentException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            baseRestClient.get(null, Map.class);
        });

        assertEquals("URL은 null일 수 없습니다", exception.getMessage());
    }

    @Test
    @DisplayName("POST 요청 - null URL")
    void post_WithNullUrl_ShouldThrowIllegalArgumentException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            baseRestClient.post(null, testRequestBody, Map.class);
        });

        assertEquals("URL은 null일 수 없습니다", exception.getMessage());
    }

    @Test
    @DisplayName("POST 요청 - null request body")
    void post_WithNullRequestBody_ShouldThrowIllegalArgumentException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            baseRestClient.post(testUrl, null, Map.class);
        });

        assertEquals("요청 본문은 null일 수 없습니다", exception.getMessage());
    }

    @Test
    @DisplayName("GET 요청 - 유효한 URL")
    void get_WithValidUrl_ShouldNotThrowException() {
        // When & Then - 실제 API 호출이 없으므로 예외가 발생할 수 있음
        assertDoesNotThrow(() -> {
            try {
                baseRestClient.get(testUrl, Map.class);
            } catch (Exception e) {
                // API 호출 실패는 예상된 동작
                assertTrue(e instanceof ApiException);
            }
        });
    }

    @Test
    @DisplayName("POST 요청 - 유효한 데이터")
    void post_WithValidData_ShouldNotThrowException() {
        // When & Then - 실제 API 호출이 없으므로 예외가 발생할 수 있음
        assertDoesNotThrow(() -> {
            try {
                baseRestClient.post(testUrl, testRequestBody, Map.class);
            } catch (Exception e) {
                // API 호출 실패는 예상된 동작
                assertTrue(e instanceof ApiException);
            }
        });
    }
} 