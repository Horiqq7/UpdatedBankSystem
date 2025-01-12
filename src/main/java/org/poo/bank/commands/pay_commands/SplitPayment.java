package org.poo.bank.commands.pay_commands;

import org.poo.bank.account.Account;
import org.poo.bank.exchange_rates.ExchangeRateManager;
import org.poo.fileio.CommandInput;
import org.poo.bank.user.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SplitPayment {
    private final List<User> users;

    public SplitPayment(final List<User> users) {
        this.users = users;
    }

    /**
     * Proceseaza o comanda de tip splitPayment distribuind suma specificata intre conturile date.
     *
     * @param command Comanda de plata care contine informatiile necesare pentru procesare.
     * @return O lista de erori sau tranzactii efectuate.
     */
    public List<Map<String, Object>> splitPayment(final CommandInput command) {
        List<Map<String, Object>> output = new ArrayList<>();
        List<String> accountIBANs = command.getAccounts();
        double totalAmount = command.getAmount();
        String currency = command.getCurrency();
        int timestamp = command.getTimestamp();
        List<Double> amountForUsers = command.getAmountForUsers();

        // Verifică dacă totalAmount este valid
        if (totalAmount <= 0) {
            Map<String, Object> error = new HashMap<>();
            error.put("description", "Invalid amount");
            output.add(error);
            return output;
        }

        // Verifică dacă amountForUsers nu este null și are aceeași dimensiune ca lista conturilor
        if (amountForUsers == null || amountForUsers.size() != accountIBANs.size()) {
            Map<String, Object> error = new HashMap<>();
            error.put("description", "Mismatch between accounts and amounts");
            output.add(error);
            return output;
        }

        // Verifică existența conturilor și disponibilitatea fondurilor
        Account problematicAccount = null;
        ExchangeRateManager exchangeRateManager = ExchangeRateManager.getInstance();

        for (int i = 0; i < accountIBANs.size(); i++) {
            String accountIBAN = accountIBANs.get(i);
            double amountForUser = amountForUsers.get(i);

            Account account = null;
            for (User u : users) {
                account = u.getAccountByIBAN(accountIBAN);
                if (account != null) {
                    break;
                }
            }

            if (account == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("description", "Account not found: " + accountIBAN);
                error.put("involvedAccounts", accountIBANs);
                error.put("timestamp", timestamp);
                output.add(error);
                return output;
            }

            // Verifică dacă contul are suficiente fonduri
            double convertedAmount = amountForUser;
            if (!currency.equalsIgnoreCase(account.getCurrency())) {
                try {
                    convertedAmount = exchangeRateManager.convertCurrency(currency, account.getCurrency(), amountForUser);
                } catch (IllegalArgumentException e) {
                    Map<String, Object> error = new HashMap<>();
                    error.put("description", "Conversion rate not available for " + currency + " to " + account.getCurrency());
                    error.put("involvedAccounts", accountIBANs);
                    error.put("timestamp", timestamp);
                    output.add(error);
                    return output;
                }
            }

            // Verifică dacă există fonduri suficiente în contul utilizatorului
            if (account.getBalance() < convertedAmount) {
                problematicAccount = account;
                break;  // Dacă un cont nu are fonduri suficiente, ieșim din buclă
            }

            // Blochează banii din contul utilizatorului pentru a preveni tranzacțiile ulterioare
            account.blockFunds(convertedAmount);
        }

        // Dacă un cont are fonduri insuficiente
        if (problematicAccount != null) {
            Map<String, Object> error = new HashMap<>();
            error.put("amount", totalAmount);
            error.put("currency", currency);
            error.put("description", "Split payment of " + String.format("%.2f", totalAmount) + " " + currency);
            error.put("error", "Account " + problematicAccount.getIban() + " has insufficient funds for a split payment.");
            error.put("involvedAccounts", accountIBANs);
            error.put("timestamp", timestamp);
            output.add(error);
            return output;
        }

        // Dacă nu au apărut erori, trimitem un mesaj de succes
        Map<String, Object> success = new HashMap<>();
        success.put("description", "Split payment of " + String.format("%.2f", totalAmount) + " " + currency);
        success.put("accounts", accountIBANs);
        success.put("timestamp", timestamp);
        output.add(success);

        return output;
    }
}
