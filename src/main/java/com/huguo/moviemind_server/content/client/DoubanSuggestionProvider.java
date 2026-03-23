package com.huguo.moviemind_server.content.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huguo.moviemind_server.content.dto.ExternalMovieData;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
public class DoubanSuggestionProvider implements ChineseMovieDataProvider {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public DoubanSuggestionProvider(RestTemplateBuilder restTemplateBuilder, ObjectMapper objectMapper) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(8))
                .build();
        this.objectMapper = objectMapper;
    }

    @Override
    public String source() {
        return "douban";
    }

    @Override
    public List<ExternalMovieData> searchMovies(String keyword, int limit) {
        String url = UriComponentsBuilder
                .fromHttpUrl("https://movie.douban.com/j/subject_suggest")
                .queryParam("q", keyword)
                .toUriString();

        String response = restTemplate.getForObject(url, String.class);
        if (response == null || response.isBlank()) {
            return List.of();
        }

        try {
            JsonNode arrayNode = objectMapper.readTree(response);
            List<ExternalMovieData> result = new ArrayList<>();
            for (JsonNode node : arrayNode) {
                if (result.size() >= limit) {
                    break;
                }

                ExternalMovieData item = new ExternalMovieData();
                item.setExternalId(node.path("id").asText(null));
                item.setTitle(node.path("title").asText(null));

                String year = node.path("year").asText(null);
                if (year != null && year.matches("\\d{4}")) {
                    item.setYear(Integer.parseInt(year));
                }

                String subTitle = node.path("sub_title").asText("");
                item.setSummary(subTitle);
                item.setGenres(List.of());
                item.setRawPayload(node.toString());
                result.add(item);
            }
            return result;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse Douban suggestion response", ex);
        }
    }
}
