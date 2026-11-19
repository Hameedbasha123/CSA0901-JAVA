# CO3 Java Assignment 3
## Exception Handling and Multithreading

**Student A Name:** [Enter name]  
**Student A Register Number:** [Enter register number]  
**Student A GitHub Link:** [Paste your repository link]  
**Student B Name:** [Enter name]  
**Student B Register Number:** [Enter register number]  
**Student B GitHub Link:** [Paste your repository link]  
**Java Version:** JDK 8 or later (reference outputs taken with JDK 21)

> This is a simple student-level explanation. Read the answers, run the programs, and make sure both students can explain every question during the viva. Items marked **[Run and record]** need your own screenshot or measurement from your own computer.

---

## Q1 – Handling Failure in a Bank ATM Simulation

**Code:** `Task01/AtmWithdrawal.java`

### Shared Group Work

**Failure points and classification**

| Failure point | Exception | Category |
|---|---|---|
| Card-reader file missing | `FileNotFoundException` (subclass of `IOException`) | Checked |
| User types letters instead of a number | `NumberFormatException` | Unchecked |
| Negative amount | `IllegalArgumentException` | Unchecked |
| Amount greater than balance | `IllegalStateException` (could also be a user-defined exception) | Unchecked |
| JVM runs out of memory or the stack overflows | `OutOfMemoryError`, `StackOverflowError` | Error (should not be caught) |

The program uses `try`, a multi-catch (`IllegalArgumentException | IllegalStateException`) and `finally`. The reference output is in `Task01/expected_output.txt`.

### Student A – Exception Design

**Throwable hierarchy relevant to the ATM**
```text
Throwable
 ├── Error
 │     ├── OutOfMemoryError
 │     └── StackOverflowError
 └── Exception
       ├── IOException                    (checked)
       │     └── FileNotFoundException    (checked)
       └── RuntimeException               (unchecked)
             ├── IllegalArgumentException
             │     └── NumberFormatException
             └── IllegalStateException
```

**Table: failure point → exception → checked/unchecked → where caught**

| Failure point | Exception type | Checked / unchecked | Where it is caught |
|---|---|---|---|
| Card file missing | FileNotFoundException | Checked | First catch block in `run()` |
| Letters typed | NumberFormatException | Unchecked | Second catch block |
| Negative amount | IllegalArgumentException | Unchecked | Multi-catch block |
| Amount above balance | IllegalStateException | Unchecked | Multi-catch block |
| Any other I/O problem | IOException | Checked | Last IOException catch block |

**Order of the catch blocks:** Java checks the catch blocks from top to bottom and uses the first one that matches. A subclass must therefore come before its superclass: `FileNotFoundException` before `IOException`, and `NumberFormatException` before `IllegalArgumentException`. If the broad class were first, the narrow block could never run, and the compiler reports an error.

**What `finally` guarantees:** The line "card returned" is printed after a successful withdrawal and after every failure. It runs whether or not an exception occurs. It would not run only if the JVM stops, for example by `System.exit()` or a crash.

### Student B – Testing and Tracing

**Test table**

| # | Input (account, amount) | Path triggered | Catch block | finally executed? |
|---|---|---|---|---|
| 1 | 1001, 2000 | Normal withdrawal | None | Yes |
| 2 | 1001, abc | Letters typed | NumberFormatException | Yes |
| 3 | 1001, -500 | Negative amount | Multi-catch (IllegalArgumentException) | Yes |
| 4 | 1001, 9000 | More than balance | Multi-catch (IllegalStateException) | Yes |
| 5 | 9999, 100 | Card file missing | FileNotFoundException | Yes |

**Stack trace of an uncaught exception.** If `Integer.parseInt("abc")` is called in `main` without a catch block, Java prints a trace like the one below. Line numbers depend on the JDK, so **[Run and record]** your own trace and paste it here.
```text
Exception in thread "main" java.lang.NumberFormatException: For input string: "abc"
    at java.base/java.lang.NumberFormatException.forInputString(NumberFormatException.java:67)
    at java.base/java.lang.Integer.parseInt(Integer.java:661)
    at java.base/java.lang.Integer.parseInt(Integer.java:777)
    at AtmWithdrawal.withdraw(AtmWithdrawal.java:19)
    at AtmWithdrawal.main(AtmWithdrawal.java:50)
```
- **First line:** the thread name, the exception class and the message.
- **Top `at` lines:** code inside the Java library where the exception was created (the innermost call).
- **`AtmWithdrawal.withdraw`:** our method that called `parseInt`; this is the line to fix.
- **Last line:** the `main` method that started the whole call chain. Reading from top to bottom goes from the failure point back to the start.

**Broader catch before narrower:** If `catch (IOException e)` is written before `catch (FileNotFoundException e)`, the compiler gives an error similar to *"exception FileNotFoundException has already been caught"*. **[Run and record]** the compiler message on your computer.

### Joint Design Question
*Should the ATM catch every exception with `catch (Exception e)`?* Usually not. A single catch treats a wrong amount, a missing card file and a programming bug in the same way, so the program cannot show the right message or recover correctly. It can also hide bugs such as a `NullPointerException`. It is acceptable only at the outermost level of the program (for example in `main`) to log the problem and show a safe "service unavailable" message, after the specific exceptions have been handled earlier.

### Alternative Concept and Complexity Analysis
**Error codes instead of exceptions:**
```java
if (!scanner.hasNextInt()) return -1;     // letters typed
int amount = scanner.nextInt();
if (amount < 0) return -2;                // negative
if (amount > balance) return -3;          // insufficient
```
The caller must check the returned code after every call. If one check is forgotten, the program continues with a wrong value. With exceptions, the normal flow stays clean and the failures are handled in separate catch blocks. Error codes need an `if` check for each failure at each level; exceptions need only one try-catch around the whole operation.

**Cost:** Checking an input in advance is O(1) time and O(1) space. Creating and throwing an exception is also O(1) in the number of inputs, but it costs more in practice because Java captures the stack trace. That costs O(d) time and O(d) space, where d is the call-stack depth. For a failure that happens rarely, this extra cost is acceptable. For a failure that happens in every loop iteration, an advance check is cheaper.

---

## Q2 – User-Defined Exceptions and Propagation in an Online Examination Portal

**Code:** `Task02/StorageDemo.java`

### Shared Group Work

**Exception hierarchy**
```text
Exception
  └── ExamException                       (checked, base class for the portal)
        ├── ExamClosedException           (checked, exam window has closed)
        ├── NotRegisteredException        (checked, student not registered)
        └── SubmissionStorageException    (checked, file could not be stored)
```
`FileTooLargeException` can be added as an unchecked subclass of `IllegalArgumentException`, because an oversized file is a bad argument that the caller could have avoided by checking the size first.

**Layers:** The controller calls the service, and the service calls the storage layer. The storage layer throws `SubmissionStorageException` using `throw`, the service declares it with `throws`, and the controller finally catches it and shows a message. The original `IOException` is kept as the cause.

### Student A – Hierarchy and Propagation Design

**Checked or unchecked, and why**

| Exception | Type | Reason |
|---|---|---|
| ExamClosedException | Checked | A normal business situation that the caller must handle (show "exam closed"). |
| NotRegisteredException | Checked | The caller must decide what to do (ask the student to register). |
| SubmissionStorageException | Checked | A recoverable failure (retry or ask the student to upload again). |
| File size too large (optional) | Unchecked | A programming or validation mistake that should be prevented by an earlier check. |

**Propagation path:** `Storage.save()` → `SubmissionService.submit()` → `ExamController.handle()`. The IOException is created in `Storage.save()`, wrapped into `SubmissionStorageException`, passes through the service (declared with `throws`), and is finally handled in the controller. The student sees a message such as *"Could not store answer02.pdf"*, not a technical disk message.

### Student B – Failure Scenarios and Chaining Evidence

| Scenario | Input | Exception | Observed behaviour |
|---|---|---|---|
| Exam window closed | Submit after the end time | ExamClosedException | Message "Exam has closed" |
| Not registered | Student not in course list | NotRegisteredException | Message "Not registered for this course" |
| File too large | `answer02.pdf`, 800 KB | SubmissionStorageException (cause: IOException) | `Error: Could not store answer02.pdf` and `Cause: Disk quota exceeded` |

**Chaining:** `e.getCause()` returns the original `IOException`, and `e.printStackTrace()` prints the exception followed by a `Caused by:` section with the original trace. **[Run and record]** the full chained stack trace.

**If re-thrown without the cause:** `throw new SubmissionStorageException("Could not store", null)` loses the message "Disk quota exceeded", the original class (`IOException`) and the original line where the problem happened. Only the new, general message remains, which makes debugging much harder.

### Complete the Code
TODO 1 was completed as:
```java
throw new SubmissionStorageException("Could not store " + fileName, e);
```
Output (also in `Task02/expected_output.txt`):
```text
Saved answer01.pdf
Error: Could not store answer02.pdf
Cause: Disk quota exceeded
```
**If the IOException were not passed to the constructor:** `getCause()` would return `null`, so `e.getCause().getMessage()` in `main` would throw a `NullPointerException` and the Cause line would never be printed.

### Joint Conclusion
A meaningful `SubmissionStorageException` hides the storage technology from the user interface (disk, database or cloud can change without changing the controller). It also explains the problem in the language of the application, while the original cause is still available for the log. An `IOException` shown unchanged would force the interface to know low-level details.

### Alternative Concept and Complexity Analysis
If the custom exceptions extended `RuntimeException`, the `throws` clauses could be removed from `save()` and `submit()`, and callers would no longer be forced to write try-catch. The code is shorter, but the compiler no longer reminds the programmer that a failure is possible, so a handler can be forgotten.

**Cost:** Creating an exception captures the stack trace, which takes O(d) time and O(d) space for stack depth d. This is the same for checked and unchecked exceptions. Chaining adds only one reference per level, so a chain of k exceptions holds k objects, with total space O(k·d), because each exception object holds its own stack trace.

---

## Q3 – Resource Safety, Debugging and Robust Code

**Code:** `Task03/ReportGenerator.java`

### Shared Group Work
The program has two versions. The first uses `try–finally` with manual `close()`. The second uses try-with-resources. A `CountingReader` increases a counter when a file is opened and decreases it when closed, so leaked files can be counted.

**Reference output**
```text
Good input, manual version : 350
Open files after manual    : 1
Good input, try-with-res   : 350
Open files after twr       : 0
Manual failed on store3: true
Files left open (manual, failing input): 1
Try-with-resources failed on store3: true
Files left open (try-with-resources, failing input): 0
```
Both versions produce the same total (350) for good input. The manual version is deliberately written the way a beginner often writes it: the `finally` block closes only the first reader. That is why it leaves one file open even for good input.

### Student A – Debugging and Experimental Analysis
- **Debugging:** Put a breakpoint on the first line of `manual()` and on the `finally` block. Step through with *Step Over*. After the `finally` block, the `readers` list still holds open readers that were never closed. **[Run and record]** a screenshot of the debugger.
- **Failing input:** a list of three files where the third file does not exist. The first two files are opened, the third throws `FileNotFoundException`.
- **Files left open for the same failing input:** manual version = **1**, try-with-resources = **0** (see the output above).
- **Logging:** The exception message contains the file name, so the catch block prints which file failed. You can also use `java.util.logging.Logger` with `logger.severe("Failed on " + file)` inside the catch block. **[Run and record]** your log output.

### Student B – Design and Correctness

**Resources and where they must be closed**

| Resource | Opened | Must be closed |
|---|---|---|
| One reader per store file | When the file is opened | After its data is read, even if an error occurs |
| Output writer for the summary file | Before the summary is written | After the summary is written and flushed |

**Why the manual version can leak:** If an exception occurs between `open` and `close()`, the `close()` line is skipped. Even `finally` can leak if it is written incorrectly (for example, closing only one resource) or if `close()` of one resource throws and the later `close()` calls never run.

**Try-with-resources:** Resources declared in the parentheses are closed automatically when the block ends, whether normally or by an exception. They are closed in **reverse order of declaration** (last opened is closed first). In `twr()`, `r2` is closed before `r1`.

**Suppressed exceptions:** If the try block throws an exception and then `close()` also throws, the exception from the try block is the main exception, and the one from `close()` is attached to it as a *suppressed* exception. It can be read using `e.getSuppressed()`. Without try-with-resources, the exception from `close()` in `finally` would replace the original exception and hide it.

### Joint Engineering Decision
Try-with-resources alone is not enough when: (1) the resource does not implement `AutoCloseable`; (2) `close()` itself can fail and data is lost (for example a writer that has not flushed properly); (3) all resources are opened at the same time and the operating-system limit is reached (open and close each store file one at a time instead); (4) the output file should not exist half-written (write to a temporary file and rename it only when complete); and (5) logic errors such as a wrong total are not resource problems.

### Alternative Concept and Complexity Analysis
**Explicit `close()` in `finally`:** The solution needs a separate `try/finally` (or a nested one) for each resource, so the code is longer and easier to get wrong. **No closing at all:** The program works for small inputs, but file handles stay open until garbage collection, and "Too many open files" appears for large inputs.

For f store files:

| Version | Time | Max file handles open when a failure occurs |
|---|---|---|
| try-with-resources (one file at a time) | O(f) | 1 (closed automatically) |
| Explicit `close()` in `finally` (written correctly) | O(f) | 1 |
| Buggy manual close (as in our program) | O(f) | Up to f − 1 stay open |
| No closing | O(f) | f stay open |

The time is O(f) in every version because each file is read once; the difference is in space (open handles), not in speed.

---

## Q4 – Thread Creation and the Thread Life Cycle

**Code:** `Task04/ScannerStations.java`, `Task04/FinishOrder.java`

### Shared Group Work
Four stations scan batches of 40, 25, 30 and 15 scripts. They are created once by extending `Thread` (`StationThread`) and once by implementing `Runnable` (`StationTask`). The program uses `start()`, `sleep()`, `join()` and `isAlive()`.

**Reference output (this part is the same on every run)**
```text
State before start : NEW
isAlive after start: true
State after join   : TERMINATED
Total (Thread)     : 110
Total (Runnable)   : 110
Running in thread  : main
Running in thread  : worker
Total (Executor)   : 110
```

### Student A – Thread Design

**Thread vs Runnable:** Extending `Thread` is simple, but the class can no longer extend another class (Java has single inheritance) and the task is tied to the thread. Implementing `Runnable` separates the task from the thread, lets the class extend another class, and the same task can be passed to a thread pool. `Runnable` is preferred in almost all real programs.

**Life-cycle diagram**
```text
NEW --start()--> RUNNABLE <------------------------+
                  |   |                            |
                  |   +--sleep(ms)--> TIMED_WAITING --time over--+
                  |   +--join()/wait()--> WAITING --notify/end---+
                  |   +--waiting for a lock--> BLOCKED --lock got-+
                  |
                  +--run() ends--> TERMINATED
```
A station thread passes through **NEW → RUNNABLE → TIMED_WAITING (sleep) → RUNNABLE → TERMINATED**. The `main` thread is in **WAITING** while it calls `join()`.

**`run()` instead of `start()`:** `run()` is an ordinary method call, so the code runs in the calling thread. The evidence is in the output: `new Thread(r, "worker").run()` printed `main`, but `start()` printed `worker`.

**How `join()` works:** `main` calls `join()` on each station before it adds the totals, so the final total is printed only after every station has finished. Without `join()`, `main` could print a total of 0.

### Student B – Behavioural Experiments

**Finish order over ten runs** (from `Task04/finish_order_output.txt`, four stations, same work each)

| Run | Finish order | Run | Finish order |
|---|---|---|---|
| 1 | S3, S1, S4, S2 | 6 | S1, S2, S3, S4 |
| 2 | S1, S3, S4, S2 | 7 | S1, S2, S3, S4 |
| 3 | S2, S3, S4, S1 | 8 | S1, S2, S3, S4 |
| 4 | S1, S3, S2, S4 | 9 | S1, S2, S3, S4 |
| 5 | S2, S4, S1, S3 | 10 | S1, S2, S3, S4 |

The first five runs gave five different orders. Runs 6–10 happened to be the same, probably because the JVM had warmed up the code by then. Your results will be different, so **[Run and record]** your own ten runs.

- **Thread priorities:** Add `t[0].setPriority(Thread.MAX_PRIORITY)` and `t[3].setPriority(Thread.MIN_PRIORITY)` and run again. **[Run and record]** whether the order changes. Priorities are only hints to the scheduler, and many operating systems largely ignore them, so do not expect a guaranteed change.
- **Daemon thread:** In `FinishOrder`, a daemon thread sleeps 2 seconds, but `main()` ends first. The JVM exits without waiting for daemon threads, so "Daemon finished" is never printed. A normal (non-daemon) thread would keep the JVM alive until it finished.
- **Scheduler explanation:** The operating-system scheduler decides which RUNNABLE thread gets the CPU and for how long. This depends on the number of cores, other programs and timing, so the order is not under the program's control.

### Joint Conclusion
The same program can give different orders because the scheduler's decisions change from run to run. A correct program must not depend on the order. It must use `join()`, locks or other synchronization to get a correct result for every possible order.

### Alternative Concept and Complexity Analysis
**ExecutorService with a fixed pool:**
```java
ExecutorService pool = Executors.newFixedThreadPool(2);
for (int b : batch) pool.submit(() -> scan(b));
pool.shutdown();
pool.awaitTermination(5, TimeUnit.SECONDS);
```
This is shown in `ScannerStations.java` and gives the same total, 110. The pool reuses threads, so we do not create a new thread for each task.

For n scripts and p threads (equal work per script, time t per script), the expected completion time is about **(n / p) · t** with p worker threads. With one thread per station, the number of threads equals the number of stations, so the number of stations decides the speed; with a pool, p can be chosen. Each platform thread reserves a stack (about 1 MB by default on common 64-bit JVMs, set with `-Xss`). Memory is therefore O(number of threads × stack size): with the manual approach it grows with the number of tasks, with the pool it is limited to p stacks.

---

## Q5 – Race Conditions in a Cinema Seat-Booking System

**Code:** `Task05/SeatBooking.java`, `Task05/RaceExperiment.java`, `Task05/BookingTiming.java`

### Shared Group Work
The unsynchronized version is in `RaceExperiment.java`. The synchronized **method** version is in `SeatBooking.java` (the completed program). A synchronized **block** version and an `AtomicBoolean[]` and `ReentrantLock` version are in `BookingTiming.java`. In every synchronized version the number of seats sold is correct (see the timing output).

### Student A – Producing and Explaining the Race

**Test design:** 100 threads compete for 10 seats (10 threads per seat). `Thread.yield()` is placed between the check and the booking to widen the gap, so the race appears reliably.

**Double bookings over 10 runs** (`Task05/race_experiment_output.txt`): seats sold were 10, 17, 21, 24, 18, 10, 10, 41, 54, 48. The correct value is 10, so **7 of 10 runs double-booked**. **[Run and record]** your own ten runs.

**Interleaving of two threads for seat 3**

| Step | Thread 1 | Thread 2 |
|---|---|---|
| 1 | checks `booked[3]` → false | |
| 2 | | checks `booked[3]` → false |
| 3 | sets `booked[3] = true`, `bookingsMade++` | |
| 4 | | sets `booked[3] = true`, `bookingsMade++` |

Both threads saw the seat as free, so the seat was sold twice. Also, `bookingsMade++` is not atomic (read, add, write), so updates can even be lost.

**Critical section:** the whole check-then-book block: `if (!booked[seat]) { booked[seat] = true; bookingsMade++; }`.

### Student B – Comparing Synchronization Strategies

**Timing for 10,000 bookings** (100 threads × 100 bookings, 1000 seats, one run, `booking_timing_output.txt`)

| Strategy | Time (microseconds) | Seats sold (expected 1000) |
|---|---|---|
| synchronized method | 11136 | 1000 |
| synchronized block | 7992 | 1000 |
| AtomicBoolean[] | 9825 | 1000 |
| ReentrantLock | 9365 | 1000 |

**Honest note:** In this test most of the time is spent creating and starting 100 threads, not in locking, and the numbers change on every run. The differences above are within normal noise. **[Run and record]** the average of at least 5 runs on your computer before drawing a conclusion.

**Whole method vs critical section only:** Locking the whole method is simplest and safe. Locking only the critical section can reduce waiting if the method does other slow work (for example printing or calculating a price) that does not touch shared data. In `book()` almost the whole method is critical, so there is little to gain.

**Static synchronization:** A `static synchronized` method locks the `Class` object, not an instance. It is needed if the shared data is `static` (for example, one booking counter shared by all screens). Here the data is per screen (instance), so it is not needed.

**Intrinsic lock:** The synchronized method uses the lock of the object (`this`). The synchronized block uses the lock of the object written in the parentheses (here, a private `lock` object).

### Joint Engineering Decision
For 20 screens, I would use a **synchronized method (or `ReentrantLock`) per screen object**. Each screen has its own lock, so bookings for different screens never wait for each other, and the code is simple. One global lock for all 20 screens would make unrelated bookings wait. If a single hit film has very heavy contention, `AtomicBoolean` per seat (compare-and-set) avoids blocking.

### Complete the Code
TODO 1 was completed with `synchronized void book(int seat) { ... }` (see `SeatBooking.java`).

Output (also in `Task05/expected_output.txt`):
```text
Seats sold: 10
```
**Why the unsynchronized version may sell more than 10 seats:** Two threads can both read `booked[seat] == false` before either writes `true`; both then book and both increment `bookingsMade`, so the total can exceed 10. Because the original program has no pause between the check and the write, the error may show only occasionally, so **[Run and record]** it over 10 runs as asked (use `RaceExperiment.java` for a reliable demonstration).

### Alternative Concept and Complexity Analysis

| Approach | Solution | Time per booking under contention | Extra space for s seats |
|---|---|---|---|
| synchronized method | One lock per screen | O(1) work, but threads wait in a queue | O(1) (the lock) |
| `AtomicBoolean[]` with `compareAndSet` | Each seat is its own atomic flag; no lock | O(1); a thread retries only if another thread updates the same seat | O(s) objects |
| `ReentrantLock` | Explicit lock/unlock in try-finally | O(1) work, waiting like synchronized, can use tryLock | O(1) |
| `ConcurrentHashMap.putIfAbsent(seat, id)` | Returns the existing value if the seat is taken | O(1) average | O(s) entries |

Atomic and concurrent-map solutions let bookings for different seats proceed at the same time; a single lock lets only one booking run at a time. They use more memory (O(s)), but the lock-based solutions use only O(1) extra memory.

---

## Q6 – Deadlock and Inter-Thread Communication

**Code:** `Task06/TokenCounter.java`, `Task06/WalletTransfer.java`

### Shared Group Work
**Part A (wallets):** `WalletTransfer.java` has `unsafeTransfer` (locks the *from* account first, then the *to* account) and `safeTransfer` (always locks the account with the smaller id first). **Part B (mess counter):** `TokenCounter.java` uses `wait()`, `notifyAll()` and a circular buffer of size 5.

**Reference outputs**
```text
Deadlock detected in unsafe version: true
Safe version finished. Total money: 2000 (expected 2000)
```
```text
Sum of tokens collected: 210 (expected 210)
```

### Student A – Producer–Consumer Design

**Why `wait()` must be in a loop and a synchronized block:**
- `wait()` must be called by the thread that holds the object's monitor (inside `synchronized`); otherwise Java throws `IllegalMonitorStateException`.
- It must be inside a `while` loop because after waking up, the condition may no longer be true. Another thread could have used the token first, and a thread can also wake up without a notification (spurious wakeup). The loop re-checks the condition.

**`notify()` vs `notifyAll()`:** With several consumers *and* several producers on one lock, `notify()` could wake a thread of the wrong kind (a consumer when a producer was needed), and all threads could end up waiting forever. In *this* program there is one producer and the producer waits only when the counter is full, while consumers wait only when it is empty. Both cannot be true at the same time, so the waiting threads are always of one kind and `notify()` would also work here. `notifyAll()` is still the safer choice, because the design stays correct if more producers are added.

**Counter size never exceeds 5 or drops below 0:** `put()` waits while `count == 5` and `take()` waits while `count == 0`, so `count` always stays between 0 and 5. To show it, print `count` inside both methods after each change **[Run and record]**.

**`sleep()` vs `wait()`:**

| | `sleep()` | `wait()` |
|---|---|---|
| Class | `Thread` (static) | `Object` |
| Lock | Keeps the locks it holds | Releases the object's monitor while waiting |
| Needs synchronized | No | Yes |
| Wakes up | After the time ends | After `notify()` / `notifyAll()` (or timeout) |

### Student B – Deadlock Analysis and Prevention

**Reliable deadlock:** Thread 1 transfers from account 1 to account 2 and Thread 2 transfers from account 2 to account 1, with a short pause after the first lock. Thread 1 holds account 1 and waits for account 2; Thread 2 holds account 2 and waits for account 1. `WalletTransfer.java` detects this using `ThreadMXBean.findDeadlockedThreads()`.

**The four conditions present**

| Condition | In this program |
|---|---|
| Mutual exclusion | Only one thread can hold an account's lock |
| Hold and wait | Each thread holds one account lock while waiting for the second |
| No preemption | A lock cannot be taken away from a thread |
| Circular wait | Thread 1 waits for Thread 2 and Thread 2 waits for Thread 1 |

**Prevention by lock ordering:** Always lock the account with the smaller id first. Then no thread can hold the larger account while waiting for the smaller one, so a circle of waiting threads cannot form. This removes the circular-wait condition. In the test, 20 threads made 20,000 opposite transfers and the total remained 2000.

### Joint Conclusion
Both problems are about threads waiting for each other, and both are solved by clear rules. For deadlock, the lock-ordering rule makes the waiting chain one-directional so someone can always continue. For producer–consumer, the conditions (`count < 5`, `count > 0`) with `notifyAll()` make sure that each waiting thread is woken when its condition may be true.

### Complete the Code
TODO 1 (`put`) and TODO 2 (`take`) were completed with `while (count == buffer.length) wait();` / `while (count == 0) wait();`, updating the buffer position and calling `notifyAll()`.

Output (also in `Task06/expected_output.txt`):
```text
Sum of tokens collected: 210 (expected 210)
```
**Changing `while` to `if`:** After `wait()` returns, the thread continues without checking again. If another consumer took the token first, the woken consumer would read an empty counter, so `count` could become negative and the sum or index could go wrong, or a token might be taken twice. In this particular program the failure is timing-dependent and may not appear in every run. **[Run and record]** at least 10 runs with `if` and compare with the `while` version. Even when the output looks correct, `if` is wrong in principle.

### Alternative Concept and Complexity Analysis
- **`ArrayBlockingQueue<Integer>(5)`:** `queue.put(t)` and `queue.take()` replace the whole `wait/notifyAll` code. Each operation takes O(1) time and the queue needs O(5) = O(1) fixed space (O(capacity) in general). It is shorter and less error-prone because there is no manual loop or notification.
- **`tryLock` with a timeout:** Lock the first account with `lock.tryLock(timeout)`, then the second. If either fails, release the lock that was acquired and retry later. Each transfer takes O(1) time when there is no contention, and a deadlock can no longer wait forever. The cost is extra code and retry logic, and repeated retries can cause livelock if not handled carefully.
- **Which is less error-prone:** `ArrayBlockingQueue` is simpler and safer than hand-written `wait/notify`. Lock ordering is simple and has no retry cost, but it requires every programmer to follow the rule, while `tryLock` handles mistakes at runtime.

---

## Q7 – Design a Concurrent, Fault-Tolerant Processor from First Principles

**Code:** `Task07/ResultProcessor.java`

### Stage 1 – Interpret the Problem (Joint) | 2 Marks

**Parallel tasks and shared data**
- Each score file can be read and validated independently, so each file is one parallel task.
- Shared data: the collection of results and the success/failure counters.

**Failures and handling**

| Failure | How it is handled |
|---|---|
| File missing | `IOException` is caught inside the task; the file is recorded as "missing" |
| File corrupt (not a number) | `NumberFormatException` is caught; recorded as "corrupt" |
| Score outside 0–100 | `IllegalArgumentException`; recorded as "corrupt" |
| Unexpected error in a task | Seen as `ExecutionException` from `Future.get()`; counted as failed |
| A thread is interrupted | Interrupt flag is restored and recorded as "interrupted" |

**Measurable requirements**
1. All 10,000 files must be processed and each file counted exactly once: success + corrupt + missing = 10,000.
2. One bad file must not stop the run (0 runs aborted).
3. The summary counts must match the hand-checked set (7 success, 2 corrupt, 1 missing).
4. Processing time must be measured for 1, 2, 4 and 8 threads.

### Stage 2 – Design (Student A) | 3 Marks

**Design:** An `ExecutorService` with a fixed thread pool. For each file one `ScoreTask implements Callable<Outcome>` is submitted, and the returned `Future<Outcome>` is stored in a list. The main thread calls `get()` on each Future and updates the counters. Results are stored in a `ConcurrentHashMap<String,Integer>`. It is thread-safe, so it stays correct even if the tasks write results directly. A plain `HashMap` is not safe for concurrent writes.

**Exception design:** `ScoreTask.call()` catches every expected exception and returns an `Outcome` object with an error reason instead of throwing. So the Future always completes normally and one failure cannot stop the others. An `ExecutionException` from `get()` is still caught as a safety net.

**Class diagram**
```text
+----------------------+        +-----------------------------+
| ResultProcessor      |        | ScoreTask                   |
|----------------------|        |-----------------------------|
| process(files, n)    |------->| implements Callable<Outcome>|
+----------------------+ submit | call(): Outcome             |
        |                       +-----------------------------+
        | uses                                 | returns
        v                                      v
 ExecutorService, List<Future<Outcome>>   +-------------------+
 ConcurrentHashMap<String,Integer>        | Outcome           |
                                          | file, score, error|
                                          +-------------------+
```

**Pseudocode**
```text
create fixed thread pool of n threads
for each file: futures.add( pool.submit( ScoreTask(file) ) )
for each future:
    try   outcome = future.get()
    catch ExecutionException -> failed++ ; continue
    if outcome.ok  -> ok++ ; results.put(file, score)
    else if corrupt -> corrupt++
    else            -> missing++
pool.shutdown()
print summary
```

### Stage 3 – Implement and Measure (Student B) | 3 Marks

**Corrupt and missing files injected:** 10,000 files, of which 200 are not created (missing), 297 contain `??` (corrupt) and 9,503 are valid.

**Hand-checked small set:** 7 valid, 2 corrupt, 1 missing. Result: `Small set -> success: 7, corrupt: 2, missing: 1 (expected 7, 2, 1)`. ✔

**Timing for 10,000 files** (each task also includes a simulated 1 ms of validation work; one run, `Task07/measurement_output.txt`)

| Pool size | Time (ms) | Success | Corrupt | Missing |
|---|---|---|---|---|
| 1 | 11100 | 9503 | 297 | 200 |
| 2 | 5484 | 9503 | 297 | 200 |
| 4 | 2729 | 9503 | 297 | 200 |
| 8 | 1357 | 9503 | 297 | 200 |

In every row, 9503 + 297 + 200 = 10,000, so no file was lost. **Unhandled failures observed:** none in this run. **[Run and record]** your own timing, and describe any failure you find.

### Stage 4 – Defend the Design (Joint) | 2 Marks

**Pool size:** In our test the time almost halved each time the pool doubled. This is because the simulated work is *waiting* (sleep), which does not use the CPU, so more threads can overlap. Real file reading is also partly waiting, but real CPU work does not scale like this. For CPU-heavy work, the best pool size is about the number of CPU cores, and more threads than that give no speed-up and add switching overhead. For file I/O, a pool somewhat larger than the number of cores is reasonable, until the disk becomes the bottleneck. **I would recommend a pool of about 4–8 threads**, then test on the real machine. Our measurements were up to 8 threads, so they do not show the point where adding threads stops helping; **[Run and record]** 16 and 32 threads on your computer to find it.

**Why the summary is complete and correct:** Every file produces exactly one `Future`; the task never throws for an expected failure, and `get()` is called for every Future. Each outcome is counted in exactly one category, so success + corrupt + missing always equals the number of files.

**One remaining limitation:** The summary is kept only in memory, so if the JVM crashes in the middle of the run, all progress is lost. There is also no per-file time limit, so a file that hangs (for example on a network drive) would block `get()`; `future.get(timeout, unit)` would solve this.

### Alternative Concept and Complexity Analysis
- **Parallel stream:** `files.parallelStream().map(this::process).collect(...)`. The code is shorter, but it uses the shared common pool (usually number of cores − 1 threads), so the pool size is not under our control. An exception thrown inside the lambda stops the stream, so each element must catch its own exceptions, exactly as in `ScoreTask`.
- **One thread per file:** 10,000 threads, each reserving a stack of roughly 1 MB, means about **10,000 × 1 MB ≈ 10 GB** of stack space (virtual). This will probably fail with `OutOfMemoryError: unable to create native thread`, and the OS spends too much time switching threads. An exception in one thread kills only that thread, so each thread needs its own try-catch and a way to report its result.

| Approach | Total time | Memory for threads | Handling a failure in one file |
|---|---|---|---|
| ExecutorService (p threads) | About (n / p) · t | p × stack size | Caught inside the task; the Future still completes |
| Parallel stream | About (n / cores) · t | cores × stack size | Must be caught inside the lambda, otherwise the stream stops |
| One thread per file | About t if enough CPU and memory, but heavy switching | n × stack size (very large) | Only that thread fails, but the result must be reported separately |

Here n is the number of files, t is the time per file, and p is the pool size.

---

## MARK DISTRIBUTION

| Question | Assessed Area | Bloom's Level | Marks |
|---|---|---|---|
| Q1 | Exception hierarchy, try, catch and finally | K4 – Analyse | 6 |
| Q2 | User-defined exceptions, propagation and chaining | K4 – Analyse | 7 |
| Q3 | try-with-resources and debugging | K4 – Analyse | 7 |
| Q4 | Thread creation and life cycle | K4 – Analyse | 7 |
| Q5 | Race conditions and synchronization | K4 – Analyse | 6 |
| Q6 | Deadlock and inter-thread communication | K4 – Analyse | 7 |
| Q7 | Concurrent, fault-tolerant design from first principles | K5 – Evaluate | 10 |
| | **TOTAL** | | **50** |

---

## Individual Contribution Record

Fill in the table with your own responsibilities. The entries below follow the assignment: edit them to match what each of you actually did.

| Question | Student A – Primary Responsibility | Student B – Primary Responsibility | Joint |
|---|---|---|---|
| Q1 | Exception design (hierarchy, table, catch order, finally) | Test inputs, tracing, stack trace, wrong catch order | Failure points, implementation, design question |
| Q2 | Hierarchy diagram, checked/unchecked reasons, propagation path | Failure scenarios, chaining evidence, test table | Exception classes, throw/throws, conclusion |
| Q3 | Debugging, failing input, file counts, logging | Resource analysis, try-with-resources, suppressed exceptions | Both versions, engineering decision |
| Q4 | Thread vs Runnable, life-cycle diagram, run() vs start(), join() | Ten runs, priorities, daemon thread, scheduler explanation | Implementation, conclusion |
| Q5 | Reproduce race, interleaving, critical section | Timing, strategy comparison, static sync, intrinsic lock | Three versions, engineering decision |
| Q6 | wait/notify analysis, counter size, sleep vs wait | Deadlock, four conditions, lock ordering | Both parts, conclusion |
| Q7 | Stage 2 – Design | Stage 3 – Implement and Measure | Stage 1 and Stage 4 |

---

**Assessment verification:** Either student may be asked to explain any part of this assignment, including the part completed mainly by the other student.
