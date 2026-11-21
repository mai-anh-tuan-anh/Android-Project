package com.example.myapplication;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** User-selected aisle groups for manually managed shopping items. */
final class ShoppingCategoryCatalog {
    static final String VEGETABLES = "Rau củ";
    static final String MEAT_AND_FISH = "Thịt cá";
    static final String DAIRY_AND_EGGS = "Sữa và trứng";
    static final String SEASONINGS = "Gia vị";
    static final String DRY_GOODS = "Đồ khô";
    static final String FROZEN = "Đông lạnh";
    static final String OTHER = "Khác";

    static final List<String> ORDER = Collections.unmodifiableList(Arrays.asList(
            VEGETABLES, MEAT_AND_FISH, DAIRY_AND_EGGS, SEASONINGS, DRY_GOODS, FROZEN, OTHER));

    private ShoppingCategoryCatalog() { }

    static boolean contains(String category) { return ORDER.contains(category); }
}
