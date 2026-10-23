package edu.itc.se.lab01.task2.src.functions;

import edu.itc.se.lab01.task2.src.interfaces.Counter;

public class LongAdderCounter implements Counter {
    private final java.util.concurrent.atomic.LongAdder count = new java.util.concurrent.atomic.LongAdder();

    @Override
    public void increment() {
        count.increment();
    }

    @Override
    public long get() {
        return count.sum();
    }
}
