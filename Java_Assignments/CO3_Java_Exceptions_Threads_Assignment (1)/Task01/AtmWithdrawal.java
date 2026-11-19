import java.io.FileNotFoundException;
import java.io.IOException;

public class AtmWithdrawal {
    static int balance = 5000;

    // Simulates reading the card-reader file: only account "1001" exists.
    static void readCard(String account) throws FileNotFoundException {
        if (!account.equals("1001")) {
            throw new FileNotFoundException("Card file not found for " + account);
        }
    }

    static void withdraw(String account, String amountText) throws IOException {
        readCard(account);                                   // checked exception
        int amount = Integer.parseInt(amountText);           // NumberFormatException (unchecked)
        if (amount < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        if (amount > balance) {
            throw new IllegalStateException("Insufficient balance");
        }
        balance -= amount;
        System.out.println("Dispensed " + amount + ", balance " + balance);
    }

    static void run(String account, String amountText) {
        System.out.println("Input: " + account + ", " + amountText);
        try {
            withdraw(account, amountText);
        } catch (FileNotFoundException e) {                  // narrower IOException subclass first
            System.out.println("  Caught FileNotFoundException: " + e.getMessage());
        } catch (NumberFormatException e) {                  // narrower than IllegalArgumentException
            System.out.println("  Caught NumberFormatException: " + e.getMessage());
        } catch (IllegalArgumentException | IllegalStateException e) {   // multi-catch
            System.out.println("  Caught " + e.getClass().getSimpleName() + ": " + e.getMessage());
        } catch (IOException e) {
            System.out.println("  Caught IOException: " + e.getMessage());
        } finally {
            System.out.println("  finally: card returned");
        }
    }

    public static void main(String[] args) {
        run("1001", "2000");
        run("1001", "abc");
        run("1001", "-500");
        run("1001", "9000");
        run("9999", "100");
    }
}
