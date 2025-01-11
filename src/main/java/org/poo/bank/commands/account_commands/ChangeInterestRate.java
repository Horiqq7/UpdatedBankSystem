package org.poo.bank.commands.account_commands;

import org.poo.bank.account.Account;
import org.poo.bank.transaction.Transaction;
import org.poo.fileio.CommandInput;
import org.poo.bank.user.User;

import java.util.List;
import java.util.Map;


public final class ChangeInterestRate {

    /**
     * Executa comanda de schimbare a ratei dobanzii pe un cont de economii.
     *
     * @param command Comanda care contine informatii despre contul vizat si noua rata a dobanzii.
     * @param users Lista de utilizatori care sunt verificati pentru a gasi
     * contul cu IBAN-ul corespunzator.
     * @return O lista de harti care contin detaliile rezultatului comenzii.
     *         In caz de eroare, va fi returnat un map cu mesajul corespunzator.
     */
    public List<Map<String, Object>> execute(final CommandInput command, final List<User> users) {
        String targetIBAN = command.getAccount();
        double newInterestRate = command.getInterestRate();
        int currentTimestamp = command.getTimestamp();

        Account targetAccount = null;
        User targetUser = null;
        for (User user : users) {
            targetAccount = user.getAccountByIBAN(targetIBAN);
            if (targetAccount != null) {
                targetUser = user;
                break;
            }
        }

        if (targetAccount == null) {
            Map<String, Object> output = Map.of(
                    "description", "Account not found",
                    "timestamp", currentTimestamp
            );

            Map<String, Object> response = Map.of(
                    "command", "changeInterestRate",
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
                    "command", "changeInterestRate",
                    "output", output,
                    "timestamp", currentTimestamp
            );

            return List.of(response);

        }

        targetAccount.setInterestRate(newInterestRate);

        Transaction newInterestRateTransaction = new Transaction(
                currentTimestamp,
                "Interest rate of the account changed to " + newInterestRate,
                null,
                null,
                0,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "changeInterestRate"
        );

        targetUser.addTransaction(newInterestRateTransaction);

        return List.of();
    }
}
