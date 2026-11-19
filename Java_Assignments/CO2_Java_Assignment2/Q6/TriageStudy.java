import java.util.*;

/** Q6 - hand-checked instance, heap printing, growth, starvation, and the two alternative designs. */
public class TriageStudy {
    static final Comparator<Triage2.P> CMP = Comparator.comparingInt((Triage2.P p) -> p.sev).thenComparingLong(p -> p.arr);
    static class Triage2 { static class P { final String n; final int sev; final long arr; P(String n, int s, long a) { this.n = n; sev = s; arr = a; } public String toString() { return n + "(S" + sev + ")"; } } }
    static volatile long sink;

    public static void main(String[] args) {
        // 1. Hand-checked instance with 8 patients. Expected order (worked out by hand):
        //    S1: Meena, Kumar | S2: Lata, Sita | S3: Ravi, John, Farid | S5: Asha
        String[] names = {"Ravi", "Meena", "John", "Asha", "Kumar", "Lata", "Farid", "Sita"};
        int[] sev = {3, 1, 3, 5, 1, 2, 3, 2};
        PriorityQueue<Triage2.P> pq = new PriorityQueue<>(CMP);
        for (int i = 0; i < names.length; i++) pq.add(new Triage2.P(names[i], sev[i], i));
        System.out.println("PriorityQueue printed directly (heap array order, NOT treatment order):");
        System.out.println("   " + pq);
        StringBuilder sb = new StringBuilder();
        PriorityQueue<Triage2.P> copy = new PriorityQueue<>(pq);
        while (!copy.isEmpty()) sb.append(copy.poll()).append(' ');
        String actual = sb.toString().trim();
        String expected = "Meena(S1) Kumar(S1) Lata(S2) Sita(S2) Ravi(S3) John(S3) Farid(S3) Asha(S5)";
        System.out.println("Treatment order (poll one by one): " + actual);
        System.out.println("Hand-worked expected order        : " + expected);
        System.out.println("Matches hand calculation? " + actual.equals(expected));

        // 2. Growth: add n patients, then remove n patients (n log n total => log n each)
        System.out.println("\nGrowth of PriorityQueue (average of 5 runs, ms; per-operation time in microseconds)");
        System.out.printf("%-10s %12s %14s %12s %14s%n", "n", "add n (ms)", "add/op (us)", "poll n (ms)", "poll/op (us)");
        for (int n : new int[]{10000, 100000, 1000000}) {
            double addT = 0, pollT = 0;
            for (int run = -1; run < 5; run++) {
                Random r = new Random(2);
                PriorityQueue<Triage2.P> q = new PriorityQueue<>(CMP);
                long t = System.nanoTime();
                for (int i = 0; i < n; i++) q.add(new Triage2.P("p", 1 + r.nextInt(5), i));
                double a = (System.nanoTime() - t) / 1e6;
                t = System.nanoTime();
                while (!q.isEmpty()) sink += q.poll().arr;
                double p = (System.nanoTime() - t) / 1e6;
                if (run >= 0) { addT += a; pollT += p; }
            }
            System.out.printf("%-10d %12.2f %14.3f %12.2f %14.3f%n", n, addT / 5, addT / 5 * 1000 / n, pollT / 5, pollT / 5 * 1000 / n);
        }

        // 3. Starvation: arrivals are always S1..S4, one patient is served per round; one S5 patient waits from the start
        PriorityQueue<Triage2.P> q = new PriorityQueue<>(CMP);
        long ctr = 0; Random r = new Random(4);
        q.add(new Triage2.P("Asha", 5, ctr++));
        for (int i = 0; i < 20; i++) q.add(new Triage2.P("x", 1 + r.nextInt(4), ctr++));   // initial backlog
        int round, servedAsha = -1;
        for (round = 1; round <= 100000; round++) {
            q.add(new Triage2.P("x", 1 + r.nextInt(4), ctr++));   // one arrival per round
            Triage2.P p = q.poll();                                // one served per round
            if (p.n.equals("Asha")) { servedAsha = round; break; }
        }
        System.out.println("\nStarvation test: S5 patient 'Asha' served at round " + (servedAsha < 0 ? "NEVER in 100000 rounds" : servedAsha) + " (one arrival and one service per round, arrivals are S1-S4)");

        // 4. Alternative designs: sorted LinkedList vs 5 FIFO queues vs PriorityQueue (admit n, then serve n)
        System.out.println("\nAlternative designs: admit n then serve n (ms, average of 3 runs)");
        System.out.printf("%-10s %14s %16s %16s%n", "n", "PriorityQueue", "SortedLinkedList", "5 FIFO queues");
        for (int n : new int[]{2000, 10000, 40000}) {
            double[] tt = new double[3];
            for (int run = -1; run < 3; run++) {
                Random rr = new Random(9); int[] sv = new int[n]; for (int i = 0; i < n; i++) sv[i] = 1 + rr.nextInt(5);
                long t = System.nanoTime();
                PriorityQueue<Triage2.P> a = new PriorityQueue<>(CMP);
                for (int i = 0; i < n; i++) a.add(new Triage2.P("p", sv[i], i));
                while (!a.isEmpty()) sink += a.poll().arr;
                double t1 = (System.nanoTime() - t) / 1e6;
                t = System.nanoTime();
                LinkedList<Triage2.P> ll = new LinkedList<>();
                for (int i = 0; i < n; i++) {   // insert at correct position: after last element with sev <= new sev (O(n) scan)
                    Triage2.P np = new Triage2.P("p", sv[i], i);
                    ListIterator<Triage2.P> it = ll.listIterator();
                    while (it.hasNext()) { if (it.next().sev > np.sev) { it.previous(); break; } }
                    it.add(np);
                }
                while (!ll.isEmpty()) sink += ll.removeFirst().arr;
                double t2 = (System.nanoTime() - t) / 1e6;
                t = System.nanoTime();
                @SuppressWarnings("unchecked") ArrayDeque<Triage2.P>[] lv = new ArrayDeque[6];
                for (int i = 1; i <= 5; i++) lv[i] = new ArrayDeque<>();
                for (int i = 0; i < n; i++) lv[sv[i]].addLast(new Triage2.P("p", sv[i], i));
                for (int served = 0; served < n; served++) for (int i = 1; i <= 5; i++) if (!lv[i].isEmpty()) { sink += lv[i].pollFirst().arr; break; }
                double t3 = (System.nanoTime() - t) / 1e6;
                if (run >= 0) { tt[0] += t1; tt[1] += t2; tt[2] += t3; }
            }
            System.out.printf("%-10d %14.2f %16.2f %16.2f%n", n, tt[0] / 3, tt[1] / 3, tt[2] / 3);
        }
        // 5. City hospital: 5000 patients a day
        PriorityQueue<Triage2.P> day = new PriorityQueue<>(CMP); Random d = new Random(8);
        long t = System.nanoTime();
        for (int i = 0; i < 5000; i++) day.add(new Triage2.P("p", 1 + d.nextInt(5), i));
        while (!day.isEmpty()) sink += day.poll().arr;
        System.out.printf("%nOne full day (5000 admits + 5000 serves) took %.3f ms%n", (System.nanoTime() - t) / 1e6);
    }
}
