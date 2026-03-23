package com.huguo.moviemind_server.content.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huguo.moviemind_server.content.dto.ExternalMovieData;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
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
        String response;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.USER_AGENT, "Mozilla/5.0 (compatible; MovieMindBot/1.0)");
            ResponseEntity<String> entity = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            response = entity.getBody();
            if (response == null || response.isBlank()) {
                return fallbackResults(keyword, limit, "empty_response");
            }
        } catch (RestClientException ex) {
            return fallbackResults(keyword, limit, "network_error");
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
            if (result.isEmpty()) {
                return fallbackResults(keyword, limit, "no_items");
            }
            return result;
        } catch (Exception ex) {
            return fallbackResults(keyword, limit, "parse_error");
        }
    }

    private List<ExternalMovieData> fallbackResults(String keyword, int limit, String reason) {
        List<ExternalMovieData> fallback = List.of(
                buildFallback("fallback_1", "流浪地球", 2019, "科幻,冒险", keyword, reason),
                buildFallback("fallback_2", "你好，李焕英", 2021, "喜剧,剧情", keyword, reason),
                buildFallback("fallback_3", "让子弹飞", 2010, "剧情,犯罪", keyword, reason)
        );
        return fallback.stream().limit(limit).toList();
    }

    private ExternalMovieData buildFallback(String id, String title, Integer year, String tags, String keyword, String reason) {
        ExternalMovieData data = new ExternalMovieData();
        data.setExternalId(id);
        data.setTitle(title);
        data.setYear(year);
        data.setSummary("fallback:" + reason + ";keyword:" + keyword);
        data.setGenres(List.of(tags.split(",")));
        data.setRawPayload("{\"source\":\"fallback\",\"reason\":\"" + reason + "\"}");
        return data;
    }
}
