package com.huguo.moviemind_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class MoviemindServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(MoviemindServerApplication.class, args);
	}

}
