package trade.project.api.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 해외 주식 시세 조회 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForeignStockPriceRequest {
    
    @NotBlank(message = "종목코드는 필수입니다")
    @Pattern(regexp = "^[A-Z]{1,5}$", message = "종목코드는 1-5자리 영문 대문자여야 합니다")
    private String stockCode;
    
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "시작일은 YYYY-MM-DD 형식이어야 합니다")
    private String startDate;
    
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "종료일은 YYYY-MM-DD 형식이어야 합니다")
    private String endDate;
    
    private String currency = "USD";
    private String exchange = "NASDAQ";
} 