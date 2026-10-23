package edu.itc.se.lab01.task5;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.ThreadLocalRandom;

public class Bank {
    static final class Account {
        final int id; private long balance;
        Account(int id, long balance) { this.id = id; this.balance = balance; }
        long balance() { return balance; }
        void withdraw(long a) { balance -= a; }
        void deposit(long a)  { balance += a; }
    }

    // BUG: lock order depends on the argument order → circular wait
    static void transfer(Account from, Account to, long amount) {
        synchronized (from) {
            synchronized (to) {
                if (from.balance() >= amount) { from.withdraw(amount); to.deposit(amount); }
            }
        }
    }

    static void startDeadlockWatchdog() {
        Thread watchdog = new Thread(() -> {
            ThreadMXBean bean = ManagementFactory.getThreadMXBean();
            while (true) {
                try {
                    Thread.sleep(1000);
                    long[] deadlocked = bean.findDeadlockedThreads();
                    if (deadlocked != null && deadlocked.length > 0) {
                        System.err.println("\n[DEADLOCK WATCHDOG] Deadlock detected among " 
                                + deadlocked.length + " thread(s)!");
                        ThreadInfo[] infos = bean.getThreadInfo(deadlocked, true, true);
                        for (ThreadInfo info : infos) {
                            System.err.printf("[WATCHDOG] Thread '%s' (id=%d, state=%s) waiting for lock '%s' held by '%s'%n",
                                    info.getThreadName(),
                                    info.getThreadId(),
                                    info.getThreadState(),
                                    info.getLockName(),
                                    info.getLockOwnerName());
                        }
                        System.err.println("[WATCHDOG] Use 'jstack <pid>' to inspect the full thread dump.\n");
                        break;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "deadlock-watchdog");
        watchdog.setDaemon(true);
        watchdog.start();
    }

    public static void main(String[] args) throws InterruptedException {
        startDeadlockWatchdog();
        Account[] accounts = new Account[4];
        for (int i = 0; i < accounts.length; i++) accounts[i] = new Account(i, 1_000);

        Thread[] workers = new Thread[8];
        for (int w = 0; w < workers.length; w++) {
            workers[w] = new Thread(() -> {
                var rnd = ThreadLocalRandom.current();
                for (int i = 0; i < 100_000; i++) {
                    Account a = accounts[rnd.nextInt(accounts.length)];
                    Account b = accounts[rnd.nextInt(accounts.length)];
                    if (a != b) transfer(a, b, rnd.nextInt(1, 50));
                }
            }, "transfer-" + w);
            workers[w].start();
        }
        for (Thread t : workers) t.join();
        long total = 0; for (Account a : accounts) total += a.balance();
        System.out.println("total = " + total + " (expected 4000)");   // never printed – deadlock
    }
}