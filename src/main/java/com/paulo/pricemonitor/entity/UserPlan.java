package com.paulo.pricemonitor.entity;

public enum UserPlan {

    FREE(3),
    PRO(15),
    ENTERPRISE(-1); // -1 = sem limite de produtos Enterprise

    private final int maxProducts;

    UserPlan(int maxProducts) {
        this.maxProducts = maxProducts;
    }

    public int getMaxProducts() {
        return maxProducts;
    }

    public boolean isEnterprise() {
        return this == ENTERPRISE;
    }
}
