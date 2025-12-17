package com.ayrotek.dogecoin_pool.doge.boot;

import com.ayrotek.dogecoin_pool.doge.service.DogecoinSweeperService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class DogecoinGenericSweepStartupRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DogecoinGenericSweepStartupRunner.class);

    private final DogecoinSweeperService sweeperService;
    private final Environment environment;

    public DogecoinGenericSweepStartupRunner(
            DogecoinSweeperService sweeperService,
            Environment environment
    ) {
        this.sweeperService = sweeperService;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean enabled = environment.getProperty("DOGE_GENERIC_SWEEP_ON_STARTUP", Boolean.class, false);
        if (!enabled) {
            log.info("Dogecoin generic sweep on startup disabled (DOGE_GENERIC_SWEEP_ON_STARTUP != true)."
                    + " Set DOGE_GENERIC_SWEEP_ON_STARTUP=true to enable.");
            return;
        }

        log.info("Dogecoin sweep on startup ENABLED.");

        DogecoinSweeperService.DogeSweepResult result;
        try {
            result = sweeperService.sweepOnce();
        } catch (Exception ex) {
            log.error("Dogecoin sweep on startup failed (unexpected exception).", ex);
            return;
        }
        if (result == null) {
            log.warn("Dogecoin sweep returned null result.");
            return;
        }

        if (result.submitted()) {
            log.info("Dogecoin sweep submitted. txId={} amountSent={} hotBalance={}",
                    result.txId(), result.amountSent(), result.hotBalance());
        } else {
            log.warn("Dogecoin sweep not submitted. message={} amountSent={} hotBalance={}",
                    result.message(), result.amountSent(), result.hotBalance());
        }
    }
}
