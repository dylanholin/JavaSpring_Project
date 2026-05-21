package com.squaregames.api.game;

import fr.le_campus_numerique.square_games.engine.GameFactory;
import fr.le_campus_numerique.square_games.engine.connectfour.ConnectFourGameFactory;
import fr.le_campus_numerique.square_games.engine.taquin.TaquinGameFactory;
import fr.le_campus_numerique.square_games.engine.tictactoe.TicTacToeGameFactory;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Service
public class GameCatalogImpl implements GameCatalog {

    private final List<GameFactory> gameFactories;

    public GameCatalogImpl() {
        this.gameFactories = List.of(
                new TicTacToeGameFactory(),
                new ConnectFourGameFactory(),
                new TaquinGameFactory()
        );
    }

    @Override
    public Collection<String> getGameIdentifiers() {
        return gameFactories.stream()
                .map(GameFactory::getGameFactoryId)
                .toList();
    }
}
