package trade.project.api.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockDailyPriceResponse {
    
    private String stockCode; // 종목코드
    private String stockName; // 종목명
    private String startDate; // 시작일자
    private String endDate; // 종료일자
    private List<DailyPriceData> dailyPrices; // 일자별 시세 데이터
    private String message; // 응답메시지
    private String errorCode; // 에러코드
    private String errorMessage; // 에러메시지
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyPriceData {
        private LocalDate date; // 날짜
        private Integer openPrice; // 시가
        private Integer highPrice; // 고가
        private Integer lowPrice; // 저가
        private Integer closePrice; // 종가
        private Long tradingVolume; // 거래량
        private Long tradingValue; // 거래대금
        private Double changeRate; // 등락률
        private Integer changeAmount; // 등락폭
    }
} 