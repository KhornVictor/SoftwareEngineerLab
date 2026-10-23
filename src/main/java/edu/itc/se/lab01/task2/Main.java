package edu.itc.se.lab01.task2;

import java.util.ArrayList;
import java.util.List;

import edu.itc.se.lab01.task2.src.functions.AtomicCounter;
import edu.itc.se.lab01.task2.src.functions.LockCounter;
import edu.itc.se.lab01.task2.src.functions.LongAdderCounter;
import edu.itc.se.lab01.task2.src.functions.SynchronizedCounter;
import edu.itc.se.lab01.task2.src.functions.UnsafeCounter;
import edu.itc.se.lab01.task2.src.interfaces.Counter;

public class Main {
    static final int THREADS = 8;
    static final int ITERATIONS = 1_000_000;

    static void run(String label, Counter counter) throws InterruptedException {
        long start = System.nanoTime();
        List<Thread> threads = new ArrayList<>();
        for (int t = 0; t < THREADS; t++) {
            Thread th = new Thread(() -> {
                for (int i = 0; i < ITERATIONS; i++) counter.increment();
            }, "inc-" + t);
            threads.add(th);
            th.start();
        }
        for (Thread th : threads) th.join();
        long ms = (System.nanoTime() - start) / 1_000_000;
        long expected = (long) THREADS * ITERATIONS;
        System.out.printf("%-22s value=%,d  correct=%-5b  %d ms%n",
                label, counter.get(), counter.get() == expected, ms);
    }

    public static void main(String[] args) throws InterruptedException {
        Main.run("UnsafeCounter", new UnsafeCounter());
        Main.run("SynchronizedCounter", new SynchronizedCounter());
        Main.run("LockCounter", new LockCounter());
        Main.run("AtomicCounter", new AtomicCounter());
        Main.run("LongAdderCounter", new LongAdderCounter());
    }
}