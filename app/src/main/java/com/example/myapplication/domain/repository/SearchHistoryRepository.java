package com.example.myapplication.domain.repository;

/** Storage boundary for recent recipe-search history. */
public interface SearchHistoryRepository {
    String readSerializedSearchHistory();
    void writeSerializedSearchHistory(String value);
}
