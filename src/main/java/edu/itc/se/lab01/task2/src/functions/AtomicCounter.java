package edu.itc.se.lab01.task2.src.functions;

import edu.itc.se.lab01.task2.src.interfaces.Counter;

public class AtomicCounter implements Counter {
    private final java.util.concurrent.atomic.AtomicLong count = new java.util.concurrent.atomic.AtomicLong(0);

    @Override
    public void increment() {
        count.incrementAndGet();
    }

    @Override
    public long get() {
        return count.get();
    }
}