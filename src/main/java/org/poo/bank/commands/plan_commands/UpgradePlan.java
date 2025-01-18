package org.poo.bank.commands.plan_commands;

import org.poo.bank.account.Account;
import org.poo.bank.exchange_rates.ExchangeRateManager;
import org.poo.bank.transaction.Transaction;
import org.poo.bank.user.User;
import org.poo.fileio.CommandInput;

import java.util.List;

public class UpgradePlan {
    private final List<User> users;
    private static final double SILVER_UPGRADE_FEE = 100.0;
    private static final double GOLD_UPGRADE_FEE_SILVER_TO_GOLD = 250.0;
    private static final double GOLD_UPGRADE_FEE_STANDARD_TO_GOLD = 350.0;

    public UpgradePlan(final List<User> users) {
        this.users = users;
    }

    /**
     * Proceseaza comanda de upgrade a planului utilizatorului.
     *
     * @param command Comanda ce contine informatiile necesare pentru upgrade-ul planului
     * @throws IllegalArgumentException daca utilizatorul nu este gasit,
     * nu are suficiente fonduri sau daca planul nu poate fi schimbat.
     */
    public void execute(final CommandInput command) {
        final User user = findUserByAccount(command.getAccount());
        if (user == null) {
            throw new IllegalArgumentException("Account not found");
        }

        final Account account = user.getAccountByIBAN(command.getAccount());
        if (account == null) {
            throw new IllegalArgumentException("Account not found");
        }

        final String currentPlan = user.getPlan() == null ? "standard" : user.getPlan();
        final String newPlan = command.getNewPlanType();

        if (currentPlan.equals(newPlan)) {
            throw new IllegalArgumentException("The user already has the " + newPlan + " plan.");
        }

        if (isDowngrade(currentPlan, newPlan)) {
            throw new IllegalArgumentException("You cannot downgrade your plan.");
        }

        final double feeRON = calculateUpgradeFee(currentPlan, newPlan);
        final double feeInAccountCurrency = ExchangeRateManager.getInstance().
                convertCurrency("RON", account.getCurrency(), feeRON);

        if (account.getBalance() < feeInAccountCurrency) {
            throw new IllegalArgumentException("Insufficient funds");
        }

        account.withdrawFunds(feeInAccountCurrency);
        user.setPlan(newPlan);
        final Transaction upgradeTransaction = new Transaction(
                command.getTimestamp(),
                "Upgrade plan",
                account.getIban(),
                null,
                feeInAccountCurrency,
                account.getCurrency(),
                null,
                null,
                null,
                null,
                null,
                null,
                user.getPlan(),
                true,
                null,
                null,
                "upgradePlan"
        );

        user.addTransaction(upgradeTransaction);
        account.addTransaction(upgradeTransaction);

        for (final Account userAccount : user.getAccounts()) {
            userAccount.addTransaction(upgradeTransaction);
        }
    }

    /**
     * Găsește utilizatorul care deține contul specificat prin IBAN.
     *
     * @param accountIban IBAN-ul contului.
     * @return Utilizatorul care deține contul sau null dacă nu există.
     */
    private User findUserByAccount(final String accountIban) {
        for (final User user : users) {
            if (user.getAccountByIBAN(accountIban) != null) {
                return user;
            }
        }
        return null;
    }

    /**
     * Verifica dacă trecerea la noul plan este un downgrade.
     *
     * @param currentPlan Planul actual.
     * @param newPlan Planul nou.
     * @return True daca este un downgrade, false altfel.
     */
    private boolean isDowngrade(final String currentPlan, final String newPlan) {
        final List<String> plans = List.of("standard", "student", "silver", "gold");
        return plans.indexOf(newPlan) < plans.indexOf(currentPlan);
    }

    /**
     * Calculeaza taxa de upgrade pentru trecerea intre planuri.
     *
     * @param currentPlan Planul actual.
     * @param newPlan Planul nou.
     */
    private double calculateUpgradeFee(final String currentPlan, final String newPlan) {
        if ((currentPlan.equals("standard") || currentPlan.equals("student"))
                && newPlan.equals("silver")) {
            return SILVER_UPGRADE_FEE;
        } else if (currentPlan.equals("silver") && newPlan.equals("gold")) {
            return GOLD_UPGRADE_FEE_SILVER_TO_GOLD;
        } else if ((currentPlan.equals("standard") || currentPlan.equals("student"))
                && newPlan.equals("gold")) {
            return GOLD_UPGRADE_FEE_STANDARD_TO_GOLD;
        }
        return 0.0;
    }
}
