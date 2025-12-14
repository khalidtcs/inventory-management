package org.rukh.inventory.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "stock_transaction")
public class StockTransaction extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    
    @Column(nullable = false)
    public String productCode;
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    public TransactionType type;
    
    @Column(nullable = false)
    public BigDecimal quantity;
    
    @Column(nullable = false)
    public BigDecimal balanceAfter;
    
    @Column
    public String referenceNumber;
    
    @Column
    public String notes;
    
    @Column(nullable = false)
    public LocalDateTime transactionDate;
    
    @Column
    public String performedBy;
    
    public enum TransactionType {
        INCOMING,      // New stock arrival
        RESERVE,       // Reserve stock for order
        RELEASE,       // Release reserved stock (order cancelled)
        DISPATCH,      // Actual stock dispatch (reduce current)
        ADJUSTMENT,    // Manual adjustment
        RETURN         // Customer return
    }

}
