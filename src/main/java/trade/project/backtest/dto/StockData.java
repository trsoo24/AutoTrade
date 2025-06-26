package trade.project.backtest.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockData {
    
    private LocalDate date;
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal close;
    private Long volume;
    private BigDecimal adjustedClose;
    
    // 기술적 지표 (계산된 값들을 저장)
    private BigDecimal sma5;
    private BigDecimal sma10;
    private BigDecimal sma20;
    private BigDecimal sma50;
    private BigDecimal sma200;
    
    private BigDecimal ema12;
    private BigDecimal ema26;
    
    private BigDecimal rsi;
    private BigDecimal macd;
    private BigDecimal macdSignal;
    private BigDecimal macdHistogram;
    
    private BigDecimal bollingerUpper;
    private BigDecimal bollingerMiddle;
    private BigDecimal bollingerLower;
    
    private BigDecimal atr; // Average True Range
    
    // 거래 신호
    private String signal; // BUY, SELL, HOLD
    
    // 가격 변화율
    private BigDecimal dailyReturn;
    private BigDecimal cumulativeReturn;
    
    // 거래량 지표
    private BigDecimal volumeSma;
    private BigDecimal volumeRatio;
    
    // 변동성 지표
    private BigDecimal volatility;
    private BigDecimal beta;
} 