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
import trade.project.trading.document.PriceQueryRecord;
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
    private final PriceQueryRecordService priceQueryRecordService; // [1] 과거 가격 데이터 서비스 주입

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
            domesticScheduler = Executors.newScheduledThreadPool(1);
        }
        // 전략별로 분산 실행: 1분 내 전략 개수만큼 분할, 최소 1초 간격 보장
        domesticScheduler.scheduleAtFixedRate(this::scheduleDomesticStrategiesSequentially, 0, 1, TimeUnit.MINUTES);
        domesticScheduler.scheduleAtFixedRate(this::checkDomesticRiskManagement, 0, 5, TimeUnit.MINUTES);
        domesticScheduler.scheduleAtFixedRate(this::generateDomesticStatusReport, 0, 1, TimeUnit.HOURS);
        log.info("국내 자동매매 스케줄러 시작");
    }

    // 전략별로 1분 내 분산 실행 (최대 초당 10건 제한)
    private void scheduleDomesticStrategiesSequentially() {
        int strategyCount = (int) domesticStrategies.values().stream().filter(AutoTradingStrategy::isEnabled).count();
        if (strategyCount == 0) return;
        int intervalSec = Math.max(6, 60 / strategyCount); // 최소 6초 간격(10개면 6초, 5개면 12초)
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
     * 기술적 지표 계산 (실전 트레이딩 표준)
     */
    private Map<String, BigDecimal> calculateTechnicalIndicators(AutoTradingStrategy strategy, StockPriceResponse priceResponse) {
        List<PriceQueryRecord> priceHistory = priceQueryRecordService.findByStockCodeAndQueryType(
            strategy.getStockCode(), "daily");
        if (priceHistory.size() < 30) {
            return getFallbackIndicators(priceResponse);
        }
        List<PriceQueryRecord> recent = priceHistory.subList(0, Math.min(50, priceHistory.size()));
        List<BigDecimal> closes = recent.stream().map(r -> new BigDecimal(r.getCurrentPrice())).toList();
        List<BigDecimal> highs = recent.stream().map(r -> new BigDecimal(r.getHighPrice())).toList();
        List<BigDecimal> lows = recent.stream().map(r -> new BigDecimal(r.getLowPrice())).toList();

        Map<String, BigDecimal> indicators = new HashMap<>();
        // RSI(14) - Wilders Smoothing
        indicators.put("RSI", calculateRSI_Wilders(closes, 14));
        // EMA(12,26)
        List<BigDecimal> emaFastSeries = calculateEMA_Series(closes, 12);
        List<BigDecimal> emaSlowSeries = calculateEMA_Series(closes, 26);
        // MACD 시계열 및 Signal
        List<BigDecimal> macdSeries = new java.util.ArrayList<>();
        for (int i = 0; i < Math.min(emaFastSeries.size(), emaSlowSeries.size()); i++) {
            macdSeries.add(emaFastSeries.get(i).subtract(emaSlowSeries.get(i)));
        }
        List<BigDecimal> signalSeries = calculateEMA_Series(macdSeries, 9);
        // 최신값만 저장
        indicators.put("MACD", macdSeries.isEmpty() ? BigDecimal.ZERO : macdSeries.get(0));
        indicators.put("MACD_SIGNAL", signalSeries.isEmpty() ? BigDecimal.ZERO : signalSeries.get(0));
        // SMA/EMA(5,20)
        indicators.put("SMA_SHORT", calculateSMA(closes, 5));
        indicators.put("SMA_LONG", calculateSMA(closes, 20));
        indicators.put("EMA_SHORT", emaFastSeries.isEmpty() ? closes.get(0) : emaFastSeries.get(0));
        indicators.put("EMA_LONG", emaSlowSeries.isEmpty() ? closes.get(0) : emaSlowSeries.get(0));
        // 볼린저밴드(20,2) - bias correction, 중심선 EMA 옵션
        BigDecimal[] bb = calculateBollingerBands(closes, 20, 2, true, true);
        indicators.put("BB_UPPER", bb[0]);
        indicators.put("BB_LOWER", bb[1]);
        // 최근 고가/저가
        indicators.put("HIGH_20", highs.subList(0, Math.min(20, highs.size())).stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO));
        indicators.put("LOW_20", lows.subList(0, Math.min(20, lows.size())).stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO));
        return indicators;
    }
    // 임시값 반환 (데이터 부족 시)
    private Map<String, BigDecimal> getFallbackIndicators(StockPriceResponse priceResponse) {
        Map<String, BigDecimal> indicators = new HashMap<>();
        indicators.put("RSI", new BigDecimal("50"));
        indicators.put("MACD", new BigDecimal("0"));
        indicators.put("MACD_SIGNAL", new BigDecimal("0"));
        indicators.put("SMA_SHORT", new BigDecimal(priceResponse.getCurrentPrice()));
        indicators.put("SMA_LONG", new BigDecimal(priceResponse.getCurrentPrice()));
        indicators.put("BB_UPPER", new BigDecimal(priceResponse.getHighPrice()));
        indicators.put("BB_LOWER", new BigDecimal(priceResponse.getLowPrice()));
        indicators.put("HIGH_20", new BigDecimal(priceResponse.getHighPrice()));
        indicators.put("LOW_20", new BigDecimal(priceResponse.getLowPrice()));
        return indicators;
    }
    // RSI (Wilders Smoothing)
    private BigDecimal calculateRSI_Wilders(List<BigDecimal> closes, int period) {
        if (closes.size() < period + 1) return new BigDecimal("50");
        List<BigDecimal> deltas = new java.util.ArrayList<>();
        for (int i = closes.size() - 1; i > 0; i--) {
            deltas.add(closes.get(i - 1).subtract(closes.get(i)));
        }
        BigDecimal gain = BigDecimal.ZERO, loss = BigDecimal.ZERO;
        for (int i = 0; i < period; i++) {
            BigDecimal d = deltas.get(i);
            if (d.compareTo(BigDecimal.ZERO) > 0) gain = gain.add(d);
            else loss = loss.add(d.abs());
        }
        gain = gain.divide(BigDecimal.valueOf(period), 6, BigDecimal.ROUND_HALF_UP);
        loss = loss.divide(BigDecimal.valueOf(period), 6, BigDecimal.ROUND_HALF_UP);
        for (int i = period; i < deltas.size(); i++) {
            BigDecimal d = deltas.get(i);
            if (d.compareTo(BigDecimal.ZERO) > 0) {
                gain = (gain.multiply(BigDecimal.valueOf(period - 1)).add(d)).divide(BigDecimal.valueOf(period), 6, BigDecimal.ROUND_HALF_UP);
                loss = (loss.multiply(BigDecimal.valueOf(period - 1))).divide(BigDecimal.valueOf(period), 6, BigDecimal.ROUND_HALF_UP);
            } else {
                gain = (gain.multiply(BigDecimal.valueOf(period - 1))).divide(BigDecimal.valueOf(period), 6, BigDecimal.ROUND_HALF_UP);
                loss = (loss.multiply(BigDecimal.valueOf(period - 1)).add(d.abs())).divide(BigDecimal.valueOf(period), 6, BigDecimal.ROUND_HALF_UP);
            }
        }
        if (gain.add(loss).compareTo(BigDecimal.ZERO) == 0) return new BigDecimal("50");
        BigDecimal rs = gain.divide(loss.equals(BigDecimal.ZERO) ? BigDecimal.ONE : loss, 6, BigDecimal.ROUND_HALF_UP);
        return new BigDecimal(100).subtract(new BigDecimal(100).divide(rs.add(BigDecimal.ONE), 2, BigDecimal.ROUND_HALF_UP));
    }
    // SMA
    private BigDecimal calculateSMA(List<BigDecimal> closes, int period) {
        if (closes.size() < period) return closes.get(0);
        return closes.subList(0, period).stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(period), 2, BigDecimal.ROUND_HALF_UP);
    }
    // EMA 시계열 (초기값 SMA, 이후 k-factor)
    private List<BigDecimal> calculateEMA_Series(List<BigDecimal> closes, int period) {
        List<BigDecimal> emaSeries = new java.util.ArrayList<>();
        if (closes.size() < period) {
            emaSeries.add(closes.get(0));
            return emaSeries;
        }
        BigDecimal k = new BigDecimal(2.0 / (period + 1));
        // 초기값: SMA
        BigDecimal prevEma = calculateSMA(closes, period);
        emaSeries.add(prevEma);
        for (int i = period; i < closes.size(); i++) {
            BigDecimal price = closes.get(i);
            prevEma = price.subtract(prevEma).multiply(k).add(prevEma);
            emaSeries.add(0, prevEma.setScale(2, BigDecimal.ROUND_HALF_UP)); // 최신값이 앞에 오도록
        }
        return emaSeries;
    }
    // 볼린저밴드 (bias correction, 중심선 EMA 옵션, 신뢰구간 파라미터화)
    private BigDecimal[] calculateBollingerBands(List<BigDecimal> closes, int period, int sigma, boolean biasCorrection, boolean useEMA) {
        if (closes.size() < period) return new BigDecimal[]{closes.get(0), closes.get(0)};
        List<BigDecimal> window = closes.subList(0, period);
        BigDecimal center = useEMA ? calculateEMA_Series(window, period).get(0) : calculateSMA(window, period);
        // 분산(bias correction)
        BigDecimal variance = window.stream().map(c -> c.subtract(center).pow(2)).reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(biasCorrection ? period - 1 : period), 6, BigDecimal.ROUND_HALF_UP);
        BigDecimal std = new BigDecimal(Math.sqrt(variance.doubleValue())).setScale(2, BigDecimal.ROUND_HALF_UP);
        return new BigDecimal[]{center.add(std.multiply(BigDecimal.valueOf(sigma))), center.subtract(std.multiply(BigDecimal.valueOf(sigma)))};
    }
    
    /**
     * 매매 신호 분석 (고급화)
     */
    private TradingSignal analyzeTradingSignal(AutoTradingStrategy strategy, StockPriceResponse priceResponse, 
                                             Map<String, BigDecimal> indicators, TradingStatus status) {
        BigDecimal currentPrice = new BigDecimal(priceResponse.getCurrentPrice());
        BigDecimal rsi = indicators.get("RSI");
        BigDecimal macd = indicators.get("MACD");
        BigDecimal macdSignal = indicators.get("MACD_SIGNAL");
        BigDecimal smaShort = indicators.get("SMA_SHORT");
        BigDecimal smaLong = indicators.get("SMA_LONG");
        BigDecimal bbUpper = indicators.get("BB_UPPER");
        BigDecimal bbLower = indicators.get("BB_LOWER");
        BigDecimal high20 = indicators.get("HIGH_20");
        BigDecimal low20 = indicators.get("LOW_20");

        // 다중 지표 조합: RSI, MACD, 이동평균, 볼린저밴드, 고가/저가 돌파
        boolean buySignal = rsi.compareTo(strategy.getRsiOversold()) <= 0
                && macd.compareTo(macdSignal) > 0
                && smaShort.compareTo(smaLong) > 0
                && currentPrice.compareTo(bbLower) <= 0
                && currentPrice.compareTo(low20) <= 0;
        // 매도 신호: 보유 수량이 0 이하이면 무조건 HOLD
        if (status.currentPosition == null || status.currentPosition.compareTo(BigDecimal.ZERO) <= 0) {
            if (buySignal) return TradingSignal.BUY;
            return TradingSignal.HOLD;
        }
        boolean sellSignal = rsi.compareTo(strategy.getRsiOverbought()) >= 0
                && macd.compareTo(macdSignal) < 0
                && smaShort.compareTo(smaLong) < 0
                && currentPrice.compareTo(bbUpper) >= 0
                && currentPrice.compareTo(high20) >= 0;

        // 리스크 관리: 익절/손절/트레일링스탑
        BigDecimal profitRate = currentPrice.subtract(status.averagePrice)
                .divide(status.averagePrice.equals(BigDecimal.ZERO) ? BigDecimal.ONE : status.averagePrice, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(new BigDecimal("100"));
        boolean profitTargetReached = profitRate.compareTo(strategy.getProfitTarget()) >= 0;
        boolean stopLossTriggered = profitRate.compareTo(strategy.getStopLoss().negate()) <= 0;
        // 트레일링 스탑(최근 고점 대비 일정 % 하락 시 매도)
        boolean trailingStop = false;
        if (status.lastPrice != null && status.lastPrice.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal drop = status.lastPrice.subtract(currentPrice)
                    .divide(status.lastPrice, 4, BigDecimal.ROUND_HALF_UP)
                    .multiply(new BigDecimal("100"));
            trailingStop = drop.compareTo(new BigDecimal("3")) >= 0; // 3% 이상 하락 시
        }
        if (buySignal) return TradingSignal.BUY;
        if (sellSignal || profitTargetReached || stopLossTriggered || trailingStop) return TradingSignal.SELL;
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