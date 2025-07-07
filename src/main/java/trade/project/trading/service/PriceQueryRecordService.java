package trade.project.trading.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import trade.project.api.dto.StockPriceRequest;
import trade.project.api.dto.StockPriceResponse;
import trade.project.api.dto.StockDailyPriceRequest;
import trade.project.api.dto.StockDailyPriceResponse;
import trade.project.trading.document.PriceQueryRecord;
import trade.project.trading.repository.PriceQueryRecordRepository;
import trade.project.api.dto.ForeignStockPriceRequest;
import trade.project.api.dto.ForeignStockPriceResponse;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PriceQueryRecordService {

    private final PriceQueryRecordRepository priceQueryRecordRepository;
    private final ObjectMapper objectMapper;

    /**
     * 현재가 조회 기록 저장
     */
    public PriceQueryRecord saveCurrentPriceRecord(StockPriceRequest request, StockPriceResponse response, 
                                                 Object apiResponse, HttpServletRequest httpRequest) {
        try {
            PriceQueryRecord record = PriceQueryRecord.builder()
                    .stockCode(request.getStockCode())
                    .stockName(response.getStockName())
                    .queryType("current")
                    .currentPrice(response.getCurrentPrice())
                    .previousClose(response.getPreviousClose())
                    .openPrice(response.getOpenPrice())
                    .highPrice(response.getHighPrice())
                    .lowPrice(response.getLowPrice())
                    .tradingVolume(response.getTradingVolume())
                    .tradingValue(response.getTradingValue())
                    .changeRate(response.getChangeRate())
                    .changeAmount(response.getChangeAmount())
                    .marketStatus(response.getMarketStatus())
                    .errorCode(response.getErrorCode())
                    .errorMessage(response.getErrorMessage())
                    .apiResponse(convertToJson(apiResponse))
                    .userAgent(getUserAgent(httpRequest))
                    .clientIp(getClientIp(httpRequest))
                    .sessionId(getSessionId(httpRequest))
                    .build();

            record.setCreatedAt();
            
            PriceQueryRecord savedRecord = priceQueryRecordRepository.save(record);
            log.info("현재가 조회 기록 저장 완료: {} - {}", savedRecord.getStockCode(), savedRecord.getStockName());
            
            return savedRecord;
        } catch (Exception e) {
            log.error("현재가 조회 기록 저장 중 오류 발생: {}", e.getMessage());
            throw new RuntimeException("현재가 조회 기록 저장 실패", e);
        }
    }

    /**
     * 일자별 시세 조회 기록 저장
     */
    public PriceQueryRecord saveDailyPriceRecord(StockDailyPriceRequest request, StockDailyPriceResponse response, 
                                               Object apiResponse, HttpServletRequest httpRequest) {
        try {
            PriceQueryRecord record = PriceQueryRecord.builder()
                    .stockCode(request.getStockCode())
                    .stockName(response.getStockName())
                    .queryType("daily")
                    .startDate(request.getStartDate())
                    .endDate(request.getEndDate())
                    .dailyCount(response.getDailyPrices() != null ? response.getDailyPrices().size() : 0)
                    .errorCode(response.getErrorCode())
                    .errorMessage(response.getErrorMessage())
                    .apiResponse(convertToJson(apiResponse))
                    .userAgent(getUserAgent(httpRequest))
                    .clientIp(getClientIp(httpRequest))
                    .sessionId(getSessionId(httpRequest))
                    .build();

            record.setCreatedAt();
            
            PriceQueryRecord savedRecord = priceQueryRecordRepository.save(record);
            log.info("일자별 시세 조회 기록 저장 완료: {} - {} ({}일)", 
                    savedRecord.getStockCode(), savedRecord.getStockName(), savedRecord.getDailyCount());
            
            return savedRecord;
        } catch (Exception e) {
            log.error("일자별 시세 조회 기록 저장 중 오류 발생: {}", e.getMessage());
            throw new RuntimeException("일자별 시세 조회 기록 저장 실패", e);
        }
    }

    /**
     * 해외 주식 현재가 조회 기록 저장
     */
    public PriceQueryRecord saveForeignCurrentPriceRecord(ForeignStockPriceRequest request, ForeignStockPriceResponse response, Object apiResponse, HttpServletRequest httpRequest) {
        try {
            PriceQueryRecord record = PriceQueryRecord.builder()
                    .stockCode(request.getStockCode())
                    .stockName(response.getCompanyName())
                    .queryType("foreign_current")
                    .currentPrice(response.getCurrentPrice() != null ? response.getCurrentPrice().intValue() : null)
                    .openPrice(response.getOpenPrice() != null ? response.getOpenPrice().intValue() : null)
                    .highPrice(response.getHighPrice() != null ? response.getHighPrice().intValue() : null)
                    .lowPrice(response.getLowPrice() != null ? response.getLowPrice().intValue() : null)
                    .changeRate(response.getChangeRate() != null ? response.getChangeRate().doubleValue() : null)
                    .changeAmount(response.getChangeAmount() != null ? response.getChangeAmount().intValue() : null)
                    .marketStatus(null)
                    .errorCode(null)
                    .errorMessage(null)
                    .apiResponse(convertToJson(apiResponse))
                    .userAgent(getUserAgent(httpRequest))
                    .clientIp(getClientIp(httpRequest))
                    .sessionId(getSessionId(httpRequest))
                    .build();
            record.setCreatedAt();
            return priceQueryRecordRepository.save(record);
        } catch (Exception e) {
            log.error("해외 주식 현재가 조회 기록 저장 중 오류 발생: {}", e.getMessage());
            throw new RuntimeException("해외 주식 현재가 조회 기록 저장 실패", e);
        }
    }

    /**
     * 해외 주식 일자별 시세 조회 기록 저장
     */
    public PriceQueryRecord saveForeignDailyPriceRecord(ForeignStockPriceRequest request, ForeignStockPriceResponse response, Object apiResponse, HttpServletRequest httpRequest) {
        try {
            PriceQueryRecord record = PriceQueryRecord.builder()
                    .stockCode(request.getStockCode())
                    .stockName(response.getCompanyName())
                    .queryType("foreign_daily")
                    .startDate(request.getStartDate())
                    .endDate(request.getEndDate())
                    .dailyCount(response.getDailyData() != null ? response.getDailyData().size() : 0)
                    .errorCode(null)
                    .errorMessage(null)
                    .apiResponse(convertToJson(apiResponse))
                    .userAgent(getUserAgent(httpRequest))
                    .clientIp(getClientIp(httpRequest))
                    .sessionId(getSessionId(httpRequest))
                    .build();
            record.setCreatedAt();
            return priceQueryRecordRepository.save(record);
        } catch (Exception e) {
            log.error("해외 주식 일자별 시세 조회 기록 저장 중 오류 발생: {}", e.getMessage());
            throw new RuntimeException("해외 주식 일자별 시세 조회 기록 저장 실패", e);
        }
    }

    /**
     * 종목코드로 시세 조회 기록 조회
     */
    public List<PriceQueryRecord> findByStockCode(String stockCode) {
        return priceQueryRecordRepository.findByStockCodeOrderByQueryDateTimeDesc(stockCode);
    }

    /**
     * 조회유형으로 시세 조회 기록 조회
     */
    public List<PriceQueryRecord> findByQueryType(String queryType) {
        return priceQueryRecordRepository.findByQueryTypeOrderByQueryDateTimeDesc(queryType);
    }

    /**
     * 종목코드와 조회유형으로 시세 조회 기록 조회
     */
    public List<PriceQueryRecord> findByStockCodeAndQueryType(String stockCode, String queryType) {
        return priceQueryRecordRepository.findByStockCodeAndQueryTypeOrderByQueryDateTimeDesc(stockCode, queryType);
    }

    /**
     * 기간별 시세 조회 기록 조회
     */
    public List<PriceQueryRecord> findByQueryDateTimeBetween(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        return priceQueryRecordRepository.findByQueryDateTimeBetweenOrderByQueryDateTimeDesc(startDateTime, endDateTime);
    }

    /**
     * 종목코드와 기간으로 시세 조회 기록 조회
     */
    public List<PriceQueryRecord> findByStockCodeAndQueryDateTimeBetween(String stockCode, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        return priceQueryRecordRepository.findByStockCodeAndQueryDateTimeBetweenOrderByQueryDateTimeDesc(stockCode, startDateTime, endDateTime);
    }

    /**
     * 조회유형과 기간으로 시세 조회 기록 조회
     */
    public List<PriceQueryRecord> findByQueryTypeAndQueryDateTimeBetween(String queryType, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        return priceQueryRecordRepository.findByQueryTypeAndQueryDateTimeBetweenOrderByQueryDateTimeDesc(queryType, startDateTime, endDateTime);
    }

    /**
     * 에러가 발생한 시세 조회 기록 조회
     */
    public List<PriceQueryRecord> findErrorRecords() {
        return priceQueryRecordRepository.findByErrorCodeIsNotNullOrderByQueryDateTimeDesc();
    }

    /**
     * 성공한 시세 조회 기록 조회
     */
    public List<PriceQueryRecord> findSuccessRecords() {
        return priceQueryRecordRepository.findByErrorCodeIsNullOrderByQueryDateTimeDesc();
    }

    /**
     * 특정 종목의 성공한 시세 조회 기록 조회
     */
    public List<PriceQueryRecord> findSuccessRecordsByStock(String stockCode) {
        return priceQueryRecordRepository.findByStockCodeAndErrorCodeIsNullOrderByQueryDateTimeDesc(stockCode);
    }

    /**
     * 특정 조회유형의 성공한 시세 조회 기록 조회
     */
    public List<PriceQueryRecord> findSuccessRecordsByQueryType(String queryType) {
        return priceQueryRecordRepository.findByQueryTypeAndErrorCodeIsNullOrderByQueryDateTimeDesc(queryType);
    }

    /**
     * 클라이언트 IP로 시세 조회 기록 조회
     */
    public List<PriceQueryRecord> findByClientIp(String clientIp) {
        return priceQueryRecordRepository.findByClientIpOrderByQueryDateTimeDesc(clientIp);
    }

    /**
     * 세션 ID로 시세 조회 기록 조회
     */
    public List<PriceQueryRecord> findBySessionId(String sessionId) {
        return priceQueryRecordRepository.findBySessionIdOrderByQueryDateTimeDesc(sessionId);
    }

    /**
     * 최근 시세 조회 기록 조회 (최근 10개)
     */
    public List<PriceQueryRecord> findRecentRecords() {
        return priceQueryRecordRepository.findTop10ByOrderByQueryDateTimeDesc();
    }

    /**
     * 특정 종목의 최근 시세 조회 기록 조회 (최근 5개)
     */
    public List<PriceQueryRecord> findRecentRecordsByStock(String stockCode) {
        return priceQueryRecordRepository.findTop5ByStockCodeOrderByQueryDateTimeDesc(stockCode);
    }

    /**
     * 특정 종목의 현재가 조회 기록만 조회
     */
    public List<PriceQueryRecord> findCurrentPriceRecordsByStock(String stockCode) {
        return priceQueryRecordRepository.findByStockCodeAndQueryTypeOrderByQueryDateTimeDesc(stockCode, "current");
    }

    /**
     * 특정 종목의 일자별 시세 조회 기록만 조회
     */
    public List<PriceQueryRecord> findDailyPriceRecordsByStock(String stockCode) {
        return priceQueryRecordRepository.findByStockCodeAndQueryTypeAndStartDateIsNotNullOrderByQueryDateTimeDesc(stockCode, "daily");
    }

    /**
     * 시세 조회 통계 조회
     */
    public List<PriceQueryRecord> getQueryStatistics() {
        return priceQueryRecordRepository.findDistinctByStockCodeAndQueryType();
    }

    /**
     * 특정 기간의 시세 조회 기록 수 조회
     */
    public long countByQueryDateTimeBetween(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        return priceQueryRecordRepository.countByQueryDateTimeBetween(startDateTime, endDateTime);
    }

    /**
     * 특정 종목의 시세 조회 기록 수 조회
     */
    public long countByStockCode(String stockCode) {
        return priceQueryRecordRepository.countByStockCode(stockCode);
    }

    /**
     * 특정 조회유형의 시세 조회 기록 수 조회
     */
    public long countByQueryType(String queryType) {
        return priceQueryRecordRepository.countByQueryType(queryType);
    }

    /**
     * 에러가 발생한 시세 조회 기록 수 조회
     */
    public long countErrorRecords() {
        return priceQueryRecordRepository.countByErrorCodeIsNotNull();
    }

    /**
     * 성공한 시세 조회 기록 수 조회
     */
    public long countSuccessRecords() {
        return priceQueryRecordRepository.countByErrorCodeIsNull();
    }

    /**
     * API 응답을 JSON으로 변환
     */
    private String convertToJson(Object apiResponse) {
        try {
            return objectMapper.writeValueAsString(apiResponse);
        } catch (JsonProcessingException e) {
            log.warn("API 응답 JSON 변환 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 사용자 에이전트 가져오기
     */
    private String getUserAgent(HttpServletRequest request) {
        if (request == null) return null;
        return request.getHeader("User-Agent");
    }

    /**
     * 클라이언트 IP 가져오기
     */
    private String getClientIp(HttpServletRequest request) {
        if (request == null) return null;
        
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    /**
     * 세션 ID 가져오기
     */
    private String getSessionId(HttpServletRequest request) {
        if (request == null) return null;
        return request.getSession().getId();
    }
} 