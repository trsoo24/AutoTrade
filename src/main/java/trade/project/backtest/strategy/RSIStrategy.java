package trade.project.backtest.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import trade.project.backtest.dto.BackTestRequest;
import trade.project.backtest.dto.StockData;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Component
public class RSIStrategy implements TradingStrategy {
    
    @Override
    public String generateSignal(StockData currentData, List<StockData> historicalData, BackTestRequest request) {
        if (currentData == null || historicalData == null || historicalData.isEmpty()) {
            return "HOLD";
        }
        
        int rsiPeriod = request.getRsiPeriod() != null ? request.getRsiPeriod() : 14;
        int overbought = request.getRsiOverbought() != null ? request.getRsiOverbought() : 70;
        int oversold = request.getRsiOversold() != null ? request.getRsiOversold() : 30;
        
        // 충분한 데이터가 있는지 확인
        if (historicalData.size() < rsiPeriod + 1) {
            return "HOLD";
        }
        
        // RSI 계산
        currentData.calculateRSI(historicalData, rsiPeriod);
        
        if (currentData.getRsi() == null) {
            return "HOLD";
        }
        
        BigDecimal currentRSI = currentData.getRsi();
        
        // 이전 RSI 값
        BigDecimal previousRSI = null;
        if (historicalData.size() > 1) {
            StockData prevData = historicalData.get(historicalData.size() - 2);
            prevData.calculateRSI(historicalData.subList(0, historicalData.size() - 1), rsiPeriod);
            previousRSI = prevData.getRsi();
        }
        
        // 과매도 구간에서 상승 반전 (매수 신호)
        if (previousRSI != null && 
            previousRSI.compareTo(BigDecimal.valueOf(oversold)) <= 0 && 
            currentRSI.compareTo(BigDecimal.valueOf(oversold)) > 0) {
            log.debug("RSI Oversold reversal detected: Previous RSI = {}, Current RSI = {}", previousRSI, currentRSI);
            return "BUY";
        }
        
        // 과매수 구간에서 하락 반전 (매도 신호)
        if (previousRSI != null && 
            previousRSI.compareTo(BigDecimal.valueOf(overbought)) >= 0 && 
            currentRSI.compareTo(BigDecimal.valueOf(overbought)) < 0) {
            log.debug("RSI Overbought reversal detected: Previous RSI = {}, Current RSI = {}", previousRSI, currentRSI);
            return "SELL";
        }
        
        // RSI가 과매도 구간에 있을 때 (매수 신호)
        if (currentRSI.compareTo(BigDecimal.valueOf(oversold)) <= 0) {
            log.debug("RSI Oversold condition: RSI = {}", currentRSI);
            return "BUY";
        }
        
        // RSI가 과매수 구간에 있을 때 (매도 신호)
        if (currentRSI.compareTo(BigDecimal.valueOf(overbought)) >= 0) {
            log.debug("RSI Overbought condition: RSI = {}", currentRSI);
            return "SELL";
        }
        
        // RSI 중립 구간 (30-70)에서의 신호
        BigDecimal neutralUpper = BigDecimal.valueOf(60);
        BigDecimal neutralLower = BigDecimal.valueOf(40);
        
        // RSI가 중립 상단에서 하락할 때 (매도 신호)
        if (previousRSI != null && 
            previousRSI.compareTo(neutralUpper) >= 0 && 
            currentRSI.compareTo(neutralUpper) < 0) {
            log.debug("RSI Neutral upper reversal: Previous RSI = {}, Current RSI = {}", previousRSI, currentRSI);
            return "SELL";
        }
        
        // RSI가 중립 하단에서 상승할 때 (매수 신호)
        if (previousRSI != null && 
            previousRSI.compareTo(neutralLower) <= 0 && 
            currentRSI.compareTo(neutralLower) > 0) {
            log.debug("RSI Neutral lower reversal: Previous RSI = {}, Current RSI = {}", previousRSI, currentRSI);
            return "BUY";
        }
        
        return "HOLD";
    }
    
    @Override
    public String getStrategyName() {
        return "RSI";
    }
    
    @Override
    public String getStrategyDescription() {
        return "Relative Strength Index Strategy - RSI가 과매도 구간에서 상승 반전할 때 매수, 과매수 구간에서 하락 반전할 때 매도";
    }
} 