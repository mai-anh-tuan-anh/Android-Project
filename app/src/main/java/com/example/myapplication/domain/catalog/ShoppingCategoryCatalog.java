package com.example.myapplication.domain.catalog;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** User-selected aisle groups for manually managed shopping items. */
public final class ShoppingCategoryCatalog {
    public static final String VEGETABLES = "Rau củ";
    public static final String MEAT_AND_FISH = "Thịt cá";
    public static final String DAIRY_AND_EGGS = "Sữa và trứng";
    public static final String SEASONINGS = "Gia vị";
    public static final String DRY_GOODS = "Đồ khô";
    public static final String FROZEN = "Đông lạnh";
    public static final String OTHER = "Khác";

    public static final List<String> ORDER = Collections.unmodifiableList(Arrays.asList(
            VEGETABLES, MEAT_AND_FISH, DAIRY_AND_EGGS, SEASONINGS, DRY_GOODS, FROZEN, OTHER));

    private ShoppingCategoryCatalog() { }

    public static boolean contains(String category) { return ORDER.contains(category); }
}
