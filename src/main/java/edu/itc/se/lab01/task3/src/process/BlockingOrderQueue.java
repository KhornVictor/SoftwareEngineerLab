package edu.itc.se.lab01.task3.src.process;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import edu.itc.se.lab01.task3.src.action.Order;
import edu.itc.se.lab01.task3.src.interfaces.OrderQueue;

public final class BlockingOrderQueue implements OrderQueue {
    private final BlockingQueue<Order> queue;

    public BlockingOrderQueue(int capacity) {
        this.queue = new ArrayBlockingQueue<>(capacity);
    }

    @Override 
    public void put(Order order) throws InterruptedException {
        queue.put(order);
    }

    @Override 
    public Order take() throws InterruptedException {
        return queue.take();
    }

}