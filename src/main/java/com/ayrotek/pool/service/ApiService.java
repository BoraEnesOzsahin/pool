package com.ayrotek.pool.service;

import org.springframework.beans.factory.annotation.Value;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Hex;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpMethod;


@Service
public class ApiService {
	// Antpool API credentials (set via environment variables or properties)
	@Value("${antpool.api.key}")
	private String antpoolApiKey;

	@Value("${antpool.api.secret}")
	private String antpoolApiSecret;

	@Value("${antpool.api.userid}")
	private String antpoolUserId;


	// Generate a random nonce (timestamp)
	private String generateNonce() {
		return String.valueOf(System.currentTimeMillis());
	}


	private String generateSignature(String userId, String key, String nonce, String secret) {
	    try {
	        String data = nonce + userId + key;
	        Mac hmacSha256 = Mac.getInstance("HmacSHA256");
	        SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
	        hmacSha256.init(secretKey);
	        return Hex.encodeHexString(hmacSha256.doFinal(data.getBytes())).toUpperCase();
	    } catch (Exception e) {
	        throw new RuntimeException("Failed to generate signature", e);
	    }
	}

	// Call Antpool poolStats API
	public String getAntpoolPoolStats(String coin) {
	String url = "https://antpool.com/api/poolStats.htm";
	String key = antpoolApiKey;
	String nonce = generateNonce();
	String signature = generateSignature(antpoolUserId, key, nonce, antpoolApiSecret);

	org.springframework.util.LinkedMultiValueMap<String, String> params = new org.springframework.util.LinkedMultiValueMap<>();
	params.add("userId", antpoolUserId);
	params.add("key", key);
	params.add("nonce", nonce);
	params.add("signature", signature);
	params.add("coin", coin);

	HttpHeaders headers = new HttpHeaders();
	headers.set("Content-Type", "application/x-www-form-urlencoded");
	HttpEntity<org.springframework.util.MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);
	ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
	return response.getBody();
	}


	private final RestTemplate restTemplate = new RestTemplate();
}
