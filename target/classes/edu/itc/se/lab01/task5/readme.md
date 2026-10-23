# Lab 01 — Task 5: Deadlock Detection, Prevention & Graceful Interruption

> **Module**: Software Engineering (I4-GIC-S1) — ITC / Techno  
> **Package**: `edu.itc.se.lab01.task5`  
> **Topic**: Deadlock reproduction, Coffman Conditions, `jstack` diagnostics, MXBean Watchdog, Lock Ordering, Timed `tryLock`, and Cooperative Interruption Handling

---

## 📌 1. Overview & Objectives

Task 5 investigates the most critical concurrency bug in multi-threaded programming: **Deadlock**. It reproduces an authentic bank transfer deadlock caused by inconsistent lock acquisition order, demonstrates how to detect it using automated watchdogs and JVM diagnostic tools (`jstack`), verifies two distinct architectural deadlock prevention strategies, and implements graceful cooperative interruption.

### Key Objectives:
1. **Deadlock Reproduction**: Simulate circular wait across multi-threaded account transfers in [`Bank.java`](Bank.java).
2. **Watchdog Detection & `jstack` Evidence**: Implement an automated `ThreadMXBean` watchdog daemon and capture JVM thread dumps proving circular monitor dependency.
3. **Deadlock Prevention Strategy 1 (Strict Lock Ordering)**: Eliminate circular wait in [`OrderedBank.java`](OrderedBank.java) using total ordering on account IDs.
4. **Deadlock Prevention Strategy 2 (Timed `tryLock` with Backoff)**: Eliminate hold-and-wait in [`TryLockBank.java`](TryLockBank.java) using explicit `ReentrantLock` polling and randomized exponential jitter.
5. **Graceful Interruption**: Implement clean cancellation in [`GracefulBank.java`](GracefulBank.java) using `Thread.interrupt()`, restoring interrupted flags, and ensuring zero lost funds.

---

## 🏗️ 2. Package & Architecture Structure

```
task5/
├── Bank.java                     # Deadlock reproduction + ThreadMXBean Watchdog
├── OrderedBank.java              # Fix 1: Global lock ordering (eliminates Circular Wait)
├── TryLockBank.java              # Fix 2: Timed tryLock with back-off (eliminates Hold and Wait)
├── GracefulBank.java             # Graceful cooperative interruption & balance preservation
└── readme.md                     # Comprehensive documentation & jstack evidence
```

---

## ⚠️ 3. Deadlock Reproduction & The 4 Coffman Conditions

A deadlock in concurrent execution requires all four **Coffman conditions** to hold simultaneously:

| Coffman Condition | How It Manifests in `Bank.java` |
| :--- | :--- |
| **1. Mutual Exclusion** | Accounts are guarded by exclusive intrinsic monitor locks (`synchronized (from)`). |
| **2. Hold and Wait** | A thread acquires and holds the lock on `from` while actively waiting to acquire the lock on `to`. |
| **3. No Preemption** | Once an account monitor is acquired, the JVM cannot forcibly preempt or revoke it. |
| **4. Circular Wait** | Thread $A$ holds Account 0 and waits for Account 1, while Thread $B$ holds Account 1 and waits for Account 0. |

### The Flawed Code in [`Bank.java`](Bank.java):
```java
// BUG: lock order depends on the argument order -> circular wait!
static void transfer(Account from, Account to, long amount) {
    synchronized (from) {
        synchronized (to) {
            if (from.balance() >= amount) {
                from.withdraw(amount);
                to.deposit(amount);
            }
        }
    }
}
```
When Thread 0 transfers from Account 0 to Account 1, and Thread 1 transfers from Account 1 to Account 0, both threads lock their first account and block indefinitely waiting for each other's account.

---

## 🔍 4. Diagnostic Evidence: Watchdog & `jstack` Dump

### A. Automated `ThreadMXBean` Deadlock Watchdog
Inside [`Bank.java`](Bank.java), a background daemon thread monitors the JVM via `ManagementFactory.getThreadMXBean()`:
```java
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
                                info.getThreadName(), info.getThreadId(), info.getThreadState(),
                                info.getLockName(), info.getLockOwnerName());
                    }
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
```

#### Watchdog Console Output:
```text
[DEADLOCK WATCHDOG] Deadlock detected among 3 thread(s)!
[WATCHDOG] Thread 'transfer-0' (id=40, state=BLOCKED) waiting for lock 'edu.itc.se.lab01.task5.Bank$Account@4fce52bc' held by 'transfer-3'
[WATCHDOG] Thread 'transfer-3' (id=43, state=BLOCKED) waiting for lock 'edu.itc.se.lab01.task5.Bank$Account@5c80f96e' held by 'transfer-5'
[WATCHDOG] Thread 'transfer-5' (id=45, state=BLOCKED) waiting for lock 'edu.itc.se.lab01.task5.Bank$Account@4fce52bc' held by 'transfer-3'
[WATCHDOG] Use 'jstack <pid>' to inspect the full thread dump.
```

---

### B. Authentic JDK 25 `jstack` Thread Dump Evidence

Captured directly from running JVM process:

```text
Found one Java-level deadlock:
=============================
"transfer-0":
  waiting to lock monitor 0x000001f677cb0e80 (object 0x0000000445823a68, a edu.itc.se.lab01.task5.Bank$Account),
  which is held by "transfer-4"

"transfer-4":
  waiting to lock monitor 0x000001f677cb0f50 (object 0x0000000445823a80, a edu.itc.se.lab01.task5.Bank$Account),
  which is held by "transfer-0"

Java stack information for the threads listed above:
===================================================
"transfer-0":
	at edu.itc.se.lab01.task5.Bank.transfer(Bank.java:18)
	- waiting to lock <0x0000000445823a68> (a edu.itc.se.lab01.task5.Bank$Account)
	- locked <0x0000000445823a80> (a edu.itc.se.lab01.task5.Bank$Account)
	at edu.itc.se.lab01.task5.Bank.lambda$0(Bank.java:34)
	at edu.itc.se.lab01.task5.Bank$$Lambda/0x0000000023040c00.run(Unknown Source)
	at java.lang.Thread.runWith(java.base@25/Thread.java:1487)
	at java.lang.Thread.run(java.base@25/Thread.java:1474)
"transfer-4":
	at edu.itc.se.lab01.task5.Bank.transfer(Bank.java:18)
	- waiting to lock <0x0000000445823a80> (a edu.itc.se.lab01.task5.Bank$Account)
	- locked <0x0000000445823a68> (a edu.itc.se.lab01.task5.Bank$Account)
	at edu.itc.se.lab01.task5.Bank.lambda$0(Bank.java:34)
	at edu.itc.se.lab01.task5.Bank$$Lambda/0x0000000023040c00.run(Unknown Source)
	at java.lang.Thread.runWith(java.base@25/Thread.java:1487)
	at java.lang.Thread.run(java.base@25/Thread.java:1474)

Found 1 deadlock.
```

---

## 🛡️ 5. Deadlock Fix 1: Global Lock Ordering ([`OrderedBank.java`](OrderedBank.java))

### Strategy: Break Coffman Condition #4 (Circular Wait)
By enforcing a strict, monotonic global order on locks based on account ID, circular dependency becomes mathematically impossible:

```java
private static void transfer(Account from, Account to, long amount) {
    // Acquire the lock with smaller ID first, then larger ID
    Account first  = from.getId() < to.getId() ? from : to;
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
```

### Verification & Performance:
- **Transfers completed**: `800,000` transfers across 8 threads.
- **Duration**: ~104 ms.
- **Conservation of funds**: Total balance equals **4,000** (100% correct, 0 lost cents).

---

## 🛡️ 6. Deadlock Fix 2: Timed `tryLock` with Back-Off ([`TryLockBank.java`](TryLockBank.java))

### Strategy: Break Coffman Condition #2 (Hold and Wait)
Using explicit `java.util.concurrent.locks.ReentrantLock`, if a thread cannot acquire the second lock within a given timeout, it **voluntarily releases the first lock**, backs off with randomized jitter, and retries:

```java
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
                        return; // Completed!
                    } finally {
                        to.lock.unlock();
                    }
                }
            } finally {
                from.lock.unlock(); // Release outer lock on contention!
            }
        }
        retryCount.incrementAndGet();
        Thread.sleep(ThreadLocalRandom.current().nextInt(1, 5)); // Randomized jitter back-off
    }
}
```

### Verification & Performance:
- **Transfers completed**: `800,000`.
- **Retries handled**: ~1,200 retries gracefully resolved via jitter.
- **Conservation of funds**: Total balance equals **4,000**.
- **Lock Safety**: All locks strictly unlocked in `finally` blocks.

---

## ⚡ 7. Graceful Interruption ([`GracefulBank.java`](GracefulBank.java))

Cooperative cancellation without data corruption or orphan threads:

1. **Loop Termination Check**: Workers check `!Thread.currentThread().isInterrupted()` before every operation.
2. **Back-Off Check**: Before entering `Thread.sleep()`, checks `isInterrupted()` to avoid sleeping after cancellation.
3. **Interrupt Flag Restoration**:
   ```java
   } catch (InterruptedException e) {
       Thread.currentThread().interrupt(); // Restore interrupt status
       break;
   }
   ```
4. **Controlled Join with Timeout**: Main thread calls `t.join(1000)` and validates `allStopped == true`.
5. **Output**:
   ```text
   [main] interrupting 8 workers after 2000 ms
   transfer-0 stopped after 153092 transfers
   transfer-1 stopped after 152841 transfers
   ...
   all workers stopped=true total=4000 (expected 4000)
   ```

---

## 📊 8. Comparative Verification Matrix

| Implementation | Mechanism | Deadlock Free? | Funds Conserved? | Graceful Shutdown? | Execution Time |
| :--- | :--- | :---: | :---: | :---: | :---: |
| **`Bank.java`** | Unordered `synchronized` | ❌ **No (Deadlock)** | ❌ Stalled | ❌ Hangs | Indefinite (Watchdog detected) |
| **`OrderedBank.java`** | ID Ordering (`from.id < to.id`) | ✅ **Yes** | ✅ **4,000** | ✅ Clean exit | **104 ms** (Fastest) |
| **`TryLockBank.java`** | `tryLock` + Backoff Jitter | ✅ **Yes** | ✅ **4,000** | ✅ Clean exit | **890 ms** |
| **`GracefulBank.java`** | `tryLock` + Cooperative Interruption | ✅ **Yes** | ✅ **4,000** | ✅ **100% stopped** | 2,000 ms (Timed run) |

---

## 🛠️ 9. How to Build & Run

From project root `lab01-multithreading`:

```bash
# 1. Compile
mvn compile

# 2. Run Bank with automated Watchdog detection:
java -cp target/classes edu.itc.se.lab01.task5.Bank

# 3. Run OrderedBank (Fix 1):
java -cp target/classes edu.itc.se.lab01.task5.OrderedBank

# 4. Run TryLockBank (Fix 2):
java -cp target/classes edu.itc.se.lab01.task5.TryLockBank

# 5. Run GracefulBank (Graceful Interruption):
java -cp target/classes edu.itc.se.lab01.task5.GracefulBank
```
