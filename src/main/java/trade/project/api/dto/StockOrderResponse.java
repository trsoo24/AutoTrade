package trade.project.api.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockOrderResponse {
    
    private String orderNumber; // 주문번호
    private String accountNumber; // 계좌번호
    private String stockCode; // 종목코드
    private String stockName; // 종목명
    private String orderType; // 주문구분 (매수/매도)
    private Integer quantity; // 주문수량
    private Integer price; // 주문가격
    private String priceType; // 주문유형 (지정가/시장가)
    private String orderStatus; // 주문상태
    private String orderCategory; // 주문구분 (일반/정정/취소)
    private LocalDateTime orderDateTime; // 주문일시
    private String message; // 응답메시지
    private String errorCode; // 에러코드
    private String errorMessage; // 에러메시지
    private String orderTime; // 주문시간 (문자열)
} 