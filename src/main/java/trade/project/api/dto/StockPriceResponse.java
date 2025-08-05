package trade.project.api.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockPriceResponse {
    
    private String stockCode; // 종목코드
    private String stockName; // 종목명
    private Integer currentPrice; // 현재가
    private Integer previousClose; // 전일종가
    private Integer openPrice; // 시가
    private Integer highPrice; // 고가
    private Integer lowPrice; // 저가
    private Long tradingVolume; // 거래량
    private Long tradingValue; // 거래대금
    private Double changeRate; // 등락률
    private Integer changeAmount; // 등락폭
    private String marketStatus; // 시장상태
    private LocalDateTime timestamp; // 조회시간
    private String message; // 응답메시지
    private String errorCode; // 에러코드
    private String errorMessage; // 에러메시지
} 