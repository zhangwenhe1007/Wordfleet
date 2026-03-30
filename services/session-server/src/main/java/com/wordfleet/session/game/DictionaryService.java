package com.wordfleet.session.game;

import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

@Service
public class DictionaryService {
    private static final Logger log = LoggerFactory.getLogger(DictionaryService.class);
    private static final List<String> DEFAULT_TWO_LETTER_SUBSTRINGS = List.of("ar", "re", "in", "er", "on", "an");
    private static final List<String> DEFAULT_THREE_LETTER_SUBSTRINGS = List.of("ing", "ion", "ter", "ers");
    private static final List<Path> SYSTEM_DICTIONARY_PATHS = List.of(
            Path.of("/usr/share/dict/words"),
            Path.of("/usr/share/dict/american-english"),
            Path.of("/usr/share/dict/british-english"),
            Path.of("/usr/share/dict/web2"),
            Path.of("/usr/share/hunspell/en_US.dic"),
            Path.of("/usr/share/hunspell/en_GB.dic"),
            Path.of("/usr/share/myspell/dicts/en_US.dic"),
            Path.of("/usr/share/myspell/dicts/en_GB.dic")
    );

    private final Set<String> words = new HashSet<>();
    private final Map<Integer, List<String>> twoLetterSubstringsByTier = new HashMap<>();
    private final Map<Integer, List<String>> threeLetterSubstringsByTier = new HashMap<>();

    @PostConstruct
    public void init() {
        loadWords();
        buildSubstringTiers();
    }

    public boolean isKnownWord(String normalizedWord) {
        return words.contains(normalizedWord);
    }

    public String pickSubstringForTier(int tier, Random random) {
        int normalizedTier = Math.max(1, Math.min(4, tier));
        boolean useThreeLetters = random.nextDouble() < switch (normalizedTier) {
            case 2 -> 0.10;
            case 3 -> 0.18;
            case 4 -> 0.28;
            default -> 0.04;
        };

        List<String> primary = useThreeLetters
                ? threeLetterSubstringsByTier.getOrDefault(normalizedTier, List.of())
                : twoLetterSubstringsByTier.getOrDefault(normalizedTier, List.of());
        List<String> fallback = useThreeLetters
                ? twoLetterSubstringsByTier.getOrDefault(normalizedTier, List.of())
                : threeLetterSubstringsByTier.getOrDefault(normalizedTier, List.of());

        List<String> options = !primary.isEmpty() ? primary : fallback;
        if (options.isEmpty()) {
            options = useThreeLetters ? DEFAULT_THREE_LETTER_SUBSTRINGS : DEFAULT_TWO_LETTER_SUBSTRINGS;
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
        List<String> sources = new ArrayList<>();
        sources.addAll(loadSystemDictionaries());
        sources.addAll(loadBundledDictionaries());

        if (words.isEmpty()) {
            Collections.addAll(words,
                    "planet", "planted", "charter", "harbor", "reading", "reader", "rocket", "cradle",
                    "triangle", "string", "strain", "drainer", "orange", "operator", "gateway", "memory",
                    "render", "cluster", "kubernetes", "container", "garden", "mission", "session", "java",
                    "spring", "redis", "agones", "server", "client", "leader", "board", "winner", "timers");
            sources = List.of("emergency fallback");
        }
        log.info("Loaded {} dictionary words from {}", words.size(), String.join(", ", sources));
    }

    private List<String> loadSystemDictionaries() {
        List<String> loadedSources = new ArrayList<>();
        for (Path path : SYSTEM_DICTIONARY_PATHS) {
            if (!Files.isRegularFile(path)) {
                continue;
            }
            try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                loadWordsFromReader(reader);
                loadedSources.add(path.toString());
            } catch (IOException ignored) {
                // Fall through to the next candidate path.
            }
        }
        return loadedSources;
    }

    private List<String> loadBundledDictionaries() {
        List<String> loadedSources = new ArrayList<>();
        try {
            ClassPathResource resource = new ClassPathResource("dictionary.txt");
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                loadWordsFromReader(reader);
                loadedSources.add("classpath:dictionary.txt");
            }
        } catch (Exception ignored) {
            // Fall through to the emergency fallback list.
        }
        return loadedSources;
    }

    private void loadWordsFromReader(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            String word = normalizeDictionaryEntry(line);
            if (word.length() >= 4) {
                words.add(word);
            }
        }
    }

    private String normalizeDictionaryEntry(String rawEntry) {
        if (rawEntry == null) {
            return "";
        }

        String candidate = rawEntry.trim();
        if (candidate.isBlank() || candidate.startsWith("#")) {
            return "";
        }

        int slashIndex = candidate.indexOf('/');
        if (slashIndex > 0) {
            candidate = candidate.substring(0, slashIndex);
        }

        return normalize(candidate);
    }

    private void buildSubstringTiers() {
        Map<String, Integer> twoLetterCounts = collectSubstringCounts(2);
        Map<String, Integer> threeLetterCounts = collectSubstringCounts(3);

        buildTieredSubstringMap(twoLetterCounts, twoLetterSubstringsByTier, DEFAULT_TWO_LETTER_SUBSTRINGS);
        buildTieredSubstringMap(threeLetterCounts, threeLetterSubstringsByTier, DEFAULT_THREE_LETTER_SUBSTRINGS);
    }

    private Map<String, Integer> collectSubstringCounts(int length) {
        Map<String, Integer> counts = new HashMap<>();
        for (String word : words) {
            if (word.length() < length) {
                continue;
            }

            Set<String> unique = new HashSet<>();
            for (int i = 0; i <= word.length() - length; i++) {
                unique.add(word.substring(i, i + length));
            }

            for (String substring : unique) {
                counts.merge(substring, 1, Integer::sum);
            }
        }
        return counts;
    }

    private void buildTieredSubstringMap(Map<String, Integer> counts,
                                         Map<Integer, List<String>> destination,
                                         List<String> fallback) {
        destination.clear();

        List<Map.Entry<String, Integer>> ranked = new ArrayList<>(counts.entrySet());
        ranked.sort(Comparator
                .comparingInt((Map.Entry<String, Integer> entry) -> entry.getValue()).reversed()
                .thenComparing(Map.Entry::getKey));

        if (ranked.isEmpty()) {
            for (int tier = 1; tier <= 4; tier++) {
                destination.put(tier, fallback);
            }
            return;
        }

        int size = ranked.size();
        int tier1End = Math.max(1, (int) Math.ceil(size * 0.40));
        int tier2End = Math.max(tier1End + 1, (int) Math.ceil(size * 0.70));
        int tier3End = Math.max(tier2End + 1, (int) Math.ceil(size * 0.90));

        destination.put(1, entriesToKeys(ranked.subList(0, Math.min(size, tier1End))));
        destination.put(2, entriesToKeys(ranked.subList(Math.min(size, tier1End), Math.min(size, tier2End))));
        destination.put(3, entriesToKeys(ranked.subList(Math.min(size, tier2End), Math.min(size, tier3End))));
        destination.put(4, entriesToKeys(ranked.subList(Math.min(size, tier3End), size)));

        if (destination.get(2).isEmpty()) {
            destination.put(2, destination.get(1));
        }
        if (destination.get(3).isEmpty()) {
            destination.put(3, destination.get(2));
        }
        if (destination.get(4).isEmpty()) {
            destination.put(4, destination.get(3));
        }
    }

    private List<String> entriesToKeys(List<Map.Entry<String, Integer>> entries) {
        List<String> keys = new ArrayList<>(entries.size());
        for (Map.Entry<String, Integer> entry : entries) {
            keys.add(entry.getKey());
        }
        return keys;
    }

    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.toLowerCase().replaceAll("[^a-z]", "");
    }
}
