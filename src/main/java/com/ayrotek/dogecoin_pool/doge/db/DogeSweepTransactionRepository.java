package com.ayrotek.dogecoin_pool.doge.db;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public class DogeSweepTransactionRepository {

    private final JdbcTemplate jdbcTemplate;

    public DogeSweepTransactionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(
            String hotAddress,
            String coldAddress,
            String txId,
            boolean submitted,
            BigDecimal hotBalance,
            BigDecimal amountSent,
            BigDecimal updatedHotBalance,
            String message
    ) {
        jdbcTemplate.update(
            "INSERT INTO doge_sweep_transactions (hot_address, cold_address, tx_id, submitted, hot_balance, amount_sent, updated_hot_balance, message) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                hotAddress,
                coldAddress,
                txId,
                submitted,
                hotBalance,
                amountSent,
            updatedHotBalance,
                message
        );
    }
}
