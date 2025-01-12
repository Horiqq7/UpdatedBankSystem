package org.poo.bank.commands.plan_commands;

import org.poo.bank.account.Account;
import org.poo.bank.exchange_rates.ExchangeRateManager;
import org.poo.bank.transaction.Transaction;
import org.poo.bank.user.User;
import org.poo.fileio.CommandInput;

import java.util.List;

public class UpgradePlan {
    private final List<User> users;

    public UpgradePlan(List<User> users) {
        this.users = users;
    }

    public void execute(CommandInput command) {
        // Găsim utilizatorul pe baza IBAN-ului
        User user = findUserByAccount(command.getAccount());
        if (user == null) {
            throw new IllegalArgumentException("Account not found");
        }

        Account account = user.getAccountByIBAN(command.getAccount());
        if (account == null) {
            throw new IllegalArgumentException("Account not found");
        }

        // Obținem planul actual (folosim "standard" dacă este null)
        String currentPlan = user.getPlan() == null ? "standard" : user.getPlan();
        String newPlan = command.getNewPlanType();

//        // Debugging: Afișăm planul actual și planul nou
//        System.out.println(user.getEmail());
//        System.out.println("Current plan: " + currentPlan);
//        System.out.println("New plan: " + newPlan);

        // Validăm dacă planul curent este același cu noul
        if (currentPlan.equalsIgnoreCase(newPlan)) {
            throw new IllegalArgumentException("The user already has the " + newPlan + " plan.");
        }

        // Verificăm dacă upgrade-ul este un downgrade (acesta trebuie să prevină downgrade-urile doar)
        if (isDowngrade(currentPlan, newPlan)) {
            throw new IllegalArgumentException("You cannot downgrade your plan.");
        }

        // Calculăm taxa de upgrade (în RON) și o convertim în moneda contului utilizatorului
        double feeRON = calculateUpgradeFee(currentPlan, newPlan);
        double feeInAccountCurrency = ExchangeRateManager.getInstance().convertCurrency("RON", account.getCurrency(), feeRON);

        // Verificăm dacă utilizatorul are fonduri suficiente
        if (account.getBalance() < feeInAccountCurrency) {
            throw new IllegalArgumentException("Insufficient funds");
        }

        // Deductăm taxa din contul utilizatorului
        account.withdrawFunds(feeInAccountCurrency);

//        // Debugging: Afișăm mesajul de succes după retragerea taxei
//        System.out.println("Fee deducted: " + feeInAccountCurrency + " " + account.getCurrency());

        // Actualizăm planul utilizatorului
        user.setPlan(newPlan);

//        // Debugging: Verificăm dacă planul a fost actualizat corect
//        System.out.println("Updated plan: " + user.getPlan());
//        System.out.println(command.getTimestamp());

        // Creăm tranzacția pentru upgrade
        Transaction upgradeTransaction = new Transaction(
                command.getTimestamp(),
                "Upgrade plan",
                account.getIban(),
                null,
                feeInAccountCurrency,
                account.getCurrency(),  // Folosim moneda contului utilizatorului
                null,
                null,
                null,
                null,
                null,
                null,
                user.getPlan(),
                true,
                "upgradePlan"
        );

        // Adăugăm tranzacția la utilizator și la cont
        user.addTransaction(upgradeTransaction);
        account.addTransaction(upgradeTransaction);

        // Adăugăm tranzacția de upgrade la toate conturile utilizatorului
        for (Account userAccount : user.getAccounts()) {
            userAccount.addTransaction(upgradeTransaction);
        }
    }

    /**
     * Găsește utilizatorul care deține contul specificat prin IBAN.
     * @param accountIban IBAN-ul contului.
     * @return Utilizatorul care deține contul sau null dacă nu există.
     */
    private User findUserByAccount(String accountIban) {
        for (User user : users) {
            if (user.getAccountByIBAN(accountIban) != null) {
                return user;
            }
        }
        return null;
    }

    /**
     * Verifică dacă trecerea la noul plan este un downgrade.
     * @param currentPlan Planul actual.
     * @param newPlan Planul nou.
     * @return True dacă este un downgrade, false altfel.
     */
    private boolean isDowngrade(String currentPlan, String newPlan) {
        List<String> plans = List.of("standard", "student", "silver", "gold");
        return plans.indexOf(newPlan) < plans.indexOf(currentPlan);  // Asigurăm upgrade valid
    }

    /**
     * Calculează taxa de upgrade pentru trecerea între planuri, folosind RON ca monedă de bază.
     * @param currentPlan Planul actual.
     * @param newPlan Planul nou.
     * @return Taxa de upgrade în RON.
     */
    private double calculateUpgradeFee(String currentPlan, String newPlan) {
        if ((currentPlan.equals("standard") || currentPlan.equals("student")) && newPlan.equals("silver")) {
            return 100.0;
        } else if (currentPlan.equals("silver") && newPlan.equals("gold")) {
            return 250.0;
        } else if ((currentPlan.equals("standard") || currentPlan.equals("student")) && newPlan.equals("gold")) {
            return 350.0;
        }
        return 0.0;
    }
}
