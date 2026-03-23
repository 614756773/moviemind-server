package com.huguo.moviemind_server.content.client;

import com.huguo.moviemind_server.content.dto.ExternalMovieData;

import java.util.List;

public interface ChineseMovieDataProvider {
    String source();
    List<ExternalMovieData> searchMovies(String keyword, int limit);
}
