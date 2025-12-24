package com.ayrotek.dogecoin_pool.doge.client;

import com.ayrotek.dogecoin_pool.doge.config.ElectrsDogecoinProperties;
import com.ayrotek.dogecoin_pool.doge.dto.DogeBalanceDto;
import com.ayrotek.dogecoin_pool.doge.dto.ElectrsAddressResponse;
import com.ayrotek.dogecoin_pool.doge.util.DogecoinAddressValidator;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class ElectrsDogecoinClient {

    private static final BigDecimal ATOMS_PER_DOGE = new BigDecimal("100000000");

    private final WebClient webClient;

    public ElectrsDogecoinClient(ElectrsDogecoinProperties props, WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl(props.getBaseUrl())
                .build();
    }

    public DogeBalanceDto getAddressBalance(String address) {
        String validationError = DogecoinAddressValidator.validate(address);
        if (validationError != null) {
            throw new IllegalArgumentException(validationError);
        }

        ElectrsAddressResponse resp = webClient.get()
                .uri("/address/{address}", address)
                .retrieve()
                .bodyToMono(ElectrsAddressResponse.class)
                .block();

        if (resp == null || resp.chainStats() == null || resp.mempoolStats() == null) {
            throw new IllegalStateException("electrs returned null/invalid response");
        }

        BigDecimal incoming = atomsToDoge(resp.chainStats().fundedTxoSum());
        BigDecimal outgoing = atomsToDoge(resp.chainStats().spentTxoSum());

        BigDecimal incomingPending = atomsToDoge(resp.mempoolStats().fundedTxoSum());
        BigDecimal outgoingPending = atomsToDoge(resp.mempoolStats().spentTxoSum());

        BigDecimal confirmed = incoming.subtract(outgoing);
        if (confirmed.compareTo(BigDecimal.ZERO) < 0) {
            confirmed = BigDecimal.ZERO;
        }

        return new DogeBalanceDto(
                incoming.toPlainString(),
                outgoing.toPlainString(),
                incomingPending.toPlainString(),
                outgoingPending.toPlainString(),
                confirmed.setScale(8, RoundingMode.DOWN).toPlainString()
        );
    }

    private BigDecimal atomsToDoge(long atoms) {
        return BigDecimal.valueOf(atoms)
                .divide(ATOMS_PER_DOGE, 8, RoundingMode.DOWN);
    }
}
