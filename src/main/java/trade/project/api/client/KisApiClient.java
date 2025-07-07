package trade.project.api.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import trade.project.common.client.BaseRestClient;
import trade.project.common.dto.ApiResponse;
import trade.project.common.exception.ApiException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class KisApiClient {

    private final BaseRestClient baseRestClient;

    @Value("${kis.api.base-url:https://openapi.koreainvestment.com:9443}")
    private String baseUrl;

    @Value("${kis.api.app-key}")
    private String appKey;

    @Value("${kis.api.app-secret}")
    private String appSecret;

    // 토큰 캐싱을 위한 필드
    private String cachedAccessToken;
    private LocalDateTime tokenExpiryTime;
    private static final int TOKEN_EXPIRY_MINUTES = 23; // 24시간보다 조금 짧게 설정

    // API 엔드포인트 상수
    private static final String OAUTH_TOKEN_ENDPOINT = "/oauth2/tokenP";
    private static final String STOCK_PRICE_ENDPOINT = "/uapi/domestic-stock/v1/quotations/inquire-price";
    private static final String STOCK_DAILY_PRICE_ENDPOINT = "/uapi/domestic-stock/v1/quotations/inquire-daily-price";
    private static final String STOCK_TRADE_HISTORY_ENDPOINT = "/uapi/domestic-stock/v1/trading/inquire-daily-ccld";
    private static final String ACCOUNT_BALANCE_ENDPOINT = "/uapi/domestic-stock/v1/trading/inquire-balance";
    private static final String STOCK_ORDER_ENDPOINT = "/uapi/domestic-stock/v1/trading/order-cash";
    private static final String ORDER_STATUS_ENDPOINT = "/uapi/domestic-stock/v1/trading/inquire-order";

    // TR ID 상수
    private static final String TR_ID_STOCK_PRICE = "FHKST01010100";
    private static final String TR_ID_STOCK_DAILY = "FHKST01010400";
    private static final String TR_ID_TRADE_HISTORY = "TTTC8001R";
    private static final String TR_ID_ACCOUNT_BALANCE = "TTTC8434R";
    private static final String TR_ID_STOCK_ORDER = "TTTC0802U";
    private static final String TR_ID_ORDER_STATUS = "TTTC8001R";

    /**
     * 한국투자증권 API 인증 토큰 발급 (캐싱 포함)
     */
    public String getAccessToken() {
        // 토큰이 유효한지 확인
        if (isTokenValid()) {
            return cachedAccessToken;
        }

        // 토큰 재발급
        return refreshAccessToken();
    }

    /**
     * 토큰 유효성 검사
     */
    private boolean isTokenValid() {
        return cachedAccessToken != null && 
               tokenExpiryTime != null && 
               LocalDateTime.now().isBefore(tokenExpiryTime);
    }

    /**
     * 토큰 재발급
     */
    private String refreshAccessToken() {
        try {
            String url = baseUrl + OAUTH_TOKEN_ENDPOINT;
            
            Map<String, String> headers = new HashMap<>();
            headers.put("content-type", "application/json");
            
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("grant_type", "client_credentials");
            requestBody.put("appkey", appKey);
            requestBody.put("appsecret", appSecret);

            Map<String, Object> response = baseRestClient.post(url, headers, requestBody, Map.class);
            
            if (response.containsKey("access_token")) {
                cachedAccessToken = (String) response.get("access_token");
                tokenExpiryTime = LocalDateTime.now().plusMinutes(TOKEN_EXPIRY_MINUTES);
                
                log.info("토큰 재발급 완료, 만료시간: {}", tokenExpiryTime);
                return cachedAccessToken;
            } else {
                throw new ApiException("토큰 발급 실패: " + response);
            }
        } catch (Exception e) {
            log.error("토큰 발급 중 오류 발생: {}", e.getMessage());
            throw new ApiException("토큰 발급 실패", e);
        }
    }

    /**
     * 주식 현재가 시세 조회
     */
    public Map<String, Object> getStockPrice(String stockCode) {
        try {
            String url = baseUrl + STOCK_PRICE_ENDPOINT;
            
            Map<String, String> headers = getAuthHeaders();
            headers.put("tr_id", TR_ID_STOCK_PRICE);
            
            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("FID_COND_MRKT_DIV_CODE", "J");
            queryParams.put("FID_INPUT_ISCD", stockCode);

            String fullUrl = url + "?" + buildQueryString(queryParams);
            
            return baseRestClient.get(fullUrl, headers, Map.class);
        } catch (Exception e) {
            log.error("주식 시세 조회 중 오류 발생: {}", e.getMessage());
            throw new ApiException("주식 시세 조회 실패", e);
        }
    }

    /**
     * 주식 일자별 시세 조회
     */
    public Map<String, Object> getStockDailyPrice(String stockCode, String startDate, String endDate) {
        try {
            String url = baseUrl + STOCK_DAILY_PRICE_ENDPOINT;
            
            Map<String, String> headers = getAuthHeaders();
            headers.put("tr_id", TR_ID_STOCK_DAILY);
            
            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("FID_COND_MRKT_DIV_CODE", "J");
            queryParams.put("FID_INPUT_ISCD", stockCode);
            queryParams.put("FID_INPUT_DATE_1", startDate);
            queryParams.put("FID_INPUT_DATE_2", endDate);
            queryParams.put("FID_PERIOD_DIV_CODE", "D");

            String fullUrl = url + "?" + buildQueryString(queryParams);
            
            return baseRestClient.get(fullUrl, headers, Map.class);
        } catch (Exception e) {
            log.error("주식 일자별 시세 조회 중 오류 발생: {}", e.getMessage());
            throw new ApiException("주식 일자별 시세 조회 실패", e);
        }
    }

    /**
     * 주식 체결 내역 조회
     */
    public Map<String, Object> getStockTradeHistory(String stockCode, String startDate, String endDate) {
        try {
            String url = baseUrl + STOCK_TRADE_HISTORY_ENDPOINT;
            
            Map<String, String> headers = getAuthHeaders();
            headers.put("tr_id", TR_ID_TRADE_HISTORY);
            
            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("FID_COND_MRKT_DIV_CODE", "J");
            queryParams.put("FID_INPUT_ISCD", stockCode);
            queryParams.put("FID_INPUT_DATE_1", startDate);
            queryParams.put("FID_INPUT_DATE_2", endDate);
            queryParams.put("FID_PERIOD_DIV_CODE", "D");

            String fullUrl = url + "?" + buildQueryString(queryParams);
            
            return baseRestClient.get(fullUrl, headers, Map.class);
        } catch (Exception e) {
            log.error("주식 체결 내역 조회 중 오류 발생: {}", e.getMessage());
            throw new ApiException("주식 체결 내역 조회 실패", e);
        }
    }

    /**
     * 계좌 잔고 조회
     */
    public Map<String, Object> getAccountBalance(String accountNumber) {
        try {
            String url = baseUrl + ACCOUNT_BALANCE_ENDPOINT;
            
            Map<String, String> headers = getAuthHeaders();
            headers.put("tr_id", TR_ID_ACCOUNT_BALANCE);
            
            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("FID_COND_MRKT_DIV_CODE", "J");
            queryParams.put("FID_INPUT_ACNT_NO", accountNumber);
            queryParams.put("FID_INPUT_PRICE_1", "");
            queryParams.put("FID_INPUT_PRICE_2", "");
            queryParams.put("FID_VOL_CNT", "");

            String fullUrl = url + "?" + buildQueryString(queryParams);
            
            return baseRestClient.get(fullUrl, headers, Map.class);
        } catch (Exception e) {
            log.error("계좌 잔고 조회 중 오류 발생: {}", e.getMessage());
            throw new ApiException("계좌 잔고 조회 실패", e);
        }
    }

    /**
     * 주식 주문 실행
     */
    public Map<String, Object> executeStockOrder(Map<String, String> orderParams) {
        try {
            String url = baseUrl + STOCK_ORDER_ENDPOINT;
            
            Map<String, String> headers = getAuthHeaders();
            headers.put("tr_id", TR_ID_STOCK_ORDER);
            
            return baseRestClient.post(url, headers, orderParams, Map.class);
        } catch (Exception e) {
            log.error("주식 주문 실행 중 오류 발생: {}", e.getMessage());
            throw new ApiException("주식 주문 실행 실패", e);
        }
    }

    /**
     * 주문 상태 조회
     */
    public Map<String, Object> getOrderStatus(String accountNumber, String orderNumber) {
        try {
            String url = baseUrl + ORDER_STATUS_ENDPOINT;
            
            Map<String, String> headers = getAuthHeaders();
            headers.put("tr_id", TR_ID_ORDER_STATUS);
            
            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("FID_COND_MRKT_DIV_CODE", "J");
            queryParams.put("FID_INPUT_ACNT_NO", accountNumber);
            queryParams.put("FID_INPUT_ODNO", orderNumber);

            String fullUrl = url + "?" + buildQueryString(queryParams);
            
            return baseRestClient.get(fullUrl, headers, Map.class);
        } catch (Exception e) {
            log.error("주문 상태 조회 중 오류 발생: {}", e.getMessage());
            throw new ApiException("주문 상태 조회 실패", e);
        }
    }

    /**
     * 인증 헤더 생성
     */
    private Map<String, String> getAuthHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("authorization", "Bearer " + getAccessToken());
        headers.put("appkey", appKey);
        headers.put("appsecret", appSecret);
        headers.put("tr_id", "");
        headers.put("custtype", "P");
        headers.put("hashkey", "");
        headers.put("content-type", "application/json");
        return headers;
    }

    /**
     * 쿼리 스트링 생성
     */
    private String buildQueryString(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return sb.toString();
    }
} 