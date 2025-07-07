package trade.project.trading.enums;

import lombok.Getter;
import lombok.AllArgsConstructor;

import java.time.LocalTime;

/**
 * 자동매매 스케줄링 설정
 * 전문적인 트레이딩 관점에서 최적화된 시간대 설정
 */
@Getter
@AllArgsConstructor
public enum TradingSchedule {
    
    // 장 시작 전 준비 시간 (8:30-9:00)
    PRE_MARKET_OPEN("PRE_MARKET_OPEN", LocalTime.of(8, 30), LocalTime.of(9, 0), 
            "장 시작 전 준비", 30, false),
    
    // 장 시작 직후 (9:00-9:30) - 급등락 주의
    MARKET_OPEN("MARKET_OPEN", LocalTime.of(9, 0), LocalTime.of(9, 30), 
            "장 시작 직후", 10, true),
    
    // 오전 거래 시간 (9:30-11:30)
    MORNING_SESSION("MORNING_SESSION", LocalTime.of(9, 30), LocalTime.of(11, 30), 
            "오전 거래", 60, false),
    
    // 점심시간 (11:30-13:00) - 거래량 감소
    LUNCH_BREAK("LUNCH_BREAK", LocalTime.of(11, 30), LocalTime.of(13, 0), 
            "점심시간", 90, false),
    
    // 오후 거래 시간 (13:00-14:30)
    AFTERNOON_SESSION("AFTERNOON_SESSION", LocalTime.of(13, 0), LocalTime.of(14, 30), 
            "오후 거래", 60, false),
    
    // 장 마감 직전 (14:30-15:00) - 급등락 주의
    MARKET_CLOSE("MARKET_CLOSE", LocalTime.of(14, 30), LocalTime.of(15, 0), 
            "장 마감 직전", 10, true),
    
    // 장 마감 후 정리 (15:00-15:30)
    POST_MARKET_CLOSE("POST_MARKET_CLOSE", LocalTime.of(15, 0), LocalTime.of(15, 30), 
            "장 마감 후 정리", 30, false),
    
    // 야간 모니터링 (15:30-18:00) - 긴급 상황 대응
    EVENING_MONITORING("EVENING_MONITORING", LocalTime.of(15, 30), LocalTime.of(18, 0), 
            "야간 모니터링", 120, false),
    
    // 긴급 모니터링 (18:00-24:00) - 글로벌 시장 영향
    EMERGENCY_MONITORING("EMERGENCY_MONITORING", LocalTime.of(18, 0), LocalTime.of(0, 0), 
            "긴급 모니터링", 180, false);
    
    private final String scheduleName;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final String description;
    private final int intervalSeconds; // 시세 조회 주기 (초)
    private final boolean highFrequency; // 고빈도 모니터링 여부
    
    /**
     * 현재 시간에 해당하는 스케줄 조회
     */
    public static TradingSchedule getCurrentSchedule() {
        LocalTime now = LocalTime.now();
        
        for (TradingSchedule schedule : values()) {
            if (schedule.isInTimeRange(now)) {
                return schedule;
            }
        }
        
        // 기본값: 야간 모니터링
        return EVENING_MONITORING;
    }
    
    /**
     * 현재 시간이 해당 스케줄 시간대에 포함되는지 확인
     */
    public boolean isInTimeRange(LocalTime time) {
        if (startTime.isBefore(endTime)) {
            // 같은 날 내의 시간대
            return !time.isBefore(startTime) && !time.isAfter(endTime);
        } else {
            // 자정을 넘어가는 시간대 (긴급 모니터링)
            return !time.isBefore(startTime) || !time.isAfter(endTime);
        }
    }
    
    /**
     * 장 시간인지 확인 (9:00-15:00)
     */
    public static boolean isMarketHours() {
        LocalTime now = LocalTime.now();
        return !now.isBefore(LocalTime.of(9, 0)) && !now.isAfter(LocalTime.of(15, 0));
    }
    
    /**
     * 거래 가능 시간인지 확인 (9:00-15:30)
     */
    public static boolean isTradingHours() {
        LocalTime now = LocalTime.now();
        return !now.isBefore(LocalTime.of(9, 0)) && !now.isAfter(LocalTime.of(15, 30));
    }
    
    /**
     * 고빈도 모니터링이 필요한 시간대인지 확인
     */
    public static boolean isHighFrequencyTime() {
        TradingSchedule current = getCurrentSchedule();
        return current.isHighFrequency();
    }
    
    /**
     * 시세 조회 주기 (밀리초) 반환
     */
    public long getIntervalMillis() {
        return intervalSeconds * 1000L;
    }
    
    /**
     * 다음 스케줄까지 대기 시간 (밀리초) 계산
     */
    public long getWaitTimeToNextSchedule() {
        LocalTime now = LocalTime.now();
        LocalTime nextStart = getNextSchedule().getStartTime();
        
        if (nextStart.isBefore(now)) {
            // 다음날로 넘어가는 경우
            return java.time.Duration.between(now, LocalTime.MAX).toMillis() + 
                   java.time.Duration.between(LocalTime.MIN, nextStart).toMillis();
        } else {
            return java.time.Duration.between(now, nextStart).toMillis();
        }
    }
    
    /**
     * 다음 스케줄 조회
     */
    public TradingSchedule getNextSchedule() {
        TradingSchedule[] schedules = values();
        int currentIndex = java.util.Arrays.asList(schedules).indexOf(this);
        int nextIndex = (currentIndex + 1) % schedules.length;
        return schedules[nextIndex];
    }
    
    /**
     * 이전 스케줄 조회
     */
    public TradingSchedule getPreviousSchedule() {
        TradingSchedule[] schedules = values();
        int currentIndex = java.util.Arrays.asList(schedules).indexOf(this);
        int prevIndex = (currentIndex - 1 + schedules.length) % schedules.length;
        return schedules[prevIndex];
    }
} 