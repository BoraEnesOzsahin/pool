package com.ayrotek.dogecoin_pool.etc.boot;

import com.ayrotek.dogecoin_pool.etc.service.EtcSweeperService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EtcSweepScheduler {

    private static final Logger log = LoggerFactory.getLogger(EtcSweepScheduler.class);

    private final EtcSweeperService sweeperService;

    @Value("${ETC_SWEEP_PERIODIC_ENABLED:false}")
    private boolean periodicEnabled;

    public EtcSweepScheduler(EtcSweeperService sweeperService) {
        this.sweeperService = sweeperService;
    }

    /**
     * Periodically sweep ETC from hot wallet to cold wallet.
     * Initial delay and fixed delay are configured via application.properties.
     */
    @Scheduled(
        initialDelayString = "${ETC_SWEEP_INITIAL_DELAY_MS:15000}",
        fixedDelayString = "${ETC_SWEEP_FIXED_DELAY_MS:15000}"
    )
    public void scheduledSweep() {
        if (!periodicEnabled) {
            log.debug("[ETC] ETC_SWEEP_PERIODIC_ENABLED=false. Skipping scheduled sweep.");
            return;
        }

        log.info("[ETC] Running scheduled ETC sweep...");
        try {
            EtcSweeperService.EtcSweepResult result = sweeperService.sweepOnce();
            log.info("[ETC] Scheduled sweep completed: submitted={} message={}", 
                result.submitted(), result.message());
        } catch (Exception e) {
            log.error("[ETC] Scheduled sweep failed", e);
        }
    }
}
