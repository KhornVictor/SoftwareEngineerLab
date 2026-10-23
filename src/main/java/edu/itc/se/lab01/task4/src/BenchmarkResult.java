package edu.itc.se.lab01.task4.src;

public record BenchmarkResult(String name, int ok, int failed, int timedOut, long elapsedMs) {

    public void printSummary() {
        System.out.printf("%-42s: ok=%d failed=%d timedOut=%d in %d ms%n",
                name, ok, failed, timedOut, elapsedMs);
    }
}