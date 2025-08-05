package trade.project.trading.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import trade.project.trading.document.PriceQueryRecord;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PriceQueryRecordRepository extends MongoRepository<PriceQueryRecord, String> {
    
    /**
     * 종목코드로 시세 조회 기록 조회
     */
    List<PriceQueryRecord> findByStockCodeOrderByQueryDateTimeDesc(String stockCode);
    
    /**
     * 조회유형으로 시세 조회 기록 조회
     */
    List<PriceQueryRecord> findByQueryTypeOrderByQueryDateTimeDesc(String queryType);
    
    /**
     * 종목코드와 조회유형으로 시세 조회 기록 조회
     */
    List<PriceQueryRecord> findByStockCodeAndQueryTypeOrderByQueryDateTimeDesc(String stockCode, String queryType);
    
    /**
     * 기간별 시세 조회 기록 조회
     */
    List<PriceQueryRecord> findByQueryDateTimeBetweenOrderByQueryDateTimeDesc(
            LocalDateTime startDateTime, LocalDateTime endDateTime);
    
    /**
     * 종목코드와 기간으로 시세 조회 기록 조회
     */
    List<PriceQueryRecord> findByStockCodeAndQueryDateTimeBetweenOrderByQueryDateTimeDesc(
            String stockCode, LocalDateTime startDateTime, LocalDateTime endDateTime);
    
    /**
     * 조회유형과 기간으로 시세 조회 기록 조회
     */
    List<PriceQueryRecord> findByQueryTypeAndQueryDateTimeBetweenOrderByQueryDateTimeDesc(
            String queryType, LocalDateTime startDateTime, LocalDateTime endDateTime);
    
    /**
     * 에러가 발생한 시세 조회 기록 조회
     */
    List<PriceQueryRecord> findByErrorCodeIsNotNullOrderByQueryDateTimeDesc();
    
    /**
     * 성공한 시세 조회 기록 조회
     */
    List<PriceQueryRecord> findByErrorCodeIsNullOrderByQueryDateTimeDesc();
    
    /**
     * 특정 종목의 성공한 시세 조회 기록 조회
     */
    List<PriceQueryRecord> findByStockCodeAndErrorCodeIsNullOrderByQueryDateTimeDesc(String stockCode);
    
    /**
     * 특정 조회유형의 성공한 시세 조회 기록 조회
     */
    List<PriceQueryRecord> findByQueryTypeAndErrorCodeIsNullOrderByQueryDateTimeDesc(String queryType);
    
    /**
     * 클라이언트 IP로 시세 조회 기록 조회
     */
    List<PriceQueryRecord> findByClientIpOrderByQueryDateTimeDesc(String clientIp);
    
    /**
     * 세션 ID로 시세 조회 기록 조회
     */
    List<PriceQueryRecord> findBySessionIdOrderByQueryDateTimeDesc(String sessionId);
    
    /**
     * 시세 조회 통계 조회 (종목별)
     */
    @Query(value = "{}", fields = "{'stockCode': 1, 'stockName': 1, 'queryType': 1, 'queryDateTime': 1}")
    List<PriceQueryRecord> findDistinctByStockCodeAndQueryType();
    
    /**
     * 최근 시세 조회 기록 조회 (최근 N개)
     */
    List<PriceQueryRecord> findTop10ByOrderByQueryDateTimeDesc();
    
    /**
     * 특정 종목의 최근 시세 조회 기록 조회
     */
    List<PriceQueryRecord> findTop5ByStockCodeOrderByQueryDateTimeDesc(String stockCode);
    
    /**
     * 특정 종목의 현재가 조회 기록만 조회
     */
    List<PriceQueryRecord> findByStockCodeAndQueryTypeAndStartDateIsNullOrderByQueryDateTimeDesc(String stockCode, String queryType);
    
    /**
     * 특정 종목의 일자별 시세 조회 기록만 조회
     */
    List<PriceQueryRecord> findByStockCodeAndQueryTypeAndStartDateIsNotNullOrderByQueryDateTimeDesc(String stockCode, String queryType);
    
    /**
     * 시세 조회 빈도 통계 (종목별)
     */
    @Query(value = "{}", fields = "{'stockCode': 1, 'stockName': 1, 'queryType': 1}")
    List<PriceQueryRecord> findDistinctByStockCodeAndStockNameAndQueryType();
    
    /**
     * 특정 기간의 시세 조회 기록 수 조회
     */
    long countByQueryDateTimeBetween(LocalDateTime startDateTime, LocalDateTime endDateTime);
    
    /**
     * 특정 종목의 시세 조회 기록 수 조회
     */
    long countByStockCode(String stockCode);
    
    /**
     * 특정 조회유형의 시세 조회 기록 수 조회
     */
    long countByQueryType(String queryType);
    
    /**
     * 에러가 발생한 시세 조회 기록 수 조회
     */
    long countByErrorCodeIsNotNull();
    
    /**
     * 성공한 시세 조회 기록 수 조회
     */
    long countByErrorCodeIsNull();
} 