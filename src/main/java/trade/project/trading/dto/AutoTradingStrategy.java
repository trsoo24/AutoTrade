package trade.project.trading.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import trade.project.trading.enums.TopKospiStocks;
import trade.project.trading.enums.TopNasdaqStocks;
import trade.project.trading.enums.MarketType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 자동매매 전략 설정
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutoTradingStrategy {
    
    // 기본 정보
    private String strategyId;
    private String strategyName;
    private String description;
    private boolean enabled;
    
    // 시장 타입
    private MarketType marketType;
    
    // 대상 종목 (국내)
    private TopKospiStocks targetStock;
    
    // 대상 종목 (해외)
    private TopNasdaqStocks targetForeignStock;
    
    private String stockCode;
    private String stockName;
    
    // 자금 관리
    private BigDecimal totalInvestment; // 총 투자금액
    private BigDecimal maxPositionSize; // 최대 포지션 크기 (총 투자금액 대비 %)
    private BigDecimal minOrderAmount; // 최소 주문 금액
    private BigDecimal maxOrderAmount; // 최대 주문 금액
    
    // 매수 전략
    private BigDecimal buyThreshold; // 매수 임계값 (RSI, MACD 등)
    private BigDecimal buyPriceLimit; // 매수 가격 한도
    private int buyQuantity; // 매수 수량
    private String buyPriceType; // 매수 가격 유형 (지정가/시장가)
    
    // 매도 전략
    private BigDecimal sellThreshold; // 매도 임계값
    private BigDecimal sellPriceLimit; // 매도 가격 한도
    private BigDecimal profitTarget; // 수익 목표 (%)
    private BigDecimal stopLoss; // 손절 기준 (%)
    
    // 기술적 지표 설정
    private int rsiPeriod; // RSI 기간
    private BigDecimal rsiOverbought; // RSI 과매수 기준
    private BigDecimal rsiOversold; // RSI 과매도 기준
    
    private int macdFastPeriod; // MACD 빠른 기간
    private int macdSlowPeriod; // MACD 느린 기간
    private int macdSignalPeriod; // MACD 신호 기간
    
    private int smaShortPeriod; // 단기 이동평균 기간
    private int smaLongPeriod; // 장기 이동평균 기간
    
    // 시간 설정
    private boolean tradeDuringMarketHours; // 장 시간에만 거래
    private boolean tradeDuringHighFrequency; // 고빈도 시간에만 거래
    private int priceCheckInterval; // 시세 조회 주기 (초)
    
    // 리스크 관리
    private BigDecimal maxDailyLoss; // 일일 최대 손실 (%)
    private BigDecimal maxDrawdown; // 최대 낙폭 (%)
    private int maxDailyTrades; // 일일 최대 거래 횟수
    
    // 알림 설정
    private boolean enableNotifications; // 알림 활성화
    private String notificationEmail; // 알림 이메일
    private String notificationPhone; // 알림 전화번호
    
    // 백테스팅 결과
    private BigDecimal backtestReturn; // 백테스팅 수익률
    private BigDecimal backtestSharpeRatio; // 샤프 비율
    private BigDecimal backtestMaxDrawdown; // 최대 낙폭
    private int backtestTotalTrades; // 총 거래 횟수
    
    // 생성/수정 정보
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
    
    /**
     * 기본 전략 생성 (삼성전자)
     */
    public static AutoTradingStrategy createDefaultStrategy() {
        return AutoTradingStrategy.builder()
                .strategyId("DEFAULT_SAMSUNG")
                .strategyName("삼성전자 기본 전략")
                .description("삼성전자 대상 기본 자동매매 전략")
                .enabled(true)
                .targetStock(TopKospiStocks.SAMSUNG_ELECTRONICS)
                .stockCode("005930")
                .stockName("삼성전자")
                .totalInvestment(new BigDecimal("10000000")) // 1천만원
                .maxPositionSize(new BigDecimal("0.1")) // 10%
                .minOrderAmount(new BigDecimal("100000")) // 10만원
                .maxOrderAmount(new BigDecimal("500000")) // 50만원
                .buyThreshold(new BigDecimal("30")) // RSI 30 이하
                .buyPriceLimit(new BigDecimal("80000")) // 8만원 이하
                .buyQuantity(1)
                .buyPriceType("지정가")
                .sellThreshold(new BigDecimal("70")) // RSI 70 이상
                .sellPriceLimit(new BigDecimal("90000")) // 9만원 이상
                .profitTarget(new BigDecimal("5")) // 5% 수익 목표
                .stopLoss(new BigDecimal("3")) // 3% 손절
                .rsiPeriod(14)
                .rsiOverbought(new BigDecimal("70"))
                .rsiOversold(new BigDecimal("30"))
                .macdFastPeriod(12)
                .macdSlowPeriod(26)
                .macdSignalPeriod(9)
                .smaShortPeriod(5)
                .smaLongPeriod(20)
                .tradeDuringMarketHours(true)
                .tradeDuringHighFrequency(true)
                .priceCheckInterval(60) // 1분
                .maxDailyLoss(new BigDecimal("2")) // 2%
                .maxDrawdown(new BigDecimal("10")) // 10%
                .maxDailyTrades(10)
                .enableNotifications(true)
                .createdAt(LocalDateTime.now())
                .build();
    }
    
    /**
     * 보수적 전략 생성
     */
    public static AutoTradingStrategy createConservativeStrategy(TopKospiStocks stock) {
        return AutoTradingStrategy.builder()
                .strategyId("CONSERVATIVE_" + stock.getStockCode())
                .strategyName(stock.getStockName() + " 보수적 전략")
                .description("보수적인 자동매매 전략")
                .enabled(true)
                .targetStock(stock)
                .stockCode(stock.getStockCode())
                .stockName(stock.getStockName())
                .totalInvestment(new BigDecimal("5000000")) // 500만원
                .maxPositionSize(new BigDecimal("0.05")) // 5%
                .minOrderAmount(new BigDecimal("50000")) // 5만원
                .maxOrderAmount(new BigDecimal("200000")) // 20만원
                .buyThreshold(new BigDecimal("25")) // RSI 25 이하
                .buyPriceLimit(new BigDecimal("0")) // 제한 없음
                .buyQuantity(1)
                .buyPriceType("지정가")
                .sellThreshold(new BigDecimal("75")) // RSI 75 이상
                .sellPriceLimit(new BigDecimal("0")) // 제한 없음
                .profitTarget(new BigDecimal("3")) // 3% 수익 목표
                .stopLoss(new BigDecimal("2")) // 2% 손절
                .rsiPeriod(14)
                .rsiOverbought(new BigDecimal("75"))
                .rsiOversold(new BigDecimal("25"))
                .macdFastPeriod(12)
                .macdSlowPeriod(26)
                .macdSignalPeriod(9)
                .smaShortPeriod(10)
                .smaLongPeriod(30)
                .tradeDuringMarketHours(true)
                .tradeDuringHighFrequency(false)
                .priceCheckInterval(300) // 5분
                .maxDailyLoss(new BigDecimal("1")) // 1%
                .maxDrawdown(new BigDecimal("5")) // 5%
                .maxDailyTrades(5)
                .enableNotifications(true)
                .createdAt(LocalDateTime.now())
                .build();
    }
    
    /**
     * 공격적 전략 생성
     */
    public static AutoTradingStrategy createAggressiveStrategy(TopKospiStocks stock) {
        return AutoTradingStrategy.builder()
                .strategyId("AGGRESSIVE_" + stock.getStockCode())
                .strategyName(stock.getStockName() + " 공격적 전략")
                .description("공격적인 자동매매 전략")
                .enabled(true)
                .targetStock(stock)
                .stockCode(stock.getStockCode())
                .stockName(stock.getStockName())
                .totalInvestment(new BigDecimal("20000000")) // 2천만원
                .maxPositionSize(new BigDecimal("0.2")) // 20%
                .minOrderAmount(new BigDecimal("200000")) // 20만원
                .maxOrderAmount(new BigDecimal("1000000")) // 100만원
                .buyThreshold(new BigDecimal("35")) // RSI 35 이하
                .buyPriceLimit(new BigDecimal("0")) // 제한 없음
                .buyQuantity(2)
                .buyPriceType("시장가")
                .sellThreshold(new BigDecimal("65")) // RSI 65 이상
                .sellPriceLimit(new BigDecimal("0")) // 제한 없음
                .profitTarget(new BigDecimal("8")) // 8% 수익 목표
                .stopLoss(new BigDecimal("5")) // 5% 손절
                .rsiPeriod(14)
                .rsiOverbought(new BigDecimal("65"))
                .rsiOversold(new BigDecimal("35"))
                .macdFastPeriod(8)
                .macdSlowPeriod(21)
                .macdSignalPeriod(5)
                .smaShortPeriod(3)
                .smaLongPeriod(10)
                .tradeDuringMarketHours(true)
                .tradeDuringHighFrequency(true)
                .priceCheckInterval(30) // 30초
                .maxDailyLoss(new BigDecimal("3")) // 3%
                .maxDrawdown(new BigDecimal("15")) // 15%
                .maxDailyTrades(20)
                .enableNotifications(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * 해외 기본 전략 생성 (AAPL 등)
     */
    public static AutoTradingStrategy createDefaultStrategy(TopNasdaqStocks stock) {
        return AutoTradingStrategy.builder()
                .strategyId("DEFAULT_" + stock.getStockCode())
                .strategyName(stock.getCompanyName() + " 기본 전략")
                .description(stock.getCompanyName() + " 대상 기본 자동매매 전략")
                .enabled(true)
                .targetForeignStock(stock)
                .stockCode(stock.getStockCode())
                .stockName(stock.getCompanyName())
                .totalInvestment(new BigDecimal("10000")) // $10,000
                .maxPositionSize(new BigDecimal("0.1")) // 10%
                .minOrderAmount(new BigDecimal("1000")) // $1,000
                .maxOrderAmount(new BigDecimal("5000")) // $5,000
                .buyThreshold(new BigDecimal("30"))
                .buyPriceLimit(new BigDecimal("200"))
                .buyQuantity(1)
                .buyPriceType("LIMIT")
                .sellThreshold(new BigDecimal("70"))
                .sellPriceLimit(new BigDecimal("250"))
                .profitTarget(new BigDecimal("5"))
                .stopLoss(new BigDecimal("3"))
                .rsiPeriod(14)
                .rsiOverbought(new BigDecimal("70"))
                .rsiOversold(new BigDecimal("30"))
                .macdFastPeriod(12)
                .macdSlowPeriod(26)
                .macdSignalPeriod(9)
                .smaShortPeriod(5)
                .smaLongPeriod(20)
                .tradeDuringMarketHours(true)
                .tradeDuringHighFrequency(true)
                .priceCheckInterval(60)
                .maxDailyLoss(new BigDecimal("2"))
                .maxDrawdown(new BigDecimal("10"))
                .maxDailyTrades(10)
                .enableNotifications(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * 해외 보수적 전략 생성
     */
    public static AutoTradingStrategy createConservativeStrategy(TopNasdaqStocks stock) {
        return AutoTradingStrategy.builder()
                .strategyId("CONSERVATIVE_" + stock.getStockCode())
                .strategyName(stock.getCompanyName() + " 보수적 전략")
                .description("보수적인 해외 자동매매 전략")
                .enabled(true)
                .targetForeignStock(stock)
                .stockCode(stock.getStockCode())
                .stockName(stock.getCompanyName())
                .totalInvestment(new BigDecimal("5000")) // $5,000
                .maxPositionSize(new BigDecimal("0.05")) // 5%
                .minOrderAmount(new BigDecimal("500")) // $500
                .maxOrderAmount(new BigDecimal("2000")) // $2,000
                .buyThreshold(new BigDecimal("25"))
                .buyPriceLimit(new BigDecimal("0"))
                .buyQuantity(1)
                .buyPriceType("LIMIT")
                .sellThreshold(new BigDecimal("75"))
                .sellPriceLimit(new BigDecimal("0"))
                .profitTarget(new BigDecimal("3"))
                .stopLoss(new BigDecimal("2"))
                .rsiPeriod(14)
                .rsiOverbought(new BigDecimal("75"))
                .rsiOversold(new BigDecimal("25"))
                .macdFastPeriod(12)
                .macdSlowPeriod(26)
                .macdSignalPeriod(9)
                .smaShortPeriod(10)
                .smaLongPeriod(30)
                .tradeDuringMarketHours(true)
                .tradeDuringHighFrequency(false)
                .priceCheckInterval(300)
                .maxDailyLoss(new BigDecimal("1"))
                .maxDrawdown(new BigDecimal("5"))
                .maxDailyTrades(5)
                .enableNotifications(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * 해외 공격적 전략 생성
     */
    public static AutoTradingStrategy createAggressiveStrategy(TopNasdaqStocks stock) {
        return AutoTradingStrategy.builder()
                .strategyId("AGGRESSIVE_" + stock.getStockCode())
                .strategyName(stock.getCompanyName() + " 공격적 전략")
                .description("공격적인 해외 자동매매 전략")
                .enabled(true)
                .targetForeignStock(stock)
                .stockCode(stock.getStockCode())
                .stockName(stock.getCompanyName())
                .totalInvestment(new BigDecimal("20000")) // $20,000
                .maxPositionSize(new BigDecimal("0.2")) // 20%
                .minOrderAmount(new BigDecimal("2000")) // $2,000
                .maxOrderAmount(new BigDecimal("10000")) // $10,000
                .buyThreshold(new BigDecimal("35"))
                .buyPriceLimit(new BigDecimal("0"))
                .buyQuantity(2)
                .buyPriceType("MARKET")
                .sellThreshold(new BigDecimal("65"))
                .sellPriceLimit(new BigDecimal("0"))
                .profitTarget(new BigDecimal("8"))
                .stopLoss(new BigDecimal("5"))
                .rsiPeriod(14)
                .rsiOverbought(new BigDecimal("65"))
                .rsiOversold(new BigDecimal("35"))
                .macdFastPeriod(8)
                .macdSlowPeriod(21)
                .macdSignalPeriod(5)
                .smaShortPeriod(3)
                .smaLongPeriod(10)
                .tradeDuringMarketHours(true)
                .tradeDuringHighFrequency(true)
                .priceCheckInterval(30)
                .maxDailyLoss(new BigDecimal("3"))
                .maxDrawdown(new BigDecimal("15"))
                .maxDailyTrades(20)
                .enableNotifications(true)
                .createdAt(LocalDateTime.now())
                .build();
    }
} 