package trade.project.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import trade.project.api.client.KisApiClient;
import trade.project.user.dto.AccountBalanceDto;
import trade.project.user.dto.StockHoldingDto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountService {

    private final KisApiClient kisApiService;  // 기존 서비스 활용

    /**
     * 계좌 잔액 및 매수 가능 금액 조회
     */
    public AccountBalanceDto getAccountBalance() {
        try {
            // 한국투자증권 API 호출
            Map<String, Object> response = kisApiService.getAccountBalance();

            if (response == null || !response.containsKey("output2")) {
                log.error("계좌 잔액 응답 형식 오류");
                return null;
            }

            // output2 파싱 (계좌 잔고 정보)
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> output2 = (List<Map<String, Object>>) response.get("output2");

            if (output2 == null || output2.isEmpty()) {
                log.error("계좌 잔고 정보 없음");
                return null;
            }

            // 첫 번째 계좌 정보 사용 (보통 종합계좌)
            Map<String, Object> balanceInfo = output2.get(0);

            // 금액 정보 추출 및 변환
            String accountNumber = String.valueOf(balanceInfo.get("ACC_NUM"));
            BigDecimal totalAssets = parseAmount(balanceInfo.get("tot_evlu_amt"));      // 총평가금액
            BigDecimal cashBalance = parseAmount(balanceInfo.get("dnca_tot_amt"));      // 예수금총액
            BigDecimal buyingPower = parseAmount(balanceInfo.get("ord_psbl_cash"));     // 주문가능현금
            BigDecimal stockValue = parseAmount(balanceInfo.get("scts_evlu_amt"));      // 유가평가금액

            // D+2 예수금 체크 (실제 사용 가능 금액)
            BigDecimal availableAmount = buyingPower;
            if (balanceInfo.containsKey("d2_auto_rdpt_amt")) {
                BigDecimal d2Amount = parseAmount(balanceInfo.get("d2_auto_rdpt_amt"));
                if (d2Amount.compareTo(BigDecimal.ZERO) > 0) {
                    availableAmount = availableAmount.min(d2Amount);
                }
            }

            AccountBalanceDto result = AccountBalanceDto.builder()
                    .accountNumber(accountNumber)
                    .totalBalance(totalAssets)        // 총자산 (주식+현금)
                    .cashBalance(cashBalance)         // 현금(예수금)
                    .availableAmount(availableAmount) // 실제 매수가능금액
                    .stockValue(stockValue)          // 보유주식평가액
                    .profitLoss(parseAmount(balanceInfo.get("tot_evlu_pfls_amt"))) // 총손익
                    .profitRate(parseRate(balanceInfo.get("tot_evlu_pfls_rt")))    // 손익률
                    .build();

            log.info("계좌 잔액 조회 성공: 계좌={}, 총자산={}, 매수가능={}",
                    accountNumber, totalAssets, availableAmount);

            return result;

        } catch (Exception e) {
            log.error("계좌 잔액 파싱 실패: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 보유 종목 조회
     */
    public List<StockHoldingDto> getStockHoldings() {
        try {
            Map<String, Object> response = kisApiService.getAccountBalance();

            if (response == null || !response.containsKey("output1")) {
                return new ArrayList<>();
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> output1 = (List<Map<String, Object>>) response.get("output1");

            List<StockHoldingDto> holdings = new ArrayList<>();

            for (Map<String, Object> stock : output1) {
                // 종목코드가 있는 항목만 처리
                String stockCode = (String) stock.get("pdno");
                if (stockCode == null || stockCode.trim().isEmpty()) {
                    continue;
                }

                StockHoldingDto holding = StockHoldingDto.builder()
                        .stockCode(stockCode)
                        .stockName((String) stock.get("prdt_name"))
                        .quantity(parseInt(stock.get("hldg_qty")))           // 보유수량
                        .avgPrice(parseAmount(stock.get("pchs_avg_pric")))  // 평균매수가
                        .currentPrice(parseAmount(stock.get("prpr")))       // 현재가
                        .evaluationAmount(parseAmount(stock.get("evlu_amt"))) // 평가금액
                        .profitLoss(parseAmount(stock.get("evlu_pfls_amt"))) // 평가손익
                        .profitRate(parseRate(stock.get("evlu_pfls_rt")))    // 손익률
                        .build();

                holdings.add(holding);
            }

            return holdings;

        } catch (Exception e) {
            log.error("보유 종목 조회 실패: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 금액 문자열을 BigDecimal로 변환
     */
    private BigDecimal parseAmount(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }

        try {
            String strValue = value.toString().replaceAll("[,\\s]", "");
            return new BigDecimal(strValue);
        } catch (NumberFormatException e) {
            log.warn("금액 변환 실패: {}", value);
            return BigDecimal.ZERO;
        }
    }

    /**
     * 비율 문자열을 BigDecimal로 변환 (% 단위)
     */
    private BigDecimal parseRate(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }

        try {
            String strValue = value.toString().replaceAll("[%,\\s]", "");
            return new BigDecimal(strValue);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * 수량 파싱
     */
    private int parseInt(Object value) {
        if (value == null) {
            return 0;
        }

        try {
            return Integer.parseInt(value.toString().replaceAll("[,\\s]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}