package trade.project.trading.engine;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import trade.project.api.dto.StockPriceRequest;
import trade.project.api.dto.StockOrderRequest;
import trade.project.api.dto.StockOrderResponse;
import trade.project.api.service.StockOrderService;
import trade.project.api.service.StockPriceService;
import trade.project.backtest.dto.BackTestRequest;
import trade.project.backtest.dto.StockData;
import trade.project.backtest.strategy.MACDStrategy;
import trade.project.backtest.strategy.SMAStrategy;
import trade.project.trading.document.PriceQueryRecord;
import trade.project.trading.dto.AutoTradingStrategy;
import trade.project.trading.enums.TopNasdaqStocks;
import trade.project.trading.enums.MarketType;
import trade.project.trading.service.PriceQueryRecordService;
import trade.project.user.dto.AccountBalanceDto;
import trade.project.user.service.AccountService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * 자동매매 엔진
 * 전문적인 트레이딩 관점에서 최적화된 자동매매 시스템
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AutoTradingEngine {
    private final StockPriceService stockPriceService;
    private final StockOrderService stockOrderService;
    private final PriceQueryRecordService priceQueryRecordService; // [1] 과거 가격 데이터 서비스 주입
    private final AccountService accountService; // 계좌 & 잔액 조회
    private final MACDStrategy macdStrategy;
    private final SMAStrategy smaStrategy;

    // 국내/해외 전략, 상태, 스케줄러 분리
    private final Map<String, AutoTradingStrategy> domesticStrategies = new ConcurrentHashMap<>();
    private final Map<String, AutoTradingStrategy> foreignStrategies = new ConcurrentHashMap<>();

    private final Map<String, TradingStatus> domesticStatuses = new ConcurrentHashMap<>();
    private final Map<String, TradingStatus> foreignStatuses = new ConcurrentHashMap<>();

    private ScheduledExecutorService domesticScheduler;
    private ScheduledExecutorService foreignScheduler;

    private final AtomicBoolean domesticRunning = new AtomicBoolean(false);
    private final AtomicBoolean foreignRunning = new AtomicBoolean(false);

    private static class TradingStatus {
        private BigDecimal currentPosition = BigDecimal.ZERO;
        private BigDecimal averagePrice = BigDecimal.ZERO;
        private BigDecimal dailyPnL = BigDecimal.ZERO;
        private int dailyTradeCount = 0;
        private LocalDateTime lastTradeTime;
        private BigDecimal lastPrice;
        private LocalDateTime lastUpdateTime;
        private BigDecimal peakPrice = BigDecimal.ZERO;  // 트레일링 스톱용
        private int winCount = 0;  // 승률 계산용
        private int lossCount = 0;
        private BigDecimal totalProfit = BigDecimal.ZERO;
        private BigDecimal totalLoss = BigDecimal.ZERO;
        private int consecutiveLosses = 0;  // 연속 손실
        private String lastSignal = "HOLD";  // 마지막 신호 추적
        private BigDecimal confidenceScore = BigDecimal.ZERO;  // 신호 신뢰도
    }

    // ==================== 국내 자동매매 ====================
    public synchronized boolean initializeDomestic() {
        if (domesticRunning.get()) {
            log.warn("국내 자동매매 엔진이 이미 실행 중입니다.");
            return false;
        }
        log.info("국내 자동매매 엔진 초기화 시작");
        domesticScheduler = Executors.newScheduledThreadPool(1);
        domesticStrategies.clear();
        domesticStatuses.clear();
        registerDomesticDefaultStrategies();
        startDomesticScheduling();
        domesticRunning.set(true);
        log.info("국내 자동매매 엔진 초기화 완료");
        return true;
    }

    private void registerDomesticDefaultStrategies() {
        AutoTradingStrategy testTrading1 = AutoTradingStrategy.createDefaultStrategy();
        testTrading1.setAccountNumber(accountService.getAccountBalance().getAccountNumber());
        registerDomesticStrategy(testTrading1);

        log.info("국내 전략 {}개 등록 완료", domesticStrategies.size());
    }

    public void registerDomesticStrategy(AutoTradingStrategy strategy) {
        domesticStrategies.put(strategy.getStrategyId(), strategy);
        domesticStatuses.put(strategy.getStrategyId(), new TradingStatus());
        log.info("국내 전략 등록: {} - {}", strategy.getStrategyId(), strategy.getStrategyName());
    }

    private void startDomesticScheduling() {
        if (domesticScheduler == null || domesticScheduler.isShutdown() || domesticScheduler.isTerminated()) {
            domesticScheduler = Executors.newScheduledThreadPool(1);
        }
        // 전략별로 분산 실행: 1분 내 전략 개수만큼 분할, 최소 1초 간격 보장
        domesticScheduler.scheduleAtFixedRate(this::scheduleDomesticStrategiesSequentially, 0, 1, TimeUnit.MINUTES);
        domesticScheduler.scheduleAtFixedRate(this::checkDomesticRiskManagement, 0, 5, TimeUnit.MINUTES);
        domesticScheduler.scheduleAtFixedRate(this::generateDomesticStatusReport, 0, 1, TimeUnit.HOURS);
        log.info("국내 자동매매 스케줄러 시작");
    }

    private void scheduleDomesticStrategiesSequentially() {
        int strategyCount = (int) domesticStrategies.values().stream().filter(AutoTradingStrategy::isEnabled).count();
        if (strategyCount == 0) return;
        int intervalSec = 60; // 1분 간격
        int idx = 0;
        for (AutoTradingStrategy strategy : domesticStrategies.values()) {
            if (!strategy.isEnabled()) continue;
            int delay = idx * intervalSec;
            domesticScheduler.schedule(() -> {
                try {
                    checkStrategy(strategy, domesticStatuses);
                } catch (Exception e) {
                    log.error("국내 전략 분산 체크 중 오류: {} - {}", strategy.getStrategyId(), e.getMessage());
                }
            }, delay, TimeUnit.SECONDS);
            idx++;
        }
    }

    private void checkDomesticRiskManagement() {
        for (AutoTradingStrategy strategy : domesticStrategies.values()) {
            if (!strategy.isEnabled()) continue;
            TradingStatus status = domesticStatuses.get(strategy.getStrategyId());
            if (status == null) continue;
            BigDecimal dailyLossRate = status.dailyPnL.divide(strategy.getTotalInvestment(), 4, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal("100"));
            if (dailyLossRate.compareTo(strategy.getMaxDailyLoss().negate()) <= 0) {
                log.warn("국내 일일 손실 한도 초과: {} - {}%", strategy.getStrategyId(), dailyLossRate);
                strategy.setEnabled(false);
            }
        }
    }
    private void generateDomesticStatusReport() {
        log.info("=== 국내 자동매매 상태 리포트 ===");
        log.info("활성 전략 수: {}", domesticStrategies.values().stream().filter(AutoTradingStrategy::isEnabled).count());
        for (AutoTradingStrategy strategy : domesticStrategies.values()) {
            if (!strategy.isEnabled()) continue;
            TradingStatus status = domesticStatuses.get(strategy.getStrategyId());
            if (status == null) continue;
            log.info("전략: {} - 보유: {}주, 평균가: {}, 일일손익: {}, 거래횟수: {}", strategy.getStrategyName(), status.currentPosition, status.averagePrice, status.dailyPnL, status.dailyTradeCount);
        }
        log.info("==========================");
    }
    public synchronized boolean shutdownDomestic() {
        if (!domesticRunning.get()) {
            log.warn("국내 자동매매 엔진이 이미 종료된 상태입니다.");
            return false;
        }
        log.info("국내 자동매매 엔진 종료 중...");
        if (domesticScheduler != null && !domesticScheduler.isShutdown()) {
            domesticScheduler.shutdown();
            try { if (!domesticScheduler.awaitTermination(60, TimeUnit.SECONDS)) domesticScheduler.shutdownNow(); } catch (InterruptedException e) { domesticScheduler.shutdownNow(); Thread.currentThread().interrupt(); }
        }
        domesticStrategies.clear();
        domesticStatuses.clear();
        domesticRunning.set(false);
        log.info("국내 자동매매 엔진 종료 완료");
        return true;
    }
    public boolean isDomesticRunning() { return domesticRunning.get(); }

    // ==================== 해외 자동매매 ====================
    public synchronized boolean initializeForeign() {
        if (foreignRunning.get()) {
            log.warn("해외 자동매매 엔진이 이미 실행 중입니다.");
            return false;
        }
        log.info("해외 자동매매 엔진 초기화 시작");
        foreignScheduler = Executors.newScheduledThreadPool(3);
        foreignStrategies.clear();
        foreignStatuses.clear();
        registerForeignDefaultStrategies();
        startForeignScheduling();
        foreignRunning.set(true);
        log.info("해외 자동매매 엔진 초기화 완료");
        return true;
    }
    private void registerForeignDefaultStrategies() {
        // AAPL: 기본, MSFT: 보수적, GOOGL: 공격적
        registerForeignStrategy(AutoTradingStrategy.createDefaultForeignStrategy(TopNasdaqStocks.YYAI));
        log.info("해외 전략 {}개 등록 완료", foreignStrategies.size());
    }
    public void registerForeignStrategy(AutoTradingStrategy strategy) {
        foreignStrategies.put(strategy.getStrategyId(), strategy);
        foreignStatuses.put(strategy.getStrategyId(), new TradingStatus());
        log.info("해외 전략 등록: {} - {}", strategy.getStrategyId(), strategy.getStrategyName());
    }
    private void startForeignScheduling() {
        if (foreignScheduler == null || foreignScheduler.isShutdown() || foreignScheduler.isTerminated()) {
            foreignScheduler = Executors.newScheduledThreadPool(3);
        }
        // 해외 전략도 국내와 동일하게 분산 실행: 1분 내 전략 개수만큼 분할, 최소 6초 간격 보장
        foreignScheduler.scheduleAtFixedRate(this::scheduleForeignStrategiesSequentially, 0, 1, TimeUnit.MINUTES);
        foreignScheduler.scheduleAtFixedRate(this::checkForeignRiskManagement, 0, 5, TimeUnit.MINUTES);
        foreignScheduler.scheduleAtFixedRate(this::generateForeignStatusReport, 0, 1, TimeUnit.HOURS);
        log.info("해외 자동매매 스케줄러 시작");
    }
    // 해외 전략 분산 실행 (최대 초당 10건 제한)
    private void scheduleForeignStrategiesSequentially() {
        int strategyCount = (int) foreignStrategies.values().stream().filter(AutoTradingStrategy::isEnabled).count();
        if (strategyCount == 0) return;
        int intervalSec = Math.max(6, 60 / strategyCount); // 최소 6초 간격(10개면 6초, 5개면 12초)
        int idx = 0;
        for (AutoTradingStrategy strategy : foreignStrategies.values()) {
            if (!strategy.isEnabled()) continue;
            int delay = idx * intervalSec;
            foreignScheduler.schedule(() -> {
                try {
                    checkStrategy(strategy, foreignStatuses);
                } catch (Exception e) {
                    log.error("해외 전략 분산 체크 중 오류: {} - {}", strategy.getStrategyId(), e.getMessage());
                }
            }, delay, TimeUnit.SECONDS);
            idx++;
        }
    }
    private void checkAllForeignStrategies() {
        for (AutoTradingStrategy strategy : foreignStrategies.values()) {
            if (!strategy.isEnabled()) continue;
            try { checkStrategy(strategy, foreignStatuses); } catch (Exception e) {
                log.error("해외 전략 체크 중 오류: {} - {}", strategy.getStrategyId(), e.getMessage());
            }
        }
    }
    private void checkForeignRiskManagement() {
        for (AutoTradingStrategy strategy : foreignStrategies.values()) {
            if (!strategy.isEnabled()) continue;
            TradingStatus status = foreignStatuses.get(strategy.getStrategyId());
            if (status == null) continue;
            BigDecimal dailyLossRate = status.dailyPnL.divide(strategy.getTotalInvestment(), 4, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal("100"));
            if (dailyLossRate.compareTo(strategy.getMaxDailyLoss().negate()) <= 0) {
                log.warn("해외 일일 손실 한도 초과: {} - {}%", strategy.getStrategyId(), dailyLossRate);
                strategy.setEnabled(false);
            }
        }
    }
    private void generateForeignStatusReport() {
        log.info("=== 해외 자동매매 상태 리포트 ===");
        log.info("활성 전략 수: {}", foreignStrategies.values().stream().filter(AutoTradingStrategy::isEnabled).count());
        for (AutoTradingStrategy strategy : foreignStrategies.values()) {
            if (!strategy.isEnabled()) continue;
            TradingStatus status = foreignStatuses.get(strategy.getStrategyId());
            if (status == null) continue;
            log.info("전략: {} - 보유: {}주, 평균가: {}, 일일손익: {}, 거래횟수: {}", strategy.getStrategyName(), status.currentPosition, status.averagePrice, status.dailyPnL, status.dailyTradeCount);
        }
        log.info("==========================");
    }
    public synchronized boolean shutdownForeign() {
        if (!foreignRunning.get()) {
            log.warn("해외 자동매매 엔진이 이미 종료된 상태입니다.");
            return false;
        }
        log.info("해외 자동매매 엔진 종료 중...");
        if (foreignScheduler != null && !foreignScheduler.isShutdown()) {
            foreignScheduler.shutdown();
            try { if (!foreignScheduler.awaitTermination(60, TimeUnit.SECONDS)) foreignScheduler.shutdownNow(); } catch (InterruptedException e) { foreignScheduler.shutdownNow(); Thread.currentThread().interrupt(); }
        }
        foreignStrategies.clear();
        foreignStatuses.clear();
        foreignRunning.set(false);
        log.info("해외 자동매매 엔진 종료 완료");
        return true;
    }
    public boolean isForeignRunning() { return foreignRunning.get(); }

    // ==================== 공통 메서드 ====================
    /**
     * 전략 등록 (국내/해외 자동 분류)
     */
    public void registerStrategy(AutoTradingStrategy strategy) {
        if (strategy.getMarketType() == MarketType.FOREIGN) {
            registerForeignStrategy(strategy);
        } else {
            registerDomesticStrategy(strategy);
        }
    }

    /**
     * 전략 제거 (국내/해외 자동 분류)
     */
    public void unregisterStrategy(String strategyId) {
        if (foreignStrategies.containsKey(strategyId)) {
            foreignStrategies.remove(strategyId);
            foreignStatuses.remove(strategyId);
            log.info("해외 전략 제거: {}", strategyId);
        } else if (domesticStrategies.containsKey(strategyId)) {
            domesticStrategies.remove(strategyId);
            domesticStatuses.remove(strategyId);
            log.info("국내 전략 제거: {}", strategyId);
        }
    }

    /**
     * 개별 전략 체크 (국내용)
     */
    private void checkStrategy(AutoTradingStrategy strategy, Map<String, TradingStatus> statuses) {
        String strategyId = strategy.getStrategyId();
        TradingStatus status = statuses.get(strategyId);

        if (status == null) {
            status = new TradingStatus();
            statuses.put(strategyId, status);
        }

        try {
            getCurrentPrice(strategy.getStockCode()); // MongoDB 시세 저장

            // 1. 과거 데이터 수집 (최소 120개)
            List<StockData> historicalData = collectHistoricalData(strategy.getStockCode(), 120);
            if (historicalData.size() < 60) {
                log.warn("불충분한 데이터: {} - {}개만 수집됨", strategy.getStockCode(), historicalData.size());
                return;
            }

            // 2. 현재 데이터
            StockData currentData = historicalData.get(historicalData.size() - 1);
            status.lastPrice = currentData.getClose();
            status.lastUpdateTime = LocalDateTime.now();

            // 3. 전략별 신호 생성 (Strategy 클래스 사용)
            String signal = generateAdvancedSignal(strategy, currentData, historicalData, status);

            // 4. 리스크 관리 체크
            signal = applyRiskManagement(signal, strategy, status, currentData.getClose());

            // 5. 포지션 사이징
            int quantity = calculatePositionSize(strategy, status, signal, currentData.getClose());

            // 6. 매매 실행
            if (!signal.equals("HOLD") && quantity > 0) {
                executeAdvancedTrade(strategy, signal, currentData, status, quantity);
            }

            // 7. 상태 업데이트
            updateTradingStatus(status, currentData.getClose());

        } catch (Exception e) {
            log.error("전략 체크 중 오류: {} - {}", strategyId, e.getMessage());
        }
    }

    /**
     * 고급 신호 생성 - Strategy 클래스 활용
     */
    private String generateAdvancedSignal(AutoTradingStrategy strategy,
                                          StockData currentData,
                                          List<StockData> historicalData,
                                          TradingStatus status) {

        // BackTestRequest 생성 (전략 파라미터 전달)
        BackTestRequest request = BackTestRequest.builder()
                .stockCode(strategy.getStockCode())
                .shortPeriod(5)
                .longPeriod(20)
                .macdFastPeriod(12)
                .macdSlowPeriod(26)
                .macdSignalPeriod(9)
                .build();

        String primarySignal = "HOLD";
        String secondarySignal = "HOLD";
        BigDecimal confidence = BigDecimal.ZERO;

        // 전략 타입에 따라 적절한 Strategy 클래스 사용
        switch (strategy.getStrategyType()) {
            case "MACD":
                primarySignal = macdStrategy.generateSignal(currentData, historicalData, request);
                confidence = calculateSignalConfidence(primarySignal, historicalData, "MACD");
                break;

            case "SMA":
                primarySignal = smaStrategy.generateSignal(currentData, historicalData, request);
                confidence = calculateSignalConfidence(primarySignal, historicalData, "SMA");
                break;

            case "MACD_SMA":  // 하이브리드 전략
                primarySignal = macdStrategy.generateSignal(currentData, historicalData, request);
                secondarySignal = smaStrategy.generateSignal(currentData, historicalData, request);

                // 신호 조합 로직
                if (primarySignal.equals(secondarySignal)) {
                    // 두 전략이 일치하면 높은 신뢰도
                    confidence = new BigDecimal("0.9");
                } else if (primarySignal.equals("BUY") && !secondarySignal.equals("SELL")) {
                    // 부분 일치
                    primarySignal = "BUY";
                    confidence = new BigDecimal("0.6");
                } else if (primarySignal.equals("SELL") && !secondarySignal.equals("BUY")) {
                    primarySignal = "SELL";
                    confidence = new BigDecimal("0.6");
                } else {
                    // 상충하는 신호
                    primarySignal = "HOLD";
                    confidence = new BigDecimal("0.3");
                }
                break;

            case "ADAPTIVE":  // 적응형 전략 (시장 상황에 따라 전략 선택)
                MarketCondition condition = analyzeMarketCondition(historicalData);

                if (condition == MarketCondition.TRENDING) {
                    // 트렌드 시장에서는 MACD 우선
                    primarySignal = macdStrategy.generateSignal(currentData, historicalData, request);
                    confidence = calculateSignalConfidence(primarySignal, historicalData, "MACD");
                } else if (condition == MarketCondition.RANGING) {
                    // 횡보 시장에서는 SMA 우선
                    primarySignal = smaStrategy.generateSignal(currentData, historicalData, request);
                    confidence = calculateSignalConfidence(primarySignal, historicalData, "SMA");
                } else {
                    // 불확실한 시장
                    primarySignal = "HOLD";
                    confidence = new BigDecimal("0.2");
                }
                break;

            default:
                // 기본 하이브리드 전략
                primarySignal = macdStrategy.generateSignal(currentData, historicalData, request);
                secondarySignal = smaStrategy.generateSignal(currentData, historicalData, request);

                if (primarySignal.equals(secondarySignal)) {
                    confidence = new BigDecimal("0.8");
                } else {
                    primarySignal = "HOLD";
                    confidence = new BigDecimal("0.4");
                }
        }

        // 신뢰도 저장
        status.confidenceScore = confidence;

        // 신뢰도 임계값 체크 (최소 50% 이상)
        if (confidence.compareTo(new BigDecimal("0.5")) < 0) {
            log.debug("신뢰도 부족: {} ({}%)", primarySignal, confidence.multiply(new BigDecimal("100")));
            return "HOLD";
        }

        log.info("신호 생성: {} (신뢰도: {}%)", primarySignal, confidence.multiply(new BigDecimal("100")));
        return primarySignal;
    }

    /**
     * 시장 상황 분석
     */
    private enum MarketCondition {
        TRENDING, RANGING, VOLATILE, UNCERTAIN
    }

    private MarketCondition analyzeMarketCondition(List<StockData> historicalData) {
        if (historicalData.size() < 20) return MarketCondition.UNCERTAIN;

        // ADX 계산 (트렌드 강도)
        BigDecimal adx = calculateADX(historicalData, 14);

        // 변동성 계산
        BigDecimal volatility = calculateVolatility(historicalData, 20);

        if (adx.compareTo(new BigDecimal("25")) > 0) {
            return MarketCondition.TRENDING;
        } else if (volatility.compareTo(new BigDecimal("0.02")) < 0) {
            return MarketCondition.RANGING;
        } else if (volatility.compareTo(new BigDecimal("0.05")) > 0) {
            return MarketCondition.VOLATILE;
        } else {
            return MarketCondition.UNCERTAIN;
        }
    }

    private String applyRiskManagement(String signal, AutoTradingStrategy strategy,
                                       TradingStatus status, BigDecimal currentPrice) {

        // 1. 연속 손실 체크
        if (status.consecutiveLosses >= 3) {
            log.warn("연속 손실 3회 이상 - 매수 제한");
            if (signal.equals("BUY")) {
                return "HOLD";
            }
        }

        // 2. 일일 손실 한도 체크
        BigDecimal dailyLossRate = status.dailyPnL.abs().divide(
                strategy.getTotalInvestment(), 4, RoundingMode.HALF_UP);

        if (dailyLossRate.compareTo(strategy.getMaxDailyLoss()) > 0) {
            log.warn("일일 손실 한도 초과: {}%", dailyLossRate.multiply(new BigDecimal("100")));
            return "HOLD";
        }

        // 3. 포지션 보유 시 추가 체크
        if (status.currentPosition.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal profitRate = currentPrice.subtract(status.averagePrice)
                    .divide(status.averagePrice, 4, RoundingMode.HALF_UP);

            // 동적 손절
            BigDecimal dynamicStopLoss = calculateDynamicStopLoss(status, currentPrice);
            if (profitRate.compareTo(dynamicStopLoss.negate()) < 0) {
                log.info("동적 손절 실행: {}%", profitRate.multiply(new BigDecimal("100")));
                return "SELL";
            }

            // 트레일링 스톱
            if (currentPrice.compareTo(status.peakPrice) > 0) {
                status.peakPrice = currentPrice;
            } else {
                BigDecimal dropFromPeak = status.peakPrice.subtract(currentPrice)
                        .divide(status.peakPrice, 4, RoundingMode.HALF_UP);

                // 수익 상태에서 3% 이상 하락 시 매도
                if (profitRate.compareTo(BigDecimal.ZERO) > 0 &&
                        dropFromPeak.compareTo(new BigDecimal("0.03")) > 0) {
                    log.info("트레일링 스톱 실행: 고점 대비 {}% 하락",
                            dropFromPeak.multiply(new BigDecimal("100")));
                    return "SELL";
                }
            }
        }

        return signal;
    }

    /**
     * 동적 포지션 사이징 (켈리 공식 변형)
     */
    private int calculatePositionSize(AutoTradingStrategy strategy, TradingStatus status,
                                      String signal, BigDecimal currentPrice) {

        if (signal.equals("HOLD")) return 0;

        // 기본 포지션 크기
        BigDecimal baseInvestment = strategy.getTotalInvestment()
                .multiply(strategy.getMaxPositionSize());

        // 켈리 비율 계산
        BigDecimal winRate = calculateWinRate(status);
        BigDecimal avgWin = status.winCount > 0 ?
                status.totalProfit.divide(new BigDecimal(status.winCount), 4, RoundingMode.HALF_UP) :
                BigDecimal.ONE;
        BigDecimal avgLoss = status.lossCount > 0 ?
                status.totalLoss.divide(new BigDecimal(status.lossCount), 4, RoundingMode.HALF_UP) :
                BigDecimal.ONE;

        BigDecimal kellyRatio = BigDecimal.ZERO;
        if (avgLoss.compareTo(BigDecimal.ZERO) > 0) {
            // Kelly % = (승률 * 평균수익 - 패률 * 평균손실) / 평균수익
            BigDecimal lossRate = BigDecimal.ONE.subtract(winRate);
            kellyRatio = winRate.multiply(avgWin).subtract(lossRate.multiply(avgLoss))
                    .divide(avgWin, 4, RoundingMode.HALF_UP);
        }

        // 켈리 비율 제한 (최대 25%)
        kellyRatio = kellyRatio.min(new BigDecimal("0.25")).max(new BigDecimal("0.05"));

        // 신뢰도 반영
        BigDecimal confidenceMultiplier = status.confidenceScore;

        // 최종 투자 금액
        BigDecimal finalInvestment = baseInvestment
                .multiply(kellyRatio)
                .multiply(confidenceMultiplier);

        // 주식 수 계산
        int quantity = finalInvestment.divide(currentPrice, 0, RoundingMode.DOWN).intValue();

        // 최소/최대 제한
        quantity = Math.max(1, Math.min(quantity, 1000));

        log.debug("포지션 사이징: 켈리={}%, 신뢰도={}%, 수량={}",
                kellyRatio.multiply(new BigDecimal("100")),
                confidenceMultiplier.multiply(new BigDecimal("100")),
                quantity);

        return quantity;
    }


    private List<StockData> collectHistoricalData(String stockCode, int days) {
        List<PriceQueryRecord> records = priceQueryRecordService
                .findTop50ByStockCodeAndQueryTypeOrderByQueryDateTimeDesc(stockCode, "current");

        // PriceQueryRecord를 StockData로 변환
        return records.stream()
                .map(this::convertToStockData)
                .sorted(Comparator.comparing(StockData::getDate))
                .collect(Collectors.toList());
    }

    /**
     * PriceQueryRecord를 StockData로 변환
     */
    private StockData convertToStockData(PriceQueryRecord record) {
        return StockData.builder()
                .date(record.getQueryDateTime().toLocalDate())
                .open(new BigDecimal(record.getOpenPrice()))
                .high(new BigDecimal(record.getHighPrice()))
                .low(new BigDecimal(record.getLowPrice()))
                .close(new BigDecimal(record.getCurrentPrice()))
                .volume(record.getTradingVolume())
                .build();
    }

    private BigDecimal calculateSignalConfidence(String signal, List<StockData> data, String strategy) {
        // 간단한 신뢰도 계산 로직
        if (signal.equals("HOLD")) return new BigDecimal("0.3");

        // 거래량, 변동성 등을 고려한 신뢰도
        BigDecimal volumeScore = calculateVolumeScore(data);
        BigDecimal volatilityScore = BigDecimal.ONE.subtract(calculateVolatility(data, 20).min(BigDecimal.ONE));

        return volumeScore.add(volatilityScore).divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateVolumeScore(List<StockData> data) {
        if (data.size() < 20) return new BigDecimal("0.5");

        BigDecimal currentVolume = data.get(data.size() - 1).getVolume();
        BigDecimal avgVolume = data.stream()
                .skip(Math.max(0, data.size() - 20))
                .map(StockData::getVolume)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(new BigDecimal("20"), 2, RoundingMode.HALF_UP);

        if (avgVolume.compareTo(BigDecimal.ZERO) == 0) return new BigDecimal("0.5");

        BigDecimal ratio = currentVolume.divide(avgVolume, 2, RoundingMode.HALF_UP);
        return ratio.min(new BigDecimal("2")).divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateVolatility(List<StockData> data, int period) {
        if (data.size() < period) return BigDecimal.ZERO;

        List<BigDecimal> returns = new ArrayList<>();
        for (int i = data.size() - period; i < data.size(); i++) {
            if (i > 0) {
                BigDecimal prevClose = data.get(i - 1).getClose();
                BigDecimal currentClose = data.get(i).getClose();
                if (prevClose.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal dailyReturn = currentClose.subtract(prevClose)
                            .divide(prevClose, 6, RoundingMode.HALF_UP);
                    returns.add(dailyReturn);
                }
            }
        }

        if (returns.isEmpty()) return BigDecimal.ZERO;

        // 표준편차 계산
        BigDecimal mean = returns.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(new BigDecimal(returns.size()), 6, RoundingMode.HALF_UP);

        BigDecimal variance = returns.stream()
                .map(r -> r.subtract(mean).pow(2))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(new BigDecimal(returns.size()), 6, RoundingMode.HALF_UP);

        return new BigDecimal(Math.sqrt(variance.doubleValue()));
    }

    private BigDecimal calculateADX(List<StockData> data, int period) {
        // 간단한 ADX 근사값 (실제로는 복잡한 계산)
        if (data.size() < period * 2) return new BigDecimal("20");

        BigDecimal trendStrength = BigDecimal.ZERO;
        for (int i = data.size() - period; i < data.size(); i++) {
            BigDecimal change = data.get(i).getClose().subtract(data.get(i - 1).getClose()).abs();
            trendStrength = trendStrength.add(change);
        }

        return trendStrength.divide(new BigDecimal(period), 2, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    private BigDecimal calculateDynamicStopLoss(TradingStatus status, BigDecimal currentPrice) {
        // ATR 기반 동적 손절
        BigDecimal baseStopLoss = new BigDecimal("0.03"); // 3% 기본

        // 연속 손실이 많으면 손절폭 축소
        if (status.consecutiveLosses > 2) {
            baseStopLoss = new BigDecimal("0.02");
        }

        return baseStopLoss;
    }

    private BigDecimal calculateWinRate(TradingStatus status) {
        int totalTrades = status.winCount + status.lossCount;
        if (totalTrades == 0) return new BigDecimal("0.5"); // 기본값 50%

        return new BigDecimal(status.winCount).divide(new BigDecimal(totalTrades), 4, RoundingMode.HALF_UP);
    }

    private void updateBuyStatus(TradingStatus status, BigDecimal price, int quantity) {
        BigDecimal qty = new BigDecimal(quantity);
        BigDecimal totalCost = status.averagePrice.multiply(status.currentPosition)
                .add(price.multiply(qty));

        status.currentPosition = status.currentPosition.add(qty);
        status.averagePrice = totalCost.divide(status.currentPosition, 2, RoundingMode.HALF_UP);
        status.peakPrice = price; // 피크 가격 리셋
    }

    private BigDecimal updateSellStatus(TradingStatus status, BigDecimal price, int quantity) {
        BigDecimal qty = new BigDecimal(quantity);
        BigDecimal profit = price.subtract(status.averagePrice).multiply(qty);

        status.currentPosition = status.currentPosition.subtract(qty);
        status.dailyPnL = status.dailyPnL.add(profit);

        if (profit.compareTo(BigDecimal.ZERO) > 0) {
            status.winCount++;
            status.totalProfit = status.totalProfit.add(profit);
            status.consecutiveLosses = 0;
        } else {
            status.lossCount++;
            status.totalLoss = status.totalLoss.add(profit.abs());
            status.consecutiveLosses++;
        }

        if (status.currentPosition.compareTo(BigDecimal.ZERO) == 0) {
            status.averagePrice = BigDecimal.ZERO;
            status.peakPrice = BigDecimal.ZERO;
        }

        return profit;
    }

    private void updateTradingStatus(TradingStatus status, BigDecimal currentPrice) {
        status.lastPrice = currentPrice;
        status.lastUpdateTime = LocalDateTime.now();
    }

    /**
     * 개선된 매매 실행
     */
    private void executeAdvancedTrade(AutoTradingStrategy strategy, String signal,
                                      StockData currentData, TradingStatus status, int quantity) {

        StockOrderRequest orderRequest = StockOrderRequest.builder()
                .accountNumber(strategy.getAccountNumber())
                .stockCode(strategy.getStockCode())
                .orderType(signal.equals("BUY") ? "매수" : "매도")
                .quantity(quantity)
                .price(currentData.getClose().intValue())
                .priceType("지정가")  // 슬리피지 방지
                .orderCategory("일반")
                .build();

        try {
            StockOrderResponse orderResponse = stockOrderService.executeOrder(orderRequest);

            if ("성공".equals(orderResponse.getOrderStatus())) {
                if (signal.equals("BUY")) {
                    updateBuyStatus(status, currentData.getClose(), quantity);
                    log.info("매수 체결: {} - {}주 @ {}", strategy.getStockCode(),
                            quantity, currentData.getClose());
                } else {
                    BigDecimal profit = updateSellStatus(status, currentData.getClose(), quantity);
                    log.info("매도 체결: {} - {}주 @ {} (손익: {})",
                            strategy.getStockCode(), quantity, currentData.getClose(), profit);
                }

                status.lastSignal = signal;
                status.dailyTradeCount++;
                status.lastTradeTime = LocalDateTime.now();
            }
        } catch (Exception e) {
            log.error("주문 실행 실패: {} - {}", strategy.getStockCode(), e.getMessage());
        }
    }


    /**
     * 현재가 조회
     */
    private void getCurrentPrice(String stockCode) {
        try {
            StockPriceRequest request = StockPriceRequest.builder()
                    .stockCode(stockCode)
                    .build();

            stockPriceService.getCurrentPrice(request, null);
        } catch (Exception e) {
            log.error("현재가 조회 실패: {} - {}", stockCode, e.getMessage());
        }
    }


    // ==================== 해외 주식 관련 메서드 ====================

    /**
     * 나스닥 상위 종목 목록 조회
     */
    public Map<String, Object> getTopNasdaqStocks() {
        Map<String, Object> result = new HashMap<>();
        result.put("market", "NASDAQ");
        result.put("description", "나스닥 상위 20개 종목");

        Map<String, Object>[] stocks = new Map[TopNasdaqStocks.values().length];
        int i = 0;
        for (TopNasdaqStocks stock : TopNasdaqStocks.values()) {
            Map<String, Object> stockInfo = new HashMap<>();
            stockInfo.put("stockCode", stock.getStockCode());
            stockInfo.put("companyName", stock.getCompanyName());
            stockInfo.put("koreanName", stock.getKoreanName());
            stockInfo.put("sector", stock.getSector());
            stockInfo.put("description", stock.getDescription());
            stocks[i++] = stockInfo;
        }
        result.put("stocks", stocks);

        return result;
    }

    /**
     * 섹터별 종목 목록 조회 (해외)
     */
    public Map<String, Object> getForeignStocksBySector(String sector) {
        Map<String, Object> result = new HashMap<>();
        result.put("sector", sector);
        result.put("market", "NASDAQ");

        TopNasdaqStocks[] sectorStocks = TopNasdaqStocks.getBySector(sector);
        Map<String, Object>[] stocks = new Map[sectorStocks.length];

        for (int i = 0; i < sectorStocks.length; i++) {
            TopNasdaqStocks stock = sectorStocks[i];
            Map<String, Object> stockInfo = new HashMap<>();
            stockInfo.put("stockCode", stock.getStockCode());
            stockInfo.put("companyName", stock.getCompanyName());
            stockInfo.put("koreanName", stock.getKoreanName());
            stockInfo.put("sector", stock.getSector());
            stockInfo.put("description", stock.getDescription());
            stocks[i] = stockInfo;
        }
        result.put("stocks", stocks);

        return result;
    }

    /**
     * 섹터 목록 조회 (해외)
     */
    public Map<String, Object> getAllForeignSectors() {
        Map<String, Object> result = new HashMap<>();
        result.put("market", "NASDAQ");

        String[] sectors = TopNasdaqStocks.getAllSectors();
        result.put("sectors", sectors);

        return result;
    }

    private void updateDynamicInvestment(AutoTradingStrategy strategy) {
        if (!strategy.isUseDynamicInvestment()) {
            return;
        }

        // 계좌 잔액 조회
        AccountBalanceDto balance = accountService.getAccountBalance();
        if (balance == null) {
            log.error("계좌 잔액 조회 실패: {}", strategy.getAccountNumber());
            strategy.setEnabled(false);  // 전략 비활성화
            return;
        }

        // 매수가능금액 기준으로 투자금 계산
        BigDecimal availableAmount = balance.getAvailableAmount();

        // 안전 마진 적용 (매수가능금액의 95% 사용)
        availableAmount = availableAmount.multiply(new BigDecimal("0.95"));

        BigDecimal calculatedInvestment = availableAmount.multiply(strategy.getInvestmentRatio());

        // 최소/최대 제한
        if (calculatedInvestment.compareTo(strategy.getMinInvestment()) < 0) {
            log.warn("투자금액이 최소금액 미달: {} < {}", calculatedInvestment, strategy.getMinInvestment());
            calculatedInvestment = BigDecimal.ZERO;  // 투자 중지
        } else if (calculatedInvestment.compareTo(strategy.getMaxInvestment()) > 0) {
            calculatedInvestment = strategy.getMaxInvestment();
        }

        strategy.setTotalInvestment(calculatedInvestment);

        log.info("투자금액 업데이트: 매수가능={}, 투자비율={}%, 투자금액={}",
                balance.getAvailableAmount(),
                strategy.getInvestmentRatio().multiply(new BigDecimal("100")),
                calculatedInvestment);
    }
}