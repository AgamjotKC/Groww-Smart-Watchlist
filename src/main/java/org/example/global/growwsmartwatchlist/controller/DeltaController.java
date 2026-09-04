package org.example.global.growwsmartwatchlist.controller;

import org.example.global.growwsmartwatchlist.model.DeltaResponse;
import org.example.global.growwsmartwatchlist.service.DeltaComputationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/watchlists", "/api/watchlists"})
public class DeltaController {

    @Autowired
    private DeltaComputationService deltaComputationService;

    @GetMapping("/{id}/delta")
    public ResponseEntity<DeltaResponse> getWatchlistDelta(@PathVariable Long id,
                                                          @RequestParam(required = false, defaultValue = "SINCE_LAST_SEEN") String anchor) {
        DeltaResponse response = deltaComputationService.computeRankedDelta(id, anchor);
        return ResponseEntity.ok(response);
    }
}
