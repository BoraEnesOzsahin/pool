package com.ayrotek.dogecoin_pool.doge.boot;

import com.ayrotek.dogecoin_pool.doge.service.DogecoinSweeperService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "DOGE_SWEEP_PERIODIC_ENABLED", havingValue = "true")
public class DogecoinSweepScheduler {

    private static final Logger log = LoggerFactory.getLogger(DogecoinSweepScheduler.class);

    private final DogecoinSweeperService sweeperService;

    public DogecoinSweepScheduler(DogecoinSweeperService sweeperService) {
        this.sweeperService = sweeperService;
    }

    @Scheduled(
            fixedDelayString = "${DOGE_SWEEP_FIXED_DELAY_MS:60000}",
            initialDelayString = "${DOGE_SWEEP_INITIAL_DELAY_MS:15000}"
    )
    public void sweepPeriodically() {
        DogecoinSweeperService.DogeSweepResult result;
        try {
            result = sweeperService.sweepOnce();
        } catch (Exception ex) {
            log.error("Dogecoin periodic sweep failed (unexpected exception).", ex);
            return;
        }

        if (result == null) {
            log.warn("Dogecoin periodic sweep returned null result.");
            return;
        }

        if (result.submitted()) {
            log.info("Dogecoin periodic sweep submitted. txId={} amountSent={} hotBalance={}",
                    result.txId(), result.amountSent(), result.hotBalance());
        } else {
            log.info("Dogecoin periodic sweep not submitted. message={} amountSent={} hotBalance={}",
                    result.message(), result.amountSent(), result.hotBalance());
        }
    }
}
