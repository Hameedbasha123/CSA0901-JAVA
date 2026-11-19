import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

// Way 1: extend Thread
class StationThread extends Thread {
    private final int scripts;
    int scanned;
    StationThread(String name, int scripts) { super(name); this.scripts = scripts; }
    @Override public void run() {
        try { Thread.sleep(50 * scripts / 10); } catch (InterruptedException e) { return; }
        scanned = scripts;
    }
}

// Way 2: implement Runnable
class StationTask implements Runnable {
    private final int scripts;
    volatile int scanned;
    StationTask(int scripts) { this.scripts = scripts; }
    @Override public void run() {
        try { Thread.sleep(50 * scripts / 10); } catch (InterruptedException e) { return; }
        scanned = scripts;
    }
}

public class ScannerStations {
    public static void main(String[] args) throws Exception {
        int[] batch = { 40, 25, 30, 15 };

        // ---- Extending Thread
        StationThread[] t = new StationThread[4];
        for (int i = 0; i < 4; i++) { t[i] = new StationThread("Station-" + (i + 1), batch[i]); }
        System.out.println("State before start : " + t[0].getState());
        for (StationThread s : t) s.start();
        System.out.println("isAlive after start: " + t[0].isAlive());
        int total = 0;
        for (StationThread s : t) { s.join(); total += s.scanned; }   // join waits for each station
        System.out.println("State after join   : " + t[0].getState());
        System.out.println("Total (Thread)     : " + total);

        // ---- Implementing Runnable
        StationTask[] tasks = new StationTask[4];
        Thread[] th = new Thread[4];
        for (int i = 0; i < 4; i++) { tasks[i] = new StationTask(batch[i]); th[i] = new Thread(tasks[i], "Runnable-" + (i + 1)); th[i].start(); }
        total = 0;
        for (int i = 0; i < 4; i++) { th[i].join(); total += tasks[i].scanned; }
        System.out.println("Total (Runnable)   : " + total);

        // ---- run() versus start()
        Runnable r = () -> System.out.println("Running in thread  : " + Thread.currentThread().getName());
        new Thread(r, "worker").run();      // plain method call, still the main thread
        Thread w = new Thread(r, "worker"); w.start(); w.join();   // a real new thread

        // ---- Executor alternative with a fixed pool of 2 threads
        ExecutorService pool = Executors.newFixedThreadPool(2);
        AtomicInteger sum = new AtomicInteger();
        for (int b : batch) pool.submit(() -> { try { Thread.sleep(50L * b / 10); } catch (InterruptedException e) { } sum.addAndGet(b); });
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);
        System.out.println("Total (Executor)   : " + sum.get());
    }
}
