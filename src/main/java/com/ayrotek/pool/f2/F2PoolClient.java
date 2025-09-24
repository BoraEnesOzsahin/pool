package com.ayrotek.pool.f2;


import com.ayrotek.pool.config.F2PoolProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;


import java.util.Map;


@Component
public class F2PoolClient {
private final RestTemplate restTemplate;
private final F2PoolProperties props;
private final ObjectMapper om = new ObjectMapper();


public F2PoolClient(RestTemplate restTemplate, F2PoolProperties props) {
this.restTemplate = restTemplate;
this.props = props;
}


public JsonNode post(String path, Map<String, Object> body) {
String url = props.getBaseUrl() + path; // path like "/hash_rate/worker/list"
try {
HttpHeaders headers = new HttpHeaders();
headers.setContentType(MediaType.APPLICATION_JSON);
headers.set("F2P-API-SECRET", props.getApiSecret());
HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
String response = restTemplate.postForObject(url, entity, String.class);
return om.readTree(response);
} catch (HttpStatusCodeException e) {
throw new RuntimeException("F2Pool API error (" + e.getStatusCode() + ") " + e.getResponseBodyAsString(), e);
} catch (Exception e) {
throw new RuntimeException("F2Pool API call failed: " + e.getMessage(), e);
}
}
}