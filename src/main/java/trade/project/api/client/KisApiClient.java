package trade.project.api.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import trade.project.common.client.BaseRestClient;
import trade.project.common.dto.ApiResponse;
import trade.project.common.exception.ApiException;

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

    @Value("${kis.api.access-token}")
    private String accessToken;

    /**
     * 한국투자증권 API 인증 토큰 발급
     */
    public String getAccessToken() {
        try {
            String url = baseUrl + "/oauth2/tokenP";
            
            Map<String, String> headers = new HashMap<>();
            headers.put("content-type", "application/json");
            
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("grant_type", "client_credentials");
            requestBody.put("appkey", appKey);
            requestBody.put("appsecret", appSecret);

            Map<String, Object> response = baseRestClient.post(url, headers, requestBody, Map.class);
            
            if (response.containsKey("access_token")) {
                return (String) response.get("access_token");
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
            String url = baseUrl + "/uapi/domestic-stock/v1/quotations/inquire-price";
            
            Map<String, String> headers = getAuthHeaders();
            headers.put("tr_id", "FHKST01010100");
            
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
            String url = baseUrl + "/uapi/domestic-stock/v1/quotations/inquire-daily-price";
            
            Map<String, String> headers = getAuthHeaders();
            headers.put("tr_id", "FHKST01010400");
            
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
            String url = baseUrl + "/uapi/domestic-stock/v1/trading/inquire-daily-ccld";
            
            Map<String, String> headers = getAuthHeaders();
            headers.put("tr_id", "TTTC8001R");
            
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
            String url = baseUrl + "/uapi/domestic-stock/v1/trading/inquire-balance";
            
            Map<String, String> headers = getAuthHeaders();
            headers.put("tr_id", "TTTC8434R");
            
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
     * 인증 헤더 생성
     */
    private Map<String, String> getAuthHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("authorization", "Bearer " + accessToken);
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