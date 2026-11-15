# CO2 Java Assignment 2 – Strings, Collections, Generics, Comparators and Queues

**Student Name:** K.Hameed Basha
**Register Number:** 192425216  
**Java Version:** JDK 8 or later (measurements in the report were taken on OpenJDK 21)  
**Repository name:** `CSA09-<RegisterNumber>-Assignment2`

## Project contents
- `Assignment_Report.md` – predictions, designs, measured evidence and explanations for Q1–Q7.
- `Q1/TicketBench.java` – String / StringBuilder / StringBuffer / join benchmark and thread-safety test.
- `Q2/ListBench.java` – ArrayList vs LinkedList vs ArrayDeque at three sizes.
- `Q3/Attendance.java` – completed "Complete the Code" program (`Q3/before/` has the version without `hashCode()`); `Q3/HashContractTests.java` – three designed tests.
- `Q4/PharmacyMaps.java` – HashMap / TreeMap / LinkedHashMap / sorted array comparison.
- `Q5/LeaderboardDemo.java` – completed generic Leaderboard and comparator; `Q5/LeaderboardTests.java` – tie and faulty-comparator tests.
- `Q6/Triage.java` – completed priority-queue program; `Q6/TriageStudy.java` – hand check, growth, starvation, alternatives.
- `Q7/AgingStudy.java` – Day-4 design challenge (diagnose, redesign, validate).
- Each task folder has a short `README.md`, `expected_output.txt` (deterministic programs) and/or `measured_output.txt` (raw benchmark output from my run).

## How to compile and run
Open a terminal in the project folder.

```bash
cd Q1 && javac TicketBench.java && java TicketBench && cd ..
cd Q2 && javac ListBench.java && java ListBench && cd ..
cd Q3 && javac Attendance.java HashContractTests.java && java Attendance && java HashContractTests && cd ..
cd Q3/before && javac Attendance.java && java Attendance && cd ../..
cd Q4 && javac PharmacyMaps.java && java PharmacyMaps && cd ..
cd Q5 && javac LeaderboardDemo.java LeaderboardTests.java && java LeaderboardDemo && java LeaderboardTests && cd ..
cd Q6 && javac Triage.java TriageStudy.java && java Triage && java TriageStudy && cd ..
cd Q7 && javac AgingStudy.java && java AgingStudy && cd ..
```
(Q1–Q2 and Q4 need about 10–30 seconds; Q6 takes a few more seconds because the sorted-LinkedList test is O(n²).)

## Before submitting
1. Replace the student name and register number in this file and in the report.
2. Run every program on your own computer – timings will differ from mine, but the pattern should be the same. If your numbers differ, **update the tables in the report with your own results.**
3. Save your own screenshots of the outputs in the task folders.
4. Create the GitHub repository named `CSA09-<RegisterNumber>-Assignment2`, add the course faculty, and upload the project.
5. Make **at least one commit per task** (Q1 … Q7) – the commit history is checked.
6. Add your GitHub link to the report and the assignment cover page.

**Note:** `expected_output.txt` and `measured_output.txt` are reference results from one run. Do not claim that screenshots were taken if you have not taken them, and be ready to explain every program in the viva. Two results vary from run to run by nature: the thread-safety test in Q1 (the shared StringBuilder race appears only sometimes) and any millisecond timing.
