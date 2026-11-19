# CO2 Java Assignment 2
## Strings, Collections, Generics, Comparators and Queues (Sessions 15–26)

**Student Name:** [Enter your name]  
**Register Number:** [Enter your register number]  
**GitHub Link:** [Paste your repository link]  
**Java Version:** JDK 8 or later (all measurements below were taken on OpenJDK 21)

> This is a simple student-level explanation. Read the answers, run the programs yourself, and make sure you can explain every table during the viva. Timing numbers change a little on every computer and every run; the *pattern* (which one is faster and how it grows) is what matters.

**How the measurements were taken:** every benchmark has a warm-up run (so the JIT compiler is ready), then 3–5 repeated runs that are averaged. Wherever possible I also give *operation counts* or *characters copied*, because they do not change from run to run. Timings below about 1 ms are noisy and should not be over-read.

---

## Q1 – Building Text Under Load: String, StringBuilder or StringBuffer? (6 marks)

**Files:** `Q1/TicketBench.java`, `Q1/measured_output.txt`

### Prediction (made before running)
- `String +` in a loop creates a **new String every time** because String is immutable, and each new String copies all the old characters. For n fields the copying is 1 + 2 + … + n pieces, so about n²/2 → **O(n²)**. I predicted a big difference for long tickets and a small difference for short tickets.
- `StringBuilder` and `StringBuffer` keep one growing char array, so each field is copied about once → **O(n)**. I predicted StringBuffer would be only slightly slower than StringBuilder, because every StringBuffer method is `synchronized`.
- `String.join` should be as fast as StringBuilder (it also builds once).

### Three workloads
| Workload | Fields per ticket | Tickets | Reason |
|---|---|---|---|
| W1 short | 10 | 1,000 | Small tickets – the "negligible" case |
| W2 medium | 50 | 10,000 | The example in the question |
| W3 own | 5,000 | 20 | Very long ticket (e.g. a group booking with many passengers/remarks) |

### Measured time (ms for the whole workload, average of 5 runs)
| Workload | plus (+) | StringBuilder | StringBuffer | String.join |
|---|---|---|---|---|
| W1 | 4.68 | 1.87 | 1.98 | 0.93 |
| W2 | 16.05 | 6.53 | 19.02 | 7.93 |
| W3 | 359.86 | 0.90 | 1.13 | 0.67 |

### Intermediate objects and characters copied (one ticket)
| Fields n | `+` : intermediate Strings | `+` : characters copied | Builder : characters copied |
|---|---|---|---|
| 10 | 10 | 806 | 296 |
| 50 | 50 | 20,476 | 1,646 |
| 5,000 | 5,000 | 231,083,420 | 186,680 |

**Memory (bytes allocated by the JVM while building 20 tickets of 5,000 fields):** `+` = 4,517,590 KB (about 4.3 GB), StringBuilder = 7,588 KB, String.join = 2,214 KB. The final tickets are the same size; all the extra memory in the `+` version is garbage that the garbage collector must clean up.

### Prediction vs observation
- **Correct:** `+` is only about 2.5× slower for short tickets (W1, W2) but about **400× slower** for the long ticket (W3). The gap grows with n, exactly as O(n²) vs O(n) says. The character counts (231 million vs 187 thousand) match the formula.
- **Correct:** Builder, Buffer and join are in the same range.
- **Partly wrong / noisy:** I expected StringBuffer to be consistently slightly slower than StringBuilder. W1 and W3 show that (1.98 vs 1.87, 1.13 vs 0.90) but W2 shows 19.02 vs 6.53. That large gap is most likely garbage collection or JIT noise in one run, not the cost of synchronization alone. With a single thread the lock is almost free (biased/thin locking), so StringBuffer is only a little slower.
- **Conclusion:** the difference is negligible for a short ticket, but not negligible for long ones or heavy load.

### Design decision for the multithreaded booking server
- **Per-ticket text → `StringBuilder`.** A ticket is built by one thread and no other thread sees it while it is being built, so no locking is needed.
- **Shared booking log → not a bare StringBuilder.** I tested 8 threads × 200,000 appends on one shared object (expected length 1,600,000). In repeated runs the shared `StringBuilder` gave lengths such as **1,105,202** (with `ArrayIndexOutOfBoundsException`), **1,600,000** (the race did not show that time), and **1,200,000** (exception again). The saved `measured_output.txt` happens to be a run where the race did not appear – this shows the bug is *random*, which makes it worse. `StringBuffer` always gave 1,600,000. So for a shared log, use `StringBuffer` (or better: a `ConcurrentLinkedQueue<String>` / a logging framework / synchronized block, because even StringBuffer only makes each single `append` safe – a multi-step "append two parts" is still not atomic).

### Alternative concept and complexity
- **`String.join("\n", fields)`** (or `StringJoiner`): collects the fields and builds the result once. Internally it calculates the total length first, so it creates only one array – measured fastest (0.67 ms for W3, 2,214 KB allocated). **`String.format()`** is the slowest of these because it parses the format string each time; it is fine for one ticket, not for 10,000.
- **Time:** `+` in a loop copies 1 + 2 + … + n pieces → **O(n²)** characters (each piece has length about c, total c·n²/2). StringBuilder → **O(n)** (amortised, because when the internal array is full it doubles in size). `join` → O(n).
- **Space:** `+` creates n intermediate Strings; at any moment only one is alive, but total garbage is O(n²) characters. StringBuilder keeps one buffer of O(n) (up to 2× the final size because of doubling) plus one final String. `join` needs only the result (O(n)).

---

## Q2 – When Is ArrayList an Appropriate Engineering Choice? (4 marks)

**Files:** `Q2/ListBench.java`, `Q2/measured_output.txt`

### Experiments designed
Each operation is run on playlists of **10,000 / 50,000 / 100,000** songs. Add-at-end and iterate use all n elements; insert, remove and get use 1,000 operations (the "get" positions are random).

### Results (ms, average of 3 runs)
**n = 100,000**
| Operation | ArrayList | LinkedList | ArrayDeque |
|---|---|---|---|
| Add at end (n adds) | 1.054 | 1.521 | 0.839 |
| Insert at front (1000) | 9.179 | 0.043 | 0.015 |
| Insert in middle (1000) | 4.581 | 114.300 | n/a |
| Remove from front (1000) | 8.828 | 0.027 | 0.015 |
| Get k-th (1000 random) | 0.181 | 68.437 | n/a |
| Iterate all | 0.992 | 1.397 | 0.467 |

**n = 50,000:** insert middle 5.147 (ArrayList) vs 48.398 (LinkedList); get k-th 0.457 vs 30.606; insert front 4.120 vs 0.023.  
**n = 10,000:** insert middle 0.334 vs 10.895; get k-th 0.109 vs 4.203; insert front 0.859 vs 0.768.

(Small values of 1–2 ms such as "ArrayDeque add at end = 1.853 at n = 10,000" are warm-up/GC noise; I do not draw conclusions from them.)

### Answer – when is ArrayList reasonable?
- **Get k-th:** ArrayList is O(1); LinkedList must walk, and it is about **380× slower** at n = 100,000 (68.4 vs 0.18 ms). "Jump to the k-th song" is a core playlist feature, so this alone favours ArrayList.
- **Insert in the middle:** surprisingly, ArrayList is also faster (4.6 vs 114 ms) because LinkedList needs O(n) walking to *find* the place, and the insert itself is cheap, while ArrayList's shifting is a fast `System.arraycopy`.
- **Insert/remove at the front:** ArrayList must shift every element: 9 ms per 1,000 operations at n = 100,000 (about 9 µs each) against 0.04 ms for LinkedList.
- **Boundary:** ArrayList is a reasonable choice when the playlist is mostly *append + random access + iteration + occasional edits*, and the list is up to tens of thousands of songs (a normal playlist has hundreds). I would stop recommending it when **front insertion/removal becomes frequent and n is large** (about 50,000 and above, where each front operation costs several microseconds and grows linearly with n).
- LinkedList is *not* a good replacement for the playlist in any of my tests except front insert/remove, and ArrayDeque beats it there too.

### Changed condition – "Up Next" queue
The Up Next queue adds at both ends and removes from the front. This is exactly the pattern where ArrayList is weak. In my test ArrayDeque took **0.015 ms** per 1,000 front inserts or removals at n = 100,000, compared with 9.2 / 8.8 ms for ArrayList (about 600× faster). ArrayDeque is better than LinkedList too: 0.015 vs 0.043 ms for inserts, 0.467 vs 1.397 ms to iterate, and it uses less memory. **Recommendation:** keep the playlist as `ArrayList<Song>`, and use `ArrayDeque<Song>` for "Up Next". The limitation of ArrayDeque is that it has no `get(k)` or middle insert, which Up Next does not need.

### Alternative concept and complexity
| Operation | ArrayList | LinkedList | ArrayDeque | Plain array with manual shifting |
|---|---|---|---|---|
| Add at end | O(1) amortised | O(1) | O(1) amortised | O(1) if space, else O(n) copy to a bigger array |
| Insert at front | O(n) | O(1) | O(1) amortised | O(n) – shift every element yourself |
| Insert in middle | O(n) shift (fast arraycopy) | O(n) find + O(1) link | not supported | O(n) |
| Get k-th | O(1) | O(n) | not supported | O(1) |
| Iterate all | O(n), cache-friendly | O(n), pointer chasing | O(n) | O(n) |

**Space per element:** ArrayList and ArrayDeque store one reference (4–8 bytes) per slot plus unused capacity (ArrayList grows by 1.5×, ArrayDeque doubles). LinkedList stores a separate *node object* for each song (object header + `item`, `next`, `prev` references, about 24 bytes) in addition to the song itself, so it uses several times more memory per element. A plain array has the same complexity as ArrayList (it *is* what ArrayList does inside) but the programmer must write the shifting and resizing code; it gives nothing extra except fixed capacity.

---

## Q3 – Engineer the hashCode–equals Contract (6 marks)

**Files:** `Q3/Attendance.java` (completed), `Q3/before/Attendance.java` (TODO 1 not completed), `Q3/HashContractTests.java`, `Q3/measured_output.txt`

### Complete the Code – output before and after TODO 1
- **Before** (no `hashCode()`): `Unique students: 3`  ← wrong
- **After** (`hashCode()` returns `regNo.hashCode()`): `Unique students: 2`  ← expected output

**Why different:** `HashSet.add` first calls `hashCode()` to choose a bucket. Without an override, `Object.hashCode()` is based on object identity, so the two `Student("192311001", …)` objects normally get different hash codes and go to different buckets. `equals()` is never even asked, because `HashSet` only compares objects *inside the same bucket*. The set therefore keeps both Arun objects.

### Three designed tests (prediction → observation)
| Test | Description | Predicted size | Observed |
|---|---|---|---|
| 1 | Bad class; the **same object** is added twice | 2 – same reference has the same identity hash, and `equals` is true | 2 ✔ (HashSet *seems* to work) |
| 2 | Bad class; a **new object** for each scan of the same regNo | 3 – different identity hashes → different buckets | 3 ✔ duplicates exposed. Also `contains(probe)` is `false` although `probe.equals(...)` is `true` |
| 2b | Corrected class with `hashCode()` | 2 | 2 ✔ (verification of the fix) |
| 3a | Own test: change `regNo` **after** insertion (hashCode uses regNo) | still size 1, but object impossible to find | size 1, `contains(m)=false`, `remove(m)=false`, size stays 1 ✔ |
| 3b | Own test: `TreeSet` whose `compareTo` uses *name*, two different students with the same name | 1 – TreeSet uses `compareTo` (returns 0), not `equals` | 1 ✔ (the second student is silently dropped) |

### Inquiry
- **How HashSet finds an element:** (1) compute `hashCode()`; (2) map it to a bucket index; (3) inside that bucket compare hash values and call `equals()`. Contract: *if `a.equals(b)` then `a.hashCode() == b.hashCode()`*. The reverse is not required (two different students may share a hash – this is only a collision).
- **Changing a field used in hashCode() after insertion (test 3a):** the object stays in the old bucket, but lookups now compute the new hash and look in a different bucket, so `contains` and `remove` fail. The object becomes unreachable (a "memory leak" inside the set). Lesson: keys should be **immutable** – that is why the corrected class uses `final` fields and why `String` and `Integer` make good keys.
- **Corrected class:** add `@Override public int hashCode() { return regNo.hashCode(); }` – use exactly the fields used in `equals`. Or `Objects.hash(regNo)`.
- **How to verify:** (1) Test 2b above (size 2); (2) check `contains(new Student(...same regNo...))` is `true`; (3) assert `a.equals(b)` ⇒ `a.hashCode()==b.hashCode()` for sample objects.

### Alternative concept and complexity
- **`HashMap<String, Student>` keyed by regNo:** `putIfAbsent(regNo, student)` stores one student per regNo. Using a `String` key avoids writing `hashCode` yourself (String already has a correct one). **`TreeSet<Student>` with `Comparator.comparing(s -> s.regNo)`:** equality is defined by the comparator, so hashCode is not needed.
- **Measured (200,000 scans, 49,124 unique students found; every structure agreed):** HashSet 14.9 ms, HashMap 13.8 ms, TreeSet 141.4 ms – the TreeSet was about 9–10× slower.
- **Time per scan:** HashSet/HashMap **O(1) average** (worst case O(log n) in Java 8+ after treeification of a bad bucket, O(n) in older versions); TreeSet **O(log n)** (about 16 comparisons for 50,000 students).
- **Space for n unique students:** HashSet and HashMap O(n) – a table of buckets plus one entry object per element (HashSet is a HashMap inside), and the table keeps up to 25% free slots (load factor 0.75). TreeSet O(n) – one tree node per element (about 40 bytes: key, left, right, parent, colour). TreeSet keeps students *sorted by regNo* for free, which HashSet cannot do.

---

## Q4 – Choosing a Map for a Pharmacy Inventory (6 marks)

**Files:** `Q4/PharmacyMaps.java`, `Q4/measured_output.txt`

### Design reasoning (before implementation)
| | put | get | iteration | natural order |
|---|---|---|---|---|
| HashMap | O(1) avg | O(1) avg | O(capacity + n) | none (random-looking) |
| TreeMap | O(log n) | O(log n) | O(n) | **sorted by code** → code-order report |
| LinkedHashMap | O(1) avg | O(1) avg | O(n) | **insertion order** → "order added" report |

Each report needs one "right" map. With the wrong map, extra work is needed: copy the keys and **sort them** (O(n log n)) to get code order from HashMap/LinkedHashMap, or store a sequence number in each Medicine and **sort by it** to get insertion order from HashMap/TreeMap.

### Implementation and tests
Medicine codes `MED0000000…`, added in shuffled order, with n = **1,000, 10,000 and 100,000** medicines, 100,000 random lookups, average of 5 runs.

| n | Lookup 100,000 (ms): HashMap / TreeMap / LinkedHashMap / sorted array | Report by code order (ms): HashMap / TreeMap / LinkedHashMap | Report by insertion order (ms): HashMap / TreeMap / LinkedHashMap |
|---|---|---|---|
| 1,000 | 1.92 / 14.66 / 1.31 / 16.21 | 1.93 / **0.37** / 1.65 | 0.50 / 2.11 / **0.02** |
| 10,000 | 4.16 / 39.67 / 2.46 / 45.98 | 3.35 / **3.65** / 6.33 | 6.42 / 3.54 / **0.19** |
| 100,000 | 4.11 / 71.27 / 4.82 / 61.36 | 19.13 / **11.60** / 45.15 | 21.29 / 44.03 / **1.64** |

Output order was checked in the program (`true` for every map's "right" report and also for the extra-sorted "wrong" reports), so the extra work *does* give the correct order, but costs time (e.g. 45 ms vs 11.6 ms for the code-order report at n = 100,000, and 21–44 ms vs 1.64 ms for the insertion-order report).

### Scalability inquiry
- **Acceptable response time:** a pharmacist waiting for a screen needs a result in **under about 100 ms**, and for the system to handle hundreds of lookups a day (and a safe margin of 1,000× growth) I set **1 ms per lookup** as the engineering target.
- **Measured per lookup at n = 100,000:** HashMap 0.041 µs, LinkedHashMap 0.048 µs, TreeMap 0.713 µs, binary search 0.614 µs. **All of them are far below 1 ms**, so every design is acceptable for lookup – the difference is about 15× but both are microseconds.
- **Deployment choice: `LinkedHashMap`.** It has the fastest lookup, gives the "order added" report for free (1.64 ms), and the code-order evening report costs one sort (about 45 ms at 100,000 medicines, once per evening). `TreeMap` would also be acceptable (code-order report is free, insertion order costs 44 ms) but its lookups are about 15× slower. 
- **Where the remaining cost comes from:** hash lookups – computing/using the String hash (cached after the first time) and a possible cache miss on the bucket; for TreeMap – about log₂(100,000) ≈ 17 String comparisons plus pointer chasing per lookup; for the evening report – the O(n log n) sort. With a growing n the sort is the part that will grow fastest.
- **Why not `Hashtable`:** it is a legacy class from Java 1.0, **every method is synchronized** (slow if only one thread uses it, and still not safe for compound actions such as check-then-put), it does not allow `null` keys or values, and offers no ordering. In new code use `HashMap` (single-thread) or `ConcurrentHashMap` (multi-thread).

### Alternative concept and complexity
- **Sorted array + `Arrays.binarySearch`:** keep `Medicine[]` (or `String[]` of codes) sorted by code. Measured lookup at 100,000 medicines: 61.36 ms per 100,000 lookups – like TreeMap, much slower than the hash maps.
- **Lookup:** O(log n) (HashMap O(1), TreeMap O(log n)). **Insertion:** O(n) – to keep the array sorted you must shift (measured: 1,000 inserts into a sorted ArrayList of 50,000 took 9.37 ms; HashMap O(1), TreeMap O(log n)). **Code-order report:** O(n) – just walk the array (already sorted; TreeMap also O(n)); insertion-order report is not possible without extra data. **Space:** O(n) with the smallest overhead (just references; HashMap/TreeMap need an entry object per element). The sorted array suits a **mostly read-only** inventory, but not one where stock items are added frequently.

---

## Q5 – Derive a Generic Ranking Component from a Principle (7 marks)

**Files:** `Q5/LeaderboardDemo.java` (completed), `Q5/LeaderboardTests.java`, `Q5/expected_output.txt`, `Q5/measured_output.txt`

### Design derived from the principle
The principle says that the comparison must be *transitive* and that *equal items compare as equal*. A comparison with this property is a **total ordering**. I derived:
1. **Comparator, not Comparable, for Team.** `Comparable` gives only *one* natural ordering fixed inside the class. A leaderboard needs a *policy* (points → goal difference → name) that may change (e.g. a different tie-break next season) and may be different for players or colleges. A separate `Comparator` can be passed into the `Leaderboard` without changing the data classes. (I still show a `Comparable<Team>` version in the tests for comparison.)
2. **Multi-field comparison as a chain:** `Comparator.comparingInt(Team::points).reversed().thenComparing(goalDiff reversed).thenComparing(name)`. Each level only runs when the previous level says "equal".
3. **Generic class:** `Leaderboard<T>` takes a `Comparator<? super T>` – the `? super T` (contravariant wildcard) means that a comparator written for a parent type (e.g. `Comparator<Team>`) also works for a child type (e.g. `StudentTeam`). If instead the class should sort items that carry their own order, a bounded type `RankedBoard<T extends Comparable<? super T>>` is used (tested).
4. **Where a wildcard `List<? extends T>` helps:** `addAll(List<? extends T> more)` so a `List<StudentTeam>` can be added to a `Leaderboard<Team>` (shown in the test: it worked and printed `[Kites(12,0), Moths(9,4), Owls(9,4)]`). We only *read* from the list, which is why `extends` is the right wildcard.

### Complete the Code – output
`[Eagles(9,6), Bisons(9,4), Panthers(9,4), Cobras(6,1)]` – matches the expected output.

- **TODO 1** returns `new ArrayList<>(items)` sorted with `copy.sort(order)`, so the stored list is untouched.
- **TODO 2** builds `byRank` with `comparingInt(points).reversed().thenComparing(goalDiff reversed).thenComparing(name)`.
- **Why Bisons is above Panthers:** both have 9 points and goal difference 4, so the first two levels say "equal"; the third level compares names and "Bisons" comes before "Panthers" alphabetically.

### Tests
**Data set 1 – many ties at each level** (8 teams): `[Eagles(9,6), Bisons(9,4), Panthers(9,4), Aces(6,3), Cobras(6,1), Zebras(6,1), Hawks(3,0), Lions(3,0)]`. The result is the same for the reversed input – so the ordering does not depend on input order.

**Data set 2 – built to break faulty comparators** (points of +2,000,000,000 and −2,000,000,000, plus two teams that tie completely except for the name):
- **Faulty 1 – subtraction `b.points - a.points`:** `-2,000,000,000 - 2,000,000,000` overflows to **294,967,296** (positive), so `Big` is told it ranks *below* `Negative`. Result: `[Alpha, Beta, Mid, Negative, Big]` – the team with the most points is ranked **last**. This breaks the transitivity principle. (With 32+ elements TimSort may even throw "Comparison method violates its general contract".)
- **Faulty 2 – ignoring the name tie-breaker:** Alpha/Beta come out in different order depending on the input order (`Beta, Alpha` vs `Alpha, Beta`) – so the ranking is not deterministic.
- **Correct comparator:** `[Big, Alpha, Beta, Mid, Negative]`.

### Correctness argument (reasoning, not only output)
Let the key of a team be the triple (−points, −goalDiff, name). The comparator compares triples **lexicographically**. Each component is a total order (integers with `Integer.compare`, Strings with `compareTo`). Lexicographic combination of total orders is a total order: if A < B and B < C then at the first component where A and B differ A is smaller, and for B, C likewise; taking the first component where A and C differ shows A < C (**transitive**). Two teams compare as equal only when all three components are equal (**equal items compare equal**). `Integer.compare` is used instead of subtraction, so no overflow can break this.

### Alternative concept and complexity
- **`Team implements Comparable<Team>`** (shown in the tests): the same triple comparison written once inside `compareTo`, used by `Collections.sort(list)`. One sort, O(n log n). Downside: only one ranking per class; reuse for players and colleges means writing `compareTo` again in each class (or creating a common interface).
- **Three stable sorts (name, then goalDiff, then points):** the result is the same (tested: `true`) because stable sorts keep earlier order for ties. It requires **3 sorts = 3 × O(n log n)**, which is still O(n log n) in Big-O but has about 3× the constant. Measured for 200,000 teams: one comparator chain 128.3 ms, three stable sorts 166.2 ms. Extra space: `List.sort` (TimSort) uses up to O(n) temporary array during each sort, but the three sorts reuse it one after another, so peak extra space is still O(n); the ranking copy is another O(n).
- **Easiest to reuse:** the **Comparator chain** – the `Leaderboard<T>` takes any comparator, so players, colleges and quiz teams only need a different comparator, and no data class has to change.

---

## Q6 – Priority, Order and Fairness in a Queue (8 marks)

**Files:** `Q6/Triage.java` (completed), `Q6/TriageStudy.java`, `Q6/expected_output.txt`, `Q6/measured_output.txt`

### Design
- **Why a plain Queue is not enough:** a FIFO queue (`LinkedList`/`ArrayDeque`) serves strictly in arrival order. A critical patient arriving after a minor one would wait behind them.
- **PriorityQueue with Comparator:** `Comparator.comparingInt(severity).thenComparingLong(arrival)`. The smallest severity number comes out first.
- **Arrival order among equals:** PriorityQueue is **not stable** – for equal elements the order is unspecified. So a **counter** (`arrival`) is given to each patient at admission and used as the tie-breaker. The counter only grows, so it is a strict order of arrival, and no two patients compare as equal.
- **Removing / displaying:** `poll()` removes the next patient. To display the waiting list in treatment order, copy the queue (`new PriorityQueue<>(q)`) and poll the copy repeatedly (or copy to a list and sort); the original is not harmed.

### Complete the Code – output
`Meena(S1) Kumar(S1) Lata(S2) Ravi(S3) John(S3) Asha(S5)` – matches the expected output. Ravi (counter 0) is before John (counter 2) because the arrival counter is the tie-breaker when severity is equal.

### Correctness check (8 patients, solved by hand)
Admission order: Ravi S3, Meena S1, John S3, Asha S5, Kumar S1, Lata S2, Farid S3, Sita S2. By hand: S1 → Meena, Kumar; S2 → Lata, Sita; S3 → Ravi, John, Farid; S5 → Asha.
**Program output equals the hand result** (`Matches hand calculation? true`).
**Printing the PriorityQueue directly** gave `[Meena(S1), Kumar(S1), Lata(S2), Sita(S2), Ravi(S3), John(S3), Farid(S3), Asha(S5)]` here by coincidence of this small input, but this is **not guaranteed**: `toString()` prints the internal *binary-heap array* in array order. A heap only guarantees that each parent is ≤ its children, not that the whole array is sorted. Only the head (`peek`) is guaranteed to be the next patient. Always `poll()` repeatedly to get treatment order.

### Growth investigation
Prediction: `add` is O(log n) (sift-up) and `poll` is O(log n) (sift-down), so per-operation time should grow very slowly (about one more comparison each time n doubles), and total time for n operations should grow a little faster than linearly.

| n | add n (ms) | add per op (µs) | poll n (ms) | poll per op (µs) |
|---|---|---|---|---|
| 10,000 | 3.83 | 0.383 | 4.15 | 0.415 |
| 100,000 | 6.17 | 0.062 | 29.91 | 0.299 |
| 1,000,000 | 95.13 | 0.095 | 343.59 | 0.344 |

**Observed:** 100× more patients made each `poll` cost about the same (0.3–0.4 µs; log₂ n grows only from 13 to 20). Each `add` stays well under 0.5 µs (the average heap `add` is close to O(1) for random data, worst case O(log n)). This supports the prediction. (The n = 10,000 add figure includes JIT warm-up noise.)

### Starvation (inquiry)
A severity-5 patient is always behind every severity 1–4 patient. If severe patients keep arriving at least as fast as the doctor can serve, the queue of severe patients never becomes empty and the S5 patient never reaches the front. In my test (one arrival from S1–S4 and one service per round) the S5 patient "Asha" was **never served in 100,000 rounds**.
**Fair rule:** raise priority with waiting time – I use a *static key* `arrival + severity × W` (designed and evaluated in Q7). A patient of severity s can only be overtaken by patients who arrive up to (s − s′) × W ticks *after* them, so every patient is eventually served.

### Deployment decision
One full simulated day for a city hospital with **5,000 patients** (5,000 admits + 5,000 serves) took **0.825 ms** on my laptop. Even at 1,000,000 patients, the queue operations cost about 0.3–0.4 µs each. The data structure is nowhere near the limit; the real limits are doctors, not the queue. **Yes, the design can serve 5,000 patients a day** – provided the starvation rule from Q7 is added and the system is protected from concurrent access (`PriorityQueue` is **not thread-safe**; use `PriorityBlockingQueue` or synchronise if several staff members admit patients at once).

### Alternative concept and complexity
Measured, admit n then serve n (ms): 

| n | PriorityQueue | Sorted LinkedList | 5 FIFO queues |
|---|---|---|---|
| 2,000 | 0.26 | 14.58 | 1.64 |
| 10,000 | 1.40 | 132.07 | 2.33 |
| 40,000 | 7.70 | 7,379.59 | 1.27 |

- **Sorted LinkedList** (insert each patient at the correct position): admit = O(n) (walk the list), serve = O(1) (remove head). Total O(n²) – 40,000 patients took over 7 seconds. Space O(n), with a node object per patient.
- **Array of five FIFO queues** (one per severity): admit = O(1) (add to the queue for that severity), serve = O(1) in the worst case (check at most 5 queues; a constant). Space O(n) plus 5 queue objects. It is the *fastest* and keeps arrival order for free, because each queue is FIFO. Its limit: it only works when the number of priority levels is small and fixed; an aging rule means moving patients between queues.
- **PriorityQueue:** admit O(log n), serve O(log n), space O(n) (one array). The general solution that also works if the priority is a continuous value (such as the Q7 aging key).

---

## Q7 – Diagnose, Redesign and Validate (13 marks)

**Chosen solution:** the Q6 triage queue and its starvation fix.  
**Files:** `Q7/AgingStudy.java`, `Q7/measured_output.txt`

### A. Diagnose the bottleneck (2 marks)
Q6 showed that the plain `PriorityQueue` **starves** S5 patients (0 of 50 / 100 / 200 S5 patients served in 4,000 rounds in the Q7 test). The first fix I tried was **naive aging**: every time a patient is served, recompute every waiting patient's *effective severity* (`severity − waited/W`, minimum 1) and **rebuild the heap**.
- **Where the time goes:** the rebuild. Comparisons for 4,000 rounds: plain PQ **97,024** vs naive aging **8,089,357** with a backlog of 2,000 – about **83× more** work; time 0.64 ms vs 69.12 ms.
- **Which operation grows:** the rebuild per service is O(B log B) for a backlog of B patients; comparisons grew 2,070,705 → 4,078,941 → 8,089,357 as B went 500 → 1,000 → 2,000 – it doubles when the backlog doubles.
- **Repeated work:** yes – almost all patients keep the same relative order from one service to the next, but the whole heap is re-sorted every time.
- **Characteristic of the class that causes it:** a `PriorityQueue` is only correct if the key of every element **stays fixed while the element is inside**. A time-dependent comparator breaks that, so the structure has to be rebuilt.

### B. Design objective (2 marks)
1. **Every S5 patient must eventually be served** (S5 served = S5 total in the test).
2. **No re-sorting:** work per service must stay O(log n). Measurable target: **total comparisons within 2× of the plain PriorityQueue** and time of the same order (not 80× larger).
3. **Preserve the Q6 correctness:** the hand-verified six-patient example must give the same order as before.
4. **Bound the waiting time** of S5 patients (by roughly the backlog size plus a fixed multiple of W).

### C. Design the revised solution (4 marks)
**Idea:** make the "aging" part of the **key itself**, computed once when the patient is admitted.

```
key(patient) = arrivalTime + severity * W        // computed once, never changes
PriorityQueue ordered by (key, arrivalTime)

admit(p):  p.key = p.arrival + p.severity * W ;  pq.add(p)          // O(log n)
serve():   return pq.poll()                                        // O(log n)
```
Class design:
```
Patient { int severity; long arrival; long key }
Triage  { PriorityQueue<Patient> pq (comparator: key, then arrival); long clock; W }
```
- **What changed:** the comparator no longer depends on the current time, and there is no "update waiting patients" step.
- **Property exploited:** waiting makes a patient *older*; a later arrival has a *larger* arrival value. Comparing `arrival + severity×W` shows A before B when `arrA + sevA×W < arrB + sevB×W`. A newer critical patient does not have to be moved ahead of an old minor one if the old one arrived more than (sevB − sevA)×W earlier. The aging is implied by the arithmetic, so the heap order stays valid for ever.
- **Operations avoided:** the O(B) scan and the O(B log B) rebuild per service.
- **Extra memory/preprocessing:** one `long key` per patient (8 bytes); no extra passes.
- **New trade-offs:** (1) *absolute* priority is lost – with W = 100 an S5 patient can be served before an S1 patient who arrived more than 400 ticks later; the hospital must choose W (W → ∞ gives the plain PriorityQueue back; W = 0 gives plain FIFO). (2) The rule is a fixed formula; it cannot express "doctor overrides" without extra code.

### D. Prediction before implementing (1 mark)
- **Comparisons:** close to the plain PriorityQueue (not 80× more).
- **S5 served:** all of them in 4,000 rounds.
- **Waiting time:** the longest S5 wait should be **bounded by about the backlog B plus at most 4W** (W = 100) – compare plain PQ, where it grows without limit.
- The measure in which I expect improvement is **comparisons and time per service versus naive aging**, and **fairness versus the plain queue**.

### E. Implement and validate (2 marks)
Same test conditions for all three: a backlog of B patients (10% of them S5), then 4,000 rounds of 1 arrival (S1–S4 only) and 1 service. W = 100. Time = average of 3 runs.

| B | Design | Time (ms) | Comparisons | S5 served | Longest S5 wait (ticks) |
|---|---|---|---|---|---|
| 500 | Original plain PQ | 2.31 | 83,741 | 0/50 | 4,500 |
| 500 | Naive aging (rebuild) | 66.39 | 2,070,705 | 50/50 | 501 |
| 500 | **Redesign (static key)** | 6.13 | 68,525 | 50/50 | 748 |
| 1,000 | Original plain PQ | 0.58 | 90,909 | 0/100 | 5,000 |
| 1,000 | Naive aging (rebuild) | 50.78 | 4,078,941 | 100/100 | 1,001 |
| 1,000 | **Redesign (static key)** | 0.57 | 77,027 | 100/100 | 1,247 |
| 2,000 | Original plain PQ | 0.64 | 97,024 | 0/200 | 6,000 |
| 2,000 | Naive aging (rebuild) | 69.12 | 8,089,357 | 200/200 | 2,001 |
| 2,000 | **Redesign (static key)** | 0.68 | 86,022 | 200/200 | 2,245 |

**Order check:** on the six-patient Q6 example the redesign gives `Meena(S1) Kumar(S1) Lata(S2) Ravi(S3) John(S3) Asha(S5)`, identical to the Q6 expected output.

### F. Defend the final design (2 marks)
- **Prediction supported?** Yes. Comparisons for the redesign (68,525 / 77,027 / 86,022) are the *same size or even smaller* than the plain PQ's (83,741 / 90,909 / 97,024), not larger. All S5 patients were served, and the longest S5 wait was 748 / 1,247 / 2,245 ticks, which is about B + 248, B + 247, B + 245 – inside my predicted bound of B + 4W = B + 400. (Plain PQ: 0 served; the "wait" of 4,500–6,000 is patients still waiting at the end of the test.)
- **Magnitude:** against the naive fix, comparisons fell by **about 30× (B = 500) to 94× (B = 2,000)** (2,070,705 → 68,525; 8,089,357 → 86,022) and time fell from about 50–70 ms to about 0.6 ms (for B = 1,000 and 2,000; the 6.13 ms at B = 500 is a one-time JIT warm-up effect, so the *comparison counts* are the more reliable evidence).
- **New costs/trade-offs:** absolute priority is relaxed (an S1 critical patient who arrives later than 4W after an S5 patient waits behind that patient); the choice of W is a medical policy decision, not a technical one; S5 patients wait slightly longer than under naive aging (748 vs 501) because the key is a gentler rule.
- **When the original may be preferred:** when *strict* priority is a legal or safety requirement (e.g. real-time alarm handling), or when starvation is not possible (arrival of severe cases is rare). The naive rebuild may be preferable when the fairness rule depends on information that changes in other ways and cannot be written as a fixed key (e.g. the doctor can raise a patient's priority manually) – but then it should be done only for the changed patient (`remove` + `add`, which is O(n) for `PriorityQueue.remove`; a custom indexed heap would give O(log n)).
- **Final conclusion:** the redesign meets all four objectives: it removes starvation, keeps O(log n) work per patient, keeps the Q6 results, and costs only one extra long per patient.

---

## Mark-wise checklist
| Q | Evidence in this repository |
|---|---|
| Q1 | prediction, 3 workloads, 3 implementations (+ join), time/objects/memory, thread-safety test, complexity |
| Q2 | 3 sizes × 6 operations, ArrayDeque for Up Next, complexity table |
| Q3 | 3 tests with predictions, before/after TODO 1, corrected class, alternatives |
| Q4 | 3 sizes, 3 maps + sorted array, acceptability criterion, Hashtable discussion |
| Q5 | completed generic class and comparator, tests with ties and faulty comparators, correctness proof |
| Q6 | completed priority queue, hand-checked 8 patients, growth table, starvation, alternatives |
| Q7 | A–F with Original → Naive → Redesign comparison |
