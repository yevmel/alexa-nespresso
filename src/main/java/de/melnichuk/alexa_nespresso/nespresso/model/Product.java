package de.melnichuk.alexa_nespresso.nespresso.model;

public enum Product {
    ristretto("7615.20", "Ristretto"),
    arpeggio("7431.20", "Arpeggio"),
    roma("7439.20", "Roma");

    private final String name;
    private final String productId;

    Product(String productId, String name) {
        this.productId = productId;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getProductId() {
        return productId;
    }
}
