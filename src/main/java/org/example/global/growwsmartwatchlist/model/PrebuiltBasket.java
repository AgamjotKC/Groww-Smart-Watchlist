package org.example.global.growwsmartwatchlist.model;

import java.util.List;

public class PrebuiltBasket {
    private String name;
    private String description;
    private List<String> symbols;

    public PrebuiltBasket() {}

    public PrebuiltBasket(String name, String description, List<String> symbols) {
        this.name = name;
        this.description = description;
        this.symbols = symbols;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<String> getSymbols() { return symbols; }
    public void setSymbols(List<String> symbols) { this.symbols = symbols; }
}
