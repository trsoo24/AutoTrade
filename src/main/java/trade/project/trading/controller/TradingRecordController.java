package trade.project.trading.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import trade.project.common.dto.ApiResponse;
import trade.project.trading.entity.TradingRecord;
import trade.project.trading.service.TradingRecordService;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/trading/records")
@RequiredArgsConstructor
public class TradingRecordController {

    private final TradingRecordService tradingRecordService;

    /**
     * 주문번호로 매매 기록 조회
     */
    @GetMapping("/order/{orderNumber}")
    public ResponseEntity<ApiResponse<TradingRecord>> getTradingRecordByOrderNumber(
            @PathVariable String orderNumber) {
        try {
            log.info("매매 기록 조회 요청 (주문번호): {}", orderNumber);
            
            TradingRecord record = tradingRecordService.findByOrderNumber(orderNumber);
            
            if (record == null) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(ApiResponse.success(record));
        } catch (Exception e) {
            log.error("매매 기록 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("TRADING_RECORD_QUERY_ERROR", "매매 기록 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 계좌번호로 매매 기록 목록 조회
     */
    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<ApiResponse<List<TradingRecord>>> getTradingRecordsByAccount(
            @PathVariable String accountNumber) {
        try {
            log.info("매매 기록 조회 요청 (계좌번호): {}", accountNumber);
            
            List<TradingRecord> records = tradingRecordService.findByAccountNumber(accountNumber);
            
            return ResponseEntity.ok(ApiResponse.success(records));
        } catch (Exception e) {
            log.error("매매 기록 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("TRADING_RECORD_QUERY_ERROR", "매매 기록 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 종목코드로 매매 기록 목록 조회
     */
    @GetMapping("/stock/{stockCode}")
    public ResponseEntity<ApiResponse<List<TradingRecord>>> getTradingRecordsByStock(
            @PathVariable String stockCode) {
        try {
            log.info("매매 기록 조회 요청 (종목코드): {}", stockCode);
            
            List<TradingRecord> records = tradingRecordService.findByStockCode(stockCode);
            
            return ResponseEntity.ok(ApiResponse.success(records));
        } catch (Exception e) {
            log.error("매매 기록 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("TRADING_RECORD_QUERY_ERROR", "매매 기록 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 주문구분으로 매매 기록 목록 조회
     */
    @GetMapping("/type/{orderType}")
    public ResponseEntity<ApiResponse<List<TradingRecord>>> getTradingRecordsByOrderType(
            @PathVariable String orderType) {
        try {
            log.info("매매 기록 조회 요청 (주문구분): {}", orderType);
            
            List<TradingRecord> records = tradingRecordService.findByOrderType(orderType);
            
            return ResponseEntity.ok(ApiResponse.success(records));
        } catch (Exception e) {
            log.error("매매 기록 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("TRADING_RECORD_QUERY_ERROR", "매매 기록 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 주문상태로 매매 기록 목록 조회
     */
    @GetMapping("/status/{orderStatus}")
    public ResponseEntity<ApiResponse<List<TradingRecord>>> getTradingRecordsByOrderStatus(
            @PathVariable String orderStatus) {
        try {
            log.info("매매 기록 조회 요청 (주문상태): {}", orderStatus);
            
            List<TradingRecord> records = tradingRecordService.findByOrderStatus(orderStatus);
            
            return ResponseEntity.ok(ApiResponse.success(records));
        } catch (Exception e) {
            log.error("매매 기록 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("TRADING_RECORD_QUERY_ERROR", "매매 기록 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 계좌번호와 종목코드로 매매 기록 목록 조회
     */
    @GetMapping("/account/{accountNumber}/stock/{stockCode}")
    public ResponseEntity<ApiResponse<List<TradingRecord>>> getTradingRecordsByAccountAndStock(
            @PathVariable String accountNumber, @PathVariable String stockCode) {
        try {
            log.info("매매 기록 조회 요청 (계좌번호: {}, 종목코드: {})", accountNumber, stockCode);
            
            List<TradingRecord> records = tradingRecordService.findByAccountNumberAndStockCode(accountNumber, stockCode);
            
            return ResponseEntity.ok(ApiResponse.success(records));
        } catch (Exception e) {
            log.error("매매 기록 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("TRADING_RECORD_QUERY_ERROR", "매매 기록 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 기간별 매매 기록 조회
     */
    @GetMapping("/period")
    public ResponseEntity<ApiResponse<List<TradingRecord>>> getTradingRecordsByPeriod(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDateTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDateTime) {
        try {
            log.info("매매 기록 조회 요청 (기간: {} ~ {})", startDateTime, endDateTime);
            
            List<TradingRecord> records = tradingRecordService.findByOrderDateTimeBetween(startDateTime, endDateTime);
            
            return ResponseEntity.ok(ApiResponse.success(records));
        } catch (Exception e) {
            log.error("매매 기록 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("TRADING_RECORD_QUERY_ERROR", "매매 기록 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 계좌번호와 기간으로 매매 기록 조회
     */
    @GetMapping("/account/{accountNumber}/period")
    public ResponseEntity<ApiResponse<List<TradingRecord>>> getTradingRecordsByAccountAndPeriod(
            @PathVariable String accountNumber,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDateTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDateTime) {
        try {
            log.info("매매 기록 조회 요청 (계좌번호: {}, 기간: {} ~ {})", accountNumber, startDateTime, endDateTime);
            
            List<TradingRecord> records = tradingRecordService.findByAccountNumberAndOrderDateTimeBetween(accountNumber, startDateTime, endDateTime);
            
            return ResponseEntity.ok(ApiResponse.success(records));
        } catch (Exception e) {
            log.error("매매 기록 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("TRADING_RECORD_QUERY_ERROR", "매매 기록 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 종목코드와 기간으로 매매 기록 조회
     */
    @GetMapping("/stock/{stockCode}/period")
    public ResponseEntity<ApiResponse<List<TradingRecord>>> getTradingRecordsByStockAndPeriod(
            @PathVariable String stockCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDateTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDateTime) {
        try {
            log.info("매매 기록 조회 요청 (종목코드: {}, 기간: {} ~ {})", stockCode, startDateTime, endDateTime);
            
            List<TradingRecord> records = tradingRecordService.findByStockCodeAndOrderDateTimeBetween(stockCode, startDateTime, endDateTime);
            
            return ResponseEntity.ok(ApiResponse.success(records));
        } catch (Exception e) {
            log.error("매매 기록 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("TRADING_RECORD_QUERY_ERROR", "매매 기록 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 에러가 발생한 매매 기록 조회
     */
    @GetMapping("/errors")
    public ResponseEntity<ApiResponse<List<TradingRecord>>> getErrorTradingRecords() {
        try {
            log.info("에러 매매 기록 조회 요청");
            
            List<TradingRecord> records = tradingRecordService.findErrorRecords();
            
            return ResponseEntity.ok(ApiResponse.success(records));
        } catch (Exception e) {
            log.error("매매 기록 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("TRADING_RECORD_QUERY_ERROR", "매매 기록 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 성공한 매매 기록 조회
     */
    @GetMapping("/success")
    public ResponseEntity<ApiResponse<List<TradingRecord>>> getSuccessTradingRecords() {
        try {
            log.info("성공 매매 기록 조회 요청");
            
            List<TradingRecord> records = tradingRecordService.findSuccessRecords();
            
            return ResponseEntity.ok(ApiResponse.success(records));
        } catch (Exception e) {
            log.error("매매 기록 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("TRADING_RECORD_QUERY_ERROR", "매매 기록 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 특정 계좌의 성공한 매매 기록 조회
     */
    @GetMapping("/account/{accountNumber}/success")
    public ResponseEntity<ApiResponse<List<TradingRecord>>> getSuccessTradingRecordsByAccount(
            @PathVariable String accountNumber) {
        try {
            log.info("성공 매매 기록 조회 요청 (계좌번호: {})", accountNumber);
            
            List<TradingRecord> records = tradingRecordService.findSuccessRecordsByAccount(accountNumber);
            
            return ResponseEntity.ok(ApiResponse.success(records));
        } catch (Exception e) {
            log.error("매매 기록 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("TRADING_RECORD_QUERY_ERROR", "매매 기록 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 특정 종목의 성공한 매매 기록 조회
     */
    @GetMapping("/stock/{stockCode}/success")
    public ResponseEntity<ApiResponse<List<TradingRecord>>> getSuccessTradingRecordsByStock(
            @PathVariable String stockCode) {
        try {
            log.info("성공 매매 기록 조회 요청 (종목코드: {})", stockCode);
            
            List<TradingRecord> records = tradingRecordService.findSuccessRecordsByStock(stockCode);
            
            return ResponseEntity.ok(ApiResponse.success(records));
        } catch (Exception e) {
            log.error("매매 기록 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("TRADING_RECORD_QUERY_ERROR", "매매 기록 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 매매 기록 통계 조회 (계좌별)
     */
    @GetMapping("/statistics/account/{accountNumber}")
    public ResponseEntity<ApiResponse<List<Object[]>>> getTradingStatisticsByAccount(
            @PathVariable String accountNumber) {
        try {
            log.info("매매 기록 통계 조회 요청 (계좌번호: {})", accountNumber);
            
            List<Object[]> statistics = tradingRecordService.getTradingStatisticsByAccount(accountNumber);
            
            return ResponseEntity.ok(ApiResponse.success(statistics));
        } catch (Exception e) {
            log.error("매매 기록 통계 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("TRADING_STATISTICS_ERROR", "매매 기록 통계 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 매매 기록 통계 조회 (종목별)
     */
    @GetMapping("/statistics/stock/{stockCode}")
    public ResponseEntity<ApiResponse<List<Object[]>>> getTradingStatisticsByStock(
            @PathVariable String stockCode) {
        try {
            log.info("매매 기록 통계 조회 요청 (종목코드: {})", stockCode);
            
            List<Object[]> statistics = tradingRecordService.getTradingStatisticsByStock(stockCode);
            
            return ResponseEntity.ok(ApiResponse.success(statistics));
        } catch (Exception e) {
            log.error("매매 기록 통계 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("TRADING_STATISTICS_ERROR", "매매 기록 통계 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 최근 매매 기록 조회 (최근 N개)
     */
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<TradingRecord>>> getRecentTradingRecords(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            log.info("최근 매매 기록 조회 요청 (최근 {}개)", limit);
            
            List<TradingRecord> records = tradingRecordService.findRecentTradingRecords(limit);
            
            return ResponseEntity.ok(ApiResponse.success(records));
        } catch (Exception e) {
            log.error("매매 기록 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("TRADING_RECORD_QUERY_ERROR", "매매 기록 조회 실패: " + e.getMessage()));
        }
    }
} 