package edu.itc.se.lab01.task4.src;

import java.io.IOException;
import java.util.concurrent.Callable;

public class FakeDownload implements Callable<String> {
    private final String url;
    private final int download;
    private final boolean slow;

    public FakeDownload(String url, int download, boolean slow) {
        this.url = url;
        this.download = download;
        this.slow = slow;
    }

    @Override 
    public String call() throws Exception {
        if (download % 50 == 0) throw new IOException("boom");
        Thread.sleep(slow ? 5000 : 100);
        return "downloaded " + url.length();
    }
}
