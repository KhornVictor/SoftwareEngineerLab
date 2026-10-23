package edu.itc.se.lab01.task3.src.action;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import edu.itc.se.lab01.task3.src.interfaces.OrderQueue;

public class Producer implements Runnable {

    private final int producerId;
    private final int orderCount;
    private final int producerCount;
    private final int consumerCount;
    private final OrderQueue queue;
    private final AtomicInteger finishedProducers;

    public Producer(
            int producerId,
            int orderCount,
            int producerCount,
            int consumerCount,
            OrderQueue queue,
            AtomicInteger finishedProducers) {

        this.producerId = producerId;
        this.orderCount = orderCount;
        this.producerCount = producerCount;
        this.consumerCount = consumerCount;
        this.queue = queue;
        this.finishedProducers = finishedProducers;
    }

    @Override
    public void run() {
        try {
            for (int i = 0; i < orderCount; i++) {
                Order order = new Order(
                    producerId * orderCount + i + 1,
                    "item-" + (producerId * orderCount + i + 1),
                    ThreadLocalRandom.current().nextInt(1, 6)
                );

                queue.put(order);

                Thread.sleep(
                    ThreadLocalRandom.current().nextInt(21)
                );
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            int finished = finishedProducers.incrementAndGet();

            if (finished == producerCount) {
                sendPoisonPills();
            }
        }
    }

    private void sendPoisonPills() {
        try {
            for (int i = 0; i < consumerCount; i++) {
                queue.put(new Order(-1, "POISON", 0));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}