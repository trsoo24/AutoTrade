package trade.project.api.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockOrderRequest {
    
    @NotBlank(message = "계좌번호는 필수입니다")
    private String accountNumber;
    
    @NotBlank(message = "종목코드는 필수입니다")
    @Pattern(regexp = "^[0-9]{6}$", message = "종목코드는 6자리 숫자여야 합니다")
    private String stockCode;
    
    @NotBlank(message = "주문구분은 필수입니다")
    @Pattern(regexp = "^(매수|매도)$", message = "주문구분은 '매수' 또는 '매도'여야 합니다")
    private String orderType; // 매수, 매도
    
    @NotNull(message = "주문수량은 필수입니다")
    @Positive(message = "주문수량은 양수여야 합니다")
    private Integer quantity;
    
    @NotNull(message = "주문가격은 필수입니다")
    @Positive(message = "주문가격은 양수여야 합니다")
    private Integer price;
    
    @Pattern(regexp = "^(지정가|시장가)$", message = "주문유형은 '지정가' 또는 '시장가'여야 합니다")
    @Builder.Default
    private String priceType = "지정가"; // 지정가, 시장가
    
    @Pattern(regexp = "^(일반|정정|취소)$", message = "주문구분은 '일반', '정정', '취소' 중 하나여야 합니다")
    @Builder.Default
    private String orderCategory = "일반"; // 일반, 정정, 취소
    
    private String originalOrderNumber; // 정정/취소 시 원주문번호
} 