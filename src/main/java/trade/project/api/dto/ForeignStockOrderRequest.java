package trade.project.api.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

/**
 * 해외 주식 주문 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForeignStockOrderRequest {
    
    @NotBlank(message = "계좌번호는 필수입니다")
    @Pattern(regexp = "^[A-Z0-9]{8,12}$", message = "계좌번호는 8-12자리 영문자와 숫자 조합이어야 합니다")
    private String accountNumber;
    
    @NotBlank(message = "종목코드는 필수입니다")
    @Pattern(regexp = "^[A-Z]{1,5}$", message = "종목코드는 1-5자리 영문 대문자여야 합니다")
    private String stockCode;
    
    @NotBlank(message = "주문구분은 필수입니다")
    @Pattern(regexp = "^(매수|매도)$", message = "주문구분은 '매수' 또는 '매도'여야 합니다")
    private String orderType;
    
    @NotNull(message = "주문수량은 필수입니다")
    @Positive(message = "주문수량은 양수여야 합니다")
    private Integer quantity;
    
    @NotNull(message = "주문가격은 필수입니다")
    @Positive(message = "주문가격은 양수여야 합니다")
    private BigDecimal price;
    
    @NotBlank(message = "가격구분은 필수입니다")
    @Pattern(regexp = "^(지정가|시장가)$", message = "가격구분은 '지정가' 또는 '시장가'여야 합니다")
    private String priceType;
    
    @NotBlank(message = "주문구분은 필수입니다")
    @Pattern(regexp = "^(일반|신용|대출)$", message = "주문구분은 '일반', '신용', '대출' 중 하나여야 합니다")
    private String orderCategory;
    
    private String currency = "USD";
    private String exchange = "NASDAQ";
} 