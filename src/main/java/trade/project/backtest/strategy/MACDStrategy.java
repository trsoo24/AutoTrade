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
public class MACDStrategy implements TradingStrategy {
    
    @Override
    public String generateSignal(StockData currentData, List<StockData> historicalData, BackTestRequest request) {
        if (currentData == null || historicalData == null || historicalData.isEmpty()) {
            return "HOLD";
        }
        
        int fastPeriod = request.getMacdFastPeriod() != null ? request.getMacdFastPeriod() : 12;
        int slowPeriod = request.getMacdSlowPeriod() != null ? request.getMacdSlowPeriod() : 26;
        int signalPeriod = request.getMacdSignalPeriod() != null ? request.getMacdSignalPeriod() : 9;
        
        // 충분한 데이터가 있는지 확인
        if (historicalData.size() < slowPeriod + signalPeriod) {
            return "HOLD";
        }
        
        // MACD 계산
        TechnicalIndicatorCalculator.MACDResult currentMACD = TechnicalIndicatorCalculator.calculateMACD(
                historicalData, fastPeriod, slowPeriod, signalPeriod);
        
        if (currentMACD == null) {
            return "HOLD";
        }
        
        // 이전 MACD 값
        TechnicalIndicatorCalculator.MACDResult prevMACD = null;
        if (historicalData.size() > 1) {
            List<StockData> prevHistoricalData = historicalData.subList(0, historicalData.size() - 1);
            prevMACD = TechnicalIndicatorCalculator.calculateMACD(
                    prevHistoricalData, fastPeriod, slowPeriod, signalPeriod);
        }
        
        // MACD가 시그널선을 상향 돌파 (매수 신호)
        if (prevMACD != null && 
            prevMACD.getMacd().compareTo(prevMACD.getMacdSignal()) <= 0 && 
            currentMACD.getMacd().compareTo(currentMACD.getMacdSignal()) > 0) {
            log.debug("MACD 매수 신호: MACD가 시그널선을 상향 돌파 (MACD: {}, Signal: {})", 
                    currentMACD.getMacd(), currentMACD.getMacdSignal());
            return "BUY";
        }
        
        // MACD가 시그널선을 하향 돌파 (매도 신호)
        if (prevMACD != null && 
            prevMACD.getMacd().compareTo(prevMACD.getMacdSignal()) >= 0 && 
            currentMACD.getMacd().compareTo(currentMACD.getMacdSignal()) < 0) {
            log.debug("MACD 매도 신호: MACD가 시그널선을 하향 돌파 (MACD: {}, Signal: {})", 
                    currentMACD.getMacd(), currentMACD.getMacdSignal());
            return "SELL";
        }
        
        // MACD 히스토그램 변화에 따른 신호
        if (prevMACD != null) {
            BigDecimal currentHistogram = currentMACD.getMacd().subtract(currentMACD.getMacdSignal());
            BigDecimal prevHistogram = prevMACD.getMacd().subtract(prevMACD.getMacdSignal());
            
            // 히스토그램이 음수에서 양수로 전환 (매수 신호)
            if (prevHistogram.compareTo(BigDecimal.ZERO) <= 0 && 
                currentHistogram.compareTo(BigDecimal.ZERO) > 0) {
                log.debug("MACD 히스토그램 매수 신호: 음수에서 양수로 전환");
                return "BUY";
            }
            
            // 히스토그램이 양수에서 음수로 전환 (매도 신호)
            if (prevHistogram.compareTo(BigDecimal.ZERO) >= 0 && 
                currentHistogram.compareTo(BigDecimal.ZERO) < 0) {
                log.debug("MACD 히스토그램 매도 신호: 양수에서 음수로 전환");
                return "SELL";
            }
        }
        
        return "HOLD";
    }
    
    @Override
    public String getStrategyName() {
        return "MACD";
    }
    
    @Override
    public String getStrategyDescription() {
        return "Moving Average Convergence Divergence Strategy";
    }
} 