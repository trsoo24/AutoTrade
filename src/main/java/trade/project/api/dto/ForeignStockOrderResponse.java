package trade.project.api.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 해외 주식 주문 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForeignStockOrderResponse {
    
    private String orderNumber;
    private String accountNumber;
    private String stockCode;
    private String companyName;
    private String koreanName;
    private String orderType;
    private Integer quantity;
    private BigDecimal price;
    private String priceType;
    private String orderCategory;
    private String orderStatus;
    private LocalDateTime orderTime;
    private LocalDateTime executedTime;
    private Integer executedQuantity;
    private BigDecimal executedPrice;
    private BigDecimal executedAmount;
    private String currency;
    private String exchange;
    private String errorMessage;
} 