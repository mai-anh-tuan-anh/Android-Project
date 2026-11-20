package com.example.myapplication;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class MainActivityStateTest {
    @Test
    public void mealPlanPickerDraftSurvivesActivityRecreation() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                try {
                    setField(activity, "screen", MainActivity.Screen.MEAL_PLAN_PICKER);
                    MealPlanViewModel viewModel = (MealPlanViewModel) getField(activity, "mealPlanViewModel");
                    @SuppressWarnings("unchecked")
                    List<String> selected = (List<String>) getField(viewModel, "selectedRecipeIds");
                    selected.clear();
                    selected.addAll(Arrays.asList("sample-ga-chien-nuoc-mam", "sample-com-rang-dua-bo"));
                    setField(viewModel, "pickerServings", 4);
                    setField(viewModel, "searchQuery", "com");
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
                    MealPlanViewModel viewModel = (MealPlanViewModel) getField(activity, "mealPlanViewModel");
                    assertEquals(new ArrayList<>(Arrays.asList(
                            "sample-ga-chien-nuoc-mam", "sample-com-rang-dua-bo")),
                            getField(viewModel, "selectedRecipeIds"));
                    assertEquals(4, getField(viewModel, "pickerServings"));
                    assertEquals("com", getField(viewModel, "searchQuery"));
                    assertEquals("com", ((android.widget.EditText) getField(activity,
                            "planPickerSearchInput")).getText().toString());
                } catch (ReflectiveOperationException e) {
                    throw new AssertionError(e);
                }
            });
        }
    }

    @Test
    public void pickerServingsApplyToNewRecipesAndKeepExistingRecipeServings() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                try {
                    MealPlanViewModel viewModel = (MealPlanViewModel) getField(activity, "mealPlanViewModel");
                    setField(viewModel, "pickerServings", 7);
                    Method openPicker = MainActivity.class.getDeclaredMethod("openMealPlanPicker");
                    openPicker.setAccessible(true);
                    openPicker.invoke(activity);
                    assertEquals(1, viewModel.getPickerServings());

                    PersonalDataDbHelper.PlannedRecipe existing = new PersonalDataDbHelper.PlannedRecipe(
                            MainActivity.DESIGN_RECIPES.get(2), 3);
                    assertEquals(2, MealPlanViewModel.servingsForSelection(2, null));
                    assertEquals(3, MealPlanViewModel.servingsForSelection(2, existing));
                    assertEquals(5, MealPlanViewModel.servingsForSelection(5, null));
                    assertEquals(3, MealPlanViewModel.servingsForSelection(5, existing));
                } catch (ReflectiveOperationException e) {
                    throw new AssertionError(e);
                }
            });
        }
    }

    private static Object getField(Object target, String name) throws ReflectiveOperationException {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void setField(Object target, String name, Object value)
            throws ReflectiveOperationException {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
