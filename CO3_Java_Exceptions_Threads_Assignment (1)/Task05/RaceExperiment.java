// Runs the booking WITHOUT synchronization 10 times and counts the runs that double-booked.
public class RaceExperiment {
    static class Unsafe {
        final boolean[] booked = new boolean[10];
        int bookingsMade = 0;
        void book(int seat) {
            if (!booked[seat]) {
                Thread.yield();                 // widens the gap between check and write
                booked[seat] = true;
                bookingsMade++;
            }
        }
    }
    public static void main(String[] args) throws Exception {
        int bad = 0;
        for (int run = 1; run <= 10; run++) {
            Unsafe s = new Unsafe();
            Thread[] u = new Thread[100];
            for (int i = 0; i < u.length; i++) { final int seat = i % 10; u[i] = new Thread(() -> s.book(seat)); }
            for (Thread t : u) t.start();
            for (Thread t : u) t.join();
            System.out.println("Run " + run + ": seats sold = " + s.bookingsMade);
            if (s.bookingsMade != 10) bad++;
        }
        System.out.println("Runs with double booking: " + bad + " of 10");
    }
}
