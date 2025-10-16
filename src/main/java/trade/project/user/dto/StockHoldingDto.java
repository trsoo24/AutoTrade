package trade.project.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockHoldingDto {
    private String stockCode;
    private String stockName;
    private int quantity;
    private BigDecimal avgPrice;
    private BigDecimal currentPrice;
    private BigDecimal evaluationAmount;
    private BigDecimal profitLoss;
    private BigDecimal profitRate;
}