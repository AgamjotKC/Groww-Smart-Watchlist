package org.example.global.growwsmartwatchlist.controller;

import org.example.global.growwsmartwatchlist.model.PrebuiltBasket;
import org.example.global.growwsmartwatchlist.service.BasketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/baskets", "/api/baskets"})
public class BasketController {

    @Autowired
    private BasketService basketService;

    @GetMapping
    public ResponseEntity<List<PrebuiltBasket>> getAllBaskets() {
        return ResponseEntity.ok(basketService.getAllBaskets());
    }

    @GetMapping("/{name}")
    public ResponseEntity<PrebuiltBasket> getBasketByName(@PathVariable String name) {
        return basketService.getBasketByName(name)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
