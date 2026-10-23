package edu.itc.se.lab01.task1.src;

public class CountdownTask implements Runnable {
    
    @Override
    public void run() {
        Countdown.countdown();
    }
}
