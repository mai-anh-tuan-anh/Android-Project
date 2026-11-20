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
import static org.junit.Assert.assertTrue;

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
                    setField(activity, "planPickerServingsChanged", true);
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
                    assertTrue((Boolean) getField(activity, "planPickerServingsChanged"));
                    assertEquals("com", getField(activity, "planSearchQuery"));
                    assertEquals("com", ((android.widget.EditText) getField(activity,
                            "planPickerSearchInput")).getText().toString());
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

    private static void setField(MainActivity activity, String name, Object value)
            throws ReflectiveOperationException {
        Field field = MainActivity.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(activity, value);
    }
}
