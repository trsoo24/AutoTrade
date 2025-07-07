package trade.project.api.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import trade.project.api.dto.ForeignStockPriceRequest;
import trade.project.api.dto.ForeignStockPriceResponse;
import trade.project.common.dto.ApiResponse;
import trade.project.api.service.ForeignStockPriceService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/**
 * 해외 주식 시세 조회 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/foreign-stock/price")
@RequiredArgsConstructor
public class ForeignStockPriceController {
    
    private final ForeignStockPriceService foreignStockPriceService;
    
    /**
     * 해외 주식 현재가 조회 (POST)
     */
    @PostMapping("/current")
    public ResponseEntity<ApiResponse<ForeignStockPriceResponse>> getCurrentPrice(
            @Valid @RequestBody ForeignStockPriceRequest request,
            HttpServletRequest httpRequest) {
        
        log.info("해외 주식 현재가 조회 요청: {}", request.getStockCode());
        ApiResponse<ForeignStockPriceResponse> response = foreignStockPriceService.getCurrentPrice(request, httpRequest);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 해외 주식 현재가 조회 (GET)
     */
    @GetMapping("/current/{stockCode}")
    public ResponseEntity<ApiResponse<ForeignStockPriceResponse>> getCurrentPriceByCode(
            @PathVariable String stockCode,
            HttpServletRequest httpRequest) {
        
        log.info("해외 주식 현재가 조회 요청 (GET): {}", stockCode);
        ApiResponse<ForeignStockPriceResponse> response = foreignStockPriceService.getCurrentPriceByCode(stockCode, httpRequest);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 해외 주식 일자별 시세 조회
     */
    @PostMapping("/daily")
    public ResponseEntity<ApiResponse<ForeignStockPriceResponse>> getDailyPrice(
            @Valid @RequestBody ForeignStockPriceRequest request,
            HttpServletRequest httpRequest) {
        
        log.info("해외 주식 일자별 시세 조회 요청: {}, 기간: {} ~ {}", 
                request.getStockCode(), request.getStartDate(), request.getEndDate());
        ApiResponse<ForeignStockPriceResponse> response = foreignStockPriceService.getDailyPrice(request, httpRequest);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 애플 현재가 조회 테스트
     */
    @GetMapping("/test/apple")
    public ResponseEntity<ApiResponse<ForeignStockPriceResponse>> getAppleCurrentPrice(HttpServletRequest httpRequest) {
        log.info("애플 현재가 조회 테스트 요청");
        ApiResponse<ForeignStockPriceResponse> response = foreignStockPriceService.getAppleCurrentPrice(httpRequest);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 마이크로소프트 현재가 조회 테스트
     */
    @GetMapping("/test/microsoft")
    public ResponseEntity<ApiResponse<ForeignStockPriceResponse>> getMicrosoftCurrentPrice(HttpServletRequest httpRequest) {
        log.info("마이크로소프트 현재가 조회 테스트 요청");
        ApiResponse<ForeignStockPriceResponse> response = foreignStockPriceService.getMicrosoftCurrentPrice(httpRequest);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 구글 현재가 조회 테스트
     */
    @GetMapping("/test/google")
    public ResponseEntity<ApiResponse<ForeignStockPriceResponse>> getGoogleCurrentPrice(HttpServletRequest httpRequest) {
        log.info("구글 현재가 조회 테스트 요청");
        ApiResponse<ForeignStockPriceResponse> response = foreignStockPriceService.getGoogleCurrentPrice(httpRequest);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 애플 일자별 시세 조회 테스트
     */
    @GetMapping("/test/apple/daily")
    public ResponseEntity<ApiResponse<ForeignStockPriceResponse>> getAppleDailyPrice(HttpServletRequest httpRequest) {
        log.info("애플 일자별 시세 조회 테스트 요청");
        ApiResponse<ForeignStockPriceResponse> response = foreignStockPriceService.getAppleDailyPrice(httpRequest);
        return ResponseEntity.ok(response);
    }
} 