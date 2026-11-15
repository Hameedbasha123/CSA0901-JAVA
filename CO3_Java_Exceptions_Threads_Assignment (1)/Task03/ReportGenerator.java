import java.io.*;
import java.nio.file.*;
import java.util.*;

public class ReportGenerator {
    static int openFiles = 0;                    // counts handles currently open

    // A reader that counts itself, so leaks can be measured.
    static class CountingReader extends BufferedReader {
        CountingReader(String path) throws IOException {
            super(new FileReader(path));
            openFiles++;
        }
        @Override public void close() throws IOException {
            super.close();
            openFiles--;
        }
    }

    // Version 1: manual close() in finally, written the buggy way: it only closes the first reader.
    static int manual(List<String> files) throws IOException {
        List<CountingReader> readers = new ArrayList<>();
        int total = 0;
        try {
            for (String f : files) {
                readers.add(new CountingReader(f));      // may throw part-way
            }
            for (CountingReader r : readers) {
                total += Integer.parseInt(r.readLine().trim());
            }
        } finally {
            if (!readers.isEmpty()) readers.get(0).close();
        }
        return total;
    }

    // Version 2: try-with-resources. Every opened reader is closed automatically.
    static int twr(String a, String b) throws IOException {
        try (CountingReader r1 = new CountingReader(a);
             CountingReader r2 = new CountingReader(b)) {      // closed in order r2, then r1
            return Integer.parseInt(r1.readLine().trim()) + Integer.parseInt(r2.readLine().trim());
        }
    }

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("sales");
        String s1 = dir.resolve("store1.txt").toString();
        String s2 = dir.resolve("store2.txt").toString();
        String s3 = dir.resolve("store3_missing.txt").toString();
        Files.write(Paths.get(s1), "100".getBytes());
        Files.write(Paths.get(s2), "250".getBytes());

        System.out.println("Good input, manual version : " + manual(Arrays.asList(s1, s2)));
        System.out.println("Open files after manual    : " + openFiles);
        openFiles = 0;
        System.out.println("Good input, try-with-res   : " + twr(s1, s2));
        System.out.println("Open files after twr       : " + openFiles);
        openFiles = 0;

        try { manual(Arrays.asList(s1, s2, s3)); }
        catch (IOException e) { System.out.println("Manual failed on store3: " + e.getMessage().contains("store3_missing")); }
        System.out.println("Files left open (manual, failing input): " + openFiles);
        openFiles = 0;

        try { twr(s1, s3); }
        catch (IOException e) { System.out.println("Try-with-resources failed on store3: " + e.getMessage().contains("store3_missing")); }
        System.out.println("Files left open (try-with-resources, failing input): " + openFiles);
    }
}
