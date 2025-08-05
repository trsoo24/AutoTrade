package trade.project.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import trade.project.api.client.KisApiClient;
import trade.project.api.dto.StockOrderRequest;
import trade.project.api.dto.StockOrderResponse;
import trade.project.api.dto.OrderStatusRequest;
import trade.project.common.exception.ApiException;
import trade.project.trading.service.TradingRecordService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockOrderService {

    private final KisApiClient kisApiClient;
    private final TradingRecordService tradingRecordService;

    /**
     * 주식 주문 실행
     */
    public StockOrderResponse executeOrder(StockOrderRequest request) {
        try {
            log.info("주식 주문 실행: {}", request);
            // 주문 수량이 0 이하일 경우 주문하지 않음
            if (request.getQuantity() == null || request.getQuantity() <= 0) {
                log.warn("주문 수량이 0 이하이므로 주문을 실행하지 않습니다: {}", request);
                return StockOrderResponse.builder()
                        .accountNumber(request.getAccountNumber())
                        .stockCode(request.getStockCode())
                        .orderType(request.getOrderType())
                        .quantity(request.getQuantity())
                        .price(request.getPrice())
                        .priceType(request.getPriceType())
                        .orderCategory(request.getOrderCategory())
                        .orderDateTime(java.time.LocalDateTime.now())
                        .orderTime(java.time.LocalDateTime.now().toString())
                        .orderStatus("수량 0 - 미실행")
                        .errorCode("QTY_ZERO")
                        .errorMessage("주문 수량이 0 이하이므로 주문이 실행되지 않았습니다.")
                        .build();
            }
            
            // 주문 파라미터 변환
            Map<String, String> orderParams = convertToOrderParams(request);
            
            // KIS API 호출
            Map<String, Object> response = kisApiClient.executeStockOrder(orderParams);
            
            // 응답 변환
            StockOrderResponse orderResponse = convertToOrderResponse(request, response);
            
            // 매매 기록 저장
            try {
                tradingRecordService.saveTradingRecord(request, orderResponse, response);
            } catch (Exception e) {
                log.error("매매 기록 저장 중 오류 발생: {}", e.getMessage());
                // 매매 기록 저장 실패는 주문 실행에 영향을 주지 않도록 함
            }
            
            return orderResponse;
            
        } catch (Exception e) {
            log.error("주식 주문 실행 중 오류 발생: {}", e.getMessage());
            throw new ApiException("주식 주문 실행 실패", e);
        }
    }

    /**
     * 주문 상태 조회
     */
    public StockOrderResponse getOrderStatus(OrderStatusRequest request) {
        try {
            log.info("주문 상태 조회: {}", request);
            
            // KIS API 호출
            Map<String, Object> response = kisApiClient.getOrderStatus(
                request.getAccountNumber(), 
                request.getOrderNumber()
            );
            
            // 응답 변환
            return convertToOrderStatusResponse(request, response);
            
        } catch (Exception e) {
            log.error("주문 상태 조회 중 오류 발생: {}", e.getMessage());
            throw new ApiException("주문 상태 조회 실패", e);
        }
    }

    /**
     * 주문 파라미터 변환
     */
    private Map<String, String> convertToOrderParams(StockOrderRequest request) {
        Map<String, String> params = new HashMap<>();
        
        // 기본 정보
        params.put("CANO", request.getAccountNumber()); // 종합계좌번호
        params.put("ACNT_PRDT_CD", "01"); // 계좌상품코드 (고정값)
        params.put("PDNO", request.getStockCode()); // 종목코드
        
        // 주문구분 설정
        if ("매수".equals(request.getOrderType())) {
            params.put("ORD_DVSN", "00"); // 매수
        } else if ("매도".equals(request.getOrderType())) {
            params.put("ORD_DVSN", "01"); // 매도
        }
        
        // 주문수량
        params.put("ORD_QTY", String.valueOf(request.getQuantity()));
        
        // 주문가격
        if ("시장가".equals(request.getPriceType())) {
            params.put("ORD_UNPR", "0"); // 시장가는 0
        } else {
            params.put("ORD_UNPR", String.valueOf(request.getPrice()));
        }
        
        // 주문구분 (일반/정정/취소)
        if ("정정".equals(request.getOrderCategory())) {
            params.put("CTAC_TLNO", request.getOriginalOrderNumber());
            params.put("MGCO_APTM_ODNO", "");
            params.put("ORD_APLC_DVSN", "01"); // 정정
        } else if ("취소".equals(request.getOrderCategory())) {
            params.put("CTAC_TLNO", request.getOriginalOrderNumber());
            params.put("MGCO_APTM_ODNO", "");
            params.put("ORD_APLC_DVSN", "02"); // 취소
        } else {
            params.put("CTAC_TLNO", "");
            params.put("MGCO_APTM_ODNO", "");
            params.put("ORD_APLC_DVSN", "00"); // 일반
        }
        
        return params;
    }

    /**
     * 주문 응답 변환
     */
    private StockOrderResponse convertToOrderResponse(StockOrderRequest request, Map<String, Object> response) {
        StockOrderResponse.StockOrderResponseBuilder builder = StockOrderResponse.builder()
                .accountNumber(request.getAccountNumber())
                .stockCode(request.getStockCode())
                .orderType(request.getOrderType())
                .quantity(request.getQuantity())
                .price(request.getPrice())
                .priceType(request.getPriceType())
                .orderCategory(request.getOrderCategory())
                .orderDateTime(LocalDateTime.now())
                .orderTime(LocalDateTime.now().toString());

        // 응답 데이터 파싱
        if (response.containsKey("output")) {
            Map<String, Object> output = (Map<String, Object>) response.get("output");
            if (output != null) {
                builder.orderNumber((String) output.get("ODNO")) // 주문번호
                       .orderStatus("접수완료");
            }
        }

        // 에러 처리
        if (response.containsKey("rt_cd") && !"0".equals(response.get("rt_cd"))) {
            builder.errorCode((String) response.get("rt_cd"))
                   .errorMessage((String) response.get("msg1"));
        } else {
            builder.message("주문이 정상적으로 접수되었습니다.");
        }

        return builder.build();
    }

    /**
     * 주문 상태 응답 변환
     */
    private StockOrderResponse convertToOrderStatusResponse(OrderStatusRequest request, Map<String, Object> response) {
        StockOrderResponse.StockOrderResponseBuilder builder = StockOrderResponse.builder()
                .accountNumber(request.getAccountNumber())
                .orderNumber(request.getOrderNumber())
                .orderTime(LocalDateTime.now().toString());

        // 응답 데이터 파싱
        if (response.containsKey("output")) {
            Map<String, Object> output = (Map<String, Object>) response.get("output");
            if (output != null) {
                builder.stockCode((String) output.get("PDNO"))
                       .stockName((String) output.get("PRDT_NAME"))
                       .orderType(convertOrderType((String) output.get("ORD_DVSN")))
                       .quantity(parseInteger(output.get("ORD_QTY")))
                       .price(parseInteger(output.get("ORD_UNPR")))
                       .orderStatus((String) output.get("ORD_STAT_NM"));
            }
        }

        // 에러 처리
        if (response.containsKey("rt_cd") && !"0".equals(response.get("rt_cd"))) {
            builder.errorCode((String) response.get("rt_cd"))
                   .errorMessage((String) response.get("msg1"));
        } else {
            builder.message("주문 상태 조회가 완료되었습니다.");
        }

        return builder.build();
    }

    /**
     * 주문구분 코드를 한글로 변환
     */
    private String convertOrderType(String orderDvsn) {
        if ("00".equals(orderDvsn)) {
            return "매수";
        } else if ("01".equals(orderDvsn)) {
            return "매도";
        }
        return orderDvsn;
    }

    private Integer parseInteger(Object value) {
        if (value == null) return null;
        try {
            if (value instanceof String) {
                return Integer.parseInt((String) value);
            } else if (value instanceof Number) {
                return ((Number) value).intValue();
            }
        } catch (NumberFormatException e) {
            log.warn("Integer 파싱 실패: {}", value);
        }
        return null;
    }
} 