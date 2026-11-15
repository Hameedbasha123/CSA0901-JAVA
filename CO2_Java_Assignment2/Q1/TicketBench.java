import java.util.*;

/** Q1 - String (+), StringBuilder, StringBuffer, String.join for building e-tickets. */
public class TicketBench {
    static String[] fields(int n) {
        String[] f = new String[n];
        for (int i = 0; i < n; i++) f[i] = "Field" + i + "=Value" + (i * 7 % 1000);
        return f;
    }
    static String viaPlus(String[] f) { String s = ""; for (String x : f) s += x + "\n"; return s; }
    static String viaBuilder(String[] f) { StringBuilder b = new StringBuilder(); for (String x : f) b.append(x).append('\n'); return b.toString(); }
    static String viaBuffer(String[] f) { StringBuffer b = new StringBuffer(); for (String x : f) b.append(x).append('\n'); return b.toString(); }
    static String viaJoin(String[] f) { return String.join("\n", f); }

    interface Maker { String make(String[] f); }
    static volatile int sink;

    static double timeMs(Maker m, String[] f, int tickets, int runs) {
        for (int i = 0; i < 2; i++) for (int t = 0; t < Math.max(1, tickets / 10); t++) sink += m.make(f).length(); // warm-up
        double total = 0;
        for (int r = 0; r < runs; r++) {
            long s = System.nanoTime();
            for (int t = 0; t < tickets; t++) sink += m.make(f).length();
            total += (System.nanoTime() - s) / 1e6;
        }
        return total / runs;
    }
    static long memUsedKB() { System.gc(); Runtime r = Runtime.getRuntime(); return (r.totalMemory() - r.freeMemory()) / 1024; }

    public static void main(String[] args) throws Exception {
        int[][] workloads = {{10, 1000}, {50, 10000}, {5000, 20}};   // {fields per ticket, tickets}
        String[] names = {"W1 short: 10 fields x 1000 tickets", "W2 medium: 50 fields x 10000 tickets", "W3 own long: 5000 fields x 20 tickets"};
        int runs = 5;
        System.out.println("Averaged over " + runs + " runs (ms for the whole workload)");
        System.out.printf("%-40s %10s %10s %10s %10s%n", "Workload", "plus(+)", "Builder", "Buffer", "join");
        for (int w = 0; w < workloads.length; w++) {
            String[] f = fields(workloads[w][0]);
            int t = workloads[w][1];
            System.out.printf("%-40s %10.2f %10.2f %10.2f %10.2f%n", names[w],
                timeMs(TicketBench::viaPlus, f, t, runs), timeMs(TicketBench::viaBuilder, f, t, runs),
                timeMs(TicketBench::viaBuffer, f, t, runs), timeMs(TicketBench::viaJoin, f, t, runs));
        }
        System.out.println();
        System.out.println("Estimated intermediate String objects and characters copied for ONE ticket");
        System.out.printf("%-12s %14s %18s %22s%n", "fields (n)", "plus: objects", "plus: chars copied", "builder: chars copied");
        for (int n : new int[]{10, 50, 5000}) {
            String[] f = fields(n);
            long copied = 0, len = 0;
            for (String x : f) { len += x.length() + 1; copied += len; }      // each += copies the whole string so far
            System.out.printf("%-12d %14d %18d %22d%n", n, n, copied, len * 2); // builder: append copies each field once + toString copies once
        }
        System.out.println();
        // Memory: bytes allocated by the JVM while building 20 tickets of 5000 fields (more reliable than free-memory before/after)
        com.sun.management.ThreadMXBean mx = (com.sun.management.ThreadMXBean) java.lang.management.ManagementFactory.getThreadMXBean();
        String[] f = fields(5000);
        long tid = Thread.currentThread().getId();
        long[] alloc = new long[3];
        Maker[] mk = {TicketBench::viaPlus, TicketBench::viaBuilder, TicketBench::viaJoin};
        for (int k = 0; k < 3; k++) {
            long before = mx.getThreadAllocatedBytes(tid);
            for (int i = 0; i < 20; i++) sink += mk[k].make(f).length();
            alloc[k] = (mx.getThreadAllocatedBytes(tid) - before) / 1024;
        }
        System.out.println("Memory allocated while building 20 tickets x 5000 fields (KB): plus=" + alloc[0] + "  builder=" + alloc[1] + "  join=" + alloc[2]);
        System.out.println();
        final int THREADS = 8, PER = 200000;
        StringBuilder sb = new StringBuilder(); StringBuffer sf = new StringBuffer();
        String[] err = {"none"};
        for (int pass = 0; pass < 2; pass++) {
            final boolean useBuilder = pass == 0;
            java.util.concurrent.CountDownLatch go = new java.util.concurrent.CountDownLatch(1);
            Thread[] ts = new Thread[THREADS];
            for (int i = 0; i < THREADS; i++) {
                ts[i] = new Thread(() -> {
                    try { go.await(); for (int k = 0; k < PER; k++) { if (useBuilder) sb.append("x"); else sf.append("x"); } }
                    catch (Exception e) { err[0] = e.getClass().getSimpleName(); }
                });
                ts[i].start();
            }
            go.countDown();                       // all threads start together
            for (Thread t : ts) t.join();
        }
        System.out.println("Shared log, " + THREADS + " threads x " + PER + " appends, expected length " + (THREADS * PER));
        System.out.println("StringBuilder length = " + sb.length() + "   (exception: " + err[0] + ")");
        System.out.println("StringBuffer  length = " + sf.length());
    }
}
