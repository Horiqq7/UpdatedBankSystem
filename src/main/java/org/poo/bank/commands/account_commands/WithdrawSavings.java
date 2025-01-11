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

    public WithdrawSavings(final List<User> users) {
        this.users = users;
    }

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
        String error = null;

        if (savingsAccount == null) {
            description = "Account not found";
            transactionType = "withdrawSavingsError";
            error = "No savings account with IBAN: " + savingsIBAN;
        } else if (!"savings".equals(savingsAccount.getType())) {
            description = "Account is not of type savings.";
            transactionType = "withdrawSavingsError";
            error = "Account type mismatch.";
        } else if (accountHolder.getAge() < 21) {
            description = "You don't have the minimum age required.";
            transactionType = "withdrawSavingsError";
            error = "Age restriction: Minimum 21 years.";
        } else if (savingsAccount.getBalance() < amount) {
            description = "Insufficient funds";
            transactionType = "withdrawSavingsError";
            error = "Savings account balance is insufficient.";
        } else {
            Account classicAccount = accountHolder.getFirstClassicAccountByCurrency(currency);
            if (classicAccount == null) {
                description = "You do not have a classic account.";
                transactionType = "withdrawSavingsError";
                error = "No classic account found for currency: " + currency;
            } else {
                double equivalentAmount = ExchangeRate.convert(savingsAccount.getCurrency(), currency, amount);
                double fee = savingsAccount.calculateFee(equivalentAmount);

                if (savingsAccount.getBalance() >= amount + fee) {
                    savingsAccount.withdraw(amount + fee);
                    classicAccount.addFunds(equivalentAmount);

                    description = "Savings withdrawal";
                    transactionType = "withdrawSavings";

                    // Creăm și adăugăm tranzacția de succes la istoricul utilizatorului.
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
                            transactionType
                    );
                    accountHolder.addTransaction(successTransaction); // Adăugăm în istoricul utilizatorului
                    return List.of(successTransaction.toMap());
                } else {
                    description = "Insufficient funds";
                    transactionType = "withdrawSavingsError";
                    error = "Savings account balance is insufficient after applying fees.";
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
                transactionType
        );

        if (accountHolder != null) {
            accountHolder.addTransaction(errorTransaction); // Adăugăm în istoricul utilizatorului
        }

        return List.of(errorTransaction.toMap());
    }

}
