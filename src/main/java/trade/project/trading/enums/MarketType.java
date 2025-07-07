package trade.project.trading.enums;

import lombok.Getter;

/**
 * 시장 타입 Enum
 */
@Getter
public enum MarketType {
    
    DOMESTIC("DOMESTIC", "국내", "KOSPI, KOSDAQ 등 국내 주식 시장"),
    FOREIGN("FOREIGN", "해외", "NASDAQ, NYSE 등 해외 주식 시장");
    
    private final String code;
    private final String koreanName;
    private final String description;
    
    MarketType(String code, String koreanName, String description) {
        this.code = code;
        this.koreanName = koreanName;
        this.description = description;
    }
    
    /**
     * 코드로 Enum 찾기
     */
    public static MarketType fromCode(String code) {
        for (MarketType marketType : values()) {
            if (marketType.getCode().equals(code)) {
                return marketType;
            }
        }
        throw new IllegalArgumentException("Unknown market type code: " + code);
    }
} 