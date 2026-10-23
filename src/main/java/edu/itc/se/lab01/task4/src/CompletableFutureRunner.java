package edu.itc.se.lab01.task4.src;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class CompletableFutureRunner {
    public static BenchmarkResult runAsyncDownload(String label, ExecutorService executor, int taskCount, long timeoutSeconds) throws Exception {
        long t0 = System.nanoTime();
        List<CompletableFuture<String>> futures = new ArrayList<>(taskCount);

        try (executor) {
            for (int i = 0; i < taskCount; i++) {
                final int id = i;
                final String url = "http://example.com/file" + id;

                CompletableFuture<String> completableFuture = CompletableFuture.supplyAsync(() -> {
                    try {
                        return new FakeDownload(url, id, id == taskCount - 1).call();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }, executor)
                .orTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .handle((result, ex) -> {
                    if (ex != null) {
                        Throwable cause = ex;
                        if (cause instanceof CompletionException) {
                            Throwable completionCause = cause.getCause();
                            if (completionCause != null) {
                                cause = completionCause;
                            }
                        }
                        return (cause instanceof TimeoutException) ? "TIMED_OUT" : "FAILED";
                    } else {
                        return result;
                    }
                });
                futures.add(completableFuture);
            }

            int ok = 0, failed = 0, timedOut = 0;
            for (CompletableFuture<String> completableFuture : futures) {
                try {
                    completableFuture.get(timeoutSeconds, TimeUnit.SECONDS);
                    ok++;
                } catch (TimeoutException e) {
                    timedOut++;
                    completableFuture.cancel(true);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    failed++;
                } catch (ExecutionException e) {
                    failed++;
                }
            }

            long elapsedMs = (System.nanoTime() - t0) / 1_000_000;
            return new BenchmarkResult(label, ok, failed, timedOut, elapsedMs);
        }
    }

    public static BenchmarkResult runAsyncDowmload(String label, ExecutorService executor, int taskCount, long timeoutSeconds) throws Exception {
        return runAsyncDownload(label, executor, taskCount, timeoutSeconds);
    }
}
