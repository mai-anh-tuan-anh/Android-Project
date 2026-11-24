package com.example.myapplication.data.repository;

import com.example.myapplication.data.local.database.RecipeCatalogDbHelper.ApplyResult;

import org.junit.Test;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class CatalogRefreshServiceTest {
    @Test
    public void overlappingRequestsUseOneFetchAndApplyDespiteCallerCancellation() throws Exception {
        Queue<Runnable> worker = new ArrayDeque<>();
        AtomicInteger fetches = new AtomicInteger();
        AtomicInteger writes = new AtomicInteger();
        CatalogRefreshService service = new CatalogRefreshService(() -> {
            fetches.incrementAndGet();
            return "snapshot";
        }, json -> {
            assertEquals("snapshot", json);
            writes.incrementAndGet();
            return ApplyResult.APPLIED;
        }, worker::add);

        CompletableFuture<ApplyResult> first = service.refresh();
        CompletableFuture<ApplyResult> second = service.refresh();
        assertEquals(1, worker.size());
        assertEquals(0, fetches.get());
        assertTrue(first.cancel(false));
        worker.remove().run();

        assertEquals(ApplyResult.APPLIED, second.get());
        assertEquals(1, fetches.get());
        assertEquals(1, writes.get());
    }

    @Test
    public void doesNotWriteWhenFetchFailsAndAllowsRetry() throws Exception {
        AtomicInteger attempts = new AtomicInteger();
        AtomicInteger writes = new AtomicInteger();
        IOException offline = new IOException("offline");
        CatalogRefreshService service = new CatalogRefreshService(() -> {
            if (attempts.incrementAndGet() == 1) throw offline;
            return "snapshot";
        }, json -> {
            writes.incrementAndGet();
            return ApplyResult.UNCHANGED;
        }, Runnable::run);

        ExecutionException error = assertThrows(ExecutionException.class, () -> service.refresh().get());
        assertSame(offline, error.getCause());
        assertEquals(0, writes.get());
        assertEquals(ApplyResult.UNCHANGED, service.refresh().get());
        assertEquals(1, writes.get());
    }
}
