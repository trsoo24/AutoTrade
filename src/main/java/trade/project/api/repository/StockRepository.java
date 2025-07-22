package trade.project.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import trade.project.api.entity.Stock;

import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {
    Optional<Stock> findByStockCode(String stockCode);
} 