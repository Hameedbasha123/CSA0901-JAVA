import java.util.*;

/**
 * Q7 - Diagnose, Redesign and Validate (chosen solution: Q6 triage queue and its starvation fix).
 *   Original (plain PQ)        : starves S5 patients.
 *   Naive fix  (rebuild heap)  : every service, recompute 'effective severity' of ALL waiting patients and rebuild the heap.
 *   Redesign   (static key)    : key = arrival + severity * W, computed ONCE when the patient is admitted. No re-sorting.
 */
public class AgingStudy {
    static class P { final int sev; final long arr; int eff; long key; P(int s, long a) { sev = s; arr = a; eff = s; } }
    static long comparisons;     // operation counter

    static final Comparator<P> PLAIN = (a, b) -> { comparisons++; int c = Integer.compare(a.sev, b.sev); return c != 0 ? c : Long.compare(a.arr, b.arr); };
    static final Comparator<P> EFF   = (a, b) -> { comparisons++; int c = Integer.compare(a.eff, b.eff); return c != 0 ? c : Long.compare(a.arr, b.arr); };
    static final Comparator<P> KEY   = (a, b) -> { comparisons++; int c = Long.compare(a.key, b.key); return c != 0 ? c : Long.compare(a.arr, b.arr); };

    static final int W = 100;  // aging step (ticks)

    static class Result { double ms; long comps; long maxWaitS5; long served; int s5Served, s5Total; }

    /** mode 0 = plain PQ, 1 = naive aging (rebuild every service), 2 = redesign (static aging key) */
    static Result run(int mode, int backlog, int ticks) {
        Random r = new Random(42);
        comparisons = 0;
        PriorityQueue<P> pq = new PriorityQueue<>(mode == 0 ? PLAIN : mode == 1 ? EFF : KEY);
        long clock = 0; long maxWait5 = 0, served = 0; int s5Served = 0, s5Total = 0;
        for (int i = 0; i < backlog; i++) { int sv = (i % 10 == 0) ? 5 : 1 + r.nextInt(4); if (sv == 5) s5Total++; pq.add(make(mode, sv, clock++)); }
        long start = System.nanoTime();
        for (int t = 0; t < ticks; t++) {
            pq.add(make(mode, 1 + r.nextInt(4), clock++));            // arrivals are only S1-S4
            if (mode == 1) {                                           // naive: recompute effective severity, rebuild heap
                List<P> all = new ArrayList<>(pq);
                for (P p : all) p.eff = (int) Math.max(1, p.sev - (clock - p.arr) / W);
                pq = new PriorityQueue<>(EFF); pq.addAll(all);
            }
            P p = pq.poll();
            served++;
            if (p.sev == 5) { s5Served++; maxWait5 = Math.max(maxWait5, clock - p.arr); }
        }
        Result res = new Result();
        res.ms = (System.nanoTime() - start) / 1e6; res.comps = comparisons; res.served = served;
        for (P p : pq) if (p.sev == 5) maxWait5 = Math.max(maxWait5, clock - p.arr);   // still waiting at the end
        res.maxWaitS5 = maxWait5; res.s5Served = s5Served; res.s5Total = s5Total;
        return res;
    }
    static P make(int mode, int sev, long arr) { P p = new P(sev, arr); p.key = arr + (long) sev * W; return p; }

    public static void main(String[] args) {
        int ticks = 4000;
        int[] backlogs = {500, 1000, 2000};
        String[] names = {"Original plain PQ", "Naive aging (rebuild per service)", "Redesign (static key)"};
        System.out.println("Same test for every design: backlog B patients waiting, then " + ticks + " rounds of (1 arrival S1-S4, 1 patient served). Aging step W = " + W + ".");
        System.out.println("Each cell is the average of 3 runs (time) - comparison counts and waits are deterministic.\n");
        System.out.printf("%-8s %-36s %10s %14s %12s %22s%n", "B", "Design", "time (ms)", "comparisons", "S5 served", "longest S5 wait (ticks)");
        for (int B : backlogs) {
            for (int m = 0; m < 3; m++) {
                run(m, B, ticks);                               // warm-up
                double ms = 0; Result last = null;
                for (int i = 0; i < 3; i++) { last = run(m, B, ticks); ms += last.ms; }
                System.out.printf("%-8d %-36s %10.2f %14d %12s %22d%n", B, names[m], ms / 3, last.comps, last.s5Served + "/" + last.s5Total, last.maxWaitS5);
            }
            System.out.println();
        }
        // Order check on the hand-verified Q6 example: with no long waiting the redesign still serves by severity then arrival
        PriorityQueue<P> q = new PriorityQueue<>(KEY);
        int[] sv = {3, 1, 3, 5, 1, 2};
        String[] nm = {"Ravi", "Meena", "John", "Asha", "Kumar", "Lata"};
        Map<P, String> label = new IdentityHashMap<>();
        for (int i = 0; i < sv.length; i++) { P p = make(2, sv[i], i); label.put(p, nm[i] + "(S" + sv[i] + ")"); q.add(p); }
        StringBuilder sb = new StringBuilder();
        while (!q.isEmpty()) sb.append(label.get(q.poll())).append(' ');
        System.out.println("Redesign on the Q6 six-patient example: " + sb.toString().trim());
        System.out.println("Q6 expected                            : Meena(S1) Kumar(S1) Lata(S2) Ravi(S3) John(S3) Asha(S5)");
    }
}
