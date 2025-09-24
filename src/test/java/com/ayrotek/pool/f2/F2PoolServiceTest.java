package com.ayrotek.pool.f2;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;


import java.util.HashMap;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.when;


class F2PoolServiceTest {
@Test
void listWorkers_delegatesToClient() throws Exception {
F2PoolClient client = Mockito.mock(F2PoolClient.class);
F2PoolService service = new F2PoolService(client);
ObjectMapper om = new ObjectMapper();
JsonNode fake = om.readTree("{\"data\":{\"workers\":[]}} ");
when(client.post(Mockito.eq("/hash_rate/worker/list"), anyMap())).thenReturn(fake);


JsonNode res = service.listWorkers("mysub", "LTC");
assertThat(res.path("data").path("workers").isArray()).isTrue();
}
}