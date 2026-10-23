package edu.itc.se.lab01.challenge;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TestDataGenerator {

    private static final String[] BASE_WORDS = {
        "the", "and", "of", "to", "a", "in", "that", "is", "was", "he",
        "for", "it", "with", "as", "his", "on", "be", "at", "by", "i",
        "this", "had", "not", "are", "but", "from", "or", "have", "an", "they",
        "which", "one", "you", "were", "her", "all", "she", "there", "would", "their",
        "we", "him", "been", "has", "when", "who", "will", "more", "no", "if",
        "out", "so", "said", "what", "up", "its", "about", "into", "than", "them",
        "can", "only", "other", "new", "some", "could", "time", "these", "two", "may",
        "then", "do", "first", "any", "my", "now", "such", "like", "our", "over",
        "man", "me", "even", "most", "made", "after", "also", "did", "many", "before",
        "must", "through", "back", "years", "where", "much", "your", "way", "well", "down",
        "should", "because", "each", "just", "those", "people", "mr", "how", "too", "little"
    };

    public static List<String> buildVocabulary(int vocabSize) {
        List<String> vocab = new ArrayList<>(vocabSize);
        for (String w : BASE_WORDS) {
            if (vocab.size() < vocabSize) {
                vocab.add(w);
            }
        }
        int index = 0;
        while (vocab.size() < vocabSize) {
            vocab.add(toAlphaWord(index++));
        }
        return vocab;
    }

    private static String toAlphaWord(int index) {
        StringBuilder sb = new StringBuilder("x");
        int n = index;
        do {
            sb.append((char) ('a' + (n % 26)));
            n = (n / 26) - 1;
        } while (n >= 0);
        return sb.toString();
    }

    private static double[] computeZipfCdf(int n, double s) {
        double[] cdf = new double[n];
        double sum = 0.0;
        for (int i = 1; i <= n; i++) {
            sum += 1.0 / Math.pow(i, s);
        }
        double cum = 0.0;
        for (int i = 0; i < n; i++) {
            cum += (1.0 / Math.pow(i + 1, s)) / sum;
            cdf[i] = cum;
        }
        cdf[n - 1] = 1.0;
        return cdf;
    }

    private static int sampleZipf(double[] cdf, Random random) {
        double r = random.nextDouble();
        int low = 0, high = cdf.length - 1;
        while (low < high) {
            int mid = (low + high) >>> 1;
            if (cdf[mid] < r) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return low;
    }

    public static void generate(Path targetDir, int fileCount, int wordsPerFile, int vocabSize, long seed) throws IOException {
        Files.createDirectories(targetDir);
        List<String> vocab = buildVocabulary(vocabSize);
        double[] cdf = computeZipfCdf(vocabSize, 0.95);

        int threads = Math.min(Runtime.getRuntime().availableProcessors(), 16);
        try (ExecutorService pool = Executors.newFixedThreadPool(threads)) {
            for (int f = 0; f < fileCount; f++) {
                final int fileIndex = f;
                pool.submit(() -> {
                    try {
                        Path file = targetDir.resolve(String.format("file_%04d.txt", fileIndex));
                        Random rng = new Random(seed + fileIndex * 31L);
                        try (BufferedWriter bw = Files.newBufferedWriter(file)) {
                            StringBuilder line = new StringBuilder(256);
                            int wordsInLine = 0;
                            for (int w = 0; w < wordsPerFile; w++) {
                                int wordIdx = sampleZipf(cdf, rng);
                                line.append(vocab.get(wordIdx)).append(' ');
                                wordsInLine++;
                                if (wordsInLine >= 25) {
                                    bw.write(line.toString());
                                    bw.newLine();
                                    line.setLength(0);
                                    wordsInLine = 0;
                                }
                            }
                            if (!line.isEmpty()) {
                                bw.write(line.toString());
                                bw.newLine();
                            }
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
            pool.shutdown();
            try {
                pool.awaitTermination(5, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public static void main(String[] args) throws IOException {
        Path dir = Path.of(args.length > 0 ? args[0] : "./data");
        int fileCount = args.length > 1 ? Integer.parseInt(args[1]) : 200;
        int wordsPerFile = args.length > 2 ? Integer.parseInt(args[2]) : 50_000;
        int vocabSize = args.length > 3 ? Integer.parseInt(args[3]) : 2_000;

        System.out.printf("Generating %d files (~%d words each, vocabulary %d) in %s...%n",
            fileCount, wordsPerFile, vocabSize, dir.toAbsolutePath());
        long start = System.currentTimeMillis();
        generate(dir, fileCount, wordsPerFile, vocabSize, 42L);
        long elapsed = System.currentTimeMillis() - start;
        System.out.printf("Done generating %d files in %.2f s%n", fileCount, elapsed / 1000.0);
    }
}
