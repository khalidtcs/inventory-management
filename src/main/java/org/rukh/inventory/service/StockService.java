package org.rukh.inventory.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.jboss.logging.Logger;
import org.rukh.inventory.model.StockLedger;
import org.rukh.inventory.model.StockTransaction;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;

@ApplicationScoped
public class StockService {

     @Inject
    Logger log;
    
    public Uni<StockLedger> getStockByProductCode(String productCode) {
        return StockLedger.<StockLedger>find("productCode", productCode).firstResult();
    }
    
    public Uni<StockLedger> addIncomingStock(String productCode, BigDecimal quantity, 
                                             String referenceNumber, String performedBy) {
        return Panache.withTransaction(() -> 
            getOrCreateStock(productCode)
                .flatMap(stock -> {
                    stock.incomingStock = stock.incomingStock.add(quantity);
                    stock.currentStock = stock.currentStock.add(quantity);
                    
                    return Panache.getSession()
                        .flatMap(session -> session.persist(stock)
                            .flatMap(v -> session.flush())
                            .map(v -> stock))
                        .flatMap(s -> createTransaction(
                            productCode, 
                            StockTransaction.TransactionType.INCOMING,
                            quantity,
                            s.currentStock,
                            referenceNumber,
                            "Stock incoming",
                            performedBy
                        ).map(t -> s));
                })
        );
    }
    
    public Uni<StockLedger> reserveStock(String productCode, BigDecimal quantity,
                                        String referenceNumber, String performedBy) {
        return Panache.withTransaction(() ->
            getStockByProductCode(productCode)
                .onItem().ifNull().failWith(new WebApplicationException("Product not found", 404))
                .flatMap(stock -> {
                    if (stock.availableStock.compareTo(quantity) < 0) {
                        return Uni.createFrom().failure(
                            new WebApplicationException("Insufficient available stock", 400)
                        );
                    }
                    
                    stock.reservedStock = stock.reservedStock.add(quantity);
                    
                    return Panache.getSession()
                        .flatMap(session -> session.persist(stock)
                            .flatMap(v -> session.flush())
                            .map(v -> stock))
                        .flatMap(s -> createTransaction(
                            productCode,
                            StockTransaction.TransactionType.RESERVE,
                            quantity,
                            s.reservedStock,
                            referenceNumber,
                            "Stock reserved",
                            performedBy
                        ).map(t -> s));
                })
        );
    }
    
    public Uni<StockLedger> releaseReservedStock(String productCode, BigDecimal quantity,
                                                 String referenceNumber, String performedBy) {
        return Panache.withTransaction(() ->
            getStockByProductCode(productCode)
                .onItem().ifNull().failWith(new WebApplicationException("Product not found", 404))
                .flatMap(stock -> {
                    if (stock.reservedStock.compareTo(quantity) < 0) {
                        return Uni.createFrom().failure(
                            new WebApplicationException("Cannot release more than reserved", 400)
                        );
                    }
                    
                    stock.reservedStock = stock.reservedStock.subtract(quantity);
                    
                    return Panache.getSession()
                        .flatMap(session -> session.persist(stock)
                            .flatMap(v -> session.flush())
                            .map(v -> stock))
                        .flatMap(s -> createTransaction(
                            productCode,
                            StockTransaction.TransactionType.RELEASE,
                            quantity,
                            s.reservedStock,
                            referenceNumber,
                            "Reserved stock released",
                            performedBy
                        ).map(t -> s));
                })
        );
    }
    
    public Uni<StockLedger> dispatchStock(String productCode, BigDecimal quantity,
                                         String referenceNumber, String performedBy) {
        return Panache.withTransaction(() ->
            getStockByProductCode(productCode)
                .onItem().ifNull().failWith(new WebApplicationException("Product not found", 404))
                .flatMap(stock -> {
                    if (stock.reservedStock.compareTo(quantity) < 0) {
                        return Uni.createFrom().failure(
                            new WebApplicationException("Quantity must be reserved first", 400)
                        );
                    }
                    
                    stock.reservedStock = stock.reservedStock.subtract(quantity);
                    stock.currentStock = stock.currentStock.subtract(quantity);
                    
                    return Panache.getSession()
                        .flatMap(session -> session.persist(stock)
                            .flatMap(v -> session.flush())
                            .map(v -> stock))
                        .flatMap(s -> createTransaction(
                            productCode,
                            StockTransaction.TransactionType.DISPATCH,
                            quantity,
                            s.currentStock,
                            referenceNumber,
                            "Stock dispatched",
                            performedBy
                        ).map(t -> s));
                })
        );
    }
    
    private Uni<StockLedger> getOrCreateStock(String productCode) {
        return getStockByProductCode(productCode)
            .onItem().ifNull().switchTo(() -> {
                StockLedger newStock = new StockLedger();
                newStock.productCode = productCode;
                newStock.productName = "Product " + productCode;
                newStock.currentStock = BigDecimal.ZERO;
                newStock.reservedStock = BigDecimal.ZERO;
                newStock.availableStock = BigDecimal.ZERO;
                newStock.incomingStock = BigDecimal.ZERO;
                newStock.lastUpdated = LocalDateTime.now();
                
                return Panache.getSession()
                    .flatMap(session -> session.persist(newStock)
                        .flatMap(v -> session.flush())
                        .map(v -> newStock));
            });
    }
    
    private Uni<StockTransaction> createTransaction(String productCode, 
                                                    StockTransaction.TransactionType type,
                                                    BigDecimal quantity,
                                                    BigDecimal balanceAfter,
                                                    String referenceNumber,
                                                    String notes,
                                                    String performedBy) {
        StockTransaction txn = new StockTransaction();
        txn.productCode = productCode;
        txn.type = type;
        txn.quantity = quantity;
        txn.balanceAfter = balanceAfter;
        txn.referenceNumber = referenceNumber;
        txn.notes = notes;
        txn.performedBy = performedBy;
        txn.transactionDate = LocalDateTime.now();
        
        return Panache.getSession()
            .flatMap(session -> session.persist(txn)
                .flatMap(v -> session.flush())
                .map(v -> txn));
    }
}
