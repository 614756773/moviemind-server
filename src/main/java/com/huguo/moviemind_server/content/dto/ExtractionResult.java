package com.huguo.moviemind_server.content.dto;

import lombok.Builder;
import lombok.Value;

import java.util.Set;

@Value
@Builder
public class ExtractionResult {
    double sentimentScore;
    Set<String> themes;
    Set<String> moodTags;
    Set<String> styleTags;
    Set<String> keywords;
    String algorithmVersion;
}
