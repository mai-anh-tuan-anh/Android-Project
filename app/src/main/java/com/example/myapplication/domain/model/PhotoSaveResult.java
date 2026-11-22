package com.example.myapplication.domain.model;

/** Result of copying a cooking journal photo into app-private storage. */
public final class PhotoSaveResult {
    public final String filename;
    public final boolean tooLarge;

    public PhotoSaveResult(String filename, boolean tooLarge) {
        this.filename = filename;
        this.tooLarge = tooLarge;
    }
}
