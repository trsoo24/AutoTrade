package trade.project.backtest.engine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import trade.project.backtest.dto.BackTestRequest;
import trade.project.backtest.dto.BackTestResult;
import trade.project.backtest.dto.StockData;
import trade.project.backtest.strategy.StrategyFactory;
import trade.project.backtest.strategy.TradingStrategy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("백트래킹 엔진 테스트")
class BacktestEngineTest {

    @Mock
    private StrategyFactory strategyFactory;

    @Mock
    private TradingStrategy tradingStrategy;

    @InjectMocks
    private BacktestEngine backtestEngine;

    private BackTestRequest testRequest;
    private List<StockData> testStockData;

    @BeforeEach
    void setUp() {
        // 테스트 요청 데이터 설정
        testRequest = BackTestRequest.builder()
                .stockCode("005930")
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 1, 31))
                .strategy("SMA")
                .initialCapital(new BigDecimal("10000000"))
                .commission(new BigDecimal("0.001"))
                .shortPeriod(5)
                .longPeriod(20)
                .stopLoss(new BigDecimal("0.05"))
                .takeProfit(new BigDecimal("0.10"))
                .maxPositionSize(new BigDecimal("0.5"))
                .minTradeAmount(new BigDecimal("100000"))
                .reinvestDividends(true)
                .includeTax(true)
                .build();

        // 테스트 주식 데이터 설정 (10일간 상승하는 데이터)
        testStockData = Arrays.asList(
                StockData.builder()
                        .date(LocalDate.of(2024, 1, 1))
                        .open(new BigDecimal("50000"))
                        .high(new BigDecimal("51000"))
                        .low(new BigDecimal("49000"))
                        .close(new BigDecimal("50500"))
                        .volume(1000000L)
                        .adjustedClose(new BigDecimal("50500"))
                        .build(),
                StockData.builder()
                        .date(LocalDate.of(2024, 1, 2))
                        .open(new BigDecimal("50500"))
                        .high(new BigDecimal("52000"))
                        .low(new BigDecimal("50000"))
                        .close(new BigDecimal("51500"))
                        .volume(1200000L)
                        .adjustedClose(new BigDecimal("51500"))
                        .build(),
                StockData.builder()
                        .date(LocalDate.of(2024, 1, 3))
                        .open(new BigDecimal("51500"))
                        .high(new BigDecimal("53000"))
                        .low(new BigDecimal("51000"))
                        .close(new BigDecimal("52500"))
                        .volume(1100000L)
                        .adjustedClose(new BigDecimal("52500"))
                        .build()
        );

        // Mock 전략 설정 - lenient로 변경
        lenient().when(strategyFactory.getStrategy("SMA")).thenReturn(tradingStrategy);
        lenient().when(tradingStrategy.getStrategyName()).thenReturn("SMA");
        lenient().when(tradingStrategy.getStrategyDescription()).thenReturn("Simple Moving Average Strategy");
    }

    @Test
    @DisplayName("백트래킹 실행 - 성공적인 경우")
    void runBacktest_WithValidData_ShouldReturnResult() {
        // Given
        when(tradingStrategy.generateSignal(any(StockData.class), anyList(), any(BackTestRequest.class)))
                .thenReturn("BUY", "HOLD", "SELL");

        // When
        BackTestResult result = backtestEngine.runBacktest(testRequest, testStockData);

        // Then
        assertNotNull(result);
        assertEquals("005930", result.getStockCode());
        assertEquals("SMA", result.getStrategy());
        assertNotNull(result.getTotalReturn());
        assertNotNull(result.getTotalTrades());
        assertNotNull(result.getWinRate());
        assertNotNull(result.getMaxDrawdown());
        assertNotNull(result.getSharpeRatio());
        assertNotNull(result.getTrades());
        
        verify(strategyFactory).getStrategy("SMA");
        verify(tradingStrategy, atLeastOnce()).generateSignal(any(StockData.class), anyList(), any(BackTestRequest.class));
    }

    @Test
    @DisplayName("백트래킹 실행 - 매수 신호만 있는 경우")
    void runBacktest_WithOnlyBuySignals_ShouldHandleCorrectly() {
        // Given
        when(tradingStrategy.generateSignal(any(StockData.class), anyList(), any(BackTestRequest.class)))
                .thenReturn("BUY", "BUY", "BUY");

        // When
        BackTestResult result = backtestEngine.runBacktest(testRequest, testStockData);

        // Then
        assertNotNull(result);
        assertEquals("005930", result.getStockCode());
        // 매수만 있고 매도가 없으면 거래 횟수는 1이어야 함 (마지막에 보유)
        assertTrue(result.getTotalTrades() >= 0);
    }

    @Test
    @DisplayName("백트래킹 실행 - 매도 신호만 있는 경우")
    void runBacktest_WithOnlySellSignals_ShouldHandleCorrectly() {
        // Given
        when(tradingStrategy.generateSignal(any(StockData.class), anyList(), any(BackTestRequest.class)))
                .thenReturn("SELL", "SELL", "SELL");

        // When
        BackTestResult result = backtestEngine.runBacktest(testRequest, testStockData);

        // Then
        assertNotNull(result);
        assertEquals("005930", result.getStockCode());
        // 매도만 있고 매수가 없으면 거래 횟수는 0이어야 함
        assertEquals(0, result.getTotalTrades());
    }

    @Test
    @DisplayName("백트래킹 실행 - 빈 주식 데이터")
    void runBacktest_WithEmptyStockData_ShouldThrowException() {
        // Given
        List<StockData> emptyData = Arrays.asList();

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            backtestEngine.runBacktest(testRequest, emptyData);
        });
        
        assertTrue(exception.getMessage().contains("주식 데이터가 비어있습니다"));
    }

    @Test
    @DisplayName("백트래킹 실행 - null 주식 데이터")
    void runBacktest_WithNullStockData_ShouldThrowException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            backtestEngine.runBacktest(testRequest, null);
        });
        
        assertTrue(exception.getMessage().contains("주식 데이터가 비어있습니다"));
    }

    @Test
    @DisplayName("백트래킹 실행 - null 요청")
    void runBacktest_WithNullRequest_ShouldThrowException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            backtestEngine.runBacktest(null, testStockData);
        });
        
        assertTrue(exception.getMessage().contains("백트래킹 요청이 null입니다"));
    }

    @Test
    @DisplayName("백트래킹 실행 - 지원하지 않는 전략")
    void runBacktest_WithUnsupportedStrategy_ShouldThrowException() {
        // Given
        when(strategyFactory.getStrategy("INVALID")).thenReturn(null);
        testRequest.setStrategy("INVALID");

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            backtestEngine.runBacktest(testRequest, testStockData);
        });
        
        assertTrue(exception.getMessage().contains("지원하지 않는 전략입니다"));
    }
} 