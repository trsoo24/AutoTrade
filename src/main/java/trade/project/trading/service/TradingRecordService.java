package trade.project.trading.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import trade.project.api.dto.StockOrderRequest;
import trade.project.api.dto.StockOrderResponse;
import trade.project.api.dto.ForeignStockOrderRequest;
import trade.project.api.dto.ForeignStockOrderResponse;
import trade.project.trading.entity.TradingRecord;
import trade.project.trading.repository.TradingRecordRepository;

import java.time.LocalDateTime;
import jakarta.servlet.http.HttpServletRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class TradingRecordService {

    private final TradingRecordRepository tradingRecordRepository;
    private final ObjectMapper objectMapper;

    /**
     * 매매 기록 저장
     */
    public TradingRecord saveTradingRecord(StockOrderRequest request, StockOrderResponse response, String apiResponseJson) {
        try {
            TradingRecord tradingRecord = TradingRecord.builder()
                    .orderNumber(response.getOrderNumber())
                    .accountNumber(request.getAccountNumber())
                    .stockCode(request.getStockCode())
                    .stockName(response.getStockName())
                    .orderType(request.getOrderType())
                    .quantity(request.getQuantity())
                    .price(request.getPrice())
                    .priceType(request.getPriceType())
                    .orderStatus(response.getOrderStatus())
                    .orderCategory(request.getOrderCategory())
                    .originalOrderNumber(request.getOriginalOrderNumber())
                    .totalAmount(calculateTotalAmount(request.getQuantity(), request.getPrice()))
                    .commission(calculateCommission(request.getQuantity(), request.getPrice()))
                    .errorCode(response.getErrorCode())
                    .errorMessage(response.getErrorMessage())
                    .apiResponse(apiResponseJson)
                    .build();

            TradingRecord savedRecord = tradingRecordRepository.save(tradingRecord);
            log.info("매매 기록 저장 완료: {}", savedRecord.getOrderNumber());
            
            return savedRecord;
        } catch (Exception e) {
            log.error("매매 기록 저장 중 오류 발생: {}", e.getMessage());
            throw new RuntimeException("매매 기록 저장 실패", e);
        }
    }

    /**
     * 매매 기록 저장 (API 응답 객체로)
     */
    public TradingRecord saveTradingRecord(StockOrderRequest request, StockOrderResponse response, Object apiResponse) {
        String apiResponseJson = null;
        try {
            apiResponseJson = objectMapper.writeValueAsString(apiResponse);
        } catch (JsonProcessingException e) {
            log.warn("API 응답 JSON 변환 실패: {}", e.getMessage());
        }
        
        return saveTradingRecord(request, response, apiResponseJson);
    }
    
    /**
     * 해외 주식 매매 기록 저장
     */
    public TradingRecord saveForeignTradingRecord(ForeignStockOrderRequest request, ForeignStockOrderResponse response, String status, HttpServletRequest httpRequest) {
        try {
            String orderNumber = response != null ? response.getOrderNumber() : "UNKNOWN";
            String executedPrice = response != null && response.getExecutedPrice() != null ? 
                    response.getExecutedPrice().toString() : "0";
            String executedQuantity = response != null && response.getExecutedQuantity() != null ? 
                    response.getExecutedQuantity().toString() : "0";
            
            TradingRecord tradingRecord = TradingRecord.builder()
                    .orderNumber(orderNumber)
                    .accountNumber(request.getAccountNumber())
                    .stockCode(request.getStockCode())
                    .stockName(response != null ? response.getCompanyName() : "Unknown")
                    .orderType(request.getOrderType())
                    .quantity(request.getQuantity())
                    .price(request.getPrice().intValue())
                    .priceType(request.getPriceType())
                    .orderStatus(status)
                    .orderCategory(request.getOrderCategory())
                    .originalOrderNumber(null)
                    .totalAmount(calculateTotalAmount(request.getQuantity(), request.getPrice().intValue()))
                    .commission(calculateCommission(request.getQuantity(), request.getPrice().intValue()))
                    .errorCode(status.equals("실패") ? "FOREIGN_ORDER_ERROR" : null)
                    .errorMessage(status.equals("실패") ? "해외 주식 주문 실패" : null)
                    .apiResponse("Foreign Stock Order")
                    .build();

            TradingRecord savedRecord = tradingRecordRepository.save(tradingRecord);
            log.info("해외 주식 매매 기록 저장 완료: {}", savedRecord.getOrderNumber());
            
            return savedRecord;
        } catch (Exception e) {
            log.error("해외 주식 매매 기록 저장 중 오류 발생: {}", e.getMessage());
            throw new RuntimeException("해외 주식 매매 기록 저장 실패", e);
        }
    }

    /**
     * 주문번호로 매매 기록 조회
     */
    public TradingRecord findByOrderNumber(String orderNumber) {
        return tradingRecordRepository.findByOrderNumber(orderNumber)
                .orElse(null);
    }

    /**
     * 계좌번호로 매매 기록 목록 조회
     */
    public java.util.List<TradingRecord> findByAccountNumber(String accountNumber) {
        return tradingRecordRepository.findByAccountNumberOrderByOrderDateTimeDesc(accountNumber);
    }

    /**
     * 종목코드로 매매 기록 목록 조회
     */
    public java.util.List<TradingRecord> findByStockCode(String stockCode) {
        return tradingRecordRepository.findByStockCodeOrderByOrderDateTimeDesc(stockCode);
    }

    /**
     * 주문구분으로 매매 기록 목록 조회
     */
    public java.util.List<TradingRecord> findByOrderType(String orderType) {
        return tradingRecordRepository.findByOrderTypeOrderByOrderDateTimeDesc(orderType);
    }

    /**
     * 주문상태로 매매 기록 목록 조회
     */
    public java.util.List<TradingRecord> findByOrderStatus(String orderStatus) {
        return tradingRecordRepository.findByOrderStatusOrderByOrderDateTimeDesc(orderStatus);
    }

    /**
     * 계좌번호와 종목코드로 매매 기록 목록 조회
     */
    public java.util.List<TradingRecord> findByAccountNumberAndStockCode(String accountNumber, String stockCode) {
        return tradingRecordRepository.findByAccountNumberAndStockCodeOrderByOrderDateTimeDesc(accountNumber, stockCode);
    }

    /**
     * 기간별 매매 기록 조회
     */
    public java.util.List<TradingRecord> findByOrderDateTimeBetween(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        return tradingRecordRepository.findByOrderDateTimeBetweenOrderByOrderDateTimeDesc(startDateTime, endDateTime);
    }

    /**
     * 계좌번호와 기간으로 매매 기록 조회
     */
    public java.util.List<TradingRecord> findByAccountNumberAndOrderDateTimeBetween(String accountNumber, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        return tradingRecordRepository.findByAccountNumberAndOrderDateTimeBetweenOrderByOrderDateTimeDesc(accountNumber, startDateTime, endDateTime);
    }

    /**
     * 종목코드와 기간으로 매매 기록 조회
     */
    public java.util.List<TradingRecord> findByStockCodeAndOrderDateTimeBetween(String stockCode, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        return tradingRecordRepository.findByStockCodeAndOrderDateTimeBetweenOrderByOrderDateTimeDesc(stockCode, startDateTime, endDateTime);
    }

    /**
     * 에러가 발생한 매매 기록 조회
     */
    public java.util.List<TradingRecord> findErrorRecords() {
        return tradingRecordRepository.findByErrorCodeIsNotNullOrderByOrderDateTimeDesc();
    }

    /**
     * 성공한 매매 기록 조회
     */
    public java.util.List<TradingRecord> findSuccessRecords() {
        return tradingRecordRepository.findByErrorCodeIsNullOrderByOrderDateTimeDesc();
    }

    /**
     * 특정 계좌의 성공한 매매 기록 조회
     */
    public java.util.List<TradingRecord> findSuccessRecordsByAccount(String accountNumber) {
        return tradingRecordRepository.findByAccountNumberAndErrorCodeIsNullOrderByOrderDateTimeDesc(accountNumber);
    }

    /**
     * 특정 종목의 성공한 매매 기록 조회
     */
    public java.util.List<TradingRecord> findSuccessRecordsByStock(String stockCode) {
        return tradingRecordRepository.findByStockCodeAndErrorCodeIsNullOrderByOrderDateTimeDesc(stockCode);
    }

    /**
     * 매매 기록 통계 조회 (계좌별)
     */
    public java.util.List<Object[]> getTradingStatisticsByAccount(String accountNumber) {
        return tradingRecordRepository.getTradingStatisticsByAccount(accountNumber);
    }

    /**
     * 매매 기록 통계 조회 (종목별)
     */
    public java.util.List<Object[]> getTradingStatisticsByStock(String stockCode) {
        return tradingRecordRepository.getTradingStatisticsByStock(stockCode);
    }

    /**
     * 최근 매매 기록 조회 (최근 N개)
     */
    public java.util.List<TradingRecord> findRecentTradingRecords(int limit) {
        return tradingRecordRepository.findRecentTradingRecords(
                org.springframework.data.domain.PageRequest.of(0, limit)
        );
    }

    /**
     * 총 거래금액 계산
     */
    private Long calculateTotalAmount(Integer quantity, Integer price) {
        if (quantity == null || price == null) {
            return null;
        }
        return (long) quantity * price;
    }

    /**
     * 수수료 계산 (예: 0.015% 수수료)
     */
    private Integer calculateCommission(Integer quantity, Integer price) {
        if (quantity == null || price == null) {
            return null;
        }
        long totalAmount = (long) quantity * price;
        return (int) (totalAmount * 0.00015); // 0.015% 수수료
    }
} 