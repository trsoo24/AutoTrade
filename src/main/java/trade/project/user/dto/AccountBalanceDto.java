package trade.project.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountBalanceDto {
    private String accountNumber;
    private BigDecimal totalBalance;      // 총자산
    private BigDecimal cashBalance;       // 예수금
    private BigDecimal availableAmount;   // 매수가능금액
    private BigDecimal stockValue;        // 주식평가액
    private BigDecimal profitLoss;        // 총손익
    private BigDecimal profitRate;        // 손익률(%)
}
