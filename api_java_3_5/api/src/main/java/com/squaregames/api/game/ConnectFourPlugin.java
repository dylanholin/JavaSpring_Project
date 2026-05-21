package com.squaregames.api.game;

import fr.le_campus_numerique.square_games.engine.Game;
import fr.le_campus_numerique.square_games.engine.GameFactory;
import fr.le_campus_numerique.square_games.engine.connectfour.ConnectFourGameFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class ConnectFourPlugin implements GamePlugin {

    private final ConnectFourGameFactory factory = new ConnectFourGameFactory();
    private final MessageSource messageSource;

    @Value("${game.connectfour.default-player-count}")
    private int defaultPlayerCount;

    @Value("${game.connectfour.default-board-size}")
    private int defaultBoardSize;

    public ConnectFourPlugin(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @Override
    public GameFactory getFactory() {
        return factory;
    }

    @Override
    public Game createGame() {
        return factory.createGame(defaultPlayerCount, defaultBoardSize);
    }

    @Override
    public String getName(Locale locale) {
        return messageSource.getMessage("game.connectfour.name", null, locale);
    }

    @Override
    public String getGameType() {
        return factory.getGameFactoryId();
    }
}
