package org.poo.bank.commands.withdrawal;

import org.poo.bank.account.Account;
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

        // Dacă nu am găsit cardul, returnăm un mesaj specific pentru "Card not found"
        if (account == null || card == null) {
            throw new IllegalArgumentException("Card not found");
        }

        // Verificăm dacă cardul este înghețat
        if (card.getStatus().equals("frozen")) {
            return; // Dacă cardul este înghețat, nu facem nimic
        }

        // Verificăm dacă utilizatorul are suficiente fonduri în cont
        double balance = account.getBalance();
        double minimumBalance = account.getMinimumBalance();

        // Verificăm dacă moneda este RON
        if (!account.getCurrency().equals("RON")) {
            return; // Dacă moneda nu este RON, nu facem nimic
        }

        // Verificăm dacă fondurile sunt suficiente pentru retragere
        if (balance - amount < minimumBalance) { // Comparație cu marja de toleranță
            Transaction transaction1 = new Transaction(
                    timestamp,
                    "Insufficient funds", // Descrierea erorii
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
                    "cashWithdrawalError"
            );
            user.addTransaction(transaction1);
            account.addTransaction(transaction1); // Adăugăm și tranzacția în contul asociat
            return; // Nu sunt suficiente fonduri
        }

        // Calculăm comisionul
        double fee = 0.0;

        // Verificăm planul utilizatorului și aplicăm comisionul
        String userPlan = user.getPlan();

        if (userPlan == null) {
            userPlan = "standard"; // Planul este null, presupunem un plan standard
        }

        switch (userPlan) {
            case "student":
                fee = 0.0; // Nu se percepe comision
                break;
            case "silver":
                if (amount > 500) {
                    fee = 0.001 * amount; // 0.1% pentru sume mai mari de 500 RON
                }
                break;
            case "gold":
                fee = 0.0; // Nu se percepe comision
                break;
            case "standard":
            default:
                fee = 0.002 * amount; // 0.2% comision pentru planul standard
                break;
        }

        // Afișăm comisionul calculat
        System.out.println("Comisionul pentru utilizatorul cu planul " + userPlan + " este: " + fee + " RON");

        // Calculăm suma totală de retras, inclusiv comisionul
        double totalAmountToWithdraw = amount + fee;

        // Verificăm dacă există fonduri suficiente pentru a acoperi suma totală
        if (balance < totalAmountToWithdraw) {
            Transaction transaction2 = new Transaction(
                    timestamp,
                    "Insufficient funds for commission", // Descrierea erorii
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
                    "cashWithdrawalError"
            );
            user.addTransaction(transaction2);
            account.addTransaction(transaction2); // Adăugăm și tranzacția în contul asociat
            return; // Nu sunt suficiente fonduri pentru retragerea sumei și comisionul
        }

        // Retragem suma din contul utilizatorului
        account.withdraw(totalAmountToWithdraw);
    }
}
