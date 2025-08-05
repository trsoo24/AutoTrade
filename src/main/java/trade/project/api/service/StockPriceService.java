package trade.project.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import trade.project.api.client.KisApiClient;
import trade.project.api.dto.StockPriceRequest;
import trade.project.api.dto.StockPriceResponse;
import trade.project.common.exception.ApiException;
import trade.project.trading.service.PriceQueryRecordService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockPriceService {
    private final KisApiClient kisApiClient;
    private final PriceQueryRecordService priceQueryRecordService;
    private final StockInfoCacheService stockInfoCacheService;

    public StockPriceResponse getCurrentPrice(StockPriceRequest request, HttpServletRequest httpRequest) {
        try {
            log.info("주식 현재가 조회 요청: {}", request.getStockCode());
            Map<String, Object> apiResponse = kisApiClient.getStockPrice(request.getStockCode());
            StockPriceResponse response;
            if (apiResponse != null && apiResponse.containsKey("output")) {
                Map<String, Object> output = (Map<String, Object>) apiResponse.get("output");
                log.info("output response : {}", output);
                response = StockPriceResponse.builder()
                        .stockCode(request.getStockCode())
                        .stockName(stockInfoCacheService.getStockName(request.getStockCode()))
                        .currentPrice(parseInteger(output.get("stck_prpr")))
                        .changeAmount(parseInteger(output.get("prdy_vrss")))
                        .changeRate(parseDouble(output.get("prdy_ctrt")))
                        .highPrice(parseInteger(output.get("stck_hgpr")))
                        .lowPrice(parseInteger(output.get("stck_lwpr")))
                        .openPrice(parseInteger(output.get("stck_oprc")))
                        .build();
            } else {
                response = getMockStockPrice(request.getStockCode());
            }
            // MongoDB 저장 (종목명 포함)
            try {
                priceQueryRecordService.saveCurrentPriceRecord(request, response, apiResponse, httpRequest);
            } catch (Exception e) {
                log.error("MongoDB 저장 중 오류: {}", e.getMessage());
            }
            return response;
        } catch (Exception e) {
            log.error("주식 현재가 조회 중 오류 발생: {}", e.getMessage());
            throw new ApiException("주식 현재가 조회 실패", e);
        }
    }

    // 이하 parseInteger, parseDouble, parseLong, getMockStockPrice 등 기존 유틸 메서드 복구
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
    private StockPriceResponse getMockStockPrice(String stockCode) {
        return StockPriceResponse.builder()
                .stockCode(stockCode)
                .stockName(stockInfoCacheService.getStockName(stockCode))
                .currentPrice(10000)
                .changeAmount(0)
                .changeRate(0.0)
                .highPrice(10000)
                .lowPrice(10000)
                .openPrice(10000)
                .build();
    }
} 