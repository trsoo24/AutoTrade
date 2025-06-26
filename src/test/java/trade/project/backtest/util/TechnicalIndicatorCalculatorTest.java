package trade.project.backtest.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import trade.project.backtest.dto.StockData;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("기술적 지표 계산기 테스트")
class TechnicalIndicatorCalculatorTest {

    private List<StockData> testData;

    @BeforeEach
    void setUp() {
        testData = new ArrayList<>();
        
        // 테스트용 주식 데이터 생성 (10일간 상승하는 데이터)
        for (int i = 0; i < 10; i++) {
            BigDecimal price = new BigDecimal("10000").add(BigDecimal.valueOf(i * 100));
            testData.add(StockData.builder()
                    .date(LocalDate.now().minusDays(9 - i))
                    .open(price)
                    .high(price.add(new BigDecimal("50")))
                    .low(price.subtract(new BigDecimal("50")))
                    .close(price)
                    .volume(1000000L)
                    .adjustedClose(price)
                    .build());
        }
    }

    @Test
    @DisplayName("SMA 계산 - 정상적인 경우")
    void calculateSMA_WithValidData_ShouldReturnCorrectValue() {
        // Given
        int period = 5;
        
        // When
        BigDecimal sma = TechnicalIndicatorCalculator.calculateSMA(testData, period);
        
        // Then
        assertNotNull(sma);
        // 5일간의 평균: (10000 + 10100 + 10200 + 10300 + 10400) / 5 = 10200
        // 실제 계산 결과: 10700 (마지막 5개 데이터의 평균)
        assertEquals(new BigDecimal("10700.00"), sma);
    }

    @Test
    @DisplayName("SMA 계산 - 데이터가 부족한 경우")
    void calculateSMA_WithInsufficientData_ShouldReturnNull() {
        // Given
        int period = 15;
        
        // When
        BigDecimal sma = TechnicalIndicatorCalculator.calculateSMA(testData, period);
        
        // Then
        assertNull(sma);
    }

    @Test
    @DisplayName("RSI 계산 - 정상적인 경우")
    void calculateRSI_WithValidData_ShouldReturnCorrectValue() {
        // Given
        int period = 5;
        
        // When
        BigDecimal rsi = TechnicalIndicatorCalculator.calculateRSI(testData, period);
        
        // Then
        assertNotNull(rsi);
        // 상승하는 데이터이므로 RSI는 높은 값을 가져야 함
        assertTrue(rsi.compareTo(new BigDecimal("50")) > 0);
    }

    @Test
    @DisplayName("RSI 계산 - 데이터가 부족한 경우")
    void calculateRSI_WithInsufficientData_ShouldReturnNull() {
        // Given
        int period = 15;
        
        // When
        BigDecimal rsi = TechnicalIndicatorCalculator.calculateRSI(testData, period);
        
        // Then
        assertNull(rsi);
    }

    @Test
    @DisplayName("MACD 계산 - 정상적인 경우")
    void calculateMACD_WithValidData_ShouldReturnCorrectValue() {
        // Given
        int fastPeriod = 3;
        int slowPeriod = 5;
        int signalPeriod = 2;
        
        // When
        TechnicalIndicatorCalculator.MACDResult result = TechnicalIndicatorCalculator.calculateMACD(
                testData, fastPeriod, slowPeriod, signalPeriod);
        
        // Then
        assertNotNull(result);
        assertNotNull(result.getEma12());
        assertNotNull(result.getEma26());
        assertNotNull(result.getMacd());
        // MACD = EMA12 - EMA26
        assertEquals(result.getMacd(), result.getEma12().subtract(result.getEma26()));
    }

    @Test
    @DisplayName("MACD 계산 - 데이터가 부족한 경우")
    void calculateMACD_WithInsufficientData_ShouldReturnNull() {
        // Given
        int fastPeriod = 3;
        int slowPeriod = 15;
        int signalPeriod = 2;
        
        // When
        TechnicalIndicatorCalculator.MACDResult result = TechnicalIndicatorCalculator.calculateMACD(
                testData, fastPeriod, slowPeriod, signalPeriod);
        
        // Then
        assertNull(result);
    }

    @Test
    @DisplayName("볼린저 밴드 계산 - 정상적인 경우")
    void calculateBollingerBands_WithValidData_ShouldReturnCorrectValue() {
        // Given
        int period = 5;
        double stdDev = 2.0;
        
        // When
        TechnicalIndicatorCalculator.BollingerBandsResult result = 
                TechnicalIndicatorCalculator.calculateBollingerBands(testData, period, stdDev);
        
        // Then
        assertNotNull(result);
        assertNotNull(result.getUpper());
        assertNotNull(result.getMiddle());
        assertNotNull(result.getLower());
        
        // 상단선 > 중간선 > 하단선
        assertTrue(result.getUpper().compareTo(result.getMiddle()) > 0);
        assertTrue(result.getMiddle().compareTo(result.getLower()) > 0);
    }

    @Test
    @DisplayName("볼린저 밴드 계산 - 데이터가 부족한 경우")
    void calculateBollingerBands_WithInsufficientData_ShouldReturnNull() {
        // Given
        int period = 15;
        double stdDev = 2.0;
        
        // When
        TechnicalIndicatorCalculator.BollingerBandsResult result = 
                TechnicalIndicatorCalculator.calculateBollingerBands(testData, period, stdDev);
        
        // Then
        assertNull(result);
    }

    @Test
    @DisplayName("일일 수익률 계산 - 정상적인 경우")
    void calculateDailyReturn_WithValidData_ShouldReturnCorrectValue() {
        // Given
        StockData currentData = testData.get(1); // 10100
        StockData previousData = testData.get(0); // 10000
        
        // When
        BigDecimal dailyReturn = TechnicalIndicatorCalculator.calculateDailyReturn(currentData, previousData);
        
        // Then
        assertNotNull(dailyReturn);
        // 수익률: (10100 - 10000) / 10000 * 100 = 1.0%
        assertEquals(new BigDecimal("1.0000"), dailyReturn);
    }

    @Test
    @DisplayName("일일 수익률 계산 - 이전 데이터가 없는 경우")
    void calculateDailyReturn_WithNoPreviousData_ShouldReturnZero() {
        // Given
        StockData currentData = testData.get(0);
        StockData previousData = null;
        
        // When
        BigDecimal dailyReturn = TechnicalIndicatorCalculator.calculateDailyReturn(currentData, previousData);
        
        // Then
        assertEquals(BigDecimal.ZERO, dailyReturn);
    }

    @Test
    @DisplayName("EMA 계산 - 정상적인 경우")
    void calculateEMA_WithValidData_ShouldReturnCorrectValue() {
        // Given
        List<BigDecimal> prices = new ArrayList<>();
        for (StockData data : testData) {
            prices.add(data.getClose());
        }
        int period = 5;
        
        // When
        BigDecimal ema = TechnicalIndicatorCalculator.calculateEMA(prices, period);
        
        // Then
        assertNotNull(ema);
        // EMA는 최신 데이터에 더 높은 가중치를 줌
        assertTrue(ema.compareTo(prices.get(prices.size() - 1)) < 0);
    }

    @Test
    @DisplayName("EMA 계산 - 데이터가 부족한 경우")
    void calculateEMA_WithInsufficientData_ShouldReturnNull() {
        // Given
        List<BigDecimal> prices = new ArrayList<>();
        prices.add(new BigDecimal("10000"));
        int period = 5;
        
        // When
        BigDecimal ema = TechnicalIndicatorCalculator.calculateEMA(prices, period);
        
        // Then
        assertNull(ema);
    }
} 