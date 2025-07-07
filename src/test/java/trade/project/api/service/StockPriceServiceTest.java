package trade.project.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import trade.project.api.client.KisApiClient;
import trade.project.api.dto.StockPriceRequest;
import trade.project.api.dto.StockPriceResponse;
import trade.project.api.dto.StockDailyPriceRequest;
import trade.project.api.dto.StockDailyPriceResponse;
import trade.project.common.exception.ApiException;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class StockPriceServiceTest {

    @Mock
    private KisApiClient kisApiClient;

    @InjectMocks
    private StockPriceService stockPriceService;

    private StockPriceRequest stockPriceRequest;
    private StockDailyPriceRequest dailyPriceRequest;

    @BeforeEach
    void setUp() {
        stockPriceRequest = StockPriceRequest.builder()
                .stockCode("005930")
                .build();

        dailyPriceRequest = StockDailyPriceRequest.builder()
                .stockCode("005930")
                .startDate("20231201")
                .endDate("20231207")
                .build();
    }

    @Test
    void getCurrentPrice_Success() {
        // Given
        Map<String, Object> mockResponse = new HashMap<>();
        Map<String, Object> output = new HashMap<>();
        output.put("hts_kor_isnm", "삼성전자");
        output.put("stck_prpr", "70000");
        output.put("stck_hgpr", "69500");
        output.put("stck_oprc", "69800");
        output.put("stck_hgpr", "70500");
        output.put("stck_lwpr", "69500");
        output.put("acml_vol", "1000000");
        output.put("acml_tr_pbmn", "70000000000");
        output.put("prdy_vrss", "0.72");
        output.put("prdy_vrss_sign", "500");
        output.put("hts_avls", "정상");
        mockResponse.put("output", output);
        mockResponse.put("rt_cd", "0");

        when(kisApiClient.getStockPrice(anyString()))
                .thenReturn(mockResponse);

        // When
        StockPriceResponse response = stockPriceService.getCurrentPrice(stockPriceRequest, mock(jakarta.servlet.http.HttpServletRequest.class));

        // Then
        assertNotNull(response);
        assertEquals("005930", response.getStockCode());
        assertEquals("삼성전자", response.getStockName());
        assertEquals(70000, response.getCurrentPrice());
        assertEquals(69500, response.getPreviousClose());
        assertEquals(69800, response.getOpenPrice());
        assertEquals(70500, response.getHighPrice());
        assertEquals(69500, response.getLowPrice());
        assertEquals(1000000L, response.getTradingVolume());
        assertEquals(70000000000L, response.getTradingValue());
        assertEquals(0.72, response.getChangeRate());
        assertEquals(500, response.getChangeAmount());
        assertEquals("정상", response.getMarketStatus());
        assertEquals("현재가 조회가 완료되었습니다.", response.getMessage());
    }

    @Test
    void getCurrentPrice_ApiError_ThrowsException() {
        // Given
        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("rt_cd", "1");
        mockResponse.put("msg1", "종목코드를 찾을 수 없습니다");

        when(kisApiClient.getStockPrice(anyString()))
                .thenReturn(mockResponse);

        // When & Then
        StockPriceResponse response = stockPriceService.getCurrentPrice(stockPriceRequest, mock(jakarta.servlet.http.HttpServletRequest.class));
        
        assertNotNull(response);
        assertEquals("1", response.getErrorCode());
        assertEquals("종목코드를 찾을 수 없습니다", response.getErrorMessage());
    }

    @Test
    void getDailyPrices_Success() {
        // Given
        Map<String, Object> mockResponse = new HashMap<>();
        Map<String, Object> output1 = new HashMap<>();
        output1.put("hts_kor_isnm", "삼성전자");
        output1.put("stck_bsop_date", "20231201");
        output1.put("stck_oprc", "70000");
        output1.put("stck_hgpr", "70500");
        output1.put("stck_lwpr", "69500");
        output1.put("stck_prpr", "70200");
        output1.put("acml_vol", "1000000");
        output1.put("acml_tr_pbmn", "70000000000");
        output1.put("prdy_vrss", "1.0");
        output1.put("prdy_vrss_sign", "200");

        Map<String, Object> output2 = new HashMap<>();
        output2.put("hts_kor_isnm", "삼성전자");
        output2.put("stck_bsop_date", "20231204");
        output2.put("stck_oprc", "70200");
        output2.put("stck_hgpr", "71000");
        output2.put("stck_lwpr", "70000");
        output2.put("stck_prpr", "70800");
        output2.put("acml_vol", "1200000");
        output2.put("acml_tr_pbmn", "84000000000");
        output2.put("prdy_vrss", "0.85");
        output2.put("prdy_vrss_sign", "600");

        List<Map<String, Object>> outputList = List.of(output1, output2);
        mockResponse.put("output", outputList);
        mockResponse.put("rt_cd", "0");

        when(kisApiClient.getStockDailyPrice(anyString(), anyString(), anyString()))
                .thenReturn(mockResponse);

        // When
        StockDailyPriceResponse response = stockPriceService.getDailyPrices(dailyPriceRequest, mock(jakarta.servlet.http.HttpServletRequest.class));

        // Then
        assertNotNull(response);
        assertEquals("005930", response.getStockCode());
        assertEquals("삼성전자", response.getStockName());
        assertEquals("20231201", response.getStartDate());
        assertEquals("20231207", response.getEndDate());
        assertNotNull(response.getDailyPrices());
        assertEquals(2, response.getDailyPrices().size());
        
        StockDailyPriceResponse.DailyPriceData firstData = response.getDailyPrices().get(0);
        assertEquals(LocalDate.of(2023, 12, 1), firstData.getDate());
        assertEquals(70000, firstData.getOpenPrice());
        assertEquals(70500, firstData.getHighPrice());
        assertEquals(69500, firstData.getLowPrice());
        assertEquals(70200, firstData.getClosePrice());
        assertEquals(1000000L, firstData.getTradingVolume());
        assertEquals(70000000000L, firstData.getTradingValue());
        assertEquals(1.0, firstData.getChangeRate());
        assertEquals(200, firstData.getChangeAmount());
        
        assertEquals("일자별 시세 조회가 완료되었습니다.", response.getMessage());
    }

    @Test
    void getDailyPrices_ApiError_ThrowsException() {
        // Given
        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("rt_cd", "1");
        mockResponse.put("msg1", "일자 범위가 잘못되었습니다");

        when(kisApiClient.getStockDailyPrice(anyString(), anyString(), anyString()))
                .thenReturn(mockResponse);

        // When & Then
        StockDailyPriceResponse response = stockPriceService.getDailyPrices(dailyPriceRequest, mock(jakarta.servlet.http.HttpServletRequest.class));
        
        assertNotNull(response);
        assertEquals("1", response.getErrorCode());
        assertEquals("일자 범위가 잘못되었습니다", response.getErrorMessage());
    }

    @Test
    void getCurrentPrice_EmptyOutput_ReturnsNullValues() {
        // Given
        Map<String, Object> mockResponse = new HashMap<>();
        Map<String, Object> output = new HashMap<>();
        // 빈 output 데이터
        mockResponse.put("output", output);
        mockResponse.put("rt_cd", "0");

        when(kisApiClient.getStockPrice(anyString()))
                .thenReturn(mockResponse);

        // When
        StockPriceResponse response = stockPriceService.getCurrentPrice(stockPriceRequest, mock(jakarta.servlet.http.HttpServletRequest.class));

        // Then
        assertNotNull(response);
        assertEquals("005930", response.getStockCode());
        assertNull(response.getStockName());
        assertNull(response.getCurrentPrice());
        assertNull(response.getPreviousClose());
        assertEquals("현재가 조회가 완료되었습니다.", response.getMessage());
    }

    @Test
    void getDailyPrices_EmptyOutput_ReturnsEmptyList() {
        // Given
        Map<String, Object> mockResponse = new HashMap<>();
        List<Map<String, Object>> emptyList = List.of();
        mockResponse.put("output", emptyList);
        mockResponse.put("rt_cd", "0");

        when(kisApiClient.getStockDailyPrice(anyString(), anyString(), anyString()))
                .thenReturn(mockResponse);

        // When
        StockDailyPriceResponse response = stockPriceService.getDailyPrices(dailyPriceRequest, mock(jakarta.servlet.http.HttpServletRequest.class));

        // Then
        assertNotNull(response);
        assertEquals("005930", response.getStockCode());
        assertEquals("20231201", response.getStartDate());
        assertEquals("20231207", response.getEndDate());
        assertNotNull(response.getDailyPrices());
        assertTrue(response.getDailyPrices().isEmpty());
        assertEquals("일자별 시세 조회가 완료되었습니다.", response.getMessage());
    }

    @Test
    void getCurrentPrice_InvalidNumberFormat_HandlesGracefully() {
        // Given
        Map<String, Object> mockResponse = new HashMap<>();
        Map<String, Object> output = new HashMap<>();
        output.put("hts_kor_isnm", "삼성전자");
        output.put("stck_prpr", "invalid_number");
        output.put("acml_vol", "invalid_volume");
        output.put("prdy_vrss", "invalid_rate");
        mockResponse.put("output", output);
        mockResponse.put("rt_cd", "0");

        when(kisApiClient.getStockPrice(anyString()))
                .thenReturn(mockResponse);

        // When
        StockPriceResponse response = stockPriceService.getCurrentPrice(stockPriceRequest, mock(jakarta.servlet.http.HttpServletRequest.class));

        // Then
        assertNotNull(response);
        assertEquals("005930", response.getStockCode());
        assertEquals("삼성전자", response.getStockName());
        assertNull(response.getCurrentPrice()); // 파싱 실패로 null
        assertNull(response.getTradingVolume()); // 파싱 실패로 null
        assertNull(response.getChangeRate()); // 파싱 실패로 null
        assertEquals("현재가 조회가 완료되었습니다.", response.getMessage());
    }
} 