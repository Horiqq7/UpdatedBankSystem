package org.poo.bank.commands.withdrawal;

import org.poo.bank.account.Account;
import org.poo.bank.exchange_rates.ExchangeRateManager;
import org.poo.bank.user.User;
import org.poo.bank.cards.Card;
import org.poo.bank.transaction.Transaction;

import java.util.List;

public final class CashWithdrawal {

    private static final double MAX_FEE_SILVER = 0.001;
    private static final double MAX_FEE_STANDARD = 0.002;
    private static final double FEE_THRESHOLD = 500;

    /**
     * Executia retragerii de numerar de pe card.
     *
     * @param cardNumber Numărul cardului utilizatorului.
     * @param amount     Suma de bani pe care utilizatorul dorește să o retrag.
     * @param email      Adresa de email a utilizatorului.
     * @param location   Locația de unde se efectuează retragerea.
     * @param timestamp  Timpul retragerii.
     * @param users      Lista cu utilizatorii disponibili.
     */
    public static void executeCashWithdrawal(final String cardNumber,
                                             final double amount, final String email,
                                             final String location, final int timestamp,
                                             final List<User> users) {
        final User user = User.findByEmail(users, email);
        if (user == null) {
            return;
        }

        Account account = null;
        Card card = null;
        for (final Account acc : user.getAccounts()) {
            card = acc.getCardByNumber(cardNumber);
            if (card != null) {
                account = acc;
                break;
            }
        }

        if (account == null || card == null) {
            throw new IllegalArgumentException("Card not found");
        }

        if (card.getStatus().equals("frozen")) {
            return;
        }

        String accountCurrency = account.getCurrency();
        double amountToWithdraw = amount;

        if (!accountCurrency.equals("RON")) {
            final ExchangeRateManager exchangeRateManager = ExchangeRateManager.getInstance();
            final double exchangeRate = exchangeRateManager.getExchangeRate("RON", accountCurrency);
            if (exchangeRate == 0) {
                return;
            }
            amountToWithdraw = exchangeRateManager.convertCurrency("RON", accountCurrency, amount);
        }

        final double balance = account.getBalance();
        final double minimumBalance = account.getMinimumBalance();

        if (balance - amountToWithdraw < minimumBalance) {
            final Transaction transaction1 = new Transaction(
                    timestamp,
                    "Insufficient funds",
                    null,
                    null,
                    0,
                    account.getCurrency(),
                    "payment",
                    cardNumber,
                    user.getEmail(),
                    null,
                    null,
                    null,
                    null,
                    true,
                    null,
                    null,
                    "cashWithdrawalError"
            );
            user.addTransaction(transaction1);
            account.addTransaction(transaction1);
            return;
        }

        double fee = 0.0;

        String userPlan = user.getPlan();
        if (userPlan == null) {
            userPlan = "standard";
        }

        switch (userPlan) {
            case "student":
                fee = 0.0;
                break;
            case "silver":
                if (amount > FEE_THRESHOLD) {
                    fee = MAX_FEE_SILVER * amountToWithdraw;
                }
                break;
            case "gold":
                fee = 0.0;
                break;
            case "standard":
            default:
                fee = MAX_FEE_STANDARD * amountToWithdraw;
                break;
        }

        double totalAmountToWithdraw = amountToWithdraw + fee;

        if (balance < totalAmountToWithdraw) {
            final Transaction transaction2 = new Transaction(
                    timestamp,
                    "Insufficient funds for commission",
                    null,
                    null,
                    0,
                    account.getCurrency(),
                    "payment",
                    cardNumber,
                    user.getEmail(),
                    null,
                    null,
                    null,
                    null,
                    true,
                    null,
                    null,
                    "cashWithdrawalError"
            );
            user.addTransaction(transaction2);
            account.addTransaction(transaction2);
            return;
        }

        account.withdraw(totalAmountToWithdraw);

        final Transaction cashWithdrawalTransaction = new Transaction(
                timestamp,
                "Cash withdrawal of ",
                null,
                null,
                amount,
                account.getCurrency(),
                "withdrawal",
                cardNumber,
                user.getEmail(),
                location,
                null,
                null,
                null,
                true,
                null,
                null,
                "cashWithdrawal"
        );
        user.addTransaction(cashWithdrawalTransaction);
        account.addTransaction(cashWithdrawalTransaction);
    }
}
