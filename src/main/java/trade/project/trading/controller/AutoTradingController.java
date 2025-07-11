package trade.project.trading.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import trade.project.common.dto.ApiResponse;
import trade.project.trading.dto.AutoTradingStrategy;
import trade.project.trading.engine.AutoTradingEngine;
import trade.project.trading.enums.TopKospiStocks;
import trade.project.trading.enums.TopNasdaqStocks;
import trade.project.trading.enums.TradingSchedule;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/auto-trading")
@RequiredArgsConstructor
public class AutoTradingController {

    private final AutoTradingEngine autoTradingEngine;

    /**
     * 자동매매 엔진 초기화
     */
    @PostMapping("/initialize")
    public ResponseEntity<ApiResponse<String>> initializeEngine() {
        try {
            log.info("자동매매 엔진 초기화 요청");
            boolean domesticStarted = autoTradingEngine.initializeDomestic();
            boolean foreignStarted = autoTradingEngine.initializeForeign();
            
            if (domesticStarted && foreignStarted) {
                return ResponseEntity.ok(ApiResponse.success("국내/해외 자동매매 엔진이 정상적으로 초기화되었습니다."));
            } else if (domesticStarted) {
                return ResponseEntity.ok(ApiResponse.success("국내 자동매매 엔진만 초기화되었습니다."));
            } else if (foreignStarted) {
                return ResponseEntity.ok(ApiResponse.success("해외 자동매매 엔진만 초기화되었습니다."));
            } else {
                return ResponseEntity.ok(ApiResponse.success("자동매매 엔진이 이미 실행 중입니다."));
            }
        } catch (Exception e) {
            log.error("자동매매 엔진 초기화 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("ENGINE_INIT_ERROR", "자동매매 엔진 초기화 실패: " + e.getMessage()));
        }
    }

    /**
     * 자동매매 엔진 종료
     */
    @PostMapping("/shutdown")
    public ResponseEntity<ApiResponse<String>> shutdownEngine() {
        try {
            log.info("자동매매 엔진 종료 요청");
            boolean domesticStopped = autoTradingEngine.shutdownDomestic();
            boolean foreignStopped = autoTradingEngine.shutdownForeign();
            
            if (domesticStopped && foreignStopped) {
                return ResponseEntity.ok(ApiResponse.success("국내/해외 자동매매 엔진이 정상적으로 종료되었습니다."));
            } else if (domesticStopped) {
                return ResponseEntity.ok(ApiResponse.success("국내 자동매매 엔진만 종료되었습니다."));
            } else if (foreignStopped) {
                return ResponseEntity.ok(ApiResponse.success("해외 자동매매 엔진만 종료되었습니다."));
            } else {
                return ResponseEntity.ok(ApiResponse.success("자동매매 엔진이 이미 종료된 상태입니다."));
            }
        } catch (Exception e) {
            log.error("자동매매 엔진 종료 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("ENGINE_SHUTDOWN_ERROR", "자동매매 엔진 종료 실패: " + e.getMessage()));
        }
    }

    /**
     * 전략 등록
     */
    @PostMapping("/strategies")
    public ResponseEntity<ApiResponse<String>> registerStrategy(@RequestBody AutoTradingStrategy strategy) {
        try {
            log.info("전략 등록 요청: {}", strategy.getStrategyId());
            
            autoTradingEngine.registerStrategy(strategy);
            
            return ResponseEntity.ok(ApiResponse.success("전략이 성공적으로 등록되었습니다."));
        } catch (Exception e) {
            log.error("전략 등록 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("STRATEGY_REGISTER_ERROR", "전략 등록 실패: " + e.getMessage()));
        }
    }

    /**
     * 전략 제거
     */
    @DeleteMapping("/strategies/{strategyId}")
    public ResponseEntity<ApiResponse<String>> unregisterStrategy(@PathVariable String strategyId) {
        try {
            log.info("전략 제거 요청: {}", strategyId);
            
            autoTradingEngine.unregisterStrategy(strategyId);
            
            return ResponseEntity.ok(ApiResponse.success("전략이 성공적으로 제거되었습니다."));
        } catch (Exception e) {
            log.error("전략 제거 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("STRATEGY_UNREGISTER_ERROR", "전략 제거 실패: " + e.getMessage()));
        }
    }

    /**
     * 기본 전략 등록 (삼성전자)
     */
    @PostMapping("/strategies/default/samsung")
    public ResponseEntity<ApiResponse<String>> registerDefaultSamsungStrategy() {
        try {
            log.info("삼성전자 기본 전략 등록 요청");
            
            AutoTradingStrategy strategy = AutoTradingStrategy.createDefaultStrategy();
            autoTradingEngine.registerStrategy(strategy);
            
            return ResponseEntity.ok(ApiResponse.success("삼성전자 기본 전략이 등록되었습니다."));
        } catch (Exception e) {
            log.error("삼성전자 기본 전략 등록 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("DEFAULT_STRATEGY_ERROR", "기본 전략 등록 실패: " + e.getMessage()));
        }
    }

    /**
     * 보수적 전략 등록
     */
    @PostMapping("/strategies/conservative/{stockCode}")
    public ResponseEntity<ApiResponse<String>> registerConservativeStrategy(@PathVariable String stockCode) {
        try {
            log.info("보수적 전략 등록 요청: {}", stockCode);
            
            TopKospiStocks stock = TopKospiStocks.findByStockCode(stockCode);
            if (stock == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("INVALID_STOCK_CODE", "유효하지 않은 종목코드입니다."));
            }
            
            AutoTradingStrategy strategy = AutoTradingStrategy.createConservativeStrategy(stock);
            autoTradingEngine.registerStrategy(strategy);
            
            return ResponseEntity.ok(ApiResponse.success(stock.getStockName() + " 보수적 전략이 등록되었습니다."));
        } catch (Exception e) {
            log.error("보수적 전략 등록 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("CONSERVATIVE_STRATEGY_ERROR", "보수적 전략 등록 실패: " + e.getMessage()));
        }
    }

    /**
     * 공격적 전략 등록
     */
    @PostMapping("/strategies/aggressive/{stockCode}")
    public ResponseEntity<ApiResponse<String>> registerAggressiveStrategy(@PathVariable String stockCode) {
        try {
            log.info("공격적 전략 등록 요청: {}", stockCode);
            
            TopKospiStocks stock = TopKospiStocks.findByStockCode(stockCode);
            if (stock == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("INVALID_STOCK_CODE", "유효하지 않은 종목코드입니다."));
            }
            
            AutoTradingStrategy strategy = AutoTradingStrategy.createAggressiveStrategy(stock);
            autoTradingEngine.registerStrategy(strategy);
            
            return ResponseEntity.ok(ApiResponse.success(stock.getStockName() + " 공격적 전략이 등록되었습니다."));
        } catch (Exception e) {
            log.error("공격적 전략 등록 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("AGGRESSIVE_STRATEGY_ERROR", "공격적 전략 등록 실패: " + e.getMessage()));
        }
    }

    /**
     * 모든 KOSPI 상위 종목에 기본 전략 등록
     */
    @PostMapping("/strategies/default/all")
    public ResponseEntity<ApiResponse<String>> registerDefaultStrategiesForAll() {
        try {
            log.info("모든 KOSPI 상위 종목 기본 전략 등록 요청");
            
            int registeredCount = 0;
            for (TopKospiStocks stock : TopKospiStocks.values()) {
                try {
                    AutoTradingStrategy strategy = AutoTradingStrategy.createDefaultStrategy();
                    strategy.setTargetStock(stock);
                    strategy.setStockCode(stock.getStockCode());
                    strategy.setStockName(stock.getStockName());
                    strategy.setStrategyId("DEFAULT_" + stock.getStockCode());
                    strategy.setStrategyName(stock.getStockName() + " 기본 전략");
                    
                    autoTradingEngine.registerStrategy(strategy);
                    registeredCount++;
                } catch (Exception e) {
                    log.warn("종목 {} 전략 등록 실패: {}", stock.getStockCode(), e.getMessage());
                }
            }
            
            return ResponseEntity.ok(ApiResponse.success(registeredCount + "개 종목의 기본 전략이 등록되었습니다."));
        } catch (Exception e) {
            log.error("전체 기본 전략 등록 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("BULK_STRATEGY_ERROR", "전체 기본 전략 등록 실패: " + e.getMessage()));
        }
    }

    /**
     * 현재 스케줄 정보 조회
     */
    @GetMapping("/schedule/current")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentSchedule() {
        try {
            log.info("현재 스케줄 정보 조회 요청");
            
            TradingSchedule currentSchedule = TradingSchedule.getCurrentSchedule();
            
            Map<String, Object> scheduleInfo = new java.util.HashMap<>();
            scheduleInfo.put("scheduleName", currentSchedule.getScheduleName());
            scheduleInfo.put("description", currentSchedule.getDescription());
            scheduleInfo.put("startTime", currentSchedule.getStartTime().toString());
            scheduleInfo.put("endTime", currentSchedule.getEndTime().toString());
            scheduleInfo.put("intervalSeconds", currentSchedule.getIntervalSeconds());
            scheduleInfo.put("highFrequency", currentSchedule.isHighFrequency());
            scheduleInfo.put("isMarketHours", TradingSchedule.isMarketHours());
            scheduleInfo.put("isTradingHours", TradingSchedule.isTradingHours());
            scheduleInfo.put("isHighFrequencyTime", TradingSchedule.isHighFrequencyTime());
            
            return ResponseEntity.ok(ApiResponse.success(scheduleInfo));
        } catch (Exception e) {
            log.error("스케줄 정보 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("SCHEDULE_INFO_ERROR", "스케줄 정보 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 모든 스케줄 정보 조회
     */
    @GetMapping("/schedule/all")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllSchedules() {
        try {
            log.info("모든 스케줄 정보 조회 요청");

            List<Map<String, Object>> schedules = Arrays.stream(TradingSchedule.values())
                    .map(schedule -> {
                        Map<String, Object> scheduleMap = new java.util.HashMap<>();
                        scheduleMap.put("scheduleName", schedule.getScheduleName());
                        scheduleMap.put("description", schedule.getDescription());
                        scheduleMap.put("startTime", schedule.getStartTime().toString());
                        scheduleMap.put("endTime", schedule.getEndTime().toString());
                        scheduleMap.put("intervalSeconds", schedule.getIntervalSeconds());
                        scheduleMap.put("highFrequency", schedule.isHighFrequency());
                        return scheduleMap;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success(schedules));
        } catch (Exception e) {
            log.error("전체 스케줄 정보 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("SCHEDULE_LIST_ERROR", "전체 스케줄 정보 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * KOSPI 상위 종목 목록 조회
     */
    @GetMapping("/stocks/top-kospi")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> getTopKospiStocks() {
        try {
            log.info("KOSPI 상위 종목 목록 조회 요청");
            
            List<Map<String, String>> stocks = Arrays.stream(TopKospiStocks.values())
                    .map(stock -> Map.of(
                        "stockCode", stock.getStockCode(),
                        "stockName", stock.getStockName(),
                        "sector", stock.getSector()
                    ))
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.success(stocks));
        } catch (Exception e) {
            log.error("KOSPI 상위 종목 목록 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("STOCK_LIST_ERROR", "KOSPI 상위 종목 목록 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 섹터별 종목 목록 조회
     */
    @GetMapping("/stocks/sector/{sector}")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> getStocksBySector(@PathVariable String sector) {
        try {
            log.info("섹터별 종목 목록 조회 요청: {}", sector);
            
            TopKospiStocks[] sectorStocks = TopKospiStocks.findBySector(sector);
            
            List<Map<String, String>> stocks = Arrays.stream(sectorStocks)
                    .map(stock -> Map.of(
                        "stockCode", stock.getStockCode(),
                        "stockName", stock.getStockName(),
                        "sector", stock.getSector()
                    ))
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.success(stocks));
        } catch (Exception e) {
            log.error("섹터별 종목 목록 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("SECTOR_STOCK_ERROR", "섹터별 종목 목록 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 섹터 목록 조회
     */
    @GetMapping("/stocks/sectors")
    public ResponseEntity<ApiResponse<String[]>> getAllSectors() {
        try {
            log.info("섹터 목록 조회 요청");
            
            String[] sectors = TopKospiStocks.getAllSectors();
            
            return ResponseEntity.ok(ApiResponse.success(sectors));
        } catch (Exception e) {
            log.error("섹터 목록 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("SECTOR_LIST_ERROR", "섹터 목록 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 자동매매 상태 조회
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAutoTradingStatus() {
        try {
            log.info("자동매매 상태 조회 요청");
            
            TradingSchedule currentSchedule = TradingSchedule.getCurrentSchedule();
            
            Map<String, Object> status = new java.util.HashMap<>();
            status.put("currentSchedule", currentSchedule.getScheduleName());
            status.put("isMarketHours", TradingSchedule.isMarketHours());
            status.put("isTradingHours", TradingSchedule.isTradingHours());
            status.put("isHighFrequencyTime", TradingSchedule.isHighFrequencyTime());
            status.put("currentTime", java.time.LocalTime.now().toString());
            status.put("engineStatus", "RUNNING"); // 실제로는 엔진 상태를 확인해야 함
            
            return ResponseEntity.ok(ApiResponse.success(status));
        } catch (Exception e) {
            log.error("자동매매 상태 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("STATUS_ERROR", "자동매매 상태 조회 실패: " + e.getMessage()));
        }
    }
    
    // ==================== 해외 주식 관련 API ====================
    
    /**
     * 나스닥 상위 종목 목록 조회
     */
    @GetMapping("/stocks/top-nasdaq")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> getTopNasdaqStocks() {
        try {
            log.info("나스닥 상위 종목 목록 조회 요청");
            
            List<Map<String, String>> stocks = Arrays.stream(TopNasdaqStocks.values())
                    .map(stock -> Map.of(
                        "stockCode", stock.getStockCode(),
                        "stockName", stock.getCompanyName(),
                        "sector", stock.getSector()
                    ))
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.success(stocks));
        } catch (Exception e) {
            log.error("나스닥 상위 종목 목록 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("FOREIGN_STOCK_LIST_ERROR", "나스닥 상위 종목 목록 조회 실패: " + e.getMessage()));
        }
    }
    
    /**
     * 섹터별 종목 목록 조회 (해외)
     */
    @GetMapping("/stocks/foreign-sector/{sector}")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> getForeignStocksBySector(@PathVariable String sector) {
        try {
            log.info("섹터별 종목 목록 조회 요청 (해외): {}", sector);
            
            TopNasdaqStocks[] sectorStocks = TopNasdaqStocks.getBySector(sector);
            
            List<Map<String, String>> stocks = Arrays.stream(sectorStocks)
                    .map(stock -> Map.of(
                        "stockCode", stock.getStockCode(),
                        "stockName", stock.getCompanyName(),
                        "sector", stock.getSector()
                    ))
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.success(stocks));
        } catch (Exception e) {
            log.error("섹터별 종목 목록 조회 중 오류 발생 (해외): {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("FOREIGN_SECTOR_STOCK_ERROR", "섹터별 종목 목록 조회 실패 (해외): " + e.getMessage()));
        }
    }
    
    /**
     * 섹터 목록 조회 (해외)
     */
    @GetMapping("/stocks/foreign-sectors")
    public ResponseEntity<ApiResponse<String[]>> getAllForeignSectors() {
        try {
            log.info("섹터 목록 조회 요청 (해외)");
            
            String[] sectors = TopNasdaqStocks.getAllSectors();
            
            return ResponseEntity.ok(ApiResponse.success(sectors));
        } catch (Exception e) {
            log.error("섹터 목록 조회 중 오류 발생 (해외): {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("FOREIGN_SECTOR_LIST_ERROR", "섹터 목록 조회 실패 (해외): " + e.getMessage()));
        }
    }
} 