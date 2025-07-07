package trade.project.api.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 해외 주식 시세 조회 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForeignStockPriceResponse {
    
    private String stockCode;
    private String companyName;
    private String koreanName;
    private BigDecimal currentPrice;
    private BigDecimal changeAmount;
    private BigDecimal changeRate;
    private BigDecimal openPrice;
    private BigDecimal highPrice;
    private BigDecimal lowPrice;
    private Long volume;
    private BigDecimal marketCap;
    private String currency;
    private String exchange;
    private LocalDateTime timestamp;
    private List<DailyPriceData> dailyData;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyPriceData {
        private String date;
        private BigDecimal openPrice;
        private BigDecimal closePrice;
        private BigDecimal highPrice;
        private BigDecimal lowPrice;
        private Long volume;
        private BigDecimal changeAmount;
        private BigDecimal changeRate;
    }
} 