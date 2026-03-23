package com.huguo.moviemind_server.recommendation.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class RecommendationDto {
    Long eventId;
    String algorithmVersion;
    List<RecommendationMovie> items;

    @Value
    @Builder
    public static class RecommendationMovie {
        Long movieId;
        String title;
        Integer year;
        String genres;
        String posterUrl;
        Double scoreLocal;
        String aiReason;
    }
}
