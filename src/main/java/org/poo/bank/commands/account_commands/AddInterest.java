package org.poo.bank.commands.account_commands;

import org.poo.bank.account.Account;
import org.poo.bank.transaction.Transaction;
import org.poo.fileio.CommandInput;
import org.poo.bank.user.User;

import java.util.List;
import java.util.Map;

public final class AddInterest {
    private final List<User> users;
    private static final int PERCENTAGE_DIVISOR = 100;

    public AddInterest(final List<User> users) {
        this.users = users;
    }

    public List<Map<String, Object>> addInterest(final CommandInput command) {
        String targetIBAN = command.getAccount();
        int currentTimestamp = command.getTimestamp();

        Account targetAccount = null;
        User targetUser = null; // Variabilă pentru a reține utilizatorul găsit.

        for (User user : users) {
            targetAccount = user.getAccountByIBAN(targetIBAN);
            if (targetAccount != null) {
                targetUser = user; // Salvează utilizatorul găsit.
                break;
            }
        }

        if (targetAccount == null) {
            Map<String, Object> output = Map.of(
                    "description", "Account not found",
                    "timestamp", currentTimestamp
            );

            Map<String, Object> response = Map.of(
                    "command", "addInterest",
                    "output", output,
                    "timestamp", currentTimestamp
            );

            return List.of(response);
        }

        if (!"savings".equals(targetAccount.getType())) {
            Map<String, Object> output = Map.of(
                    "description", "This is not a savings account",
                    "timestamp", currentTimestamp
            );

            Map<String, Object> response = Map.of(
                    "command", "addInterest",
                    "output", output,
                    "timestamp", currentTimestamp
            );

            return List.of(response);
        }

        double interestAmount = targetAccount.getBalance()
                * targetAccount.getInterestRate();
        targetAccount.addFunds(interestAmount);

        String description = "Interest rate income";
        Transaction transaction = new Transaction(
                command.getTimestamp(),
                description,
                null,
                null,
                interestAmount,
                targetAccount.getCurrency(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                true,
                "addInterestRate");

        // Folosește targetUser pentru a adăuga tranzacția.
        targetUser.addTransaction(transaction);
        targetAccount.addTransaction(transaction);

        return List.of();
    }

}
