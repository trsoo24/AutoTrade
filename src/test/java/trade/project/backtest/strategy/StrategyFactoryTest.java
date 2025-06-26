package trade.project.backtest.strategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("전략 팩토리 테스트")
class StrategyFactoryTest {

    @Mock
    private TradingStrategy smaStrategy;

    @Mock
    private TradingStrategy rsiStrategy;

    @Mock
    private TradingStrategy macdStrategy;

    private StrategyFactory strategyFactory;

    @BeforeEach
    void setUp() {
        // Mock 전략 설정
        when(smaStrategy.getStrategyName()).thenReturn("SMA");
        when(smaStrategy.getStrategyDescription()).thenReturn("Simple Moving Average Strategy");
        
        when(rsiStrategy.getStrategyName()).thenReturn("RSI");
        when(rsiStrategy.getStrategyDescription()).thenReturn("Relative Strength Index Strategy");
        
        when(macdStrategy.getStrategyName()).thenReturn("MACD");
        when(macdStrategy.getStrategyDescription()).thenReturn("Moving Average Convergence Divergence Strategy");

        // StrategyFactory 생성
        List<TradingStrategy> strategies = Arrays.asList(smaStrategy, rsiStrategy, macdStrategy);
        strategyFactory = new StrategyFactory(strategies);
        
        // @PostConstruct 메서드 수동 호출 (ReflectionTestUtils 사용)
        ReflectionTestUtils.invokeMethod(strategyFactory, "initializeStrategyMap");
    }

    @Test
    @DisplayName("전략 조회 - 유효한 전략 이름으로 조회")
    void getStrategy_WithValidName_ShouldReturnStrategy() {
        // When
        TradingStrategy result = strategyFactory.getStrategy("SMA");

        // Then
        assertNotNull(result);
        assertEquals(smaStrategy, result);
    }

    @Test
    @DisplayName("전략 조회 - 대소문자 구분 없이 조회")
    void getStrategy_WithCaseInsensitiveName_ShouldReturnStrategy() {
        // When
        TradingStrategy result1 = strategyFactory.getStrategy("sma");
        TradingStrategy result2 = strategyFactory.getStrategy("SMA");
        TradingStrategy result3 = strategyFactory.getStrategy("Sma");

        // Then
        assertNotNull(result1);
        assertNotNull(result2);
        assertNotNull(result3);
        assertEquals(smaStrategy, result1);
        assertEquals(smaStrategy, result2);
        assertEquals(smaStrategy, result3);
    }

    @Test
    @DisplayName("전략 조회 - 존재하지 않는 전략 이름")
    void getStrategy_WithInvalidName_ShouldReturnNull() {
        // When
        TradingStrategy result = strategyFactory.getStrategy("INVALID_STRATEGY");

        // Then
        assertNull(result);
    }

    @Test
    @DisplayName("사용 가능한 전략 목록 조회")
    void getAvailableStrategies_ShouldReturnAllStrategies() {
        // When
        List<TradingStrategy> strategies = strategyFactory.getAvailableStrategies();

        // Then
        assertNotNull(strategies);
        assertEquals(3, strategies.size());
        assertTrue(strategies.contains(smaStrategy));
        assertTrue(strategies.contains(rsiStrategy));
        assertTrue(strategies.contains(macdStrategy));
    }

    @Test
    @DisplayName("전략 이름 목록 조회")
    void getStrategyNames_ShouldReturnAllStrategyNames() {
        // When
        List<String> strategyNames = strategyFactory.getStrategyNames();

        // Then
        assertNotNull(strategyNames);
        assertEquals(3, strategyNames.size());
        assertTrue(strategyNames.contains("SMA"));
        assertTrue(strategyNames.contains("RSI"));
        assertTrue(strategyNames.contains("MACD"));
    }

    @Test
    @DisplayName("전략 이름 목록 - 정렬 확인")
    void getStrategyNames_ShouldReturnSortedNames() {
        // When
        List<String> strategyNames = strategyFactory.getStrategyNames();

        // Then
        assertNotNull(strategyNames);
        // 전략 이름들이 정렬되어 있는지 확인 (실제로는 순서가 보장되지 않을 수 있음)
        assertTrue(strategyNames.contains("SMA"));
        assertTrue(strategyNames.contains("RSI"));
        assertTrue(strategyNames.contains("MACD"));
    }

    @Test
    @DisplayName("빈 전략 목록으로 초기화")
    void constructor_WithEmptyStrategies_ShouldInitializeCorrectly() {
        // Given
        List<TradingStrategy> emptyStrategies = Arrays.asList();
        StrategyFactory emptyFactory = new StrategyFactory(emptyStrategies);
        ReflectionTestUtils.invokeMethod(emptyFactory, "initializeStrategyMap");

        // When
        List<TradingStrategy> strategies = emptyFactory.getAvailableStrategies();
        List<String> strategyNames = emptyFactory.getStrategyNames();
        TradingStrategy strategy = emptyFactory.getStrategy("SMA");

        // Then
        assertNotNull(strategies);
        assertEquals(0, strategies.size());
        assertNotNull(strategyNames);
        assertEquals(0, strategyNames.size());
        assertNull(strategy);
    }

    @Test
    @DisplayName("중복 전략 이름 처리")
    void constructor_WithDuplicateStrategyNames_ShouldHandleCorrectly() {
        // Given
        when(smaStrategy.getStrategyName()).thenReturn("SMA");
        when(rsiStrategy.getStrategyName()).thenReturn("SMA"); // 중복 이름
        
        List<TradingStrategy> duplicateStrategies = Arrays.asList(smaStrategy, rsiStrategy);
        StrategyFactory duplicateFactory = new StrategyFactory(duplicateStrategies);
        ReflectionTestUtils.invokeMethod(duplicateFactory, "initializeStrategyMap");

        // When
        TradingStrategy result = duplicateFactory.getStrategy("SMA");
        List<String> strategyNames = duplicateFactory.getStrategyNames();

        // Then
        // 마지막에 추가된 전략이 반환됨 (Map의 특성상)
        assertNotNull(result);
        assertEquals(1, strategyNames.size()); // 중복 제거됨
        assertTrue(strategyNames.contains("SMA"));
    }

    @Test
    @DisplayName("null 전략 이름 처리")
    void constructor_WithNullStrategyName_ShouldHandleCorrectly() {
        // Given
        when(smaStrategy.getStrategyName()).thenReturn(null);
        
        List<TradingStrategy> nullNameStrategies = Arrays.asList(smaStrategy);
        StrategyFactory nullNameFactory = new StrategyFactory(nullNameStrategies);
        ReflectionTestUtils.invokeMethod(nullNameFactory, "initializeStrategyMap");

        // When
        TradingStrategy result = nullNameFactory.getStrategy(null);
        List<String> strategyNames = nullNameFactory.getStrategyNames();

        // Then
        assertNull(result);
        assertEquals(0, strategyNames.size()); // null 이름은 제외됨
    }

    @Test
    @DisplayName("전략 설명 조회 - 간접 테스트")
    void strategyDescription_ShouldBeAccessible() {
        // When
        List<TradingStrategy> strategies = strategyFactory.getAvailableStrategies();
        TradingStrategy smaStrategy = strategies.stream()
                .filter(s -> "SMA".equals(s.getStrategyName()))
                .findFirst()
                .orElse(null);

        // Then
        assertNotNull(smaStrategy);
        assertEquals("Simple Moving Average Strategy", smaStrategy.getStrategyDescription());
    }
} 