import java.util.*;

/** Q5 - design tests: ties at every level, a faulty comparator, bounded generics, wildcards, stable-sort alternative. */
public class LeaderboardTests {
    static class Team implements Comparable<Team> {
        final String name; final int points, goalDiff;
        Team(String n, int p, int g) { name = n; points = p; goalDiff = g; }
        @Override public String toString() { return name + "(" + points + "," + goalDiff + ")"; }
        @Override public int compareTo(Team o) {   // Comparable version = the "natural" ranking
            int c = Integer.compare(o.points, points);
            if (c != 0) return c;
            c = Integer.compare(o.goalDiff, goalDiff);
            return c != 0 ? c : name.compareTo(o.name);
        }
    }
    // Reusable generic board: T must be comparable to itself (or a super type); works for Team, Player, College ...
    static class RankedBoard<T extends Comparable<? super T>> {
        private final List<T> items = new ArrayList<>();
        void addAll(List<? extends T> more) { items.addAll(more); }   // wildcard: accepts List<SubTeam> too
        List<T> ranking() { List<T> c = new ArrayList<>(items); Collections.sort(c); return c; }
    }
    static class StudentTeam extends Team { StudentTeam(String n, int p, int g) { super(n, p, g); } }

    // Correct comparator chain
    static final Comparator<Team> GOOD = Comparator.comparingInt((Team t) -> t.points).reversed()
            .thenComparing(Comparator.comparingInt((Team t) -> t.goalDiff).reversed())
            .thenComparing(t -> t.name);
    // Faulty 1: compares points by subtraction (overflow) 
    static final Comparator<Team> FAULTY_SUBTRACT = (a, b) -> {
        int c = b.points - a.points;
        if (c != 0) return c;
        c = b.goalDiff - a.goalDiff;
        return c != 0 ? c : a.name.compareTo(b.name);
    };
    // Faulty 2: ignores the name tie-breaker (equal items then depend on input order)
    static final Comparator<Team> FAULTY_NO_NAME = (a, b) -> {
        int c = Integer.compare(b.points, a.points);
        return c != 0 ? c : Integer.compare(b.goalDiff, a.goalDiff);
    };

    public static void main(String[] args) {
        // Data set 1: several ties at each level
        List<Team> d1 = Arrays.asList(new Team("Panthers", 9, 4), new Team("Eagles", 9, 6), new Team("Cobras", 6, 1), new Team("Bisons", 9, 4),
                new Team("Zebras", 6, 1), new Team("Aces", 6, 3), new Team("Lions", 3, 0), new Team("Hawks", 3, 0));
        List<Team> a = new ArrayList<>(d1); a.sort(GOOD);
        System.out.println("Data set 1, correct comparator: " + a);
        List<Team> rev = new ArrayList<>(d1); Collections.reverse(rev); rev.sort(GOOD);
        System.out.println("   same result for reversed input? " + a.toString().equals(rev.toString()));

        // Data set 2: designed to break faulty comparators
        List<Team> d2 = Arrays.asList(new Team("Big", 2_000_000_000, 0), new Team("Negative", -2_000_000_000, 0), new Team("Mid", 5, 1),
                new Team("Beta", 7, 2), new Team("Alpha", 7, 2));
        List<Team> g = new ArrayList<>(d2); g.sort(GOOD);
        System.out.println("\nData set 2, correct comparator      : " + g);
        System.out.println("   check: Big(2e9) vs Negative(-2e9) with subtraction -> " + (-2_000_000_000 - 2_000_000_000) + " (overflow, should be negative)");
        System.out.println("   FAULTY_SUBTRACT compare(Big, Negative) = " + FAULTY_SUBTRACT.compare(d2.get(0), d2.get(1)) + "  (positive means Big ranks BELOW Negative - wrong)");
        List<Team> f1 = new ArrayList<>(d2); f1.sort(FAULTY_SUBTRACT);
        System.out.println("   FAULTY_SUBTRACT result             : " + f1);
        List<Team> f2a = new ArrayList<>(d2), f2b = new ArrayList<>(d2); Collections.reverse(f2b);
        f2a.sort(FAULTY_NO_NAME); f2b.sort(FAULTY_NO_NAME);
        System.out.println("   FAULTY_NO_NAME, input order 1      : " + f2a);
        System.out.println("   FAULTY_NO_NAME, input order 2      : " + f2b + "  (Alpha/Beta order depends on input -> not deterministic)");

        // Generic bounded board + wildcard
        RankedBoard<Team> board = new RankedBoard<>();
        List<StudentTeam> sub = Arrays.asList(new StudentTeam("Owls", 9, 4), new StudentTeam("Moths", 9, 4));
        board.addAll(sub);                      // List<StudentTeam> accepted because of List<? extends T>
        board.addAll(Arrays.asList(new Team("Kites", 12, 0)));
        System.out.println("\nRankedBoard<Team> with a List<StudentTeam> added: " + board.ranking());

        // Alternative: three stable sorts (name, then goalDiff, then points)
        List<Team> s = new ArrayList<>(d1);
        s.sort(Comparator.comparing((Team t) -> t.name));
        s.sort(Comparator.comparingInt((Team t) -> t.goalDiff).reversed());
        s.sort(Comparator.comparingInt((Team t) -> t.points).reversed());
        System.out.println("\nThree stable sorts give the same ranking as one chain? " + s.toString().equals(a.toString()));

        // Timing: one chained sort vs three stable sorts on 200,000 teams
        Random r = new Random(11); List<Team> big = new ArrayList<>();
        for (int i = 0; i < 200000; i++) big.add(new Team("T" + r.nextInt(100000), r.nextInt(30), r.nextInt(20) - 10));
        for (int rep = 0; rep < 3; rep++) {
            List<Team> x = new ArrayList<>(big); long t0 = System.nanoTime(); x.sort(GOOD); double one = (System.nanoTime() - t0) / 1e6;
            List<Team> y = new ArrayList<>(big); t0 = System.nanoTime();
            y.sort(Comparator.comparing((Team t) -> t.name));
            y.sort(Comparator.comparingInt((Team t) -> t.goalDiff).reversed());
            y.sort(Comparator.comparingInt((Team t) -> t.points).reversed());
            double three = (System.nanoTime() - t0) / 1e6;
            if (rep == 2) System.out.printf("Sorting 200,000 teams: one comparator chain = %.1f ms, three stable sorts = %.1f ms, same result = %s%n", one, three, x.toString().equals(y.toString()));
        }
    }
}
