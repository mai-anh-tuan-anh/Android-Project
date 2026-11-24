package com.example.myapplication.data.image;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.myapplication.presentation.main.MainActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class RecipeImageCacheTest {
    private static final String V1 = "recipes/recipe-0001/cover-v1.png";
    private static final String V2 = "recipes/recipe-0001/cover-v2.png";
    private final HostingImageUrlResolver resolver = new HostingImageUrlResolver(HostingImageUrlResolver.PRODUCTION_ORIGIN);

    private Context context;
    private File directory;

    @Before public void createDirectory() {
        context = ApplicationProvider.getApplicationContext();
        directory = new File(context.getCacheDir(), "test-images-" + UUID.randomUUID());
    }

    @After public void deleteDirectory() {
        File[] entries = directory.listFiles();
        if (entries != null) for (File entry : entries) entry.delete();
        directory.delete();
    }

    @Test public void memoryCacheAndRecreatedOfflineLoaderReadSameVersion() throws Exception {
        AtomicInteger fetches = new AtomicInteger();
        byte[] red = png(Color.RED, 24, 24);
        RecipeImageLoader loader = loader(uri -> {
            fetches.incrementAndGet();
            return red;
        }, Runnable::run, 100_000, 10_000);
        Bitmap first = loader.load(V1, 100, 100).get(5, TimeUnit.SECONDS);
        assertSame(first, loader.load(V1, 100, 100).get(5, TimeUnit.SECONDS));
        assertEquals(1, fetches.get());
        assertEquals(red.length, diskEntry(V1).length());
        Bitmap offline = loader(uri -> { throw new IOException("offline"); }, Runnable::run, 100_000, 10_000)
                .load(V1, 100, 100).get(5, TimeUnit.SECONDS);
        assertEquals(Color.RED, offline.getPixel(0, 0));
    }

    @Test public void versionsEvictAtLimitsWithoutServingTheOldImageForTheNewPath() throws Exception {
        byte[] red = png(Color.RED, 24, 24);
        byte[] blue = png(Color.BLUE, 24, 24);
        RecipeImageLoader loader = loader(uri -> uri.getPath().endsWith("v1.png") ? red : blue,
                Runnable::run, Math.max(red.length, blue.length), 2400);
        Bitmap first = loader.load(V1, 100, 100).get(5, TimeUnit.SECONDS);
        assertEquals(Color.RED, first.getPixel(0, 0));
        assertEquals(Color.BLUE, loader.load(V2, 100, 100).get(5, TimeUnit.SECONDS).getPixel(0, 0));
        assertNotEquals(diskEntry(V1), diskEntry(V2));
        assertFalse(diskEntry(V1).exists());
        assertTrue(diskEntry(V2).isFile());
        assertNotSame(first, loader.load(V1, 100, 100).get(5, TimeUnit.SECONDS));
        assertFalse(diskEntry(V2).exists());
    }

    @Test public void corruptDiskEntryIsDiscardedAndCanDownloadAgain() throws Exception {
        byte[] red = png(Color.RED, 24, 24);
        loader(uri -> red, Runnable::run, 100_000, 10_000).load(V1, 100, 100).get(5, TimeUnit.SECONDS);
        try (FileOutputStream cache = new FileOutputStream(diskEntry(V1))) {
            cache.write(new byte[]{0, 1, 2});
        }
        RecipeImageLoader offline = loader(uri -> { throw new IOException("offline"); }, Runnable::run, 100_000, 10_000);
        assertThrows(ExecutionException.class, () -> offline.load(V1, 100, 100).get(5, TimeUnit.SECONDS));
        assertFalse(diskEntry(V1).exists());
        assertEquals(Color.RED, loader(uri -> red, Runnable::run, 100_000, 10_000)
                .load(V1, 100, 100).get(5, TimeUnit.SECONDS).getPixel(0, 0));
    }

    @Test public void sameRequestSharesWorkAndDecodeIsSampled() throws Exception {
        Queue<Runnable> work = new ArrayDeque<>();
        AtomicInteger fetches = new AtomicInteger();
        byte[] png = png(Color.RED, 1024, 512);
        RecipeImageLoader loader = loader(uri -> {
            fetches.incrementAndGet();
            return png;
        }, work::add, 100_000, 1_000_000);
        CompletableFuture<Bitmap> first = loader.load(V1, 128, 128);
        CompletableFuture<Bitmap> second = loader.load(V1, 128, 128);
        assertEquals(1, work.size());
        first.cancel(false);
        work.remove().run();
        Bitmap sampled = second.get(5, TimeUnit.SECONDS);
        assertTrue(sampled.getWidth() < 1024);
        assertTrue(sampled.getHeight() < 512);
        assertEquals(1, fetches.get());
    }

    @Test public void reusedViewDoesNotReceiveBitmapFromItsPreviousPath() throws Exception {
        Queue<Runnable> work = new ArrayDeque<>();
        byte[] red = png(Color.RED, 24, 24);
        byte[] blue = png(Color.BLUE, 24, 24);
        RecipeImageLoader loader = loader(uri -> uri.getPath().endsWith("v1.png") ? red : blue,
                work::add, 100_000, 10_000);
        AtomicReference<ImageView> target = new AtomicReference<>();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                ImageView view = new ImageView(activity);
                activity.addContentView(view, new ViewGroup.LayoutParams(128, 128));
                target.set(view);
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(activity -> loader.bind(target.get(), V1));
            scenario.onActivity(activity -> loader.bind(target.get(), V2));
            assertEquals(2, work.size());
            work.remove().run();
            scenario.onActivity(activity -> assertNull(target.get().getDrawable()));
            work.remove().run();
            scenario.onActivity(activity -> {
                BitmapDrawable image = (BitmapDrawable) target.get().getDrawable();
                assertNotNull(image);
                assertEquals(Color.BLUE, image.getBitmap().getPixel(0, 0));
            });
        }
    }

    private RecipeImageLoader loader(RecipeImageLoader.Source source, Executor worker, long limit, int memory) {
        return new RecipeImageLoader(directory, resolver, source, worker, ContextCompat.getMainExecutor(context),
                limit, memory);
    }

    private File diskEntry(String path) {
        return new File(directory, RecipeImageLoader.cacheKey(resolver.resolve(path)) + ".image");
    }

    private static byte[] png(int color, int width, int height) {
        Bitmap image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        image.eraseColor(color);
        ByteArrayOutputStream encoded = new ByteArrayOutputStream();
        assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, encoded));
        image.recycle();
        return encoded.toByteArray();
    }
}
