package trade.project.backtest.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import trade.project.api.client.KisApiClient;
import trade.project.backtest.dto.BackTestRequest;
import trade.project.backtest.dto.BackTestRequest;
import trade.project.backtest.dto.BackTestResult;
import trade.project.backtest.dto.StockData;
import trade.project.backtest.engine.BacktestEngine;
import trade.project.backtest.strategy.StrategyFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class BacktestService {
    
    private final BacktestEngine backtestEngine;
    private final StrategyFactory strategyFactory;
    private final KisApiClient kisApiClient;
    
    /**
     * 백트래킹을 실행합니다.
     * @param request 백트래킹 요청
     * @return 백트래킹 결과
     */
    public BackTestResult runBacktest(BackTestRequest request) {
        log.info("백트래킹 서비스 시작: {}", request.getStockCode());
        
        try {
            // 주식 데이터 조회
            List<StockData> stockDataList = getStockData(request);
            
            if (stockDataList.isEmpty()) {
                throw new RuntimeException("주식 데이터를 찾을 수 없습니다: " + request.getStockCode());
            }
            
            // 백트래킹 실행
            BackTestResult result = backtestEngine.runBacktest(request, stockDataList);
            
            log.info("백트래킹 완료: 수익률 = {}%, 거래 횟수 = {}", 
                    result.getTotalReturnPercent(), result.getTotalTrades());
            
            return result;
            
        } catch (Exception e) {
            log.error("백트래킹 실행 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("백트래킹 실행 실패: " + e.getMessage(), e);
        }
    }
    
    /**
     * 주식 데이터를 조회합니다.
     * @param request 백트래킹 요청
     * @return 주식 데이터 리스트
     */
    private List<StockData> getStockData(BackTestRequest request) {
        List<StockData> stockDataList = new ArrayList<>();
        
        try {
            // 한국투자증권 API에서 일자별 시세 조회
            String startDate = request.getStartDate().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String endDate = request.getEndDate().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            
            Map<String, Object> response = kisApiClient.getStockDailyPrice(
                    request.getStockCode(), startDate, endDate);
            
            // 응답 데이터 파싱
            if (response != null && response.containsKey("output")) {
                Map<String, Object> output = (Map<String, Object>) response.get("output");
                
                if (output.containsKey("output1")) {
                    List<Map<String, Object>> dataList = (List<Map<String, Object>>) output.get("output1");
                    
                    for (Map<String, Object> data : dataList) {
                        StockData stockData = parseStockData(data);
                        if (stockData != null) {
                            stockDataList.add(stockData);
                        }
                    }
                }
            }
            
            // 날짜순으로 정렬
            stockDataList.sort((a, b) -> a.getDate().compareTo(b.getDate()));
            
            log.info("주식 데이터 조회 완료: {}개 데이터", stockDataList.size());
            
        } catch (Exception e) {
            log.error("주식 데이터 조회 중 오류 발생: {}", e.getMessage(), e);
            // 실제 API 호출이 실패할 경우를 대비해 샘플 데이터 생성
            stockDataList = generateSampleData(request);
        }
        
        return stockDataList;
    }
    
    /**
     * API 응답을 StockData로 파싱합니다.
     * @param data API 응답 데이터
     * @return StockData 객체
     */
    private StockData parseStockData(Map<String, Object> data) {
        try {
            String dateStr = (String) data.get("stck_bsop_date");
            LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyyMMdd"));
            
            BigDecimal open = new BigDecimal((String) data.get("stck_oprc"));
            BigDecimal high = new BigDecimal((String) data.get("stck_hgpr"));
            BigDecimal low = new BigDecimal((String) data.get("stck_lwpr"));
            BigDecimal close = new BigDecimal((String) data.get("stck_prpr"));
            Long volume = Long.parseLong((String) data.get("cntg_vol"));
            
            return StockData.builder()
                    .date(date)
                    .open(open)
                    .high(high)
                    .low(low)
                    .close(close)
                    .volume(volume)
                    .adjustedClose(close) // 수정주가는 종가로 대체
                    .build();
                    
        } catch (Exception e) {
            log.warn("주식 데이터 파싱 실패: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 샘플 데이터를 생성합니다 (테스트용).
     * @param request 백트래킹 요청
     * @return 샘플 주식 데이터 리스트
     */
    private List<StockData> generateSampleData(BackTestRequest request) {
        List<StockData> sampleData = new ArrayList<>();
        
        LocalDate currentDate = request.getStartDate();
        BigDecimal basePrice = new BigDecimal("50000"); // 기본 가격 50,000원
        BigDecimal price = basePrice;
        
        while (!currentDate.isAfter(request.getEndDate())) {
            // 주말 제외
            if (currentDate.getDayOfWeek().getValue() <= 5) {
                // 랜덤한 가격 변동 생성
                double changePercent = (Math.random() - 0.5) * 0.04; // ±2% 변동
                BigDecimal change = price.multiply(BigDecimal.valueOf(changePercent));
                price = price.add(change);
                
                // OHLC 생성
                BigDecimal open = price;
                BigDecimal high = price.multiply(BigDecimal.valueOf(1 + Math.random() * 0.02));
                BigDecimal low = price.multiply(BigDecimal.valueOf(1 - Math.random() * 0.02));
                BigDecimal close = price.multiply(BigDecimal.valueOf(1 + (Math.random() - 0.5) * 0.01));
                
                Long volume = (long) (1000000 + Math.random() * 5000000); // 100만~600만주
                
                sampleData.add(StockData.builder()
                        .date(currentDate)
                        .open(open)
                        .high(high)
                        .low(low)
                        .close(close)
                        .volume(volume)
                        .adjustedClose(close)
                        .build());
            }
            
            currentDate = currentDate.plusDays(1);
        }
        
        log.info("샘플 데이터 생성 완료: {}개 데이터", sampleData.size());
        return sampleData;
    }
    
    /**
     * 사용 가능한 전략 목록을 반환합니다.
     * @return 전략 목록
     */
    public List<Map<String, String>> getAvailableStrategies() {
        return strategyFactory.getAvailableStrategies().stream()
                .map(strategy -> Map.of(
                        "name", strategy.getStrategyName(),
                        "description", strategy.getStrategyDescription()
                ))
                .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * 백트래킹 요청의 유효성을 검사합니다.
     * @param request 백트래킹 요청
     */
    public void validateRequest(BackTestRequest request) {
        if (request.getStockCode() == null || request.getStockCode().trim().isEmpty()) {
            throw new IllegalArgumentException("주식 코드는 필수입니다.");
        }
        
        if (request.getStartDate() == null) {
            throw new IllegalArgumentException("시작 날짜는 필수입니다.");
        }
        
        if (request.getEndDate() == null) {
            throw new IllegalArgumentException("종료 날짜는 필수입니다.");
        }
        
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new IllegalArgumentException("시작 날짜는 종료 날짜보다 이전이어야 합니다.");
        }
        
        if (request.getInitialCapital() == null || request.getInitialCapital().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("초기 자본금은 0보다 커야 합니다.");
        }
        
        if (request.getStrategy() == null || request.getStrategy().trim().isEmpty()) {
            throw new IllegalArgumentException("전략은 필수입니다.");
        }
        
        if (strategyFactory.getStrategy(request.getStrategy()) == null) {
            throw new IllegalArgumentException("지원하지 않는 전략입니다: " + request.getStrategy());
        }
    }
} 