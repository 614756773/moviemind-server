package com.huguo.moviemind_server.recommendation.dto;

import com.huguo.moviemind_server.recommendation.model.RecommendationItem;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FeedbackRequest {
    @NotNull
    private Long eventId;

    @NotNull
    private Long movieId;

    @NotNull
    private RecommendationItem.FeedbackType feedbackType;
}
