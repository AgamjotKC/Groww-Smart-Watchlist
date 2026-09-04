package org.example.global.growwsmartwatchlist;

import org.example.global.growwsmartwatchlist.controller.BasketController;
import org.example.global.growwsmartwatchlist.model.PrebuiltBasket;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class BasketControllerTest {

    @Autowired
    private BasketController basketController;

    @Test
    void testGetAllBaskets() {
        ResponseEntity<List<PrebuiltBasket>> response = basketController.getAllBaskets();
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isEmpty());
    }

    @Test
    void testGetBasketByName() {
        ResponseEntity<PrebuiltBasket> response = basketController.getBasketByName("Nifty Tech Leaders");
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("Nifty Tech Leaders", response.getBody().getName());
        assertTrue(response.getBody().getSymbols().contains("TCS"));
    }
}
