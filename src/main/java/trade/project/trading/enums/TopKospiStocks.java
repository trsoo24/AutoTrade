package trade.project.trading.enums;

import lombok.Getter;
import lombok.AllArgsConstructor;

/**
 * KOSPI 상위 20개 종목 Enum
 * 시가총액 기준 상위 종목들 (2024년 기준)
 */
@Getter
@AllArgsConstructor
public enum TopKospiStocks {
    
    // 1-10위
    SAMSUNG_ELECTRONICS("005930", "삼성전자", "전자"),
    SK_HYNIX("000660", "SK하이닉스", "전자"),
    NAVER("035420", "NAVER", "서비스업"),
    LG_CHEM("051910", "LG화학", "화학"),
    SAMSUNG_SDI("006400", "삼성SDI", "전기전자"),
    KAKAO("035720", "카카오", "서비스업"),
    HYUNDAI_MOTOR("005380", "현대차", "운수장비"),
    LG_ENERGY_SOLUTION("373220", "LG에너지솔루션", "전기전자"),
    KIA("000270", "기아", "운수장비"),
    POSCO_HOLDINGS("005490", "POSCO홀딩스", "철강금속"),
    
    // 11-20위
    KB_FINANCIAL("105560", "KB금융", "은행"),
    SHINHAN_FINANCIAL("055550", "신한지주", "은행"),
    HANWHA("000880", "한화", "화학"),
    LG("003550", "LG", "전자"),
    SAMSUNG_BIOLOGICS("207940", "삼성바이오로직스", "의약품"),
    CELLTRION("068270", "셀트리온", "의약품"),
    AMOREPACIFIC("090430", "아모레퍼시픽", "화학"),
    LOTTE_SHOPPING("023530", "롯데쇼핑", "유통업"),
    LOTTE_CHEMICAL("051900", "롯데케미칼", "화학"),
    HYUNDAI_MOBIS("012330", "현대모비스", "운수장비");
    
    private final String stockCode;
    private final String stockName;
    private final String sector;
    
    /**
     * 종목코드로 Enum 찾기
     */
    public static TopKospiStocks findByStockCode(String stockCode) {
        for (TopKospiStocks stock : values()) {
            if (stock.getStockCode().equals(stockCode)) {
                return stock;
            }
        }
        return null;
    }
    
    /**
     * 종목명으로 Enum 찾기
     */
    public static TopKospiStocks findByStockName(String stockName) {
        for (TopKospiStocks stock : values()) {
            if (stock.getStockName().equals(stockName)) {
                return stock;
            }
        }
        return null;
    }
    
    /**
     * 섹터별 종목 목록 조회
     */
    public static TopKospiStocks[] findBySector(String sector) {
        return java.util.Arrays.stream(values())
                .filter(stock -> stock.getSector().equals(sector))
                .toArray(TopKospiStocks[]::new);
    }
    
    /**
     * 모든 종목코드 배열 반환
     */
    public static String[] getAllStockCodes() {
        return java.util.Arrays.stream(values())
                .map(TopKospiStocks::getStockCode)
                .toArray(String[]::new);
    }
    
    /**
     * 모든 종목명 배열 반환
     */
    public static String[] getAllStockNames() {
        return java.util.Arrays.stream(values())
                .map(TopKospiStocks::getStockName)
                .toArray(String[]::new);
    }
    
    /**
     * 섹터 목록 반환
     */
    public static String[] getAllSectors() {
        return java.util.Arrays.stream(values())
                .map(TopKospiStocks::getSector)
                .distinct()
                .toArray(String[]::new);
    }
} 