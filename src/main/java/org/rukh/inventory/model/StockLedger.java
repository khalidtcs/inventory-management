package org.rukh.inventory.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "stock_ledger")
public class StockLedger extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    
    @Column(nullable = false, unique = true)
    public String productCode;
    
    @Column(nullable = false)
    public String productName;
    
    @Column(nullable = false)
    public BigDecimal currentStock = BigDecimal.ZERO;
    
    @Column(nullable = false)
    public BigDecimal reservedStock = BigDecimal.ZERO;
    
    @Column(nullable = false)
    public BigDecimal availableStock = BigDecimal.ZERO;
    
    @Column(nullable = false)
    public BigDecimal incomingStock = BigDecimal.ZERO;
    
    @Column
    public String unit; // e.g., "pcs", "kg", "liters"
    
    @Column
    public BigDecimal reorderLevel;
    
    @Column(nullable = false)
    public LocalDateTime lastUpdated;
    
    @Version
    public Long version; // For optimistic locking
    
    @PrePersist
    @PreUpdate
    public void calculateAvailableStock() {
        this.availableStock = this.currentStock.subtract(this.reservedStock);
        this.lastUpdated = LocalDateTime.now();
    }

}
