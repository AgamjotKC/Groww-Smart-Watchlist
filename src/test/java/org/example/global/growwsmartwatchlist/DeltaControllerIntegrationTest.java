package org.example.global.growwsmartwatchlist;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class DeltaControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testGetWatchlistDelta() throws Exception {
        mockMvc.perform(get("/api/watchlists/1/delta?anchor=SINCE_LAST_SEEN"))
                .andExpect(status().isOk());
    }
}
