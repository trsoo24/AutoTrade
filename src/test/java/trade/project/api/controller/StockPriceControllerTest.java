package trade.project.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import trade.project.api.dto.StockPriceRequest;
import trade.project.api.dto.StockPriceResponse;
import trade.project.api.dto.StockDailyPriceRequest;
import trade.project.api.dto.StockDailyPriceResponse;
import trade.project.api.service.StockPriceService;

import java.time.LocalDate;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StockPriceController.class)
class StockPriceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StockPriceService stockPriceService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getCurrentPrice_Success() throws Exception {
        // Given
        StockPriceRequest request = StockPriceRequest.builder()
                .stockCode("005930")
                .build();

        StockPriceResponse response = StockPriceResponse.builder()
                .stockCode("005930")
                .stockName("삼성전자")
                .currentPrice(70000)
                .previousClose(69500)
                .openPrice(69800)
                .highPrice(70500)
                .lowPrice(69500)
                .tradingVolume(1000000L)
                .tradingValue(70000000000L)
                .changeRate(0.72)
                .changeAmount(500)
                .marketStatus("정상")
                .message("현재가 조회가 완료되었습니다.")
                .build();

        when(stockPriceService.getCurrentPrice(any(StockPriceRequest.class), any()))
                .thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/stock/price/current")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.stockCode").value("005930"))
                .andExpect(jsonPath("$.data.stockName").value("삼성전자"))
                .andExpect(jsonPath("$.data.currentPrice").value(70000))
                .andExpect(jsonPath("$.data.changeRate").value(0.72));
    }

    @Test
    void getCurrentPrice_ValidationError() throws Exception {
        // Given
        StockPriceRequest request = StockPriceRequest.builder()
                .stockCode("") // 빈 종목코드
                .build();

        // When & Then
        mockMvc.perform(post("/api/stock/price/current")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getDailyPrices_Success() throws Exception {
        // Given
        StockDailyPriceRequest request = StockDailyPriceRequest.builder()
                .stockCode("005930")
                .startDate("20231201")
                .endDate("20231207")
                .build();

        StockDailyPriceResponse.DailyPriceData dailyData1 = StockDailyPriceResponse.DailyPriceData.builder()
                .date(LocalDate.of(2023, 12, 1))
                .openPrice(70000)
                .highPrice(70500)
                .lowPrice(69500)
                .closePrice(70200)
                .tradingVolume(1000000L)
                .tradingValue(70000000000L)
                .changeRate(1.0)
                .changeAmount(200)
                .build();

        StockDailyPriceResponse.DailyPriceData dailyData2 = StockDailyPriceResponse.DailyPriceData.builder()
                .date(LocalDate.of(2023, 12, 4))
                .openPrice(70200)
                .highPrice(71000)
                .lowPrice(70000)
                .closePrice(70800)
                .tradingVolume(1200000L)
                .tradingValue(84000000000L)
                .changeRate(0.85)
                .changeAmount(600)
                .build();

        StockDailyPriceResponse response = StockDailyPriceResponse.builder()
                .stockCode("005930")
                .stockName("삼성전자")
                .startDate("20231201")
                .endDate("20231207")
                .dailyPrices(Arrays.asList(dailyData1, dailyData2))
                .message("일자별 시세 조회가 완료되었습니다.")
                .build();

        when(stockPriceService.getDailyPrices(any(StockDailyPriceRequest.class), any()))
                .thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/stock/price/daily")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.stockCode").value("005930"))
                .andExpect(jsonPath("$.data.stockName").value("삼성전자"))
                .andExpect(jsonPath("$.data.dailyPrices").isArray())
                .andExpect(jsonPath("$.data.dailyPrices.length()").value(2));
    }

    @Test
    void getCurrentPriceByGet_Success() throws Exception {
        // Given
        StockPriceResponse response = StockPriceResponse.builder()
                .stockCode("005930")
                .stockName("삼성전자")
                .currentPrice(70000)
                .previousClose(69500)
                .openPrice(69800)
                .highPrice(70500)
                .lowPrice(69500)
                .tradingVolume(1000000L)
                .tradingValue(70000000000L)
                .changeRate(0.72)
                .changeAmount(500)
                .marketStatus("정상")
                .message("현재가 조회가 완료되었습니다.")
                .build();

        when(stockPriceService.getCurrentPrice(any(StockPriceRequest.class), any()))
                .thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/stock/price/current/005930"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.stockCode").value("005930"))
                .andExpect(jsonPath("$.data.stockName").value("삼성전자"));
    }

    @Test
    void testSamsungPrice_Success() throws Exception {
        // Given
        StockPriceResponse response = StockPriceResponse.builder()
                .stockCode("005930")
                .stockName("삼성전자")
                .currentPrice(70000)
                .previousClose(69500)
                .openPrice(69800)
                .highPrice(70500)
                .lowPrice(69500)
                .tradingVolume(1000000L)
                .tradingValue(70000000000L)
                .changeRate(0.72)
                .changeAmount(500)
                .marketStatus("정상")
                .message("현재가 조회가 완료되었습니다.")
                .build();

        when(stockPriceService.getCurrentPrice(any(StockPriceRequest.class), any()))
                .thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/stock/price/test/samsung"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.stockCode").value("005930"))
                .andExpect(jsonPath("$.data.stockName").value("삼성전자"));
    }

    @Test
    void testSKHynixPrice_Success() throws Exception {
        // Given
        StockPriceResponse response = StockPriceResponse.builder()
                .stockCode("000660")
                .stockName("SK하이닉스")
                .currentPrice(120000)
                .previousClose(118000)
                .openPrice(119000)
                .highPrice(122000)
                .lowPrice(118500)
                .tradingVolume(800000L)
                .tradingValue(96000000000L)
                .changeRate(1.69)
                .changeAmount(2000)
                .marketStatus("정상")
                .message("현재가 조회가 완료되었습니다.")
                .build();

        when(stockPriceService.getCurrentPrice(any(StockPriceRequest.class), any()))
                .thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/stock/price/test/skhynix"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.stockCode").value("000660"))
                .andExpect(jsonPath("$.data.stockName").value("SK하이닉스"));
    }

    @Test
    void testNaverPrice_Success() throws Exception {
        // Given
        StockPriceResponse response = StockPriceResponse.builder()
                .stockCode("035420")
                .stockName("NAVER")
                .currentPrice(180000)
                .previousClose(178000)
                .openPrice(179000)
                .highPrice(182000)
                .lowPrice(178500)
                .tradingVolume(600000L)
                .tradingValue(108000000000L)
                .changeRate(1.12)
                .changeAmount(2000)
                .marketStatus("정상")
                .message("현재가 조회가 완료되었습니다.")
                .build();

        when(stockPriceService.getCurrentPrice(any(StockPriceRequest.class), any()))
                .thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/stock/price/test/naver"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.stockCode").value("035420"))
                .andExpect(jsonPath("$.data.stockName").value("NAVER"));
    }

    @Test
    void testSamsungDailyPrice_Success() throws Exception {
        // Given
        StockDailyPriceResponse.DailyPriceData dailyData = StockDailyPriceResponse.DailyPriceData.builder()
                .date(LocalDate.of(2023, 12, 1))
                .openPrice(70000)
                .highPrice(70500)
                .lowPrice(69500)
                .closePrice(70200)
                .tradingVolume(1000000L)
                .tradingValue(70000000000L)
                .changeRate(1.0)
                .changeAmount(200)
                .build();

        StockDailyPriceResponse response = StockDailyPriceResponse.builder()
                .stockCode("005930")
                .stockName("삼성전자")
                .startDate("20231201")
                .endDate("20231207")
                .dailyPrices(Arrays.asList(dailyData))
                .message("일자별 시세 조회가 완료되었습니다.")
                .build();

        when(stockPriceService.getDailyPrices(any(StockDailyPriceRequest.class), any()))
                .thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/stock/price/test/samsung/daily"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.stockCode").value("005930"))
                .andExpect(jsonPath("$.data.stockName").value("삼성전자"))
                .andExpect(jsonPath("$.data.dailyPrices").isArray());
    }
} 