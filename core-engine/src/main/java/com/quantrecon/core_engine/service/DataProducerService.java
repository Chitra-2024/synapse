package com.quantrecon.core_engine.service;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Random;
import java.util.UUID;

@Service
public class DataProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Random random = new Random();

    private static final String[] UNDERLYINGS = {"SPX", "NDX", "RUT"};

    private static final int[][] STRIKE_RANGES = {
        {4200, 4800},  // SPX
        {15000, 16500}, // NDX
        {1800, 2200}   // RUT
    };

    private static final String[] EXPIRIES = {
        "2025-10-17", "2025-11-21", "2025-12-19",
        "2026-01-16", "2026-02-20", "2026-03-20"
    };

    private enum ChaosType {
        MATCHED, PRICE_BREAK, QUANTITY_BREAK
    }

    private int strategyCounter = 0;

    public DataProducerService(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelay = 2000)
    public void generateAndPublishStrategy() {
        strategyCounter++;
        ChaosType chaos = pickChaosType();

        int underlyingIdx = random.nextInt(UNDERLYINGS.length);
        String underlying = UNDERLYINGS[underlyingIdx];
        int baseStrike = roundToNearest50(
            STRIKE_RANGES[underlyingIdx][0] +
            random.nextInt(STRIKE_RANGES[underlyingIdx][1] - STRIKE_RANGES[underlyingIdx][0])
        );
        String expiry = EXPIRIES[random.nextInt(EXPIRIES.length)];

        boolean isIronCondor = random.nextBoolean();
        String strategyPrefix = isIronCondor ? "IC_" : "BF_";
        String strategyId = strategyPrefix + underlying + "_" + baseStrike;
        int legCount = isIronCondor ? 4 : 3;

        int baseQuantity = (random.nextInt(4) + 1) * 5; 

        System.out.println("[Generator #" + strategyCounter + "] Creating " + strategyId
            + " | Chaos: " + chaos + " | Legs: " + legCount);

        Instant now = Instant.now();

        for (int i = 0; i < legCount; i++) {
            String execId = "EXEC-" + UUID.randomUUID().toString().substring(0, 8);
            String ledgerId = "L-" + UUID.randomUUID().toString().substring(0, 8);

            Instant legTimestamp = now.plusMillis(i * 2);

            int legStrike = computeLegStrike(baseStrike, i, isIronCondor);
            String optionType = computeOptionType(i, isIronCondor);
            String side = computeSide(i, isIronCondor);
            double basePrice = generateRealisticPrice();
            int quantity = computeQuantity(baseQuantity, i, isIronCondor);

            String exchangeLine = String.join(",",
                execId,
                legTimestamp.toString(),
                strategyId,
                underlying,
                expiry,
                String.valueOf(legStrike),
                optionType,
                side,
                String.format("%.2f", basePrice),
                String.valueOf(quantity)
            );
            kafkaTemplate.send("exchange-feed-topic", exchangeLine);

            double ledgerPrice = basePrice;
            int ledgerQuantity = quantity;

            if (chaos == ChaosType.PRICE_BREAK) {
                if (i == 0 || random.nextDouble() < 0.3) {
                    double offset = 0.01 + random.nextDouble() * 0.49;
                    ledgerPrice = basePrice + (random.nextBoolean() ? offset : -offset);
                    ledgerPrice = Math.round(ledgerPrice * 100.0) / 100.0;
                }
            } else if (chaos == ChaosType.QUANTITY_BREAK) {
                if (i == 0 || random.nextDouble() < 0.3) {
                    int qtyOffset = random.nextInt(5) + 1; 
                    ledgerQuantity = quantity + (random.nextBoolean() ? qtyOffset : -qtyOffset);
                    if (ledgerQuantity <= 0) ledgerQuantity = 1;
                }
            }

            String ledgerLine = String.join(",",
                ledgerId,
                strategyId,
                underlying,
                expiry,
                String.valueOf(legStrike),
                optionType,
                side,
                String.format("%.2f", ledgerPrice),
                String.valueOf(ledgerQuantity)
            );
            kafkaTemplate.send("ledger-feed-topic", ledgerLine);
        }
    }

    private ChaosType pickChaosType() {
        int roll = random.nextInt(3);
        return ChaosType.values()[roll];
    }

    private int roundToNearest50(int value) {
        return ((value + 25) / 50) * 50;
    }

    private int computeLegStrike(int baseStrike, int legIndex, boolean isIronCondor) {
        if (isIronCondor) {
            return switch (legIndex) {
                case 0 -> baseStrike - 50;
                case 1 -> baseStrike;
                case 2 -> baseStrike + 100;
                case 3 -> baseStrike + 150;
                default -> baseStrike;
            };
        } else {
            return switch (legIndex) {
                case 0 -> baseStrike - 50;
                case 1 -> baseStrike;
                case 2 -> baseStrike + 50;
                default -> baseStrike;
            };
        }
    }

    
    private String computeOptionType(int legIndex, boolean isIronCondor) {
        if (isIronCondor) {
            return legIndex < 2 ? "PUT" : "CALL";
        }
        return "CALL";
    }

    private String computeSide(int legIndex, boolean isIronCondor) {
        if (isIronCondor) {
            return (legIndex == 0 || legIndex == 3) ? "SELL" : "BUY";
        }
        return (legIndex == 1) ? "SELL" : "BUY";
    }

    private int computeQuantity(int baseQuantity, int legIndex, boolean isIronCondor) {
        if (!isIronCondor && legIndex == 1) {
            return baseQuantity * 2;
        }
        return baseQuantity;
    }

    private double generateRealisticPrice() {
        double price = 5.0 + random.nextDouble() * 90.0;
        return Math.round(price * 100.0) / 100.0;
    }
}