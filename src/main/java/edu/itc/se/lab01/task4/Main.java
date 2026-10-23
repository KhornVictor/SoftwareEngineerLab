package edu.itc.se.lab01.task4;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import edu.itc.se.lab01.task4.src.BenchmarkResult;
import edu.itc.se.lab01.task4.src.BenchmarkRunner;
import edu.itc.se.lab01.task4.src.CompletableFutureRunner;
import edu.itc.se.lab01.task4.src.PrimeCounter;

public class Main {

    private static final long LIMIT = 4_000_000;
    private static final long CHUNKS = 16;
    private static final int TOTAL_DOWNLOADS = 500;
    private static final long TIMEOUT_SECONDS = 1;

    public static void main(String[] args) throws Exception {
        // ----------------------------------------------------
        // Step 1: CPU-Bound Benchmark
        // ----------------------------------------------------
        long t0 = System.nanoTime();
        long singleCount = PrimeCounter.countPrimes(0, LIMIT);
        long singleElapsed = (System.nanoTime() - t0) / 1_000_000;
        System.out.printf("single-threaded: %d primes in %d ms%n", singleCount, singleElapsed);

        int cores = Runtime.getRuntime().availableProcessors();
        t0 = System.nanoTime();
        long totalParallel = 0;

        try (ExecutorService pool = Executors.newFixedThreadPool(cores)) {
            List<Future<Long>> futures = new ArrayList<>();
            long chunkSize = LIMIT / CHUNKS;

            for (long i = 0; i < CHUNKS; i++) {
                long from = i * chunkSize;
                long to = (i == CHUNKS - 1) ? LIMIT : from + chunkSize;
                futures.add(pool.submit(new PrimeCounter(from, to)));
            }

            for (Future<Long> f : futures) {
                totalParallel += f.get();
            }
        }
        long poolElapsed = (System.nanoTime() - t0) / 1_000_000;
        System.out.printf("pool (%d threads): %d primes in %d ms%n", cores, totalParallel, poolElapsed);

        // ----------------------------------------------------
        // Steps 2 - 5: I/O-Bound Benchmark
        // ----------------------------------------------------
        // (a) Fixed pool of 20 platform threads
        BenchmarkResult platformResult = BenchmarkRunner.runBenchmark(
                "downloads on fixed(20) platform threads",
                Executors.newFixedThreadPool(20),
                TOTAL_DOWNLOADS,
                TIMEOUT_SECONDS
        );
        platformResult.printSummary();

        // (b) Virtual threads per task
        BenchmarkResult virtualResult = BenchmarkRunner.runBenchmark(
                "downloads on virtual threads",
                Executors.newVirtualThreadPerTaskExecutor(),
                TOTAL_DOWNLOADS,
                TIMEOUT_SECONDS
        );
        virtualResult.printSummary();

        // ----------------------------------------------------
        // Step 6: CompletableFuture Bonus
        // ----------------------------------------------------
        BenchmarkResult bonusResult = CompletableFutureRunner.runAsyncDowmload(
                "bonus (CompletableFuture + virtual)",
                Executors.newVirtualThreadPerTaskExecutor(),
                TOTAL_DOWNLOADS,
                TIMEOUT_SECONDS
        );
        bonusResult.printSummary();
    }
}