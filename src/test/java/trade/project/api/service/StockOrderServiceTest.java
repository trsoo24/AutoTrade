package trade.project.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import trade.project.api.client.KisApiClient;
import trade.project.api.dto.StockOrderRequest;
import trade.project.api.dto.StockOrderResponse;
import trade.project.api.dto.OrderStatusRequest;
import trade.project.common.exception.ApiException;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockOrderServiceTest {

    @Mock
    private KisApiClient kisApiClient;

    @InjectMocks
    private StockOrderService stockOrderService;

    private StockOrderRequest buyOrderRequest;
    private StockOrderRequest sellOrderRequest;
    private OrderStatusRequest orderStatusRequest;

    @BeforeEach
    void setUp() {
        buyOrderRequest = StockOrderRequest.builder()
                .accountNumber("1234567890")
                .stockCode("005930")
                .orderType("매수")
                .quantity(10)
                .price(70000)
                .priceType("지정가")
                .orderCategory("일반")
                .build();

        sellOrderRequest = StockOrderRequest.builder()
                .accountNumber("1234567890")
                .stockCode("005930")
                .orderType("매도")
                .quantity(5)
                .price(72000)
                .priceType("지정가")
                .orderCategory("일반")
                .build();

        orderStatusRequest = OrderStatusRequest.builder()
                .accountNumber("1234567890")
                .orderNumber("202312010001")
                .build();
    }

    @Test
    void executeOrder_BuyOrder_Success() {
        // Given
        Map<String, Object> mockResponse = new HashMap<>();
        Map<String, Object> output = new HashMap<>();
        output.put("ODNO", "202312010001");
        mockResponse.put("output", output);
        mockResponse.put("rt_cd", "0");

        when(kisApiClient.executeStockOrder(any(Map.class)))
                .thenReturn(mockResponse);

        // When
        StockOrderResponse response = stockOrderService.executeOrder(buyOrderRequest);

        // Then
        assertNotNull(response);
        assertEquals("202312010001", response.getOrderNumber());
        assertEquals("매수", response.getOrderType());
        assertEquals(10, response.getQuantity());
        assertEquals(70000, response.getPrice());
        assertEquals("지정가", response.getPriceType());
        assertEquals("일반", response.getOrderCategory());
        assertEquals("주문이 정상적으로 접수되었습니다.", response.getMessage());
    }

    @Test
    void executeOrder_SellOrder_Success() {
        // Given
        Map<String, Object> mockResponse = new HashMap<>();
        Map<String, Object> output = new HashMap<>();
        output.put("ODNO", "202312010002");
        mockResponse.put("output", output);
        mockResponse.put("rt_cd", "0");

        when(kisApiClient.executeStockOrder(any(Map.class)))
                .thenReturn(mockResponse);

        // When
        StockOrderResponse response = stockOrderService.executeOrder(sellOrderRequest);

        // Then
        assertNotNull(response);
        assertEquals("202312010002", response.getOrderNumber());
        assertEquals("매도", response.getOrderType());
        assertEquals(5, response.getQuantity());
        assertEquals(72000, response.getPrice());
    }

    @Test
    void executeOrder_MarketPrice_Success() {
        // Given
        StockOrderRequest marketOrderRequest = StockOrderRequest.builder()
                .accountNumber("1234567890")
                .stockCode("005930")
                .orderType("매수")
                .quantity(10)
                .price(0)
                .priceType("시장가")
                .orderCategory("일반")
                .build();

        Map<String, Object> mockResponse = new HashMap<>();
        Map<String, Object> output = new HashMap<>();
        output.put("ODNO", "202312010003");
        mockResponse.put("output", output);
        mockResponse.put("rt_cd", "0");

        when(kisApiClient.executeStockOrder(any(Map.class)))
                .thenReturn(mockResponse);

        // When
        StockOrderResponse response = stockOrderService.executeOrder(marketOrderRequest);

        // Then
        assertNotNull(response);
        assertEquals("시장가", response.getPriceType());
    }

    @Test
    void executeOrder_ApiError_ThrowsException() {
        // Given
        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("rt_cd", "1");
        mockResponse.put("msg1", "API 오류 발생");

        when(kisApiClient.executeStockOrder(any(Map.class)))
                .thenReturn(mockResponse);

        // When & Then
        StockOrderResponse response = stockOrderService.executeOrder(buyOrderRequest);
        
        assertNotNull(response);
        assertEquals("1", response.getErrorCode());
        assertEquals("API 오류 발생", response.getErrorMessage());
    }

    @Test
    void getOrderStatus_Success() {
        // Given
        Map<String, Object> mockResponse = new HashMap<>();
        Map<String, Object> output = new HashMap<>();
        output.put("PDNO", "005930");
        output.put("PRDT_NAME", "삼성전자");
        output.put("ORD_DVSN", "00");
        output.put("ORD_QTY", "10");
        output.put("ORD_UNPR", "70000");
        output.put("ORD_STAT_NM", "체결완료");
        mockResponse.put("output", output);
        mockResponse.put("rt_cd", "0");

        when(kisApiClient.getOrderStatus(anyString(), anyString()))
                .thenReturn(mockResponse);

        // When
        StockOrderResponse response = stockOrderService.getOrderStatus(orderStatusRequest);

        // Then
        assertNotNull(response);
        assertEquals("005930", response.getStockCode());
        assertEquals("삼성전자", response.getStockName());
        assertEquals("매수", response.getOrderType());
        assertEquals(10, response.getQuantity());
        assertEquals(70000, response.getPrice());
        assertEquals("체결완료", response.getOrderStatus());
    }

    @Test
    void getOrderStatus_ApiError_ThrowsException() {
        // Given
        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("rt_cd", "1");
        mockResponse.put("msg1", "주문번호를 찾을 수 없습니다");

        when(kisApiClient.getOrderStatus(anyString(), anyString()))
                .thenReturn(mockResponse);

        // When & Then
        StockOrderResponse response = stockOrderService.getOrderStatus(orderStatusRequest);
        
        assertNotNull(response);
        assertEquals("1", response.getErrorCode());
        assertEquals("주문번호를 찾을 수 없습니다", response.getErrorMessage());
    }

    @Test
    void executeOrder_CancelOrder_Success() {
        // Given
        StockOrderRequest cancelOrderRequest = StockOrderRequest.builder()
                .accountNumber("1234567890")
                .stockCode("005930")
                .orderType("매수")
                .quantity(10)
                .price(70000)
                .priceType("지정가")
                .orderCategory("취소")
                .originalOrderNumber("202312010001")
                .build();

        Map<String, Object> mockResponse = new HashMap<>();
        Map<String, Object> output = new HashMap<>();
        output.put("ODNO", "202312010004");
        mockResponse.put("output", output);
        mockResponse.put("rt_cd", "0");

        when(kisApiClient.executeStockOrder(any(Map.class)))
                .thenReturn(mockResponse);

        // When
        StockOrderResponse response = stockOrderService.executeOrder(cancelOrderRequest);

        // Then
        assertNotNull(response);
        assertEquals("취소", response.getOrderCategory());
    }

    @Test
    void executeOrder_ModifyOrder_Success() {
        // Given
        StockOrderRequest modifyOrderRequest = StockOrderRequest.builder()
                .accountNumber("1234567890")
                .stockCode("005930")
                .orderType("매수")
                .quantity(15)
                .price(75000)
                .priceType("지정가")
                .orderCategory("정정")
                .originalOrderNumber("202312010001")
                .build();

        Map<String, Object> mockResponse = new HashMap<>();
        Map<String, Object> output = new HashMap<>();
        output.put("ODNO", "202312010005");
        mockResponse.put("output", output);
        mockResponse.put("rt_cd", "0");

        when(kisApiClient.executeStockOrder(any(Map.class)))
                .thenReturn(mockResponse);

        // When
        StockOrderResponse response = stockOrderService.executeOrder(modifyOrderRequest);

        // Then
        assertNotNull(response);
        assertEquals("정정", response.getOrderCategory());
        assertEquals(15, response.getQuantity());
        assertEquals(75000, response.getPrice());
    }
} 