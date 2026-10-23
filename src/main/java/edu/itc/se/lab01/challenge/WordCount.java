package edu.itc.se.lab01.challenge;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class WordCount {
    private final BlockingQueue<Path> queue = new ArrayBlockingQueue<>(16);
    private final ConcurrentHashMap<String, LongAdder> counts = new ConcurrentHashMap<>();
    private final AtomicInteger filesDone = new AtomicInteger();
    private final LongAdder wordsDone = new LongAdder();
    private final AtomicBoolean stopping = new AtomicBoolean();
    private static final Path POISON = Path.of("__POISON__");

    private final Path dir;
    private final int workers;
    private final boolean virtual;
    private final int top;
    private final long timeoutSec;
    private final int totalFiles;
    private final Path outputCsv;

    private static final Pattern NON_LETTERS = Pattern.compile("[^\\p{L}]+");

    private volatile long startTime;
    private volatile boolean isPartial = false;
    private volatile Thread scannerThread;
    private volatile ExecutorService pool;
    private volatile ScheduledExecutorService reporter;
    private volatile Thread shutdownHookThread;

    public WordCount(Path dir, int workers, boolean virtual, int top, long timeoutSec) {
        this(dir, workers, virtual, top, timeoutSec, Path.of("result.csv"));
    }

    public WordCount(Path dir, int workers, boolean virtual, int top, long timeoutSec, Path outputCsv) {
        this.dir = dir;
        this.workers = workers > 0 ? workers : Runtime.getRuntime().availableProcessors();
        this.virtual = virtual;
        this.top = top > 0 ? top : 10;
        this.timeoutSec = timeoutSec;
        this.outputCsv = outputCsv != null ? outputCsv : Path.of("result.csv");
        this.totalFiles = countTotalFiles(dir);
    }

    private static int countTotalFiles(Path dir) {
        if (!Files.exists(dir)) return 0;
        try (Stream<Path> stream = Files.walk(dir)) {
            return (int) stream.filter(Files::isRegularFile).count();
        } catch (IOException e) {
            return 0;
        }
    }

    public void run() throws InterruptedException {
        startTime = System.currentTimeMillis();
        CountDownLatch doneLatch = new CountDownLatch(workers);

        pool = virtual ? Executors.newVirtualThreadPerTaskExecutor()
                       : Executors.newFixedThreadPool(workers);

        scannerThread = Thread.ofPlatform().name("scanner").start(this::scan);

        reporter = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "reporter");
            t.setDaemon(true);
            return t;
        });
        reporter.scheduleAtFixedRate(this::report, 500, 500, TimeUnit.MILLISECONDS);

        shutdownHookThread = new Thread(this::shutdown, "shutdown-hook");
        Runtime.getRuntime().addShutdownHook(shutdownHookThread);

        for (int i = 0; i < workers; i++) {
            pool.submit(() -> {
                try {
                    work();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        boolean completedInTime;
        if (timeoutSec > 0) {
            completedInTime = doneLatch.await(timeoutSec, TimeUnit.SECONDS);
            if (!completedInTime) {
                if (reporter != null) {
                    reporter.shutdownNow();
                }
                long elapsedMillis = System.currentTimeMillis() - startTime;
                System.out.printf(Locale.US, "[%.1fs] deadline reached - stopping%n", elapsedMillis / 1000.0);
                shutdown();
                return;
            }
        } else {
            doneLatch.await();
        }

        // Normal completion path
        try {
            Runtime.getRuntime().removeShutdownHook(shutdownHookThread);
        } catch (IllegalStateException ignored) {
            // JVM is already in shutdown sequence
        }

        if (reporter != null) {
            reporter.shutdown();
            try {
                reporter.awaitTermination(500, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if (scannerThread != null) {
            scannerThread.join(2000);
        }

        pool.shutdown();
        pool.awaitTermination(2, TimeUnit.SECONDS);

        if (stopping.compareAndSet(false, true)) {
            printResult(false);
        }
    }

    private void scan() {
        try (Stream<Path> stream = Files.walk(dir)) {
            Iterator<Path> it = stream.filter(Files::isRegularFile).iterator();
            while (it.hasNext() && !Thread.currentThread().isInterrupted() && !stopping.get()) {
                Path p = it.next();
                queue.put(p);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            System.err.println("Scanner error: " + e.getMessage());
        } finally {
            if (!stopping.get()) {
                for (int i = 0; i < workers; i++) {
                    try {
                        queue.put(POISON);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
    }

    private void work() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Path p = queue.take();
                if (p == POISON) return;
                countFile(p);
                filesDone.incrementAndGet();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void countFile(Path p) {
        try (BufferedReader reader = Files.newBufferedReader(p)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (Thread.currentThread().isInterrupted() || stopping.get()) {
                    return;
                }
                String[] tokens = NON_LETTERS.split(line);
                long lineWords = 0;
                for (String token : tokens) {
                    if (!token.isEmpty()) {
                        String w = token.toLowerCase();
                        counts.computeIfAbsent(w, k -> new LongAdder()).increment();
                        lineWords++;
                    }
                }
                wordsDone.add(lineWords);
            }
        } catch (IOException ignored) {
        }
    }

    private void report() {
        if (stopping.get()) return;
        long elapsedMillis = System.currentTimeMillis() - startTime;
        if (elapsedMillis <= 0) return;
        double elapsedSec = elapsedMillis / 1000.0;
        int done = filesDone.get();
        long words = wordsDone.sum();
        long wordsPerSec = Math.round(words / elapsedSec);

        List<Map.Entry<String, Long>> top3 = getTopWords(3);
        StringBuilder topSb = new StringBuilder();
        for (Map.Entry<String, Long> entry : top3) {
            if (!topSb.isEmpty()) topSb.append(" ");
            topSb.append(entry.getKey()).append(String.format(Locale.US, "(%,d)", entry.getValue()));
        }

        System.out.printf(Locale.US, "[%.1fs] files %d/%d  words %,d  %,d w/s  top: %s%n",
            elapsedSec, done, totalFiles, words, wordsPerSec, topSb);
    }

    public void shutdown() {
        if (stopping.compareAndSet(false, true)) {
            isPartial = true;
            if (reporter != null) {
                reporter.shutdownNow();
            }
            if (scannerThread != null) {
                scannerThread.interrupt();
            }
            if (pool != null) {
                pool.shutdownNow();
                try {
                    pool.awaitTermination(2, TimeUnit.SECONDS);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
            if (scannerThread != null) {
                try {
                    scannerThread.join(500);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
            printResult(true);
        }
    }

    void printResult(boolean partial) {
        long elapsedMillis = System.currentTimeMillis() - startTime;
        double elapsedSec = elapsedMillis / 1000.0;
        long totalWords = wordsDone.sum();
        int done = filesDone.get();
        String modeStr = virtual ? "virtual(" + workers + ")" : "platform(" + workers + ")";

        String header = partial ? "[PARTIAL]" : "DONE ";
        System.out.printf(Locale.US, "%s files=%d words=%,d elapsed=%.2fs mode=%s%n",
            header, done, totalWords, elapsedSec, modeStr);

        List<Map.Entry<String, Long>> topEntries = getTopWords(top);

        System.out.println(" rank  word        count     share");
        for (int i = 0; i < topEntries.size(); i++) {
            var entry = topEntries.get(i);
            double share = totalWords > 0 ? (entry.getValue() * 100.0) / totalWords : 0.0;
            System.out.printf(Locale.US, "%5d  %-10s %,10d    %5.2f %%%n",
                i + 1, entry.getKey(), entry.getValue(), share);
        }

        // Write result.csv
        try (BufferedWriter writer = Files.newBufferedWriter(outputCsv)) {
            writer.write("rank,word,count,share\n");
            for (int i = 0; i < topEntries.size(); i++) {
                var entry = topEntries.get(i);
                double share = totalWords > 0 ? (entry.getValue() * 100.0) / totalWords : 0.0;
                writer.write(String.format(Locale.US, "%d,%s,%d,%.2f%%\n",
                    i + 1, entry.getKey(), entry.getValue(), share));
            }
        } catch (IOException e) {
            System.err.println("Failed to write " + outputCsv + ": " + e.getMessage());
        }
        System.out.println("result.csv written");
    }

    private List<Map.Entry<String, Long>> getTopWords(int n) {
        if (counts.isEmpty() || n <= 0) return List.of();
        PriorityQueue<Map.Entry<String, Long>> pq = new PriorityQueue<>(
            Comparator.comparingLong(Map.Entry::getValue)
        );

        for (Map.Entry<String, LongAdder> e : counts.entrySet()) {
            long c = e.getValue().sum();
            if (c <= 0) continue;
            if (pq.size() < n) {
                pq.offer(Map.entry(e.getKey(), c));
            } else if (c > pq.peek().getValue()) {
                pq.poll();
                pq.offer(Map.entry(e.getKey(), c));
            }
        }

        List<Map.Entry<String, Long>> list = new ArrayList<>(pq);
        list.sort(Map.Entry.<String, Long>comparingByValue().reversed()
            .thenComparing(Map.Entry.comparingByKey()));
        return list;
    }

    public Map<String, Long> getWordCounts() {
        Map<String, Long> snapshot = new HashMap<>();
        counts.forEach((k, v) -> snapshot.put(k, v.sum()));
        return snapshot;
    }

    public boolean isPartial() {
        return isPartial;
    }

    public int getFilesDone() {
        return filesDone.get();
    }

    public long getWordsDone() {
        return wordsDone.sum();
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0 || args[0].equals("--help") || args[0].equals("-h")) {
            System.out.println("Usage: java edu.itc.se.lab01.challenge.WordCount <dir> [--top N] [--timeout SECONDS] [--virtual] [--workers N]");
            return;
        }

        Path dir = null;
        int top = 10;
        long timeoutSec = 0;
        boolean virtual = false;
        int workers = Runtime.getRuntime().availableProcessors();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("--top") && i + 1 < args.length) {
                top = Integer.parseInt(args[++i]);
            } else if (arg.equals("--timeout") && i + 1 < args.length) {
                timeoutSec = Long.parseLong(args[++i]);
            } else if (arg.equals("--virtual")) {
                virtual = true;
            } else if (arg.equals("--workers") && i + 1 < args.length) {
                workers = Integer.parseInt(args[++i]);
            } else if (!arg.startsWith("--") && dir == null) {
                dir = Path.of(arg);
            }
        }

        if (dir == null) {
            System.err.println("Error: Directory path not specified.");
            System.exit(1);
        }

        if (!Files.exists(dir)) {
            System.err.println("Error: Directory does not exist: " + dir.toAbsolutePath());
            System.exit(1);
        }

        WordCount wc = new WordCount(dir, workers, virtual, top, timeoutSec);
        wc.run();
    }
}
