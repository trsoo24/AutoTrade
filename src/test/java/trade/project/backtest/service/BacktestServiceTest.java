package trade.project.backtest.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import trade.project.api.client.KisApiClient;
import trade.project.backtest.dto.BackTestRequest;
import trade.project.backtest.dto.BackTestResult;
import trade.project.backtest.dto.StockData;
import trade.project.backtest.engine.BacktestEngine;
import trade.project.backtest.strategy.StrategyFactory;
import trade.project.backtest.strategy.TradingStrategy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("백트래킹 서비스 테스트")
class BacktestServiceTest {

    @Mock
    private BacktestEngine backtestEngine;

    @Mock
    private StrategyFactory strategyFactory;

    @Mock
    private KisApiClient kisApiClient;

    @Mock
    private TradingStrategy tradingStrategy;

    @InjectMocks
    private BacktestService backtestService;

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

        // 테스트 주식 데이터 설정
        testStockData = Arrays.asList(
                StockData.builder()
                        .date(LocalDate.of(2024, 1, 1))
                        .close(new BigDecimal("50000"))
                        .build(),
                StockData.builder()
                        .date(LocalDate.of(2024, 1, 2))
                        .close(new BigDecimal("51000"))
                        .build()
        );

        // Mock 설정 - lenient로 변경
        lenient().when(strategyFactory.getStrategy("SMA")).thenReturn(tradingStrategy);
        lenient().when(strategyFactory.getStrategy("INVALID")).thenReturn(null);
    }

    @Test
    @DisplayName("백트래킹 실행 - 성공적인 경우")
    void runBacktest_WithValidRequest_ShouldReturnResult() {
        // Given
        BackTestResult expectedResult = BackTestResult.builder()
                .stockCode("005930")
                .totalReturn(new BigDecimal("1000000"))
                .totalTrades(5)
                .build();

        when(strategyFactory.getStrategy("SMA")).thenReturn(tradingStrategy);
        when(kisApiClient.getStockDailyPrice(anyString(), anyString(), anyString()))
                .thenReturn(createMockApiResponse());
        when(backtestEngine.runBacktest(any(BackTestRequest.class), anyList()))
                .thenReturn(expectedResult);

        // When
        BackTestResult result = backtestService.runBacktest(testRequest);

        // Then
        assertNotNull(result);
        assertEquals("005930", result.getStockCode());
        assertEquals(new BigDecimal("1000000"), result.getTotalReturn());
        assertEquals(5, result.getTotalTrades());
        
        verify(strategyFactory).getStrategy("SMA");
        verify(kisApiClient).getStockDailyPrice("005930", "20240101", "20240131");
        verify(backtestEngine).runBacktest(any(BackTestRequest.class), anyList());
    }

    @Test
    @DisplayName("백트래킹 실행 - API 호출 실패 시 샘플 데이터 사용")
    void runBacktest_WhenApiFails_ShouldUseSampleData() {
        // Given
        BackTestResult expectedResult = BackTestResult.builder()
                .stockCode("005930")
                .totalReturn(new BigDecimal("500000"))
                .totalTrades(3)
                .build();

        when(strategyFactory.getStrategy("SMA")).thenReturn(tradingStrategy);
        when(kisApiClient.getStockDailyPrice(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("API Error"));
        when(backtestEngine.runBacktest(any(BackTestRequest.class), anyList()))
                .thenReturn(expectedResult);

        // When
        BackTestResult result = backtestService.runBacktest(testRequest);

        // Then
        assertNotNull(result);
        assertEquals("005930", result.getStockCode());
        
        // 샘플 데이터가 생성되어 백트래킹 엔진에 전달되었는지 확인
        verify(backtestEngine).runBacktest(any(BackTestRequest.class), argThat(dataList -> 
                !dataList.isEmpty() && dataList.size() > 0));
    }

    @Test
    @DisplayName("백트래킹 실행 - 주식 데이터가 없는 경우")
    void runBacktest_WithNoStockData_ShouldThrowException() {
        // Given
        when(strategyFactory.getStrategy("SMA")).thenReturn(tradingStrategy);
        when(kisApiClient.getStockDailyPrice(anyString(), anyString(), anyString()))
                .thenReturn(createEmptyApiResponse());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            backtestService.runBacktest(testRequest);
        });
        
        assertTrue(exception.getMessage().contains("주식 데이터를 찾을 수 없습니다"));
    }

    @Test
    @DisplayName("백트래킹 실행 - 지원하지 않는 전략")
    void runBacktest_WithInvalidStrategy_ShouldThrowException() {
        // Given
        when(strategyFactory.getStrategy("INVALID")).thenReturn(null);

        BackTestRequest invalidRequest = BackTestRequest.builder()
                .stockCode("005930")
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 1, 31))
                .strategy("INVALID")
                .initialCapital(new BigDecimal("10000000"))
                .build();

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            backtestService.runBacktest(invalidRequest);
        });
        
        assertTrue(exception.getMessage().contains("백트래킹 실행 실패"));
    }

    @Test
    @DisplayName("사용 가능한 전략 목록 조회")
    void getAvailableStrategies_ShouldReturnStrategyList() {
        // Given
        List<TradingStrategy> strategies = Arrays.asList(tradingStrategy);
        when(strategyFactory.getAvailableStrategies()).thenReturn(strategies);

        // When
        List<Map<String, String>> result = backtestService.getAvailableStrategies();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("SMA", result.get(0).get("name"));
        assertEquals("Simple Moving Average Strategy", result.get(0).get("description"));
    }

    @Test
    @DisplayName("요청 유효성 검사 - 유효한 요청")
    void validateRequest_WithValidRequest_ShouldNotThrowException() {
        // Given
        when(strategyFactory.getStrategy("SMA")).thenReturn(tradingStrategy);

        // When & Then
        assertDoesNotThrow(() -> {
            backtestService.validateRequest(testRequest);
        });
    }

    @Test
    @DisplayName("요청 유효성 검사 - 주식 코드가 없는 경우")
    void validateRequest_WithNullStockCode_ShouldThrowException() {
        // Given
        BackTestRequest invalidRequest = BackTestRequest.builder()
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 1, 31))
                .strategy("SMA")
                .initialCapital(new BigDecimal("10000000"))
                .build();

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            backtestService.validateRequest(invalidRequest);
        });
        
        assertTrue(exception.getMessage().contains("주식 코드는 필수입니다"));
    }

    @Test
    @DisplayName("요청 유효성 검사 - 시작 날짜가 없는 경우")
    void validateRequest_WithNullStartDate_ShouldThrowException() {
        // Given
        BackTestRequest invalidRequest = BackTestRequest.builder()
                .stockCode("005930")
                .endDate(LocalDate.of(2024, 1, 31))
                .strategy("SMA")
                .initialCapital(new BigDecimal("10000000"))
                .build();

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            backtestService.validateRequest(invalidRequest);
        });
        
        assertTrue(exception.getMessage().contains("시작 날짜는 필수입니다"));
    }

    @Test
    @DisplayName("요청 유효성 검사 - 종료 날짜가 없는 경우")
    void validateRequest_WithNullEndDate_ShouldThrowException() {
        // Given
        BackTestRequest invalidRequest = BackTestRequest.builder()
                .stockCode("005930")
                .startDate(LocalDate.of(2024, 1, 1))
                .strategy("SMA")
                .initialCapital(new BigDecimal("10000000"))
                .build();

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            backtestService.validateRequest(invalidRequest);
        });
        
        assertTrue(exception.getMessage().contains("종료 날짜는 필수입니다"));
    }

    @Test
    @DisplayName("요청 유효성 검사 - 시작 날짜가 종료 날짜보다 늦은 경우")
    void validateRequest_WithInvalidDateRange_ShouldThrowException() {
        // Given
        BackTestRequest invalidRequest = BackTestRequest.builder()
                .stockCode("005930")
                .startDate(LocalDate.of(2024, 1, 31))
                .endDate(LocalDate.of(2024, 1, 1))
                .strategy("SMA")
                .initialCapital(new BigDecimal("10000000"))
                .build();

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            backtestService.validateRequest(invalidRequest);
        });
        
        assertTrue(exception.getMessage().contains("시작 날짜는 종료 날짜보다 이전이어야 합니다"));
    }

    @Test
    @DisplayName("요청 유효성 검사 - 초기 자본금이 0 이하인 경우")
    void validateRequest_WithInvalidInitialCapital_ShouldThrowException() {
        // Given
        BackTestRequest invalidRequest = BackTestRequest.builder()
                .stockCode("005930")
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 1, 31))
                .strategy("SMA")
                .initialCapital(new BigDecimal("0"))
                .build();

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            backtestService.validateRequest(invalidRequest);
        });
        
        assertTrue(exception.getMessage().contains("초기 자본금은 0보다 커야 합니다"));
    }

    @Test
    @DisplayName("요청 유효성 검사 - 전략이 없는 경우")
    void validateRequest_WithNullStrategy_ShouldThrowException() {
        // Given
        BackTestRequest invalidRequest = BackTestRequest.builder()
                .stockCode("005930")
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 1, 31))
                .initialCapital(new BigDecimal("10000000"))
                .build();

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            backtestService.validateRequest(invalidRequest);
        });
        
        assertTrue(exception.getMessage().contains("전략은 필수입니다"));
    }

    @Test
    @DisplayName("요청 유효성 검사 - 지원하지 않는 전략")
    void validateRequest_WithUnsupportedStrategy_ShouldThrowException() {
        // Given
        when(strategyFactory.getStrategy("INVALID")).thenReturn(null);

        BackTestRequest invalidRequest = BackTestRequest.builder()
                .stockCode("005930")
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 1, 31))
                .strategy("INVALID")
                .initialCapital(new BigDecimal("10000000"))
                .build();

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            backtestService.validateRequest(invalidRequest);
        });
        
        assertTrue(exception.getMessage().contains("지원하지 않는 전략입니다"));
    }

    // 헬퍼 메서드들
    private Map<String, Object> createMockApiResponse() {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> output = new HashMap<>();
        Map<String, Object> output1 = new HashMap<>();
        
        output1.put("stck_bsop_date", "20240101");
        output1.put("stck_oprc", "50000");
        output1.put("stck_hgpr", "51000");
        output1.put("stck_lwpr", "49000");
        output1.put("stck_prpr", "50500");
        output1.put("cntg_vol", "1000000");
        
        output.put("output1", Arrays.asList(output1));
        response.put("output", output);
        
        return response;
    }

    private Map<String, Object> createEmptyApiResponse() {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> output = new HashMap<>();
        output.put("output1", Arrays.asList());
        response.put("output", output);
        return response;
    }
} 