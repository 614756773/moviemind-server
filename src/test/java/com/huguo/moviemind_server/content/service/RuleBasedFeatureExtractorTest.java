package com.huguo.moviemind_server.content.service;

import com.huguo.moviemind_server.content.dto.ExtractionResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuleBasedFeatureExtractorTest {

    private final RuleBasedFeatureExtractor extractor = new RuleBasedFeatureExtractor();

    @Test
    void extract_shouldComputeWeightedSentimentAndTags() {
        String content = "这是一部温暖治愈又励志的家庭成长电影，带有现实主义风格";

        ExtractionResult result = extractor.extract(content);

        assertTrue(result.getSentimentScore() > 0.5);
        assertTrue(result.getThemes().contains("家庭"));
        assertTrue(result.getThemes().contains("成长"));
        assertTrue(result.getMoodTags().contains("治愈"));
        assertTrue(result.getStyleTags().contains("现实主义"));
        assertEquals("rule-based-v1.1", result.getAlgorithmVersion());
    }

    @Test
    void extract_shouldReturnNeutralWhenNoSentimentWord() {
        ExtractionResult result = extractor.extract("一部讲述历史与战争的电影");

        assertEquals(0.0, result.getSentimentScore());
        assertTrue(result.getThemes().contains("历史"));
        assertTrue(result.getThemes().contains("战争"));
    }
}
