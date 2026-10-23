Task 2 — Fixing a race condition
Package: edu.itc.se.lab01.task2
Run
From the task2-race-condition directory, compile and run:
javac -d out src/edu/itc/se/lab01/task2/*.java
java -cp out edu.itc.se.lab01.task2.Bench
The benchmark starts 8 threads. Each thread increments the counter 1,000,000 times, so the expected result is:
8 × 1,000,000 = 8,000,000
Implementations
UnsafeCounter: uses a plain long. The expression count++ is a read, increment, and write—not one atomic operation. Threads can read the same old value and overwrite each other's updates. This is a lost-update race condition.
SynchronizedCounter: synchronizes both increment() and get() so both operations use the object's monitor.
LockCounter: protects both operations with a ReentrantLock. The lock is always released in a finally block.
AtomicCounter: uses AtomicLong.incrementAndGet(), which performs an atomic increment without an explicit lock in this code.
Recording the unsafe runs
Run the program and record the three values printed for UnsafeCounter #1, #2, and #3. They will often be below 8,000,000, and the exact values and timings vary by run and machine. Do not copy invented values; use the output from your own machine.
Why must get() also be synchronized?
If increment() is synchronized but get() is not, the two methods do not use the same synchronization protocol. A concurrent call to get() is not required to acquire the monitor, so it does not get the visibility guarantee provided by that monitor and may observe a stale value. Synchronizing both methods makes reads and writes consistently use the same monitor. In this benchmark, join() also establishes a happens-before relationship between each worker's actions and the main thread after the join, but that does not make an unsynchronized get() safe for general concurrent use.
Performance comparison
The benchmark reports elapsed time for each implementation. Times depend on the CPU, JVM, and system load. synchronized, ReentrantLock, and AtomicLong should all produce the correct final value; their relative speed can change across environments. This task does not include the optional LongAdderCounter bonus.
