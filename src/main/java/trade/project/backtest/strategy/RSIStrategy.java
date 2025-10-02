package trade.project.backtest.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import trade.project.backtest.dto.BackTestRequest;
import trade.project.backtest.dto.StockData;
import trade.project.backtest.util.TechnicalIndicatorCalculator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class RSIStrategy implements TradingStrategy {

    // RSI 동적 임계값 (시장 상황에 따라 조정)
    private static final BigDecimal BASE_OVERBOUGHT = new BigDecimal("70");
    private static final BigDecimal BASE_OVERSOLD = new BigDecimal("30");
    private static final BigDecimal EXTREME_OVERBOUGHT = new BigDecimal("80");
    private static final BigDecimal EXTREME_OVERSOLD = new BigDecimal("20");

    // 거래량 및 변동성 임계값
    private static final BigDecimal VOLUME_CONFIRMATION = new BigDecimal("1.3");
    private static final BigDecimal VOLATILITY_THRESHOLD = new BigDecimal("0.03");

    @Override
    public String generateSignal(StockData currentData, List<StockData> historicalData, BackTestRequest request) {
        if (currentData == null || historicalData == null || historicalData.isEmpty()) {
            return "HOLD";
        }

        int rsiPeriod = request.getRsiPeriod() != null ? request.getRsiPeriod() : 14;
        int overbought = request.getRsiOverbought() != null ? request.getRsiOverbought() : 70;
        int oversold = request.getRsiOversold() != null ? request.getRsiOversold() : 30;

        // 충분한 데이터가 있는지 확인
        if (historicalData.size() < Math.max(rsiPeriod * 2, 50)) {
            return "HOLD";
        }

        // 1. 다중 기간 RSI 계산
        BigDecimal currentRSI = TechnicalIndicatorCalculator.calculateRSI(historicalData, rsiPeriod);
        BigDecimal shortRSI = TechnicalIndicatorCalculator.calculateRSI(historicalData, 7);  // 단기
        BigDecimal longRSI = TechnicalIndicatorCalculator.calculateRSI(historicalData, 21);  // 장기

        if (currentRSI == null || shortRSI == null || longRSI == null) {
            return "HOLD";
        }

        // 2. RSI 이동평균 계산 (RSI의 평활화)
        List<BigDecimal> rsiHistory = calculateRSIHistory(historicalData, rsiPeriod);
        BigDecimal rsiMA = calculateMA(rsiHistory, 5);

        // 3. 이전 RSI 값들
        BigDecimal prevRSI = null;
        if (historicalData.size() > 1) {
            List<StockData> prevHistoricalData = historicalData.subList(0, historicalData.size() - 1);
            prevRSI = TechnicalIndicatorCalculator.calculateRSI(prevHistoricalData, rsiPeriod);
        }

        // 4. 거래량 분석
        BigDecimal volumeRatio = calculateVolumeRatio(historicalData);
        boolean volumeConfirmation = volumeRatio.compareTo(VOLUME_CONFIRMATION) > 0;

        // 5. 가격 트렌드 분석
        TrendInfo trend = analyzeTrend(historicalData);

        // 6. RSI 다이버전스 확인
        DivergenceInfo divergence = checkRSIDivergence(historicalData, rsiHistory);

        // 7. 스토캐스틱 RSI 계산 (RSI의 RSI)
        BigDecimal stochRSI = calculateStochasticRSI(rsiHistory);

        // 8. 변동성 분석
        BigDecimal volatility = calculateVolatility(historicalData, 20);
        boolean lowVolatility = volatility.compareTo(VOLATILITY_THRESHOLD) < 0;

        // 9. 동적 임계값 조정
        BigDecimal dynamicOverbought = adjustOverboughtLevel(trend, volatility);
        BigDecimal dynamicOversold = adjustOversoldLevel(trend, volatility);

        // === 매수 신호 조건 ===

        // 극단적 과매도 + 반전 신호 (최강 매수)
        if (currentRSI.compareTo(EXTREME_OVERSOLD) <= 0 &&
                prevRSI != null && currentRSI.compareTo(prevRSI) > 0 &&
                volumeConfirmation) {
            log.info("=== RSI 극단적 과매도 반전 매수 ===");
            log.info("RSI: {}, 거래량 비율: {}", currentRSI, volumeRatio);
            return "BUY";
        }

        // RSI 상승 다이버전스 + 과매도 탈출
        if (divergence.isBullish &&
                prevRSI != null && prevRSI.compareTo(dynamicOversold) <= 0 &&
                currentRSI.compareTo(dynamicOversold) > 0) {
            log.info("RSI 상승 다이버전스 매수 신호: RSI {}", currentRSI);
            return "BUY";
        }

        // 다중 RSI 정렬 매수 (단기 > 중기 > 장기)
        if (shortRSI.compareTo(currentRSI) > 0 &&
                currentRSI.compareTo(longRSI) > 0 &&
                currentRSI.compareTo(new BigDecimal("40")) < 0 &&
                trend.isUptrend) {
            log.debug("RSI 다중 기간 정렬 매수: 단기={}, 중기={}, 장기={}",
                    shortRSI, currentRSI, longRSI);
            return "BUY";
        }

        // 과매도 구간 진입 + 스토캐스틱 RSI 확인
        if (currentRSI.compareTo(dynamicOversold) <= 0 &&
                stochRSI.compareTo(new BigDecimal("20")) < 0 &&
                !trend.isStrongDowntrend) {

            // 추가 필터: RSI가 이동평균보다 낮고 반전 조짐
            if (currentRSI.compareTo(rsiMA) < 0 &&
                    prevRSI != null && currentRSI.subtract(prevRSI).compareTo(BigDecimal.ZERO) >= 0) {
                log.debug("RSI 과매도 + 스토캐스틱 확인 매수");
                return "BUY";
            }
        }

        // 횡보장에서의 RSI 바운스 매수
        if (trend.isSideways && lowVolatility) {
            if (currentRSI.compareTo(new BigDecimal("35")) <= 0 &&
                    prevRSI != null && currentRSI.compareTo(prevRSI) > 0) {
                log.debug("횡보장 RSI 바운스 매수: RSI {}", currentRSI);
                return "BUY";
            }
        }

        // === 매도 신호 조건 ===

        // 극단적 과매수 + 반전 신호 (최강 매도)
        if (currentRSI.compareTo(EXTREME_OVERBOUGHT) >= 0 &&
                prevRSI != null && currentRSI.compareTo(prevRSI) < 0 &&
                volumeConfirmation) {
            log.info("=== RSI 극단적 과매수 반전 매도 ===");
            log.info("RSI: {}, 거래량 비율: {}", currentRSI, volumeRatio);
            return "SELL";
        }

        // RSI 하락 다이버전스 + 과매수 이탈
        if (divergence.isBearish &&
                prevRSI != null && prevRSI.compareTo(dynamicOverbought) >= 0 &&
                currentRSI.compareTo(dynamicOverbought) < 0) {
            log.info("RSI 하락 다이버전스 매도 신호: RSI {}", currentRSI);
            return "SELL";
        }

        // 다중 RSI 정렬 매도 (단기 < 중기 < 장기)
        if (shortRSI.compareTo(currentRSI) < 0 &&
                currentRSI.compareTo(longRSI) < 0 &&
                currentRSI.compareTo(new BigDecimal("60")) > 0 &&
                trend.isDowntrend) {
            log.debug("RSI 다중 기간 정렬 매도: 단기={}, 중기={}, 장기={}",
                    shortRSI, currentRSI, longRSI);
            return "SELL";
        }

        // 과매수 구간 진입 + 스토캐스틱 RSI 확인
        if (currentRSI.compareTo(dynamicOverbought) >= 0 &&
                stochRSI.compareTo(new BigDecimal("80")) > 0 &&
                !trend.isStrongUptrend) {

            // 추가 필터: RSI가 이동평균보다 높고 반전 조짐
            if (currentRSI.compareTo(rsiMA) > 0 &&
                    prevRSI != null && currentRSI.subtract(prevRSI).compareTo(BigDecimal.ZERO) <= 0) {
                log.debug("RSI 과매수 + 스토캐스틱 확인 매도");
                return "SELL";
            }
        }

        // 횡보장에서의 RSI 저항 매도
        if (trend.isSideways && lowVolatility) {
            if (currentRSI.compareTo(new BigDecimal("65")) >= 0 &&
                    prevRSI != null && currentRSI.compareTo(prevRSI) < 0) {
                log.debug("횡보장 RSI 저항 매도: RSI {}", currentRSI);
                return "SELL";
            }
        }

        return "HOLD";
    }

    // RSI 이력 계산
    private List<BigDecimal> calculateRSIHistory(List<StockData> historicalData, int period) {
        List<BigDecimal> rsiHistory = new ArrayList<>();

        for (int i = period + 1; i <= historicalData.size(); i++) {
            List<StockData> subList = historicalData.subList(0, i);
            BigDecimal rsi = TechnicalIndicatorCalculator.calculateRSI(subList, period);
            if (rsi != null) {
                rsiHistory.add(rsi);
            }
        }

        return rsiHistory;
    }

    // 이동평균 계산
    private BigDecimal calculateMA(List<BigDecimal> values, int period) {
        if (values.size() < period) return values.get(values.size() - 1);

        BigDecimal sum = BigDecimal.ZERO;
        for (int i = values.size() - period; i < values.size(); i++) {
            sum = sum.add(values.get(i));
        }

        return sum.divide(new BigDecimal(period), 2, RoundingMode.HALF_UP);
    }

    // 거래량 비율 계산
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

    // 트렌드 분석
    private TrendInfo analyzeTrend(List<StockData> historicalData) {
        TrendInfo trend = new TrendInfo();

        if (historicalData.size() < 50) {
            return trend;
        }

        // 20일, 50일 이동평균
        BigDecimal ma20 = TechnicalIndicatorCalculator.calculateSMA(historicalData, 20);
        BigDecimal ma50 = TechnicalIndicatorCalculator.calculateSMA(historicalData, 50);
        BigDecimal currentPrice = historicalData.get(historicalData.size() - 1).getClose();

        // 가격 위치로 트렌드 판단
        if (currentPrice.compareTo(ma20) > 0 && ma20.compareTo(ma50) > 0) {
            trend.isUptrend = true;

            // 20일선이 50일선보다 5% 이상 높으면 강한 상승
            BigDecimal diff = ma20.subtract(ma50).divide(ma50, 4, RoundingMode.HALF_UP);
            if (diff.compareTo(new BigDecimal("0.05")) > 0) {
                trend.isStrongUptrend = true;
            }
        } else if (currentPrice.compareTo(ma20) < 0 && ma20.compareTo(ma50) < 0) {
            trend.isDowntrend = true;

            // 20일선이 50일선보다 5% 이상 낮으면 강한 하락
            BigDecimal diff = ma50.subtract(ma20).divide(ma50, 4, RoundingMode.HALF_UP);
            if (diff.compareTo(new BigDecimal("0.05")) > 0) {
                trend.isStrongDowntrend = true;
            }
        } else {
            trend.isSideways = true;
        }

        return trend;
    }

    // RSI 다이버전스 확인
    private DivergenceInfo checkRSIDivergence(List<StockData> historicalData, List<BigDecimal> rsiHistory) {
        DivergenceInfo divergence = new DivergenceInfo();

        if (historicalData.size() < 30 || rsiHistory.size() < 15) {
            return divergence;
        }

        // 최근 15일간의 고점/저점 찾기
        int lookback = Math.min(15, rsiHistory.size());

        BigDecimal maxPrice = BigDecimal.ZERO;
        BigDecimal minPrice = new BigDecimal("999999");
        BigDecimal maxRSI = BigDecimal.ZERO;
        BigDecimal minRSI = new BigDecimal("100");

        int priceStartIdx = historicalData.size() - lookback;
        int rsiStartIdx = rsiHistory.size() - lookback;

        for (int i = 0; i < lookback; i++) {
            BigDecimal price = historicalData.get(priceStartIdx + i).getClose();
            BigDecimal rsi = rsiHistory.get(rsiStartIdx + i);

            if (price.compareTo(maxPrice) > 0) maxPrice = price;
            if (price.compareTo(minPrice) < 0) minPrice = price;
            if (rsi.compareTo(maxRSI) > 0) maxRSI = rsi;
            if (rsi.compareTo(minRSI) < 0) minRSI = rsi;
        }

        BigDecimal currentPrice = historicalData.get(historicalData.size() - 1).getClose();
        BigDecimal currentRSI = rsiHistory.get(rsiHistory.size() - 1);

        // 상승 다이버전스: 가격은 낮은 저점, RSI는 높은 저점
        if (currentPrice.compareTo(minPrice) <= 0 && currentRSI.compareTo(minRSI) > 0) {
            divergence.isBullish = true;
        }

        // 하락 다이버전스: 가격은 높은 고점, RSI는 낮은 고점
        if (currentPrice.compareTo(maxPrice) >= 0 && currentRSI.compareTo(maxRSI) < 0) {
            divergence.isBearish = true;
        }

        return divergence;
    }

    // 스토캐스틱 RSI 계산
    private BigDecimal calculateStochasticRSI(List<BigDecimal> rsiHistory) {
        if (rsiHistory.size() < 14) {
            return new BigDecimal("50"); // 중립값 반환
        }

        int period = 14;
        BigDecimal maxRSI = BigDecimal.ZERO;
        BigDecimal minRSI = new BigDecimal("100");

        for (int i = rsiHistory.size() - period; i < rsiHistory.size(); i++) {
            BigDecimal rsi = rsiHistory.get(i);
            if (rsi.compareTo(maxRSI) > 0) maxRSI = rsi;
            if (rsi.compareTo(minRSI) < 0) minRSI = rsi;
        }

        BigDecimal currentRSI = rsiHistory.get(rsiHistory.size() - 1);
        BigDecimal range = maxRSI.subtract(minRSI);

        if (range.compareTo(BigDecimal.ZERO) == 0) {
            return new BigDecimal("50");
        }

        return currentRSI.subtract(minRSI)
                .divide(range, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    // 변동성 계산
    private BigDecimal calculateVolatility(List<StockData> historicalData, int period) {
        if (historicalData.size() < period) return BigDecimal.ZERO;

        BigDecimal sumSquaredReturns = BigDecimal.ZERO;

        for (int i = historicalData.size() - period + 1; i < historicalData.size(); i++) {
            BigDecimal prevClose = historicalData.get(i - 1).getClose();
            BigDecimal currClose = historicalData.get(i).getClose();

            if (prevClose.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal dailyReturn = currClose.subtract(prevClose)
                        .divide(prevClose, 6, RoundingMode.HALF_UP);
                sumSquaredReturns = sumSquaredReturns.add(dailyReturn.multiply(dailyReturn));
            }
        }

        BigDecimal variance = sumSquaredReturns.divide(new BigDecimal(period - 1), 6, RoundingMode.HALF_UP);

        // 표준편차 계산 (간단한 근사치)
        return variance.multiply(new BigDecimal("100")); // 백분율로 변환
    }

    // 동적 과매수 레벨 조정
    private BigDecimal adjustOverboughtLevel(TrendInfo trend, BigDecimal volatility) {
        BigDecimal baseLevel = BASE_OVERBOUGHT;

        // 강한 상승 트렌드에서는 과매수 기준을 높임
        if (trend.isStrongUptrend) {
            baseLevel = baseLevel.add(new BigDecimal("5"));
        }

        // 높은 변동성에서는 과매수 기준을 높임
        if (volatility.compareTo(new BigDecimal("0.04")) > 0) {
            baseLevel = baseLevel.add(new BigDecimal("3"));
        }

        return baseLevel.min(new BigDecimal("85")); // 최대 85
    }

    // 동적 과매도 레벨 조정
    private BigDecimal adjustOversoldLevel(TrendInfo trend, BigDecimal volatility) {
        BigDecimal baseLevel = BASE_OVERSOLD;

        // 강한 하락 트렌드에서는 과매도 기준을 낮춤
        if (trend.isStrongDowntrend) {
            baseLevel = baseLevel.subtract(new BigDecimal("5"));
        }

        // 높은 변동성에서는 과매도 기준을 낮춤
        if (volatility.compareTo(new BigDecimal("0.04")) > 0) {
            baseLevel = baseLevel.subtract(new BigDecimal("3"));
        }

        return baseLevel.max(new BigDecimal("15")); // 최소 15
    }

    // 트렌드 정보 클래스
    private static class TrendInfo {
        boolean isUptrend = false;
        boolean isDowntrend = false;
        boolean isSideways = false;
        boolean isStrongUptrend = false;
        boolean isStrongDowntrend = false;
    }

    // 다이버전스 정보 클래스
    private static class DivergenceInfo {
        boolean isBullish = false;
        boolean isBearish = false;
    }

    @Override
    public String getStrategyName() {
        return "Enhanced RSI";
    }

    @Override
    public String getStrategyDescription() {
        return "고도화된 RSI 전략 - 다중 기간 분석, 다이버전스, 스토캐스틱 RSI, 동적 임계값";
    }
}