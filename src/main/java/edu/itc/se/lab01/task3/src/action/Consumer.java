package edu.itc.se.lab01.task3.src.action;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import edu.itc.se.lab01.task3.src.interfaces.OrderQueue;

public class Consumer implements Runnable {

    private final int consumerId;
    private final OrderQueue queue;
    private final AtomicInteger processedCount;

    public Consumer(
            int consumerId,
            OrderQueue queue,
            AtomicInteger processedCount) {

        this.consumerId = consumerId;
        this.queue = queue;
        this.processedCount = processedCount;
    }

    @Override
    public void run() {
        try {
            while (true) {
                Order order = queue.take();

                if (order.id() == -1) {
                    System.out.printf(
                        "consumer-%d received poison pill, stopping%n",
                        consumerId
                    );
                    break;
                }

                processedCount.incrementAndGet();

                System.out.printf(
                    "consumer-%d processed %s%n",
                    consumerId,
                    order
                );

                Thread.sleep(
                    ThreadLocalRandom.current().nextInt(31)
                );
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}