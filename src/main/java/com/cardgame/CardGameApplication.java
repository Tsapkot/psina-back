package com.cardgame;

import com.cardgame.service.GameService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class CardGameApplication {

    public static void main(String[] args) {
        SpringApplication.run(CardGameApplication.class, args);
    }

    @Bean
    public CommandLineRunner init(GameService gameService) {
        return args -> gameService.initializeCards();
    }
}