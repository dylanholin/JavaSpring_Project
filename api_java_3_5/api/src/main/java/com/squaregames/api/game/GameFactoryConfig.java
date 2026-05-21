package com.squaregames.api.game;

import fr.le_campus_numerique.square_games.engine.GameFactory;
import fr.le_campus_numerique.square_games.engine.connectfour.ConnectFourGameFactory;
import fr.le_campus_numerique.square_games.engine.taquin.TaquinGameFactory;
import fr.le_campus_numerique.square_games.engine.tictactoe.TicTacToeGameFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GameFactoryConfig {

    @Bean
    public GameFactory ticTacToeGameFactory() {
        return new TicTacToeGameFactory();
    }

    @Bean
    public GameFactory connectFourGameFactory() {
        return new ConnectFourGameFactory();
    }

    @Bean
    public GameFactory taquinGameFactory() {
        return new TaquinGameFactory();
    }
}
