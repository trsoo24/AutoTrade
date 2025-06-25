package trade.project.backtest.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;

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
    
    // 기술적 지표
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
    
    // 이동평균 계산
    public void calculateSMA(List<StockData> data, int period) {
        if (data.size() < period) return;
        
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = data.size() - period; i < data.size(); i++) {
            sum = sum.add(data.get(i).getClose());
        }
        
        BigDecimal sma = sum.divide(BigDecimal.valueOf(period), 2, BigDecimal.ROUND_HALF_UP);
        
        switch (period) {
            case 5:
                this.sma5 = sma;
                break;
            case 10:
                this.sma10 = sma;
                break;
            case 20:
                this.sma20 = sma;
                break;
            case 50:
                this.sma50 = sma;
                break;
            case 200:
                this.sma200 = sma;
                break;
        }
    }
    
    // RSI 계산
    public void calculateRSI(List<StockData> data, int period) {
        if (data.size() < period + 1) return;
        
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
        
        BigDecimal avgGain = gains.divide(BigDecimal.valueOf(period), 4, BigDecimal.ROUND_HALF_UP);
        BigDecimal avgLoss = losses.divide(BigDecimal.valueOf(period), 4, BigDecimal.ROUND_HALF_UP);
        
        if (avgLoss.compareTo(BigDecimal.ZERO) == 0) {
            this.rsi = BigDecimal.valueOf(100);
        } else {
            BigDecimal rs = avgGain.divide(avgLoss, 4, BigDecimal.ROUND_HALF_UP);
            this.rsi = BigDecimal.valueOf(100).subtract(
                BigDecimal.valueOf(100).divide(BigDecimal.ONE.add(rs), 2, BigDecimal.ROUND_HALF_UP)
            );
        }
    }
    
    // MACD 계산
    public void calculateMACD(List<StockData> data, int fastPeriod, int slowPeriod, int signalPeriod) {
        if (data.size() < slowPeriod) return;
        
        // EMA 계산
        BigDecimal ema12 = calculateEMAFromStockData(data, fastPeriod);
        BigDecimal ema26 = calculateEMAFromStockData(data, slowPeriod);
        
        this.ema12 = ema12;
        this.ema26 = ema26;
        this.macd = ema12.subtract(ema26);
        
        // MACD Signal 계산
        if (data.size() >= slowPeriod + signalPeriod) {
            List<BigDecimal> macdValues = new ArrayList<>();
            for (int i = data.size() - signalPeriod; i < data.size(); i++) {
                BigDecimal fastEMA = calculateEMAFromStockData(data.subList(0, i + 1), fastPeriod);
                BigDecimal slowEMA = calculateEMAFromStockData(data.subList(0, i + 1), slowPeriod);
                macdValues.add(fastEMA.subtract(slowEMA));
            }
            this.macdSignal = calculateEMAFromBigDecimal(macdValues, signalPeriod);
            this.macdHistogram = this.macd.subtract(this.macdSignal);
        }
    }
    
    // EMA 계산 헬퍼 메서드 (StockData 리스트용)
    private BigDecimal calculateEMAFromStockData(List<StockData> data, int period) {
        if (data.size() < period) return null;
        
        BigDecimal multiplier = BigDecimal.valueOf(2.0 / (period + 1));
        BigDecimal ema = data.get(period - 1).getClose();
        
        for (int i = period; i < data.size(); i++) {
            ema = data.get(i).getClose().multiply(multiplier)
                    .add(ema.multiply(BigDecimal.ONE.subtract(multiplier)));
        }
        
        return ema;
    }
    
    // EMA 계산 헬퍼 메서드 (BigDecimal 리스트용)
    private BigDecimal calculateEMAFromBigDecimal(List<BigDecimal> data, int period) {
        if (data.size() < period) return null;
        
        BigDecimal multiplier = BigDecimal.valueOf(2.0 / (period + 1));
        BigDecimal ema = data.get(period - 1);
        
        for (int i = period; i < data.size(); i++) {
            ema = data.get(i).multiply(multiplier)
                    .add(ema.multiply(BigDecimal.ONE.subtract(multiplier)));
        }
        
        return ema;
    }
    
    // 볼린저 밴드 계산
    public void calculateBollingerBands(List<StockData> data, int period, double stdDev) {
        if (data.size() < period) return;
        
        // 중간선 (SMA)
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = data.size() - period; i < data.size(); i++) {
            sum = sum.add(data.get(i).getClose());
        }
        this.bollingerMiddle = sum.divide(BigDecimal.valueOf(period), 2, BigDecimal.ROUND_HALF_UP);
        
        // 표준편차 계산
        BigDecimal variance = BigDecimal.ZERO;
        for (int i = data.size() - period; i < data.size(); i++) {
            BigDecimal diff = data.get(i).getClose().subtract(this.bollingerMiddle);
            variance = variance.add(diff.multiply(diff));
        }
        BigDecimal avgVariance = variance.divide(BigDecimal.valueOf(period), 4, BigDecimal.ROUND_HALF_UP);
        BigDecimal standardDeviation = BigDecimal.valueOf(Math.sqrt(avgVariance.doubleValue()));
        
        // 상단선과 하단선
        BigDecimal multiplier = BigDecimal.valueOf(stdDev);
        this.bollingerUpper = this.bollingerMiddle.add(standardDeviation.multiply(multiplier));
        this.bollingerLower = this.bollingerMiddle.subtract(standardDeviation.multiply(multiplier));
    }
    
    // 일일 수익률 계산
    public void calculateDailyReturn(StockData previousData) {
        if (previousData != null) {
            this.dailyReturn = this.close.subtract(previousData.getClose())
                    .divide(previousData.getClose(), 4, BigDecimal.ROUND_HALF_UP);
        }
    }
} 