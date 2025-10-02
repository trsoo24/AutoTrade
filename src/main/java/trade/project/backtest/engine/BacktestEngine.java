package trade.project.backtest.engine;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import trade.project.backtest.dto.*;
import trade.project.backtest.strategy.StrategyFactory;
import trade.project.backtest.strategy.TradingStrategy;
import trade.project.backtest.util.TechnicalIndicatorCalculator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BacktestEngine {

    private final StrategyFactory strategyFactory;

    // === 추가: 슬리피지 상수 ===
    private static final BigDecimal DEFAULT_SLIPPAGE = new BigDecimal("0.001"); // 0.1%

    public BackTestResult runBacktest(BackTestRequest request, List<StockData> stockDataList) {
        if (request == null) {
            throw new IllegalArgumentException("백트래킹 요청이 null입니다");
        }

        if (stockDataList == null || stockDataList.isEmpty()) {
            throw new IllegalArgumentException("주식 데이터가 비어있습니다");
        }

        log.info("백트래킹 시작: {} - {} to {}", request.getStockCode(), request.getStartDate(), request.getEndDate());

        TradingStrategy strategy = strategyFactory.getStrategy(request.getStrategy());
        if (strategy == null) {
            throw new IllegalArgumentException("지원하지 않는 전략입니다: " + request.getStrategy());
        }

        BacktestState state = new BacktestState(request);

        List<BackTestResult.TradeRecord> trades = new ArrayList<>();
        List<BackTestResult.PortfolioSnapshot> portfolioHistory = new ArrayList<>();

        for (int i = 0; i < stockDataList.size(); i++) {
            StockData currentData = stockDataList.get(i);
            List<StockData> historicalData = stockDataList.subList(0, i + 1);

            calculateTechnicalIndicators(currentData, historicalData, request);

            // === 추가: 손절/익절 체크 (포지션이 있을 때만) ===
            if (state.getPosition().compareTo(BigDecimal.ZERO) > 0) {
                boolean forcedExit = checkStopLossAndTakeProfit(state, currentData, trades);
                if (forcedExit) {
                    // 손절/익절이 실행되면 다음 루프로
                    createPortfolioSnapshot(state, currentData, portfolioHistory);
                    continue;
                }
            }

            String signal = strategy.generateSignal(currentData, historicalData, request);
            currentData.setSignal(signal);

            executeTrade(state, currentData, signal, trades);

            createPortfolioSnapshot(state, currentData, portfolioHistory);
        }

        BackTestResult result = calculateBackTestResult(request, state, trades, portfolioHistory);

        log.info("백트래킹 완료: 이 수익률 = {}%, 이 거래 횟수 = {}",
                result.getTotalReturnPercent(), result.getTotalTrades());

        return result;
    }

    // === 추가: 손절/익절 체크 메서드 ===
    /**
     * 손절/익절 조건을 체크하고 필요시 강제 청산합니다.
     * @return true: 청산 실행됨, false: 청산 안됨
     */
    private boolean checkStopLossAndTakeProfit(BacktestState state, StockData currentData,
                                               List<BackTestResult.TradeRecord> trades) {
        if (state.getPosition().compareTo(BigDecimal.ZERO) == 0 || state.getEntryPrice() == null) {
            return false;
        }

        BigDecimal entryPrice = state.getEntryPrice();
        BigDecimal currentPrice = currentData.getClose();
        BigDecimal returnRate = currentPrice.subtract(entryPrice)
                .divide(entryPrice, 6, RoundingMode.HALF_UP);

        // 손절 체크
        if (state.getRequest().getStopLoss() != null) {
            BigDecimal stopLossThreshold = state.getRequest().getStopLoss().negate();

            if (returnRate.compareTo(stopLossThreshold) <= 0) {
                log.warn(">>> 손절 실행: 손실률={}%",
                        returnRate.multiply(new BigDecimal("100")));
                forceCloseTrade(state, currentData, "STOP_LOSS", trades);
                return true;
            }
        }

        // 익절 체크
        if (state.getRequest().getTakeProfit() != null) {
            BigDecimal takeProfitThreshold = state.getRequest().getTakeProfit();

            if (returnRate.compareTo(takeProfitThreshold) >= 0) {
                log.info(">>> 익절 실행: 수익률={}%",
                        returnRate.multiply(new BigDecimal("100")));
                forceCloseTrade(state, currentData, "TAKE_PROFIT", trades);
                return true;
            }
        }

        return false;
    }

    // === 추가: 강제 청산 메서드 ===
    /**
     * 손절/익절 시 포지션을 강제로 청산합니다.
     */
    private void forceCloseTrade(BacktestState state, StockData currentData, String reason,
                                 List<BackTestResult.TradeRecord> trades) {
        if (state.getPosition().compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        // === 수정: 슬리피지 적용된 가격으로 매도 ===
        BigDecimal sellPrice = applySlippage(currentData.getClose(), false);

        BigDecimal tradeAmount = state.getPosition().multiply(sellPrice);
        BigDecimal commissionAmount = tradeAmount.multiply(state.getRequest().getCommission());
        BigDecimal netAmount = tradeAmount.subtract(commissionAmount);

        BigDecimal positionBeforeSell = state.getPosition();

        state.setCapital(state.getCapital().add(netAmount));
        state.setPosition(BigDecimal.ZERO);
        state.setEntryPrice(null); // 진입가 초기화

        trades.add(BackTestResult.TradeRecord.builder()
                .date(currentData.getDate())
                .action("SELL")
                .price(sellPrice)
                .quantity(positionBeforeSell.intValue())
                .amount(tradeAmount)
                .commission(commissionAmount)
                .balance(state.getCapital())
                .position(BigDecimal.ZERO)
                .reason(reason)
                .build());

        log.debug("강제 매도 완료: {}주 @ {}, 수수료: {}, 사유: {}",
                positionBeforeSell, sellPrice, commissionAmount, reason);
    }

    // === 추가: 슬리피지 적용 메서드 ===
    /**
     * 매수/매도 시 슬리피지를 적용합니다.
     * @param price 원래 가격
     * @param isBuy true: 매수(가격 상승), false: 매도(가격 하락)
     * @return 슬리피지가 적용된 가격
     */
    private BigDecimal applySlippage(BigDecimal price, boolean isBuy) {
        BigDecimal slippageMultiplier = isBuy ?
                BigDecimal.ONE.add(DEFAULT_SLIPPAGE) :  // 매수: 0.1% 높게
                BigDecimal.ONE.subtract(DEFAULT_SLIPPAGE);  // 매도: 0.1% 낮게

        return price.multiply(slippageMultiplier).setScale(2, RoundingMode.HALF_UP);
    }

    private void calculateTechnicalIndicators(StockData currentData, List<StockData> historicalData, BackTestRequest request) {
        currentData.setSma5(TechnicalIndicatorCalculator.calculateSMA(historicalData, 5));
        currentData.setSma10(TechnicalIndicatorCalculator.calculateSMA(historicalData, 10));
        currentData.setSma20(TechnicalIndicatorCalculator.calculateSMA(historicalData, 20));
        currentData.setSma50(TechnicalIndicatorCalculator.calculateSMA(historicalData, 50));
        currentData.setSma200(TechnicalIndicatorCalculator.calculateSMA(historicalData, 200));

        currentData.setRsi(TechnicalIndicatorCalculator.calculateRSI(historicalData,
                request.getRsiPeriod() != null ? request.getRsiPeriod() : 14));

        TechnicalIndicatorCalculator.MACDResult macdResult = TechnicalIndicatorCalculator.calculateMACD(historicalData,
                request.getMacdFastPeriod() != null ? request.getMacdFastPeriod() : 12,
                request.getMacdSlowPeriod() != null ? request.getMacdSlowPeriod() : 26,
                request.getMacdSignalPeriod() != null ? request.getMacdSignalPeriod() : 9);

        if (macdResult != null) {
            currentData.setEma12(macdResult.getEma12());
            currentData.setEma26(macdResult.getEma26());
            currentData.setMacd(macdResult.getMacd());
            currentData.setMacdSignal(macdResult.getMacdSignal());
            currentData.setMacdHistogram(macdResult.getMacdHistogram());
        }

        TechnicalIndicatorCalculator.BollingerBandsResult bbResult = TechnicalIndicatorCalculator.calculateBollingerBands(historicalData, 20, 2.0);
        if (bbResult != null) {
            currentData.setBollingerUpper(bbResult.getUpper());
            currentData.setBollingerMiddle(bbResult.getMiddle());
            currentData.setBollingerLower(bbResult.getLower());
        }

        if (historicalData.size() > 1) {
            currentData.setDailyReturn(TechnicalIndicatorCalculator.calculateDailyReturn(currentData, historicalData.get(historicalData.size() - 2)));
        }
    }

    // === 수정: executeTrade 메서드에 슬리피지 적용 ===
    private void executeTrade(BacktestState state, StockData currentData, String signal, List<BackTestResult.TradeRecord> trades) {
        BigDecimal commission = state.getRequest().getCommission();

        if ("BUY".equals(signal) && state.getPosition().compareTo(BigDecimal.ZERO) == 0) {
            // === 수정: 슬리피지 적용된 가격으로 매수 ===
            BigDecimal buyPrice = applySlippage(currentData.getClose(), true);

            BigDecimal availableCapital = state.getCapital().multiply(state.getRequest().getMaxPositionSize());
            BigDecimal maxQuantity = availableCapital.divide(buyPrice, 0, RoundingMode.DOWN);

            if (maxQuantity.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal tradeAmount = maxQuantity.multiply(buyPrice);
                BigDecimal commissionAmount = tradeAmount.multiply(commission);
                BigDecimal totalCost = tradeAmount.add(commissionAmount);

                if (totalCost.compareTo(state.getCapital()) <= 0) {
                    state.setPosition(maxQuantity);
                    state.setCapital(state.getCapital().subtract(totalCost));
                    // === 추가: 진입가 저장 ===
                    state.setEntryPrice(buyPrice);

                    trades.add(BackTestResult.TradeRecord.builder()
                            .date(currentData.getDate())
                            .action("BUY")
                            .price(buyPrice)
                            .quantity(maxQuantity.intValue())
                            .amount(tradeAmount)
                            .commission(commissionAmount)
                            .balance(state.getCapital())
                            .position(state.getPosition())
                            .reason(signal)
                            .build());

                    log.debug("매수 실행: {}주 @ {} (슬리피지 적용), 수수료: {}", maxQuantity, buyPrice, commissionAmount);
                }
            }
        } else if ("SELL".equals(signal) && state.getPosition().compareTo(BigDecimal.ZERO) > 0) {
            // === 수정: 슬리피지 적용된 가격으로 매도 ===
            BigDecimal sellPrice = applySlippage(currentData.getClose(), false);

            BigDecimal tradeAmount = state.getPosition().multiply(sellPrice);
            BigDecimal commissionAmount = tradeAmount.multiply(commission);
            BigDecimal netAmount = tradeAmount.subtract(commissionAmount);

            BigDecimal positionBeforeSell = state.getPosition();

            state.setCapital(state.getCapital().add(netAmount));
            state.setPosition(BigDecimal.ZERO);
            // === 추가: 진입가 초기화 ===
            state.setEntryPrice(null);

            trades.add(BackTestResult.TradeRecord.builder()
                    .date(currentData.getDate())
                    .action("SELL")
                    .price(sellPrice)
                    .quantity(positionBeforeSell.intValue())
                    .amount(tradeAmount)
                    .commission(commissionAmount)
                    .balance(state.getCapital())
                    .position(BigDecimal.ZERO)
                    .reason(signal)
                    .build());

            log.debug("매도 실행: {}주 @ {} (슬리피지 적용), 수수료: {}", positionBeforeSell, sellPrice, commissionAmount);
        }
    }

    private void createPortfolioSnapshot(BacktestState state, StockData currentData, List<BackTestResult.PortfolioSnapshot> portfolioHistory) {
        BigDecimal positionValue = state.getPosition().multiply(currentData.getClose());
        BigDecimal totalValue = state.getCapital().add(positionValue);
        BigDecimal returnPercent = totalValue.subtract(state.getRequest().getInitialCapital())
                .divide(state.getRequest().getInitialCapital(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        if (totalValue.compareTo(state.getPeakCapital()) > 0) {
            state.setPeakCapital(totalValue);
            state.setPeakDate(currentData.getDate());
        }

        BigDecimal drawdown = state.getPeakCapital().subtract(totalValue);
        BigDecimal drawdownPercent = drawdown.divide(state.getPeakCapital(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        if (drawdown.compareTo(state.getMaxDrawdown()) > 0) {
            state.setMaxDrawdown(drawdown);
            state.setMaxDrawdownPercent(drawdownPercent);
            state.setMaxDrawdownDate(currentData.getDate());
        }

        portfolioHistory.add(BackTestResult.PortfolioSnapshot.builder()
                .date(currentData.getDate())
                .capital(state.getCapital())
                .position(positionValue)
                .totalValue(totalValue)
                .returnPercent(returnPercent)
                .drawdown(drawdown)
                .drawdownPercent(drawdownPercent)
                .build());
    }

    private BackTestResult calculateBackTestResult(BackTestRequest request, BacktestState state,
                                                   List<BackTestResult.TradeRecord> trades,
                                                   List<BackTestResult.PortfolioSnapshot> portfolioHistory) {

        BigDecimal finalCapital = state.getCapital();
        if (state.getPosition().compareTo(BigDecimal.ZERO) > 0 && !portfolioHistory.isEmpty()) {
            BigDecimal lastPrice = portfolioHistory.get(portfolioHistory.size() - 1).getPosition()
                    .divide(state.getPosition(), 2, RoundingMode.HALF_UP);
            finalCapital = finalCapital.add(state.getPosition().multiply(lastPrice));
        }

        BigDecimal totalReturn = finalCapital.subtract(request.getInitialCapital());
        BigDecimal totalReturnPercent = totalReturn.divide(request.getInitialCapital(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        long days = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate());
        BigDecimal annualizedReturn = BigDecimal.ZERO;
        if (days > 0) {
            double years = days / 365.0;
            annualizedReturn = BigDecimal.valueOf(Math.pow(1 + totalReturnPercent.doubleValue() / 100, 1 / years) - 1)
                    .multiply(BigDecimal.valueOf(100));
        }

        int totalTrades = trades.size();
        int winningTrades = 0;
        BigDecimal totalWin = BigDecimal.ZERO;
        BigDecimal totalLoss = BigDecimal.ZERO;

        for (int i = 0; i < trades.size(); i += 2) {
            if (i + 1 < trades.size()) {
                BackTestResult.TradeRecord buy = trades.get(i);
                BackTestResult.TradeRecord sell = trades.get(i + 1);

                BigDecimal profit = sell.getAmount().subtract(buy.getAmount())
                        .subtract(buy.getCommission())
                        .subtract(sell.getCommission());

                if (profit.compareTo(BigDecimal.ZERO) > 0) {
                    winningTrades++;
                    totalWin = totalWin.add(profit);
                } else {
                    totalLoss = totalLoss.add(profit.abs());
                }
            }
        }

        BigDecimal winRate = totalTrades > 0 ?
                BigDecimal.valueOf(winningTrades).divide(BigDecimal.valueOf(totalTrades), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)) : BigDecimal.ZERO;

        BigDecimal averageWin = winningTrades > 0 ?
                totalWin.divide(BigDecimal.valueOf(winningTrades), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        BigDecimal averageLoss = (totalTrades - winningTrades) > 0 ?
                totalLoss.divide(BigDecimal.valueOf(totalTrades - winningTrades), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        BigDecimal profitFactor = totalLoss.compareTo(BigDecimal.ZERO) > 0 ?
                totalWin.divide(totalLoss, 4, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        BigDecimal volatility = calculateVolatility(portfolioHistory);
        BigDecimal sharpeRatio = volatility.compareTo(BigDecimal.ZERO) > 0 ?
                annualizedReturn.divide(volatility, 4, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        return BackTestResult.builder()
                .stockCode(request.getStockCode())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .strategy(request.getStrategy())
                .initialCapital(request.getInitialCapital())
                .finalCapital(finalCapital)
                .totalReturn(totalReturn)
                .totalReturnPercent(totalReturnPercent)
                .annualizedReturn(annualizedReturn)
                .maxDrawdown(state.getMaxDrawdown())
                .maxDrawdownPercent(state.getMaxDrawdownPercent())
                .sharpeRatio(sharpeRatio)
                .volatility(volatility)
                .totalTrades(totalTrades)
                .winningTrades(winningTrades)
                .losingTrades(totalTrades - winningTrades)
                .winRate(winRate)
                .averageWin(averageWin)
                .averageLoss(averageLoss)
                .profitFactor(profitFactor)
                .peakCapital(state.getPeakCapital())
                .peakDate(state.getPeakDate())
                .maxDrawdownDate(state.getMaxDrawdownDate())
                .trades(trades)
                .portfolioHistory(portfolioHistory)
                .build();
    }

    private BigDecimal calculateVolatility(List<BackTestResult.PortfolioSnapshot> portfolioHistory) {
        if (portfolioHistory.size() < 2) return BigDecimal.ZERO;

        List<BigDecimal> returns = new ArrayList<>();
        for (int i = 1; i < portfolioHistory.size(); i++) {
            BigDecimal prevValue = portfolioHistory.get(i - 1).getTotalValue();
            BigDecimal currValue = portfolioHistory.get(i).getTotalValue();
            BigDecimal dailyReturn = currValue.subtract(prevValue).divide(prevValue, 4, RoundingMode.HALF_UP);
            returns.add(dailyReturn);
        }

        BigDecimal avgReturn = returns.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(returns.size()), 4, RoundingMode.HALF_UP);

        BigDecimal variance = returns.stream()
                .map(r -> r.subtract(avgReturn).pow(2))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(returns.size()), 4, RoundingMode.HALF_UP);

        double stdDev = Math.sqrt(variance.doubleValue());
        return BigDecimal.valueOf(stdDev).multiply(BigDecimal.valueOf(Math.sqrt(252)));
    }

    // === 수정: BacktestState 클래스에 entryPrice 필드 추가 ===
    private static class BacktestState {
        private final BackTestRequest request;
        private BigDecimal capital;
        private BigDecimal position;
        private BigDecimal entryPrice;  // 추가
        private BigDecimal peakCapital;
        private LocalDate peakDate;
        private BigDecimal maxDrawdown;
        private BigDecimal maxDrawdownPercent;
        private LocalDate maxDrawdownDate;

        public BacktestState(BackTestRequest request) {
            this.request = request;
            this.capital = request.getInitialCapital();
            this.position = BigDecimal.ZERO;
            this.entryPrice = null;  // 추가
            this.peakCapital = request.getInitialCapital();
            this.peakDate = request.getStartDate();
            this.maxDrawdown = BigDecimal.ZERO;
            this.maxDrawdownPercent = BigDecimal.ZERO;
            this.maxDrawdownDate = request.getStartDate();
        }

        public BackTestRequest getRequest() { return request; }
        public BigDecimal getCapital() { return capital; }
        public void setCapital(BigDecimal capital) { this.capital = capital; }
        public BigDecimal getPosition() { return position; }
        public void setPosition(BigDecimal position) { this.position = position; }
        public BigDecimal getEntryPrice() { return entryPrice; }  // 추가
        public void setEntryPrice(BigDecimal entryPrice) { this.entryPrice = entryPrice; }  // 추가
        public BigDecimal getPeakCapital() { return peakCapital; }
        public void setPeakCapital(BigDecimal peakCapital) { this.peakCapital = peakCapital; }
        public LocalDate getPeakDate() { return peakDate; }
        public void setPeakDate(LocalDate peakDate) { this.peakDate = peakDate; }
        public BigDecimal getMaxDrawdown() { return maxDrawdown; }
        public void setMaxDrawdown(BigDecimal maxDrawdown) { this.maxDrawdown = maxDrawdown; }
        public BigDecimal getMaxDrawdownPercent() { return maxDrawdownPercent; }
        public void setMaxDrawdownPercent(BigDecimal maxDrawdownPercent) { this.maxDrawdownPercent = maxDrawdownPercent; }
        public LocalDate getMaxDrawdownDate() { return maxDrawdownDate; }
        public void setMaxDrawdownDate(LocalDate maxDrawdownDate) { this.maxDrawdownDate = maxDrawdownDate; }
    }
}