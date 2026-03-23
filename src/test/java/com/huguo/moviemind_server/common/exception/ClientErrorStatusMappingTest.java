package com.huguo.moviemind_server.common.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huguo.moviemind_server.preference.controller.RatingController;
import com.huguo.moviemind_server.preference.service.RatingService;
import com.huguo.moviemind_server.watchlist.controller.WatchlistController;
import com.huguo.moviemind_server.watchlist.service.WatchlistService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ClientErrorStatusMappingTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private RatingService ratingService;

    @Mock
    private WatchlistService watchlistService;

    private MockMvc ratingMockMvc;
    private MockMvc watchlistMockMvc;

    @BeforeEach
    void setUp() {
        ratingMockMvc = MockMvcBuilders.standaloneSetup(new RatingController(ratingService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        watchlistMockMvc = MockMvcBuilders.standaloneSetup(new WatchlistController(watchlistService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("alice", "N/A")
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createPreference_whenDuplicateRating_thenReturns409() throws Exception {
        when(ratingService.createRating(eq("alice"), any(RatingService.RatingRequest.class)))
                .thenThrow(new ConflictException("Rating for this movie already exists"));

        ratingMockMvc.perform(post("/api/preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RatingRequestBody(101L, 8, "great movie"))))
                .andExpect(status().isConflict());
    }

    @Test
    void getPreference_whenCrossUserAccess_thenReturns403() throws Exception {
        when(ratingService.getRatingById(anyLong(), eq("alice")))
                .thenThrow(new ForbiddenException("Forbidden to access this rating"));

        ratingMockMvc.perform(get("/api/preferences/12"))
                .andExpect(status().isForbidden());
    }

    @Test
    void addToWatchlist_whenDuplicateItem_thenReturns409() throws Exception {
        when(watchlistService.addToWatchlist(eq("alice"), eq(202L), eq("because")))
                .thenThrow(new ConflictException("Movie already in watchlist"));

        watchlistMockMvc.perform(post("/api/watchlist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new WatchlistRequestBody(202L, "because"))))
                .andExpect(status().isConflict());
    }

    @Test
    void getWatchlistItem_whenCrossUserAccess_thenReturns403() throws Exception {
        when(watchlistService.getWatchlistItem(anyLong(), eq("alice")))
                .thenThrow(new ForbiddenException("Forbidden to access this item"));

        watchlistMockMvc.perform(get("/api/watchlist/44"))
                .andExpect(status().isForbidden());
    }

    private record RatingRequestBody(Long movieId, Integer score, String notes) {}

    private record WatchlistRequestBody(Long movieId, String aiReason) {}
}
