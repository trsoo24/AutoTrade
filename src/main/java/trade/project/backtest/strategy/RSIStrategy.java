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
        BigDecimal currentRSI = TechnicalIndicatorCalculator.calculateRSI(historicalData, rsiPeriod);
        
        if (currentRSI == null) {
            return "HOLD";
        }
        
        // 이전 RSI 값
        BigDecimal prevRSI = null;
        if (historicalData.size() > 1) {
            List<StockData> prevHistoricalData = historicalData.subList(0, historicalData.size() - 1);
            prevRSI = TechnicalIndicatorCalculator.calculateRSI(prevHistoricalData, rsiPeriod);
        }
        
        // 과매도 상태에서 반등 (매수 신호)
        if (prevRSI != null && prevRSI.compareTo(BigDecimal.valueOf(oversold)) <= 0 && 
            currentRSI.compareTo(BigDecimal.valueOf(oversold)) > 0) {
            log.debug("RSI 매수 신호: RSI가 과매도 구간에서 반등 ({} -> {})", prevRSI, currentRSI);
            return "BUY";
        }
        
        // 과매수 상태에서 하락 (매도 신호)
        if (prevRSI != null && prevRSI.compareTo(BigDecimal.valueOf(overbought)) >= 0 && 
            currentRSI.compareTo(BigDecimal.valueOf(overbought)) < 0) {
            log.debug("RSI 매도 신호: RSI가 과매수 구간에서 하락 ({} -> {})", prevRSI, currentRSI);
            return "SELL";
        }
        
        // 현재 RSI 값에 따른 신호
        if (currentRSI.compareTo(BigDecimal.valueOf(oversold)) <= 0) {
            log.debug("RSI 과매도 상태: {}", currentRSI);
            return "BUY";
        }
        
        if (currentRSI.compareTo(BigDecimal.valueOf(overbought)) >= 0) {
            log.debug("RSI 과매수 상태: {}", currentRSI);
            return "SELL";
        }
        
        return "HOLD";
    }
    
    @Override
    public String getStrategyName() {
        return "RSI";
    }
    
    @Override
    public String getStrategyDescription() {
        return "Relative Strength Index Strategy";
    }
} 