package edu.itc.se.lab01.task3;

import java.util.concurrent.atomic.AtomicInteger;

import edu.itc.se.lab01.task3.src.action.Consumer;
import edu.itc.se.lab01.task3.src.action.Producer;
import edu.itc.se.lab01.task3.src.interfaces.OrderQueue;
import edu.itc.se.lab01.task3.src.process.BoundedBuffer;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        int producerCount = 2;
        int consumerCount = 3;
        int ordersPerProducer = 50;
        int capacity = 5;

        OrderQueue queue = new BoundedBuffer(capacity);
        AtomicInteger processedCount = new AtomicInteger(0);
        AtomicInteger finishedProducers = new AtomicInteger(0);

        Thread[] producers = new Thread[producerCount];
        Thread[] consumers = new Thread[consumerCount];

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < consumerCount; i++) {
            consumers[i] = new Thread(new Consumer(
                i,
                queue,
                processedCount
            ), "consumer-" + i);
            consumers[i].start();
        }

        for (int i = 0; i < producerCount; i++) {
            producers[i] = new Thread(new Producer(
                i,
                ordersPerProducer,
                producerCount,
                consumerCount,
                queue,
                finishedProducers
            ), "producer-" + i);
            producers[i].start();
        }

        for (Thread producer : producers) {
            producer.join();
        }

        for (Thread consumer : consumers) {
            consumer.join();
        }

        long elapsed = System.currentTimeMillis() - startTime;
        int total = processedCount.get();

        System.out.printf("Total processed: %d (expected %d) in %d ms%n", total, producerCount * ordersPerProducer, elapsed);

        assert total == 100 : "Expected exactly 100 orders";
        if (total != 100) {
            System.err.printf("Error: Expected %d orders, but processed %d%n", producerCount * ordersPerProducer, total);
        }
    }
}
