package com.example.myapplication.domain.model;


import com.example.myapplication.domain.model.*;
/** Local shopping item shown by the shopping feature. */
public final class ShoppingListItem {
    public final String id;
    public final String name;
    public final String amount;
    public final String unit;
    public final String status;
    public final String category;

    public ShoppingListItem(String id, String name, String amount, String unit, String status, String category) {
        this.id = id;
        this.name = name;
        this.amount = amount;
        this.unit = unit;
        this.status = status;
        this.category = category;
    }
}
