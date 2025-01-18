package org.poo.bank.commands.account_commands;

import org.poo.bank.account.Account;
import org.poo.bank.transaction.Transaction;
import org.poo.fileio.CommandInput;
import org.poo.bank.user.User;
import org.poo.bank.exchange_rates.ExchangeRate;

import java.util.List;
import java.util.Map;

public final class WithdrawSavings {
    private final List<User> users;
    private static final int MINIMUM_AGE = 21;

    /**
     * Constructor pentru clasa WithdrawSavings.
     *
     * @param users Lista de utilizatori care dețin conturi.
     */
    public WithdrawSavings(final List<User> users) {
        this.users = users;
    }

    /**
     * Proceseaza o cerere de retragere dintr-un cont de economii.
     *
     * @param command Obiectul de intrare care contine detaliile comenzii.
     * @return O lista cu o singura intrare Map, care reprezintă rezultatul tranzactiei.
     */
    public List<Map<String, Object>> withdrawSavings(final CommandInput command) {
        String savingsIBAN = command.getAccount();
        double amount = command.getAmount();
        String currency = command.getCurrency();
        int timestamp = command.getTimestamp();

        Account savingsAccount = null;
        User accountHolder = null;

        for (User user : users) {
            savingsAccount = user.getAccountByIBAN(savingsIBAN);
            if (savingsAccount != null) {
                accountHolder = user;
                break;
            }
        }

        String description;
        String transactionType;

        if (savingsAccount == null) {
            description = "Account not found";
            transactionType = "withdrawSavingsError";
        } else if (!"savings".equals(savingsAccount.getType())) {
            description = "Account is not of type savings.";
            transactionType = "withdrawSavingsError";
        } else if (accountHolder.getAge() < MINIMUM_AGE) {
            description = "You don't have the minimum age required.";
            transactionType = "withdrawSavingsError";
        } else if (savingsAccount.getBalance() < amount) {
            description = "Insufficient funds";
            transactionType = "withdrawSavingsError";
        } else {
            Account classicAccount = accountHolder.getFirstClassicAccountByCurrency(currency);
            if (classicAccount == null) {
                description = "You do not have a classic account.";
                transactionType = "withdrawSavingsError";
            } else {
                double equivalentAmount = ExchangeRate.convert(
                        savingsAccount.getCurrency(),
                        currency,
                        amount
                );
                double fee = savingsAccount.calculateFee(equivalentAmount);

                if (savingsAccount.getBalance() >= amount + fee) {
                    savingsAccount.withdraw(amount + fee);
                    classicAccount.addFunds(equivalentAmount);

                    description = "Savings withdrawal";
                    transactionType = "withdrawSavings";

                    Transaction successTransaction = new Transaction(
                            timestamp,
                            description,
                            savingsIBAN,
                            null,
                            amount,
                            savingsAccount.getCurrency(),
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            true,
                            null,
                            null,
                            transactionType
                    );
                    accountHolder.addTransaction(successTransaction);
                    return List.of(successTransaction.toMap());
                } else {
                    description = "Insufficient funds";
                    transactionType = "withdrawSavingsError";
                }
            }
        }

        Transaction errorTransaction = new Transaction(
                timestamp,
                description,
                null,
                null,
                0,
                savingsAccount != null ? savingsAccount.getCurrency() : currency,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                true,
                null,
                null,
                transactionType
        );

        if (accountHolder != null) {
            accountHolder.addTransaction(errorTransaction);
        }

        return List.of(errorTransaction.toMap());
    }
}
