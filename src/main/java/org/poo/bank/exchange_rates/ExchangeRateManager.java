package org.poo.bank.exchange_rates;

import org.poo.fileio.ExchangeInput;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Collections;

public final class ExchangeRateManager {
    private static ExchangeRateManager instance;
    private final List<ExchangeRate> exchangeRates = new ArrayList<>();
    private final Map<String, Map<String, Double>> exchangeGraph = new HashMap<>();

    private ExchangeRateManager() { }

    /**
     * Returneaza instanta unica a ExchangeRateManager.
     * @return instanta ExchangeRateManager
     */
    public static ExchangeRateManager getInstance() {
        if (instance == null) {
            instance = new ExchangeRateManager();
        }
        return instance;
    }

    /**
     * Incarca ratele de schimb dintr-o lista de inputuri si
     * construieste graful de rate de schimb.
     * @param exchangeInputs lista de inputuri care contine ratele de schimb.
     */
    public void loadExchangeRates(final List<ExchangeInput> exchangeInputs) {
        exchangeRates.clear();
        exchangeGraph.clear();
        for (final ExchangeInput input : exchangeInputs) {
            ExchangeRate rate = new ExchangeRate(input);
            exchangeRates.add(rate);
            addToGraph(rate.getFrom(), rate.getTo(), rate.getRate());
        }
    }

    /**
     * Adauga o rata de schimb in graful de rate.
     * @param from moneda de origine
     * @param to moneda tinta
     * @param rate rata de schimb
     */
    private void addToGraph(final String from, final String to, final double rate) {
        exchangeGraph.putIfAbsent(from, new HashMap<>());
        exchangeGraph.get(from).put(to, rate);

        // Adaugă rata inversă corectă
        exchangeGraph.putIfAbsent(to, new HashMap<>());
        if (rate > 0) {
            double inverseRate = 1.0 / rate;
            exchangeGraph.get(to).put(from, inverseRate);
        }
    }


    /**
     * Converteste o suma dintr-o moneda in alta folosind rata de schimb.
     * @param from moneda de origine
     * @param to moneda tinta
     * @param amount suma de convertit
     * @return suma convertita
     */
    public double convertCurrency(final String from, final String to, final double amount) {
        double rate = getExchangeRate(from, to);
        return amount * rate;
    }

    /**
     * Obtine rata de schimb dintre două monede.
     * Daca nu exista o rata directa se va cauta o rata indirecta folosind un algoritm de BFS.
     * @param from moneda de origine
     * @param to moneda tintă
     * @return rata de schimb
     */
    public double getExchangeRate(final String from, final String to) {
        if (from.equalsIgnoreCase(to)) {
            return 1.0;
        }

        Queue<String> queue = new LinkedList<>();
        Map<String, Double> visited = new HashMap<>();
        queue.add(from);
        visited.put(from, 1.0);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            double currentRate = visited.get(current);

            Map<String, Double> neighbors = exchangeGraph.getOrDefault(current,
                    Collections.emptyMap());
            for (Map.Entry<String, Double> entry : neighbors.entrySet()) {
                String neighbor = entry.getKey();
                double rate = entry.getValue();

                if (!visited.containsKey(neighbor)) {
                    double newRate = currentRate * rate;
                    visited.put(neighbor, newRate);

                    if (neighbor.equalsIgnoreCase(to)) {
                        return newRate;
                    }
                    queue.add(neighbor);
                }
            }
        }

        // Dacă nu există o cale valabilă între monede
        return 0;
    }

}
