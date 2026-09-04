package org.example.global.growwsmartwatchlist;

import org.example.global.growwsmartwatchlist.controller.DeltaController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
public class DeltaControllerIntegrationTest {

    @Autowired
    private DeltaController deltaController;

    @Test
    void testGetWatchlistDelta() {
        assertNotNull(deltaController);
    }
}
