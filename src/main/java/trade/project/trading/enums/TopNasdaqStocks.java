package trade.project.trading.enums;

import lombok.Getter;

/**
 * 나스닥 상위 20개 종목 Enum
 * 시가총액 기준 상위 종목들
 */
@Getter
public enum TopNasdaqStocks {
    
    // 기술주 (Technology)
    AAPL("AAPL", "Apple Inc.", "애플", "Technology", "컴퓨터 및 전자제품"),
    MSFT("MSFT", "Microsoft Corporation", "마이크로소프트", "Technology", "소프트웨어"),
    GOOGL("GOOGL", "Alphabet Inc.", "알파벳", "Technology", "인터넷 서비스"),
    AMZN("AMZN", "Amazon.com Inc.", "아마존", "Consumer Discretionary", "전자상거래"),
    NVDA("NVDA", "NVIDIA Corporation", "엔비디아", "Technology", "반도체"),
    META("META", "Meta Platforms Inc.", "메타", "Technology", "소셜미디어"),
    TSLA("TSLA", "Tesla Inc.", "테슬라", "Consumer Discretionary", "자동차"),
    NFLX("NFLX", "Netflix Inc.", "넷플릭스", "Communication Services", "스트리밍"),
    
    // 반도체 및 하드웨어
    AMD("AMD", "Advanced Micro Devices Inc.", "AMD", "Technology", "반도체"),
    INTC("INTC", "Intel Corporation", "인텔", "Technology", "반도체"),
    QCOM("QCOM", "QUALCOMM Incorporated", "퀄컴", "Technology", "반도체"),
    
    // 소프트웨어 및 클라우드
    CRM("CRM", "Salesforce Inc.", "세일즈포스", "Technology", "소프트웨어"),
    ADBE("ADBE", "Adobe Inc.", "어도비", "Technology", "소프트웨어"),
    ORCL("ORCL", "Oracle Corporation", "오라클", "Technology", "소프트웨어"),
    
    // 인터넷 및 전자상거래
    PYPL("PYPL", "PayPal Holdings Inc.", "페이팔", "Technology", "결제서비스"),
    EBAY("EBAY", "eBay Inc.", "이베이", "Consumer Discretionary", "전자상거래"),
    
    // 바이오테크
    GILD("GILD", "Gilead Sciences Inc.", "길리어드 사이언스", "Healthcare", "제약"),
    REGN("REGN", "Regeneron Pharmaceuticals Inc.", "리제네론", "Healthcare", "제약"),
    
    // 기타
    COST("COST", "Costco Wholesale Corporation", "코스트코", "Consumer Staples", "도매"),
    PEP("PEP", "PepsiCo Inc.", "펩시코", "Consumer Staples", "음료");
    
    private final String stockCode;
    private final String companyName;
    private final String koreanName;
    private final String sector;
    private final String description;
    
    TopNasdaqStocks(String stockCode, String companyName, String koreanName, String sector, String description) {
        this.stockCode = stockCode;
        this.companyName = companyName;
        this.koreanName = koreanName;
        this.sector = sector;
        this.description = description;
    }
    
    /**
     * 종목코드로 Enum 찾기
     */
    public static TopNasdaqStocks fromStockCode(String stockCode) {
        for (TopNasdaqStocks stock : values()) {
            if (stock.getStockCode().equals(stockCode)) {
                return stock;
            }
        }
        throw new IllegalArgumentException("Unknown stock code: " + stockCode);
    }
    
    /**
     * 섹터별 종목 목록 조회
     */
    public static TopNasdaqStocks[] getBySector(String sector) {
        return java.util.Arrays.stream(values())
                .filter(stock -> stock.getSector().equals(sector))
                .toArray(TopNasdaqStocks[]::new);
    }
    
    /**
     * 모든 섹터 목록 조회
     */
    public static String[] getAllSectors() {
        return java.util.Arrays.stream(values())
                .map(TopNasdaqStocks::getSector)
                .distinct()
                .toArray(String[]::new);
    }
} 