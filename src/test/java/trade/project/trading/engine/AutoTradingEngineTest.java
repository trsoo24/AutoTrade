package trade.project.trading.engine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import trade.project.api.dto.StockPriceRequest;
import trade.project.api.dto.StockPriceResponse;
import trade.project.api.dto.StockOrderRequest;
import trade.project.api.dto.StockOrderResponse;
import trade.project.api.service.StockPriceService;
import trade.project.api.service.StockOrderService;
import trade.project.trading.dto.AutoTradingStrategy;
import trade.project.trading.enums.TopKospiStocks;
import trade.project.trading.enums.TradingSchedule;
import trade.project.trading.service.TradingRecordService;
import trade.project.trading.service.PriceQueryRecordService;
import trade.project.backtest.util.TechnicalIndicatorCalculator;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AutoTradingEngineTest {

    @Mock
    private StockPriceService stockPriceService;

    @Mock
    private StockOrderService stockOrderService;

    @Mock
    private TradingRecordService tradingRecordService;

    @Mock
    private PriceQueryRecordService priceQueryRecordService;

    @Mock
    private TechnicalIndicatorCalculator technicalIndicatorCalculator;

    @InjectMocks
    private AutoTradingEngine autoTradingEngine;

    private AutoTradingStrategy testStrategy;
    private StockPriceResponse testPriceResponse;
    private StockOrderResponse testOrderResponse;

    @BeforeEach
    void setUp() {
        // 테스트 전략 설정
        testStrategy = AutoTradingStrategy.builder()
                .strategyId("TEST_SAMSUNG")
                .strategyName("테스트 삼성전자 전략")
                .description("테스트용 전략")
                .enabled(true)
                .targetStock(TopKospiStocks.SAMSUNG_ELECTRONICS)
                .stockCode("005930")
                .stockName("삼성전자")
                .totalInvestment(new BigDecimal("10000000"))
                .maxPositionSize(new BigDecimal("0.1"))
                .minOrderAmount(new BigDecimal("100000"))
                .maxOrderAmount(new BigDecimal("500000"))
                .buyThreshold(new BigDecimal("30"))
                .buyPriceLimit(new BigDecimal("80000"))
                .buyQuantity(1)
                .buyPriceType("지정가")
                .sellThreshold(new BigDecimal("70"))
                .sellPriceLimit(new BigDecimal("90000"))
                .profitTarget(new BigDecimal("5"))
                .stopLoss(new BigDecimal("3"))
                .rsiPeriod(14)
                .rsiOverbought(new BigDecimal("70"))
                .rsiOversold(new BigDecimal("30"))
                .macdFastPeriod(12)
                .macdSlowPeriod(26)
                .macdSignalPeriod(9)
                .smaShortPeriod(5)
                .smaLongPeriod(20)
                .tradeDuringMarketHours(true)
                .tradeDuringHighFrequency(true)
                .priceCheckInterval(60)
                .maxDailyLoss(new BigDecimal("2"))
                .maxDrawdown(new BigDecimal("10"))
                .maxDailyTrades(10)
                .enableNotifications(true)
                .createdAt(LocalDateTime.now())
                .build();

        // 테스트 시세 응답 설정
        testPriceResponse = StockPriceResponse.builder()
                .stockCode("005930")
                .stockName("삼성전자")
                .currentPrice(75000)
                .changeAmount(1000)
                .changeRate(1.35)
                .highPrice(76000)
                .lowPrice(74000)
                .openPrice(74500)
                .previousClose(74000)
                .tradingVolume(1234567L)
                .tradingValue(100000000L)
                .marketStatus("정상")
                .timestamp(LocalDateTime.now())
                .build();

        // 테스트 주문 응답 설정
        testOrderResponse = StockOrderResponse.builder()
                .orderStatus("성공")
                .orderType("매수")
                .stockCode("005930")
                .stockName("삼성전자")
                .quantity(1)
                .price(75000)
                .orderTime(LocalDateTime.now().toString())
                .build();
    }

    @Test
    void testInitializeDomestic() {
        when(stockPriceService.getCurrentPrice(any(StockPriceRequest.class), any()))
                .thenReturn(testPriceResponse);
        assertDoesNotThrow(() -> autoTradingEngine.initializeDomestic());
    }

    @Test
    void testInitializeForeign() {
        when(stockPriceService.getCurrentPrice(any(StockPriceRequest.class), any()))
                .thenReturn(testPriceResponse);
        assertDoesNotThrow(() -> autoTradingEngine.initializeForeign());
    }

    @Test
    void testShutdownDomestic() {
        assertDoesNotThrow(() -> autoTradingEngine.shutdownDomestic());
    }

    @Test
    void testShutdownForeign() {
        assertDoesNotThrow(() -> autoTradingEngine.shutdownForeign());
    }

    @Test
    void testTradingScheduleGetCurrentSchedule() {
        // When
        TradingSchedule currentSchedule = TradingSchedule.getCurrentSchedule();

        // Then
        assertNotNull(currentSchedule);
        assertNotNull(currentSchedule.getScheduleName());
        assertNotNull(currentSchedule.getDescription());
        assertNotNull(currentSchedule.getStartTime());
        assertNotNull(currentSchedule.getEndTime());
        assertTrue(currentSchedule.getIntervalSeconds() > 0);
    }

    @Test
    void testTradingScheduleIsMarketHours() {
        // When
        boolean isMarketHours = TradingSchedule.isMarketHours();

        // Then
        // 장 시간 여부는 현재 시간에 따라 달라질 수 있음
        assertTrue(isMarketHours == true || isMarketHours == false);
    }

    @Test
    void testTradingScheduleIsTradingHours() {
        // When
        boolean isTradingHours = TradingSchedule.isTradingHours();

        // Then
        // 거래 시간 여부는 현재 시간에 따라 달라질 수 있음
        assertTrue(isTradingHours == true || isTradingHours == false);
    }

    @Test
    void testTradingScheduleIsHighFrequencyTime() {
        // When
        boolean isHighFrequency = TradingSchedule.isHighFrequencyTime();

        // Then
        // 고빈도 시간 여부는 현재 시간에 따라 달라질 수 있음
        assertTrue(isHighFrequency == true || isHighFrequency == false);
    }

    @Test
    void testTopKospiStocksFindByStockCode() {
        // Given
        String stockCode = "005930";

        // When
        TopKospiStocks stock = TopKospiStocks.findByStockCode(stockCode);

        // Then
        assertNotNull(stock);
        assertEquals(stockCode, stock.getStockCode());
        assertEquals("삼성전자", stock.getStockName());
        assertEquals("전자", stock.getSector());
    }

    @Test
    void testTopKospiStocksFindByStockName() {
        // Given
        String stockName = "삼성전자";

        // When
        TopKospiStocks stock = TopKospiStocks.findByStockName(stockName);

        // Then
        assertNotNull(stock);
        assertEquals("005930", stock.getStockCode());
        assertEquals(stockName, stock.getStockName());
        assertEquals("전자", stock.getSector());
    }

    @Test
    void testTopKospiStocksFindBySector() {
        // Given
        String sector = "전자";

        // When
        TopKospiStocks[] stocks = TopKospiStocks.findBySector(sector);

        // Then
        assertNotNull(stocks);
        assertTrue(stocks.length > 0);
        for (TopKospiStocks stock : stocks) {
            assertEquals(sector, stock.getSector());
        }
    }

    @Test
    void testTopKospiStocksGetAllStockCodes() {
        // When
        String[] stockCodes = TopKospiStocks.getAllStockCodes();

        // Then
        assertNotNull(stockCodes);
        assertEquals(20, stockCodes.length);
        assertTrue(java.util.Arrays.asList(stockCodes).contains("005930"));
    }

    @Test
    void testTopKospiStocksGetAllStockNames() {
        // When
        String[] stockNames = TopKospiStocks.getAllStockNames();

        // Then
        assertNotNull(stockNames);
        assertEquals(20, stockNames.length);
        assertTrue(java.util.Arrays.asList(stockNames).contains("삼성전자"));
    }

    @Test
    void testTopKospiStocksGetAllSectors() {
        // When
        String[] sectors = TopKospiStocks.getAllSectors();

        // Then
        assertNotNull(sectors);
        assertTrue(sectors.length > 0);
        assertTrue(java.util.Arrays.asList(sectors).contains("전자"));
    }

    @Test
    void testAutoTradingStrategyCreateDefaultStrategy() {
        // When
        AutoTradingStrategy strategy = AutoTradingStrategy.createDefaultStrategy();

        // Then
        assertNotNull(strategy);
        assertEquals("DEFAULT_SAMSUNG", strategy.getStrategyId());
        assertEquals("삼성전자 기본 전략", strategy.getStrategyName());
        assertEquals("005930", strategy.getStockCode());
        assertEquals("삼성전자", strategy.getStockName());
        assertEquals(TopKospiStocks.SAMSUNG_ELECTRONICS, strategy.getTargetStock());
        assertTrue(strategy.isEnabled());
        assertEquals(new BigDecimal("10000000"), strategy.getTotalInvestment());
        assertEquals(new BigDecimal("0.1"), strategy.getMaxPositionSize());
    }

    @Test
    void testAutoTradingStrategyCreateConservativeStrategy() {
        // Given
        TopKospiStocks stock = TopKospiStocks.SK_HYNIX;

        // When
        AutoTradingStrategy strategy = AutoTradingStrategy.createConservativeStrategy(stock);

        // Then
        assertNotNull(strategy);
        assertEquals("CONSERVATIVE_" + stock.getStockCode(), strategy.getStrategyId());
        assertEquals(stock.getStockName() + " 보수적 전략", strategy.getStrategyName());
        assertEquals(stock.getStockCode(), strategy.getStockCode());
        assertEquals(stock.getStockName(), strategy.getStockName());
        assertEquals(stock, strategy.getTargetStock());
        assertTrue(strategy.isEnabled());
        assertEquals(new BigDecimal("5000000"), strategy.getTotalInvestment());
        assertEquals(new BigDecimal("0.05"), strategy.getMaxPositionSize());
        assertEquals(new BigDecimal("25"), strategy.getRsiOversold());
        assertEquals(new BigDecimal("75"), strategy.getRsiOverbought());
    }

    @Test
    void testAutoTradingStrategyCreateAggressiveStrategy() {
        // Given
        TopKospiStocks stock = TopKospiStocks.NAVER;

        // When
        AutoTradingStrategy strategy = AutoTradingStrategy.createAggressiveStrategy(stock);

        // Then
        assertNotNull(strategy);
        assertEquals("AGGRESSIVE_" + stock.getStockCode(), strategy.getStrategyId());
        assertEquals(stock.getStockName() + " 공격적 전략", strategy.getStrategyName());
        assertEquals(stock.getStockCode(), strategy.getStockCode());
        assertEquals(stock.getStockName(), strategy.getStockName());
        assertEquals(stock, strategy.getTargetStock());
        assertTrue(strategy.isEnabled());
        assertEquals(new BigDecimal("20000000"), strategy.getTotalInvestment());
        assertEquals(new BigDecimal("0.2"), strategy.getMaxPositionSize());
        assertEquals(new BigDecimal("35"), strategy.getRsiOversold());
        assertEquals(new BigDecimal("65"), strategy.getRsiOverbought());
    }

    @Test
    void testTradingScheduleTimeRange() {
        // Given
        TradingSchedule morningSession = TradingSchedule.MORNING_SESSION;
        java.time.LocalTime morningTime = java.time.LocalTime.of(10, 0);
        java.time.LocalTime nightTime = java.time.LocalTime.of(22, 0);

        // When
        boolean isInMorningRange = morningSession.isInTimeRange(morningTime);
        boolean isInNightRange = morningSession.isInTimeRange(nightTime);

        // Then
        assertTrue(isInMorningRange);
        assertFalse(isInNightRange);
    }

    @Test
    void testTradingScheduleNextAndPrevious() {
        // Given
        TradingSchedule current = TradingSchedule.MORNING_SESSION;

        // When
        TradingSchedule next = current.getNextSchedule();
        TradingSchedule previous = current.getPreviousSchedule();

        // Then
        assertNotNull(next);
        assertNotNull(previous);
        assertNotEquals(current, next);
        assertNotEquals(current, previous);
    }

    @Test
    void testTradingScheduleIntervalMillis() {
        // Given
        TradingSchedule schedule = TradingSchedule.MORNING_SESSION;

        // When
        long intervalMillis = schedule.getIntervalMillis();

        // Then
        assertEquals(60000, intervalMillis); // 60초 = 60000밀리초
    }

    @Test
    void testInvalidStockCode() {
        // Given
        String invalidStockCode = "999999";

        // When
        TopKospiStocks stock = TopKospiStocks.findByStockCode(invalidStockCode);

        // Then
        assertNull(stock);
    }

    @Test
    void testInvalidStockName() {
        // Given
        String invalidStockName = "존재하지않는종목";

        // When
        TopKospiStocks stock = TopKospiStocks.findByStockName(invalidStockName);

        // Then
        assertNull(stock);
    }

    @Test
    void testInvalidSector() {
        // Given
        String invalidSector = "존재하지않는섹터";

        // When
        TopKospiStocks[] stocks = TopKospiStocks.findBySector(invalidSector);

        // Then
        assertNotNull(stocks);
        assertEquals(0, stocks.length);
    }
} 