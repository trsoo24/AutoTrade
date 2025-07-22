package trade.project.api.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.math.BigDecimal;

@Entity
@Table(name = "stocks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Stock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long stockId;

    @Column(name = "stock_code", unique = true, nullable = false, length = 10)
    private String stockCode;

    @Column(name = "stock_name", nullable = false, length = 100)
    private String stockName;

    @Column(name = "market_type", nullable = false, length = 20)
    private String marketType;

    @Column(name = "sector", length = 100)
    private String sector;

    @Column(name = "industry", length = 100)
    private String industry;

    @Column(name = "company_name", length = 200)
    private String companyName;

    @Column(name = "listing_date")
    private LocalDate listingDate;

    @Column(name = "delisting_date")
    private LocalDate delistingDate;

    @Column(name = "par_value")
    private BigDecimal parValue;

    @Column(name = "total_shares")
    private Long totalShares;

    @Column(name = "market_cap")
    private BigDecimal marketCap;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "created_at")
    private java.sql.Timestamp createdAt;

    @Column(name = "updated_at")
    private java.sql.Timestamp updatedAt;
} 