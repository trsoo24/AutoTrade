package trade.project.api.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockDailyPriceRequest {
    
    @NotBlank(message = "종목코드는 필수입니다")
    @Pattern(regexp = "^[0-9]{6}$", message = "종목코드는 6자리 숫자여야 합니다")
    private String stockCode;
    
    @NotBlank(message = "시작일자는 필수입니다")
    @Pattern(regexp = "^[0-9]{8}$", message = "시작일자는 YYYYMMDD 형식이어야 합니다")
    private String startDate;
    
    @NotBlank(message = "종료일자는 필수입니다")
    @Pattern(regexp = "^[0-9]{8}$", message = "종료일자는 YYYYMMDD 형식이어야 합니다")
    private String endDate;
} 