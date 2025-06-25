package trade.project.api.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import trade.project.api.client.KisApiClient;
import trade.project.common.dto.ApiResponse;
import trade.project.common.exception.ApiException;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class ApiTestController {

    private final KisApiClient kisApiClient;

    /**
     * 토큰 발급 테스트
     */
    @GetMapping("/token")
    public ApiResponse<String> getToken() {
        try {
            String token = kisApiClient.getAccessToken();
            return ApiResponse.success(token);
        } catch (Exception e) {
            log.error("토큰 발급 테스트 실패: {}", e.getMessage());
            return ApiResponse.error("TOKEN_ERROR", e.getMessage());
        }
    }

    /**
     * 주식 현재가 조회 테스트
     */
    @GetMapping("/stock/price/{stockCode}")
    public ApiResponse<Map<String, Object>> getStockPrice(@PathVariable String stockCode) {
        try {
            Map<String, Object> result = kisApiClient.getStockPrice(stockCode);
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("주식 현재가 조회 테스트 실패: {}", e.getMessage());
            return ApiResponse.error("STOCK_PRICE_ERROR", e.getMessage());
        }
    }

    /**
     * 주식 일자별 시세 조회 테스트
     */
    @GetMapping("/stock/daily/{stockCode}")
    public ApiResponse<Map<String, Object>> getStockDailyPrice(
            @PathVariable String stockCode,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        try {
            Map<String, Object> result = kisApiClient.getStockDailyPrice(stockCode, startDate, endDate);
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("주식 일자별 시세 조회 테스트 실패: {}", e.getMessage());
            return ApiResponse.error("STOCK_DAILY_ERROR", e.getMessage());
        }
    }

    /**
     * 주식 체결 내역 조회 테스트
     */
    @GetMapping("/stock/history/{stockCode}")
    public ApiResponse<Map<String, Object>> getStockTradeHistory(
            @PathVariable String stockCode,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        try {
            Map<String, Object> result = kisApiClient.getStockTradeHistory(stockCode, startDate, endDate);
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("주식 체결 내역 조회 테스트 실패: {}", e.getMessage());
            return ApiResponse.error("STOCK_HISTORY_ERROR", e.getMessage());
        }
    }

    /**
     * 계좌 잔고 조회 테스트
     */
    @GetMapping("/account/balance/{accountNumber}")
    public ApiResponse<Map<String, Object>> getAccountBalance(@PathVariable String accountNumber) {
        try {
            Map<String, Object> result = kisApiClient.getAccountBalance(accountNumber);
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("계좌 잔고 조회 테스트 실패: {}", e.getMessage());
            return ApiResponse.error("ACCOUNT_BALANCE_ERROR", e.getMessage());
        }
    }

    /**
     * API 상태 확인
     */
    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.success("API 서버가 정상적으로 동작 중입니다.");
    }
} 