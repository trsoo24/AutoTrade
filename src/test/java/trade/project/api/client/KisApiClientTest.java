package trade.project.api.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import trade.project.common.client.BaseRestClient;
import trade.project.common.exception.ApiException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("한국투자증권 API 클라이언트 테스트")
class KisApiClientTest {

    @Mock
    private BaseRestClient baseRestClient;

    @InjectMocks
    private KisApiClient kisApiClient;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(kisApiClient, "baseUrl", "https://test.api.com");
        ReflectionTestUtils.setField(kisApiClient, "appKey", "test-app-key");
        ReflectionTestUtils.setField(kisApiClient, "appSecret", "test-app-secret");
    }

    @Test
    @DisplayName("토큰 발급 - 성공적인 경우")
    void getAccessToken_WithValidCredentials_ShouldReturnToken() {
        // Given
        Map<String, Object> response = new HashMap<>();
        response.put("access_token", "test-token-123");
        when(baseRestClient.post(anyString(), anyMap(), anyMap(), eq(Map.class)))
                .thenReturn(response);

        // When
        String token = kisApiClient.getAccessToken();

        // Then
        assertNotNull(token);
        assertEquals("test-token-123", token);
        verify(baseRestClient).post(anyString(), anyMap(), anyMap(), eq(Map.class));
    }

    @Test
    @DisplayName("토큰 발급 - 응답에 토큰이 없는 경우")
    void getAccessToken_WithInvalidResponse_ShouldThrowException() {
        // Given
        Map<String, Object> response = new HashMap<>();
        response.put("error", "invalid_credentials");
        when(baseRestClient.post(anyString(), anyMap(), anyMap(), eq(Map.class)))
                .thenReturn(response);

        // When & Then
        ApiException exception = assertThrows(ApiException.class, () -> {
            kisApiClient.getAccessToken();
        });
        assertTrue(exception.getMessage().contains("토큰 발급 실패"));
    }

    @Test
    @DisplayName("토큰 캐싱 - 유효한 토큰이 있으면 재사용")
    void getAccessToken_WithValidCachedToken_ShouldReturnCachedToken() {
        // Given
        Map<String, Object> response = new HashMap<>();
        response.put("access_token", "cached-token-123");
        when(baseRestClient.post(anyString(), anyMap(), anyMap(), eq(Map.class)))
                .thenReturn(response);

        // When - 첫 번째 호출로 토큰 캐싱
        String firstToken = kisApiClient.getAccessToken();
        
        // Then - 두 번째 호출은 캐시된 토큰 사용
        String secondToken = kisApiClient.getAccessToken();
        
        assertEquals(firstToken, secondToken);
        // API 호출은 한 번만 발생해야 함
        verify(baseRestClient, times(1)).post(anyString(), anyMap(), anyMap(), eq(Map.class));
    }

    @Test
    @DisplayName("주식 현재가 조회 - 성공적인 경우")
    void getStockPrice_WithValidStockCode_ShouldReturnPriceData() {
        // Given
        String stockCode = "005930";
        Map<String, Object> expectedResponse = new HashMap<>();
        expectedResponse.put("output", new HashMap<>());
        
        when(baseRestClient.get(anyString(), anyMap(), eq(Map.class)))
                .thenReturn(expectedResponse);

        // When
        Map<String, Object> result = kisApiClient.getStockPrice(stockCode);

        // Then
        assertNotNull(result);
        verify(baseRestClient).get(contains("/uapi/domestic-stock/v1/quotations/inquire-price"), anyMap(), eq(Map.class));
    }

    @Test
    @DisplayName("주식 일자별 시세 조회 - 성공적인 경우")
    void getStockDailyPrice_WithValidParameters_ShouldReturnDailyData() {
        // Given
        String stockCode = "005930";
        String startDate = "20240101";
        String endDate = "20240131";
        Map<String, Object> expectedResponse = new HashMap<>();
        expectedResponse.put("output", new HashMap<>());
        
        when(baseRestClient.get(anyString(), anyMap(), eq(Map.class)))
                .thenReturn(expectedResponse);

        // When
        Map<String, Object> result = kisApiClient.getStockDailyPrice(stockCode, startDate, endDate);

        // Then
        assertNotNull(result);
        verify(baseRestClient).get(contains("/uapi/domestic-stock/v1/quotations/inquire-daily-price"), anyMap(), eq(Map.class));
    }

    @Test
    @DisplayName("주식 체결 내역 조회 - 성공적인 경우")
    void getStockTradeHistory_WithValidParameters_ShouldReturnTradeData() {
        // Given
        String stockCode = "005930";
        String startDate = "20240101";
        String endDate = "20240131";
        Map<String, Object> expectedResponse = new HashMap<>();
        expectedResponse.put("output", new HashMap<>());
        
        when(baseRestClient.get(anyString(), anyMap(), eq(Map.class)))
                .thenReturn(expectedResponse);

        // When
        Map<String, Object> result = kisApiClient.getStockTradeHistory(stockCode, startDate, endDate);

        // Then
        assertNotNull(result);
        verify(baseRestClient).get(contains("/uapi/domestic-stock/v1/trading/inquire-daily-ccld"), anyMap(), eq(Map.class));
    }

    @Test
    @DisplayName("계좌 잔고 조회 - 성공적인 경우")
    void getAccountBalance_WithValidAccountNumber_ShouldReturnBalanceData() {
        // Given
        String accountNumber = "1234567890";
        Map<String, Object> expectedResponse = new HashMap<>();
        expectedResponse.put("output", new HashMap<>());
        
        when(baseRestClient.get(anyString(), anyMap(), eq(Map.class)))
                .thenReturn(expectedResponse);

        // When
        Map<String, Object> result = kisApiClient.getAccountBalance(accountNumber);

        // Then
        assertNotNull(result);
        verify(baseRestClient).get(contains("/uapi/domestic-stock/v1/trading/inquire-balance"), anyMap(), eq(Map.class));
    }

    @Test
    @DisplayName("API 호출 실패 - 예외 처리")
    void apiCall_WhenExceptionOccurs_ShouldThrowApiException() {
        // Given
        when(baseRestClient.get(anyString(), anyMap(), eq(Map.class)))
                .thenThrow(new RuntimeException("Network error"));

        // When & Then
        ApiException exception = assertThrows(ApiException.class, () -> {
            kisApiClient.getStockPrice("005930");
        });
        assertTrue(exception.getMessage().contains("주식 시세 조회 실패"));
    }

    @Test
    @DisplayName("토큰 만료 - 만료된 토큰은 재발급")
    void getAccessToken_WithExpiredToken_ShouldRefreshToken() {
        // Given
        Map<String, Object> response = new HashMap<>();
        response.put("access_token", "new-token-456");
        when(baseRestClient.post(anyString(), anyMap(), anyMap(), eq(Map.class)))
                .thenReturn(response);

        // When - 첫 번째 호출
        String firstToken = kisApiClient.getAccessToken();
        
        // 토큰 만료 시간을 과거로 설정
        ReflectionTestUtils.setField(kisApiClient, "tokenExpiryTime", LocalDateTime.now().minusMinutes(1));
        
        // When - 두 번째 호출 (토큰 만료로 인한 재발급)
        String secondToken = kisApiClient.getAccessToken();

        // Then
        assertNotEquals(firstToken, secondToken);
        assertEquals("new-token-456", secondToken);
        // API 호출이 두 번 발생해야 함 (초기 발급 + 재발급)
        verify(baseRestClient, times(2)).post(anyString(), anyMap(), anyMap(), eq(Map.class));
    }

    @Test
    @DisplayName("헤더 생성 - 올바른 인증 헤더 포함")
    void getAuthHeaders_ShouldIncludeCorrectHeaders() {
        // Given
        Map<String, Object> response = new HashMap<>();
        response.put("access_token", "test-token");
        when(baseRestClient.post(anyString(), anyMap(), anyMap(), eq(Map.class)))
                .thenReturn(response);

        // When
        kisApiClient.getAccessToken(); // 토큰 발급
        Map<String, Object> result = kisApiClient.getStockPrice("005930"); // 헤더 사용

        // Then
        verify(baseRestClient).get(anyString(), argThat(headers -> {
            return headers.containsKey("authorization") &&
                   headers.containsKey("appkey") &&
                   headers.containsKey("appsecret") &&
                   headers.containsKey("tr_id") &&
                   headers.containsKey("custtype") &&
                   headers.containsKey("content-type");
        }), eq(Map.class));
    }
} 