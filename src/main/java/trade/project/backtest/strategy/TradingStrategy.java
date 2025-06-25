package trade.project.backtest.strategy;

import trade.project.backtest.dto.StockData;
import trade.project.backtest.dto.BackTestRequest;

import java.util.List;

public interface TradingStrategy {
    
    /**
     * 거래 신호를 생성합니다.
     * @param currentData 현재 주식 데이터
     * @param historicalData 과거 주식 데이터
     * @param request 백트래킹 요청 정보
     * @return 거래 신호 (BUY, SELL, HOLD)
     */
    String generateSignal(StockData currentData, List<StockData> historicalData, BackTestRequest request);
    
    /**
     * 전략 이름을 반환합니다.
     * @return 전략 이름
     */
    String getStrategyName();
    
    /**
     * 전략 설명을 반환합니다.
     * @return 전략 설명
     */
    String getStrategyDescription();
} 