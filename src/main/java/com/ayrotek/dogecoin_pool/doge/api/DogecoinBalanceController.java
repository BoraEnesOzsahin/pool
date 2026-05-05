package com.ayrotek.dogecoin_pool.doge.api;

import com.ayrotek.dogecoin_pool.doge.client.ElectrsDogecoinClient;
import com.ayrotek.dogecoin_pool.doge.config.TatumDogecoinProperties;
import com.ayrotek.dogecoin_pool.doge.dto.DogeBalanceDto;
import com.ayrotek.dogecoin_pool.doge.util.DogecoinAddressValidator;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@RestController
public class DogecoinBalanceController {

    private final ElectrsDogecoinClient electrsClient;
    private final TatumDogecoinProperties props;

    public DogecoinBalanceController(ElectrsDogecoinClient electrsClient, TatumDogecoinProperties props) {
        this.electrsClient = electrsClient;
        this.props = props;
    }

    /**
     * Example:
     * - /api/doge/balances?address=ADDR1&address=ADDR2
     * - /api/doge/balances?addresses=ADDR1,ADDR2
     * If no addresses are provided, returns balances for configured hot/cold (if present).
     */
    @GetMapping(value = "/api/doge/balances", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<List<AddressBalanceResult>> getBalances(
            @RequestParam(name = "address", required = false) List<String> addressParams,
            @RequestParam(name = "addresses", required = false) String addressesCsv
    ) {
        List<String> addresses = normalizeAddresses(addressParams, addressesCsv);

        if (addresses.isEmpty()) {
            return Mono.just(List.of(new AddressBalanceResult(null, null, null,
                    "No addresses provided. Pass ?address=... or configure tatum.doge.hot-address / tatum.doge.cold-address.")));
        }

        if (addresses.size() > 100) {
            return Mono.just(List.of(new AddressBalanceResult(null, null, null,
                    "Too many addresses (max 100).")));
        }

        return Flux.fromIterable(addresses)
                .flatMap(address -> Mono.fromCallable(() -> {
                            String validationError = DogecoinAddressValidator.validate(address);
                            if (validationError != null) {
                                throw new IllegalArgumentException(validationError);
                            }
                            return electrsClient.getAddressBalance(address);
                        })
                        .subscribeOn(Schedulers.boundedElastic())
                        .map(dto -> new AddressBalanceResult(address, dto, computeConfirmedBalance(dto), null))
                        .onErrorResume(ex -> Mono.just(new AddressBalanceResult(address, null, null, toErrorMessage(ex)))),
                        6
                )
                .collectList();
    }

    /**
        * "Actual" confirmed balance approximation when the upstream returns components.
     * Uses:
     * - if dto.balance is present and > 0 => use it
     * - else compute (incoming - outgoing - outgoingPending)
     * incomingPending is intentionally not added (it's not confirmed).
     */
    private String computeConfirmedBalance(DogeBalanceDto dto) {
        if (dto == null) {
            return null;
        }

        BigDecimal direct = parseDecimal(dto.balance());
        if (direct.compareTo(BigDecimal.ZERO) > 0) {
            return direct.stripTrailingZeros().toPlainString();
        }

        BigDecimal incoming = parseDecimal(dto.incoming());
        BigDecimal outgoing = parseDecimal(dto.outgoing());
        BigDecimal outgoingPending = parseDecimal(dto.outgoingPending());

        BigDecimal computed = incoming.subtract(outgoing).subtract(outgoingPending);
        if (computed.compareTo(BigDecimal.ZERO) < 0) {
            computed = BigDecimal.ZERO;
        }
        return computed.stripTrailingZeros().toPlainString();
    }

    private BigDecimal parseDecimal(String value) {
        if (value == null || value.isBlank()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(value.trim());
    }

    private List<String> normalizeAddresses(List<String> addressParams, String addressesCsv) {
        Set<String> out = new LinkedHashSet<>();

        if (addressParams != null) {
            for (String a : addressParams) {
                if (a != null && !a.isBlank()) {
                    out.add(a.trim());
                }
            }
        }

        if (addressesCsv != null && !addressesCsv.isBlank()) {
            String[] parts = addressesCsv.split("[\\s,]+");
            for (String p : parts) {
                if (p != null && !p.isBlank()) {
                    out.add(p.trim());
                }
            }
        }

        if (out.isEmpty()) {
            if (props.getHotAddress() != null && !props.getHotAddress().isBlank()) {
                out.add(props.getHotAddress().trim());
            }
            if (props.getColdAddress() != null && !props.getColdAddress().isBlank()) {
                out.add(props.getColdAddress().trim());
            }
        }

        return new ArrayList<>(out);
    }

    private String toErrorMessage(Throwable ex) {
        if (ex instanceof WebClientResponseException wcre) {
            String body = wcre.getResponseBodyAsString();
            String msg = "Electrs error: " + wcre.getStatusCode();
            if (body != null && !body.isBlank()) {
                msg = msg + " body=" + shorten(body, 400);
            }
            return msg;
        }
        if (ex instanceof IllegalArgumentException iae) {
            return iae.getMessage();
        }
        String message = ex.getMessage();
        return (message == null || message.isBlank()) ? ex.getClass().getSimpleName() : message;
    }

    private String shorten(String value, int maxLen) {
        String trimmed = value.trim();
        if (trimmed.length() <= maxLen) {
            return trimmed;
        }
        return trimmed.substring(0, maxLen) + "…";
    }

    public record AddressBalanceResult(
            String address,
            DogeBalanceDto balance,
            String confirmedBalance,
            String error
    ) {
    }
}
