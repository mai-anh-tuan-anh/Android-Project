package com.example.myapplication.data.remote;

import java.io.IOException;

/** Synchronous fetch of the catalog snapshot. Call on a background executor. */
public interface CatalogRemoteDataSource {
    String fetchSnapshot() throws IOException;
}
