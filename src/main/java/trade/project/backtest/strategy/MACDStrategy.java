package trade.project.backtest.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import trade.project.backtest.dto.BackTestRequest;
import trade.project.backtest.dto.StockData;

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
        currentData.calculateMACD(historicalData, fastPeriod, slowPeriod, signalPeriod);
        
        if (currentData.getMacd() == null || currentData.getMacdSignal() == null) {
            return "HOLD";
        }
        
        BigDecimal currentMACD = currentData.getMacd();
        BigDecimal currentSignal = currentData.getMacdSignal();
        BigDecimal currentHistogram = currentData.getMacdHistogram();
        
        // 이전 MACD 값들
        BigDecimal previousMACD = null;
        BigDecimal previousSignal = null;
        BigDecimal previousHistogram = null;
        
        if (historicalData.size() > 1) {
            StockData prevData = historicalData.get(historicalData.size() - 2);
            prevData.calculateMACD(historicalData.subList(0, historicalData.size() - 1), fastPeriod, slowPeriod, signalPeriod);
            previousMACD = prevData.getMacd();
            previousSignal = prevData.getMacdSignal();
            previousHistogram = prevData.getMacdHistogram();
        }
        
        // MACD 골든 크로스 (MACD가 시그널선을 상향 돌파)
        if (previousMACD != null && previousSignal != null) {
            boolean goldenCross = previousMACD.compareTo(previousSignal) <= 0 && 
                                 currentMACD.compareTo(currentSignal) > 0;
            
            if (goldenCross) {
                log.debug("MACD Golden Cross detected: MACD = {}, Signal = {}", currentMACD, currentSignal);
                return "BUY";
            }
        }
        
        // MACD 데드 크로스 (MACD가 시그널선을 하향 돌파)
        if (previousMACD != null && previousSignal != null) {
            boolean deadCross = previousMACD.compareTo(previousSignal) >= 0 && 
                               currentMACD.compareTo(currentSignal) < 0;
            
            if (deadCross) {
                log.debug("MACD Dead Cross detected: MACD = {}, Signal = {}", currentMACD, currentSignal);
                return "SELL";
            }
        }
        
        // 히스토그램 반전 신호
        if (previousHistogram != null && currentHistogram != null) {
            // 히스토그램이 음수에서 양수로 전환 (매수 신호)
            if (previousHistogram.compareTo(BigDecimal.ZERO) < 0 && 
                currentHistogram.compareTo(BigDecimal.ZERO) > 0) {
                log.debug("MACD Histogram positive reversal: Previous = {}, Current = {}", previousHistogram, currentHistogram);
                return "BUY";
            }
            
            // 히스토그램이 양수에서 음수로 전환 (매도 신호)
            if (previousHistogram.compareTo(BigDecimal.ZERO) > 0 && 
                currentHistogram.compareTo(BigDecimal.ZERO) < 0) {
                log.debug("MACD Histogram negative reversal: Previous = {}, Current = {}", previousHistogram, currentHistogram);
                return "SELL";
            }
        }
        
        // MACD가 0선을 상향 돌파 (매수 신호)
        if (previousMACD != null && 
            previousMACD.compareTo(BigDecimal.ZERO) < 0 && 
            currentMACD.compareTo(BigDecimal.ZERO) > 0) {
            log.debug("MACD Zero line bullish crossover: Previous = {}, Current = {}", previousMACD, currentMACD);
            return "BUY";
        }
        
        // MACD가 0선을 하향 돌파 (매도 신호)
        if (previousMACD != null && 
            previousMACD.compareTo(BigDecimal.ZERO) > 0 && 
            currentMACD.compareTo(BigDecimal.ZERO) < 0) {
            log.debug("MACD Zero line bearish crossover: Previous = {}, Current = {}", previousMACD, currentMACD);
            return "SELL";
        }
        
        // MACD와 시그널선의 위치에 따른 신호
        if (currentMACD.compareTo(currentSignal) > 0 && currentMACD.compareTo(BigDecimal.ZERO) > 0) {
            // MACD가 시그널선 위에 있고 0선 위에 있으면 매수 신호
            return "BUY";
        }
        
        if (currentMACD.compareTo(currentSignal) < 0 && currentMACD.compareTo(BigDecimal.ZERO) < 0) {
            // MACD가 시그널선 아래에 있고 0선 아래에 있으면 매도 신호
            return "SELL";
        }
        
        return "HOLD";
    }
    
    @Override
    public String getStrategyName() {
        return "MACD";
    }
    
    @Override
    public String getStrategyDescription() {
        return "Moving Average Convergence Divergence Strategy - MACD가 시그널선을 상향 돌파할 때 매수, 하향 돌파할 때 매도";
    }
} 