package trade.project.trading.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import trade.project.trading.entity.TradingRecord;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TradingRecordRepository extends JpaRepository<TradingRecord, Long> {
    
    /**
     * 주문번호로 매매 기록 조회
     */
    Optional<TradingRecord> findByOrderNumber(String orderNumber);
    
    /**
     * 계좌번호로 매매 기록 목록 조회
     */
    List<TradingRecord> findByAccountNumberOrderByOrderDateTimeDesc(String accountNumber);
    
    /**
     * 종목코드로 매매 기록 목록 조회
     */
    List<TradingRecord> findByStockCodeOrderByOrderDateTimeDesc(String stockCode);
    
    /**
     * 주문구분으로 매매 기록 목록 조회
     */
    List<TradingRecord> findByOrderTypeOrderByOrderDateTimeDesc(String orderType);
    
    /**
     * 주문상태로 매매 기록 목록 조회
     */
    List<TradingRecord> findByOrderStatusOrderByOrderDateTimeDesc(String orderStatus);
    
    /**
     * 계좌번호와 종목코드로 매매 기록 목록 조회
     */
    List<TradingRecord> findByAccountNumberAndStockCodeOrderByOrderDateTimeDesc(
            String accountNumber, String stockCode);
    
    /**
     * 기간별 매매 기록 조회
     */
    List<TradingRecord> findByOrderDateTimeBetweenOrderByOrderDateTimeDesc(
            LocalDateTime startDateTime, LocalDateTime endDateTime);
    
    /**
     * 계좌번호와 기간으로 매매 기록 조회
     */
    List<TradingRecord> findByAccountNumberAndOrderDateTimeBetweenOrderByOrderDateTimeDesc(
            String accountNumber, LocalDateTime startDateTime, LocalDateTime endDateTime);
    
    /**
     * 종목코드와 기간으로 매매 기록 조회
     */
    List<TradingRecord> findByStockCodeAndOrderDateTimeBetweenOrderByOrderDateTimeDesc(
            String stockCode, LocalDateTime startDateTime, LocalDateTime endDateTime);
    
    /**
     * 에러가 발생한 매매 기록 조회
     */
    List<TradingRecord> findByErrorCodeIsNotNullOrderByOrderDateTimeDesc();
    
    /**
     * 성공한 매매 기록 조회
     */
    List<TradingRecord> findByErrorCodeIsNullOrderByOrderDateTimeDesc();
    
    /**
     * 특정 계좌의 성공한 매매 기록 조회
     */
    List<TradingRecord> findByAccountNumberAndErrorCodeIsNullOrderByOrderDateTimeDesc(String accountNumber);
    
    /**
     * 특정 종목의 성공한 매매 기록 조회
     */
    List<TradingRecord> findByStockCodeAndErrorCodeIsNullOrderByOrderDateTimeDesc(String stockCode);
    
    /**
     * 매매 기록 통계 조회 (계좌별)
     */
    @Query("SELECT t.orderType, COUNT(t), SUM(t.quantity), SUM(t.totalAmount) " +
           "FROM TradingRecord t " +
           "WHERE t.accountNumber = :accountNumber AND t.errorCode IS NULL " +
           "GROUP BY t.orderType")
    List<Object[]> getTradingStatisticsByAccount(@Param("accountNumber") String accountNumber);
    
    /**
     * 매매 기록 통계 조회 (종목별)
     */
    @Query("SELECT t.stockCode, t.stockName, t.orderType, COUNT(t), SUM(t.quantity), SUM(t.totalAmount) " +
           "FROM TradingRecord t " +
           "WHERE t.stockCode = :stockCode AND t.errorCode IS NULL " +
           "GROUP BY t.stockCode, t.stockName, t.orderType")
    List<Object[]> getTradingStatisticsByStock(@Param("stockCode") String stockCode);
    
    /**
     * 최근 매매 기록 조회 (최근 N개)
     */
    @Query("SELECT t FROM TradingRecord t ORDER BY t.orderDateTime DESC")
    List<TradingRecord> findRecentTradingRecords(org.springframework.data.domain.Pageable pageable);
} 