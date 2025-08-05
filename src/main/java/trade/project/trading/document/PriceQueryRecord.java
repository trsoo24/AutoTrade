package trade.project.trading.document;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;

@Document(collection = "price_query_records")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceQueryRecord {
    
    @Id
    private String id;
    
    @Indexed
    private String stockCode; // 종목코드
    
    @Indexed
    private String stockName; // 종목명
    
    @Indexed
    private String queryType; // 조회유형 (current, daily)
    
    private Integer currentPrice; // 현재가
    private Integer previousClose; // 전일종가
    private Integer openPrice; // 시가
    private Integer highPrice; // 고가
    private Integer lowPrice; // 저가
    private Long tradingVolume; // 거래량
    private Long tradingValue; // 거래대금
    private Double changeRate; // 등락률
    private Integer changeAmount; // 등락폭
    private String marketStatus; // 시장상태
    
    // 일자별 시세 조회 시 사용
    private String startDate; // 시작일자
    private String endDate; // 종료일자
    private Integer dailyCount; // 일자별 데이터 개수
    
    @Indexed
    private LocalDateTime queryDateTime; // 조회일시
    
    private String userAgent; // 사용자 에이전트
    private String clientIp; // 클라이언트 IP
    private String sessionId; // 세션 ID
    
    private String errorCode; // 에러코드
    private String errorMessage; // 에러메시지
    
    private String apiResponse; // API 응답 전체 JSON
    
    @Indexed
    private LocalDateTime createdAt; // 생성일시
    
    private LocalDateTime updatedAt; // 수정일시
    
    // 생성 시 자동으로 시간 설정
    public void setCreatedAt() {
        this.createdAt = LocalDateTime.now();
        this.queryDateTime = LocalDateTime.now();
    }
    
    public void setUpdatedAt() {
        this.updatedAt = LocalDateTime.now();
    }
} 