import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

public class WalletTransfer {
    static class Account {
        final int id; int balance;
        Account(int id, int balance) { this.id = id; this.balance = balance; }
    }

    // Can deadlock: locks "from" first and "to" second, so opposite transfers collide.
    static void unsafeTransfer(Account from, Account to, int amt, long pauseMs) throws InterruptedException {
        synchronized (from) {
            Thread.sleep(pauseMs);
            synchronized (to) { from.balance -= amt; to.balance += amt; }
        }
    }

    // Cannot deadlock: always lock the account with the smaller id first.
    static void safeTransfer(Account from, Account to, int amt) {
        Account first = from.id < to.id ? from : to;
        Account second = first == from ? to : from;
        synchronized (first) {
            synchronized (second) { from.balance -= amt; to.balance += amt; }
        }
    }

    public static void main(String[] args) throws Exception {
        // 1. Deadlocking version, detected with ThreadMXBean (daemon threads so the JVM can still exit)
        Account a = new Account(1, 1000), b = new Account(2, 1000);
        Thread t1 = new Thread(() -> { try { unsafeTransfer(a, b, 100, 200); } catch (InterruptedException e) { } });
        Thread t2 = new Thread(() -> { try { unsafeTransfer(b, a, 50, 200); } catch (InterruptedException e) { } });
        t1.setDaemon(true); t2.setDaemon(true);
        t1.start(); t2.start();
        Thread.sleep(600);
        ThreadMXBean mx = ManagementFactory.getThreadMXBean();
        long[] dead = mx.findDeadlockedThreads();
        System.out.println("Deadlock detected in unsafe version: " + (dead != null));

        // 2. Lock-ordering version, many opposite transfers, always completes
        Account c = new Account(1, 1000), d = new Account(2, 1000);
        Thread[] ts = new Thread[20];
        for (int i = 0; i < ts.length; i++) {
            final boolean forward = i % 2 == 0;
            ts[i] = new Thread(() -> { for (int k = 0; k < 1000; k++) { if (forward) safeTransfer(c, d, 1); else safeTransfer(d, c, 1); } });
            ts[i].start();
        }
        for (Thread t : ts) t.join();
        System.out.println("Safe version finished. Total money: " + (c.balance + d.balance) + " (expected 2000)");
    }
}
