package org.poo.bank.commands.report_commands;

import org.poo.bank.account.Account;
import org.poo.fileio.CommandInput;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class SpendingsReport extends AbstractReportCommand {

    protected String getCommandName() {
        return "spendingsReport";
    }

    protected Map<String, Object> generateReport(final Account account,
                                                 final CommandInput command) {
        int startTimestamp = command.getStartTimestamp();
        int endTimestamp = command.getEndTimestamp();

        if ("savings".equals(account.getType())) {
            return Map.of(
                    "error", "This kind of report is not supported for a saving account"
            );
        }

        Map<String, Object> report = account.generateSpendingsReport(startTimestamp, endTimestamp);

        List<Map<String, Object>> commerciants
                = (List<Map<String, Object>>) report.get("commerciants");
        if (commerciants != null) {
            commerciants.sort(Comparator.comparing(a -> (String) a.get("commerciant")));
        }

        return report;
    }
}
