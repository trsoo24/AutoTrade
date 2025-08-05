package trade.project.trading.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import trade.project.common.dto.ApiResponse;
import trade.project.trading.document.PriceQueryRecord;
import trade.project.trading.service.PriceQueryRecordService;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/price/records")
@RequiredArgsConstructor
public class PriceQueryRecordController {

    private final PriceQueryRecordService priceQueryRecordService;

    /**
     * 종목코드로 시세 조회 기록 조회
     */
    @GetMapping("/stock/{stockCode}")
    public ResponseEntity<ApiResponse<List<PriceQueryRecord>>> getPriceQueryRecordsByStock(
            @PathVariable String stockCode) {
        try {
            log.info("시세 조회 기록 조회 요청 (종목코드): {}", stockCode);
            
            List<PriceQueryRecord> records = priceQueryRecordService.findByStockCode(stockCode);
            
            return ResponseEntity.ok(ApiResponse.success(records));
        } catch (Exception e) {
            log.error("시세 조회 기록 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("PRICE_QUERY_RECORD_ERROR", "시세 조회 기록 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 조회유형으로 시세 조회 기록 조회
     */
    @GetMapping("/type/{queryType}")
    public ResponseEntity<ApiResponse<List<PriceQueryRecord>>> getPriceQueryRecordsByType(
            @PathVariable String queryType) {
        try {
            log.info("시세 조회 기록 조회 요청 (조회유형): {}", queryType);
            
            List<PriceQueryRecord> records = priceQueryRecordService.findByQueryType(queryType);
            
            return ResponseEntity.ok(ApiResponse.success(records));
        } catch (Exception e) {
            log.error("시세 조회 기록 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("PRICE_QUERY_RECORD_ERROR", "시세 조회 기록 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 종목코드와 조회유형으로 시세 조회 기록 조회
     */
    @GetMapping("/stock/{stockCode}/type/{queryType}")
    public ResponseEntity<ApiResponse<List<PriceQueryRecord>>> getPriceQueryRecordsByStockAndType(
            @PathVariable String stockCode, @PathVariable String queryType) {
        try {
            log.info("시세 조회 기록 조회 요청 (종목코드: {}, 조회유형: {})", stockCode, queryType);
            
            List<PriceQueryRecord> records = priceQueryRecordService.findByStockCodeAndQueryType(stockCode, queryType);
            
            return ResponseEntity.ok(ApiResponse.success(records));
        } catch (Exception e) {
            log.error("시세 조회 기록 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("PRICE_QUERY_RECORD_ERROR", "시세 조회 기록 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 기간별 시세 조회 기록 조회
     */
    @GetMapping("/period")
    public ResponseEntity<ApiResponse<List<PriceQueryRecord>>> getPriceQueryRecordsByPeriod(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDateTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDateTime) {
        try {
            log.info("시세 조회 기록 조회 요청 (기간: {} ~ {})", startDateTime, endDateTime);
            
            List<PriceQueryRecord> records = priceQueryRecordService.findByQueryDateTimeBetween(startDateTime, endDateTime);
            
            return ResponseEntity.ok(ApiResponse.success(records));
        } catch (Exception e) {
            log.error("시세 조회 기록 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("PRICE_QUERY_RECORD_ERROR", "시세 조회 기록 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 종목코드와 기간으로 시세 조회 기록 조회
     */
    @GetMapping("/stock/{stockCode}/period")
    public ResponseEntity<ApiResponse<List<PriceQueryRecord>>> getPriceQueryRecordsByStockAndPeriod(
            @PathVariable String stockCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDateTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDateTime) {
        try {
            log.info("시세 조회 기록 조회 요청 (종목코드: {}, 기간: {} ~ {})", stockCode, startDateTime, endDateTime);
            
            List<PriceQueryRecord> records = priceQueryRecordService.findByStockCodeAndQueryDateTimeBetween(stockCode, startDateTime, endDateTime);
            
            return ResponseEntity.ok(ApiResponse.success(records));
        } catch (Exception e) {
            log.error("시세 조회 기록 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("PRICE_QUERY_RECORD_ERROR", "시세 조회 기록 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 조회유형과 기간으로 시세 조회 기록 조회
     */
    @GetMapping("/type/{queryType}/period")
    public ResponseEntity<ApiResponse<List<PriceQueryRecord>>> getPriceQueryRecordsByTypeAndPeriod(
            @PathVariable String queryType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDateTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDateTime) {
        try {
            log.info("시세 조회 기록 조회 요청 (조회유형: {}, 기간: {} ~ {})", queryType, startDateTime, endDateTime);
            
            List<PriceQueryRecord> records = priceQueryRecordService.findByQueryTypeAndQueryDateTimeBetween(queryType, startDateTime, endDateTime);
            
            return ResponseEntity.ok(ApiResponse.success(records));
        } catch (Exception e) {
            log.error("시세 조회 기록 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("PRICE_QUERY_RECORD_ERROR", "시세 조회 기록 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 에러가 발생한 시세 조회 기록 조회
     */
    @GetMapping("/errors")
    public ResponseEntity<ApiResponse<List<PriceQueryRecord>>> getErrorPriceQueryRecords() {
        try {
            log.info("에러 시세 조회 기록 조회 요청");
            
            List<PriceQueryRecord> records = priceQueryRecordService.findErrorRecords();
            
            return ResponseEntity.ok(ApiResponse.success(records));
        } catch (Exception e) {
            log.error("시세 조회 기록 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("PRICE_QUERY_RECORD_ERROR", "시세 조회 기록 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 성공한 시세 조회 기록 조회
     */
    @GetMapping("/success")
    public ResponseEntity<ApiResponse<List<PriceQueryRecord>>> getSuccessPriceQueryRecords() {
        try {
            log.info("성공 시세 조회 기록 조회 요청");
            
            List<PriceQueryRecord> records = priceQueryRecordService.findSuccessRecords();
            
            return ResponseEntity.ok(ApiResponse.success(records));
        } catch (Exception e) {
            log.error("시세 조회 기록 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("PRICE_QUERY_RECORD_ERROR", "시세 조회 기록 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 특정 종목의 성공한 시세 조회 기록 조회
     */
    @GetMapping("/stock/{stockCode}/success")
    public ResponseEntity<ApiResponse<List<PriceQueryRecord>>> getSuccessPriceQueryRecordsByStock(
            @PathVariable String stockCode) {
        try {
            log.info("성공 시세 조회 기록 조회 요청 (종목코드: {})", stockCode);
            
            List<PriceQueryRecord> records = priceQueryRecordService.findSuccessRecordsByStock(stockCode);
            
            return ResponseEntity.ok(ApiResponse.success(records));
        } catch (Exception e) {
            log.error("시세 조회 기록 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("PRICE_QUERY_RECORD_ERROR", "시세 조회 기록 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 특정 조회유형의 성공한 시세 조회 기록 조회
     */
    @GetMapping("/type/{queryType}/success")
    public ResponseEntity<ApiResponse<List<PriceQueryRecord>>> getSuccessPriceQueryRecordsByType(
            @PathVariable String queryType) {
        try {
            log.info("성공 시세 조회 기록 조회 요청 (조회유형: {})", queryType);
            
            List<PriceQueryRecord> records = priceQueryRecordService.findSuccessRecordsByQueryType(queryType);
            
            return ResponseEntity.ok(ApiResponse.success(records));
        } catch (Exception e) {
            log.error("시세 조회 기록 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("PRICE_QUERY_RECORD_ERROR", "시세 조회 기록 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 클라이언트 IP로 시세 조회 기록 조회
     */
    @GetMapping("/client/{clientIp}")
    public ResponseEntity<ApiResponse<List<PriceQueryRecord>>> getPriceQueryRecordsByClientIp(
            @PathVariable String clientIp) {
        try {
            log.info("시세 조회 기록 조회 요청 (클라이언트 IP: {})", clientIp);
            
            List<PriceQueryRecord> records = priceQueryRecordService.findByClientIp(clientIp);
            
            return ResponseEntity.ok(ApiResponse.success(records));
        } catch (Exception e) {
            log.error("시세 조회 기록 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("PRICE_QUERY_RECORD_ERROR", "시세 조회 기록 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 세션 ID로 시세 조회 기록 조회
     */
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<ApiResponse<List<PriceQueryRecord>>> getPriceQueryRecordsBySessionId(
            @PathVariable String sessionId) {
        try {
            log.info("시세 조회 기록 조회 요청 (세션 ID: {})", sessionId);
            
            List<PriceQueryRecord> records = priceQueryRecordService.findBySessionId(sessionId);
            
            return ResponseEntity.ok(ApiResponse.success(records));
        } catch (Exception e) {
            log.error("시세 조회 기록 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("PRICE_QUERY_RECORD_ERROR", "시세 조회 기록 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 최근 시세 조회 기록 조회 (최근 10개)
     */
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<PriceQueryRecord>>> getRecentPriceQueryRecords() {
        try {
            log.info("최근 시세 조회 기록 조회 요청");
            
            List<PriceQueryRecord> records = priceQueryRecordService.findRecentRecords();
            
            return ResponseEntity.ok(ApiResponse.success(records));
        } catch (Exception e) {
            log.error("시세 조회 기록 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("PRICE_QUERY_RECORD_ERROR", "시세 조회 기록 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 특정 종목의 최근 시세 조회 기록 조회 (최근 5개)
     */
    @GetMapping("/stock/{stockCode}/recent")
    public ResponseEntity<ApiResponse<List<PriceQueryRecord>>> getRecentPriceQueryRecordsByStock(
            @PathVariable String stockCode) {
        try {
            log.info("최근 시세 조회 기록 조회 요청 (종목코드: {})", stockCode);
            
            List<PriceQueryRecord> records = priceQueryRecordService.findRecentRecordsByStock(stockCode);
            
            return ResponseEntity.ok(ApiResponse.success(records));
        } catch (Exception e) {
            log.error("시세 조회 기록 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("PRICE_QUERY_RECORD_ERROR", "시세 조회 기록 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 특정 종목의 현재가 조회 기록만 조회
     */
    @GetMapping("/stock/{stockCode}/current")
    public ResponseEntity<ApiResponse<List<PriceQueryRecord>>> getCurrentPriceQueryRecordsByStock(
            @PathVariable String stockCode) {
        try {
            log.info("현재가 조회 기록 조회 요청 (종목코드: {})", stockCode);
            
            List<PriceQueryRecord> records = priceQueryRecordService.findCurrentPriceRecordsByStock(stockCode);
            
            return ResponseEntity.ok(ApiResponse.success(records));
        } catch (Exception e) {
            log.error("시세 조회 기록 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("PRICE_QUERY_RECORD_ERROR", "시세 조회 기록 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 특정 종목의 일자별 시세 조회 기록만 조회
     */
    @GetMapping("/stock/{stockCode}/daily")
    public ResponseEntity<ApiResponse<List<PriceQueryRecord>>> getDailyPriceQueryRecordsByStock(
            @PathVariable String stockCode) {
        try {
            log.info("일자별 시세 조회 기록 조회 요청 (종목코드: {})", stockCode);
            
            List<PriceQueryRecord> records = priceQueryRecordService.findDailyPriceRecordsByStock(stockCode);
            
            return ResponseEntity.ok(ApiResponse.success(records));
        } catch (Exception e) {
            log.error("시세 조회 기록 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("PRICE_QUERY_RECORD_ERROR", "시세 조회 기록 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 시세 조회 통계 조회
     */
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<List<PriceQueryRecord>>> getPriceQueryStatistics() {
        try {
            log.info("시세 조회 통계 조회 요청");
            
            List<PriceQueryRecord> statistics = priceQueryRecordService.getQueryStatistics();
            
            return ResponseEntity.ok(ApiResponse.success(statistics));
        } catch (Exception e) {
            log.error("시세 조회 통계 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("PRICE_QUERY_STATISTICS_ERROR", "시세 조회 통계 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 특정 기간의 시세 조회 기록 수 조회
     */
    @GetMapping("/count/period")
    public ResponseEntity<ApiResponse<Long>> getPriceQueryRecordCountByPeriod(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDateTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDateTime) {
        try {
            log.info("시세 조회 기록 수 조회 요청 (기간: {} ~ {})", startDateTime, endDateTime);
            
            long count = priceQueryRecordService.countByQueryDateTimeBetween(startDateTime, endDateTime);
            
            return ResponseEntity.ok(ApiResponse.success(count));
        } catch (Exception e) {
            log.error("시세 조회 기록 수 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("PRICE_QUERY_COUNT_ERROR", "시세 조회 기록 수 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 특정 종목의 시세 조회 기록 수 조회
     */
    @GetMapping("/count/stock/{stockCode}")
    public ResponseEntity<ApiResponse<Long>> getPriceQueryRecordCountByStock(
            @PathVariable String stockCode) {
        try {
            log.info("시세 조회 기록 수 조회 요청 (종목코드: {})", stockCode);
            
            long count = priceQueryRecordService.countByStockCode(stockCode);
            
            return ResponseEntity.ok(ApiResponse.success(count));
        } catch (Exception e) {
            log.error("시세 조회 기록 수 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("PRICE_QUERY_COUNT_ERROR", "시세 조회 기록 수 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 특정 조회유형의 시세 조회 기록 수 조회
     */
    @GetMapping("/count/type/{queryType}")
    public ResponseEntity<ApiResponse<Long>> getPriceQueryRecordCountByType(
            @PathVariable String queryType) {
        try {
            log.info("시세 조회 기록 수 조회 요청 (조회유형: {})", queryType);
            
            long count = priceQueryRecordService.countByQueryType(queryType);
            
            return ResponseEntity.ok(ApiResponse.success(count));
        } catch (Exception e) {
            log.error("시세 조회 기록 수 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("PRICE_QUERY_COUNT_ERROR", "시세 조회 기록 수 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 에러가 발생한 시세 조회 기록 수 조회
     */
    @GetMapping("/count/errors")
    public ResponseEntity<ApiResponse<Long>> getErrorPriceQueryRecordCount() {
        try {
            log.info("에러 시세 조회 기록 수 조회 요청");
            
            long count = priceQueryRecordService.countErrorRecords();
            
            return ResponseEntity.ok(ApiResponse.success(count));
        } catch (Exception e) {
            log.error("시세 조회 기록 수 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("PRICE_QUERY_COUNT_ERROR", "시세 조회 기록 수 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 성공한 시세 조회 기록 수 조회
     */
    @GetMapping("/count/success")
    public ResponseEntity<ApiResponse<Long>> getSuccessPriceQueryRecordCount() {
        try {
            log.info("성공 시세 조회 기록 수 조회 요청");
            
            long count = priceQueryRecordService.countSuccessRecords();
            
            return ResponseEntity.ok(ApiResponse.success(count));
        } catch (Exception e) {
            log.error("시세 조회 기록 수 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("PRICE_QUERY_COUNT_ERROR", "시세 조회 기록 수 조회 실패: " + e.getMessage()));
        }
    }
} 