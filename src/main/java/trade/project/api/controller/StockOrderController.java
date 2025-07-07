package trade.project.api.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import trade.project.api.dto.StockOrderRequest;
import trade.project.api.dto.StockOrderResponse;
import trade.project.api.dto.OrderStatusRequest;
import trade.project.api.service.StockOrderService;
import trade.project.common.dto.ApiResponse;

import jakarta.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/stock/order")
@RequiredArgsConstructor
public class StockOrderController {

    private final StockOrderService stockOrderService;

    /**
     * 주식 주문 실행
     */
    @PostMapping("/execute")
    public ResponseEntity<ApiResponse<StockOrderResponse>> executeOrder(
            @Valid @RequestBody StockOrderRequest request) {
        try {
            log.info("주식 주문 요청: {}", request);
            
            StockOrderResponse response = stockOrderService.executeOrder(request);
            
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("주식 주문 실행 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("ORDER_EXECUTION_ERROR", "주식 주문 실행 실패: " + e.getMessage()));
        }
    }

    /**
     * 주문 상태 조회
     */
    @PostMapping("/status")
    public ResponseEntity<ApiResponse<StockOrderResponse>> getOrderStatus(
            @Valid @RequestBody OrderStatusRequest request) {
        try {
            log.info("주문 상태 조회 요청: {}", request);
            
            StockOrderResponse response = stockOrderService.getOrderStatus(request);
            
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("주문 상태 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("ORDER_STATUS_ERROR", "주문 상태 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 주식 주문 테스트 (매수)
     */
    @PostMapping("/test/buy")
    public ResponseEntity<ApiResponse<StockOrderResponse>> testBuyOrder() {
        try {
            StockOrderRequest request = StockOrderRequest.builder()
                    .accountNumber("1234567890")
                    .stockCode("005930") // 삼성전자
                    .orderType("매수")
                    .quantity(10)
                    .price(70000)
                    .priceType("지정가")
                    .orderCategory("일반")
                    .build();

            log.info("매수 테스트 주문 요청: {}", request);
            
            StockOrderResponse response = stockOrderService.executeOrder(request);
            
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("매수 테스트 주문 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("TEST_BUY_ERROR", "매수 테스트 주문 실패: " + e.getMessage()));
        }
    }

    /**
     * 주식 주문 테스트 (매도)
     */
    @PostMapping("/test/sell")
    public ResponseEntity<ApiResponse<StockOrderResponse>> testSellOrder() {
        try {
            StockOrderRequest request = StockOrderRequest.builder()
                    .accountNumber("1234567890")
                    .stockCode("005930") // 삼성전자
                    .orderType("매도")
                    .quantity(5)
                    .price(72000)
                    .priceType("지정가")
                    .orderCategory("일반")
                    .build();

            log.info("매도 테스트 주문 요청: {}", request);
            
            StockOrderResponse response = stockOrderService.executeOrder(request);
            
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("매도 테스트 주문 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("TEST_SELL_ERROR", "매도 테스트 주문 실패: " + e.getMessage()));
        }
    }

    /**
     * 주식 주문 테스트 (시장가 매수)
     */
    @PostMapping("/test/market-buy")
    public ResponseEntity<ApiResponse<StockOrderResponse>> testMarketBuyOrder() {
        try {
            StockOrderRequest request = StockOrderRequest.builder()
                    .accountNumber("1234567890")
                    .stockCode("005930") // 삼성전자
                    .orderType("매수")
                    .quantity(10)
                    .price(0) // 시장가는 0
                    .priceType("시장가")
                    .orderCategory("일반")
                    .build();

            log.info("시장가 매수 테스트 주문 요청: {}", request);
            
            StockOrderResponse response = stockOrderService.executeOrder(request);
            
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("시장가 매수 테스트 주문 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("TEST_MARKET_BUY_ERROR", "시장가 매수 테스트 주문 실패: " + e.getMessage()));
        }
    }
} 