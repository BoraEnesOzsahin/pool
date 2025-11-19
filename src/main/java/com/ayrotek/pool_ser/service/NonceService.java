package com.ayrotek.pool_ser.service;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ayrotek.pool_ser.entity.NonceLock;
import com.ayrotek.pool_ser.repository.NonceLockRepository;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;

@Service
public class NonceService {

    private final Web3j web3j;
    private final NonceLockRepository nonceLockRepository;

    public NonceService(Web3j web3j, NonceLockRepository nonceLockRepository) {
        this.web3j = web3j;
        this.nonceLockRepository = nonceLockRepository;
    }

    @Transactional
    public BigInteger nextNonce(String address) {
        String normalized = normalizeAddress(address);

        return nonceLockRepository.findForUpdate(normalized)
            .map(lock -> allocateNextNonce(normalized, lock))
            .orElseGet(() -> initializeNonce(normalized));
    }

    private BigInteger allocateNextNonce(String normalized, NonceLock lock) {
        BigInteger chainNonce = fetchChainNonce(normalized);

        BigInteger last = lock.getLastNonce();
        BigInteger candidate = last.add(BigInteger.ONE);
        if (chainNonce != null && chainNonce.compareTo(candidate) > 0) {
            candidate = chainNonce;
        }

        lock.setLastNonce(candidate);
        return candidate;
    }

    private BigInteger initializeNonce(String normalized) {
        BigInteger chainNonce = fetchChainNonce(normalized);
        if (chainNonce == null) {
            throw new IllegalStateException("Chain returned null nonce for address " + normalized);
        }

        NonceLock lock = new NonceLock();
        lock.setAddress(normalized);
        lock.setLastNonce(chainNonce);
        try {
            nonceLockRepository.save(lock);
            return chainNonce;
        } catch (DataIntegrityViolationException ex) {
            return nonceLockRepository.findForUpdate(normalized)
                    .map(existing -> allocateNextNonce(normalized, existing))
                    .orElseThrow(() -> new IllegalStateException(
                            "Nonce lock insert failed due to race for address " + normalized, ex));
        }
    }

    private BigInteger fetchChainNonce(String normalized) {
        EthGetTransactionCount response;
        try {
            response = web3j.ethGetTransactionCount(normalized, DefaultBlockParameterName.PENDING).send();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to fetch nonce from chain for address " + normalized, ex);
        }
        return response != null ? response.getTransactionCount() : null;
    }

    private String normalizeAddress(String address) {
        if (address == null) {
            throw new IllegalArgumentException("Address must not be null");
        }
        return address.toLowerCase(Locale.ROOT);
    }
}
