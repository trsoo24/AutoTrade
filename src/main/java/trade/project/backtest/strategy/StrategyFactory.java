package trade.project.backtest.strategy;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class StrategyFactory {
    
    private final List<TradingStrategy> strategies;
    
    private Map<String, TradingStrategy> strategyMap;
    
    /**
     * 전략 맵을 초기화합니다.
     */
    private void initializeStrategyMap() {
        if (strategyMap == null) {
            strategyMap = strategies.stream()
                    .collect(Collectors.toMap(
                            TradingStrategy::getStrategyName,
                            Function.identity()
                    ));
        }
    }
    
    /**
     * 전략 이름으로 전략을 가져옵니다.
     * @param strategyName 전략 이름
     * @return 전략 객체
     */
    public TradingStrategy getStrategy(String strategyName) {
        initializeStrategyMap();
        return strategyMap.get(strategyName.toUpperCase());
    }
    
    /**
     * 사용 가능한 전략 목록을 반환합니다.
     * @return 전략 목록
     */
    public List<TradingStrategy> getAvailableStrategies() {
        return strategies;
    }
    
    /**
     * 전략 이름 목록을 반환합니다.
     * @return 전략 이름 목록
     */
    public List<String> getStrategyNames() {
        initializeStrategyMap();
        return strategyMap.keySet().stream().collect(Collectors.toList());
    }
} 