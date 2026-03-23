package com.huguo.moviemind_server.watchlist.controller;

import com.huguo.moviemind_server.common.dto.ApiResponse;
import com.huguo.moviemind_server.common.dto.PageResponse;
import com.huguo.moviemind_server.watchlist.service.WatchlistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/watchlist")
@CrossOrigin(origins = "http://localhost:3000")
public class WatchlistController {

    private final WatchlistService watchlistService;

    @Autowired
    public WatchlistController(WatchlistService watchlistService) {
        this.watchlistService = watchlistService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<WatchlistService.WatchlistResponse>>> getWatchlist(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "addedAt,desc") String[] sort,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(new ApiResponse<>("Not authenticated", null));
        }

        // Parse sort parameters
        String sortField = sort[0];
        Sort.Direction sortDirection = sort.length > 1 ?
                Sort.Direction.fromString(sort[1]) : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, size, sortDirection, sortField);
        Page<WatchlistService.WatchlistResponse> items = watchlistService.getUserWatchlist(
                authentication.getName(), status, search, pageable);

        PageResponse<WatchlistService.WatchlistResponse> response = new PageResponse<>(items);
        return ResponseEntity.ok(new ApiResponse<>("Watchlist retrieved successfully", response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<WatchlistService.WatchlistResponse>> addToWatchlist(
            @RequestBody WatchlistService.WatchlistItemAddRequest request) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(new ApiResponse<>("Not authenticated", null));
        }

        WatchlistService.WatchlistResponse item = watchlistService.addToWatchlist(
                authentication.getName(),
                request.getMovieId(),
                request.getAiReason());

        return ResponseEntity.status(201)
                .body(new ApiResponse<>("Movie added to watchlist successfully", item));
    }

    @PutMapping("/{id}/watched")
    public ResponseEntity<ApiResponse<WatchlistService.WatchlistResponse>> markAsWatched(
            @PathVariable Long id,
            @RequestBody WatchlistService.WatchlistItemMarkWatchedRequest request) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(new ApiResponse<>("Not authenticated", null));
        }

        WatchlistService.WatchlistResponse item = watchlistService.markAsWatched(
                id, authentication.getName(), request);

        return ResponseEntity.ok(new ApiResponse<>("Movie marked as watched successfully", item));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> removeFromWatchlist(@PathVariable Long id) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(new ApiResponse<>("Not authenticated", null));
        }

        watchlistService.removeFromWatchlist(id, authentication.getName());
        return ResponseEntity.ok(new ApiResponse<>("Movie removed from watchlist successfully", null));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WatchlistService.WatchlistResponse>> getWatchlistItem(@PathVariable Long id) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(new ApiResponse<>("Not authenticated", null));
        }

        WatchlistService.WatchlistResponse item = watchlistService.getWatchlistItem(
                id, authentication.getName());

        return ResponseEntity.ok(new ApiResponse<>("Watchlist item retrieved successfully", item));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<WatchlistService.WatchlistStats>> getWatchlistStats() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(new ApiResponse<>("Not authenticated", null));
        }

        WatchlistService.WatchlistStats stats = watchlistService.getWatchlistStats(
                authentication.getName());

        return ResponseEntity.ok(new ApiResponse<>("Watchlist stats retrieved successfully", stats));
    }

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<WatchlistService.WatchlistResponse>>> getPendingItems(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(new ApiResponse<>("Not authenticated", null));
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "addedAt"));
        Page<WatchlistService.WatchlistResponse> items = watchlistService.getUserWatchlist(
                authentication.getName(), "PENDING", null, pageable);

        List<WatchlistService.WatchlistResponse> response = items.getContent();
        return ResponseEntity.ok(new ApiResponse<>("Pending items retrieved successfully", response));
    }

    @GetMapping("/watched")
    public ResponseEntity<ApiResponse<List<WatchlistService.WatchlistResponse>>> getWatchedItems(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(new ApiResponse<>("Not authenticated", null));
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "watchedAt"));
        Page<WatchlistService.WatchlistResponse> items = watchlistService.getUserWatchlist(
                authentication.getName(), "WATCHED", null, pageable);

        List<WatchlistService.WatchlistResponse> response = items.getContent();
        return ResponseEntity.ok(new ApiResponse<>("Watched items retrieved successfully", response));
    }
}