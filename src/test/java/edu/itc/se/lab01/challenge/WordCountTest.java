package edu.itc.se.lab01.challenge;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class WordCountTest {

    @Test
    void testParallelAndSingleThreadedProduceIdenticalResults(@TempDir Path tempDir) throws Exception {
        // Generate a 20-file fixture with ~1,000 words each from a 200-word vocabulary
        TestDataGenerator.generate(tempDir, 20, 1000, 200, 42L);

        // Run single-threaded reference implementation
        Map<String, Long> expected = SingleThreadedWordCount.count(tempDir);
        assertFalse(expected.isEmpty(), "Expected non-empty word counts from fixture");

        // Run parallel implementation with 4 platform workers
        Path csvOutput = tempDir.resolve("result_parallel.csv");
        WordCount parallel = new WordCount(tempDir, 4, false, 10, 0, csvOutput);
        parallel.run();
        Map<String, Long> actual = parallel.getWordCounts();

        // Assert identical counts for every word
        assertEquals(expected, actual, "Parallel and single-threaded counts must match identically");
        assertFalse(parallel.isPartial(), "Expected full non-partial completion");
        assertEquals(20, parallel.getFilesDone(), "Expected all 20 files to be processed");
        assertTrue(Files.exists(csvOutput), "Expected result_parallel.csv to be written");
    }

    @Test
    void testVirtualThreadsProduceIdenticalResults(@TempDir Path tempDir) throws Exception {
        // Generate a 20-file fixture
        TestDataGenerator.generate(tempDir, 20, 1000, 200, 99L);

        Map<String, Long> expected = SingleThreadedWordCount.count(tempDir);

        // Run parallel implementation with virtual threads
        Path csvOutput = tempDir.resolve("result_virtual.csv");
        WordCount virtualWc = new WordCount(tempDir, 8, true, 10, 0, csvOutput);
        virtualWc.run();
        Map<String, Long> actual = virtualWc.getWordCounts();

        assertEquals(expected, actual, "Virtual threads and single-threaded counts must match identically");
        assertFalse(virtualWc.isPartial());
        assertEquals(20, virtualWc.getFilesDone());
    }

    @Test
    void testTimeoutStopsWithinThreeSecondsOn5000Files(@TempDir Path tempDir) throws Exception {
        // Quickly generate 5,000 files in parallel
        int fileCount = 5_000;
        int threads = Math.min(Runtime.getRuntime().availableProcessors(), 16);
        try (ExecutorService pool = Executors.newFixedThreadPool(threads)) {
            for (int i = 0; i < fileCount; i++) {
                final int id = i;
                pool.submit(() -> {
                    try {
                        Path file = tempDir.resolve(String.format("bench_%05d.txt", id));
                        try (BufferedWriter bw = Files.newBufferedWriter(file)) {
                            // ~500 words per file to ensure sufficient processing workload
                            for (int w = 0; w < 50; w++) {
                                bw.write("the quick brown fox jumps over the lazy dog repeatedly and consistently ");
                            }
                            bw.newLine();
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
            pool.shutdown();
            assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS), "File generation timed out");
        }

        // Run WordCount with --timeout 1 second
        Path csvOutput = tempDir.resolve("result_timeout.csv");
        WordCount wc = new WordCount(tempDir, 4, false, 5, 1, csvOutput);

        long start = System.currentTimeMillis();
        wc.run();
        long elapsed = System.currentTimeMillis() - start;

        // Must stop within 3 seconds
        assertTrue(elapsed < 3000, "Tool must stop within 3 s when given --timeout 1, took " + elapsed + " ms");
        assertTrue(wc.isPartial(), "Execution should be marked as partial");
        assertTrue(wc.getFilesDone() < fileCount, "Should not have processed all 5,000 files within 1 second");
        assertTrue(wc.getWordsDone() > 0, "Should have processed at least some words before timeout");
        assertTrue(Files.exists(csvOutput), "Partial result CSV should be written");
    }
}
