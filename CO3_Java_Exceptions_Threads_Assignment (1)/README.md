# CO3 Java Assignment 3 – Exception Handling and Multithreading

**Student A Name:** [Enter name]  
**Student A Register Number:** [Enter register number]  
**Student B Name:** [Enter name]  
**Student B Register Number:** [Enter register number]  
**Java Version:** JDK 8 or later (tested with JDK 21)  
**Repository name:** `CSA09-<RegisterNumber>-Assignment3`

## Project contents
- `Assignment_Report.md` – answers and explanations for Q1–Q7, with Student A and Student B sections.
- `Task01/AtmWithdrawal.java` – ATM withdrawal using try, multi-catch and finally.
- `Task02/StorageDemo.java` – completed "Complete the Code" program (exception chaining).
- `Task03/ReportGenerator.java` – manual close() versus try-with-resources.
- `Task04/ScannerStations.java`, `FinishOrder.java` – thread creation, join, daemon and executor.
- `Task05/SeatBooking.java` – completed program (synchronized); `RaceExperiment.java` and `BookingTiming.java` – experiments.
- `Task06/TokenCounter.java` – completed program (wait/notifyAll); `WalletTransfer.java` – deadlock and lock ordering.
- `Task07/ResultProcessor.java` – concurrent, fault-tolerant processor for 10,000 score files.
- Each task folder contains a short `README.md` and output text files.

## How to compile and run

Open a terminal in the project folder. Use the same pattern for every task:

```bash
cd Task01
javac AtmWithdrawal.java
java AtmWithdrawal
```

| Task | Compile | Run |
|---|---|---|
| Task01 | `javac AtmWithdrawal.java` | `java AtmWithdrawal` |
| Task02 | `javac StorageDemo.java` | `java StorageDemo` |
| Task03 | `javac ReportGenerator.java` | `java ReportGenerator` |
| Task04 | `javac *.java` | `java ScannerStations` and `java FinishOrder` |
| Task05 | `javac *.java` | `java SeatBooking`, `java RaceExperiment`, `java BookingTiming` |
| Task06 | `javac *.java` | `java TokenCounter` and `java WalletTransfer` |
| Task07 | `javac ResultProcessor.java` | `java ResultProcessor` (takes about 20 seconds) |

## Before submitting
1. Both students replace the names and register numbers in this file and in the report.
2. **Both students must run every program in their own repository** (pair-work rule in the assignment).
3. Save your own screenshots of every output in the matching task folder.
4. Create the GitHub repository with the required name and share it with the faculty.
5. Make at least one commit for each task. A single upload on the last day is not accepted.
6. Add your repository link to the report and complete the Individual Contribution Record at the end of the report.

**Note:** The `*_output.txt` files are reference outputs from one run. Programs that use threads (Task04–Task07) print timings or orders that change on every run and on every computer. Run the programs yourself and capture your own screenshots. Do not claim that screenshots were taken if you have not taken them.
