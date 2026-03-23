package com.huguo.moviemind_server.watchlist.controller;

import com.huguo.moviemind_server.watchlist.service.WatchlistService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WatchlistControllerTest {

    private WatchlistService watchlistService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        watchlistService = Mockito.mock(WatchlistService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new WatchlistController(watchlistService)).build();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void addToWatchlist_withoutAuthentication_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/api/watchlist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"movieId\":42,\"aiReason\":\"test\"}"))
                .andExpect(status().isUnauthorized());

        verify(watchlistService, never()).addToWatchlist(Mockito.anyString(), Mockito.anyLong(), Mockito.anyString());
    }
}
