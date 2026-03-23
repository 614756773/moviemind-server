package com.huguo.moviemind_server.content.service;

import com.huguo.moviemind_server.content.dto.ExtractionResult;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class RuleBasedFeatureExtractor {

    public static final String ALGORITHM_VERSION = "rule-based-v1.1";

    private static final Map<String, Double> POSITIVE_LEXICON = Map.of(
            "温暖", 1.0,
            "治愈", 1.2,
            "励志", 1.1,
            "希望", 1.0,
            "感动", 1.0,
            "热血", 0.9,
            "浪漫", 0.8
    );

    private static final Map<String, Double> NEGATIVE_LEXICON = Map.of(
            "压抑", 1.2,
            "悲伤", 1.0,
            "绝望", 1.1,
            "黑暗", 0.9,
            "恐怖", 1.0,
            "沉重", 0.8,
            "悲剧", 0.9
    );

    private static final Set<String> THEME_DICTIONARY = Set.of("家庭", "成长", "犯罪", "爱情", "历史", "战争", "友情", "科幻", "悬疑");
    private static final Set<String> MOOD_DICTIONARY = Set.of("治愈", "紧张", "悲情", "热血", "轻松", "伤感");
    private static final Set<String> STYLE_DICTIONARY = Set.of("现实主义", "黑色幽默", "悬疑惊悚", "魔幻现实", "史诗");

    public ExtractionResult extract(String rawContent) {
        String content = rawContent == null ? "" : rawContent.toLowerCase(Locale.ROOT);

        Set<String> themes = matchDictionary(content, THEME_DICTIONARY);
        Set<String> moodTags = matchDictionary(content, MOOD_DICTIONARY);
        Set<String> styleTags = matchDictionary(content, STYLE_DICTIONARY);

        double positiveScore = computeWeightedScore(content, POSITIVE_LEXICON);
        double negativeScore = computeWeightedScore(content, NEGATIVE_LEXICON);
        double sentimentScore = normalizeSentiment(positiveScore, negativeScore);

        Set<String> keywords = new LinkedHashSet<>();
        keywords.addAll(themes);
        keywords.addAll(moodTags);
        keywords.addAll(styleTags);
        keywords.addAll(topSentimentKeywords(content, POSITIVE_LEXICON, NEGATIVE_LEXICON));

        return ExtractionResult.builder()
                .sentimentScore(sentimentScore)
                .themes(themes)
                .moodTags(moodTags)
                .styleTags(styleTags)
                .keywords(keywords)
                .algorithmVersion(ALGORITHM_VERSION)
                .build();
    }

    private Set<String> matchDictionary(String content, Set<String> dictionary) {
        Set<String> tags = new LinkedHashSet<>();
        for (String word : dictionary) {
            if (content.contains(word.toLowerCase(Locale.ROOT))) {
                tags.add(word);
            }
        }
        return tags;
    }

    private double computeWeightedScore(String content, Map<String, Double> lexicon) {
        double score = 0.0;
        for (Map.Entry<String, Double> entry : lexicon.entrySet()) {
            if (content.contains(entry.getKey().toLowerCase(Locale.ROOT))) {
                score += entry.getValue();
            }
        }
        return score;
    }

    private double normalizeSentiment(double positiveScore, double negativeScore) {
        double total = positiveScore + negativeScore;
        if (total == 0) {
            return 0.0;
        }
        return (positiveScore - negativeScore) / total;
    }

    private Set<String> topSentimentKeywords(String content, Map<String, Double> positive, Map<String, Double> negative) {
        Map<String, Double> matched = new LinkedHashMap<>();
        for (Map.Entry<String, Double> e : positive.entrySet()) {
            if (content.contains(e.getKey().toLowerCase(Locale.ROOT))) {
                matched.put(e.getKey(), e.getValue());
            }
        }
        for (Map.Entry<String, Double> e : negative.entrySet()) {
            if (content.contains(e.getKey().toLowerCase(Locale.ROOT))) {
                matched.put(e.getKey(), e.getValue());
            }
        }

        return matched.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(LinkedHashSet::new, Set::add, Set::addAll);
    }
}
