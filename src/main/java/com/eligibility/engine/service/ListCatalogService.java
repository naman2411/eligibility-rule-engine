package com.eligibility.engine.service;

import java.util.List;

public interface ListCatalogService {
    boolean listExists(String listName);
    List<String> allLists();
    List<String> suggestLists(String wrong);
}