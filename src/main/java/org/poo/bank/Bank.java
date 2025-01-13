package org.poo.bank;

import lombok.Getter;
import lombok.Setter;
import org.poo.bank.account.Account;
import org.poo.bank.commands.account_commands.*;
import org.poo.bank.commands.account_commands.card_commands.CheckCardStatus;
import org.poo.bank.commands.account_commands.card_commands.CreateCard;
import org.poo.bank.commands.account_commands.card_commands.CreateOneTimeCard;
import org.poo.bank.commands.account_commands.card_commands.DeleteCard;
import org.poo.bank.commands.pay_commands.AcceptSplitPayment;
import org.poo.bank.commands.pay_commands.PayOnline;
import org.poo.bank.commands.pay_commands.SendMoney;
import org.poo.bank.commands.pay_commands.SplitPayment;
import org.poo.bank.commands.plan_commands.UpgradePlan;
import org.poo.bank.commands.print_commands.PrintTransactions;
import org.poo.bank.commands.print_commands.PrintUsers;
import org.poo.bank.commands.report_commands.AbstractReportCommand;
import org.poo.bank.commands.report_commands.SpendingsReport;
import org.poo.bank.commands.report_commands.Report;
import org.poo.bank.commands.withdrawal.CashWithdrawal;
import org.poo.fileio.CommandInput;
import org.poo.fileio.CommerciantInput;
import org.poo.fileio.ObjectInput;
import org.poo.bank.user.User;

import java.util.*;

/**
 * Clasa Bank gestioneaza conturile si comenzile legate de utilizatori.
 * Permite adaugarea, stergerea si gestionarea conturilor si cardurilor,
 * precum si executarea diferitelor comenzi bancare.
 */
@Getter
public class Bank {
    private final List<User> users = new ArrayList<>();
    private final List<CommerciantInput> commerciants = new ArrayList<>(); // Lista comercianților
    @Setter
    private Map<String, Account> accounts = new HashMap<>();

    // Lista pentru stocarea comenzilor procesate
    private final List<CommandInput> processedCommands = new ArrayList<>();

    /**
     * Constructor pentru crearea unei banci pe baza datelor de intrare.
     *
     * @param inputData Datele utilizatorilor si comercianților din fisierul de intrare
     */
    public Bank(final ObjectInput inputData) {
        if (inputData.getUsers() != null) {
            for (var userInput : inputData.getUsers()) {
                User user = new User(userInput);
                users.add(user);
            }
        }
        if (inputData.getCommerciants() != null) {
            commerciants.addAll(Arrays.asList(inputData.getCommerciants()));
        }
    }

    /**
     * Proceseaza o comanda si returneaza rezultatul executarii acesteia.
     *
     * @param command Comanda care trebuie procesata
     * @return Lista de obiecte care reprezinta rezultatul comenzii procesate
     * @throws IllegalArgumentException Daca comanda nu este cunoscuta
     */
    public List<Map<String, Object>> processCommand(final CommandInput command) {
        switch (command.getCommand()) {
            case "printUsers":
                PrintUsers printUsers = new PrintUsers(users);
                return printUsers.execute();
            case "addAccount":
                AddAccount addAccount = new AddAccount(users);
                addAccount.addAccount(command);
                return Collections.emptyList();
            case "createCard":
                CreateCard createCard = new CreateCard(users);
                createCard.createCard(command);
                return Collections.emptyList();
            case "createOneTimeCard":
                CreateOneTimeCard createOneTimeCard = new CreateOneTimeCard(users);
                createOneTimeCard.createOneTimeCard(command);
                return Collections.emptyList();
            case "addFunds":
                AddFunds addFunds = new AddFunds(users);
                addFunds.addFunds(command);
                return Collections.emptyList();
            case "deleteAccount":
                DeleteAccount deleteAccount = new DeleteAccount(getUsers());
                return Collections.singletonList(deleteAccount.deleteAccount(command));
            case "deleteCard":
                DeleteCard deleteCard = new DeleteCard(users);
                deleteCard.deleteCard(command);
                return Collections.emptyList();
            case "setMinimumBalance":
                SetMinimumBalance setMinimumBalance = new SetMinimumBalance(users);
                setMinimumBalance.setMinimumBalance(command);
                return Collections.emptyList();
            case "payOnline":
                PayOnline payOnlineProcessor = new PayOnline(users, commerciants); // Transmitem comercianții
                List<Map<String, Object>> response = payOnlineProcessor.payOnline(command);
                return response.isEmpty() ? Collections.emptyList() : response;
            case "sendMoney":
                SendMoney sendMoneyProcessor = new SendMoney(users);
                return sendMoneyProcessor.sendMoney(command);
            case "setAlias":
                SetAlias setAliasProcessor = new SetAlias(users);
                setAliasProcessor.setAlias(command);
                return Collections.emptyList();
            case "printTransactions":
                PrintTransactions printTransactions = new PrintTransactions(users);
                return printTransactions.printTransactions(command);
            case "checkCardStatus":
                CheckCardStatus checkCardStatus = new CheckCardStatus();
                Map<String, Object> checkCardStatusResponse = checkCardStatus.execute(command,
                        getUsers());
                return checkCardStatusResponse.isEmpty() ? Collections.emptyList()
                        : List.of(checkCardStatusResponse);
            case "changeInterestRate":
                ChangeInterestRate changeInterestRate = new ChangeInterestRate();
                List<Map<String, Object>> changeInterestRateResponse
                        = changeInterestRate.execute(command, getUsers());
                return changeInterestRateResponse.isEmpty() ? Collections.emptyList()
                        : changeInterestRateResponse;
            case "splitPayment":
                SplitPayment splitPaymentProcessor = new SplitPayment(users);
                List<Map<String, Object>> splitPaymentResponse = splitPaymentProcessor.splitPayment(command);
                // Salvăm comanda procesată în lista de comenzi
                processedCommands.add(command);
                return splitPaymentResponse.isEmpty() ? Collections.emptyList() : splitPaymentResponse;
            case "spendingsReport":
                AbstractReportCommand spendingsReport = new SpendingsReport();
                return List.of(spendingsReport.process(command, getUsers()));
            case "report":
                AbstractReportCommand report = new Report();
                return List.of(report.process(command, getUsers()));
            case "addInterest":
                AddInterest addInterestProcessor = new AddInterest(users);
                return addInterestProcessor.addInterest(command);
            case "withdrawSavings":
                WithdrawSavings withdrawSavings = new WithdrawSavings(users);
                return withdrawSavings.withdrawSavings(command);
            case "upgradePlan":
                UpgradePlan upgradePlanProcessor = new UpgradePlan(users);
                upgradePlanProcessor.execute(command);
                return Collections.emptyList();
            case "cashWithdrawal":
                // Folosim CashWithdrawal pentru a executa retragerea de numerar
                CashWithdrawal.executeCashWithdrawal(
                        command.getCardNumber(),
                        command.getAmount(),
                        command.getEmail(),
                        command.getLocation(),
                        command.getTimestamp(),
                        getUsers()
                );
                return Collections.emptyList();
            case "acceptSplitPayment":
                AcceptSplitPayment acceptSplitPaymentProcessor = new AcceptSplitPayment(users);
                Map<String, Object> acceptResult = acceptSplitPaymentProcessor.acceptSplitPayment(command);
                if (acceptResult != null) {
                    return List.of(acceptResult);
                } else {
                    Map<String, Object> error = new HashMap<>();
                    error.put("description", "Failed to process the split payment acceptance");
                    return List.of(error);
                }

            default:
                throw new IllegalArgumentException("Unknown command: "
                        + command.getCommand());
        }
    }

    // Metodă pentru a obține comenzile procesate până acum
    public List<CommandInput> getProcessedCommands() {
        return processedCommands;
    }
}
