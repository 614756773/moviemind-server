package com.huguo.moviemind_server.watchlist.service;

import com.huguo.moviemind_server.auth.model.User;
import com.huguo.moviemind_server.auth.repository.UserRepository;
import com.huguo.moviemind_server.common.exception.ResourceNotFoundException;
import com.huguo.moviemind_server.movie.model.Movie;
import com.huguo.moviemind_server.movie.repository.MovieRepository;
import com.huguo.moviemind_server.preference.repository.RatingRepository;
import com.huguo.moviemind_server.watchlist.model.WatchlistItem;
import com.huguo.moviemind_server.watchlist.repository.WatchlistItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WatchlistServiceTest {

    @Mock private WatchlistItemRepository watchlistItemRepository;
    @Mock private UserRepository userRepository;
    @Mock private MovieRepository movieRepository;
    @Mock private RatingRepository ratingRepository;

    private WatchlistService watchlistService;

    @BeforeEach
    void setUp() {
        watchlistService = new WatchlistService(watchlistItemRepository, userRepository, movieRepository, ratingRepository);
    }

    @Test
    void addToWatchlist_authenticatedUsername_shouldSucceed() {
        User user = new User();
        user.setId("user-id-1");
        user.setUsername("alice");

        Movie movie = new Movie();
        movie.setId(42L);

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(movieRepository.findById(42L)).thenReturn(Optional.of(movie));
        when(watchlistItemRepository.existsByUserIdAndMovieId("alice", 42L)).thenReturn(false);
        when(watchlistItemRepository.save(any(WatchlistItem.class))).thenAnswer(invocation -> {
            WatchlistItem item = invocation.getArgument(0);
            item.setId(100L);
            return item;
        });

        WatchlistService.WatchlistResponse response = watchlistService.addToWatchlist("alice", 42L, "Because you'll love it");

        assertEquals(100L, response.getId());
        assertEquals("alice", response.getUserId());
        assertEquals(42L, response.getMovieId());

        ArgumentCaptor<WatchlistItem> savedCaptor = ArgumentCaptor.forClass(WatchlistItem.class);
        verify(watchlistItemRepository).save(savedCaptor.capture());
        assertEquals("alice", savedCaptor.getValue().getUserId());
        assertEquals(WatchlistItem.WatchlistStatus.PENDING, savedCaptor.getValue().getStatus());
    }

    @Test
    void addToWatchlist_unknownUsername_shouldThrowNotFound() {
        when(userRepository.findByUsername("missing-user")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> watchlistService.addToWatchlist("missing-user", 42L, "reason"));

        verify(movieRepository, never()).findById(any());
        verify(watchlistItemRepository, never()).save(any());
    }
}
