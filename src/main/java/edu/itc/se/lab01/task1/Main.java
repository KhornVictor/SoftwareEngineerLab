package edu.itc.se.lab01.task1;

import edu.itc.se.lab01.task1.src.Countdown;
import edu.itc.se.lab01.task1.src.CountdownTask;

public class Main {

    @SuppressWarnings("CallToThreadRun")
    public static void main(String[] args) throws InterruptedException {
        boolean sequential = args.length > 0 && args[0].equals("--sequential");

        // Thread subclasses, Runnable, lambda, virtual thread
        Thread apollo  = new Countdown("apollo");
        Thread gemini  = new Thread(new CountdownTask(), "gemini");
        Thread mercury = new Thread(() -> { CountdownTask task = new CountdownTask(); task.run(); }, "mercury");
        Thread artemis = Thread.ofVirtual().name("artemis").unstarted(new CountdownTask());

        if (sequential) {
            apollo.run();
            gemini.run();
            mercury.run();
            artemis.run();
        } else {
            apollo.start();
            gemini.start();
            mercury.start();
            artemis.start();
        }

        System.out.println("Mission control: all launched");
        
        apollo.join();
        gemini.join();
        mercury.join();
        artemis.join();

        System.out.println("Mission control: all landed");

        printThreadInfo(apollo);
        printThreadInfo(gemini);
        printThreadInfo(mercury);
        printThreadInfo(artemis);
    }

    private static void printThreadInfo(Thread thread) {
        System.out.printf(
                "%-8s virtual=%-5s state=%s%n",
                thread.getName(),
                thread.isVirtual(),
                thread.getState()
        );
    }
}