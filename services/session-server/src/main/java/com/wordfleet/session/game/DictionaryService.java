package com.wordfleet.session.game;

import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

@Service
public class DictionaryService {
    private final Set<String> words = new HashSet<>();
    private final Map<Integer, List<String>> substringsByTier = new HashMap<>();

    @PostConstruct
    public void init() {
        loadWords();
        buildSubstringTiers();
    }

    public boolean isKnownWord(String normalizedWord) {
        return words.contains(normalizedWord);
    }

    public String pickSubstringForTier(int tier, Random random) {
        List<String> options = substringsByTier.getOrDefault(tier, List.of());
        if (options.isEmpty()) {
            options = substringsByTier.getOrDefault(1, List.of("ar"));
        }
        return options.get(random.nextInt(options.size()));
    }

    public int occurrences(String word, String substring) {
        if (substring.isBlank()) {
            return 0;
        }
        int count = 0;
        int idx = 0;
        while (idx <= word.length() - substring.length()) {
            int found = word.indexOf(substring, idx);
            if (found < 0) {
                break;
            }
            count++;
            idx = found + 1;
        }
        return count;
    }

    private void loadWords() {
        try {
            ClassPathResource resource = new ClassPathResource("dictionary.txt");
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String word = normalize(line);
                    if (word.length() >= 4) {
                        words.add(word);
                    }
                }
            }
        } catch (Exception ignored) {
            Collections.addAll(words,
                    "planet", "planted", "charter", "harbor", "reading", "reader", "rocket", "cradle",
                    "triangle", "string", "strain", "drainer", "orange", "operator", "gateway", "memory",
                    "render", "cluster", "kubernetes", "container", "garden", "mission", "session", "java",
                    "spring", "redis", "agones", "server", "client", "leader", "board", "winner", "timers");
        }
    }

    private void buildSubstringTiers() {
        Map<String, Integer> counts = new HashMap<>();
        for (String word : words) {
            Set<String> unique = new HashSet<>();
            for (int len = 2; len <= 3; len++) {
                for (int i = 0; i <= word.length() - len; i++) {
                    unique.add(word.substring(i, i + len));
                }
            }
            for (String sub : unique) {
                counts.merge(sub, 1, Integer::sum);
            }
        }

        List<String> t1 = new ArrayList<>();
        List<String> t2 = new ArrayList<>();
        List<String> t3 = new ArrayList<>();
        List<String> t4 = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            String sub = entry.getKey();
            int c = entry.getValue();
            if (c >= 5000) {
                t1.add(sub);
            } else if (c >= 1000) {
                t2.add(sub);
            } else if (c >= 200) {
                t3.add(sub);
            } else {
                t4.add(sub);
            }
        }

        if (t1.isEmpty()) {
            for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                if (entry.getValue() >= 50) {
                    t1.add(entry.getKey());
                }
            }
        }
        if (t2.isEmpty()) {
            t2.addAll(t1);
        }
        if (t3.isEmpty()) {
            t3.addAll(t2);
        }
        if (t4.isEmpty()) {
            t4.addAll(t3);
        }

        substringsByTier.put(1, t1.isEmpty() ? List.of("ar", "re", "in") : t1);
        substringsByTier.put(2, t2.isEmpty() ? substringsByTier.get(1) : t2);
        substringsByTier.put(3, t3.isEmpty() ? substringsByTier.get(2) : t3);
        substringsByTier.put(4, t4.isEmpty() ? substringsByTier.get(3) : t4);
    }

    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.toLowerCase().replaceAll("[^a-z]", "");
    }
}
