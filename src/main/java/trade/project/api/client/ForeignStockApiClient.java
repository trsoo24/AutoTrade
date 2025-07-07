package trade.project.api.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import trade.project.common.dto.ApiResponse;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 해외 주식 API 클라이언트
 * 나스닥, S&P 500 등 해외 주식 시장 API 호출
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ForeignStockApiClient {

    // 해외 주식 API 기본 URL (실제 API로 교체 필요)
    private static final String FOREIGN_STOCK_BASE_URL = "https://api.foreign-stock.com";
    
    /**
     * 해외 주식 현재가 조회
     */
    public ApiResponse<Map<String, Object>> getCurrentPrice(String stockCode) {
        try {
            log.info("해외 주식 현재가 조회 요청: {}", stockCode);
            
            String url = FOREIGN_STOCK_BASE_URL + "/v1/stock/price/" + stockCode;
            
            // 실제 API 호출 (현재는 모의 데이터 반환)
            Map<String, Object> mockData = createMockPriceData(stockCode);
            
            log.info("해외 주식 현재가 조회 성공: {}", stockCode);
            return ApiResponse.success(mockData);
            
        } catch (Exception e) {
            log.error("해외 주식 현재가 조회 실패: {}, 에러: {}", stockCode, e.getMessage());
            return ApiResponse.error("FOREIGN_STOCK_PRICE_ERROR", "해외 주식 현재가 조회에 실패했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 해외 주식 일자별 시세 조회
     */
    public ApiResponse<Map<String, Object>> getDailyPrice(String stockCode, String startDate, String endDate) {
        try {
            log.info("해외 주식 일자별 시세 조회 요청: {}, 기간: {} ~ {}", stockCode, startDate, endDate);
            
            String url = FOREIGN_STOCK_BASE_URL + "/v1/stock/daily/" + stockCode;
            
            // 실제 API 호출 (현재는 모의 데이터 반환)
            Map<String, Object> mockData = createMockDailyData(stockCode, startDate, endDate);
            
            log.info("해외 주식 일자별 시세 조회 성공: {}", stockCode);
            return ApiResponse.success(mockData);
            
        } catch (Exception e) {
            log.error("해외 주식 일자별 시세 조회 실패: {}, 에러: {}", stockCode, e.getMessage());
            return ApiResponse.error("FOREIGN_STOCK_DAILY_ERROR", "해외 주식 일자별 시세 조회에 실패했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 해외 주식 주문 실행
     */
    public ApiResponse<Map<String, Object>> executeOrder(Map<String, Object> orderRequest) {
        try {
            log.info("해외 주식 주문 실행 요청: {}", orderRequest);
            
            String url = FOREIGN_STOCK_BASE_URL + "/v1/stock/order";
            
            // 실제 API 호출 (현재는 모의 데이터 반환)
            Map<String, Object> mockData = createMockOrderData(orderRequest);
            
            log.info("해외 주식 주문 실행 성공: {}", orderRequest.get("stockCode"));
            return ApiResponse.success(mockData);
            
        } catch (Exception e) {
            log.error("해외 주식 주문 실행 실패: {}, 에러: {}", orderRequest, e.getMessage());
            return ApiResponse.error("FOREIGN_STOCK_ORDER_ERROR", "해외 주식 주문 실행에 실패했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 해외 주식 주문 상태 조회
     */
    public ApiResponse<Map<String, Object>> getOrderStatus(String accountNumber, String orderNumber) {
        try {
            log.info("해외 주식 주문 상태 조회 요청: 계좌={}, 주문번호={}", accountNumber, orderNumber);
            
            String url = FOREIGN_STOCK_BASE_URL + "/v1/stock/order/status";
            
            // 실제 API 호출 (현재는 모의 데이터 반환)
            Map<String, Object> mockData = createMockOrderStatusData(accountNumber, orderNumber);
            
            log.info("해외 주식 주문 상태 조회 성공: {}", orderNumber);
            return ApiResponse.success(mockData);
            
        } catch (Exception e) {
            log.error("해외 주식 주문 상태 조회 실패: {}, 에러: {}", orderNumber, e.getMessage());
            return ApiResponse.error("FOREIGN_STOCK_ORDER_STATUS_ERROR", "해외 주식 주문 상태 조회에 실패했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 해외 계좌 잔고 조회
     */
    public ApiResponse<Map<String, Object>> getAccountBalance(String accountNumber) {
        try {
            log.info("해외 계좌 잔고 조회 요청: {}", accountNumber);
            
            String url = FOREIGN_STOCK_BASE_URL + "/v1/account/balance/" + accountNumber;
            
            // 실제 API 호출 (현재는 모의 데이터 반환)
            Map<String, Object> mockData = createMockBalanceData(accountNumber);
            
            log.info("해외 계좌 잔고 조회 성공: {}", accountNumber);
            return ApiResponse.success(mockData);
            
        } catch (Exception e) {
            log.error("해외 계좌 잔고 조회 실패: {}, 에러: {}", accountNumber, e.getMessage());
            return ApiResponse.error("FOREIGN_ACCOUNT_BALANCE_ERROR", "해외 계좌 잔고 조회에 실패했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 모의 현재가 데이터 생성
     */
    private Map<String, Object> createMockPriceData(String stockCode) {
        Map<String, Object> data = new HashMap<>();
        data.put("stockCode", stockCode);
        data.put("currentPrice", new BigDecimal("150.50"));
        data.put("changeAmount", new BigDecimal("2.30"));
        data.put("changeRate", new BigDecimal("1.55"));
        data.put("openPrice", new BigDecimal("148.20"));
        data.put("highPrice", new BigDecimal("152.10"));
        data.put("lowPrice", new BigDecimal("147.80"));
        data.put("volume", 15000000L);
        data.put("marketCap", new BigDecimal("2500000000000"));
        data.put("currency", "USD");
        data.put("exchange", "NASDAQ");
        data.put("timestamp", System.currentTimeMillis());
        return data;
    }
    
    /**
     * 모의 일자별 시세 데이터 생성
     */
    private Map<String, Object> createMockDailyData(String stockCode, String startDate, String endDate) {
        Map<String, Object> data = new HashMap<>();
        data.put("stockCode", stockCode);
        data.put("startDate", startDate);
        data.put("endDate", endDate);
        data.put("currency", "USD");
        data.put("exchange", "NASDAQ");
        
        // 모의 일별 데이터
        Map<String, Object>[] dailyData = new Map[7];
        for (int i = 0; i < 7; i++) {
            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", "2023-12-" + String.format("%02d", i + 1));
            dayData.put("openPrice", new BigDecimal("150.00"));
            dayData.put("closePrice", new BigDecimal("152.50"));
            dayData.put("highPrice", new BigDecimal("153.20"));
            dayData.put("lowPrice", new BigDecimal("149.80"));
            dayData.put("volume", 12000000L);
            dailyData[i] = dayData;
        }
        data.put("dailyData", dailyData);
        
        return data;
    }
    
    /**
     * 모의 주문 데이터 생성
     */
    private Map<String, Object> createMockOrderData(Map<String, Object> orderRequest) {
        Map<String, Object> data = new HashMap<>();
        data.put("orderNumber", "FOREIGN_ORDER_" + System.currentTimeMillis());
        data.put("accountNumber", orderRequest.get("accountNumber"));
        data.put("stockCode", orderRequest.get("stockCode"));
        data.put("orderType", orderRequest.get("orderType"));
        data.put("quantity", orderRequest.get("quantity"));
        data.put("price", orderRequest.get("price"));
        data.put("priceType", orderRequest.get("priceType"));
        data.put("orderStatus", "접수완료");
        data.put("orderTime", System.currentTimeMillis());
        data.put("currency", "USD");
        data.put("exchange", "NASDAQ");
        return data;
    }
    
    /**
     * 모의 주문 상태 데이터 생성
     */
    private Map<String, Object> createMockOrderStatusData(String accountNumber, String orderNumber) {
        Map<String, Object> data = new HashMap<>();
        data.put("orderNumber", orderNumber);
        data.put("accountNumber", accountNumber);
        data.put("orderStatus", "체결완료");
        data.put("executedQuantity", 10);
        data.put("executedPrice", new BigDecimal("150.50"));
        data.put("executedAmount", new BigDecimal("1505.00"));
        data.put("orderTime", System.currentTimeMillis() - 3600000);
        data.put("executedTime", System.currentTimeMillis());
        data.put("currency", "USD");
        data.put("exchange", "NASDAQ");
        return data;
    }
    
    /**
     * 모의 잔고 데이터 생성
     */
    private Map<String, Object> createMockBalanceData(String accountNumber) {
        Map<String, Object> data = new HashMap<>();
        data.put("accountNumber", accountNumber);
        data.put("totalBalance", new BigDecimal("50000.00"));
        data.put("availableBalance", new BigDecimal("30000.00"));
        data.put("investedAmount", new BigDecimal("20000.00"));
        data.put("currency", "USD");
        data.put("exchange", "NASDAQ");
        data.put("lastUpdateTime", System.currentTimeMillis());
        return data;
    }
} 