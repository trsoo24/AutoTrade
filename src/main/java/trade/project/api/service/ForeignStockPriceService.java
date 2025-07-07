package trade.project.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import trade.project.api.client.ForeignStockApiClient;
import trade.project.api.dto.ForeignStockPriceRequest;
import trade.project.api.dto.ForeignStockPriceResponse;
import trade.project.common.dto.ApiResponse;
import trade.project.trading.enums.TopNasdaqStocks;
import trade.project.trading.service.PriceQueryRecordService;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 해외 주식 시세 조회 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ForeignStockPriceService {
    
    private final ForeignStockApiClient foreignStockApiClient;
    private final PriceQueryRecordService priceQueryRecordService;
    
    /**
     * 해외 주식 현재가 조회
     */
    public ApiResponse<ForeignStockPriceResponse> getCurrentPrice(ForeignStockPriceRequest request, HttpServletRequest httpRequest) {
        try {
            log.info("해외 주식 현재가 조회 요청: {}", request.getStockCode());
            
            // API 호출
            ApiResponse<Map<String, Object>> apiResponse = foreignStockApiClient.getCurrentPrice(request.getStockCode());
            
            if (!apiResponse.isSuccess()) {
                log.error("해외 주식 API 호출 실패: {}", apiResponse.getErrorMessage());
                return ApiResponse.error("FOREIGN_STOCK_API_ERROR", apiResponse.getErrorMessage());
            }
            
            // 응답 데이터 변환
            Map<String, Object> data = apiResponse.getData();
            ForeignStockPriceResponse response = convertToPriceResponse(data);
            
            // 시세 조회 기록 저장
            priceQueryRecordService.saveForeignCurrentPriceRecord(request, response, apiResponse, httpRequest);
            
            log.info("해외 주식 현재가 조회 성공: {}", request.getStockCode());
            return ApiResponse.success(response);
            
        } catch (Exception e) {
            log.error("해외 주식 현재가 조회 실패: {}, 에러: {}", request.getStockCode(), e.getMessage());
            
            // 시세 조회 기록 저장 (실패)
            priceQueryRecordService.saveForeignCurrentPriceRecord(request, null, null, httpRequest);
            
            return ApiResponse.error("FOREIGN_STOCK_PRICE_ERROR", "해외 주식 현재가 조회에 실패했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 해외 주식 일자별 시세 조회
     */
    public ApiResponse<ForeignStockPriceResponse> getDailyPrice(ForeignStockPriceRequest request, HttpServletRequest httpRequest) {
        try {
            log.info("해외 주식 일자별 시세 조회 요청: {}, 기간: {} ~ {}", 
                    request.getStockCode(), request.getStartDate(), request.getEndDate());
            
            // API 호출
            ApiResponse<Map<String, Object>> apiResponse = foreignStockApiClient.getDailyPrice(
                    request.getStockCode(), request.getStartDate(), request.getEndDate());
            
            if (!apiResponse.isSuccess()) {
                log.error("해외 주식 API 호출 실패: {}", apiResponse.getErrorMessage());
                return ApiResponse.error("FOREIGN_STOCK_API_ERROR", apiResponse.getErrorMessage());
            }
            
            // 응답 데이터 변환
            Map<String, Object> data = apiResponse.getData();
            ForeignStockPriceResponse response = convertToPriceResponse(data);
            
            // 시세 조회 기록 저장
            priceQueryRecordService.saveForeignDailyPriceRecord(request, response, apiResponse, httpRequest);
            
            log.info("해외 주식 일자별 시세 조회 성공: {}", request.getStockCode());
            return ApiResponse.success(response);
            
        } catch (Exception e) {
            log.error("해외 주식 일자별 시세 조회 실패: {}, 에러: {}", request.getStockCode(), e.getMessage());
            
            // 시세 조회 기록 저장 (실패)
            priceQueryRecordService.saveForeignDailyPriceRecord(request, null, null, httpRequest);
            
            return ApiResponse.error("FOREIGN_STOCK_DAILY_ERROR", "해외 주식 일자별 시세 조회에 실패했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 특정 종목 현재가 조회 (GET 방식)
     */
    public ApiResponse<ForeignStockPriceResponse> getCurrentPriceByCode(String stockCode, HttpServletRequest httpRequest) {
        ForeignStockPriceRequest request = ForeignStockPriceRequest.builder()
                .stockCode(stockCode)
                .build();
        return getCurrentPrice(request, httpRequest);
    }
    
    /**
     * 애플 현재가 조회 테스트
     */
    public ApiResponse<ForeignStockPriceResponse> getAppleCurrentPrice(HttpServletRequest httpRequest) {
        return getCurrentPriceByCode("AAPL", httpRequest);
    }
    
    /**
     * 마이크로소프트 현재가 조회 테스트
     */
    public ApiResponse<ForeignStockPriceResponse> getMicrosoftCurrentPrice(HttpServletRequest httpRequest) {
        return getCurrentPriceByCode("MSFT", httpRequest);
    }
    
    /**
     * 구글 현재가 조회 테스트
     */
    public ApiResponse<ForeignStockPriceResponse> getGoogleCurrentPrice(HttpServletRequest httpRequest) {
        return getCurrentPriceByCode("GOOGL", httpRequest);
    }
    
    /**
     * 애플 일자별 시세 조회 테스트
     */
    public ApiResponse<ForeignStockPriceResponse> getAppleDailyPrice(HttpServletRequest httpRequest) {
        ForeignStockPriceRequest request = ForeignStockPriceRequest.builder()
                .stockCode("AAPL")
                .startDate("2023-12-01")
                .endDate("2023-12-07")
                .build();
        return getDailyPrice(request, httpRequest);
    }
    
    /**
     * API 응답을 DTO로 변환
     */
    private ForeignStockPriceResponse convertToPriceResponse(Map<String, Object> data) {
        try {
            // 종목 정보 가져오기
            TopNasdaqStocks stock = TopNasdaqStocks.fromStockCode((String) data.get("stockCode"));
            
            return ForeignStockPriceResponse.builder()
                    .stockCode((String) data.get("stockCode"))
                    .companyName(stock.getCompanyName())
                    .koreanName(stock.getKoreanName())
                    .currentPrice(new BigDecimal(data.get("currentPrice").toString()))
                    .changeAmount(new BigDecimal(data.get("changeAmount").toString()))
                    .changeRate(new BigDecimal(data.get("changeRate").toString()))
                    .openPrice(new BigDecimal(data.get("openPrice").toString()))
                    .highPrice(new BigDecimal(data.get("highPrice").toString()))
                    .lowPrice(new BigDecimal(data.get("lowPrice").toString()))
                    .volume(Long.valueOf(data.get("volume").toString()))
                    .marketCap(new BigDecimal(data.get("marketCap").toString()))
                    .currency((String) data.get("currency"))
                    .exchange((String) data.get("exchange"))
                    .timestamp(LocalDateTime.now())
                    .build();
                    
        } catch (Exception e) {
            log.error("응답 데이터 변환 실패: {}", e.getMessage());
            throw new RuntimeException("응답 데이터 변환 실패", e);
        }
    }
} 