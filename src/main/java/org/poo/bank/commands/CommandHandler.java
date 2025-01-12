package org.poo.bank.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.poo.bank.Bank;
import org.poo.bank.commands.account_commands.card_commands.CheckCardStatus;
import org.poo.bank.commands.print_commands.PrintTransactions;
import org.poo.bank.user.User;
import org.poo.fileio.CommandInput;

import java.util.List;
import java.util.Map;

public class CommandHandler {

    private final Bank bank;
    private final ObjectMapper objectMapper;


    public CommandHandler(final Bank bank, final ObjectMapper objectMapper) {
        this.bank = bank;
        this.objectMapper = objectMapper;
    }

    /**
     * Proceseaza comanda primita si adauga rezultatele în ArrayNode de output.
     *
     * @param command Comanda care trebuie procesata.
     * @param output ArrayNode care contine rezultatele procesarii.
     * @return ArrayNode cu rezultatele procesarii comenzii.
     */
    public ArrayNode handleCommand(final CommandInput command, final ArrayNode output) {
        final ObjectNode objectNode = objectMapper.createObjectNode();
        objectNode.put("command", command.getCommand());
        objectNode.put("timestamp", command.getTimestamp());

        switch (command.getCommand()) {
            case "printUsers":
                handlePrintUsers(command, objectNode, output);
                break;
            case "addAccount":
            case "createCard":
            case "addFunds":
            case "createOneTimeCard":
            case "deleteCard":
            case "setMinimumBalance":
            case "setAlias":
            case "splitPayment":
                bank.processCommand(command);
                break;
            case "deleteAccount":
                handleDeleteAccount(command, output);
                break;
            case "payOnline":
                handlePayOnline(command, objectNode, output);
                break;
            case "sendMoney":
                handleSendMoney(command, objectNode, output);
                break;
            case "printTransactions":
                handlePrintTransactions(command, objectNode, output);
                break;
            case "checkCardStatus":
                handleCheckCardStatus(command, output);
                break;
            case "changeInterestRate":
                handleChangeInterestRate(command, output);
                break;
            case "report":
            case "spendingsReport":
                handleReport(command, output);
                break;
            case "addInterest":
                handleAddInterest(command, output);
                break;
            case "withdrawSavings":
                handleWithdrawSavings(command, objectNode, output);
                break;
            case "upgradePlan":
                try {
                    bank.processCommand(command);
                } catch (IllegalArgumentException e) {
                    final var errorNode = objectMapper.createObjectNode();
                    errorNode.put("description", e.getMessage());
                    errorNode.put("timestamp", command.getTimestamp());
                    objectNode.set("output", errorNode);
                    output.add(objectNode);
                }
                break;
            case "cashWithdrawal":
                handleCashWithdrawal(command, output);
                break;
            case "acceptSplitPayment":
                bank.processCommand(command);
                break;


            default:
                objectNode.put("type", "error");
                objectNode.put("message", "Unknown command: " + command.getCommand());
                output.add(objectNode);
        }

        return output;
    }

    private void handleCashWithdrawal(final CommandInput command, final ArrayNode output) {
        final ObjectNode objectNode = objectMapper.createObjectNode();
        objectNode.put("command", command.getCommand());
        objectNode.put("timestamp", command.getTimestamp());

        try {
            // Procesăm retragerea
            bank.processCommand(command);

            // Dacă nu există erori, nu trebuie să adăugăm nimic în output
        } catch (IllegalArgumentException e) {
            // În caz de eroare, verificăm dacă mesajul este "Card not found"
            if (e.getMessage().equals("Card not found")) {
                final var errorNode = objectMapper.createObjectNode();
                errorNode.put("description", "Card not found");
                errorNode.put("timestamp", command.getTimestamp());
                objectNode.set("output", errorNode);

                // Adăugăm rezultatul în output doar în caz de eroare
                output.add(objectNode);
            } else {
                final var errorNode = objectMapper.createObjectNode();
                errorNode.put("description", e.getMessage());
                errorNode.put("timestamp", command.getTimestamp());
                objectNode.set("output", errorNode);
                output.add(objectNode);
            }
        }
    }



    private void handleWithdrawSavings(final CommandInput command, final ObjectNode objectNode, final ArrayNode output) {
        bank.processCommand(command);
        // Nu adăugăm nimic în `output` pentru această comandă, tranzacția va fi gestionată separat
    }


    private void handlePrintUsers(final CommandInput command, final ObjectNode objectNode,
                                  final ArrayNode output) {
        final var usersOutput = objectMapper.createArrayNode();
        for (Map<String, Object> user : bank.processCommand(command)) {
            usersOutput.add(objectMapper.valueToTree(user));
        }
        objectNode.set("output", usersOutput);
        output.add(objectNode);
    }

    private void handleDeleteAccount(final CommandInput command, final ArrayNode output) {
        final Map<String, Object> deleteAccountResponse = bank.processCommand(command).get(0);
        final var responseNode = objectMapper.createObjectNode();
        responseNode.put("command", command.getCommand());
        responseNode.put("timestamp", command.getTimestamp());

        if (deleteAccountResponse.containsKey("error")) {
            final var errorNode = objectMapper.createObjectNode();
            errorNode.put("error", deleteAccountResponse.get("error").toString());
            errorNode.put("timestamp", command.getTimestamp());
            responseNode.set("output", errorNode);
        } else {
            final var successNode = objectMapper.createObjectNode();
            successNode.put("success", "Account deleted");
            successNode.put("timestamp", command.getTimestamp());
            responseNode.set("output", successNode);
        }

        output.add(responseNode);
    }

    private void handlePayOnline(final CommandInput command, final ObjectNode objectNode,
                                 final ArrayNode output) {
        final List<Map<String, Object>> response = bank.processCommand(command);
        if (!response.isEmpty()) {
            response.stream().map(line -> objectMapper.createObjectNode()).forEach(responseNode -> {
                responseNode.put("description", "Card not found");
                responseNode.put("timestamp", command.getTimestamp());
                objectNode.set("output", responseNode);
                objectNode.put("timestamp", command.getTimestamp());
                output.add(objectNode);
            });
        }
    }

    private void handleSendMoney(final CommandInput command, final ObjectNode objectNode,
                                 final ArrayNode output) {
        try {
            // Procesăm comanda sendMoney
            List<Map<String, Object>> response = bank.processCommand(command);

            // Verificăm dacă există un răspuns cu eroare "User not found" pentru destinatar
            if (!response.isEmpty() && response.get(0).containsKey("description") &&
                    response.get(0).get("description").equals("User not found")) {
                // Dacă destinatarul nu a fost găsit, adăugăm mesajul de eroare în output
                final var errorNode = objectMapper.createObjectNode();
                errorNode.put("description", "User not found");
                errorNode.put("timestamp", command.getTimestamp());
                objectNode.set("output", errorNode);
                output.add(objectNode);
            }
            // Nu adăugăm nimic în output dacă tranzacția a fost efectuată cu succes (fără erori)

        } catch (IllegalArgumentException e) {
            // În cazul unei erori generale, adăugăm eroarea în output
            final var errorNode = objectMapper.createObjectNode();
            errorNode.put("description", e.getMessage());
            errorNode.put("timestamp", command.getTimestamp());
            objectNode.set("output", errorNode);
            output.add(objectNode);
        }
    }



    private void handlePrintTransactions(final CommandInput command, final ObjectNode objectNode,
                                         final ArrayNode output) {
        final List<User> users = bank.getUsers();
        final PrintTransactions printTransactionsProcessor = new PrintTransactions(users);
        final var transactions = printTransactionsProcessor.printTransactions(command);
        objectNode.set("output", objectMapper.valueToTree(transactions));
        output.add(objectNode);
    }

    private void handleCheckCardStatus(final CommandInput command, final ArrayNode output) {
        final CheckCardStatus checkCardStatus = new CheckCardStatus();
        final Map<String, Object> checkCardStatusResponse = checkCardStatus.execute(command,
                bank.getUsers());

        if (!checkCardStatusResponse.isEmpty()) {
            final ObjectNode responseNode = objectMapper.createObjectNode();
            responseNode.put("command", checkCardStatusResponse.get("command").toString());
            responseNode.set("output",
                    objectMapper.valueToTree(checkCardStatusResponse.get("output")));
            responseNode.put("timestamp",
                    Integer.parseInt(checkCardStatusResponse.get("timestamp").toString()));

            output.add(responseNode);
        }
    }

    private void handleChangeInterestRate(final CommandInput command, final ArrayNode output) {
        final List<Map<String, Object>> response = bank.processCommand(command);
        if (!response.isEmpty()) {
            for (Map<String, Object> line : response) {
                final ObjectNode responseNode = objectMapper.createObjectNode();
                responseNode.put("command", line.get("command").toString());
                responseNode.set("output", objectMapper.valueToTree(line.get("output")));
                responseNode.put("timestamp", Integer.parseInt(line.get("timestamp").toString()));
                output.add(responseNode);
            }
        }
    }

    private void handleReport(final CommandInput command, final ArrayNode output) {
        final List<Map<String, Object>> response = bank.processCommand(command);

        for (Map<String, Object> line : response) {
            final ObjectNode responseNode = objectMapper.createObjectNode();
            responseNode.put("command", line.get("command").toString());

            if (line.containsKey("output")) {
                responseNode.set("output", objectMapper.valueToTree(line.get("output")));
            }

            responseNode.put("timestamp", Integer.parseInt(line.get("timestamp").toString()));
            output.add(responseNode);
        }
    }

    private void handleAddInterest(final CommandInput command, final ArrayNode output) {
        final List<Map<String, Object>> interestResponse = bank.processCommand(command);
        if (!interestResponse.isEmpty()) {
            for (Map<String, Object> line : interestResponse) {
                final ObjectNode responseNode = objectMapper.createObjectNode();
                responseNode.put("command", line.get("command").toString());
                responseNode.set("output", objectMapper.valueToTree(line.get("output")));
                responseNode.put("timestamp", Integer.parseInt(line.get("timestamp").toString()));
                output.add(responseNode);
            }
        }
    }
}
