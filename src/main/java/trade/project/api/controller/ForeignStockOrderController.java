package trade.project.api.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import trade.project.api.dto.ForeignStockOrderRequest;
import trade.project.api.dto.ForeignStockOrderResponse;
import trade.project.api.dto.OrderStatusRequest;
import trade.project.common.dto.ApiResponse;
import trade.project.api.service.ForeignStockOrderService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/**
 * 해외 주식 주문 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/foreign-stock/order")
@RequiredArgsConstructor
public class ForeignStockOrderController {
    
    private final ForeignStockOrderService foreignStockOrderService;
    
    /**
     * 해외 주식 주문 실행
     */
    @PostMapping("/execute")
    public ResponseEntity<ApiResponse<ForeignStockOrderResponse>> executeOrder(
            @Valid @RequestBody ForeignStockOrderRequest request,
            HttpServletRequest httpRequest) {
        
        log.info("해외 주식 주문 실행 요청: {}", request.getStockCode());
        ApiResponse<ForeignStockOrderResponse> response = foreignStockOrderService.executeOrder(request, httpRequest);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 해외 주식 주문 상태 조회
     */
    @PostMapping("/status")
    public ResponseEntity<ApiResponse<ForeignStockOrderResponse>> getOrderStatus(
            @RequestBody OrderStatusRequest request,
            HttpServletRequest httpRequest) {
        
        log.info("해외 주식 주문 상태 조회 요청: 계좌={}, 주문번호={}", request.getAccountNumber(), request.getOrderNumber());
        ApiResponse<ForeignStockOrderResponse> response = foreignStockOrderService.getOrderStatus(
                request.getAccountNumber(), request.getOrderNumber(), httpRequest);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 애플 매수 테스트 주문
     */
    @PostMapping("/test/buy-apple")
    public ResponseEntity<ApiResponse<ForeignStockOrderResponse>> buyAppleTest(HttpServletRequest httpRequest) {
        log.info("애플 매수 테스트 주문 요청");
        ApiResponse<ForeignStockOrderResponse> response = foreignStockOrderService.buyAppleTest(httpRequest);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 애플 매도 테스트 주문
     */
    @PostMapping("/test/sell-apple")
    public ResponseEntity<ApiResponse<ForeignStockOrderResponse>> sellAppleTest(HttpServletRequest httpRequest) {
        log.info("애플 매도 테스트 주문 요청");
        ApiResponse<ForeignStockOrderResponse> response = foreignStockOrderService.sellAppleTest(httpRequest);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 마이크로소프트 시장가 매수 테스트
     */
    @PostMapping("/test/buy-microsoft-market")
    public ResponseEntity<ApiResponse<ForeignStockOrderResponse>> buyMicrosoftMarketTest(HttpServletRequest httpRequest) {
        log.info("마이크로소프트 시장가 매수 테스트 주문 요청");
        ApiResponse<ForeignStockOrderResponse> response = foreignStockOrderService.buyMicrosoftMarketTest(httpRequest);
        return ResponseEntity.ok(response);
    }
} 