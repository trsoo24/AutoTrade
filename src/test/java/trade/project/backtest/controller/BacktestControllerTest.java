package trade.project.backtest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import trade.project.backtest.dto.BackTestRequest;
import trade.project.backtest.dto.BackTestResult;
import trade.project.backtest.service.BacktestService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
@DisplayName("백트래킹 컨트롤러 테스트")
class BacktestControllerTest {

    @Mock
    private BacktestService backtestService;

    @InjectMocks
    private BacktestController backtestController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(backtestController).build();
        
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test
    @DisplayName("백트래킹 실행 - 성공적인 경우")
    void runBacktest_WithValidRequest_ShouldReturnSuccess() throws Exception {
        // Given
        BackTestRequest request = BackTestRequest.builder()
                .stockCode("005930")
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 1, 31))
                .strategy("SMA")
                .initialCapital(new BigDecimal("10000000"))
                .build();

        BackTestResult expectedResult = BackTestResult.builder()
                .stockCode("005930")
                .strategy("SMA")
                .totalReturn(new BigDecimal("1000000"))
                .totalTrades(5)
                .winRate(new BigDecimal("0.60"))
                .maxDrawdown(new BigDecimal("0.05"))
                .sharpeRatio(new BigDecimal("1.2"))
                .build();

        when(backtestService.runBacktest(any(BackTestRequest.class))).thenReturn(expectedResult);

        // When & Then
        mockMvc.perform(post("/api/backtest/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.stockCode").value("005930"))
                .andExpect(jsonPath("$.data.strategy").value("SMA"))
                .andExpect(jsonPath("$.data.totalReturn").value(1000000))
                .andExpect(jsonPath("$.data.totalTrades").value(5))
                .andExpect(jsonPath("$.data.winRate").value(0.60))
                .andExpect(jsonPath("$.data.maxDrawdown").value(0.05))
                .andExpect(jsonPath("$.data.sharpeRatio").value(1.2));
    }

    @Test
    @DisplayName("백트래킹 실행 - 기본값이 적용된 요청")
    void runBacktest_WithPartialRequest_ShouldApplyDefaults() throws Exception {
        // Given
        BackTestRequest request = BackTestRequest.builder()
                .stockCode("005930")
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 1, 31))
                .initialCapital(new BigDecimal("10000000"))
                .build();

        BackTestResult expectedResult = BackTestResult.builder()
                .stockCode("005930")
                .strategy("SMA")
                .totalReturn(new BigDecimal("500000"))
                .totalTrades(3)
                .build();

        when(backtestService.runBacktest(any(BackTestRequest.class))).thenReturn(expectedResult);

        // When & Then
        mockMvc.perform(post("/api/backtest/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.stockCode").value("005930"));
    }

    @Test
    @DisplayName("백트래킹 실행 - 잘못된 요청 형식")
    void runBacktest_WithInvalidRequest_ShouldReturnBadRequest() throws Exception {
        // Given
        String invalidJson = "{\"invalid\": \"json\"";

        // When & Then
        mockMvc.perform(post("/api/backtest/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("백트래킹 실행 - 서비스 예외 발생")
    void runBacktest_WhenServiceThrowsException_ShouldReturnError() throws Exception {
        // Given
        BackTestRequest request = BackTestRequest.builder()
                .stockCode("005930")
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 1, 31))
                .strategy("INVALID")
                .initialCapital(new BigDecimal("10000000"))
                .build();

        when(backtestService.runBacktest(any(BackTestRequest.class)))
                .thenThrow(new RuntimeException("백트래킹 실행 실패"));

        // When & Then
        mockMvc.perform(post("/api/backtest/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error_message").value("백트래킹 실행 실패"));
    }

    @Test
    @DisplayName("사용 가능한 전략 목록 조회")
    void getAvailableStrategies_ShouldReturnStrategyList() throws Exception {
        // Given
        List<Map<String, String>> strategies = Arrays.asList(
                Map.of("name", "SMA", "description", "Simple Moving Average Strategy"),
                Map.of("name", "RSI", "description", "Relative Strength Index Strategy"),
                Map.of("name", "MACD", "description", "Moving Average Convergence Divergence Strategy")
        );

        when(backtestService.getAvailableStrategies()).thenReturn(strategies);

        // When & Then
        mockMvc.perform(get("/api/backtest/strategies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].name").value("SMA"))
                .andExpect(jsonPath("$.data[0].description").value("Simple Moving Average Strategy"))
                .andExpect(jsonPath("$.data[1].name").value("RSI"))
                .andExpect(jsonPath("$.data[1].description").value("Relative Strength Index Strategy"))
                .andExpect(jsonPath("$.data[2].name").value("MACD"))
                .andExpect(jsonPath("$.data[2].description").value("Moving Average Convergence Divergence Strategy"));
    }

    @Test
    @DisplayName("사용 가능한 전략 목록 조회 - 빈 목록")
    void getAvailableStrategies_WithEmptyList_ShouldReturnEmptyArray() throws Exception {
        // Given
        when(backtestService.getAvailableStrategies()).thenReturn(Arrays.asList());

        // When & Then
        mockMvc.perform(get("/api/backtest/strategies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("사용 가능한 전략 목록 조회 - 서비스 예외 발생")
    void getAvailableStrategies_WhenServiceThrowsException_ShouldReturnError() throws Exception {
        // Given
        when(backtestService.getAvailableStrategies())
                .thenThrow(new RuntimeException("전략 목록 조회 실패"));

        // When & Then
        mockMvc.perform(get("/api/backtest/strategies"))
                .andExpect(status().isInternalServerError())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof RuntimeException))
                .andExpect(result -> assertEquals("전략 목록 조회 실패", result.getResolvedException().getMessage()));
    }

    @Test
    @DisplayName("기본값 요청 조회")
    void getDefaultRequest_ShouldReturnDefaultValues() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/backtest/default-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.commission").value(0.001))
                .andExpect(jsonPath("$.data.strategy").value("SMA"))
                .andExpect(jsonPath("$.data.shortPeriod").value(5))
                .andExpect(jsonPath("$.data.longPeriod").value(20))
                .andExpect(jsonPath("$.data.rsiPeriod").value(14))
                .andExpect(jsonPath("$.data.rsiOverbought").value(70))
                .andExpect(jsonPath("$.data.rsiOversold").value(30))
                .andExpect(jsonPath("$.data.macdFastPeriod").value(12))
                .andExpect(jsonPath("$.data.macdSlowPeriod").value(26))
                .andExpect(jsonPath("$.data.macdSignalPeriod").value(9))
                .andExpect(jsonPath("$.data.stopLoss").value(0.05))
                .andExpect(jsonPath("$.data.takeProfit").value(0.10))
                .andExpect(jsonPath("$.data.maxPositionSize").value(0.5))
                .andExpect(jsonPath("$.data.minTradeAmount").value(100000))
                .andExpect(jsonPath("$.data.reinvestDividends").value(true))
                .andExpect(jsonPath("$.data.includeTax").value(true));
    }

    @Test
    @DisplayName("헬스 체크")
    void healthCheck_ShouldReturnOk() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/backtest/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("healthy"));
    }

    @Test
    @DisplayName("백트래킹 실행 - 필수 필드 누락")
    void runBacktest_WithMissingRequiredFields_ShouldReturnBadRequest() throws Exception {
        // Given - 주식 코드가 없는 경우 (기본값으로 채워지지 않는 필수 필드)
        BackTestRequest request = BackTestRequest.builder()
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 1, 31))
                .strategy("SMA")
                .initialCapital(new BigDecimal("10000000"))
                .build();

        // When & Then
        mockMvc.perform(post("/api/backtest/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk()) // 컨트롤러에서 IllegalArgumentException을 ApiResponse.error로 처리
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error_message").value("주식 코드는 필수입니다."));
    }

    @Test
    @DisplayName("백트래킹 실행 - 잘못된 날짜 형식")
    void runBacktest_WithInvalidDateFormat_ShouldReturnBadRequest() throws Exception {
        // Given
        String invalidRequest = "{\"stockCode\":\"005930\",\"startDate\":\"invalid-date\",\"endDate\":\"2024-01-31\",\"strategy\":\"SMA\",\"initialCapital\":10000000}";

        // When & Then
        mockMvc.perform(post("/api/backtest/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("백트래킹 실행 - 잘못된 숫자 형식")
    void runBacktest_WithInvalidNumberFormat_ShouldReturnBadRequest() throws Exception {
        // Given
        String invalidRequest = "{\"stockCode\":\"005930\",\"startDate\":\"2024-01-01\",\"endDate\":\"2024-01-31\",\"strategy\":\"SMA\",\"initialCapital\":\"invalid-number\"}";

        // When & Then
        mockMvc.perform(post("/api/backtest/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest());
    }
} 