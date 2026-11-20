package com.example.myapplication;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class MainActivityStateTest {
    @Test
    public void mealPlanPickerDraftSurvivesActivityRecreation() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                try {
                    setField(activity, "screen", MainActivity.Screen.MEAL_PLAN_PICKER);
                    @SuppressWarnings("unchecked")
                    List<String> selected = (List<String>) getField(activity, "selectedPlanRecipeIds");
                    selected.clear();
                    selected.addAll(Arrays.asList("sample-ga-chien-nuoc-mam", "sample-com-rang-dua-bo"));
                    setField(activity, "planPickerServings", 4);
                    setField(activity, "planPickerServingsByRecipeId", servingsMap());
                    setField(activity, "planPickerOriginalServingsByRecipeId", servingsMap());
                    setField(activity, "planSearchQuery", "com");
                    Method render = MainActivity.class.getDeclaredMethod("render");
                    render.setAccessible(true);
                    render.invoke(activity);
                } catch (ReflectiveOperationException e) {
                    throw new AssertionError(e);
                }
            });

            scenario.recreate();

            scenario.onActivity(activity -> {
                try {
                    assertEquals(MainActivity.Screen.MEAL_PLAN_PICKER, getField(activity, "screen"));
                    assertEquals(new ArrayList<>(Arrays.asList(
                            "sample-ga-chien-nuoc-mam", "sample-com-rang-dua-bo")),
                            getField(activity, "selectedPlanRecipeIds"));
                    assertEquals(4, getField(activity, "planPickerServings"));
                    assertEquals(servingsMap(), getField(activity, "planPickerServingsByRecipeId"));
                    assertEquals("com", getField(activity, "planSearchQuery"));
                    assertEquals("com", ((android.widget.EditText) getField(activity,
                            "planPickerSearchInput")).getText().toString());
                } catch (ReflectiveOperationException e) {
                    throw new AssertionError(e);
                }
            });
        }
    }

    @Test
    public void pickerServingsApplyOnlyToEachNewlySelectedRecipe() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                try {
                    setField(activity, "planPickerServings", 7);
                    Method openPicker = MainActivity.class.getDeclaredMethod("openMealPlanPicker");
                    openPicker.setAccessible(true);
                    openPicker.invoke(activity);
                    assertEquals(1, getField(activity, "planPickerServings"));

                    setField(activity, "screen", MainActivity.Screen.MEAL_PLAN_PICKER);
                    setField(activity, "planPickerServings", 1);
                    setField(activity, "planPickerServingsByRecipeId", new HashMap<String, Integer>());
                    Map<String, Integer> originalServings = new HashMap<>();
                    originalServings.put(MainActivity.DESIGN_RECIPES.get(2).id, 2);
                    setField(activity, "planPickerOriginalServingsByRecipeId", originalServings);
                    @SuppressWarnings("unchecked")
                    List<String> selected = (List<String>) getField(activity, "selectedPlanRecipeIds");
                    selected.clear();
                    selected.add(MainActivity.DESIGN_RECIPES.get(2).id);
                    @SuppressWarnings("unchecked")
                    Map<String, Integer> servings = (Map<String, Integer>) getField(
                            activity, "planPickerServingsByRecipeId");
                    servings.put(MainActivity.DESIGN_RECIPES.get(2).id, 2);
                    Method toggle = MainActivity.class.getDeclaredMethod(
                            "togglePlanPickerRecipe", MainActivity.RecipePreview.class);
                    toggle.setAccessible(true);
                    toggle.invoke(activity, MainActivity.DESIGN_RECIPES.get(2));
                    assertEquals(Integer.valueOf(2), originalServings.get(MainActivity.DESIGN_RECIPES.get(2).id));
                    toggle.invoke(activity, MainActivity.DESIGN_RECIPES.get(2));
                    assertEquals(Integer.valueOf(2), servings.get(MainActivity.DESIGN_RECIPES.get(2).id));

                    toggle.invoke(activity, MainActivity.DESIGN_RECIPES.get(0));

                    setField(activity, "planPickerServings", 3);
                    toggle.invoke(activity, MainActivity.DESIGN_RECIPES.get(1));

                    assertEquals(Integer.valueOf(1), servings.get(MainActivity.DESIGN_RECIPES.get(0).id));
                    assertEquals(Integer.valueOf(3), servings.get(MainActivity.DESIGN_RECIPES.get(1).id));

                    setField(activity, "planPickerServings", 8);
                    assertEquals(Integer.valueOf(1), servings.get(MainActivity.DESIGN_RECIPES.get(0).id));
                    assertEquals(Integer.valueOf(3), servings.get(MainActivity.DESIGN_RECIPES.get(1).id));
                } catch (ReflectiveOperationException e) {
                    throw new AssertionError(e);
                }
            });
        }
    }

    private static Object getField(MainActivity activity, String name) throws ReflectiveOperationException {
        Field field = MainActivity.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(activity);
    }

    private static Map<String, Integer> servingsMap() {
        Map<String, Integer> servings = new HashMap<>();
        servings.put("sample-ga-chien-nuoc-mam", 2);
        servings.put("sample-com-rang-dua-bo", 4);
        return servings;
    }

    private static void setField(MainActivity activity, String name, Object value)
            throws ReflectiveOperationException {
        Field field = MainActivity.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(activity, value);
    }
}
