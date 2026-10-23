package edu.itc.se.lab01.task3.src.process;
import java.util.ArrayDeque;

import edu.itc.se.lab01.task3.src.action.Order;
import edu.itc.se.lab01.task3.src.interfaces.OrderQueue;    

public class BoundedBuffer implements OrderQueue {
    private final ArrayDeque<Order> items = new ArrayDeque<>();
    private final int capacity;

    public BoundedBuffer(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("Capacity must be greater than zero.");
        this.capacity = capacity;
    }  

    @Override
    public synchronized void put(Order order) throws InterruptedException {
        while (items.size() == capacity) wait();
        items.addLast(order);
        assert items.size() <= capacity : "Buffer overflow: size exceeds capacity.";
        notifyAll();
    }

    @Override
    public synchronized Order take() throws InterruptedException {
        while (items.isEmpty()) wait();
        Order order = items.removeFirst();
        assert items.size() >= 0 : "Buffer underflow: size is negative.";
        notifyAll();
        return order;
    }

}