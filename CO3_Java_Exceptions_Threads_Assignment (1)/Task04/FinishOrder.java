import java.util.*;
import java.util.concurrent.*;

// Runs four stations ten times and records the order in which they finish.
public class FinishOrder {
    public static void main(String[] args) throws Exception {
        for (int run = 1; run <= 10; run++) {
            List<String> order = Collections.synchronizedList(new ArrayList<>());
            Thread[] t = new Thread[4];
            for (int i = 0; i < 4; i++) {
                final int id = i + 1;
                t[i] = new Thread(() -> {
                    long x = 0;
                    for (int k = 0; k < 3_000_000; k++) x += k % 7;   // same work in every station
                    order.add("S" + id);
                });
            }
            for (Thread th : t) th.start();
            for (Thread th : t) th.join();
            System.out.println("Run " + run + ": " + order);
        }
        // Daemon experiment: a daemon thread is stopped when main() ends.
        Thread d = new Thread(() -> { try { Thread.sleep(2000); System.out.println("Daemon finished"); } catch (InterruptedException e) { } });
        d.setDaemon(true);
        d.start();
        System.out.println("main() ends now, daemon is still sleeping (it will not print 'Daemon finished')");
    }
}
