package org.poo.bank.commands.account_commands;

import org.poo.bank.account.Account;
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

    /**
     * Adauga dobanda unui cont de economii specificat, daca este gasit.
     *
     * @param command Comanda care contine IBAN-ul contului si timestamp-ul.
     * @return O lista de mapuri care contine informatii despre rezultat,
     * inclusiv daca contul nu a fost gasit sau daca contul nu este un cont de economii.
     */
    public List<Map<String, Object>> addInterest(final CommandInput command) {
        String targetIBAN = command.getAccount();
        int currentTimestamp = command.getTimestamp();

        Account targetAccount = null;
        for (User user : users) {
            targetAccount = user.getAccountByIBAN(targetIBAN);
            if (targetAccount != null) {
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
                * targetAccount.getInterestRate() / PERCENTAGE_DIVISOR;
        targetAccount.addFunds(interestAmount);

        return List.of();
    }
}
