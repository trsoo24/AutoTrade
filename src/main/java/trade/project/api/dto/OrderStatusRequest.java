package trade.project.api.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotBlank;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusRequest {
    
    @NotBlank(message = "계좌번호는 필수입니다")
    private String accountNumber;
    
    @NotBlank(message = "주문번호는 필수입니다")
    private String orderNumber;
} 