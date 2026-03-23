package com.huguo.moviemind_server.content.dto;

import lombok.Data;

import java.util.List;

@Data
public class ExternalMovieData {
    private String externalId;
    private String title;
    private Integer year;
    private String summary;
    private List<String> genres;
    private String rawPayload;
}
