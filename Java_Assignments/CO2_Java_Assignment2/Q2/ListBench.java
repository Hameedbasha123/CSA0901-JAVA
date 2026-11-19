import java.util.*;

/** Q2 - ArrayList vs LinkedList vs ArrayDeque for a playlist. */
@SuppressWarnings("unchecked")
public class ListBench {
    static volatile long sink;
    static final int OPS = 1000;

    static double ms(Runnable r) { long s = System.nanoTime(); r.run(); return (System.nanoTime() - s) / 1e6; }
    static double avg(int runs, java.util.function.Supplier<Double> s) { s.get(); double t = 0; for (int i = 0; i < runs; i++) t += s.get(); return t / runs; }

    public static void main(String[] args) {
        int[] sizes = {10000, 50000, 100000};
        int runs = 3;
        System.out.println("Time in ms (average of " + runs + " runs). Insert/remove/get use " + OPS + " operations each; add-end and iterate use n elements.");
        for (int n : sizes) {
            System.out.println("\n=== Playlist size n = " + n + " ===");
            System.out.printf("%-22s %10s %10s %10s%n", "Operation", "ArrayList", "LinkedList", "ArrayDeque");
            Random rnd = new Random(1);
            int[] pos = new int[OPS]; for (int i = 0; i < OPS; i++) pos[i] = rnd.nextInt(n);
            double[] addEnd = new double[3], insFront = new double[3], insMid = new double[3], remFront = new double[3], getK = new double[3], iter = new double[3];
            for (int k = 0; k < 3; k++) {
                final int kind = k;
                addEnd[k] = avg(runs, () -> { Collection<Integer> c = make(kind); return ms(() -> { for (int i = 0; i < n; i++) c.add(i); }); });
                insFront[k] = avg(runs, () -> { Object c = makeFilled(kind, n); return ms(() -> { for (int i = 0; i < OPS; i++) addFirst(c, i); }); });
                remFront[k] = avg(runs, () -> { Object c = makeFilled(kind, n); return ms(() -> { for (int i = 0; i < OPS; i++) removeFirst(c); }); });
                iter[k] = avg(runs, () -> { Object c = makeFilled(kind, n); return ms(() -> { long s = 0; for (int x : (Iterable<Integer>) c) s += x; sink += s; }); });
                if (k < 2) {
                    insMid[k] = avg(runs, () -> { List<Integer> c = (List<Integer>) makeFilled(kind, n); return ms(() -> { for (int i = 0; i < OPS; i++) c.add(c.size() / 2, i); }); });
                    getK[k] = avg(runs, () -> { List<Integer> c = (List<Integer>) makeFilled(kind, n); return ms(() -> { long s = 0; for (int p : pos) s += c.get(p); sink += s; }); });
                }
            }
            row("Add at end (n adds)", addEnd, true);
            row("Insert at front", insFront, true);
            row("Insert in middle", insMid, false);
            row("Remove from front", remFront, true);
            row("Get k-th (random)", getK, false);
            row("Iterate whole list", iter, true);
        }
    }
    static Collection<Integer> make(int k) { return k == 0 ? new ArrayList<>() : k == 1 ? new LinkedList<>() : new ArrayDeque<>(); }
    static Object makeFilled(int k, int n) { Collection<Integer> c = make(k); for (int i = 0; i < n; i++) c.add(i); return c; }
    static void addFirst(Object c, int v) { if (c instanceof ArrayList) ((ArrayList<Integer>) c).add(0, v); else if (c instanceof LinkedList) ((LinkedList<Integer>) c).addFirst(v); else ((ArrayDeque<Integer>) c).addFirst(v); }
    static void removeFirst(Object c) { if (c instanceof ArrayList) ((ArrayList<Integer>) c).remove(0); else if (c instanceof LinkedList) ((LinkedList<Integer>) c).removeFirst(); else ((ArrayDeque<Integer>) c).removeFirst(); }
    static void row(String name, double[] v, boolean dequeToo) {
        System.out.printf("%-22s %10.3f %10.3f %10s%n", name, v[0], v[1], dequeToo ? String.format("%.3f", v[2]) : "n/a");
    }
}
