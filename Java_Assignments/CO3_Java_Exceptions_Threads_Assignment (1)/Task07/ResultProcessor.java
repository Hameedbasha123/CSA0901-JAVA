import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public class ResultProcessor {

    // Result of one file: either a score or a failure reason.
    static class Outcome {
        final String file; final int score; final String error;
        Outcome(String file, int score, String error) { this.file = file; this.score = score; this.error = error; }
        boolean ok() { return error == null; }
    }

    // One Callable per file. It never lets an exception escape, so one bad file cannot stop the run.
    static class ScoreTask implements Callable<Outcome> {
        private final Path path; private final long workMs;
        ScoreTask(Path path, long workMs) { this.path = path; this.workMs = workMs; }
        @Override public Outcome call() {
            try {
                if (workMs > 0) Thread.sleep(workMs);                       // simulates validation work
                String text = new String(Files.readAllBytes(path)).trim();
                int score = Integer.parseInt(text);                         // corrupt file -> NumberFormatException
                if (score < 0 || score > 100) throw new IllegalArgumentException("score out of range");
                return new Outcome(path.getFileName().toString(), score, null);
            } catch (IOException e) {
                return new Outcome(path.getFileName().toString(), 0, "missing/unreadable");
            } catch (RuntimeException e) {
                return new Outcome(path.getFileName().toString(), 0, "corrupt");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return new Outcome(path.getFileName().toString(), 0, "interrupted");
            }
        }
    }

    static int[] process(List<Path> files, int threads, long workMs) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Future<Outcome>> futures = new ArrayList<>();
        for (Path p : files) futures.add(pool.submit(new ScoreTask(p, workMs)));
        // Concurrent collection for shared results: ConcurrentHashMap (safe if tasks wrote directly)
        ConcurrentHashMap<String, Integer> results = new ConcurrentHashMap<>();
        int ok = 0, corrupt = 0, missing = 0;
        for (Future<Outcome> f : futures) {
            Outcome o;
            try { o = f.get(); }
            catch (ExecutionException e) { corrupt++; continue; }           // unexpected failure inside a task
            if (o.ok()) { ok++; results.put(o.file, o.score); }
            else if (o.error.equals("corrupt")) corrupt++;
            else missing++;
        }
        pool.shutdown();
        return new int[] { ok, corrupt, missing, results.size() };
    }

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("results");

        // ---- Hand-checked small set: 7 valid, 2 corrupt, 1 missing
        List<Path> small = new ArrayList<>();
        for (int i = 1; i <= 7; i++) { Path p = dir.resolve("ok" + i + ".txt"); Files.write(p, String.valueOf(50 + i).getBytes()); small.add(p); }
        for (int i = 1; i <= 2; i++) { Path p = dir.resolve("bad" + i + ".txt"); Files.write(p, "xyz".getBytes()); small.add(p); }
        small.add(dir.resolve("missing1.txt"));                              // never created
        int[] r = process(small, 4, 0);
        System.out.println("Small set -> success: " + r[0] + ", corrupt: " + r[1] + ", missing: " + r[2]
                + " (expected 7, 2, 1)");

        // ---- Measurement: 10,000 files (about 3% corrupt, 2% missing)
        List<Path> big = new ArrayList<>();
        int expOk = 0, expBad = 0, expMissing = 0;
        for (int i = 0; i < 10000; i++) {
            Path p = dir.resolve("s" + i + ".txt");
            if (i % 50 == 0) { expMissing++; }                               // not created
            else if (i % 33 == 0) { Files.write(p, "??".getBytes()); expBad++; }
            else { Files.write(p, String.valueOf(i % 101).getBytes()); expOk++; }
            big.add(p);
        }
        System.out.println("Expected for 10,000 files -> success: " + expOk + ", corrupt: " + expBad + ", missing: " + expMissing);
        for (int threads : new int[] { 1, 2, 4, 8 }) {
            long t0 = System.nanoTime();
            int[] x = process(big, threads, 1);
            long ms = (System.nanoTime() - t0) / 1_000_000;
            System.out.println("Pool " + threads + " thread(s): " + ms + " ms  -> success " + x[0] + ", corrupt " + x[1] + ", missing " + x[2]);
        }
    }
}
