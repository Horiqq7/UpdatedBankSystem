package org.poo.bank.commands.print_commands;

import org.poo.bank.user.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class PrintUsers {
    private final List<User> users;

    public PrintUsers(final List<User> users) {
        this.users = users;
    }

    /**
     * Executa comanda de printare a utilizatorilor.
     *
     * @return Lista de harti cu informatii despre utilizatori
     */
    public List<Map<String, Object>> execute() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (User user : users) {
            result.add(user.toMap());
        }
        return result;
    }

}
