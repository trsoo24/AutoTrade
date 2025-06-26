package trade.project.backtest.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("백트래킹 요청 DTO 테스트")
class BackTestRequestTest {

    @Test
    @DisplayName("기본값 설정 - 모든 필드가 올바르게 설정되어야 함")
    void getDefault_ShouldReturnCorrectDefaultValues() {
        // When
        BackTestRequest defaultRequest = BackTestRequest.getDefault();
        
        // Then
        assertNotNull(defaultRequest);
        assertEquals(new BigDecimal("0.001"), defaultRequest.getCommission());
        assertEquals("SMA", defaultRequest.getStrategy());
        assertEquals(5, defaultRequest.getShortPeriod());
        assertEquals(20, defaultRequest.getLongPeriod());
        assertEquals(14, defaultRequest.getRsiPeriod());
        assertEquals(70, defaultRequest.getRsiOverbought());
        assertEquals(30, defaultRequest.getRsiOversold());
        assertEquals(12, defaultRequest.getMacdFastPeriod());
        assertEquals(26, defaultRequest.getMacdSlowPeriod());
        assertEquals(9, defaultRequest.getMacdSignalPeriod());
        assertEquals(new BigDecimal("0.05"), defaultRequest.getStopLoss());
        assertEquals(new BigDecimal("0.10"), defaultRequest.getTakeProfit());
        assertEquals(new BigDecimal("0.5"), defaultRequest.getMaxPositionSize());
        assertEquals(new BigDecimal("100000"), defaultRequest.getMinTradeAmount());
        assertTrue(defaultRequest.getReinvestDividends());
        assertTrue(defaultRequest.getIncludeTax());
    }

    @Test
    @DisplayName("기본값 적용 - null 필드만 기본값으로 채워져야 함")
    void applyDefaults_WithPartialData_ShouldFillOnlyNullFields() {
        // Given
        BackTestRequest request = BackTestRequest.builder()
                .stockCode("005930")
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 1, 31))
                .initialCapital(new BigDecimal("50000000"))
                .strategy("RSI")
                .commission(new BigDecimal("0.002"))
                .build();
        
        // When
        BackTestRequest result = request.applyDefaults();
        
        // Then
        assertEquals("005930", result.getStockCode());
        assertEquals(LocalDate.of(2024, 1, 1), result.getStartDate());
        assertEquals(LocalDate.of(2024, 1, 31), result.getEndDate());
        assertEquals(new BigDecimal("50000000"), result.getInitialCapital());
        assertEquals("RSI", result.getStrategy()); // 사용자 설정값 유지
        assertEquals(new BigDecimal("0.002"), result.getCommission()); // 사용자 설정값 유지
        
        // 기본값이 적용된 필드들
        assertEquals(5, result.getShortPeriod());
        assertEquals(20, result.getLongPeriod());
        assertEquals(14, result.getRsiPeriod());
        assertEquals(70, result.getRsiOverbought());
        assertEquals(30, result.getRsiOversold());
        assertEquals(12, result.getMacdFastPeriod());
        assertEquals(26, result.getMacdSlowPeriod());
        assertEquals(9, result.getMacdSignalPeriod());
        assertEquals(new BigDecimal("0.05"), result.getStopLoss());
        assertEquals(new BigDecimal("0.10"), result.getTakeProfit());
        assertEquals(new BigDecimal("0.5"), result.getMaxPositionSize());
        assertEquals(new BigDecimal("100000"), result.getMinTradeAmount());
        assertTrue(result.getReinvestDividends());
        assertTrue(result.getIncludeTax());
    }

    @Test
    @DisplayName("기본값 적용 - 모든 필드가 설정된 경우 원본값 유지")
    void applyDefaults_WithAllFieldsSet_ShouldKeepOriginalValues() {
        // Given
        BackTestRequest request = BackTestRequest.builder()
                .stockCode("005930")
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 1, 31))
                .initialCapital(new BigDecimal("50000000"))
                .strategy("MACD")
                .commission(new BigDecimal("0.002"))
                .shortPeriod(10)
                .longPeriod(30)
                .rsiPeriod(20)
                .rsiOverbought(80)
                .rsiOversold(20)
                .macdFastPeriod(15)
                .macdSlowPeriod(30)
                .macdSignalPeriod(12)
                .stopLoss(new BigDecimal("0.10"))
                .takeProfit(new BigDecimal("0.20"))
                .maxPositionSize(new BigDecimal("0.8"))
                .minTradeAmount(new BigDecimal("200000"))
                .reinvestDividends(false)
                .includeTax(false)
                .build();
        
        // When
        BackTestRequest result = request.applyDefaults();
        
        // Then
        assertEquals("005930", result.getStockCode());
        assertEquals(LocalDate.of(2024, 1, 1), result.getStartDate());
        assertEquals(LocalDate.of(2024, 1, 31), result.getEndDate());
        assertEquals(new BigDecimal("50000000"), result.getInitialCapital());
        assertEquals("MACD", result.getStrategy());
        assertEquals(new BigDecimal("0.002"), result.getCommission());
        assertEquals(10, result.getShortPeriod());
        assertEquals(30, result.getLongPeriod());
        assertEquals(20, result.getRsiPeriod());
        assertEquals(80, result.getRsiOverbought());
        assertEquals(20, result.getRsiOversold());
        assertEquals(15, result.getMacdFastPeriod());
        assertEquals(30, result.getMacdSlowPeriod());
        assertEquals(12, result.getMacdSignalPeriod());
        assertEquals(new BigDecimal("0.10"), result.getStopLoss());
        assertEquals(new BigDecimal("0.20"), result.getTakeProfit());
        assertEquals(new BigDecimal("0.8"), result.getMaxPositionSize());
        assertEquals(new BigDecimal("200000"), result.getMinTradeAmount());
        assertFalse(result.getReinvestDividends());
        assertFalse(result.getIncludeTax());
    }

    @Test
    @DisplayName("빌더 패턴 - 모든 필드 설정")
    void builder_WithAllFields_ShouldCreateCorrectObject() {
        // Given & When
        BackTestRequest request = BackTestRequest.builder()
                .stockCode("005930")
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 1, 31))
                .initialCapital(new BigDecimal("10000000"))
                .strategy("SMA")
                .commission(new BigDecimal("0.001"))
                .shortPeriod(5)
                .longPeriod(20)
                .build();
        
        // Then
        assertNotNull(request);
        assertEquals("005930", request.getStockCode());
        assertEquals(LocalDate.of(2024, 1, 1), request.getStartDate());
        assertEquals(LocalDate.of(2024, 1, 31), request.getEndDate());
        assertEquals(new BigDecimal("10000000"), request.getInitialCapital());
        assertEquals("SMA", request.getStrategy());
        assertEquals(new BigDecimal("0.001"), request.getCommission());
        assertEquals(5, request.getShortPeriod());
        assertEquals(20, request.getLongPeriod());
    }

    @Test
    @DisplayName("기본 생성자 - null 값으로 초기화")
    void noArgsConstructor_ShouldInitializeWithNullValues() {
        // When
        BackTestRequest request = new BackTestRequest();
        
        // Then
        assertNull(request.getStockCode());
        assertNull(request.getStartDate());
        assertNull(request.getEndDate());
        assertNull(request.getInitialCapital());
        assertNull(request.getStrategy());
        assertNull(request.getCommission());
        assertNull(request.getShortPeriod());
        assertNull(request.getLongPeriod());
        assertNull(request.getRsiPeriod());
        assertNull(request.getRsiOverbought());
        assertNull(request.getRsiOversold());
        assertNull(request.getMacdFastPeriod());
        assertNull(request.getMacdSlowPeriod());
        assertNull(request.getMacdSignalPeriod());
        assertNull(request.getStopLoss());
        assertNull(request.getTakeProfit());
        assertNull(request.getMaxPositionSize());
        assertNull(request.getMinTradeAmount());
        assertNull(request.getReinvestDividends());
        assertNull(request.getIncludeTax());
    }

    @Test
    @DisplayName("Setter 메서드 - 값 변경 확인")
    void setterMethods_ShouldChangeValues() {
        // Given
        BackTestRequest request = new BackTestRequest();
        
        // When
        request.setStockCode("005930");
        request.setStrategy("RSI");
        request.setInitialCapital(new BigDecimal("50000000"));
        
        // Then
        assertEquals("005930", request.getStockCode());
        assertEquals("RSI", request.getStrategy());
        assertEquals(new BigDecimal("50000000"), request.getInitialCapital());
    }
} 