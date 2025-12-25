package com.eligibility.engine.service;

import com.eligibility.engine.model.AttributeDef;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SchemaServiceImpl implements UserSchemaService, ListCatalogService {

    private volatile Map<String, AttributeDef> attributes = Collections.emptyMap();
    private volatile Set<String> lists = Collections.emptySet();

    public void applySnapshot(Map<String, AttributeDef> newAttributes, Set<String> newLists) {

        this.attributes = Collections.unmodifiableMap(new HashMap<>(newAttributes));
        this.lists = Collections.unmodifiableSet(new HashSet<>(newLists));

        System.out.println("--- [CACHE] Snapshot applied. " +
                attributes.size() + " attributes, " +
                lists.size() + " lists available. ---");
    }


    @Override
    public AttributeDef getAttribute(String name) {
        return attributes.get(name.toLowerCase());
    }

    @Override
    public List<String> allAttributeNames() {
        return new ArrayList<>(attributes.keySet());
    }

    @Override
    public List<String> suggestAttributes(String wrong) {
        List<String> suggestions = new ArrayList<>();
        String input = wrong.toLowerCase();

        if (input.equals("active")) {
            return List.of("status", "is_verified");
        }

        for (String key : attributes.keySet()) {
            if (calculateLevenshtein(input, key) <= 2) {
                suggestions.add(key);
            }
        }
        return suggestions;
    }


    @Override
    public boolean listExists(String listName) {
        return lists.contains(listName.toLowerCase());
    }

    @Override
    public List<String> allLists() {
        return new ArrayList<>(lists);
    }

    @Override
    public List<String> suggestLists(String wrong) {
        List<String> suggestions = new ArrayList<>();
        String input = wrong.toLowerCase();

        for (String list : lists) {
            if (calculateLevenshtein(input, list) <= 2) {
                suggestions.add(list);
            }
        }
        return suggestions;
    }

    private int calculateLevenshtein(String x, String y) {
        int[][] dp = new int[x.length() + 1][y.length() + 1];

        for (int i = 0; i <= x.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= y.length(); j++) dp[0][j] = j;

        for (int i = 1; i <= x.length(); i++) {
            for (int j = 1; j <= y.length(); j++) {
                int cost = (x.charAt(i - 1) == y.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                );
            }
        }
        return dp[x.length()][y.length()];
    }
}