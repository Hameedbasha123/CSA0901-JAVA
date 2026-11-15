import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

// Times 10,000 bookings (100 threads x 100 bookings) for four strategies. Each seat count must equal 1000.
public class BookingTiming {
    static final int SEATS = 1000, THREADS = 100, PER = 100;

    interface Booker { boolean book(int seat); int sold(); }

    static class MethodSync implements Booker {
        boolean[] b = new boolean[SEATS]; int n;
        public synchronized boolean book(int s) { if (!b[s]) { b[s] = true; n++; return true; } return false; }
        public int sold() { return n; }
    }
    static class BlockSync implements Booker {
        boolean[] b = new boolean[SEATS]; int n; final Object lock = new Object();
        public boolean book(int s) { synchronized (lock) { if (!b[s]) { b[s] = true; n++; return true; } return false; } }
        public int sold() { return n; }
    }
    static class Atomic implements Booker {
        AtomicBoolean[] b = new AtomicBoolean[SEATS]; java.util.concurrent.atomic.AtomicInteger n = new java.util.concurrent.atomic.AtomicInteger();
        Atomic() { for (int i = 0; i < SEATS; i++) b[i] = new AtomicBoolean(); }
        public boolean book(int s) { if (b[s].compareAndSet(false, true)) { n.incrementAndGet(); return true; } return false; }
        public int sold() { return n.get(); }
    }
    static class Reentrant implements Booker {
        boolean[] b = new boolean[SEATS]; int n; final ReentrantLock l = new ReentrantLock();
        public boolean book(int s) { l.lock(); try { if (!b[s]) { b[s] = true; n++; return true; } return false; } finally { l.unlock(); } }
        public int sold() { return n; }
    }

    static long time(Booker bk) throws Exception {
        Thread[] t = new Thread[THREADS];
        long t0 = System.nanoTime();
        for (int i = 0; i < THREADS; i++) {
            final int id = i;
            t[i] = new Thread(() -> { for (int k = 0; k < PER; k++) bk.book((id * PER + k) % SEATS); });
            t[i].start();
        }
        for (Thread th : t) th.join();
        return (System.nanoTime() - t0) / 1000;      // microseconds
    }

    public static void main(String[] args) throws Exception {
        Booker[] all = { new MethodSync(), new BlockSync(), new Atomic(), new Reentrant() };
        String[] names = { "synchronized method", "synchronized block ", "AtomicBoolean[]    ", "ReentrantLock      " };
        for (int i = 0; i < all.length; i++) {
            long us = time(all[i]);
            System.out.println(names[i] + ": " + us + " microseconds, seats sold = " + all[i].sold());
        }
    }
}
