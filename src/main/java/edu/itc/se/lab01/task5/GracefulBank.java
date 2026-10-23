package edu.itc.se.lab01.task5;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

public class GracefulBank {
    private static final class Account {
        private final int id;
        private long balance;
        final ReentrantLock lock = new ReentrantLock();

        Account(int id, long balance) {
            this.id = id;
            this.balance = balance;
        }

        long balance() { return balance; }
        void withdraw(long a) { balance -= a; }
        void deposit(long a)  { balance += a; }
    }

    private static boolean transfer(Account from, Account to, long amount) throws InterruptedException {
        while (!Thread.currentThread().isInterrupted()) {
            if (from.lock.tryLock(10, TimeUnit.MILLISECONDS)) {
                try {
                    if (to.lock.tryLock(10, TimeUnit.MILLISECONDS)) {
                        try {
                            if (from.balance() >= amount) {
                                from.withdraw(amount);
                                to.deposit(amount);
                            }
                            return true;
                        } finally {
                            to.lock.unlock();
                        }
                    }
                } finally {
                    from.lock.unlock();
                }
            }

            // Check interruption before sleeping for back-off
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
            }
            Thread.sleep(ThreadLocalRandom.current().nextInt(1, 5));
        }
        return false;
    }

    public static void main(String[] args) throws InterruptedException {
        Account[] accounts = new Account[4];
        for (int i = 0; i < accounts.length; i++) accounts[i] = new Account(i, 1_000);

        int numWorkers = 8;
        Thread[] workers = new Thread[numWorkers];
        AtomicLong[] completedTransfers = new AtomicLong[numWorkers];
        for (int i = 0; i < numWorkers; i++) completedTransfers[i] = new AtomicLong(0);

        for (int w = 0; w < numWorkers; w++) {
            final int workerId = w;
            workers[w] = new Thread(() -> {
                var rnd = ThreadLocalRandom.current();
                while (!Thread.currentThread().isInterrupted()) {
                    Account a = accounts[rnd.nextInt(accounts.length)];
                    Account b = accounts[rnd.nextInt(accounts.length)];
                    if (a != b) {
                        try {
                            if (transfer(a, b, rnd.nextInt(1, 50))) {
                                completedTransfers[workerId].incrementAndGet();
                            }
                        } catch (InterruptedException e) {
                            // Restore interrupted status to exit loop cleanly
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
                System.out.println(Thread.currentThread().getName() 
                        + " stopped after " + completedTransfers[workerId].get() + " transfers");
            }, "transfer-" + w);
            workers[w].start();
        }

        // Run for 2 seconds
        Thread.sleep(2000);
        System.out.println("[main] interrupting " + numWorkers + " workers after 2000 ms");

        for (Thread t : workers) {
            t.interrupt();
        }

        boolean allStopped = true;
        for (Thread t : workers) {
            t.join(1000); // 1-second timeout
            if (t.isAlive()) {
                allStopped = false;
            }
        }

        long total = 0;
        for (Account a : accounts) total += a.balance();

        System.out.println("all workers stopped=" + allStopped + " total=" + total + " (expected 4000)");
    }
}