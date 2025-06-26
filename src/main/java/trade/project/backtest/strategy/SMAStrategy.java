package trade.project.backtest.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import trade.project.backtest.dto.BackTestRequest;
import trade.project.backtest.dto.StockData;
import trade.project.backtest.util.TechnicalIndicatorCalculator;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Component
public class SMAStrategy implements TradingStrategy {
    
    @Override
    public String getStrategyName() {
        return "SMA";
    }
    
    @Override
    public String getStrategyDescription() {
        return "Simple Moving Average Strategy";
    }
    
    @Override
    public String generateSignal(StockData currentData, List<StockData> historicalData, BackTestRequest request) {
        if (currentData == null || historicalData == null || historicalData.isEmpty()) {
            return "HOLD";
        }

        int shortPeriod = request.getShortPeriod() != null ? request.getShortPeriod() : 5;
        int longPeriod = request.getLongPeriod() != null ? request.getLongPeriod() : 20;

        // 충분한 데이터가 있는지 확인
        if (historicalData.size() < longPeriod) {
            return "HOLD";
        }

        // 이동평균 계산
        BigDecimal shortSMA = TechnicalIndicatorCalculator.calculateSMA(historicalData, shortPeriod);
        BigDecimal longSMA = TechnicalIndicatorCalculator.calculateSMA(historicalData, longPeriod);

        if (shortSMA == null || longSMA == null) {
            return "HOLD";
        }

        // 이전 이동평균 값들
        BigDecimal prevShortSMA = null;
        BigDecimal prevLongSMA = null;

        if (historicalData.size() > 1) {
            List<StockData> prevHistoricalData = historicalData.subList(0, historicalData.size() - 1);
            prevShortSMA = TechnicalIndicatorCalculator.calculateSMA(prevHistoricalData, shortPeriod);
            prevLongSMA = TechnicalIndicatorCalculator.calculateSMA(prevHistoricalData, longPeriod);
        }

        // 골든 크로스 (단기선이 장기선을 상향 돌파)
        if (prevShortSMA != null && prevLongSMA != null) {
            boolean goldenCross = prevShortSMA.compareTo(prevLongSMA) <= 0 && 
                                 shortSMA.compareTo(longSMA) > 0;
            
            if (goldenCross) {
                log.debug("SMA Golden Cross detected: Short SMA = {}, Long SMA = {}", shortSMA, longSMA);
                return "BUY";
            }
        }

        // 데드 크로스 (단기선이 장기선을 하향 돌파)
        if (prevShortSMA != null && prevLongSMA != null) {
            boolean deadCross = prevShortSMA.compareTo(prevLongSMA) >= 0 && 
                               shortSMA.compareTo(longSMA) < 0;
            
            if (deadCross) {
                log.debug("SMA Dead Cross detected: Short SMA = {}, Long SMA = {}", shortSMA, longSMA);
                return "SELL";
            }
        }

        // 현재 가격이 이동평균선 위에 있는지 확인
        BigDecimal currentPrice = currentData.getClose();

        // 단기선이 장기선 위에 있고, 현재 가격이 단기선 위에 있으면 매수 신호
        if (shortSMA.compareTo(longSMA) > 0 && currentPrice.compareTo(shortSMA) > 0) {
            return "BUY";
        }

        // 단기선이 장기선 아래에 있고, 현재 가격이 단기선 아래에 있으면 매도 신호
        if (shortSMA.compareTo(longSMA) < 0 && currentPrice.compareTo(shortSMA) < 0) {
            return "SELL";
        }

        return "HOLD";
    }
} 