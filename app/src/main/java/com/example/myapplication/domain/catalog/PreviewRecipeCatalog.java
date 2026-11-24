package com.example.myapplication.domain.catalog;

import com.example.myapplication.domain.model.Recipe;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Local preview fixtures; preview IDs and incomplete metadata are not production catalog data. */
public final class PreviewRecipeCatalog {
    private static final String[][] VIETNAMESE_PREVIEWS = {
        {"Phở bò", "soup"},
        {"Phở gà", "soup"},
        {"Bún chả Hà Nội", "main"},
        {"Bún bò Huế", "soup"},
        {"Bún riêu cua", "soup"},
        {"Bún cá", "soup"},
        {"Bún mắm", "soup"},
        {"Bún thịt nướng", "main"},
        {"Bún đậu mắm tôm", "main"},
        {"Bún mọc", "soup"},
        {"Bún thang", "soup"},
        {"Bún ốc", "soup"},
        {"Bún hến", "soup"},
        {"Bún kèn Phú Quốc", "soup"},
        {"Bún cá Châu Đốc", "soup"},
        {"Bún quậy Phú Quốc", "soup"},
        {"Bún nước lèo Sóc Trăng", "soup"},
        {"Bún chả cá Đà Nẵng", "soup"},
        {"Hủ tiếu Nam Vang", "soup"},
        {"Hủ tiếu Mỹ Tho", "soup"},
        {"Mì Quảng", "soup"},
        {"Cao lầu Hội An", "soup"},
        {"Bánh canh cua", "soup"},
        {"Bánh canh giò heo", "soup"},
        {"Bánh canh cá lóc", "soup"},
        {"Miến gà", "soup"},
        {"Miến lươn", "soup"},
        {"Bánh đa cua Hải Phòng", "soup"},
        {"Bánh đa cá rô", "soup"},
        {"Phở cuốn", "snack"},
        {"Phở xào bò", "main"},
        {"Bún bò Nam Bộ", "main"},
        {"Bún chả giò", "main"},
        {"Cháo lòng", "soup"},
        {"Cháo gà", "soup"},
        {"Cháo vịt", "soup"},
        {"Cháo sườn", "soup"},
        {"Cơm tấm sườn bì chả", "main"},
        {"Cơm gà Hội An", "main"},
        {"Cơm gà xối mỡ", "main"},
        {"Cơm hến Huế", "main"},
        {"Cơm niêu", "main"},
        {"Cơm lam Tây Bắc", "main"},
        {"Cơm chiên hải sản", "main"},
        {"Cơm chiên Dương Châu", "main"},
        {"Xôi xéo Hà Nội", "breakfast"},
        {"Xôi mặn", "breakfast"},
        {"Xôi gà", "breakfast"},
        {"Xôi vò", "breakfast"},
        {"Bánh mì thịt", "breakfast"},
        {"Bánh mì chảo", "breakfast"},
        {"Bánh mì ốp la", "breakfast"},
        {"Bánh cuốn Thanh Trì", "breakfast"},
        {"Bánh xèo miền Tây", "snack"},
        {"Bánh xèo miền Trung", "snack"},
        {"Bánh khọt Vũng Tàu", "snack"},
        {"Bánh bèo Huế", "snack"},
        {"Bánh nậm Huế", "snack"},
        {"Bánh bột lọc Huế", "snack"},
        {"Bánh ít trần", "snack"},
        {"Bánh đúc nóng", "snack"},
        {"Bánh giò", "breakfast"},
        {"Bánh chưng", "main"},
        {"Bánh tét", "main"},
        {"Bánh hỏi heo quay", "main"},
        {"Bánh tráng cuốn thịt heo", "main"},
        {"Gỏi cuốn tôm thịt", "snack"},
        {"Chả giò Sài Gòn", "snack"},
        {"Nem nướng Nha Trang", "main"},
        {"Bò lá lốt", "main"},
        {"Bò kho", "main"},
        {"Bò lúc lắc", "main"},
        {"Thịt kho tàu", "main"},
        {"Cá kho tộ", "main"},
        {"Cá lóc nướng trui", "main"},
        {"Chả cá Lã Vọng", "main"},
        {"Vịt om sấu", "main"},
        {"Vịt quay", "main"},
        {"Gà nướng muối ớt", "main"},
        {"Gà luộc lá chanh", "main"},
        {"Gà hấp lá chanh", "main"},
        {"Sườn xào chua ngọt", "main"},
        {"Sườn nướng mật ong", "main"},
        {"Lẩu cá kèo", "soup"},
        {"Lẩu gà lá é", "soup"},
        {"Lẩu mắm miền Tây", "soup"},
        {"Lẩu riêu cua bắp bò", "soup"},
        {"Canh khổ qua nhồi thịt", "soup"},
        {"Canh cua rau đay", "soup"},
        {"Canh chua tôm", "soup"},
        {"Rau muống xào tỏi", "main"},
        {"Đậu hũ sốt cà chua", "main"},
        {"Nấm kho tiêu chay", "vegetarian"},
        {"Bún chay Huế", "vegetarian"},
        {"Phở chay", "vegetarian"},
        {"Chè ba màu", "dessert"},
        {"Chè trôi nước", "dessert"},
        {"Chè bưởi", "dessert"},
        {"Bánh da lợn", "dessert"},
        {"Chuối nếp nướng", "dessert"}
    };

    private static final List<Recipe> DESIGN_RECIPES = createDesignRecipes();
    private static final List<Recipe> MEAL_PLAN_PREVIEWS = Arrays.asList(
            DESIGN_RECIPES.get(0), DESIGN_RECIPES.get(1), DESIGN_RECIPES.get(3));

    private PreviewRecipeCatalog() { }

    public static List<Recipe> designRecipes() { return DESIGN_RECIPES; }
    public static List<Recipe> mealPlanPreviews() { return MEAL_PLAN_PREVIEWS; }

    private static List<Recipe> createDesignRecipes() {
        List<Recipe> recipes = new ArrayList<>(Arrays.asList(
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
                        Arrays.asList("Xà lách", "Cà chua", "Dưa chuột", "Sốt mè rang", "Bắp ngọt"))));

        for (String[] dish : VIETNAMESE_PREVIEWS) recipes.add(createPreviewRecipe(dish[0], dish[1]));
        return Collections.unmodifiableList(recipes);
    }

    private static Recipe createPreviewRecipe(String title, String kind) {
        List<String> categories = new ArrayList<>();
        categories.add("Món Việt");
        String imageName;
        String diet = "Chưa xác định";
        switch (kind) {
            case "soup":
                categories.add("Canh & súp");
                imageName = "category_soup";
                break;
            case "breakfast":
                categories.add("Bữa sáng");
                imageName = "category_breakfast";
                break;
            case "vegetarian":
                categories.add("Món chay");
                imageName = "category_vegetarian";
                diet = "Món chay";
                break;
            case "dessert":
                imageName = "category_vietnamese";
                break;
            case "snack":
                imageName = "category_quick";
                break;
            default:
                categories.add("Món chính");
                imageName = "category_main";
                break;
        }
        String id = "preview-vn-" + slug(title);
        return new Recipe(id, title, imageName, "Chưa cập nhật", "Chưa xác định",
                Recipe.UNKNOWN_DURATION_MINUTES, diet, categories, Collections.emptyList(), 0L, false);
    }

    private static String slug(String value) {
        String normalized = Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replace('đ', 'd');
        return normalized.replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
    }
}
