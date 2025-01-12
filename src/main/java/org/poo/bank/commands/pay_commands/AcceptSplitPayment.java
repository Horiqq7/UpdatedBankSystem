package org.poo.bank.commands.pay_commands;

import org.poo.bank.account.Account;
import org.poo.bank.transaction.Transaction;
import org.poo.fileio.CommandInput;
import org.poo.bank.user.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class AcceptSplitPayment {
    private final List<User> users;
    private final List<CommandInput> commands;

    public AcceptSplitPayment(final List<User> users, final List<CommandInput> commands) {
        this.users = users;
        this.commands = commands;
    }

    /**
     * Găsește comanda `splitPayment` asociată pentru un `acceptSplitPayment`.
     *
     * @param timestamp Timestamp-ul comenzii `acceptSplitPayment`.
     * @return Comanda `splitPayment` asociată.
     */
    private CommandInput findSplitPaymentCommand(int timestamp) {
        for (CommandInput command : commands) {
            if ("splitPayment".equals(command.getCommand()) && command.getTimestamp() < timestamp) {
                return command; // Găsim ultima comandă `splitPayment` înainte de această comanda
            }
        }
        return null;
    }

    /**
     * Procesează o comandă de tip `acceptSplitPayment`, scăzând suma de plată din contul utilizatorului
     * care acceptă plata și creând tranzacția corespunzătoare.
     *
     * @param command Comanda de acceptare a plății.
     * @return Un mesaj de succes sau eroare.
     */
    public Map<String, Object> acceptSplitPayment(final CommandInput command) {
        String email = command.getEmail();
        int timestamp = command.getTimestamp();

        // Găsește utilizatorul care a acceptat plata
        User acceptingUser = null;
        for (User user : users) {
            if (user.getEmail().equals(email)) {
                acceptingUser = user;
                break;
            }
        }

        if (acceptingUser == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("description", "User not found");
            return error;
        }

        // Găsim comanda `splitPayment` asociată
        CommandInput splitPaymentCommand = findSplitPaymentCommand(timestamp);
        if (splitPaymentCommand == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("description", "Associated splitPayment command not found");
            return error;
        }

        // Obține informațiile din comanda `splitPayment`
        List<String> accountIBANs = splitPaymentCommand.getAccounts();
        List<Double> amountForUsers = splitPaymentCommand.getAmountForUsers();

        if (accountIBANs == null || amountForUsers == null || accountIBANs.size() != amountForUsers.size()) {
            Map<String, Object> error = new HashMap<>();
            error.put("description", "Invalid splitPayment command data");
            return error;
        }

        // Caută contul utilizatorului care a acceptat plata în lista IBAN-urilor
        for (int i = 0; i < accountIBANs.size(); i++) {
            String accountIBAN = accountIBANs.get(i);
            double amountForUser = amountForUsers.get(i);

            // Verifică dacă IBAN-ul aparține utilizatorului care a acceptat
            Account account = acceptingUser.getAccountByIBAN(accountIBAN);
            if (account != null) {
                // Deblochează suma din cont
                account.unblockFunds(amountForUser);

                // Creează tranzacția pentru această deducere
                Transaction transaction = new Transaction(
                        timestamp,
                        "Split payment deduction",
                        null, // Fără sender specific
                        accountIBAN,
                        amountForUser,
                        splitPaymentCommand.getCurrency(),
                        null, null, null, null,
                        null, // Fără destinatari specificați
                        null, null,
                        true,
                        "splitPayment"
                );

                account.addTransaction(transaction);

                // Returnează succesul
                Map<String, Object> success = new HashMap<>();
                success.put("description", "Split payment processed for IBAN " + accountIBAN);
                success.put("timestamp", timestamp);
                success.put("remainingBalance", account.getBalance());
                return success;
            }
        }

        // Dacă IBAN-ul nu a fost găsit în conturile utilizatorului
        Map<String, Object> error = new HashMap<>();
        error.put("description", "No matching IBAN found for user " + email);
        return error;
    }
}
