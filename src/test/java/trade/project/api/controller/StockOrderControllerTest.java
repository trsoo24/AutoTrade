package trade.project.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import trade.project.api.dto.StockOrderRequest;
import trade.project.api.dto.StockOrderResponse;
import trade.project.api.dto.OrderStatusRequest;
import trade.project.api.service.StockOrderService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StockOrderController.class)
class StockOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StockOrderService stockOrderService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void executeOrder_Success() throws Exception {
        // Given
        StockOrderRequest request = StockOrderRequest.builder()
                .accountNumber("1234567890")
                .stockCode("005930")
                .orderType("매수")
                .quantity(10)
                .price(70000)
                .priceType("지정가")
                .orderCategory("일반")
                .build();

        StockOrderResponse response = StockOrderResponse.builder()
                .orderNumber("202312010001")
                .accountNumber("1234567890")
                .stockCode("005930")
                .stockName("삼성전자")
                .orderType("매수")
                .quantity(10)
                .price(70000)
                .priceType("지정가")
                .orderStatus("접수완료")
                .orderCategory("일반")
                .message("주문이 정상적으로 접수되었습니다.")
                .build();

        when(stockOrderService.executeOrder(any(StockOrderRequest.class)))
                .thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/stock/order/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.orderNumber").value("202312010001"))
                .andExpect(jsonPath("$.data.orderType").value("매수"))
                .andExpect(jsonPath("$.data.quantity").value(10))
                .andExpect(jsonPath("$.data.price").value(70000));
    }

    @Test
    void executeOrder_ValidationError() throws Exception {
        // Given
        StockOrderRequest request = StockOrderRequest.builder()
                .accountNumber("") // 빈 계좌번호
                .stockCode("005930")
                .orderType("매수")
                .quantity(10)
                .price(70000)
                .build();

        // When & Then
        mockMvc.perform(post("/api/stock/order/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getOrderStatus_Success() throws Exception {
        // Given
        OrderStatusRequest request = OrderStatusRequest.builder()
                .accountNumber("1234567890")
                .orderNumber("202312010001")
                .build();

        StockOrderResponse response = StockOrderResponse.builder()
                .orderNumber("202312010001")
                .accountNumber("1234567890")
                .stockCode("005930")
                .stockName("삼성전자")
                .orderType("매수")
                .quantity(10)
                .price(70000)
                .orderStatus("체결완료")
                .message("주문 상태 조회가 완료되었습니다.")
                .build();

        when(stockOrderService.getOrderStatus(any(OrderStatusRequest.class)))
                .thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/stock/order/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.orderNumber").value("202312010001"))
                .andExpect(jsonPath("$.data.orderStatus").value("체결완료"));
    }

    @Test
    void testBuyOrder_Success() throws Exception {
        // Given
        StockOrderResponse response = StockOrderResponse.builder()
                .orderNumber("202312010002")
                .accountNumber("1234567890")
                .stockCode("005930")
                .stockName("삼성전자")
                .orderType("매수")
                .quantity(10)
                .price(70000)
                .priceType("지정가")
                .orderStatus("접수완료")
                .orderCategory("일반")
                .message("주문이 정상적으로 접수되었습니다.")
                .build();

        when(stockOrderService.executeOrder(any(StockOrderRequest.class)))
                .thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/stock/order/test/buy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.orderType").value("매수"));
    }

    @Test
    void testSellOrder_Success() throws Exception {
        // Given
        StockOrderResponse response = StockOrderResponse.builder()
                .orderNumber("202312010003")
                .accountNumber("1234567890")
                .stockCode("005930")
                .stockName("삼성전자")
                .orderType("매도")
                .quantity(5)
                .price(72000)
                .priceType("지정가")
                .orderStatus("접수완료")
                .orderCategory("일반")
                .message("주문이 정상적으로 접수되었습니다.")
                .build();

        when(stockOrderService.executeOrder(any(StockOrderRequest.class)))
                .thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/stock/order/test/sell"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.orderType").value("매도"));
    }

    @Test
    void testMarketBuyOrder_Success() throws Exception {
        // Given
        StockOrderResponse response = StockOrderResponse.builder()
                .orderNumber("202312010004")
                .accountNumber("1234567890")
                .stockCode("005930")
                .stockName("삼성전자")
                .orderType("매수")
                .quantity(10)
                .price(0)
                .priceType("시장가")
                .orderStatus("접수완료")
                .orderCategory("일반")
                .message("주문이 정상적으로 접수되었습니다.")
                .build();

        when(stockOrderService.executeOrder(any(StockOrderRequest.class)))
                .thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/stock/order/test/market-buy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.priceType").value("시장가"));
    }
} 