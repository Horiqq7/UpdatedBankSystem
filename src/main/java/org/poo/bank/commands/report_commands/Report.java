package org.poo.bank.commands.report_commands;

import org.poo.bank.account.Account;
import org.poo.bank.transaction.Transaction;
import org.poo.fileio.CommandInput;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Report extends AbstractReportCommand {

    protected String getCommandName() {
        return "report";
    }

    /**
     * Genereaza un raport pentru tranzactiile dintr-un cont, filtrate pe baza unui
     * interval de timp.
     *
     * Metoda filtreaza tranzactiile pe baza timestamp-urilor de inceput si sfarsit si
     * construieste o structura de date care contine datele necesare.
     *
     * @param account Contul pentru care se genereaza raportul.
     * @param command Comanda care contine intervalul de timp pentru filtrarea tranzactiilor.
     * @return Un Map care contine detaliile raportului.
     */
    protected Map<String, Object> generateReport(final Account account,
                                                 final CommandInput command) {
        int startTimestamp = command.getStartTimestamp();
        int endTimestamp = command.getEndTimestamp();

        List<Transaction> transactions = account.getTransactions();
        List<Map<String, Object>> filteredTransactions = new ArrayList<>();
        for (Transaction transaction : transactions) {
            if (transaction.getTimestamp() >= startTimestamp
                    && transaction.getTimestamp() <= endTimestamp) {
                filteredTransactions.add(transaction.toMap());
            }
        }

        Map<String, Object> reportDetails = new HashMap<>();
        reportDetails.put("IBAN", account.getIban());
        reportDetails.put("balance", account.getBalance());
        reportDetails.put("currency", account.getCurrency());
        reportDetails.put("transactions", filteredTransactions);

        return reportDetails;
    }
}
