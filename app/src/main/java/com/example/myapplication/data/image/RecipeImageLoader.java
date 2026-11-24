package com.example.myapplication.data.image;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.LruCache;
import android.view.View;
import android.widget.ImageView;

import com.example.myapplication.R;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Function;

/** Loads versioned recipe covers or step photos without retaining the displaying View. */
public final class RecipeImageLoader {
    public static final long DISK_LIMIT_BYTES = 32L * 1024 * 1024;
    public static final int RESPONSE_LIMIT_BYTES = 5 * 1024 * 1024;
    public static final String CACHE_DIRECTORY = "recipe-images";

    private static final int MAX_MEMORY_BYTES = 8 * 1024 * 1024;
    private static final int MAX_TARGET_EDGE = 1024;
    private static final int MAX_SOURCE_EDGE = 16_384;
    private static final long MAX_SOURCE_PIXELS = 64_000_000L;
    private static final long MAX_BITMAP_PIXELS = 1_500_000L;

    private final RecipeImageDiskCache disk;
    private final HostingImageUrlResolver resolver;
    private final Source source;
    private final Executor worker;
    private final Executor main;
    private final LruCache<String, Bitmap> memory;
    private final Map<String, CompletableFuture<Bitmap>> pending = new HashMap<>();

    /** The worker serializes disk reads, writes and eviction in this cache directory. */
    public RecipeImageLoader(File directory, HostingImageUrlResolver resolver, Source source,
                             Executor worker, Executor main, long diskLimit, int memoryLimit) {
        disk = new RecipeImageDiskCache(directory, diskLimit);
        this.resolver = resolver;
        this.source = source;
        this.worker = worker;
        this.main = main;
        memory = new LruCache<String, Bitmap>(memoryLimit) {
            @Override protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getAllocationByteCount();
            }
        };
    }

    public static int defaultMemoryLimit() {
        return (int) Math.min(MAX_MEMORY_BYTES, Runtime.getRuntime().maxMemory() / 8);
    }

    public synchronized CompletableFuture<Bitmap> load(String imagePath, int width, int height) {
        URI uri;
        try {
            uri = resolver.resolve(imagePath);
        } catch (IllegalArgumentException error) {
            CompletableFuture<Bitmap> rejected = new CompletableFuture<>();
            rejected.completeExceptionally(error);
            return rejected;
        }
        int targetWidth = targetEdge(width);
        int targetHeight = targetEdge(height);
        String key = uri.toASCIIString() + "|" + targetWidth + "x" + targetHeight;
        Bitmap cached = memory.get(key);
        if (cached != null) return CompletableFuture.completedFuture(cached);

        CompletableFuture<Bitmap> existing = pending.get(key);
        if (existing != null) return existing.thenApply(Function.identity());

        CompletableFuture<Bitmap> request = CompletableFuture.supplyAsync(() -> {
            try {
                Bitmap bitmap = read(uri, targetWidth, targetHeight);
                memory.put(key, bitmap);
                return bitmap;
            } catch (IOException error) {
                throw new CompletionException(error);
            }
        }, worker);
        pending.put(key, request);
        request.whenComplete((bitmap, error) -> removePending(key, request));
        return request.thenApply(Function.identity());
    }

    private synchronized void removePending(String key, CompletableFuture<Bitmap> request) {
        if (pending.get(key) == request) pending.remove(key);
    }

    public void bind(ImageView image, String imagePath) {
        Object token = new Object();
        image.setTag(R.id.recipe_image_request, token);
        image.setImageDrawable(null);
        if (imagePath == null || imagePath.trim().isEmpty()) return;
        if (image.isLaidOut() && image.isAttachedToWindow() && image.getWidth() > 0 && image.getHeight() > 0) {
            display(image, imagePath, token);
            return;
        }
        image.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override public void onLayoutChange(View view, int left, int top, int right, int bottom,
                                                  int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (view.getWidth() <= 0 || view.getHeight() <= 0) return;
                view.removeOnLayoutChangeListener(this);
                if (view.getTag(R.id.recipe_image_request) == token) {
                    display((ImageView) view, imagePath, token);
                }
            }
        });
    }

    private void display(ImageView view, String path, Object token) {
        WeakReference<ImageView> reference = new WeakReference<>(view);
        load(path, view.getWidth(), view.getHeight()).whenCompleteAsync((bitmap, error) -> {
            ImageView image = reference.get();
            if (error == null && image != null && image.isAttachedToWindow()
                    && image.getTag(R.id.recipe_image_request) == token) {
                image.setImageBitmap(bitmap);
            }
        }, main);
    }

    private Bitmap read(URI uri, int width, int height) throws IOException {
        File entry = disk.entry(cacheKey(uri));
        if (entry.isFile()) {
            if (entry.length() > 0 && entry.length() <= RESPONSE_LIMIT_BYTES) {
                try {
                    Bitmap bitmap = decode(options -> BitmapFactory.decodeFile(entry.getPath(), options), width, height);
                    entry.setLastModified(System.currentTimeMillis());
                    return bitmap;
                } catch (IOException corrupt) {
                    // A corrupt entry is never returned to the caller.
                }
            }
            entry.delete();
        }
        byte[] bytes = source.fetch(uri);
        if (bytes.length == 0 || bytes.length > RESPONSE_LIMIT_BYTES) throw new IOException("invalid image size");
        Bitmap bitmap = decode(options -> BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options), width, height);
        try {
            disk.store(entry, bytes);
        } catch (IOException error) {
            // A full or unavailable disk does not prevent display of this validated bitmap.
        }
        return bitmap;
    }

    private static Bitmap decode(Function<BitmapFactory.Options, Bitmap> decoder, int width, int height)
            throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        decoder.apply(options);
        int sourceWidth = options.outWidth;
        int sourceHeight = options.outHeight;
        if (sourceWidth <= 0 || sourceHeight <= 0 || sourceWidth > MAX_SOURCE_EDGE || sourceHeight > MAX_SOURCE_EDGE
                || (long) sourceWidth * sourceHeight > MAX_SOURCE_PIXELS) {
            throw new IOException("invalid image dimensions");
        }
        int sample = 1;
        while ((sourceWidth / (sample * 2) >= width && sourceHeight / (sample * 2) >= height)
                || (long) (sourceWidth / sample) * (sourceHeight / sample) > MAX_BITMAP_PIXELS) {
            sample *= 2;
        }
        options.inJustDecodeBounds = false;
        options.inSampleSize = sample;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = decoder.apply(options);
        if (bitmap == null) throw new IOException("image cannot be decoded");
        return bitmap;
    }

    private static int targetEdge(int pixels) {
        int clamped = Math.min(MAX_TARGET_EDGE, Math.max(128, pixels));
        return Integer.highestOneBit(clamped - 1) << 1;
    }

    public static String cacheKey(URI uri) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(uri.toASCIIString().getBytes(StandardCharsets.UTF_8));
            StringBuilder key = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                key.append(Character.forDigit((value >>> 4) & 15, 16));
                key.append(Character.forDigit(value & 15, 16));
            }
            return key.toString();
        } catch (NoSuchAlgorithmException missing) {
            throw new AssertionError(missing);
        }
    }

    @FunctionalInterface
    public interface Source { byte[] fetch(URI uri) throws IOException; }
}
