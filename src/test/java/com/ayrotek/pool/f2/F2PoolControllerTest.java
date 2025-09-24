package com.ayrotek.pool.f2;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;


import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(F2PoolController.class)
class F2PoolControllerTest {
@Autowired MockMvc mvc;
@MockBean F2PoolService service;


@Test
void listAccounts_returnsOk() throws Exception {
ObjectMapper om = new ObjectMapper();
ObjectNode node = om.createObjectNode().put("code", 0);
when(service.listMiningUsers()).thenReturn(node);


mvc.perform(get("/api/f2pool/accounts").accept(MediaType.APPLICATION_JSON))
.andExpect(status().isOk())
.andExpect(jsonPath("$.code").value(0));
}
}