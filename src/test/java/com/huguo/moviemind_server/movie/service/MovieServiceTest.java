package com.huguo.moviemind_server.movie.service;

import com.huguo.moviemind_server.common.exception.ResourceNotFoundException;
import com.huguo.moviemind_server.movie.model.Movie;
import com.huguo.moviemind_server.movie.model.Tag;
import com.huguo.moviemind_server.movie.repository.MovieRepository;
import com.huguo.moviemind_server.movie.repository.TagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovieServiceTest {

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private TagRepository tagRepository;

    private MovieService movieService;

    @BeforeEach
    void setUp() {
        movieService = new MovieService(movieRepository, tagRepository, false);
    }

    @Test
    void getMoviesByTag_shouldQueryByResolvedTagName() {
        Tag tag = new Tag();
        tag.setName("治愈");

        Movie movie = new Movie();
        movie.setTitle("千与千寻");

        when(tagRepository.findByName("治愈")).thenReturn(Optional.of(tag));
        when(movieRepository.findByTagName("治愈")).thenReturn(List.of(movie));

        List<Movie> result = movieService.getMoviesByTag("治愈");

        assertEquals(1, result.size());
        assertEquals("千与千寻", result.getFirst().getTitle());
        verify(movieRepository).findByTagName("治愈");
    }

    @Test
    void getMoviesByTag_shouldThrowWhenTagMissing() {
        when(tagRepository.findByName("不存在标签")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> movieService.getMoviesByTag("不存在标签"));
    }

    @Test
    void getMoviesByGenre_shouldDelegateToRepository() {
        Movie movie = new Movie();
        movie.setTitle("盗梦空间");

        when(movieRepository.findByGenreName("科幻")).thenReturn(List.of(movie));

        List<Movie> result = movieService.getMoviesByGenre("科幻");

        assertEquals(1, result.size());
        assertEquals("盗梦空间", result.getFirst().getTitle());
        verify(movieRepository).findByGenreName("科幻");
    }

    @Test
    void getAllGenreNames_shouldSplitSortAndDeduplicate() {
        Movie movie1 = new Movie();
        movie1.setGenresStr("剧情, 犯罪");

        Movie movie2 = new Movie();
        movie2.setGenresStr("科幻,剧情");

        Movie movie3 = new Movie();
        movie3.setGenresStr(" ");

        when(movieRepository.findAll()).thenReturn(List.of(movie1, movie2, movie3));

        List<String> result = movieService.getAllGenreNames();

        assertEquals(List.of("剧情", "犯罪", "科幻"), result);
    }
}
