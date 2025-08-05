package trade.project.api.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import trade.project.api.dto.StockPriceRequest;
import trade.project.api.dto.StockPriceResponse;
import trade.project.api.dto.StockDailyPriceRequest;
import trade.project.api.dto.StockDailyPriceResponse;
import trade.project.api.service.StockPriceService;
import trade.project.common.dto.ApiResponse;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;

@Slf4j
@RestController
@RequestMapping("/api/stock/price")
@RequiredArgsConstructor
public class StockPriceController {

    private final StockPriceService stockPriceService;

    /**
     * 주식 현재가 조회
     */
    @PostMapping("/current")
    public ResponseEntity<ApiResponse<StockPriceResponse>> getCurrentPrice(
            @Valid @RequestBody StockPriceRequest request, HttpServletRequest httpRequest) {
        try {
            log.info("주식 현재가 조회 요청: {}", request);
            
            StockPriceResponse response = stockPriceService.getCurrentPrice(request, httpRequest);
            
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("주식 현재가 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("PRICE_QUERY_ERROR", "주식 현재가 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 주식 현재가 조회 (GET 방식)
     */
    @GetMapping("/current/{stockCode}")
    public ResponseEntity<ApiResponse<StockPriceResponse>> getCurrentPriceByGet(
            @PathVariable String stockCode, HttpServletRequest httpRequest) {
        try {
            log.info("주식 현재가 조회 요청 (GET): {}", stockCode);
            
            StockPriceRequest request = StockPriceRequest.builder()
                    .stockCode(stockCode)
                    .build();
            
            StockPriceResponse response = stockPriceService.getCurrentPrice(request, httpRequest);
            
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("주식 현재가 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("PRICE_QUERY_ERROR", "주식 현재가 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 삼성전자 현재가 조회 테스트
     */
    @GetMapping("/test/samsung")
    public ResponseEntity<ApiResponse<StockPriceResponse>> testSamsungPrice(HttpServletRequest httpRequest) {
        try {
            log.info("삼성전자 현재가 조회 테스트");
            
            StockPriceRequest request = StockPriceRequest.builder()
                    .stockCode("005930")
                    .build();
            
            StockPriceResponse response = stockPriceService.getCurrentPrice(request, httpRequest);
            
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("삼성전자 현재가 조회 테스트 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("TEST_PRICE_ERROR", "삼성전자 현재가 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * SK하이닉스 현재가 조회 테스트
     */
    @GetMapping("/test/skhynix")
    public ResponseEntity<ApiResponse<StockPriceResponse>> testSKHynixPrice(HttpServletRequest httpRequest) {
        try {
            log.info("SK하이닉스 현재가 조회 테스트");
            
            StockPriceRequest request = StockPriceRequest.builder()
                    .stockCode("000660")
                    .build();
            
            StockPriceResponse response = stockPriceService.getCurrentPrice(request, httpRequest);
            
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("SK하이닉스 현재가 조회 테스트 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("TEST_PRICE_ERROR", "SK하이닉스 현재가 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * NAVER 현재가 조회 테스트
     */
    @GetMapping("/test/naver")
    public ResponseEntity<ApiResponse<StockPriceResponse>> testNaverPrice(HttpServletRequest httpRequest) {
        try {
            log.info("NAVER 현재가 조회 테스트");
            
            StockPriceRequest request = StockPriceRequest.builder()
                    .stockCode("035420")
                    .build();
            
            StockPriceResponse response = stockPriceService.getCurrentPrice(request, httpRequest);
            
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("NAVER 현재가 조회 테스트 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("TEST_PRICE_ERROR", "NAVER 현재가 조회 실패: " + e.getMessage()));
        }
    }
} 