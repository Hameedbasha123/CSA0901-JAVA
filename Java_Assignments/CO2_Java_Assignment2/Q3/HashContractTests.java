import java.util.*;

/** Q3 - three designed tests on the hashCode-equals contract. Predictions are printed next to observations. */
public class HashContractTests {
    static class BadStudent {   // the junior developer's class: equals() on regNo, NO hashCode()
        String regNo, name;
        BadStudent(String r, String n) { regNo = r; name = n; }
        @Override public boolean equals(Object o) { return o instanceof BadStudent && regNo.equals(((BadStudent) o).regNo); }
    }
    static class GoodStudent {  // corrected class
        final String regNo, name;
        GoodStudent(String r, String n) { regNo = r; name = n; }
        @Override public boolean equals(Object o) { return o instanceof GoodStudent && regNo.equals(((GoodStudent) o).regNo); }
        @Override public int hashCode() { return regNo.hashCode(); }
    }
    static class MutableStudent { // hashCode depends on a field that can change
        String regNo;
        MutableStudent(String r) { regNo = r; }
        @Override public boolean equals(Object o) { return o instanceof MutableStudent && regNo.equals(((MutableStudent) o).regNo); }
        @Override public int hashCode() { return regNo.hashCode(); }
    }
    static class NameOrdered implements Comparable<NameOrdered> { // compareTo on name, equals on regNo
        String regNo, name;
        NameOrdered(String r, String n) { regNo = r; name = n; }
        @Override public boolean equals(Object o) { return o instanceof NameOrdered && regNo.equals(((NameOrdered) o).regNo); }
        @Override public int hashCode() { return regNo.hashCode(); }
        @Override public int compareTo(NameOrdered o) { return name.compareTo(o.name); }
    }

    static void report(String test, int predicted, int observed) {
        System.out.println(test + "\n   predicted size = " + predicted + ", observed size = " + observed + (predicted == observed ? "   (prediction correct)" : "   (prediction differs)"));
    }

    public static void main(String[] args) {
        Set<BadStudent> s1 = new HashSet<>();
        BadStudent arun = new BadStudent("192311001", "Arun");
        s1.add(arun); s1.add(new BadStudent("192311002", "Divya")); s1.add(arun);
        report("Test 1 - BadStudent, the SAME object scanned twice (HashSet seems to work)", 2, s1.size());

        Set<BadStudent> s2 = new HashSet<>();
        s2.add(new BadStudent("192311001", "Arun")); s2.add(new BadStudent("192311002", "Divya")); s2.add(new BadStudent("192311001", "Arun"));
        report("Test 2 - BadStudent, a NEW object for each scan (duplicates exposed)", 3, s2.size());
        BadStudent probe = new BadStudent("192311001", "Arun");
        System.out.println("   probe.equals(first element)? " + probe.equals(s2.iterator().next()) + "  |  s2.contains(probe) = " + s2.contains(probe));

        Set<GoodStudent> s2b = new HashSet<>();
        s2b.add(new GoodStudent("192311001", "Arun")); s2b.add(new GoodStudent("192311002", "Divya")); s2b.add(new GoodStudent("192311001", "Arun"));
        report("Test 2b - GoodStudent with hashCode() (verification of the fix)", 2, s2b.size());

        Set<MutableStudent> s3 = new HashSet<>();
        MutableStudent m = new MutableStudent("192311001");
        s3.add(m);
        m.regNo = "192311999";
        System.out.println("Test 3a - regNo changed AFTER insertion (prediction: set still has size 1 but the object cannot be found)");
        System.out.println("   size = " + s3.size() + ", contains(m) = " + s3.contains(m) + ", remove(m) = " + s3.remove(m) + ", size after remove = " + s3.size());
        System.out.println("   contains(new MutableStudent(\"192311001\")) = " + s3.contains(new MutableStudent("192311001")) + "  (same bucket as old hash, but equals() now fails)");

        Set<NameOrdered> s4 = new TreeSet<>();
        s4.add(new NameOrdered("192311001", "Arun")); s4.add(new NameOrdered("192311002", "Arun"));
        report("Test 3b - TreeSet ordered by name; two DIFFERENT students with the same name (a set based on equals would hold 2)", 1, s4.size());

        int scans = 200000, unique = 50000;
        String[] ids = new String[scans];
        Random r = new Random(5); for (int i = 0; i < scans; i++) ids[i] = "R" + r.nextInt(unique);
        for (int rep = 0; rep < 3; rep++) {
            long t = System.nanoTime(); Set<GoodStudent> hs = new HashSet<>(); for (String id : ids) hs.add(new GoodStudent(id, "x")); double a = (System.nanoTime() - t) / 1e6;
            t = System.nanoTime(); Map<String, GoodStudent> hm = new HashMap<>(); for (String id : ids) hm.putIfAbsent(id, new GoodStudent(id, "x")); double b = (System.nanoTime() - t) / 1e6;
            t = System.nanoTime(); Set<GoodStudent> ts = new TreeSet<>(Comparator.comparing((GoodStudent g) -> g.regNo)); for (String id : ids) ts.add(new GoodStudent(id, "x")); double c = (System.nanoTime() - t) / 1e6;
            if (rep == 2) {
                System.out.printf("%nDe-duplicating %d scans (%d unique students), ms: HashSet=%.1f  HashMap=%.1f  TreeSet=%.1f%n", scans, unique, a, b, c);
                System.out.println("Final sizes: " + hs.size() + " " + hm.size() + " " + ts.size());
            }
        }
    }
}
