package com.ayrotek.dogecoin_pool.etc.boot;

import com.ayrotek.dogecoin_pool.etc.service.EtcSweeperService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class EtcSweepStartupRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(EtcSweepStartupRunner.class);

    private final EtcSweeperService sweeperService;

    @Value("${ETC_SWEEP_ON_STARTUP:false}")
    private boolean etcSweepOnStartup;

    public EtcSweepStartupRunner(EtcSweeperService sweeperService) {
        this.sweeperService = sweeperService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!etcSweepOnStartup) {
            log.info("[ETC] ETC_SWEEP_ON_STARTUP=false. Skipping one-shot startup sweep.");
            return;
        }

        log.info("[ETC] ETC_SWEEP_ON_STARTUP=true. Performing one-shot sweep at startup...");
        try {
            EtcSweeperService.EtcSweepResult result = sweeperService.sweepOnce();
            log.info("[ETC] Startup sweep completed: submitted={} message={}", 
                result.submitted(), result.message());
        } catch (Exception e) {
            log.error("[ETC] Startup sweep failed with exception", e);
        }
    }
}
