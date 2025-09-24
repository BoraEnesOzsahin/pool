package com.ayrotek.pool.f2;


import com.ayrotek.pool.config.F2PoolProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;


import java.util.Map;


import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;


class F2PoolClientTest {


@Test
void listMiningUsers_postsJsonAndAuthHeader() {
RestTemplate rest = new RestTemplate();
MockRestServiceServer server = MockRestServiceServer.createServer(rest);


F2PoolProperties props = new F2PoolProperties();
props.setBaseUrl("http://localhost:8089/v2");
props.setApiSecret("test-token");
F2PoolClient client = new F2PoolClient(rest, props);


server.expect(requestTo("http://localhost:8089/v2/mining_user/list"))
.andExpect(method(HttpMethod.POST))
.andExpect(header("F2P-API-SECRET", "test-token"))
.andExpect(content().json("{}"))
.andRespond(withSuccess("{\"code\":0,\"data\":{\"mining_user_list\":[]}}", MediaType.APPLICATION_JSON));


JsonNode node = client.post("/mining_user/list", Map.of());
assertThat(node.get("code").asInt()).isEqualTo(0);


server.verify();
}
}