package com.example.myapplication.domain.catalog;

import com.example.myapplication.domain.model.Recipe;

import java.util.Arrays;
import java.util.List;

/** Local design fixtures used by the debug preview experience. */
public final class PreviewRecipeCatalog {
    private static final List<Recipe> DESIGN_RECIPES = Arrays.asList(
            new Recipe("sample-canh-chua-ca", "Canh chua cá", "recipe_canh_chua", "30 phút", "Dễ",
                    30, "Không giới hạn", Arrays.asList("Món Việt", "Canh & súp", "Món nhanh", "Dễ làm"),
                    Arrays.asList("Cá basa", "Cà chua", "Dứa", "Đậu bắp", "Giá đỗ")),
            new Recipe("sample-ga-chien-nuoc-mam", "Gà chiên nước mắm", "recipe_ga_chien", "25 phút", "Dễ",
                    25, "Không giới hạn", Arrays.asList("Món Việt", "Món Á", "Món chính", "Món nhanh"),
                    Arrays.asList("Thịt gà", "Nước mắm", "Tỏi", "Đường", "Hành khô")),
            new Recipe("sample-mi-y-sot-bo-bam", "Mì Ý sốt bò bằm", "recipe_mi_y", "35 phút", "Vừa",
                    35, "Không giới hạn", Arrays.asList("Món Âu", "Món chính", "Món nhanh"),
                    Arrays.asList("Mì Ý", "Thịt bò", "Cà chua", "Hành tây", "Tỏi")),
            new Recipe("sample-com-rang-dua-bo", "Cơm rang dưa bò", "recipe_com_rang", "20 phút", "Dễ",
                    20, "Không giới hạn", Arrays.asList("Món Việt", "Món Á", "Món chính", "Món nhanh", "Dễ làm"),
                    Arrays.asList("Cơm nguội", "Thịt bò", "Dưa chua", "Trứng gà", "Tỏi")),
            new Recipe("sample-salad-rau-cu", "Salad rau củ", "recipe_salad", "15 phút", "Dễ",
                    15, "Món chay", Arrays.asList("Món chay", "Món Âu", "Món nhanh", "Dễ làm"),
                    Arrays.asList("Xà lách", "Cà chua", "Dưa chuột", "Sốt mè rang", "Bắp ngọt")));
    private static final List<Recipe> MEAL_PLAN_PREVIEWS = Arrays.asList(
            DESIGN_RECIPES.get(0), DESIGN_RECIPES.get(1), DESIGN_RECIPES.get(3));

    private PreviewRecipeCatalog() { }

    public static List<Recipe> designRecipes() { return DESIGN_RECIPES; }
    public static List<Recipe> mealPlanPreviews() { return MEAL_PLAN_PREVIEWS; }
}
