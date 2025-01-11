package org.poo.bank.commands.report_commands;

import org.poo.bank.account.Account;
import org.poo.fileio.CommandInput;
import org.poo.bank.user.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractReportCommand {

    protected abstract String getCommandName();

    protected abstract Map<String, Object> generateReport(Account account,
                                                          CommandInput command);

    /**
     * Proceseaza o comanda pentru a genera un raport pe baza unui cont specificat.
     *
     * @param command Comanda care contine informatiile necesare pentru procesare.
     * @param users Lista de utilizatori care contine utilizatorii.
     * @return Un obiect Map ce contine rezultatul procesarii comenzii.
     * Daca contul nu este gasit, va returna un mesaj de eroare.
     */
    public final Map<String, Object> process(final CommandInput command,
                                             final List<User> users) {
        String accountIBAN = command.getAccount();
        int currentTimestamp = command.getTimestamp();

        Account account = null;
        for (User user : users) {
            account = user.getAccountByIBAN(accountIBAN);
            if (account != null) {
                break;
            }
        }

        if (account == null) {
            Map<String, Object> outputMap = new HashMap<>();
            Map<String, Object> errorDetails = new HashMap<>();
            errorDetails.put("description", "Account not found");
            errorDetails.put("timestamp", currentTimestamp);

            outputMap.put("command", getCommandName());
            outputMap.put("output", errorDetails);
            outputMap.put("timestamp", currentTimestamp);

            return outputMap;

        }

        Map<String, Object> outputMap = new HashMap<>();
        Map<String, Object> report = generateReport(account, command);

        outputMap.put("command", getCommandName());
        outputMap.put("output", report);
        outputMap.put("timestamp", currentTimestamp);

        return outputMap;
    }
}
