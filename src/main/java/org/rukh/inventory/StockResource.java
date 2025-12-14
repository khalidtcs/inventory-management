package org.rukh.inventory;

import java.util.List;
import java.util.Map;

import org.rukh.inventory.model.StockLedger;
import org.rukh.inventory.model.StockOperationRequest;
import org.rukh.inventory.model.StockTransaction;
import org.rukh.inventory.service.StockService;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/stock")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class StockResource {
    
    @Inject
    StockService stockService;
    
    @GET
    @Path("/{productCode}")
    public Uni<Response> getStock(@PathParam("productCode") String productCode) {
        return stockService.getStockByProductCode(productCode)
            .onItem().ifNotNull().transform(stock -> Response.ok(stock).build())
            .onItem().ifNull().continueWith(Response.status(Response.Status.NOT_FOUND).build());
    }
    
    @GET
    public Uni<List<StockLedger>> getAllStock() {
        return StockLedger.listAll();
    }
    
    @POST
    @Path("/{productCode}/incoming")
    public Uni<Response> addIncomingStock(@PathParam("productCode") String productCode,
                                          StockOperationRequest request) {
        return stockService.addIncomingStock(
            productCode,
            request.quantity,
            request.referenceNumber,
            request.performedBy
        ).map(stock -> Response.ok(stock).build());
    }
    
    @POST
    @Path("/{productCode}/reserve")
    public Uni<Response> reserveStock(@PathParam("productCode") String productCode,
                                      StockOperationRequest request) {
        return stockService.reserveStock(
            productCode,
            request.quantity,
            request.referenceNumber,
            request.performedBy
        ).map(stock -> Response.ok(stock).build());
    }
    
    @POST
    @Path("/{productCode}/release")
    public Uni<Response> releaseReservedStock(@PathParam("productCode") String productCode,
                                              StockOperationRequest request) {
        return stockService.releaseReservedStock(
            productCode,
            request.quantity,
            request.referenceNumber,
            request.performedBy
        ).map(stock -> Response.ok(stock).build());
    }
    
    @POST
    @Path("/{productCode}/dispatch")
    public Uni<Response> dispatchStock(@PathParam("productCode") String productCode,
                                       StockOperationRequest request) {
        return stockService.dispatchStock(
            productCode,
            request.quantity,
            request.referenceNumber,
            request.performedBy
        ).map(stock -> Response.ok(stock).build());
    }
    
    @GET
    @Path("/{productCode}/transactions")
    public Uni<List<StockTransaction>> getTransactions(@PathParam("productCode") String productCode) {
        return StockTransaction.find("#StockTransaction.findByProductCode", 
            Map.of("productCode", productCode)).list();
    }
    
    @GET
    @Path("/low-stock")
    public Uni<List<StockLedger>> getLowStock() {
        return StockLedger.find(
            "currentStock <= reorderLevel and reorderLevel is not null"
        ).list();
    }
}
