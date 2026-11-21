package com.example.myapplication;

import android.app.Application;
import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class DiscoverSearchViewModelTest {

    private DiscoverSearchViewModel viewModel;
    private List<MainActivity.RecipePreview> sampleRecipes;

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Application app = (Application) context.getApplicationContext();
        viewModel = new DiscoverSearchViewModel(app);
        viewModel.clearRecentSearches();
        viewModel.resetActiveFilter();
        viewModel.setSearchQuery("");

        sampleRecipes = Arrays.asList(
                new MainActivity.RecipePreview("sample-canh-chua-ca", "Canh chua cá", "recipe_canh_chua", "30 phút", "Dễ",
                        30, "Không giới hạn", Arrays.asList("Món Việt", "Món nhanh", "Dễ làm"),
                    Arrays.asList("Cá basa", "Cà chua", "Dứa", "Đậu bắp", "Giá đỗ"), 0L, true),
                new MainActivity.RecipePreview("sample-ga-chien-nuoc-mam", "Gà chiên nước mắm", "recipe_ga_chien", "25 phút", "Dễ",
                        25, "Không giới hạn", Arrays.asList("Món Việt", "Món Á", "Món nhanh"),
                    Arrays.asList("Thịt gà", "Nước mắm", "Tỏi", "Đường", "Hành khô"), 0L, true),
                new MainActivity.RecipePreview("sample-mi-y-sot-bo-bam", "Mì Ý sốt bò bằm", "recipe_mi_y", "35 phút", "Vừa",
                        35, "Không giới hạn", Arrays.asList("Món Âu", "Món nhanh"),
                    Arrays.asList("Mì Ý", "Thịt bò", "Cà chua", "Hành tây", "Tỏi"), 0L, true),
                new MainActivity.RecipePreview("sample-com-rang-dua-bo", "Cơm rang dưa bò", "recipe_com_rang", "20 phút", "Dễ",
                        20, "Không giới hạn", Arrays.asList("Món Việt", "Món Á", "Món nhanh", "Dễ làm"),
                    Arrays.asList("Cơm nguội", "Thịt bò", "Dưa chua", "Trứng gà", "Tỏi"), 0L, true),
                new MainActivity.RecipePreview("sample-salad-rau-cu", "Salad rau củ", "recipe_salad", "15 phút", "Dễ",
                        15, "Món chay", Arrays.asList("Món chay", "Món Âu", "Món nhanh", "Dễ làm", "Bữa sáng"),
                    Arrays.asList("Xà lách", "Cà chua", "Dưa chuột", "Sốt mè rang", "Bắp ngọt"), 0L, true)
        );
    }

    @Test
    public void vietnameseNormalizationStripsDiacritics() {
        assertEquals("pho bo", DiscoverSearchViewModel.normalize("Phở bò"));
        assertEquals("canh chua ca", DiscoverSearchViewModel.normalize("Canh chua cá"));
        assertEquals("mon chay", DiscoverSearchViewModel.normalize("Món chay"));
        assertEquals("thit ga", DiscoverSearchViewModel.normalize("Thịt gà"));
    }

    @Test
    public void searchByQueryMatchesTitleAndIngredientsWithoutDiacritics() {
        viewModel.setSearchQuery("bo");
        List<MainActivity.RecipePreview> results = viewModel.filterRecipes(sampleRecipes);
        assertEquals(2, results.size());

        viewModel.setSearchQuery("ca chua");
        results = viewModel.filterRecipes(sampleRecipes);
        assertEquals(3, results.size());
    }

    @Test
    public void filterByDurationAndDifficulty() {
        viewModel.beginFilterDraft();
        viewModel.setDraftDurationRange(30);
        viewModel.setDraftDifficulty("Dễ");
        viewModel.applyDraftFilter();

        List<MainActivity.RecipePreview> results = viewModel.filterRecipes(sampleRecipes);
        assertEquals(4, results.size());
    }

    @Test
    public void filterByVegetarianDiet() {
        viewModel.beginFilterDraft();
        viewModel.setDraftDiet("Món chay");
        viewModel.applyDraftFilter();

        List<MainActivity.RecipePreview> results = viewModel.filterRecipes(sampleRecipes);
        assertEquals(1, results.size());
        assertEquals("sample-salad-rau-cu", results.get(0).id);
    }

    @Test
    public void filterByIncludedAndExcludedIngredients() {
        viewModel.beginFilterDraft();
        viewModel.setDraftIncludeIngredient("Thịt bò");
        viewModel.setDraftExcludeIngredient("Dưa chua");
        viewModel.applyDraftFilter();

        List<MainActivity.RecipePreview> results = viewModel.filterRecipes(sampleRecipes);
        assertEquals(1, results.size());
        assertEquals("sample-mi-y-sot-bo-bam", results.get(0).id);
    }

    @Test
    public void recentSearchesManagement() {
        viewModel.submitSearch("Phở bò");
        viewModel.submitSearch("Bún chả");
        viewModel.submitSearch("Canh chua");

        List<String> recent = viewModel.getRecentSearches();
        assertEquals(3, recent.size());
        assertEquals("Canh chua", recent.get(0));
        assertEquals("Bún chả", recent.get(1));
        assertEquals("Phở bò", recent.get(2));

        viewModel.removeRecentSearch("Bún chả");
        assertEquals(2, viewModel.getRecentSearches().size());
        assertFalse(viewModel.getRecentSearches().contains("Bún chả"));
    }

    @Test
    public void activeFilterBadgesManagement() {
        viewModel.beginFilterDraft();
        viewModel.setDraftDurationRange(30);
        viewModel.setDraftDifficulty("Dễ");
        viewModel.setDraftDiet("Món chay");
        viewModel.setDraftIncludeIngredient("Cà chua");
        viewModel.setDraftExcludeIngredient("Tỏi");
        viewModel.applyDraftFilter();

        List<String> badges = viewModel.getActiveFilterBadges();
        assertEquals(5, badges.size());
        assertTrue(badges.contains("15–30 phút"));
        assertTrue(badges.contains("Dễ"));
        assertTrue(badges.contains("Món chay"));
        assertTrue(badges.contains("+Cà chua"));
        assertTrue(badges.contains("-Tỏi"));

        viewModel.removeFilterBadge("15–30 phút");
        assertNull(viewModel.getDurationRange());
        assertEquals(4, viewModel.getActiveFilterBadges().size());
    }

    @Test
    public void durationFiltersUseTheThreeSpecifiedRanges() {
        viewModel.beginFilterDraft();
        viewModel.setDraftDurationRange(15);
        viewModel.applyDraftFilter();
        assertEquals(0, viewModel.filterRecipes(sampleRecipes).size());

        viewModel.beginFilterDraft();
        viewModel.setDraftDurationRange(30);
        viewModel.applyDraftFilter();
        assertEquals(4, viewModel.filterRecipes(sampleRecipes).size());

        viewModel.beginFilterDraft();
        viewModel.setDraftDurationRange(-1);
        viewModel.applyDraftFilter();
        assertEquals(1, viewModel.filterRecipes(sampleRecipes).size());
    }

    @Test
    public void ingredientSuggestionsRequireAnOverlapAndRankByMissingCount() {
        viewModel.toggleCookingIngredient("Cà chua");
        viewModel.toggleCookingIngredient("Dứa");
        List<MainActivity.RecipePreview> results = viewModel.suggestRecipes(sampleRecipes);
        assertFalse(results.isEmpty());
        assertEquals("sample-canh-chua-ca", results.get(0).id);
        assertEquals(2, viewModel.availableIngredientCount(results.get(0)));
    }

    @Test
    public void completeRecipesRankAheadWhenAllTheirIngredientsAreSelected() {
        for (String ingredient : sampleRecipes.get(4).ingredients) {
            viewModel.toggleCookingIngredient(ingredient);
        }
        List<MainActivity.RecipePreview> results = viewModel.suggestRecipes(sampleRecipes);
        assertFalse(results.isEmpty());
        assertEquals("sample-salad-rau-cu", results.get(0).id);
        assertTrue(viewModel.missingIngredients(results.get(0)).isEmpty());
        assertEquals(viewModel.totalIngredientCount(results.get(0)),
                viewModel.availableIngredientCount(results.get(0)));
    }

    @Test
    public void oldRecentSearchesExpireAfterOneHour() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.getSharedPreferences("discover_search_prefs", Context.MODE_PRIVATE).edit()
                .putString("recent_searches", (System.currentTimeMillis() - 2L * 60L * 60L * 1000L)
                        + "\told query")
                .commit();
        DiscoverSearchViewModel restored = new DiscoverSearchViewModel(
                (Application) context.getApplicationContext());
        assertTrue(restored.getRecentSearches().isEmpty());
    }
}
