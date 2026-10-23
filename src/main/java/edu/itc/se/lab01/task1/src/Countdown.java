package edu.itc.se.lab01.task1.src;


public class Countdown extends Thread {
    public Countdown(String name) {
        super(name);
    }

    @Override
    public void run() {
        countdown();
    }

    public static void countdown() {
        String name = Thread.currentThread().getName();
        
        try {
            System.out.println(name + ": 3");
            Thread.sleep(200);
            System.out.println(name + ": 2");
            Thread.sleep(200);
            System.out.println(name + ": 1");
            Thread.sleep(200);
            System.out.println(name + ": Liftoff!");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}