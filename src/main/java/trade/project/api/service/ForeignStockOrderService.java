package trade.project.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import trade.project.api.client.ForeignStockApiClient;
import trade.project.api.dto.ForeignStockOrderRequest;
import trade.project.api.dto.ForeignStockOrderResponse;
import trade.project.common.dto.ApiResponse;
import trade.project.trading.enums.TopNasdaqStocks;
import trade.project.trading.service.TradingRecordService;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 해외 주식 주문 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ForeignStockOrderService {
    
    private final ForeignStockApiClient foreignStockApiClient;
    private final TradingRecordService tradingRecordService;
    
    /**
     * 해외 주식 주문 실행
     */
    public ApiResponse<ForeignStockOrderResponse> executeOrder(ForeignStockOrderRequest request, HttpServletRequest httpRequest) {
        try {
            log.info("해외 주식 주문 실행 요청: {}", request);
            
            // 주문 요청 데이터 변환
            Map<String, Object> orderRequest = convertToOrderRequest(request);
            
            // API 호출
            ApiResponse<Map<String, Object>> apiResponse = foreignStockApiClient.executeOrder(orderRequest);
            
            if (!apiResponse.isSuccess()) {
                log.error("해외 주식 주문 API 호출 실패: {}", apiResponse.getErrorMessage());
                return ApiResponse.error("FOREIGN_STOCK_ORDER_API_ERROR", apiResponse.getErrorMessage());
            }
            
            // 응답 데이터 변환
            Map<String, Object> data = apiResponse.getData();
            ForeignStockOrderResponse response = convertToOrderResponse(data);
            
            // 매매 기록 저장
            saveTradingRecord(request, response, "성공", httpRequest);
            
            log.info("해외 주식 주문 실행 성공: {}", response.getOrderNumber());
            return ApiResponse.success(response);
            
        } catch (Exception e) {
            log.error("해외 주식 주문 실행 실패: {}, 에러: {}", request.getStockCode(), e.getMessage());
            
            // 매매 기록 저장 (실패)
            saveTradingRecord(request, null, "실패", httpRequest);
            
            return ApiResponse.error("FOREIGN_STOCK_ORDER_ERROR", "해외 주식 주문 실행에 실패했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 해외 주식 주문 상태 조회
     */
    public ApiResponse<ForeignStockOrderResponse> getOrderStatus(String accountNumber, String orderNumber, HttpServletRequest httpRequest) {
        try {
            log.info("해외 주식 주문 상태 조회 요청: 계좌={}, 주문번호={}", accountNumber, orderNumber);
            
            // API 호출
            ApiResponse<Map<String, Object>> apiResponse = foreignStockApiClient.getOrderStatus(accountNumber, orderNumber);
            
            if (!apiResponse.isSuccess()) {
                log.error("해외 주식 주문 상태 API 호출 실패: {}", apiResponse.getErrorMessage());
                return ApiResponse.error("FOREIGN_STOCK_ORDER_STATUS_API_ERROR", apiResponse.getErrorMessage());
            }
            
            // 응답 데이터 변환
            Map<String, Object> data = apiResponse.getData();
            ForeignStockOrderResponse response = convertToOrderResponse(data);
            
            log.info("해외 주식 주문 상태 조회 성공: {}", orderNumber);
            return ApiResponse.success(response);
            
        } catch (Exception e) {
            log.error("해외 주식 주문 상태 조회 실패: {}, 에러: {}", orderNumber, e.getMessage());
            return ApiResponse.error("FOREIGN_STOCK_ORDER_STATUS_ERROR", "해외 주식 주문 상태 조회에 실패했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 애플 매수 테스트 주문
     */
    public ApiResponse<ForeignStockOrderResponse> buyAppleTest(HttpServletRequest httpRequest) {
        ForeignStockOrderRequest request = ForeignStockOrderRequest.builder()
                .accountNumber("FOREIGN123456")
                .stockCode("AAPL")
                .orderType("매수")
                .quantity(10)
                .price(new BigDecimal("150.50"))
                .priceType("지정가")
                .orderCategory("일반")
                .build();
        return executeOrder(request, httpRequest);
    }
    
    /**
     * 애플 매도 테스트 주문
     */
    public ApiResponse<ForeignStockOrderResponse> sellAppleTest(HttpServletRequest httpRequest) {
        ForeignStockOrderRequest request = ForeignStockOrderRequest.builder()
                .accountNumber("FOREIGN123456")
                .stockCode("AAPL")
                .orderType("매도")
                .quantity(5)
                .price(new BigDecimal("155.00"))
                .priceType("지정가")
                .orderCategory("일반")
                .build();
        return executeOrder(request, httpRequest);
    }
    
    /**
     * 마이크로소프트 시장가 매수 테스트
     */
    public ApiResponse<ForeignStockOrderResponse> buyMicrosoftMarketTest(HttpServletRequest httpRequest) {
        ForeignStockOrderRequest request = ForeignStockOrderRequest.builder()
                .accountNumber("FOREIGN123456")
                .stockCode("MSFT")
                .orderType("매수")
                .quantity(15)
                .price(new BigDecimal("300.00"))
                .priceType("시장가")
                .orderCategory("일반")
                .build();
        return executeOrder(request, httpRequest);
    }
    
    /**
     * 주문 요청을 API 요청 형태로 변환
     */
    private Map<String, Object> convertToOrderRequest(ForeignStockOrderRequest request) {
        Map<String, Object> orderRequest = new HashMap<>();
        orderRequest.put("accountNumber", request.getAccountNumber());
        orderRequest.put("stockCode", request.getStockCode());
        orderRequest.put("orderType", request.getOrderType());
        orderRequest.put("quantity", request.getQuantity());
        orderRequest.put("price", request.getPrice());
        orderRequest.put("priceType", request.getPriceType());
        orderRequest.put("orderCategory", request.getOrderCategory());
        orderRequest.put("currency", request.getCurrency());
        orderRequest.put("exchange", request.getExchange());
        return orderRequest;
    }
    
    /**
     * API 응답을 DTO로 변환
     */
    private ForeignStockOrderResponse convertToOrderResponse(Map<String, Object> data) {
        try {
            // 종목 정보 가져오기
            TopNasdaqStocks stock = TopNasdaqStocks.fromStockCode((String) data.get("stockCode"));
            
            return ForeignStockOrderResponse.builder()
                    .orderNumber((String) data.get("orderNumber"))
                    .accountNumber((String) data.get("accountNumber"))
                    .stockCode((String) data.get("stockCode"))
                    .companyName(stock.getCompanyName())
                    .koreanName(stock.getKoreanName())
                    .orderType((String) data.get("orderType"))
                    .quantity(data.get("quantity") != null ? Integer.parseInt(data.get("quantity").toString()) : null)
                    .price(data.get("price") != null ? new BigDecimal(data.get("price").toString()) : null)
                    .priceType((String) data.get("priceType"))
                    .orderCategory((String) data.get("orderCategory"))
                    .orderStatus((String) data.get("orderStatus"))
                    .orderTime(LocalDateTime.now())
                    .executedTime(data.get("executedTime") != null ? LocalDateTime.now() : null)
                    .executedQuantity(data.get("executedQuantity") != null ? Integer.parseInt(data.get("executedQuantity").toString()) : null)
                    .executedPrice(data.get("executedPrice") != null ? new BigDecimal(data.get("executedPrice").toString()) : null)
                    .executedAmount(data.get("executedAmount") != null ? new BigDecimal(data.get("executedAmount").toString()) : null)
                    .currency((String) data.get("currency"))
                    .exchange((String) data.get("exchange"))
                    .build();
                    
        } catch (Exception e) {
            log.error("응답 데이터 변환 실패: {}", e.getMessage());
            throw new RuntimeException("응답 데이터 변환 실패", e);
        }
    }
    
    /**
     * 매매 기록 저장
     */
    private void saveTradingRecord(ForeignStockOrderRequest request, ForeignStockOrderResponse response, String status, HttpServletRequest httpRequest) {
        try {
            String orderNumber = response != null ? response.getOrderNumber() : "UNKNOWN";
            String executedPrice = response != null && response.getExecutedPrice() != null ? 
                    response.getExecutedPrice().toString() : "0";
            String executedQuantity = response != null && response.getExecutedQuantity() != null ? 
                    response.getExecutedQuantity().toString() : "0";
            
            tradingRecordService.saveForeignTradingRecord(request, response, status, httpRequest);
        } catch (Exception e) {
            log.error("매매 기록 저장 실패: {}", e.getMessage());
        }
    }
} 