package edu.itc.se.lab01.challenge;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class SingleThreadedWordCount {

    private static final Pattern NON_LETTERS = Pattern.compile("[^\\p{L}]+");
    public static Map<String, Long> count(Path dir) throws IOException {
        Map<String, Long> counts = new HashMap<>();
        try (Stream<Path> stream = Files.walk(dir)) {
            stream.filter(Files::isRegularFile)
                  .sorted()
                  .forEach(file -> countFile(file, counts));
        }
        return counts;
    }

    private static void countFile(Path file, Map<String, Long> counts) {
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = NON_LETTERS.split(line);
                for (String token : tokens) {
                    if (!token.isEmpty()) {
                        String word = token.toLowerCase();
                        counts.merge(word, 1L, Long::sum);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed reading file: " + file, e);
        }
    }
}
