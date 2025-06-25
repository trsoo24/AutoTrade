package trade.project.backtest.engine;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import trade.project.backtest.dto.*;
import trade.project.backtest.strategy.StrategyFactory;
import trade.project.backtest.strategy.TradingStrategy;

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
    
    /**
     * 백트래킹을 실행합니다.
     * @param request 백트래킹 요청
     * @param stockDataList 주식 데이터 리스트
     * @return 백트래킹 결과
     */
    public BackTestResult runBacktest(BackTestRequest request, List<StockData> stockDataList) {
        log.info("백트래킹 시작: {} - {} to {}", request.getStockCode(), request.getStartDate(), request.getEndDate());
        
        // 전략 가져오기
        TradingStrategy strategy = strategyFactory.getStrategy(request.getStrategy());
        if (strategy == null) {
            throw new IllegalArgumentException("지원하지 않는 전략입니다: " + request.getStrategy());
        }
        
        // 백트래킹 상태 초기화
        BacktestState state = new BacktestState(request);
        
        // 거래 내역과 포트폴리오 히스토리
        List<BackTestResult.TradeRecord> trades = new ArrayList<>();
        List<BackTestResult.PortfolioSnapshot> portfolioHistory = new ArrayList<>();
        
        // 각 날짜별로 백트래킹 실행
        for (int i = 0; i < stockDataList.size(); i++) {
            StockData currentData = stockDataList.get(i);
            List<StockData> historicalData = stockDataList.subList(0, i + 1);
            
            // 기술적 지표 계산
            calculateTechnicalIndicators(currentData, historicalData, request);
            
            // 거래 신호 생성
            String signal = strategy.generateSignal(currentData, historicalData, request);
            currentData.setSignal(signal);
            
            // 거래 실행
            executeTrade(state, currentData, signal, trades);
            
            // 포트폴리오 스냅샷 생성
            createPortfolioSnapshot(state, currentData, portfolioHistory);
        }
        
        // 백트래킹 결과 계산
        BackTestResult result = calculateBackTestResult(request, state, trades, portfolioHistory);
        
        log.info("백트래킹 완료: 총 수익률 = {}%, 총 거래 횟수 = {}", 
                result.getTotalReturnPercent(), result.getTotalTrades());
        
        return result;
    }
    
    /**
     * 기술적 지표를 계산합니다.
     */
    private void calculateTechnicalIndicators(StockData currentData, List<StockData> historicalData, BackTestRequest request) {
        // 이동평균선 계산
        currentData.calculateSMA(historicalData, 5);
        currentData.calculateSMA(historicalData, 10);
        currentData.calculateSMA(historicalData, 20);
        currentData.calculateSMA(historicalData, 50);
        currentData.calculateSMA(historicalData, 200);
        
        // RSI 계산
        currentData.calculateRSI(historicalData, request.getRsiPeriod() != null ? request.getRsiPeriod() : 14);
        
        // MACD 계산
        currentData.calculateMACD(historicalData, 
                request.getMacdFastPeriod() != null ? request.getMacdFastPeriod() : 12,
                request.getMacdSlowPeriod() != null ? request.getMacdSlowPeriod() : 26,
                request.getMacdSignalPeriod() != null ? request.getMacdSignalPeriod() : 9);
        
        // 볼린저 밴드 계산
        currentData.calculateBollingerBands(historicalData, 20, 2.0);
        
        // 일일 수익률 계산
        if (historicalData.size() > 1) {
            currentData.calculateDailyReturn(historicalData.get(historicalData.size() - 2));
        }
    }
    
    /**
     * 거래를 실행합니다.
     */
    private void executeTrade(BacktestState state, StockData currentData, String signal, List<BackTestResult.TradeRecord> trades) {
        BigDecimal currentPrice = currentData.getClose();
        BigDecimal commission = state.getRequest().getCommission();
        
        if ("BUY".equals(signal) && state.getPosition().compareTo(BigDecimal.ZERO) == 0) {
            // 매수 실행
            BigDecimal availableCapital = state.getCapital().multiply(state.getRequest().getMaxPositionSize());
            BigDecimal maxQuantity = availableCapital.divide(currentPrice, 0, RoundingMode.DOWN);
            
            if (maxQuantity.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal tradeAmount = maxQuantity.multiply(currentPrice);
                BigDecimal commissionAmount = tradeAmount.multiply(commission);
                BigDecimal totalCost = tradeAmount.add(commissionAmount);
                
                if (totalCost.compareTo(state.getCapital()) <= 0) {
                    state.setPosition(maxQuantity);
                    state.setCapital(state.getCapital().subtract(totalCost));
                    
                    trades.add(BackTestResult.TradeRecord.builder()
                            .date(currentData.getDate())
                            .action("BUY")
                            .price(currentPrice)
                            .quantity(maxQuantity.intValue())
                            .amount(tradeAmount)
                            .commission(commissionAmount)
                            .balance(state.getCapital())
                            .position(state.getPosition())
                            .reason(signal)
                            .build());
                    
                    log.debug("매수 실행: {}주 @ {}, 수수료: {}", maxQuantity, currentPrice, commissionAmount);
                }
            }
        } else if ("SELL".equals(signal) && state.getPosition().compareTo(BigDecimal.ZERO) > 0) {
            // 매도 실행
            BigDecimal tradeAmount = state.getPosition().multiply(currentPrice);
            BigDecimal commissionAmount = tradeAmount.multiply(commission);
            BigDecimal netAmount = tradeAmount.subtract(commissionAmount);
            
            state.setCapital(state.getCapital().add(netAmount));
            state.setPosition(BigDecimal.ZERO);
            
            trades.add(BackTestResult.TradeRecord.builder()
                    .date(currentData.getDate())
                    .action("SELL")
                    .price(currentPrice)
                    .quantity(state.getPosition().intValue())
                    .amount(tradeAmount)
                    .commission(commissionAmount)
                    .balance(state.getCapital())
                    .position(BigDecimal.ZERO)
                    .reason(signal)
                    .build());
            
            log.debug("매도 실행: {}주 @ {}, 수수료: {}", state.getPosition(), currentPrice, commissionAmount);
        }
    }
    
    /**
     * 포트폴리오 스냅샷을 생성합니다.
     */
    private void createPortfolioSnapshot(BacktestState state, StockData currentData, List<BackTestResult.PortfolioSnapshot> portfolioHistory) {
        BigDecimal positionValue = state.getPosition().multiply(currentData.getClose());
        BigDecimal totalValue = state.getCapital().add(positionValue);
        BigDecimal returnPercent = totalValue.subtract(state.getRequest().getInitialCapital())
                .divide(state.getRequest().getInitialCapital(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        
        // 최대 자본금 업데이트
        if (totalValue.compareTo(state.getPeakCapital()) > 0) {
            state.setPeakCapital(totalValue);
            state.setPeakDate(currentData.getDate());
        }
        
        // 최대 낙폭 계산
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
    
    /**
     * 백트래킹 결과를 계산합니다.
     */
    private BackTestResult calculateBackTestResult(BackTestRequest request, BacktestState state, 
                                                 List<BackTestResult.TradeRecord> trades, 
                                                 List<BackTestResult.PortfolioSnapshot> portfolioHistory) {
        
        // 최종 포트폴리오 가치 계산 (보유 주식이 있다면 현재가로 평가)
        BigDecimal finalCapital = state.getCapital();
        if (state.getPosition().compareTo(BigDecimal.ZERO) > 0 && !portfolioHistory.isEmpty()) {
            BigDecimal lastPrice = portfolioHistory.get(portfolioHistory.size() - 1).getPosition()
                    .divide(state.getPosition(), 2, RoundingMode.HALF_UP);
            finalCapital = finalCapital.add(state.getPosition().multiply(lastPrice));
        }
        
        BigDecimal totalReturn = finalCapital.subtract(request.getInitialCapital());
        BigDecimal totalReturnPercent = totalReturn.divide(request.getInitialCapital(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        
        // 연간 수익률 계산
        long days = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate());
        BigDecimal annualizedReturn = BigDecimal.ZERO;
        if (days > 0) {
            double years = days / 365.0;
            annualizedReturn = BigDecimal.valueOf(Math.pow(1 + totalReturnPercent.doubleValue() / 100, 1 / years) - 1)
                    .multiply(BigDecimal.valueOf(100));
        }
        
        // 거래 통계 계산
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
        
        // 샤프 비율 계산 (간단한 버전)
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
    
    /**
     * 변동성을 계산합니다.
     */
    private BigDecimal calculateVolatility(List<BackTestResult.PortfolioSnapshot> portfolioHistory) {
        if (portfolioHistory.size() < 2) return BigDecimal.ZERO;
        
        List<BigDecimal> returns = new ArrayList<>();
        for (int i = 1; i < portfolioHistory.size(); i++) {
            BigDecimal prevValue = portfolioHistory.get(i - 1).getTotalValue();
            BigDecimal currValue = portfolioHistory.get(i).getTotalValue();
            BigDecimal dailyReturn = currValue.subtract(prevValue).divide(prevValue, 4, RoundingMode.HALF_UP);
            returns.add(dailyReturn);
        }
        
        // 평균 수익률 계산
        BigDecimal avgReturn = returns.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(returns.size()), 4, RoundingMode.HALF_UP);
        
        // 분산 계산
        BigDecimal variance = returns.stream()
                .map(r -> r.subtract(avgReturn).pow(2))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(returns.size()), 4, RoundingMode.HALF_UP);
        
        // 표준편차 계산
        double stdDev = Math.sqrt(variance.doubleValue());
        return BigDecimal.valueOf(stdDev).multiply(BigDecimal.valueOf(Math.sqrt(252))); // 연간화
    }
    
    /**
     * 백트래킹 상태를 관리하는 내부 클래스
     */
    private static class BacktestState {
        private final BackTestRequest request;
        private BigDecimal capital;
        private BigDecimal position;
        private BigDecimal peakCapital;
        private LocalDate peakDate;
        private BigDecimal maxDrawdown;
        private BigDecimal maxDrawdownPercent;
        private LocalDate maxDrawdownDate;
        
        public BacktestState(BackTestRequest request) {
            this.request = request;
            this.capital = request.getInitialCapital();
            this.position = BigDecimal.ZERO;
            this.peakCapital = request.getInitialCapital();
            this.peakDate = request.getStartDate();
            this.maxDrawdown = BigDecimal.ZERO;
            this.maxDrawdownPercent = BigDecimal.ZERO;
            this.maxDrawdownDate = request.getStartDate();
        }
        
        // Getters and Setters
        public BackTestRequest getRequest() { return request; }
        public BigDecimal getCapital() { return capital; }
        public void setCapital(BigDecimal capital) { this.capital = capital; }
        public BigDecimal getPosition() { return position; }
        public void setPosition(BigDecimal position) { this.position = position; }
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