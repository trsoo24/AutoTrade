package trade.project.api.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import trade.project.api.entity.Stock;
import trade.project.api.repository.StockRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StockInfoCacheService {
    private final StockRepository stockRepository;
    private Map<String, String> stockCodeNameMap = new HashMap<>();

    @PostConstruct
    public void loadStockInfo() {
        List<Stock> stocks = stockRepository.findAll();
        Map<String, String> map = new HashMap<>();
        for (Stock stock : stocks) {
            map.put(stock.getStockCode(), stock.getStockName());
        }
        this.stockCodeNameMap = map;
    }

    public String getStockName(String stockCode) {
        return stockCodeNameMap.getOrDefault(stockCode, stockCode);
    }
} 