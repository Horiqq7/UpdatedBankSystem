package org.poo.bank.commands.withdrawal;

import org.poo.bank.account.Account;
import org.poo.bank.exchange_rates.ExchangeRateManager;
import org.poo.bank.user.User;
import org.poo.bank.cards.Card;
import org.poo.bank.transaction.Transaction;

import java.util.List;

public class CashWithdrawal {

    public static void executeCashWithdrawal(String cardNumber, double amount, String email, String location, int timestamp, List<User> users) {
        // Căutăm utilizatorul după email
        User user = User.findByEmail(users, email);
        if (user == null) {
            return; // Dacă nu găsim utilizatorul, nu facem nimic
        }

        // Căutăm cardul asociat
        Account account = null;
        Card card = null;
        for (Account acc : user.getAccounts()) {
            card = acc.getCardByNumber(cardNumber);
            if (card != null) {
                account = acc;
                break;
            }
        }

        if (account == null || card == null) {
            throw new IllegalArgumentException("Card not found");
        }

        // Verificăm dacă cardul este înghețat
        if (card.getStatus().equals("frozen")) {
            return; // Dacă cardul este înghețat, nu facem nimic
        }


        String accountCurrency = account.getCurrency();

        double amountToWithdraw = amount; // amount este în RON în input

        if (!accountCurrency.equals("RON")) {
            ExchangeRateManager exchangeRateManager = ExchangeRateManager.getInstance();
            double exchangeRate = exchangeRateManager.getExchangeRate("RON", accountCurrency);
            if (exchangeRate == 0) {
//                System.out.println("Exchange rate not available");
                return; // Dacă nu există o rată de schimb, nu putem continua
            }
            // Transformăm suma din RON în moneda contului utilizatorului
            amountToWithdraw = exchangeRateManager.convertCurrency("RON", accountCurrency, amount);
//            System.out.println("Amount to withdraw in " + accountCurrency + ": " + amountToWithdraw);
        }

        // Verificăm dacă utilizatorul are suficiente fonduri în cont
        double balance = account.getBalance();
        double minimumBalance = account.getMinimumBalance();

        // Verificăm dacă fondurile sunt suficiente pentru retragere
        if (balance - amountToWithdraw < minimumBalance) {
            Transaction transaction1 = new Transaction(
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
                    "cashWithdrawalError"
            );
            user.addTransaction(transaction1);
            account.addTransaction(transaction1);
            return;
        }

        // Calculăm comisionul
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
                if (amount > 500) {
                    fee = 0.001 * amountToWithdraw;
                }
                break;
            case "gold":
                fee = 0.0;
                break;
            case "standard":
            default:
                fee = 0.002 * amountToWithdraw;
                break;
        }
//
//        System.out.println("Comisionul pentru utilizatorul cu planul " + userPlan + " este: " + fee + " RON");

        double totalAmountToWithdraw = amountToWithdraw + fee;

        if (balance < totalAmountToWithdraw) {
            Transaction transaction2 = new Transaction(
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
                    "cashWithdrawalError"
            );
            user.addTransaction(transaction2);
            account.addTransaction(transaction2);
            return;
        }

        // Retragem suma din contul utilizatorului
        account.withdraw(totalAmountToWithdraw);

        // Înregistrăm tranzacția de retragere
        Transaction cashWithdrawalTransaction = new Transaction(
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
                "cashWithdrawal"
        );
        user.addTransaction(cashWithdrawalTransaction);
        account.addTransaction(cashWithdrawalTransaction);
//
//        System.out.println("Cash withdrawal înregistrat cu succes pentru utilizatorul " + user.getEmail());
    }

}
