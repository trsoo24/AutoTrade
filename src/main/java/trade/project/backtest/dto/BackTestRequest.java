package trade.project.backtest.dto;


import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackTestRequest {
    
    // 기본 설정
    private String stockCode;           // 주식 코드
    private LocalDate startDate;        // 시작 날짜
    private LocalDate endDate;          // 종료 날짜
    private BigDecimal initialCapital;  // 초기 자본금
    private BigDecimal commission;      // 수수료율 (0.001 = 0.1%)
    
    // 전략 설정
    private String strategy;            // 전략 타입 (SMA, RSI, MACD, CUSTOM)
    
    // 이동평균선 전략 설정
    private Integer shortPeriod;        // 단기 이동평균 기간
    private Integer longPeriod;         // 장기 이동평균 기간
    
    // RSI 전략 설정
    private Integer rsiPeriod;          // RSI 계산 기간
    private Integer rsiOverbought;      // 과매수 기준 (기본값: 70)
    private Integer rsiOversold;        // 과매도 기준 (기본값: 30)
    
    // MACD 전략 설정
    private Integer macdFastPeriod;     // MACD 빠른선 기간
    private Integer macdSlowPeriod;     // MACD 느린선 기간
    private Integer macdSignalPeriod;   // MACD 시그널 기간
    
    // 리스크 관리 설정
    private BigDecimal stopLoss;        // 손절 비율 (0.05 = 5%)
    private BigDecimal takeProfit;      // 익절 비율 (0.10 = 10%)
    private BigDecimal maxPositionSize; // 최대 포지션 크기 (0.5 = 50%)
    
    // 거래 설정
    private BigDecimal minTradeAmount;  // 최소 거래 금액
    private Boolean reinvestDividends;  // 배당금 재투자 여부
    private Boolean includeTax;         // 세금 포함 여부
    
    // 기본값 설정
    public static BackTestRequest getDefault() {
        return BackTestRequest.builder()
                .commission(new BigDecimal("0.001"))
                .strategy("SMA")
                .shortPeriod(5)
                .longPeriod(20)
                .rsiPeriod(14)
                .rsiOverbought(70)
                .rsiOversold(30)
                .macdFastPeriod(12)
                .macdSlowPeriod(26)
                .macdSignalPeriod(9)
                .stopLoss(new BigDecimal("0.05"))
                .takeProfit(new BigDecimal("0.10"))
                .maxPositionSize(new BigDecimal("0.5"))
                .minTradeAmount(new BigDecimal("100000"))
                .reinvestDividends(true)
                .includeTax(true)
                .build();
    }
    
    /**
     * 현재 요청에 기본값을 적용합니다.
     * @return 기본값이 적용된 새로운 BackTestRequest 객체
     */
    public BackTestRequest applyDefaults() {
        BackTestRequest defaultRequest = getDefault();
        
        return BackTestRequest.builder()
                .stockCode(this.stockCode)
                .startDate(this.startDate)
                .endDate(this.endDate)
                .initialCapital(this.initialCapital)
                .commission(this.commission != null ? this.commission : defaultRequest.getCommission())
                .strategy(this.strategy != null ? this.strategy : defaultRequest.getStrategy())
                .shortPeriod(this.shortPeriod != null ? this.shortPeriod : defaultRequest.getShortPeriod())
                .longPeriod(this.longPeriod != null ? this.longPeriod : defaultRequest.getLongPeriod())
                .rsiPeriod(this.rsiPeriod != null ? this.rsiPeriod : defaultRequest.getRsiPeriod())
                .rsiOverbought(this.rsiOverbought != null ? this.rsiOverbought : defaultRequest.getRsiOverbought())
                .rsiOversold(this.rsiOversold != null ? this.rsiOversold : defaultRequest.getRsiOversold())
                .macdFastPeriod(this.macdFastPeriod != null ? this.macdFastPeriod : defaultRequest.getMacdFastPeriod())
                .macdSlowPeriod(this.macdSlowPeriod != null ? this.macdSlowPeriod : defaultRequest.getMacdSlowPeriod())
                .macdSignalPeriod(this.macdSignalPeriod != null ? this.macdSignalPeriod : defaultRequest.getMacdSignalPeriod())
                .stopLoss(this.stopLoss != null ? this.stopLoss : defaultRequest.getStopLoss())
                .takeProfit(this.takeProfit != null ? this.takeProfit : defaultRequest.getTakeProfit())
                .maxPositionSize(this.maxPositionSize != null ? this.maxPositionSize : defaultRequest.getMaxPositionSize())
                .minTradeAmount(this.minTradeAmount != null ? this.minTradeAmount : defaultRequest.getMinTradeAmount())
                .reinvestDividends(this.reinvestDividends != null ? this.reinvestDividends : defaultRequest.getReinvestDividends())
                .includeTax(this.includeTax != null ? this.includeTax : defaultRequest.getIncludeTax())
                .build();
    }
} 