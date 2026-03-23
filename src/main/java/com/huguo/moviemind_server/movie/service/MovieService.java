package com.huguo.moviemind_server.movie.service;

import com.huguo.moviemind_server.common.exception.ResourceNotFoundException;
import com.huguo.moviemind_server.movie.model.Movie;
import com.huguo.moviemind_server.movie.model.Tag;
import com.huguo.moviemind_server.movie.repository.MovieRepository;
import com.huguo.moviemind_server.movie.repository.TagRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class MovieService {

    private final MovieRepository movieRepository;
    private final TagRepository tagRepository;

    @Autowired
    public MovieService(MovieRepository movieRepository,
                      TagRepository tagRepository) {
        this.movieRepository = movieRepository;
        this.tagRepository = tagRepository;
    }

    public Page<Movie> searchMovies(String query, Pageable pageable) {
        return movieRepository.searchMovies(query, pageable);
    }

    public Movie getMovieById(Long id) {
        return movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found with id: " + id));
    }

    public Movie getMovieByExternalId(String externalId) {
        return movieRepository.findByExternalId(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found with externalId: " + externalId));
    }


    public List<Movie> getMoviesByTag(String tagName) {
        Tag tag = tagRepository.findByName(tagName)
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found: " + tagName));
        return movieRepository.findByTagsIn(Arrays.asList(tag.getName()));
    }

    public List<Movie> getMoviesByYear(Integer year) {
        return movieRepository.findByYear(year);
    }

    public List<String> getAllTagNames() {
        return tagRepository.findAll().stream()
                .map(Tag::getName)
                .sorted()
                .collect(Collectors.toList());
    }

    public Movie createMovie(Movie movie) {
        if (movie.getExternalId() != null && movieRepository.existsByExternalId(movie.getExternalId())) {
            throw new RuntimeException("Movie with externalId " + movie.getExternalId() + " already exists");
        }
        return movieRepository.save(movie);
    }

    public void deleteMovie(Long id) {
        Movie movie = getMovieById(id);
        movieRepository.delete(movie);
    }

    @PostConstruct
    public void initializeSampleData() {
        // Initialize basic tags if they don't exist
        if (tagRepository.count() == 0) {
            initializeTags();
        }

        // Initialize sample movies if they don't exist
        if (movieRepository.count() == 0) {
            initializeMovies();
        }
    }

    private void initializeTags() {
        List<String> moodTags = Arrays.asList(
            "搞笑", "治愈", "伤感", "励志", "感动", "压抑", "紧张", "轻松",
            "烧脑", "刺激", "温馨", "沉重", "欢快", "悲情", "热血", "文艺"
        );

        List<String> styleTags = Arrays.asList(
            "黑色幽默", "魔幻现实", "现实主义", "浪漫主义", "荒诞派",
            "悬疑惊悚", "温情脉脉", "史诗巨制", "小清新", "复古"
        );

        for (String name : moodTags) {
            if (!tagRepository.existsByName(name)) {
                Tag tag = new Tag();
                tag.setName(name);
                tag.setDescription(name + "氛围的电影");
                tag.setType(Tag.TagType.MOOD);
                tagRepository.save(tag);
            }
        }

        for (String name : styleTags) {
            if (!tagRepository.existsByName(name)) {
                Tag tag = new Tag();
                tag.setName(name);
                tag.setDescription(name + "风格的电影");
                tag.setType(Tag.TagType.STYLE);
                tagRepository.save(tag);
            }
        }
    }

    private void initializeMovies() {
        // Sample movies data
        List<Movie> sampleMovies = Arrays.asList(
            createSampleMovie("肖申克的救赎", 1994, "https://example.com/shawshank.jpg", 9.7, "剧情", "犯罪"),
            createSampleMovie("霸王别姬", 1993, "https://example.com/farewell.jpg", 9.6, "剧情", "文艺"),
            createSampleMovie("阿甘正传", 1994, "https://example.com/forrest.jpg", 9.5, "剧情", "励志"),
            createSampleMovie("泰坦尼克号", 1997, "https://example.com/titanic.jpg", 9.4, "剧情", "爱情"),
            createSampleMovie("这个杀手不太冷", 1994, "https://example.com/leon.jpg", 9.4, "剧情", "动作"),
            createSampleMovie("千与千寻", 2001, "https://example.com/spirited.jpg", 9.4, "动画", "奇幻"),
            createSampleMovie("辛德勒的名单", 1993, "https://example.com/schindler.jpg", 9.6, "剧情", "历史"),
            createSampleMovie("盗梦空间", 2010, "https://example.com/inception.jpg", 9.3, "科幻", "悬疑"),
            createSampleMovie("忠犬八公的故事", 2009, "https://example.com/hachiko.jpg", 9.4, "剧情", "感动"),
            createSampleMovie("海上钢琴师", 1998, "https://example.com/pianist.jpg", 9.3, "剧情", "音乐")
        );

        movieRepository.saveAll(sampleMovies);
    }

    private Movie createSampleMovie(String title, int year, String posterUrl, double rating, String... genres) {
        Movie movie = new Movie();
        movie.setTitle(title);
        movie.setYear(year);
        movie.setPosterUrl(posterUrl);
        movie.setRatingExternal(rating);
        movie.setExternalId("MOVIE_" + title.replace(" ", "_") + "_" + year);

        return movie;
    }
}