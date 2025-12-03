package com.ayrotek.pool_ser.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.NetVersion;
import org.web3j.protocol.core.methods.response.Web3ClientVersion;

import com.ayrotek.pool_ser.api.dto.Web3HealthDto;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final Web3j web3j;
    private final Credentials credentials;

    public HealthController(Web3j web3j, Credentials credentials) {
        this.web3j = web3j;
        this.credentials = credentials;
    }

    @GetMapping("/health/web3")
    public Web3HealthDto web3Health() {
        try {
            Web3ClientVersion clientVersion = web3j.web3ClientVersion().send();
            NetVersion network = web3j.netVersion().send();
            // Touch credentials to make sure the bean is initialized even if we don't expose the address.
            credentials.getAddress();
            return new Web3HealthDto(true, clientVersion.getWeb3ClientVersion(), network.getNetVersion(), null);
        } catch (Exception ex) {
            return new Web3HealthDto(false, null, null, safeError(ex));
        }
    }

    private String safeError(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return ex.getClass().getSimpleName();
        }
        return message.length() > 256 ? message.substring(0, 256) : message;
    }
}
