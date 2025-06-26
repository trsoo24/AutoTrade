package trade.project.backtest.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class StrategyFactory {
    
    private final List<TradingStrategy> strategies;
    
    private final Map<String, TradingStrategy> strategyMap = new ConcurrentHashMap<>();
    
    /**
     * 전략 맵을 초기화합니다.
     */
    @PostConstruct
    public void initializeStrategyMap() {
        log.info("전략 팩토리 초기화 시작 - {} 개의 전략 로드", strategies.size());
        
        for (TradingStrategy strategy : strategies) {
            if (strategy.getStrategyName() != null) {
                String strategyName = strategy.getStrategyName().toUpperCase();
                strategyMap.put(strategyName, strategy);
                log.info("전략 등록: {} - {}", strategyName, strategy.getStrategyDescription());
            }
        }
        
        log.info("전략 팩토리 초기화 완료 - {} 개의 전략 등록됨", strategyMap.size());
    }
    
    /**
     * 전략 이름으로 전략을 가져옵니다.
     * @param strategyName 전략 이름
     * @return 전략 객체
     */
    public TradingStrategy getStrategy(String strategyName) {
        if (strategyName == null) {
            return null;
        }
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
        return strategyMap.keySet().stream()
                .sorted()
                .collect(Collectors.toList());
    }
} 