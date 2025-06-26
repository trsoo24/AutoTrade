package trade.project.backtest.util;

import trade.project.backtest.dto.StockData;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.ArrayList;

/**
 * 기술적 지표 계산을 담당하는 유틸리티 클래스
 */
public class TechnicalIndicatorCalculator {
    
    /**
     * 이동평균선(SMA) 계산
     */
    public static BigDecimal calculateSMA(List<StockData> data, int period) {
        if (data.size() < period) return null;
        
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = data.size() - period; i < data.size(); i++) {
            sum = sum.add(data.get(i).getClose());
        }
        
        return sum.divide(BigDecimal.valueOf(period), 2, RoundingMode.HALF_UP);
    }
    
    /**
     * 지수이동평균선(EMA) 계산
     */
    public static BigDecimal calculateEMA(List<BigDecimal> data, int period) {
        if (data.size() < period) return null;
        
        BigDecimal multiplier = BigDecimal.valueOf(2.0 / (period + 1));
        BigDecimal ema = data.get(period - 1);
        
        for (int i = period; i < data.size(); i++) {
            ema = data.get(i).multiply(multiplier)
                    .add(ema.multiply(BigDecimal.ONE.subtract(multiplier)));
        }
        
        return ema;
    }
    
    /**
     * RSI 계산
     */
    public static BigDecimal calculateRSI(List<StockData> data, int period) {
        if (data.size() < period + 1) return null;
        
        BigDecimal gains = BigDecimal.ZERO;
        BigDecimal losses = BigDecimal.ZERO;
        
        for (int i = data.size() - period; i < data.size(); i++) {
            BigDecimal change = data.get(i).getClose().subtract(data.get(i - 1).getClose());
            if (change.compareTo(BigDecimal.ZERO) > 0) {
                gains = gains.add(change);
            } else {
                losses = losses.add(change.abs());
            }
        }
        
        BigDecimal avgGain = gains.divide(BigDecimal.valueOf(period), 4, RoundingMode.HALF_UP);
        BigDecimal avgLoss = losses.divide(BigDecimal.valueOf(period), 4, RoundingMode.HALF_UP);
        
        if (avgLoss.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.valueOf(100);
        } else {
            BigDecimal rs = avgGain.divide(avgLoss, 4, RoundingMode.HALF_UP);
            return BigDecimal.valueOf(100).subtract(
                BigDecimal.valueOf(100).divide(BigDecimal.ONE.add(rs), 2, RoundingMode.HALF_UP)
            );
        }
    }
    
    /**
     * MACD 계산
     */
    public static MACDResult calculateMACD(List<StockData> data, int fastPeriod, int slowPeriod, int signalPeriod) {
        if (data.size() < slowPeriod) return null;
        
        // EMA 계산
        BigDecimal ema12 = calculateEMAFromStockData(data, fastPeriod);
        BigDecimal ema26 = calculateEMAFromStockData(data, slowPeriod);
        
        if (ema12 == null || ema26 == null) return null;
        
        BigDecimal macd = ema12.subtract(ema26);
        
        // MACD Signal 계산
        BigDecimal macdSignal = null;
        if (data.size() >= slowPeriod + signalPeriod) {
            List<BigDecimal> macdValues = new ArrayList<>();
            for (int i = data.size() - signalPeriod; i < data.size(); i++) {
                BigDecimal fastEMA = calculateEMAFromStockData(data.subList(0, i + 1), fastPeriod);
                BigDecimal slowEMA = calculateEMAFromStockData(data.subList(0, i + 1), slowPeriod);
                if (fastEMA != null && slowEMA != null) {
                    macdValues.add(fastEMA.subtract(slowEMA));
                }
            }
            macdSignal = calculateEMA(macdValues, signalPeriod);
        }
        
        return new MACDResult(ema12, ema26, macd, macdSignal);
    }
    
    /**
     * 볼린저 밴드 계산
     */
    public static BollingerBandsResult calculateBollingerBands(List<StockData> data, int period, double stdDev) {
        if (data.size() < period) return null;
        
        // 중간선 (SMA)
        BigDecimal middle = calculateSMA(data, period);
        if (middle == null) return null;
        
        // 표준편차 계산
        BigDecimal variance = BigDecimal.ZERO;
        for (int i = data.size() - period; i < data.size(); i++) {
            BigDecimal diff = data.get(i).getClose().subtract(middle);
            variance = variance.add(diff.multiply(diff));
        }
        BigDecimal avgVariance = variance.divide(BigDecimal.valueOf(period), 4, RoundingMode.HALF_UP);
        BigDecimal standardDeviation = BigDecimal.valueOf(Math.sqrt(avgVariance.doubleValue()));
        
        // 상단선과 하단선
        BigDecimal multiplier = BigDecimal.valueOf(stdDev);
        BigDecimal upper = middle.add(standardDeviation.multiply(multiplier));
        BigDecimal lower = middle.subtract(standardDeviation.multiply(multiplier));
        
        return new BollingerBandsResult(upper, middle, lower);
    }
    
    /**
     * 일일 수익률 계산
     */
    public static BigDecimal calculateDailyReturn(StockData currentData, StockData previousData) {
        if (previousData == null || previousData.getClose().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        return currentData.getClose().subtract(previousData.getClose())
                .divide(previousData.getClose(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
    
    /**
     * EMA 계산 헬퍼 메서드 (StockData 리스트용)
     */
    private static BigDecimal calculateEMAFromStockData(List<StockData> data, int period) {
        if (data.size() < period) return null;
        
        List<BigDecimal> prices = new ArrayList<>();
        for (StockData stockData : data) {
            prices.add(stockData.getClose());
        }
        
        return calculateEMA(prices, period);
    }
    
    /**
     * MACD 결과를 담는 내부 클래스
     */
    public static class MACDResult {
        private final BigDecimal ema12;
        private final BigDecimal ema26;
        private final BigDecimal macd;
        private final BigDecimal macdSignal;
        
        public MACDResult(BigDecimal ema12, BigDecimal ema26, BigDecimal macd, BigDecimal macdSignal) {
            this.ema12 = ema12;
            this.ema26 = ema26;
            this.macd = macd;
            this.macdSignal = macdSignal;
        }
        
        public BigDecimal getEma12() { return ema12; }
        public BigDecimal getEma26() { return ema26; }
        public BigDecimal getMacd() { return macd; }
        public BigDecimal getMacdSignal() { return macdSignal; }
        public BigDecimal getMacdHistogram() { 
            return macdSignal != null ? macd.subtract(macdSignal) : null; 
        }
    }
    
    /**
     * 볼린저 밴드 결과를 담는 내부 클래스
     */
    public static class BollingerBandsResult {
        private final BigDecimal upper;
        private final BigDecimal middle;
        private final BigDecimal lower;
        
        public BollingerBandsResult(BigDecimal upper, BigDecimal middle, BigDecimal lower) {
            this.upper = upper;
            this.middle = middle;
            this.lower = lower;
        }
        
        public BigDecimal getUpper() { return upper; }
        public BigDecimal getMiddle() { return middle; }
        public BigDecimal getLower() { return lower; }
    }
} 