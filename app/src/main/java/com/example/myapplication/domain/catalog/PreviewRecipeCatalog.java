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
        {"Canh chua cá lóc", "soup"},
        {"Canh chua tôm", "soup"},
        {"Canh cua rau đay", "soup"},
        {"Canh mồng tơi nấu tôm", "soup"},
        {"Canh bí xanh nấu sườn", "soup"},
        {"Canh bí đỏ nấu tôm", "soup"},
        {"Canh cải nấu thịt bằm", "soup"},
        {"Canh cải cá rô", "soup"},
        {"Canh rau ngót nấu thịt bằm", "soup"},
        {"Canh khoai mỡ nấu tôm", "soup"},
        {"Canh khổ qua nhồi thịt", "soup"},
        {"Canh bầu nấu tôm", "soup"},
        {"Canh bí đao nấu thịt bằm", "soup"},
        {"Canh cà chua trứng", "soup"},
        {"Canh cá nấu dưa chua", "soup"},
        {"Canh sườn nấu củ quả", "soup"},
        {"Canh măng sườn", "soup"},
        {"Canh riêu cua", "soup"},
        {"Canh gà lá giang", "soup"},
        {"Canh gà nấu nấm", "soup"},
        {"Canh cá nấu ngót", "soup"},
        {"Canh khoai tây cà rốt nấu sườn", "soup"},
        {"Canh rau tập tàng", "soup"},
        {"Canh mọc nấu nấm", "soup"},
        {"Canh rau củ chay", "vegetarian"},
        {"Canh nấm đậu hũ chay", "vegetarian"},
        {"Canh cà tím nấu đậu hũ", "soup"},
        {"Canh rong biển thịt bằm", "soup"},
        {"Canh rau dền nấu tôm", "soup"},
        {"Canh bắp cải cuộn thịt", "soup"},
        {"Thịt kho trứng", "main"},
        {"Thịt kho tiêu", "main"},
        {"Thịt kho tàu", "main"},
        {"Thịt ba chỉ rang cháy cạnh", "main"},
        {"Thịt rang mắm tép", "main"},
        {"Sườn rim mặn ngọt", "main"},
        {"Sườn non kho thơm", "main"},
        {"Sườn kho tiêu", "main"},
        {"Gà kho gừng", "main"},
        {"Gà kho sả", "main"},
        {"Gà kho nghệ", "main"},
        {"Gà kho nấm", "main"},
        {"Cá kho tộ", "main"},
        {"Cá kho riềng", "main"},
        {"Cá nục kho cà", "main"},
        {"Cá basa kho tiêu", "main"},
        {"Cá thu kho thơm", "main"},
        {"Tôm rim thịt ba chỉ", "main"},
        {"Tôm rim nước mắm", "main"},
        {"Tép rang khế", "main"},
        {"Trứng kho thịt", "main"},
        {"Đậu hũ kho nấm chay", "vegetarian"},
        {"Cà tím kho đậu hũ", "main"},
        {"Thịt kho măng", "main"},
        {"Thịt kho củ cải", "main"},
        {"Ba chỉ kho dừa", "main"},
        {"Cá cơm rim lạc", "main"},
        {"Cá bống kho tiêu", "main"},
        {"Sườn non rim nước dừa", "main"},
        {"Cá lóc kho tộ", "main"},
        {"Bò xào hành tây", "main"},
        {"Bò xào cần tây", "main"},
        {"Bò xào bông cải", "main"},
        {"Bò xào bông thiên lý", "main"},
        {"Thịt heo xào sả ớt", "main"},
        {"Thịt heo xào chua ngọt", "main"},
        {"Gà xào sả ớt", "main"},
        {"Gà xào nấm", "main"},
        {"Tôm xào bông cải", "main"},
        {"Mực xào cần tỏi", "main"},
        {"Mực xào sa tế", "main"},
        {"Rau muống xào tỏi", "main"},
        {"Cải thìa xào nấm", "main"},
        {"Bông cải xanh xào tỏi", "main"},
        {"Đậu que xào thịt", "main"},
        {"Su su xào tỏi", "main"},
        {"Bắp cải xào trứng", "main"},
        {"Giá xào hẹ", "main"},
        {"Mướp xào tôm", "main"},
        {"Cà tím xào thịt bằm", "main"},
        {"Đậu hũ xào rau củ", "main"},
        {"Nấm xào thập cẩm", "main"},
        {"Rau lang xào tỏi", "main"},
        {"Cải ngồng xào tỏi", "main"},
        {"Cơm chiên trứng", "main"},
        {"Miến xào lòng gà", "main"},
        {"Thịt bò xào rau muống", "main"},
        {"Gà xào ớt chuông", "main"},
        {"Tôm xào trứng", "main"},
        {"Đậu hũ non sốt nấm", "main"},
        {"Cá diêu hồng hấp xì dầu", "main"},
        {"Cá hấp gừng hành", "main"},
        {"Gà hấp hành", "main"},
        {"Gà luộc lá chanh", "main"},
        {"Gà nướng muối ớt", "main"},
        {"Sườn nướng mật ong", "main"},
        {"Thịt luộc chấm mắm nêm", "main"},
        {"Cá chiên sốt cà", "main"},
        {"Cá chiên sả ớt", "main"},
        {"Rau củ luộc chấm kho quẹt", "main"}
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
