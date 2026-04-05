package com.quantrecon.core_engine.service;

import com.quantrecon.core_engine.model.ReconciledStrategy;
import com.quantrecon.core_engine.model.TradeLeg;
import com.quantrecon.core_engine.repository.ReconciledStrategyRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ReconciliationService {

    private final ReconciledStrategyRepository repository;
    private final Map<String, List<TradeLeg>> cboeCache = new ConcurrentHashMap<>();
    private final Map<String, List<TradeLeg>> ledgerCache = new ConcurrentHashMap<>();

    public ReconciliationService(ReconciledStrategyRepository repository) {
        this.repository = repository;
    }

    @KafkaListener(topics = "exchange-feed-topic", groupId = "recon-group")
    public void listenToExchangeFeed(String message) {
        TradeLeg cboeLeg = parseCboeTrade(message);
        if (cboeLeg == null) return;
        cboeCache.computeIfAbsent(cboeLeg.getStrategyHint(), k -> new ArrayList<>()).add(cboeLeg);
        tryToReconcile(cboeLeg.getStrategyHint());
    }

    @KafkaListener(topics = "ledger-feed-topic", groupId = "recon-group")
    public void listenToLedgerFeed(String message) {
        TradeLeg ledgerLeg = parseLedgerTrade(message);
        if (ledgerLeg == null) return;
        ledgerCache.computeIfAbsent(ledgerLeg.getStrategyHint(), k -> new ArrayList<>()).add(ledgerLeg);
        tryToReconcile(ledgerLeg.getStrategyHint());
    }

    private void tryToReconcile(String strategyId) {
        List<TradeLeg> cboeLegs = cboeCache.get(strategyId);
        List<TradeLeg> ledgerLegs = ledgerCache.get(strategyId);

        if (cboeLegs == null || ledgerLegs == null) {
            return;
        }

        int expectedLegCount = strategyId.startsWith("IC_") ? 4 : 3;

        if (cboeLegs.size() == expectedLegCount && ledgerLegs.size() == expectedLegCount) {
            ReconciledStrategy result = new ReconciledStrategy();
            result.setStrategyId(strategyId);
            result.setLegCount(ledgerLegs.size());

            double totalLedgerPrice = ledgerLegs.stream().mapToDouble(TradeLeg::getPrice).sum();
            double totalCboePrice = cboeLegs.stream().mapToDouble(TradeLeg::getPrice).sum();
            double priceDifference = Math.abs(totalLedgerPrice - totalCboePrice);
            result.setNetPriceDifference(Math.round(priceDifference * 100.0) / 100.0);

            int totalLedgerQty = ledgerLegs.stream().mapToInt(TradeLeg::getQuantity).sum();
            int totalCboeQty = cboeLegs.stream().mapToInt(TradeLeg::getQuantity).sum();
            int quantityDifference = Math.abs(totalLedgerQty - totalCboeQty);
            result.setQuantityDifference(quantityDifference);

            if (priceDifference > 0.001 ) {
                result.setStatus("PRICE_BREAK");
            } else if (quantityDifference > 0) {
                result.setStatus("QUANTITY_BREAK");
            } else {
                result.setStatus("MATCHED");
            }

            repository.save(result);
            System.out.println("Reconciled [" + result.getStatus() + "] " + strategyId
                + " | ΔPrice: $" + String.format("%.2f", priceDifference)
                + " | ΔQty: " + quantityDifference);

            cboeCache.remove(strategyId);
            ledgerCache.remove(strategyId);
        }
    }

    private TradeLeg parseCboeTrade(String csvLine) {
        try {
            String[] values = csvLine.split(",");
            TradeLeg leg = new TradeLeg();
            leg.setId(values[0].trim());
            leg.setTimestamp(Instant.parse(values[1].trim()));
            leg.setStrategyHint(values[2].trim());
            leg.setUnderlying(values[3].trim());
            leg.setExpiry(values[4].trim());
            leg.setStrike(Double.parseDouble(values[5].trim())); 
            leg.setOptionType(values[6].trim());
            leg.setSide(values[7].trim());
            leg.setPrice(Double.parseDouble(values[8].trim()));     
            leg.setQuantity(Integer.parseInt(values[9].trim())); 
            return leg;
        } catch (Exception e) {
            System.err.println("Error parsing CBOE trade: " + csvLine);
            return null;
        }
    }

    private TradeLeg parseLedgerTrade(String csvLine) {
        try {
            String[] values = csvLine.split(",");
            TradeLeg leg = new TradeLeg();
            leg.setId(values[0].trim());
            leg.setStrategyHint(values[1].trim());
            leg.setUnderlying(values[2].trim());
            leg.setExpiry(values[3].trim());
            leg.setStrike(Double.parseDouble(values[4].trim()));
            leg.setOptionType(values[5].trim());
            leg.setSide(values[6].trim());
            leg.setPrice(Double.parseDouble(values[7].trim()));   
            leg.setQuantity(Integer.parseInt(values[8].trim())); 
            return leg;
        } catch (Exception e) {
            System.err.println("Error parsing Ledger trade: " + csvLine);
            return null;
        }
    }
}