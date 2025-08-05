package trade.project.trading.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "trading_records")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradingRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "order_number", nullable = false, length = 20)
    private String orderNumber; // 주문번호
    
    @Column(name = "account_number", nullable = false, length = 20)
    private String accountNumber; // 계좌번호
    
    @Column(name = "stock_code", nullable = false, length = 6)
    private String stockCode; // 종목코드
    
    @Column(name = "stock_name", length = 50)
    private String stockName; // 종목명
    
    @Column(name = "order_type", nullable = false, length = 10)
    private String orderType; // 주문구분 (매수/매도)
    
    @Column(name = "quantity", nullable = false)
    private Integer quantity; // 주문수량
    
    @Column(name = "price", nullable = false)
    private Integer price; // 주문가격
    
    @Column(name = "price_type", length = 10)
    private String priceType; // 주문유형 (지정가/시장가)
    
    @Column(name = "order_status", length = 20)
    private String orderStatus; // 주문상태
    
    @Column(name = "order_category", length = 10)
    private String orderCategory; // 주문구분 (일반/정정/취소)
    
    @Column(name = "original_order_number", length = 20)
    private String originalOrderNumber; // 정정/취소 시 원주문번호
    
    @Column(name = "executed_quantity")
    private Integer executedQuantity; // 체결수량
    
    @Column(name = "executed_price")
    private Integer executedPrice; // 체결가격
    
    @Column(name = "execution_time")
    private LocalDateTime executionTime; // 체결시간
    
    @Column(name = "total_amount")
    private Long totalAmount; // 총 거래금액
    
    @Column(name = "commission")
    private Integer commission; // 수수료
    
    @Column(name = "order_datetime", nullable = false)
    private LocalDateTime orderDateTime; // 주문일시
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt; // 생성일시
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt; // 수정일시
    
    @Column(name = "error_code", length = 10)
    private String errorCode; // 에러코드
    
    @Column(name = "error_message", length = 500)
    private String errorMessage; // 에러메시지
    
    @Column(name = "api_response", columnDefinition = "TEXT")
    private String apiResponse; // API 응답 전체 JSON
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        orderDateTime = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
} 