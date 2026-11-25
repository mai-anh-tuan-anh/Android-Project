package com.example.myapplication.data.repository;

import android.content.Context;
import android.os.Looper;

import com.example.myapplication.data.local.database.RecipeCatalogDbHelper;
import com.example.myapplication.data.local.database.RecipeCatalogDbHelper.ApplyResult;
import com.example.myapplication.data.mapper.CatalogSnapshotMapper;
import com.example.myapplication.domain.repository.RecipeCatalogRepository;

/** Local-only catalog. Snapshot ingestion is exposed to the data layer, not feature callers. */
public final class LocalRecipeCatalogRepository implements RecipeCatalogRepository {
    private final RecipeCatalogDbHelper database;

    public LocalRecipeCatalogRepository(Context context) {
        this(new RecipeCatalogDbHelper(context.getApplicationContext()));
    }

    public LocalRecipeCatalogRepository(RecipeCatalogDbHelper database) {
        this.database = database;
    }

    @Override
    public Catalog readCatalog() {
        requireIoThread();
        return database.readCatalog();
    }

    @Override
    public Long readRevision() {
        requireIoThread();
        return database.readRevision();
    }

    @Override
    public Lookup findById(String recipeId) {
        requireIoThread();
        if (recipeId == null || recipeId.trim().isEmpty()) {
            throw new IllegalArgumentException("recipeId must not be blank");
        }
        return database.findById(recipeId);
    }

    public ApplyResult applySnapshot(String json) {
        requireIoThread();
        return database.replaceSnapshot(CatalogSnapshotMapper.parse(json));
    }

    private static void requireIoThread() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new IllegalStateException("read or update the catalog on an I/O thread");
        }
    }
}
