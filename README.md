# Chapter 01: Multithreading & Modern Java Concurrency

> **Module**: Software Engineering (I4-GIC-S1) — ITC / Techno  
> **Directory**: `C1-MultiThread`

---

## 📂 Quick Access & Files

| Resource                         | File Link                          | Purpose                                                                  |
| :------------------------------- | :--------------------------------- | :----------------------------------------------------------------------- |
| 📄 **Lecture Slides**            | [`Lesson.pdf`](Lesson.pdf)         | Official course presentation slides from lecture                         |
| 📘 **Chapter Study Guide**       | [`study_guide.md`](study_guide.md) | Focused conceptual summary, mental models, code snippets & traps         |
| 📝 **Chapter Quiz & Flashcards** | [`quiz_prep.md`](quiz_prep.md)     | 10 official slide quiz questions, answer keys, explanations & flashcards |

---

### Key Learning Agenda & Core Concepts

1. **Processes vs Threads**: Operating system process separation (isolated address spaces) versus lightweight threads sharing heap memory with individual program counters, stacks, and registers.
2. **Mechanisms of Thread Creation**:
   - Subclassing `java.lang.Thread` vs implementing `java.lang.Runnable`.
   - Modern functional approaches using lambdas, anonymous inner classes, and `Callable<V>`.
   - Callback architectures ("call me when you are done") for decoupled asynchronous processing.
3. **Thread State Machine (`Thread.State`)**:
   - `NEW` ➔ `RUNNABLE` (ready or running) ➔ `BLOCKED` (waiting for lock) ➔ `WAITING` (indefinite notification) ➔ `TIMED_WAITING` (sleep/join with timeout) ➔ `TERMINATED`.
   - Programmatic state inspection using `thread.getState()`.
4. **Platform Threads vs Virtual Threads (Project Loom / JDK 21+)**:
   - OS-bound platform threads (~1MB stack, expensive context switching, limited to thousands).
   - Lightweight virtual threads (heap-allocated, managed by JVM, scaling to millions for high-throughput I/O).
5. **Synchronization & Memory Visibility**:
   - Race conditions and non-atomic compound operations (`count++`).
   - Mutual exclusion using `synchronized` blocks/methods and explicit locks (`ReentrantLock`).
   - Java Memory Model (JMM), cache coherency, the `volatile` keyword, and happens-before guarantees.
   - Lock-free atomic primitives: `AtomicInteger`, `AtomicReference`, `LongAdder`.
6. **Inter-Thread Coordination**:
   - Classical monitor methods: `wait()`, `notify()`, and `notifyAll()` within synchronized blocks.
   - High-level concurrent queues: `BlockingQueue` (`ArrayBlockingQueue`, `LinkedBlockingQueue`).
   - Explicit lock coordination: `Condition` variables with distinct wait sets ("not empty", "not full").
7. **Thread Pools & the Executor Framework**:
   - `ExecutorService`, `Executors.newFixedThreadPool()`, and custom `ThreadPoolExecutor` configurations.
   - Managing asynchronous computation with `Future<T>` and non-blocking pipeline chaining using `CompletableFuture`.
8. **Concurrency Hazards & Deadlock**:
   - Four conditions of deadlock (mutual exclusion, hold & wait, no preemption, circular wait).
   - Deadlock prevention via strict global lock acquisition ordering and timed `tryLock()`.
   - Cooperative cancellation via `Thread.interrupt()` and proper handling of `InterruptedException`.
9. **Concurrent Utilities & Collections**:
   - Thread-safe collections: `ConcurrentHashMap`, `CopyOnWriteArrayList`, `ConcurrentLinkedQueue`.
   - Synchronization barriers: `CountDownLatch` (one-shot gate), `CyclicBarrier` (reusable barrier), `Semaphore` (permit throttler).
10. **Build Tool Primer (Apache Maven 3.9)**:
    - Project Object Model (`pom.xml`), GAV coordinates (`groupId`, `artifactId`, `version`).
    - Standard directory layout (`src/main/java`, `src/test/java`).
    - Maven lifecycle phases: `validate` ➔ `compile` ➔ `test` ➔ `package` ➔ `verify` ➔ `install`.
11. **Hands-On Assessment**:
    - **Chapter Quiz**: 10 questions testing thread creation, lifecycle states, deadlock, and memory visibility.
    - **Lab-01**: 5 structured programming tasks + 1 concurrent throughput challenge.

---

## 🧭 Course Navigation

- 🏠 **[Course Syllabus & Overview](../readme.md)**
- 📚 **[Full Course Study Guide](../study_guide.md)**
- 📝 **[Full Quiz Master (120 Questions)](../quiz_prep.md)**
- ➡️ **Next Module**: [Chapter 02: C2-Spring Framework](../C2-Spring%20Framework/README.md)
