package com.quantrecon.core_engine.service; // <-- Matching your package name

import com.quantrecon.core_engine.model.ReconciledStrategy;
import com.quantrecon.core_engine.model.TradeLeg;
import com.quantrecon.core_engine.repository.ReconciledStrategyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReconciliationService {

    private final ReconciledStrategyRepository repository;
    private final ResourceLoader resourceLoader; // <-- ADDED THIS

    @Autowired
    public ReconciliationService(ReconciledStrategyRepository repository, ResourceLoader resourceLoader) { // <-- UPDATED CONSTRUCTOR
        this.repository = repository;
        this.resourceLoader = resourceLoader;
    }

    public void runReconciliation() throws IOException {
        // 1. Read the data from the classpath
        List<TradeLeg> cboeTrades = readCboeTradesFromClasspath("cboe_exchange_feed.csv");
        List<TradeLeg> ledgerTrades = readLedgerTradesFromClasspath("internal_ledger.csv");

        // (The rest of the logic is exactly the same as before)

        // 2. Group trades by their strategy identifier for easy lookup
        Map<String, List<TradeLeg>> cboeStrategyMap = cboeTrades.stream()
                .collect(Collectors.groupingBy(TradeLeg::getStrategyHint));
        Map<String, List<TradeLeg>> ledgerStrategyMap = ledgerTrades.stream()
                .collect(Collectors.groupingBy(TradeLeg::getStrategyHint));

        // 3. Loop through internal ledger strategies to find matches
        for (Map.Entry<String, List<TradeLeg>> ledgerEntry : ledgerStrategyMap.entrySet()) {
            String strategyId = ledgerEntry.getKey();
            List<TradeLeg> ledgerLegs = ledgerEntry.getValue();
            ReconciledStrategy result = new ReconciledStrategy();
            result.setStrategyId(strategyId);
            result.setLegCount(ledgerLegs.size());

            if (cboeStrategyMap.containsKey(strategyId)) {
                List<TradeLeg> cboeLegs = cboeStrategyMap.get(strategyId);

                if (ledgerLegs.size() != cboeLegs.size()) {
                    result.setStatus("MISSING_LEG");
                    result.setNetPriceDifference(0.0);
                } else {
                    double totalLedgerPrice = ledgerLegs.stream().mapToDouble(TradeLeg::getPrice).sum();
                    double totalCboePrice = cboeLegs.stream().mapToDouble(TradeLeg::getPrice).sum();
                    double priceDifference = Math.abs(totalLedgerPrice - totalCboePrice);

                    result.setNetPriceDifference(priceDifference);
                    if (priceDifference > 0.1) {
                        result.setStatus("PRICE_MISMATCH");
                    } else {
                        result.setStatus("MATCHED");
                    }
                }
                cboeStrategyMap.remove(strategyId);
            } else {
                result.setStatus("UNMATCHED_LEDGER");
            }
            repository.save(result);
        }

        // 4. Any remaining strategies in the CBOE map are orphans
        for (Map.Entry<String, List<TradeLeg>> cboeEntry : cboeStrategyMap.entrySet()) {
            ReconciledStrategy result = new ReconciledStrategy();
            result.setStrategyId(cboeEntry.getKey());
            result.setLegCount(cboeEntry.getValue().size());
            result.setStatus("UNMATCHED_EXCHANGE");
            repository.save(result);
        }
    }

    // v-- COMPLETELY NEW, MORE RELIABLE METHODS --v
    
    private List<TradeLeg> readCboeTradesFromClasspath(String fileName) throws IOException {
        List<TradeLeg> trades = new ArrayList<>();
        Resource resource = resourceLoader.getResource("classpath:data/" + fileName);
        InputStream inputStream = resource.getInputStream();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            br.readLine(); // Skip header
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                TradeLeg leg = new TradeLeg();
                leg.setId(values[0]);
                leg.setTimestamp(Instant.parse(values[1]));
                leg.setStrategyHint(values[2]);
                leg.setUnderlying(values[3]);
                leg.setExpiry(values[4]);
                leg.setStrike(Double.parseDouble(values[5]));
                leg.setOptionType(values[6]);
                leg.setSide(values[7]);
                leg.setPrice(Double.parseDouble(values[8]));
                leg.setQuantity(Integer.parseInt(values[9]));
                trades.add(leg);
            }
        }
        return trades;
    }
    
    private List<TradeLeg> readLedgerTradesFromClasspath(String fileName) throws IOException {
        List<TradeLeg> trades = new ArrayList<>();
        Resource resource = resourceLoader.getResource("classpath:data/" + fileName);
        InputStream inputStream = resource.getInputStream();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            br.readLine(); // Skip header
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                TradeLeg leg = new TradeLeg();
                leg.setId(values[0]);
                leg.setStrategyHint(values[1]); // Using strategyHint to store strategyId for grouping
                leg.setUnderlying(values[2]);
                leg.setExpiry(values[3]);
                leg.setStrike(Double.parseDouble(values[4]));
                leg.setOptionType(values[5]);
                leg.setSide(values[6]);
                leg.setPrice(Double.parseDouble(values[7]));
                leg.setQuantity(Integer.parseInt(values[8]));
                trades.add(leg);
            }
        }
        return trades;
    }
}