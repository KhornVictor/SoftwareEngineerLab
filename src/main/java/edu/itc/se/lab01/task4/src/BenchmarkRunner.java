package edu.itc.se.lab01.task4.src;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class BenchmarkRunner {
    public static BenchmarkResult runBenchmark(String name, ExecutorService executor, int taskCount, long timeoutSeconds) throws Exception {
        long t0 = System.nanoTime();
        List<Future<String>> futures = new java.util.ArrayList<>(taskCount);

        try (executor) {
            for (int i = 0; i < taskCount; i++) {
                futures.add(executor.submit(new FakeDownload("http://example.com/file" + i, i, i == timeoutSeconds)));
            }

            int ok = 0, failed = 0, timedOut = 0;
            for (Future<String> future : futures) {
                try {
                    future.get(timeoutSeconds, java.util.concurrent.TimeUnit.SECONDS);
                    ok++;
                } catch (java.util.concurrent.TimeoutException e) {
                    timedOut++;
                    future.cancel(true);
                } catch (ExecutionException e) {
                    failed++;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            long elapsedMs = (System.nanoTime() - t0) / 1_000_000;
            return new BenchmarkResult(name, ok, failed, timedOut, elapsedMs);
        } 
    }
}
