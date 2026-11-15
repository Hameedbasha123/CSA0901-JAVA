import java.util.*;

/** Q4 - HashMap vs TreeMap vs LinkedHashMap for pharmacy stock, plus sorted array + binarySearch. */
@SuppressWarnings("unchecked")
public class PharmacyMaps {
    static class Medicine {
        final String code; final int stock; final int seq;
        Medicine(String c, int s, int q) { code = c; stock = s; seq = q; }
    }
    static volatile long sink;

    static String[] codes(int n) {
        List<String> l = new ArrayList<>();
        for (int i = 0; i < n; i++) l.add(String.format("MED%07d", i));
        Collections.shuffle(l, new Random(7));      // medicines are added in random code order
        return l.toArray(new String[0]);
    }
    static double ms(Runnable r) { long s = System.nanoTime(); r.run(); return (System.nanoTime() - s) / 1e6; }

    public static void main(String[] args) {
        int[] sizes = {1000, 10000, 100000};
        int LOOKUPS = 100000, runs = 5;
        for (int n : sizes) {
            String[] cs = codes(n);
            Random rnd = new Random(3);
            String[] q = new String[LOOKUPS]; for (int i = 0; i < LOOKUPS; i++) q[i] = cs[rnd.nextInt(n)];
            System.out.println("\n=== n = " + n + " medicines, " + LOOKUPS + " lookups, average of " + runs + " runs (ms) ===");
            System.out.printf("%-34s %10s %10s %14s %14s%n", "Step", "HashMap", "TreeMap", "LinkedHashMap", "SortedArray");
            double[] put = new double[4], get = new double[4], repCode = new double[4], repIns = new double[4];
            boolean[] okCode = new boolean[4], okIns = new boolean[4];
            for (int run = -1; run < runs; run++) {   // run -1 = warm-up
                Map<String, Medicine>[] maps = new Map[]{new HashMap<>(), new TreeMap<>(), new LinkedHashMap<>()};
                for (int k = 0; k < 3; k++) {
                    Map<String, Medicine> m = maps[k];
                    double p = ms(() -> { for (int i = 0; i < n; i++) m.put(cs[i], new Medicine(cs[i], i, i)); });
                    double g = ms(() -> { long s = 0; for (String c : q) s += m.get(c).stock; sink += s; });
                    List<String> codeOrder = new ArrayList<>(), insOrder = new ArrayList<>();
                    double r1 = ms(() -> {
                        if (m instanceof TreeMap) codeOrder.addAll(m.keySet());   // natural order
                        else { List<String> t = new ArrayList<>(m.keySet()); Collections.sort(t); codeOrder.addAll(t); }  // extra sort
                    });
                    double r2 = ms(() -> {
                        if (m instanceof LinkedHashMap) insOrder.addAll(m.keySet());   // natural order
                        else { List<Medicine> t = new ArrayList<>(m.values()); t.sort(Comparator.comparingInt(x -> x.seq)); for (Medicine x : t) insOrder.add(x.code); } // extra sort by seq
                    });
                    if (run >= 0) { put[k] += p; get[k] += g; repCode[k] += r1; repIns[k] += r2; }
                    okCode[k] = isSorted(codeOrder); okIns[k] = insOrder.equals(Arrays.asList(cs));
                }
                String[] arr = cs.clone();
                double p = ms(() -> Arrays.sort(arr));
                double g = ms(() -> { long s = 0; for (String c : q) s += Arrays.binarySearch(arr, c); sink += s; });
                if (run >= 0) { put[3] += p; get[3] += g; }
                okCode[3] = true;
            }
            row("Build (n puts / one sort)", put, runs);
            row("100,000 lookups", get, runs);
            row("Report: code order", repCode, runs);
            row("Report: insertion order", repIns, runs);
            System.out.printf("%-34s %10s %10s %14s %14s%n", "Code-order output correct?", okCode[0], okCode[1], okCode[2], okCode[3]);
            System.out.printf("%-34s %10s %10s %14s %14s%n", "Insertion-order output correct?", okIns[0], okIns[1], okIns[2], "n/a");
            System.out.printf("Per-lookup time (microseconds): HashMap=%.3f TreeMap=%.3f LinkedHashMap=%.3f BinarySearch=%.3f%n",
                get[0] / runs * 1000 / LOOKUPS, get[1] / runs * 1000 / LOOKUPS, get[2] / runs * 1000 / LOOKUPS, get[3] / runs * 1000 / LOOKUPS);
        }
        String[] cs = codes(50000);
        Arrays.sort(cs);
        List<String> list = new ArrayList<>(Arrays.asList(cs));
        double t = ms(() -> { for (int i = 0; i < 1000; i++) { String c = "MED9" + String.format("%06d", i); int pos = Collections.binarySearch(list, c); if (pos < 0) list.add(-pos - 1, c); } });
        System.out.printf("%n1000 inserts into a sorted ArrayList of 50,000 (binarySearch + shift): %.2f ms%n", t);
    }
    static boolean isSorted(List<String> l) { for (int i = 1; i < l.size(); i++) if (l.get(i - 1).compareTo(l.get(i)) > 0) return false; return true; }
    static void row(String name, double[] v, int runs) {
        System.out.printf("%-34s %10.2f %10.2f %14.2f %14.2f%n", name, v[0] / runs, v[1] / runs, v[2] / runs, v[3] / runs);
    }
}
