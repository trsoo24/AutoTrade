package trade.project.backtest.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import trade.project.backtest.dto.BackTestRequest;
import trade.project.backtest.dto.StockData;
import trade.project.backtest.util.TechnicalIndicatorCalculator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Component
public class MACDStrategy implements TradingStrategy {

    // 시장 강도 임계값
    private static final BigDecimal STRONG_TREND_THRESHOLD = new BigDecimal("0.015");
    private static final BigDecimal VOLUME_SURGE_THRESHOLD = new BigDecimal("1.5");
    private static final BigDecimal DIVERGENCE_THRESHOLD = new BigDecimal("0.02");

    @Override
    public String generateSignal(StockData currentData, List<StockData> historicalData, BackTestRequest request) {
        if (currentData == null || historicalData == null || historicalData.isEmpty()) {
            return "HOLD";
        }

        int fastPeriod = request.getMacdFastPeriod() != null ? request.getMacdFastPeriod() : 12;
        int slowPeriod = request.getMacdSlowPeriod() != null ? request.getMacdSlowPeriod() : 26;
        int signalPeriod = request.getMacdSignalPeriod() != null ? request.getMacdSignalPeriod() : 9;

        // 충분한 데이터가 있는지 확인
        if (historicalData.size() < slowPeriod + signalPeriod + 20) {
            return "HOLD";
        }

        // 1. MACD 계산
        TechnicalIndicatorCalculator.MACDResult currentMACD = TechnicalIndicatorCalculator.calculateMACD(
                historicalData, fastPeriod, slowPeriod, signalPeriod);

        if (currentMACD == null) {
            return "HOLD";
        }

        // 2. 다중 시간대 MACD 분석 (단기, 중기, 장기)
        TechnicalIndicatorCalculator.MACDResult shortTermMACD = TechnicalIndicatorCalculator.calculateMACD(
                historicalData, 5, 13, 5);
        TechnicalIndicatorCalculator.MACDResult longTermMACD = TechnicalIndicatorCalculator.calculateMACD(
                historicalData, 19, 39, 9);

        // 3. 이전 MACD 값
        TechnicalIndicatorCalculator.MACDResult prevMACD = null;
        if (historicalData.size() > 1) {
            List<StockData> prevHistoricalData = historicalData.subList(0, historicalData.size() - 1);
            prevMACD = TechnicalIndicatorCalculator.calculateMACD(
                    prevHistoricalData, fastPeriod, slowPeriod, signalPeriod);
        }

        // 4. 거래량 분석
        BigDecimal volumeRatio = calculateVolumeRatio(historicalData);
        boolean volumeSurge = volumeRatio.compareTo(VOLUME_SURGE_THRESHOLD) > 0;

        // 5. 가격 모멘텀 분석
        BigDecimal priceChange = calculatePriceChangeRate(historicalData, 5);
        BigDecimal momentum = calculateMomentum(historicalData, 10);

        // 6. MACD 다이버전스 확인
        boolean bullishDivergence = checkBullishDivergence(historicalData, currentMACD, prevMACD);
        boolean bearishDivergence = checkBearishDivergence(historicalData, currentMACD, prevMACD);

        // 7. 히스토그램 강도 분석
        BigDecimal currentHistogram = currentMACD.getMacd().subtract(currentMACD.getMacdSignal());
        BigDecimal prevHistogram = prevMACD != null ?
                prevMACD.getMacd().subtract(prevMACD.getMacdSignal()) : BigDecimal.ZERO;
        BigDecimal histogramChange = currentHistogram.subtract(prevHistogram);

        // 8. 트렌드 강도 계산
        BigDecimal trendStrength = calculateTrendStrength(currentMACD, shortTermMACD, longTermMACD);

        // === 매수 신호 조건 ===
        if (prevMACD != null) {
            // 강력한 매수 신호: 골든크로스 + 거래량 급증 + 모든 시간대 정렬
            if (prevMACD.getMacd().compareTo(prevMACD.getMacdSignal()) <= 0 &&
                    currentMACD.getMacd().compareTo(currentMACD.getMacdSignal()) > 0) {

                // 추가 필터링 조건들
                boolean strongBuySignal =
                        volumeSurge && // 거래량 급증
                                momentum.compareTo(BigDecimal.ZERO) > 0 && // 양의 모멘텀
                                shortTermMACD.getMacd().compareTo(shortTermMACD.getMacdSignal()) > 0 && // 단기 상승
                                trendStrength.compareTo(STRONG_TREND_THRESHOLD) > 0; // 강한 트렌드

                if (strongBuySignal) {
                    log.info("=== 강력한 MACD 매수 신호 ===");
                    log.info("MACD 골든크로스: {} > {}", currentMACD.getMacd(), currentMACD.getMacdSignal());
                    log.info("거래량 비율: {}, 모멘텀: {}, 트렌드 강도: {}", volumeRatio, momentum, trendStrength);
                    return "BUY";
                }

                // 보통 매수 신호
                if (histogramChange.compareTo(BigDecimal.ZERO) > 0 &&
                        !isVolatilityTooHigh(historicalData)) {
                    log.debug("MACD 매수 신호 (보통): 골든크로스 발생");
                    return "BUY";
                }
            }

            // 다이버전스 매수 신호
            if (bullishDivergence && volumeSurge) {
                log.info("MACD 상승 다이버전스 매수 신호");
                return "BUY";
            }

            // 히스토그램 전환 매수
            if (prevHistogram.compareTo(BigDecimal.ZERO) <= 0 &&
                    currentHistogram.compareTo(BigDecimal.ZERO) > 0 &&
                    histogramChange.abs().compareTo(new BigDecimal("0.001")) > 0) {

                if (momentum.compareTo(BigDecimal.ZERO) > 0 && volumeRatio.compareTo(BigDecimal.ONE) > 0) {
                    log.debug("MACD 히스토그램 전환 매수 신호");
                    return "BUY";
                }
            }
        }

        // === 매도 신호 조건 ===
        if (prevMACD != null) {
            // 강력한 매도 신호: 데드크로스 + 거래량 급증 + 모든 시간대 정렬
            if (prevMACD.getMacd().compareTo(prevMACD.getMacdSignal()) >= 0 &&
                    currentMACD.getMacd().compareTo(currentMACD.getMacdSignal()) < 0) {

                boolean strongSellSignal =
                        volumeSurge && // 거래량 급증
                                momentum.compareTo(BigDecimal.ZERO) < 0 && // 음의 모멘텀
                                shortTermMACD.getMacd().compareTo(shortTermMACD.getMacdSignal()) < 0 && // 단기 하락
                                trendStrength.abs().compareTo(STRONG_TREND_THRESHOLD) > 0; // 강한 하락 트렌드

                if (strongSellSignal) {
                    log.info("=== 강력한 MACD 매도 신호 ===");
                    log.info("MACD 데드크로스: {} < {}", currentMACD.getMacd(), currentMACD.getMacdSignal());
                    log.info("거래량 비율: {}, 모멘텀: {}, 트렌드 강도: {}", volumeRatio, momentum, trendStrength);
                    return "SELL";
                }

                // 보통 매도 신호
                if (histogramChange.compareTo(BigDecimal.ZERO) < 0) {
                    log.debug("MACD 매도 신호 (보통): 데드크로스 발생");
                    return "SELL";
                }
            }

            // 다이버전스 매도 신호
            if (bearishDivergence && volumeSurge) {
                log.info("MACD 하락 다이버전스 매도 신호");
                return "SELL";
            }

            // 히스토그램 전환 매도
            if (prevHistogram.compareTo(BigDecimal.ZERO) >= 0 &&
                    currentHistogram.compareTo(BigDecimal.ZERO) < 0 &&
                    histogramChange.abs().compareTo(new BigDecimal("0.001")) > 0) {

                if (momentum.compareTo(BigDecimal.ZERO) < 0 && volumeRatio.compareTo(BigDecimal.ONE) > 0) {
                    log.debug("MACD 히스토그램 전환 매도 신호");
                    return "SELL";
                }
            }
        }

        return "HOLD";
    }

    // 거래량 비율 계산 (현재 거래량 / 평균 거래량)
    private BigDecimal calculateVolumeRatio(List<StockData> historicalData) {
        if (historicalData.size() < 20) return BigDecimal.ONE;

        StockData currentData = historicalData.get(historicalData.size() - 1);
        BigDecimal avgVolume = BigDecimal.ZERO;

        for (int i = historicalData.size() - 20; i < historicalData.size() - 1; i++) {
            avgVolume = avgVolume.add(historicalData.get(i).getVolume());
        }
        avgVolume = avgVolume.divide(new BigDecimal("19"), 2, RoundingMode.HALF_UP);

        if (avgVolume.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ONE;

        return currentData.getVolume().divide(avgVolume, 2, RoundingMode.HALF_UP);
    }

    // 가격 변화율 계산
    private BigDecimal calculatePriceChangeRate(List<StockData> historicalData, int period) {
        if (historicalData.size() < period + 1) return BigDecimal.ZERO;

        BigDecimal currentPrice = historicalData.get(historicalData.size() - 1).getClose();
        BigDecimal pastPrice = historicalData.get(historicalData.size() - period - 1).getClose();

        if (pastPrice.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;

        return currentPrice.subtract(pastPrice).divide(pastPrice, 4, RoundingMode.HALF_UP);
    }

    // 모멘텀 계산
    private BigDecimal calculateMomentum(List<StockData> historicalData, int period) {
        if (historicalData.size() < period) return BigDecimal.ZERO;

        BigDecimal momentum = BigDecimal.ZERO;
        for (int i = historicalData.size() - period; i < historicalData.size(); i++) {
            StockData data = historicalData.get(i);
            BigDecimal change = data.getClose().subtract(data.getOpen());
            momentum = momentum.add(change);
        }

        return momentum.divide(new BigDecimal(period), 4, RoundingMode.HALF_UP);
    }

    // 상승 다이버전스 확인
    private boolean checkBullishDivergence(List<StockData> historicalData,
                                           TechnicalIndicatorCalculator.MACDResult currentMACD,
                                           TechnicalIndicatorCalculator.MACDResult prevMACD) {
        if (historicalData.size() < 20 || prevMACD == null) return false;

        // 가격은 하락하는데 MACD는 상승하는 경우
        BigDecimal priceChange = calculatePriceChangeRate(historicalData, 10);
        BigDecimal macdChange = currentMACD.getMacd().subtract(prevMACD.getMacd());

        return priceChange.compareTo(BigDecimal.ZERO) < 0 &&
                macdChange.compareTo(BigDecimal.ZERO) > 0 &&
                macdChange.abs().compareTo(DIVERGENCE_THRESHOLD) > 0;
    }

    // 하락 다이버전스 확인
    private boolean checkBearishDivergence(List<StockData> historicalData,
                                           TechnicalIndicatorCalculator.MACDResult currentMACD,
                                           TechnicalIndicatorCalculator.MACDResult prevMACD) {
        if (historicalData.size() < 20 || prevMACD == null) return false;

        // 가격은 상승하는데 MACD는 하락하는 경우
        BigDecimal priceChange = calculatePriceChangeRate(historicalData, 10);
        BigDecimal macdChange = currentMACD.getMacd().subtract(prevMACD.getMacd());

        return priceChange.compareTo(BigDecimal.ZERO) > 0 &&
                macdChange.compareTo(BigDecimal.ZERO) < 0 &&
                macdChange.abs().compareTo(DIVERGENCE_THRESHOLD) > 0;
    }

    // 변동성이 너무 높은지 확인
    private boolean isVolatilityTooHigh(List<StockData> historicalData) {
        if (historicalData.size() < 5) return false;

        BigDecimal maxChange = BigDecimal.ZERO;
        for (int i = historicalData.size() - 5; i < historicalData.size(); i++) {
            StockData data = historicalData.get(i);
            BigDecimal dayChange = data.getHigh().subtract(data.getLow())
                    .divide(data.getClose(), 4, RoundingMode.HALF_UP);
            if (dayChange.compareTo(maxChange) > 0) {
                maxChange = dayChange;
            }
        }

        // 일중 변동성이 5% 이상이면 위험
        return maxChange.compareTo(new BigDecimal("0.05")) > 0;
    }

    // 트렌드 강도 계산
    private BigDecimal calculateTrendStrength(TechnicalIndicatorCalculator.MACDResult current,
                                              TechnicalIndicatorCalculator.MACDResult shortTerm,
                                              TechnicalIndicatorCalculator.MACDResult longTerm) {
        if (current == null || shortTerm == null || longTerm == null) return BigDecimal.ZERO;

        BigDecimal currentHist = current.getMacd().subtract(current.getMacdSignal());
        BigDecimal shortHist = shortTerm.getMacd().subtract(shortTerm.getMacdSignal());
        BigDecimal longHist = longTerm.getMacd().subtract(longTerm.getMacdSignal());

        // 모든 시간대의 히스토그램 평균
        return currentHist.add(shortHist).add(longHist).divide(new BigDecimal("3"), 4, RoundingMode.HALF_UP);
    }

    @Override
    public String getStrategyName() {
        return "Enhanced MACD";
    }

    @Override
    public String getStrategyDescription() {
        return "고도화된 MACD 전략 - 다중 시간대 분석, 거래량 확인, 다이버전스 감지";
    }
}