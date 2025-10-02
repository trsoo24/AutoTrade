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
public class SMAStrategy implements TradingStrategy {

    // 전략 파라미터
    private static final BigDecimal VOLUME_SURGE_RATIO = new BigDecimal("1.5");
    private static final BigDecimal BREAKOUT_STRENGTH = new BigDecimal("0.02"); // 2% 돌파
    private static final BigDecimal TREND_STRENGTH_THRESHOLD = new BigDecimal("0.03");
    private static final int CONSOLIDATION_PERIOD = 10;

    @Override
    public String getStrategyName() {
        return "Enhanced SMA";
    }

    @Override
    public String getStrategyDescription() {
        return "고도화된 이동평균 전략 - 다중 이동평균, 거래량 분석, 패턴 인식";
    }

    @Override
    public String generateSignal(StockData currentData, List<StockData> historicalData, BackTestRequest request) {
        if (currentData == null || historicalData == null || historicalData.isEmpty()) {
            return "HOLD";
        }

        int shortPeriod = request.getShortPeriod() != null ? request.getShortPeriod() : 5;
        int mediumPeriod = 10; // 중기 이동평균 추가
        int longPeriod = request.getLongPeriod() != null ? request.getLongPeriod() : 20;
        int veryLongPeriod = 60; // 장기 트렌드 확인용

        // 충분한 데이터가 있는지 확인
        if (historicalData.size() < Math.max(veryLongPeriod, 120)) {
            return "HOLD";
        }

        // 1. 다중 이동평균 계산
        BigDecimal shortSMA = TechnicalIndicatorCalculator.calculateSMA(historicalData, shortPeriod);
        BigDecimal mediumSMA = TechnicalIndicatorCalculator.calculateSMA(historicalData, mediumPeriod);
        BigDecimal longSMA = TechnicalIndicatorCalculator.calculateSMA(historicalData, longPeriod);
        BigDecimal veryLongSMA = TechnicalIndicatorCalculator.calculateSMA(historicalData, veryLongPeriod);

        if (shortSMA == null || mediumSMA == null || longSMA == null || veryLongSMA == null) {
            return "HOLD";
        }

        // 2. 지수이동평균(EMA) 계산 - 더 빠른 반응
        BigDecimal shortEMA = calculateEMA(historicalData, shortPeriod);
        BigDecimal longEMA = calculateEMA(historicalData, longPeriod);

        // 3. 이전 이동평균 값들
        BigDecimal prevShortSMA = null;
        BigDecimal prevMediumSMA = null;
        BigDecimal prevLongSMA = null;
        BigDecimal prevShortEMA = null;

        if (historicalData.size() > 1) {
            List<StockData> prevHistoricalData = historicalData.subList(0, historicalData.size() - 1);
            prevShortSMA = TechnicalIndicatorCalculator.calculateSMA(prevHistoricalData, shortPeriod);
            prevMediumSMA = TechnicalIndicatorCalculator.calculateSMA(prevHistoricalData, mediumPeriod);
            prevLongSMA = TechnicalIndicatorCalculator.calculateSMA(prevHistoricalData, longPeriod);
            prevShortEMA = calculateEMA(prevHistoricalData, shortPeriod);
        }

        // 4. 거래량 분석
        VolumeAnalysis volumeAnalysis = analyzeVolume(historicalData);

        // 5. 가격 위치 및 모멘텀
        BigDecimal currentPrice = currentData.getClose();
        BigDecimal priceToSMA = currentPrice.subtract(longSMA).divide(longSMA, 4, RoundingMode.HALF_UP);
        BigDecimal momentum = calculatePriceMomentum(historicalData, 10);

        // 6. 이동평균 수렴/발산 분석
        MAConvergence convergence = analyzeMAConvergence(shortSMA, mediumSMA, longSMA, veryLongSMA);

        // 7. 지지/저항 레벨 분석
        SupportResistance levels = analyzeSupportResistance(historicalData, shortSMA, longSMA);

        // 8. 볼린저 밴드 포지션 (변동성 고려)
        BollingerPosition bbPosition = getBollingerPosition(historicalData, longPeriod);

        // 9. 이동평균 기울기 (트렌드 강도)
        BigDecimal smaSlope = calculateSMASlope(historicalData, longPeriod, 5);

        // 10. 패턴 인식
        boolean isConsolidating = checkConsolidation(historicalData, CONSOLIDATION_PERIOD);
        boolean isBreakout = checkBreakout(historicalData, currentPrice, longSMA);

        // === 매수 신호 조건 ===

        // 퍼펙트 오더 매수 (모든 이동평균 정렬 + 거래량)
        if (convergence.isPerfectBullishOrder && volumeAnalysis.isVolumeSurge) {
            log.info("=== 퍼펙트 오더 매수 신호 ===");
            log.info("단기={}, 중기={}, 장기={}, 초장기={}", shortSMA, mediumSMA, longSMA, veryLongSMA);
            log.info("거래량 비율: {}", volumeAnalysis.volumeRatio);
            return "BUY";
        }

        // 골든 크로스 + 추가 확인
        if (prevShortSMA != null && prevLongSMA != null) {
            boolean goldenCross = prevShortSMA.compareTo(prevLongSMA) <= 0 &&
                    shortSMA.compareTo(longSMA) > 0;

            if (goldenCross) {
                // 거래량 확인 + EMA 확인 + 상위 트렌드 확인
                if (volumeAnalysis.isVolumeSurge &&
                        shortEMA.compareTo(longEMA) > 0 &&
                        longSMA.compareTo(veryLongSMA) > 0) {
                    log.info("골든 크로스 강력 매수: 거래량 및 트렌드 확인");
                    return "BUY";
                }

                // 일반 골든 크로스
                if (momentum.compareTo(BigDecimal.ZERO) > 0 &&
                        smaSlope.compareTo(BigDecimal.ZERO) > 0) {
                    log.debug("골든 크로스 매수 신호 확인");
                    return "BUY";
                }
            }
        }

        // 횡보 후 돌파 매수
        if (isConsolidating && isBreakout && volumeAnalysis.isVolumeIncreasing) {
            if (shortSMA.compareTo(longSMA) > 0 &&
                    currentPrice.compareTo(shortSMA) > 0) {
                log.info("횡보 구간 돌파 매수: 가격={}, 단기선={}", currentPrice, shortSMA);
                return "BUY";
            }
        }

        // 지지선 반등 매수
        if (levels.nearSupport && !levels.nearResistance) {
            if (currentPrice.compareTo(longSMA) > 0 &&
                    prevShortEMA != null && shortEMA.compareTo(prevShortEMA) > 0 &&
                    volumeAnalysis.volumeRatio.compareTo(BigDecimal.ONE) > 0) {
                log.debug("이동평균 지지선 반등 매수");
                return "BUY";
            }
        }

        // 볼린저 밴드 하단 터치 후 반등
        if (bbPosition.isNearLowerBand &&
                prevShortSMA != null &&
                currentPrice.compareTo(historicalData.get(historicalData.size() - 2).getClose()) > 0 &&
                shortSMA.compareTo(prevShortSMA) >= 0) {
            log.debug("볼린저 밴드 하단 반등 매수");
            return "BUY";
        }

        // 추세 추종 매수 (상승 트렌드 지속)
        if (convergence.isBullishTrend &&
                smaSlope.compareTo(TREND_STRENGTH_THRESHOLD) > 0 &&
                priceToSMA.compareTo(new BigDecimal("0.01")) > 0 &&
                priceToSMA.compareTo(new BigDecimal("0.05")) < 0) { // 이평선 위 1-5% 구간

            if (volumeAnalysis.avgVolumeRatio.compareTo(new BigDecimal("1.1")) > 0) {
                log.debug("추세 추종 매수: 강한 상승 트렌드");
                return "BUY";
            }
        }

        // === 매도 신호 조건 ===

        // 퍼펙트 오더 붕괴 매도
        if (convergence.isPerfectBearishOrder && volumeAnalysis.isVolumeSurge) {
            log.info("=== 퍼펙트 오더 매도 신호 ===");
            log.info("단기={}, 중기={}, 장기={}, 초장기={}", shortSMA, mediumSMA, longSMA, veryLongSMA);
            return "SELL";
        }

        // 데드크로스 + 추가 확인
        if (prevShortSMA != null && prevLongSMA != null) {
            boolean deadCross = prevShortSMA.compareTo(prevLongSMA) >= 0 &&
                    shortSMA.compareTo(longSMA) < 0;

            if (deadCross) {
                // 거래량 확인 + EMA 확인 + 하위 트렌드 확인
                if (volumeAnalysis.isVolumeSurge &&
                        shortEMA.compareTo(longEMA) < 0 &&
                        longSMA.compareTo(veryLongSMA) < 0) {
                    log.info("데드크로스 강력 매도: 거래량 및 트렌드 확인");
                    return "SELL";
                }

                // 일반 데드크로스
                if (momentum.compareTo(BigDecimal.ZERO) < 0 &&
                        smaSlope.compareTo(BigDecimal.ZERO) < 0) {
                    log.debug("데드크로스 매도 신호 확인");
                    return "SELL";
                }
            }
        }

        // 저항선 도달 매도
        if (levels.nearResistance && !levels.nearSupport) {
            if (currentPrice.compareTo(longSMA) < 0 &&
                    prevShortEMA != null && shortEMA.compareTo(prevShortEMA) < 0 &&
                    volumeAnalysis.volumeRatio.compareTo(new BigDecimal("1.2")) > 0) {
                log.debug("이동평균 저항선 도달 매도");
                return "SELL";
            }
        }

        // 볼린저 밴드 상단 터치 후 반락
        if (bbPosition.isNearUpperBand &&
                prevShortSMA != null &&
                currentPrice.compareTo(historicalData.get(historicalData.size() - 2).getClose()) < 0 &&
                shortSMA.compareTo(prevShortSMA) <= 0) {
            log.debug("볼린저 밴드 상단 반락 매도");
            return "SELL";
        }

        // 추세 전환 매도
        if (convergence.isBearishTrend &&
                smaSlope.compareTo(TREND_STRENGTH_THRESHOLD.negate()) < 0 &&
                priceToSMA.compareTo(new BigDecimal("-0.01")) < 0) {

            if (volumeAnalysis.isVolumeIncreasing) {
                log.debug("추세 전환 매도: 하락 트렌드 시작");
                return "SELL";
            }
        }

        // 급등 후 조정 매도 (과열 구간)
        if (priceToSMA.compareTo(new BigDecimal("0.08")) > 0 && // 이평선 위 8% 이상
                momentum.compareTo(new BigDecimal("0.05")) > 0) { // 단기 급등
            log.debug("과열 구간 매도: 이평선 대비 {}% 상승", priceToSMA.multiply(new BigDecimal("100")));
            return "SELL";
        }

        return "HOLD";
    }

    // EMA 계산
    private BigDecimal calculateEMA(List<StockData> historicalData, int period) {
        if (historicalData.size() < period) return null;

        BigDecimal multiplier = new BigDecimal("2").divide(
                new BigDecimal(period + 1), 6, RoundingMode.HALF_UP);

        // 초기값은 SMA로 설정
        BigDecimal ema = TechnicalIndicatorCalculator.calculateSMA(
                historicalData.subList(0, period), period);

        for (int i = period; i < historicalData.size(); i++) {
            BigDecimal close = historicalData.get(i).getClose();
            ema = close.subtract(ema).multiply(multiplier).add(ema);
        }

        return ema;
    }

    // 거래량 분석
    private VolumeAnalysis analyzeVolume(List<StockData> historicalData) {
        VolumeAnalysis analysis = new VolumeAnalysis();

        if (historicalData.size() < 20) {
            return analysis;
        }

        StockData currentData = historicalData.get(historicalData.size() - 1);

        // 20일 평균 거래량
        BigDecimal avgVolume20 = BigDecimal.ZERO;
        for (int i = historicalData.size() - 20; i < historicalData.size(); i++) {
            avgVolume20 = avgVolume20.add(historicalData.get(i).getVolume());
        }
        avgVolume20 = avgVolume20.divide(new BigDecimal("20"), 2, RoundingMode.HALF_UP);

        // 5일 평균 거래량
        BigDecimal avgVolume5 = BigDecimal.ZERO;
        for (int i = historicalData.size() - 5; i < historicalData.size(); i++) {
            avgVolume5 = avgVolume5.add(historicalData.get(i).getVolume());
        }
        avgVolume5 = avgVolume5.divide(new BigDecimal("5"), 2, RoundingMode.HALF_UP);

        if (avgVolume20.compareTo(BigDecimal.ZERO) > 0) {
            analysis.volumeRatio = currentData.getVolume().divide(avgVolume20, 2, RoundingMode.HALF_UP);
            analysis.avgVolumeRatio = avgVolume5.divide(avgVolume20, 2, RoundingMode.HALF_UP);
            analysis.isVolumeSurge = analysis.volumeRatio.compareTo(VOLUME_SURGE_RATIO) > 0;
            analysis.isVolumeIncreasing = avgVolume5.compareTo(avgVolume20) > 0;
        }

        return analysis;
    }

    // 가격 모멘텀 계산
    private BigDecimal calculatePriceMomentum(List<StockData> historicalData, int period) {
        if (historicalData.size() < period + 1) return BigDecimal.ZERO;

        BigDecimal currentPrice = historicalData.get(historicalData.size() - 1).getClose();
        BigDecimal pastPrice = historicalData.get(historicalData.size() - period - 1).getClose();

        if (pastPrice.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;

        return currentPrice.subtract(pastPrice).divide(pastPrice, 4, RoundingMode.HALF_UP);
    }

    // 이동평균 수렴/발산 분석
    private MAConvergence analyzeMAConvergence(BigDecimal shortSMA, BigDecimal mediumSMA,
                                               BigDecimal longSMA, BigDecimal veryLongSMA) {
        MAConvergence convergence = new MAConvergence();

        // 퍼펙트 불리시 오더: short > medium > long > veryLong
        convergence.isPerfectBullishOrder =
                shortSMA.compareTo(mediumSMA) > 0 &&
                        mediumSMA.compareTo(longSMA) > 0 &&
                        longSMA.compareTo(veryLongSMA) > 0;

        // 퍼펙트 베어리시 오더: short < medium < long < veryLong
        convergence.isPerfectBearishOrder =
                shortSMA.compareTo(mediumSMA) < 0 &&
                        mediumSMA.compareTo(longSMA) < 0 &&
                        longSMA.compareTo(veryLongSMA) < 0;

        // 일반 트렌드
        convergence.isBullishTrend = shortSMA.compareTo(longSMA) > 0;
        convergence.isBearishTrend = shortSMA.compareTo(longSMA) < 0;

        return convergence;
    }

    // 지지/저항 레벨 분석
    private SupportResistance analyzeSupportResistance(List<StockData> historicalData,
                                                       BigDecimal shortSMA, BigDecimal longSMA) {
        SupportResistance levels = new SupportResistance();

        if (historicalData.size() < 20) {
            return levels;
        }

        BigDecimal currentPrice = historicalData.get(historicalData.size() - 1).getClose();

        // 이동평균선을 지지/저항으로 활용
        BigDecimal supportLevel = longSMA.min(shortSMA);
        BigDecimal resistanceLevel = longSMA.max(shortSMA);

        // 최근 20일간 고가/저가
        BigDecimal recentHigh = BigDecimal.ZERO;
        BigDecimal recentLow = new BigDecimal("999999");

        for (int i = historicalData.size() - 20; i < historicalData.size(); i++) {
            StockData data = historicalData.get(i);
            if (data.getHigh().compareTo(recentHigh) > 0) recentHigh = data.getHigh();
            if (data.getLow().compareTo(recentLow) < 0) recentLow = data.getLow();
        }

        // 지지선 근접: 현재가가 지지선의 ±2% 이내
        BigDecimal supportThreshold = supportLevel.multiply(new BigDecimal("0.02"));
        levels.nearSupport = currentPrice.subtract(supportLevel).abs().compareTo(supportThreshold) <= 0;

        // 저항선 근접: 현재가가 저항선의 ±2% 이내
        BigDecimal resistanceThreshold = resistanceLevel.multiply(new BigDecimal("0.02"));
        levels.nearResistance = currentPrice.subtract(resistanceLevel).abs().compareTo(resistanceThreshold) <= 0;

        return levels;
    }

    // 볼린저 밴드 포지션
    private BollingerPosition getBollingerPosition(List<StockData> historicalData, int period) {
        BollingerPosition position = new BollingerPosition();

        if (historicalData.size() < period) {
            return position;
        }

        // 중간 밴드 (SMA)
        BigDecimal middleBand = TechnicalIndicatorCalculator.calculateSMA(historicalData, period);

        // 표준편차 계산
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = historicalData.size() - period; i < historicalData.size(); i++) {
            BigDecimal diff = historicalData.get(i).getClose().subtract(middleBand);
            sum = sum.add(diff.multiply(diff));
        }

        BigDecimal variance = sum.divide(new BigDecimal(period), 6, RoundingMode.HALF_UP);
        BigDecimal stdDev = BigDecimal.valueOf(Math.sqrt(variance.doubleValue()));

        // 상단/하단 밴드
        BigDecimal upperBand = middleBand.add(stdDev.multiply(new BigDecimal("2")));
        BigDecimal lowerBand = middleBand.subtract(stdDev.multiply(new BigDecimal("2")));

        BigDecimal currentPrice = historicalData.get(historicalData.size() - 1).getClose();

        // 밴드 근접 판단 (밴드 폭의 10% 이내)
        BigDecimal bandWidth = upperBand.subtract(lowerBand);
        BigDecimal threshold = bandWidth.multiply(new BigDecimal("0.1"));

        position.isNearUpperBand = currentPrice.subtract(upperBand).abs().compareTo(threshold) <= 0;
        position.isNearLowerBand = currentPrice.subtract(lowerBand).abs().compareTo(threshold) <= 0;

        return position;
    }

    // SMA 기울기 계산 (트렌드 강도)
    private BigDecimal calculateSMASlope(List<StockData> historicalData, int period, int lookback) {
        if (historicalData.size() < period + lookback) {
            return BigDecimal.ZERO;
        }

        BigDecimal currentSMA = TechnicalIndicatorCalculator.calculateSMA(historicalData, period);
        List<StockData> pastData = historicalData.subList(0, historicalData.size() - lookback);
        BigDecimal pastSMA = TechnicalIndicatorCalculator.calculateSMA(pastData, period);

        if (pastSMA.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return currentSMA.subtract(pastSMA).divide(pastSMA, 4, RoundingMode.HALF_UP);
    }

    // 횡보 구간 체크
    private boolean checkConsolidation(List<StockData> historicalData, int period) {
        if (historicalData.size() < period) {
            return false;
        }

        BigDecimal high = BigDecimal.ZERO;
        BigDecimal low = new BigDecimal("999999");

        for (int i = historicalData.size() - period; i < historicalData.size(); i++) {
            StockData data = historicalData.get(i);
            if (data.getHigh().compareTo(high) > 0) high = data.getHigh();
            if (data.getLow().compareTo(low) < 0) low = data.getLow();
        }

        // 변동폭이 5% 이하면 횡보로 판단
        if (low.compareTo(BigDecimal.ZERO) == 0) return false;

        BigDecimal range = high.subtract(low).divide(low, 4, RoundingMode.HALF_UP);
        return range.compareTo(new BigDecimal("0.05")) <= 0;
    }

    // 돌파 체크
    private boolean checkBreakout(List<StockData> historicalData, BigDecimal currentPrice, BigDecimal sma) {
        if (historicalData.size() < 2) {
            return false;
        }

        BigDecimal prevPrice = historicalData.get(historicalData.size() - 2).getClose();

        // 이전에는 SMA 아래였다가 현재 돌파
        boolean breakoutAbove = prevPrice.compareTo(sma) <= 0 && currentPrice.compareTo(sma) > 0;

        // 돌파 강도 확인 (SMA 대비 2% 이상 돌파)
        if (breakoutAbove) {
            BigDecimal breakoutStrength = currentPrice.subtract(sma).divide(sma, 4, RoundingMode.HALF_UP);
            return breakoutStrength.compareTo(BREAKOUT_STRENGTH) > 0;
        }

        return false;
    }

    // === 내부 클래스들 ===

    // 거래량 분석 정보
    private static class VolumeAnalysis {
        BigDecimal volumeRatio = BigDecimal.ONE;
        BigDecimal avgVolumeRatio = BigDecimal.ONE;
        boolean isVolumeSurge = false;
        boolean isVolumeIncreasing = false;
    }

    // 이동평균 수렴/발산 정보
    private static class MAConvergence {
        boolean isPerfectBullishOrder = false;
        boolean isPerfectBearishOrder = false;
        boolean isBullishTrend = false;
        boolean isBearishTrend = false;
    }

    // 지지/저항 레벨 정보
    private static class SupportResistance {
        boolean nearSupport = false;
        boolean nearResistance = false;
    }

    // 볼린저 밴드 포지션 정보
    private static class BollingerPosition {
        boolean isNearUpperBand = false;
        boolean isNearLowerBand = false;
    }
}