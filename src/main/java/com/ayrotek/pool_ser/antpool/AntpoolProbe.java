package com.ayrotek.pool_ser.antpool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.ayrotek.pool_ser.antpool.dto.AccountBalanceDto;
import com.ayrotek.pool_ser.antpool.dto.AntpoolResponse;
import com.ayrotek.pool_ser.antpool.dto.OverviewDto;

@Component
public class AntpoolProbe implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(AntpoolProbe.class);
    private static final String PROBE_FLAG = "ANTPOOL_PROBE_ENABLED";

    private final AntpoolClient client;
    private final AntpoolProperties properties;
    private final Environment environment;

    public AntpoolProbe(AntpoolClient client, AntpoolProperties properties, Environment environment) {
        this.client = client;
        this.properties = properties;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean enabled = Boolean.parseBoolean(environment.getProperty(PROBE_FLAG, "false"));
        if (!enabled) {
            LOGGER.info("AntPool probe disabled (set {}=true to enable)", PROBE_FLAG);
            return;
        }

        logAccountBalance();
        logOverview();
    }

    private void logAccountBalance() {
        try {
            AntpoolResponse<AccountBalanceDto> response = client.getAccountBalance(null);
            AccountBalanceDto dto = response.data();
            if (dto != null) {
                LOGGER.info("AntPool balance coin={} available={} unpaid={} total={}",
                        dto.coin(), dto.availableAmount(), dto.unpaidAmount(), dto.totalAmount());
            } else {
                LOGGER.warn("AntPool balance request returned no data for coin {}", properties.getDefaultCoin());
            }
        } catch (Exception ex) {
            LOGGER.error("AntPool balance probe failed: {}", ex.getMessage());
        }
    }

    private void logOverview() {
        try {
            AntpoolResponse<OverviewDto> response = client.getOverview(null);
            OverviewDto dto = response.data();
            if (dto != null) {
                LOGGER.info("AntPool overview coin={} userId={} workers(total/active/inactive/invalid)={}/{}/{}/{} unpaid={} total={} yesterday={}",
                        dto.coin(), dto.userId(), dto.totalWorkerNum(), dto.activeWorkerNum(),
                        dto.inactiveWorkerNum(), dto.invalidWorkerNum(), dto.unpaidAmount(),
                        dto.totalAmount(), dto.yesterdayAmount());
            } else {
                LOGGER.warn("AntPool overview request returned no data for coin {}", properties.getDefaultCoin());
            }
        } catch (Exception ex) {
            LOGGER.error("AntPool overview probe failed: {}", ex.getMessage());
        }
    }
}
