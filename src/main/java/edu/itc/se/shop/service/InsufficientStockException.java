package edu.itc.se.shop.service;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(long id, int currentStock, int delta) {
        super("Product " + id + " has " + currentStock + " units, cannot remove " + (-delta));
    }

    public InsufficientStockException(String message) {
        super(message);
    }
}
