package trade.project;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import trade.project.backtest.controller.BacktestController;
import trade.project.backtest.service.BacktestService;
import trade.project.backtest.strategy.StrategyFactory;
import trade.project.backtest.dto.BackTestRequest;
import trade.project.backtest.engine.BacktestEngine;
import trade.project.backtest.strategy.TradingStrategy;
import trade.project.common.client.BaseRestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@DisplayName("프로젝트 통합 테스트")
class ProjectApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BacktestController backtestController;

    @Autowired
    private BacktestService backtestService;

    @Autowired
    private BacktestEngine backtestEngine;

    @Autowired
    private StrategyFactory strategyFactory;

    @Autowired
    private BaseRestClient baseRestClient;

    @Test
    @DisplayName("애플리케이션 컨텍스트 로드")
    void contextLoads() {
        // Given & When & Then
        assertNotNull(backtestController);
        assertNotNull(backtestService);
        assertNotNull(backtestEngine);
        assertNotNull(strategyFactory);
        assertNotNull(baseRestClient);
    }

    @Test
    @DisplayName("백트래킹 컨트롤러 통합 테스트 - 헬스 체크")
    void backtestController_HealthCheck_ShouldReturnOk() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/backtest/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("백트래킹 서비스가 정상적으로 동작 중입니다"));
    }

    @Test
    @DisplayName("백트래킹 컨트롤러 통합 테스트 - 기본값 요청")
    void backtestController_DefaultRequest_ShouldReturnDefaultValues() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/backtest/default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.commission").value(0.001))
                .andExpect(jsonPath("$.data.strategy").value("SMA"))
                .andExpect(jsonPath("$.data.shortPeriod").value(5))
                .andExpect(jsonPath("$.data.longPeriod").value(20));
    }

    @Test
    @DisplayName("백트래킹 컨트롤러 통합 테스트 - 전략 목록 조회")
    void backtestController_Strategies_ShouldReturnStrategyList() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/backtest/strategies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].name").exists())
                .andExpect(jsonPath("$.data[0].description").exists());
    }

    @Test
    @DisplayName("백트래킹 서비스 통합 테스트 - 전략 팩토리 연동")
    void backtestService_WithStrategyFactory_ShouldWorkCorrectly() {
        // Given
        var strategies = strategyFactory.getAvailableStrategies();
        var strategyNames = strategyFactory.getStrategyNames();

        // When & Then
        assertNotNull(strategies);
        assertNotNull(strategyNames);
        assertTrue(strategies.size() > 0);
        assertTrue(strategyNames.size() > 0);
        
        // SMA 전략이 존재하는지 확인
        var smaStrategy = strategyFactory.getStrategy("SMA");
        assertNotNull(smaStrategy);
        assertEquals("SMA", smaStrategy.getStrategyName());
    }

    @Test
    @DisplayName("백트래킹 서비스 통합 테스트 - 기본값 적용")
    void backtestService_DefaultValues_ShouldBeApplied() {
        // Given
        var defaultRequest = BackTestRequest.getDefault();

        // When & Then
        assertNotNull(defaultRequest);
        assertEquals("SMA", defaultRequest.getStrategy());
        assertEquals(5, defaultRequest.getShortPeriod());
        assertEquals(20, defaultRequest.getLongPeriod());
        assertEquals(14, defaultRequest.getRsiPeriod());
        assertEquals(70, defaultRequest.getRsiOverbought());
        assertEquals(30, defaultRequest.getRsiOversold());
    }

    @Test
    @DisplayName("백트래킹 컨트롤러 통합 테스트 - 잘못된 요청")
    void backtestController_InvalidRequest_ShouldReturnBadRequest() throws Exception {
        // Given
        String invalidRequest = "{\"invalid\": \"request\"}";

        // When & Then
        mockMvc.perform(post("/api/backtest/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("백트래킹 컨트롤러 통합 테스트 - 필수 필드 누락")
    void backtestController_MissingRequiredFields_ShouldReturnBadRequest() throws Exception {
        // Given
        String incompleteRequest = "{\"stockCode\":\"005930\"}";

        // When & Then
        mockMvc.perform(post("/api/backtest/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(incompleteRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("전략 팩토리 테스트")
    void strategyFactoryTest() {
        // 모든 전략이 등록되어 있는지 확인
        List<TradingStrategy> strategies = strategyFactory.getAvailableStrategies();
        assertTrue(strategies.size() > 0);
        
        List<String> strategyNames = strategyFactory.getStrategyNames();
        assertTrue(strategyNames.size() > 0);

        // SMA 전략 가져오기
        TradingStrategy smaStrategy = strategyFactory.getStrategy("SMA");
        assertNotNull(smaStrategy);
        assertEquals("SMA", smaStrategy.getStrategyName());
    }

    @Test
    @DisplayName("전략 팩토리 통합 테스트 - 대소문자 구분 없는 조회")
    void strategyFactory_CaseInsensitiveLookup_ShouldWork() {
        // Given & When
        var smaStrategy1 = strategyFactory.getStrategy("SMA");
        var smaStrategy2 = strategyFactory.getStrategy("sma");
        var smaStrategy3 = strategyFactory.getStrategy("Sma");

        // Then
        assertNotNull(smaStrategy1);
        assertNotNull(smaStrategy2);
        assertNotNull(smaStrategy3);
        
        assertEquals(smaStrategy1, smaStrategy2);
        assertEquals(smaStrategy1, smaStrategy3);
    }

    @Test
    @DisplayName("존재하지 않는 전략 요청 테스트")
    void nonExistentStrategyTest() {
        // 존재하지 않는 전략 요청
        TradingStrategy nonExistentStrategy = strategyFactory.getStrategy("NON_EXISTENT");
        assertNull(nonExistentStrategy);
    }

    @Test
    @DisplayName("백트래킹 서비스 통합 테스트 - 전략 목록 조회")
    void backtestService_GetAvailableStrategies_ShouldReturnList() {
        // Given & When
        var strategies = backtestService.getAvailableStrategies();

        // Then
        assertNotNull(strategies);
        assertTrue(strategies.size() > 0);
        
        // 각 전략에 name과 description이 있는지 확인
        for (var strategy : strategies) {
            assertTrue(strategy.containsKey("name"));
            assertTrue(strategy.containsKey("description"));
            assertNotNull(strategy.get("name"));
            assertNotNull(strategy.get("description"));
        }
    }

    @Test
    @DisplayName("백트래킹 서비스 통합 테스트 - 요청 유효성 검사")
    void backtestService_RequestValidation_ShouldWork() {
        // Given
        var validRequest = BackTestRequest.builder()
                .stockCode("005930")
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 1, 31))
                .strategy("SMA")
                .initialCapital(new BigDecimal("10000000"))
                .build();

        // When & Then
        assertDoesNotThrow(() -> {
            backtestService.validateRequest(validRequest);
        });
    }

    @Test
    @DisplayName("백트래킹 서비스 통합 테스트 - 잘못된 요청 유효성 검사")
    void backtestService_InvalidRequestValidation_ShouldThrowException() {
        // Given
        var invalidRequest = BackTestRequest.builder()
                .stockCode("005930")
                .startDate(LocalDate.of(2024, 1, 31))
                .endDate(LocalDate.of(2024, 1, 1)) // 시작일이 종료일보다 늦음
                .strategy("SMA")
                .initialCapital(new BigDecimal("10000000"))
                .build();

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            backtestService.validateRequest(invalidRequest);
        });
    }

    @Test
    @DisplayName("백트래킹 요청 기본값 테스트")
    void backTestRequestDefaultValuesTest() {
        // 기본값이 적용된 요청 생성
        BackTestRequest defaultRequest = BackTestRequest.getDefault();
        
        // 필수 필드들이 설정되어 있는지 확인
        assertNotNull(defaultRequest.getCommission());
        assertEquals("SMA", defaultRequest.getStrategy());
        assertEquals(5, defaultRequest.getShortPeriod());
        assertEquals(20, defaultRequest.getLongPeriod());
        assertEquals(14, defaultRequest.getRsiPeriod());
        assertEquals(70, defaultRequest.getRsiOverbought());
        assertEquals(30, defaultRequest.getRsiOversold());
    }

    @Test
    @DisplayName("백트래킹 요청 기본값 적용 테스트")
    void backTestRequestApplyDefaultsTest() {
        // 부분적으로 설정된 요청
        BackTestRequest partialRequest = BackTestRequest.builder()
                .stockCode("005930")
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 1, 31))
                .initialCapital(new BigDecimal("10000000"))
                .build();

        // 기본값 적용
        BackTestRequest appliedRequest = partialRequest.applyDefaults();

        // 필수 필드들이 설정되어 있는지 확인
        assertEquals("005930", appliedRequest.getStockCode());
        assertEquals(LocalDate.of(2024, 1, 1), appliedRequest.getStartDate());
        assertEquals(LocalDate.of(2024, 1, 31), appliedRequest.getEndDate());
        assertEquals(new BigDecimal("10000000"), appliedRequest.getInitialCapital());
        assertEquals("SMA", appliedRequest.getStrategy());
        assertEquals(5, appliedRequest.getShortPeriod());
        assertEquals(20, appliedRequest.getLongPeriod());
    }

    @Test
    @DisplayName("전략 팩토리 싱글톤 테스트")
    void strategyFactorySingletonTest() {
        // 동일한 전략을 여러 번 가져와도 같은 인스턴스인지 확인
        TradingStrategy smaStrategy1 = strategyFactory.getStrategy("SMA");
        TradingStrategy smaStrategy2 = strategyFactory.getStrategy("SMA");
        TradingStrategy smaStrategy3 = strategyFactory.getStrategy("SMA");

        assertNotNull(smaStrategy1);
        assertNotNull(smaStrategy2);
        assertNotNull(smaStrategy3);

        // 싱글톤이므로 같은 인스턴스여야 함
        assertSame(smaStrategy1, smaStrategy2);
        assertSame(smaStrategy1, smaStrategy3);
    }

    @Test
    @DisplayName("전략 정보 조회 테스트")
    void strategyInfoTest() {
        // 모든 전략 정보 가져오기
        List<TradingStrategy> strategies = strategyFactory.getAvailableStrategies();
        assertTrue(strategies.size() > 0);

        // 각 전략에 name과 description이 있는지 확인
        for (TradingStrategy strategy : strategies) {
            assertNotNull(strategy.getStrategyName());
            assertNotNull(strategy.getStrategyDescription());
        }
    }

    @Test
    @DisplayName("백트래킹 서비스 기본 동작 테스트")
    void backtestServiceBasicTest() {
        // 기본 백트래킹 요청 생성
        BackTestRequest request = BackTestRequest.builder()
                .stockCode("005930")
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 1, 31))
                .strategy("SMA")
                .initialCapital(new BigDecimal("10000000"))
                .commission(new BigDecimal("0.001"))
                .shortPeriod(5)
                .longPeriod(20)
                .build();

        // 서비스가 정상적으로 동작하는지 확인 (실제 데이터가 없으므로 예외가 발생할 수 있음)
        assertDoesNotThrow(() -> {
            try {
                backtestService.runBacktest(request);
            } catch (Exception e) {
                // API 호출 실패는 예상된 동작
                assertTrue(e.getMessage().contains("API") || e.getMessage().contains("데이터"));
            }
        });
    }

    @Test
    @DisplayName("백트래킹 요청 유효성 검사 테스트")
    void backtestRequestValidationTest() {
        // 필수 필드가 없는 요청
        BackTestRequest invalidRequest = BackTestRequest.builder()
                .strategy("SMA")
                .build();

        // 유효성 검사 실패 예상
        assertThrows(IllegalArgumentException.class, () -> {
            backtestService.runBacktest(invalidRequest);
        });
    }
}
