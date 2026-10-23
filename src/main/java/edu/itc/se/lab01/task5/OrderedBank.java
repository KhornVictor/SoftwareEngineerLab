package edu.itc.se.lab01.task5;

import java.util.concurrent.ThreadLocalRandom;

public class OrderedBank {
    private static final class Account {
        private final int id; 
        private long balance;
        
        Account(int id, long balance) {
            this.id = id; 
            this.balance = balance; 
        }

        int getId() { return id; }
        long getBalance() { return balance; }
        void withdraw(long a) { balance -= a; }
        void deposit(long a)  { balance += a; }
    }

    private static void transfer(Account from, Account to, long amount) {
        Account first = from.getId() < to.getId() ? from : to;
        Account second = from.getId() < to.getId() ? to : from;

        synchronized (first) {
            synchronized (second) {
                if (from.getBalance() >= amount) {
                    from.withdraw(amount);
                    to.deposit(amount);
                }
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        OrderedBank.Account[] accounts = new OrderedBank.Account[4];
        for (int i = 0; i < accounts.length; i++) {
            accounts[i] = new OrderedBank.Account(i, 1000);
        }

        long startTime = System.currentTimeMillis();
        int numberOfWorkers = 8;
        int numberOfTransfersPerWorker = 100_000;

        Thread[] workers = new Thread[numberOfWorkers];
        for (int worker = 0; worker < numberOfWorkers; worker++) {
            workers[worker] = new Thread(() -> {
                var random = ThreadLocalRandom.current();
                for (int i = 0; i < numberOfTransfersPerWorker; i++) {
                    OrderedBank.Account a = accounts[random.nextInt(accounts.length)];
                    OrderedBank.Account b = accounts[random.nextInt(accounts.length)];
                    if (a != b) {
                        OrderedBank.transfer(a, b, random.nextInt(1, 50));
                    }
                }
            }, "transfer-" + worker);
            workers[worker].start();
        }

        for (Thread worker : workers) worker.join();
        long duration = System.currentTimeMillis() - startTime;
        System.out.println("Duration: " + duration + " ms");

        long totalBalance = 0;
        for (OrderedBank.Account account : accounts) {
            totalBalance += account.getBalance();
        }
        System.out.println("transfers=" + (numberOfWorkers * numberOfTransfersPerWorker) + " total=" + totalBalance + " (expected 4000) in " + duration + " ms");
    }
}
