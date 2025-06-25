package trade.project.backtest.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import trade.project.backtest.dto.BackTestResult;
import trade.project.backtest.service.BacktestService;
import trade.project.common.dto.ApiResponse;
import trade.project.backtest.dto.BackTestRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/backtest")
@RequiredArgsConstructor
public class BacktestController {
    
    private final BacktestService backtestService;
    
    /**
     * 백트래킹을 실행합니다.
     * @param request 백트래킹 요청
     * @return 백트래킹 결과
     */
    @PostMapping("/run")
    public ApiResponse<BackTestResult> runBacktest(@RequestBody BackTestRequest request) {
        try {
            log.info("백트래킹 요청: {}", request);
            
            // 요청 유효성 검사
            backtestService.validateRequest(request);
            
            // 백트래킹 실행
            BackTestResult result = backtestService.runBacktest(request);
            
            return ApiResponse.success(result);
            
        } catch (IllegalArgumentException e) {
            log.warn("백트래킹 요청 유효성 검사 실패: {}", e.getMessage());
            return ApiResponse.error("VALIDATION_ERROR", e.getMessage());
        } catch (Exception e) {
            log.error("백트래킹 실행 중 오류 발생: {}", e.getMessage(), e);
            return ApiResponse.error("BACKTEST_ERROR", e.getMessage());
        }
    }
    
    /**
     * 간단한 백트래킹을 실행합니다 (GET 요청).
     * @param stockCode 주식 코드
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @param strategy 전략
     * @param initialCapital 초기 자본금
     * @return 백트래킹 결과
     */
    @GetMapping("/run")
    public ApiResponse<BackTestResult> runSimpleBacktest(
            @RequestParam String stockCode,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(defaultValue = "SMA") String strategy,
            @RequestParam(defaultValue = "10000000") BigDecimal initialCapital) {
        
        try {
            BackTestRequest request = BackTestRequest.builder()
                    .stockCode(stockCode)
                    .startDate(startDate)
                    .endDate(endDate)
                    .strategy(strategy)
                    .initialCapital(initialCapital)
                    .build();
            
            // 기본값 설정
            BackTestRequest defaultRequest = BackTestRequest.getDefault();
            if (request.getCommission() == null) request.setCommission(defaultRequest.getCommission());
            if (request.getShortPeriod() == null) request.setShortPeriod(defaultRequest.getShortPeriod());
            if (request.getLongPeriod() == null) request.setLongPeriod(defaultRequest.getLongPeriod());
            if (request.getRsiPeriod() == null) request.setRsiPeriod(defaultRequest.getRsiPeriod());
            if (request.getRsiOverbought() == null) request.setRsiOverbought(defaultRequest.getRsiOverbought());
            if (request.getRsiOversold() == null) request.setRsiOversold(defaultRequest.getRsiOversold());
            if (request.getMacdFastPeriod() == null) request.setMacdFastPeriod(defaultRequest.getMacdFastPeriod());
            if (request.getMacdSlowPeriod() == null) request.setMacdSlowPeriod(defaultRequest.getMacdSlowPeriod());
            if (request.getMacdSignalPeriod() == null) request.setMacdSignalPeriod(defaultRequest.getMacdSignalPeriod());
            if (request.getStopLoss() == null) request.setStopLoss(defaultRequest.getStopLoss());
            if (request.getTakeProfit() == null) request.setTakeProfit(defaultRequest.getTakeProfit());
            if (request.getMaxPositionSize() == null) request.setMaxPositionSize(defaultRequest.getMaxPositionSize());
            if (request.getMinTradeAmount() == null) request.setMinTradeAmount(defaultRequest.getMinTradeAmount());
            if (request.getReinvestDividends() == null) request.setReinvestDividends(defaultRequest.getReinvestDividends());
            if (request.getIncludeTax() == null) request.setIncludeTax(defaultRequest.getIncludeTax());
            
            return runBacktest(request);
            
        } catch (Exception e) {
            log.error("간단한 백트래킹 실행 중 오류 발생: {}", e.getMessage(), e);
            return ApiResponse.error("BACKTEST_ERROR", e.getMessage());
        }
    }
    
    /**
     * 사용 가능한 전략 목록을 반환합니다.
     * @return 전략 목록
     */
    @GetMapping("/strategies")
    public ApiResponse<List<Map<String, String>>> getStrategies() {
        try {
            List<Map<String, String>> strategies = backtestService.getAvailableStrategies();
            return ApiResponse.success(strategies);
        } catch (Exception e) {
            log.error("전략 목록 조회 중 오류 발생: {}", e.getMessage(), e);
            return ApiResponse.error("STRATEGY_ERROR", e.getMessage());
        }
    }
    
    /**
     * 백트래킹 요청의 유효성을 검사합니다.
     * @param request 백트래킹 요청
     * @return 검사 결과
     */
    @PostMapping("/validate")
    public ApiResponse<String> validateRequest(@RequestBody BackTestRequest request) {
        try {
            backtestService.validateRequest(request);
            return ApiResponse.success("요청이 유효합니다.");
        } catch (IllegalArgumentException e) {
            return ApiResponse.error("VALIDATION_ERROR", e.getMessage());
        } catch (Exception e) {
            log.error("요청 유효성 검사 중 오류 발생: {}", e.getMessage(), e);
            return ApiResponse.error("VALIDATION_ERROR", e.getMessage());
        }
    }
    
    /**
     * 백트래킹 기본 설정을 반환합니다.
     * @return 기본 설정
     */
    @GetMapping("/default-config")
    public ApiResponse<BackTestRequest> getDefaultConfig() {
        try {
            BackTestRequest defaultConfig = BackTestRequest.getDefault();
            return ApiResponse.success(defaultConfig);
        } catch (Exception e) {
            log.error("기본 설정 조회 중 오류 발생: {}", e.getMessage(), e);
            return ApiResponse.error("CONFIG_ERROR", e.getMessage());
        }
    }
    
    /**
     * 백트래킹 상태를 확인합니다.
     * @return 상태 정보
     */
    @GetMapping("/health")
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.success(Map.of(
                "status", "healthy",
                "message", "백트래킹 서비스가 정상적으로 동작 중입니다.",
                "availableStrategies", String.valueOf(backtestService.getAvailableStrategies().size())
        ));
    }
} 