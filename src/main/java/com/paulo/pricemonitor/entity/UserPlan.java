package com.paulo.pricemonitor.entity;

public enum UserPlan {

    FREE(3),
    PRO(15);

    private final int maxProducts;

    UserPlan(int maxProducts) {
        this.maxProducts = maxProducts;
    }

    public int getMaxProducts() {
        return maxProducts;
    }
}
