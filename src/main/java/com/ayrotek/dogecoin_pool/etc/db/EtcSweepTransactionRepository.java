package com.ayrotek.dogecoin_pool.etc.db;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class EtcSweepTransactionRepository {

    private final JdbcTemplate jdbcTemplate;

    public EtcSweepTransactionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Insert a record of an ETC sweep transaction.
     * Amounts are stored as Wei strings to handle large numbers.
     */
    public void insert(
            String hotAddress,
            String coldAddress,
            String txHash,
            boolean submitted,
            String hotBalanceWei,
            String amountSentWei,
            String gasUsedWei,
            String updatedHotBalanceWei,
            String message
    ) {
        jdbcTemplate.update(
            "INSERT INTO etc_sweep_transactions " +
                "(hot_address, cold_address, tx_hash, submitted, hot_balance_wei, amount_sent_wei, gas_used_wei, updated_hot_balance_wei, message) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                hotAddress,
                coldAddress,
                txHash,
                submitted,
                hotBalanceWei,
                amountSentWei,
                gasUsedWei,
                updatedHotBalanceWei,
                message
        );
    }
}
