package edu.itc.se.lab01.task2.src.functions;

import edu.itc.se.lab01.task2.src.interfaces.Counter;

public class LockCounter implements Counter {
    private long count = 0;
    private final java.util.concurrent.locks.Lock lock = new java.util.concurrent.locks.ReentrantLock();

    @Override
    public void increment() {
        lock.lock();
        try {
            count++;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public long get() {
        lock.lock();
        try {
            return count;
        } finally {
            lock.unlock();
        }
    }
}
