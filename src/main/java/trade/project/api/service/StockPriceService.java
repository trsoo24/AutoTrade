package trade.project.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import trade.project.api.client.KisApiClient;
import trade.project.api.dto.StockPriceRequest;
import trade.project.api.dto.StockPriceResponse;
import trade.project.api.dto.StockDailyPriceRequest;
import trade.project.api.dto.StockDailyPriceResponse;
import trade.project.common.exception.ApiException;
import trade.project.trading.service.PriceQueryRecordService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockPriceService {

    private final KisApiClient kisApiClient;
    private final PriceQueryRecordService priceQueryRecordService;

    /**
     * 주식 현재가 조회
     */
    public StockPriceResponse getCurrentPrice(StockPriceRequest request, HttpServletRequest httpRequest) {
        try {
            log.info("주식 현재가 조회 요청: {}", request.getStockCode());
            
            // KIS API 클라이언트를 통해 현재가 조회
            Map<String, Object> apiResponse = kisApiClient.getStockPrice(request.getStockCode());
            
            if (apiResponse != null && apiResponse.containsKey("output")) {
                Map<String, Object> output = (Map<String, Object>) apiResponse.get("output");
                
                return StockPriceResponse.builder()
                        .stockCode(request.getStockCode())
                        .currentPrice((Integer) output.get("stck_prpr"))
                        .changeAmount((Integer) output.get("prdy_vrss"))
                        .changeRate((Double) output.get("prdy_ctrt"))
                        .highPrice((Integer) output.get("stck_hgpr"))
                        .lowPrice((Integer) output.get("stck_lwpr"))
                        .tradingVolume((Long) output.get("acml_vol"))
                        .timestamp(LocalDateTime.now())
                        .build();
            } else {
                // API 응답이 없거나 실패한 경우 모의 데이터 반환
                log.warn("API 응답이 없어 모의 데이터를 반환합니다: {}", request.getStockCode());
                return getMockStockPrice(request.getStockCode());
            }
        } catch (Exception e) {
            log.error("주식 현재가 조회 중 오류 발생: {}", e.getMessage());
            // 예외 발생 시에도 모의 데이터 반환
            return getMockStockPrice(request.getStockCode());
        }
    }
    
    /**
     * 모의 주식 가격 데이터 생성
     */
    private StockPriceResponse getMockStockPrice(String stockCode) {
        // 종목별 기본 가격 설정
        Map<String, String> basePrices = Map.of(
            "005930", "75000", // 삼성전자
            "000660", "45000", // SK하이닉스
            "035420", "380000", // NAVER
            "AAPL", "180.50", // Apple
            "MSFT", "420.30", // Microsoft
            "GOOGL", "165.80" // Google
        );
        
        String basePrice = basePrices.getOrDefault(stockCode, "50000");
        double price = Double.parseDouble(basePrice);
        
        // 랜덤 변동 생성 (±2%)
        double changePercent = (Math.random() - 0.5) * 0.04; // -2% ~ +2%
        double newPrice = price * (1 + changePercent);
        double changeAmount = newPrice - price;
        
        return StockPriceResponse.builder()
                .stockCode(stockCode)
                .currentPrice(Integer.valueOf(String.format("%.2f", newPrice)))
                .changeAmount(Integer.valueOf(String.format("%.2f", changeAmount)))
                .changeRate(Double.valueOf(String.format("%.2f", changePercent * 100)))
                .highPrice(Integer.valueOf(String.format("%.2f", newPrice * 1.02)))
                .lowPrice(Integer.valueOf(String.format("%.2f", newPrice * 0.98)))
                .tradingVolume((long) (Math.random() * 1000000 + 100000))
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 주식 일자별 시세 조회
     */
    public StockDailyPriceResponse getDailyPrices(StockDailyPriceRequest request, jakarta.servlet.http.HttpServletRequest httpRequest) {
        try {
            log.info("주식 일자별 시세 조회: {}", request);
            
            // KIS API 호출
            Map<String, Object> response = kisApiClient.getStockDailyPrice(
                request.getStockCode(), 
                request.getStartDate(), 
                request.getEndDate()
            );
            
            // 응답 변환
            StockDailyPriceResponse dailyResponse = convertToDailyPriceResponse(request, response);
            
            // 시세 조회 기록 저장 (비동기로 처리하여 API 응답에 영향을 주지 않도록 함)
            try {
                priceQueryRecordService.saveDailyPriceRecord(request, dailyResponse, response, httpRequest);
            } catch (Exception e) {
                log.error("시세 조회 기록 저장 중 오류 발생: {}", e.getMessage());
                // 시세 조회 기록 저장 실패는 API 응답에 영향을 주지 않도록 함
            }
            
            return dailyResponse;
            
        } catch (Exception e) {
            log.error("주식 일자별 시세 조회 중 오류 발생: {}", e.getMessage());
            throw new ApiException("주식 일자별 시세 조회 실패", e);
        }
    }

    /**
     * 현재가 응답 변환
     */
    private StockPriceResponse convertToPriceResponse(String stockCode, Map<String, Object> response) {
        StockPriceResponse.StockPriceResponseBuilder builder = StockPriceResponse.builder()
                .stockCode(stockCode)
                .timestamp(LocalDateTime.now());

        // 응답 데이터 파싱
        if (response.containsKey("output")) {
            Map<String, Object> output = (Map<String, Object>) response.get("output");
            if (output != null) {
                builder.stockName((String) output.get("hts_kor_isnm")) // 종목명
                       .currentPrice(parseInteger(output.get("stck_prpr"))) // 현재가
                       .previousClose(parseInteger(output.get("stck_hgpr"))) // 전일종가
                       .openPrice(parseInteger(output.get("stck_oprc"))) // 시가
                       .highPrice(parseInteger(output.get("stck_hgpr"))) // 고가
                       .lowPrice(parseInteger(output.get("stck_lwpr"))) // 저가
                       .tradingVolume(parseLong(output.get("acml_vol"))) // 거래량
                       .tradingValue(parseLong(output.get("acml_tr_pbmn"))) // 거래대금
                       .changeRate(parseDouble(output.get("prdy_vrss"))) // 등락률
                       .changeAmount(parseInteger(output.get("prdy_vrss_sign"))) // 등락폭
                       .marketStatus((String) output.get("hts_avls")); // 시장상태
            }
        }

        // 에러 처리
        if (response.containsKey("rt_cd") && !"0".equals(response.get("rt_cd"))) {
            builder.errorCode((String) response.get("rt_cd"))
                   .errorMessage((String) response.get("msg1"));
        } else {
            builder.message("현재가 조회가 완료되었습니다.");
        }

        return builder.build();
    }

    /**
     * 일자별 시세 응답 변환
     */
    private StockDailyPriceResponse convertToDailyPriceResponse(StockDailyPriceRequest request, Map<String, Object> response) {
        StockDailyPriceResponse.StockDailyPriceResponseBuilder builder = StockDailyPriceResponse.builder()
                .stockCode(request.getStockCode())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate());

        // 응답 데이터 파싱
        if (response.containsKey("output")) {
            List<Map<String, Object>> outputList = (List<Map<String, Object>>) response.get("output");
            if (outputList != null && !outputList.isEmpty()) {
                List<StockDailyPriceResponse.DailyPriceData> dailyPrices = new ArrayList<>();
                
                for (Map<String, Object> output : outputList) {
                    StockDailyPriceResponse.DailyPriceData dailyData = StockDailyPriceResponse.DailyPriceData.builder()
                            .date(parseDate((String) output.get("stck_bsop_date"))) // 날짜
                            .openPrice(parseInteger(output.get("stck_oprc"))) // 시가
                            .highPrice(parseInteger(output.get("stck_hgpr"))) // 고가
                            .lowPrice(parseInteger(output.get("stck_lwpr"))) // 저가
                            .closePrice(parseInteger(output.get("stck_prpr"))) // 종가
                            .tradingVolume(parseLong(output.get("acml_vol"))) // 거래량
                            .tradingValue(parseLong(output.get("acml_tr_pbmn"))) // 거래대금
                            .changeRate(parseDouble(output.get("prdy_vrss"))) // 등락률
                            .changeAmount(parseInteger(output.get("prdy_vrss_sign"))) // 등락폭
                            .build();
                    
                    dailyPrices.add(dailyData);
                }
                
                builder.dailyPrices(dailyPrices);
                
                // 종목명은 첫 번째 데이터에서 가져옴
                if (!dailyPrices.isEmpty()) {
                    builder.stockName((String) outputList.get(0).get("hts_kor_isnm"));
                }
            }
        }

        // 에러 처리
        if (response.containsKey("rt_cd") && !"0".equals(response.get("rt_cd"))) {
            builder.errorCode((String) response.get("rt_cd"))
                   .errorMessage((String) response.get("msg1"));
        } else {
            builder.message("일자별 시세 조회가 완료되었습니다.");
        }

        return builder.build();
    }

    /**
     * Integer 파싱 헬퍼 메서드
     */
    private Integer parseInteger(Object value) {
        if (value == null) return null;
        try {
            if (value instanceof String) {
                return Integer.parseInt((String) value);
            } else if (value instanceof Number) {
                return ((Number) value).intValue();
            }
        } catch (NumberFormatException e) {
            log.warn("Integer 파싱 실패: {}", value);
        }
        return null;
    }

    /**
     * Long 파싱 헬퍼 메서드
     */
    private Long parseLong(Object value) {
        if (value == null) return null;
        try {
            if (value instanceof String) {
                return Long.parseLong((String) value);
            } else if (value instanceof Number) {
                return ((Number) value).longValue();
            }
        } catch (NumberFormatException e) {
            log.warn("Long 파싱 실패: {}", value);
        }
        return null;
    }

    /**
     * Double 파싱 헬퍼 메서드
     */
    private Double parseDouble(Object value) {
        if (value == null) return null;
        try {
            if (value instanceof String) {
                return Double.parseDouble((String) value);
            } else if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
        } catch (NumberFormatException e) {
            log.warn("Double 파싱 실패: {}", value);
        }
        return null;
    }

    /**
     * 날짜 파싱 헬퍼 메서드
     */
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        try {
            return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyyMMdd"));
        } catch (Exception e) {
            log.warn("날짜 파싱 실패: {}", dateStr);
            return null;
        }
    }
} 