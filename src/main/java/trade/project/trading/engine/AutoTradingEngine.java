package trade.project.trading.engine;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import trade.project.api.dto.StockPriceRequest;
import trade.project.api.dto.StockPriceResponse;
import trade.project.api.dto.StockOrderRequest;
import trade.project.api.dto.StockOrderResponse;
import trade.project.api.service.StockPriceService;
import trade.project.api.service.StockOrderService;
import trade.project.api.service.ForeignStockPriceService;
import trade.project.api.service.ForeignStockOrderService;
import trade.project.backtest.util.TechnicalIndicatorCalculator;
import trade.project.trading.dto.AutoTradingStrategy;
import trade.project.trading.enums.TopKospiStocks;
import trade.project.trading.enums.TopNasdaqStocks;
import trade.project.trading.enums.TradingSchedule;
import trade.project.trading.enums.MarketType;
import trade.project.trading.service.TradingRecordService;
import trade.project.trading.service.PriceQueryRecordService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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

    // 국내/해외 전략, 상태, 스케줄러 분리
    private final Map<String, AutoTradingStrategy> domesticStrategies = new ConcurrentHashMap<>();
    private final Map<String, AutoTradingStrategy> foreignStrategies = new ConcurrentHashMap<>();
    private final Map<String, TradingStatus> domesticStatuses = new ConcurrentHashMap<>();
    private final Map<String, TradingStatus> foreignStatuses = new ConcurrentHashMap<>();
    private ScheduledExecutorService domesticScheduler;
    private ScheduledExecutorService foreignScheduler;
    private final AtomicBoolean domesticRunning = new AtomicBoolean(false);
    private final AtomicBoolean foreignRunning = new AtomicBoolean(false);
    
    /**
     * 거래 상태 클래스
     */
    private static class TradingStatus {
        private BigDecimal currentPosition = BigDecimal.ZERO; // 현재 보유 수량
        private BigDecimal averagePrice = BigDecimal.ZERO; // 평균 매수가
        private BigDecimal dailyPnL = BigDecimal.ZERO; // 일일 손익
        private int dailyTradeCount = 0; // 일일 거래 횟수
        private LocalDateTime lastTradeTime; // 마지막 거래 시간
        private BigDecimal lastPrice; // 마지막 가격
        private LocalDateTime lastUpdateTime; // 마지막 업데이트 시간
    }
    
    // ==================== 국내 자동매매 ====================
    public synchronized boolean initializeDomestic() {
        if (domesticRunning.get()) {
            log.warn("국내 자동매매 엔진이 이미 실행 중입니다.");
            return false;
        }
        log.info("국내 자동매매 엔진 초기화 시작");
        domesticScheduler = Executors.newScheduledThreadPool(3);
        domesticStrategies.clear();
        domesticStatuses.clear();
        registerDomesticDefaultStrategies();
        startDomesticScheduling();
        domesticRunning.set(true);
        log.info("국내 자동매매 엔진 초기화 완료");
        return true;
    }
    private void registerDomesticDefaultStrategies() {
        AutoTradingStrategy samsung = AutoTradingStrategy.createDefaultStrategy();
        registerDomesticStrategy(samsung);
        AutoTradingStrategy skHynix = AutoTradingStrategy.createConservativeStrategy(TopKospiStocks.SK_HYNIX);
        registerDomesticStrategy(skHynix);
        AutoTradingStrategy naver = AutoTradingStrategy.createAggressiveStrategy(TopKospiStocks.NAVER);
        registerDomesticStrategy(naver);
        log.info("국내 전략 {}개 등록 완료", domesticStrategies.size());
    }
    public void registerDomesticStrategy(AutoTradingStrategy strategy) {
        domesticStrategies.put(strategy.getStrategyId(), strategy);
        domesticStatuses.put(strategy.getStrategyId(), new TradingStatus());
        log.info("국내 전략 등록: {} - {}", strategy.getStrategyId(), strategy.getStrategyName());
    }
    private void startDomesticScheduling() {
        if (domesticScheduler == null || domesticScheduler.isShutdown() || domesticScheduler.isTerminated()) {
            domesticScheduler = Executors.newScheduledThreadPool(3);
        }
        domesticScheduler.scheduleAtFixedRate(this::checkAllDomesticStrategies, 0, 1, TimeUnit.MINUTES);
        domesticScheduler.scheduleAtFixedRate(this::checkDomesticRiskManagement, 0, 5, TimeUnit.MINUTES);
        domesticScheduler.scheduleAtFixedRate(this::generateDomesticStatusReport, 0, 1, TimeUnit.HOURS);
        log.info("국내 자동매매 스케줄러 시작");
    }
    private void checkAllDomesticStrategies() {
        for (AutoTradingStrategy strategy : domesticStrategies.values()) {
            if (!strategy.isEnabled()) continue;
            try { checkStrategy(strategy, domesticStatuses); } catch (Exception e) {
                log.error("국내 전략 체크 중 오류: {} - {}", strategy.getStrategyId(), e.getMessage());
            }
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
        registerForeignStrategy(AutoTradingStrategy.createDefaultStrategy(TopNasdaqStocks.AAPL));
        registerForeignStrategy(AutoTradingStrategy.createConservativeStrategy(TopNasdaqStocks.MSFT));
        registerForeignStrategy(AutoTradingStrategy.createAggressiveStrategy(TopNasdaqStocks.GOOGL));
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
        foreignScheduler.scheduleAtFixedRate(this::checkAllForeignStrategies, 0, 1, TimeUnit.MINUTES);
        foreignScheduler.scheduleAtFixedRate(this::checkForeignRiskManagement, 0, 5, TimeUnit.MINUTES);
        foreignScheduler.scheduleAtFixedRate(this::generateForeignStatusReport, 0, 1, TimeUnit.HOURS);
        log.info("해외 자동매매 스케줄러 시작");
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
        
        // 시세 조회
        StockPriceResponse priceResponse = getCurrentPrice(strategy.getStockCode());
        if (priceResponse == null) {
            log.warn("시세 조회 실패: {}", strategy.getStockCode());
            return;
        }
        
        status.lastPrice = new BigDecimal(priceResponse.getCurrentPrice());
        status.lastUpdateTime = LocalDateTime.now();
        
        // 기술적 지표 계산
        Map<String, BigDecimal> indicators = calculateTechnicalIndicators(strategy, priceResponse);
        
        // 매매 신호 확인
        TradingSignal signal = analyzeTradingSignal(strategy, priceResponse, indicators, status);
        
        // 매매 실행
        if (signal != TradingSignal.HOLD) {
            executeTrade(strategy, signal, priceResponse, status);
        }
    }
    
    /**
     * 현재가 조회
     */
    private StockPriceResponse getCurrentPrice(String stockCode) {
        try {
            StockPriceRequest request = StockPriceRequest.builder()
                    .stockCode(stockCode)
                    .build();
            
            return stockPriceService.getCurrentPrice(request, null);
        } catch (Exception e) {
            log.error("현재가 조회 실패: {} - {}", stockCode, e.getMessage());
            return null;
        }
    }
    
    /**
     * 기술적 지표 계산
     */
    private Map<String, BigDecimal> calculateTechnicalIndicators(AutoTradingStrategy strategy, StockPriceResponse priceResponse) {
        // 실제 구현에서는 과거 데이터를 사용하여 지표를 계산해야 함
        // 여기서는 간단한 예시로 구현
        
        Map<String, BigDecimal> indicators = new java.util.HashMap<>();
        
        // RSI (임시 값)
        indicators.put("RSI", new BigDecimal("50"));
        
        // MACD (임시 값)
        indicators.put("MACD", new BigDecimal("0"));
        indicators.put("MACD_SIGNAL", new BigDecimal("0"));
        
        // 이동평균 (임시 값)
        indicators.put("SMA_SHORT", new BigDecimal(priceResponse.getCurrentPrice()));
        indicators.put("SMA_LONG", new BigDecimal(priceResponse.getCurrentPrice()));
        
        return indicators;
    }
    
    /**
     * 매매 신호 분석
     */
    private TradingSignal analyzeTradingSignal(AutoTradingStrategy strategy, StockPriceResponse priceResponse, 
                                             Map<String, BigDecimal> indicators, TradingStatus status) {
        BigDecimal currentPrice = new BigDecimal(priceResponse.getCurrentPrice());
        BigDecimal rsi = indicators.get("RSI");
        BigDecimal macd = indicators.get("MACD");
        BigDecimal macdSignal = indicators.get("MACD_SIGNAL");
        BigDecimal smaShort = indicators.get("SMA_SHORT");
        BigDecimal smaLong = indicators.get("SMA_LONG");
        
        // 매수 신호 확인
        if (shouldBuy(strategy, currentPrice, rsi, macd, macdSignal, smaShort, smaLong, status)) {
            return TradingSignal.BUY;
        }
        
        // 매도 신호 확인
        if (shouldSell(strategy, currentPrice, rsi, macd, macdSignal, smaShort, smaLong, status)) {
            return TradingSignal.SELL;
        }
        
        return TradingSignal.HOLD;
    }
    
    /**
     * 매수 신호 확인
     */
    private boolean shouldBuy(AutoTradingStrategy strategy, BigDecimal currentPrice, BigDecimal rsi, 
                            BigDecimal macd, BigDecimal macdSignal, BigDecimal smaShort, BigDecimal smaLong, 
                            TradingStatus status) {
        // 보유 수량이 최대 포지션 크기를 초과하면 매수하지 않음
        BigDecimal maxPosition = strategy.getTotalInvestment().multiply(strategy.getMaxPositionSize());
        if (status.currentPosition.compareTo(maxPosition) >= 0) {
            return false;
        }
        
        // 일일 거래 횟수 제한 확인
        if (status.dailyTradeCount >= strategy.getMaxDailyTrades()) {
            return false;
        }
        
        // RSI 과매도 조건
        boolean rsiOversold = rsi.compareTo(strategy.getRsiOversold()) <= 0;
        
        // MACD 상승 신호
        boolean macdBullish = macd.compareTo(macdSignal) > 0;
        
        // 이동평균 상승 신호
        boolean smaBullish = smaShort.compareTo(smaLong) > 0;
        
        // 가격 제한 확인
        boolean priceLimitOk = strategy.getBuyPriceLimit().compareTo(BigDecimal.ZERO) == 0 || 
                              currentPrice.compareTo(strategy.getBuyPriceLimit()) <= 0;
        
        return rsiOversold && macdBullish && smaBullish && priceLimitOk;
    }
    
    /**
     * 매도 신호 확인
     */
    private boolean shouldSell(AutoTradingStrategy strategy, BigDecimal currentPrice, BigDecimal rsi, 
                             BigDecimal macd, BigDecimal macdSignal, BigDecimal smaShort, BigDecimal smaLong, 
                             TradingStatus status) {
        // 보유 수량이 없으면 매도하지 않음
        if (status.currentPosition.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        
        // 일일 거래 횟수 제한 확인
        if (status.dailyTradeCount >= strategy.getMaxDailyTrades()) {
            return false;
        }
        
        // RSI 과매수 조건
        boolean rsiOverbought = rsi.compareTo(strategy.getRsiOverbought()) >= 0;
        
        // MACD 하락 신호
        boolean macdBearish = macd.compareTo(macdSignal) < 0;
        
        // 이동평균 하락 신호
        boolean smaBearish = smaShort.compareTo(smaLong) < 0;
        
        // 수익 목표 달성
        BigDecimal profitRate = currentPrice.subtract(status.averagePrice)
                .divide(status.averagePrice, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(new BigDecimal("100"));
        boolean profitTargetReached = profitRate.compareTo(strategy.getProfitTarget()) >= 0;
        
        // 손절 조건
        boolean stopLossTriggered = profitRate.compareTo(strategy.getStopLoss().negate()) <= 0;
        
        // 가격 제한 확인
        boolean priceLimitOk = strategy.getSellPriceLimit().compareTo(BigDecimal.ZERO) == 0 || 
                              currentPrice.compareTo(strategy.getSellPriceLimit()) >= 0;
        
        return (rsiOverbought && macdBearish && smaBearish) || profitTargetReached || stopLossTriggered || priceLimitOk;
    }
    
    /**
     * 매매 실행
     */
    private void executeTrade(AutoTradingStrategy strategy, TradingSignal signal, 
                            StockPriceResponse priceResponse, TradingStatus status) {
        try {
            if (signal == TradingSignal.BUY) {
                executeBuyOrder(strategy, priceResponse, status);
            } else if (signal == TradingSignal.SELL) {
                executeSellOrder(strategy, priceResponse, status);
            }
        } catch (Exception e) {
            log.error("매매 실행 중 오류 발생: {} - {}", strategy.getStrategyId(), e.getMessage());
        }
    }
    
    /**
     * 매수 주문 실행
     */
    private void executeBuyOrder(AutoTradingStrategy strategy, StockPriceResponse priceResponse, TradingStatus status) {
        StockOrderRequest orderRequest = StockOrderRequest.builder()
                .accountNumber("1234567890") // 실제 계좌번호로 변경 필요
                .stockCode(strategy.getStockCode())
                .orderType("매수")
                .quantity(strategy.getBuyQuantity())
                .price(priceResponse.getCurrentPrice())
                .priceType(strategy.getBuyPriceType())
                .orderCategory("일반")
                .build();
        
        try {
            StockOrderResponse orderResponse = stockOrderService.executeOrder(orderRequest);
            
            if ("성공".equals(orderResponse.getOrderStatus())) {
                // 상태 업데이트
                status.currentPosition = status.currentPosition.add(new BigDecimal(strategy.getBuyQuantity()));
                status.averagePrice = calculateNewAveragePrice(status.averagePrice, status.currentPosition, 
                                                           new BigDecimal(priceResponse.getCurrentPrice()), 
                                                           new BigDecimal(strategy.getBuyQuantity()));
                status.dailyTradeCount++;
                status.lastTradeTime = LocalDateTime.now();
                
                log.info("매수 주문 성공: {} - {}주 @ {}", strategy.getStockCode(), 
                        strategy.getBuyQuantity(), priceResponse.getCurrentPrice());
            }
        } catch (Exception e) {
            log.error("매수 주문 실패: {} - {}", strategy.getStockCode(), e.getMessage());
        }
    }
    
    /**
     * 매도 주문 실행
     */
    private void executeSellOrder(AutoTradingStrategy strategy, StockPriceResponse priceResponse, TradingStatus status) {
        StockOrderRequest orderRequest = StockOrderRequest.builder()
                .accountNumber("1234567890") // 실제 계좌번호로 변경 필요
                .stockCode(strategy.getStockCode())
                .orderType("매도")
                .quantity(status.currentPosition.intValue())
                .price(priceResponse.getCurrentPrice())
                .priceType("시장가")
                .orderCategory("일반")
                .build();
        
        try {
            StockOrderResponse orderResponse = stockOrderService.executeOrder(orderRequest);
            
            if ("성공".equals(orderResponse.getOrderStatus())) {
                // 손익 계산
                BigDecimal currentPrice = new BigDecimal(priceResponse.getCurrentPrice());
                BigDecimal profit = currentPrice.subtract(status.averagePrice)
                        .multiply(status.currentPosition);
                status.dailyPnL = status.dailyPnL.add(profit);
                
                // 상태 업데이트
                status.currentPosition = BigDecimal.ZERO;
                status.averagePrice = BigDecimal.ZERO;
                status.dailyTradeCount++;
                status.lastTradeTime = LocalDateTime.now();
                
                log.info("매도 주문 성공: {} - {}주 @ {} (손익: {})", strategy.getStockCode(), 
                        status.currentPosition, priceResponse.getCurrentPrice(), profit);
            }
        } catch (Exception e) {
            log.error("매도 주문 실패: {} - {}", strategy.getStockCode(), e.getMessage());
        }
    }
    
    /**
     * 새로운 평균가 계산
     */
    private BigDecimal calculateNewAveragePrice(BigDecimal currentAverage, BigDecimal currentPosition, 
                                             BigDecimal newPrice, BigDecimal newQuantity) {
        BigDecimal totalValue = currentAverage.multiply(currentPosition).add(newPrice.multiply(newQuantity));
        BigDecimal totalQuantity = currentPosition.add(newQuantity);
        return totalValue.divide(totalQuantity, 2, BigDecimal.ROUND_HALF_UP);
    }
    
    /**
     * 매매 신호 Enum
     */
    private enum TradingSignal {
        BUY, SELL, HOLD
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
} 