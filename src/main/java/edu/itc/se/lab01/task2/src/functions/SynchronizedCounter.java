package edu.itc.se.lab01.task2.src.functions;

import edu.itc.se.lab01.task2.src.interfaces.Counter;

public class SynchronizedCounter implements Counter {
    private long count = 0;

    @Override
    public synchronized void increment() {
        count++;
    }

    @Override
    public synchronized long get() {
        return count;
    }
}
