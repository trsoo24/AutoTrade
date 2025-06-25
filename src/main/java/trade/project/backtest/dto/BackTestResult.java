package trade.project.backtest.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackTestResult {
    
    // 기본 정보
    private String stockCode;
    private LocalDate startDate;
    private LocalDate endDate;
    private String strategy;
    
    // 자본금 정보
    private BigDecimal initialCapital;
    private BigDecimal finalCapital;
    private BigDecimal totalReturn;
    private BigDecimal totalReturnPercent;
    
    // 수익률 정보
    private BigDecimal annualizedReturn;
    private BigDecimal maxDrawdown;
    private BigDecimal maxDrawdownPercent;
    private BigDecimal sharpeRatio;
    private BigDecimal volatility;
    
    // 거래 정보
    private Integer totalTrades;
    private Integer winningTrades;
    private Integer losingTrades;
    private BigDecimal winRate;
    private BigDecimal averageWin;
    private BigDecimal averageLoss;
    private BigDecimal profitFactor;
    
    // 포트폴리오 정보
    private BigDecimal peakCapital;
    private LocalDate peakDate;
    private LocalDate maxDrawdownDate;
    
    // 거래 내역
    private List<TradeRecord> trades;
    private List<PortfolioSnapshot> portfolioHistory;
    
    // 성과 지표
    private BigDecimal calmarRatio;
    private BigDecimal sortinoRatio;
    private BigDecimal informationRatio;
    private BigDecimal treynorRatio;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TradeRecord {
        private LocalDate date;
        private String action; // BUY, SELL
        private BigDecimal price;
        private Integer quantity;
        private BigDecimal amount;
        private BigDecimal commission;
        private BigDecimal balance;
        private BigDecimal position;
        private String reason; // 전략 신호
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PortfolioSnapshot {
        private LocalDate date;
        private BigDecimal capital;
        private BigDecimal position;
        private BigDecimal totalValue;
        private BigDecimal returnPercent;
        private BigDecimal drawdown;
        private BigDecimal drawdownPercent;
    }
} 