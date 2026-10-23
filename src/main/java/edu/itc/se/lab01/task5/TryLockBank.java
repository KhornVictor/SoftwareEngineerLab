package edu.itc.se.lab01.task5;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

public class TryLockBank {
    private static final class Account {
        private int id; 
        private long balance;
        final ReentrantLock lock = new ReentrantLock();


        Account(int id, long balance) {
            this.id = id; 
            this.balance = balance; 
        }

        int getId() { return id; }
        long getBalance() { return balance; }
        void withdraw(long a) { balance -= a; }
        void deposit(long a)  { balance += a; }
    }

    private static final AtomicLong retryCount = new AtomicLong(0);

    private static void transfer(Account from, Account to, long amount) throws InterruptedException {
        while (true) {
            if (from.lock.tryLock(10, TimeUnit.MILLISECONDS)) {
                try {
                    if (to.lock.tryLock(10, TimeUnit.MILLISECONDS)) {
                        try {
                            if (from.getBalance() >= amount) {
                                from.withdraw(amount);
                                to.deposit(amount);
                            }
                            return; // Transfer complete
                        } finally {
                            to.lock.unlock();
                        }
                    }
                } finally {
                    from.lock.unlock(); // Back off: release outer lock
                }
            }

            retryCount.incrementAndGet();
            Thread.sleep(ThreadLocalRandom.current().nextInt(1, 5));
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Account[] accounts = new Account[4];
        for (int i = 0; i < accounts.length; i++) accounts[i] = new Account(i, 1_000);

        long startTime = System.currentTimeMillis();
        int numWorkers = 8;
        int transfersPerWorker = 100_000;
        Thread[] workers = new Thread[numWorkers];

        for (int w = 0; w < workers.length; w++) {
            workers[w] = new Thread(() -> {
                var rnd = ThreadLocalRandom.current();
                for (int i = 0; i < transfersPerWorker; i++) {
                    Account a = accounts[rnd.nextInt(accounts.length)];
                    Account b = accounts[rnd.nextInt(accounts.length)];
                    if (a != b)
                        try {
                            transfer(a, b, rnd.nextInt(1, 50));
                        } catch (InterruptedException e) {
                            System.err.println("Transfer interrupted: " + e.getMessage());
                            Thread.currentThread().interrupt();
                        }
                }
            }, "transfer-" + w);
            workers[w].start();
        }

        for (Thread t : workers) t.join();
        long duration = System.currentTimeMillis() - startTime;

        long total = 0;
        for (Account a : accounts) total += a.getBalance();

        System.out.println("transfers=" + (numWorkers * transfersPerWorker) 
                + " retries=" + retryCount.get() 
                + " total=" + total + " (expected 4000) in " + duration + " ms");
    }
}
