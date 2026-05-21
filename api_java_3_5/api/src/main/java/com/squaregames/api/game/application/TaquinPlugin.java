package com.squaregames.api.game.application;

import fr.le_campus_numerique.square_games.engine.Game;
import fr.le_campus_numerique.square_games.engine.GameFactory;
import fr.le_campus_numerique.square_games.engine.taquin.TaquinGameFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class TaquinPlugin implements GamePlugin {

    private final TaquinGameFactory factory = new TaquinGameFactory();
    private final MessageSource messageSource;

    @Value("${game.taquin.default-player-count}")
    private int defaultPlayerCount;

    @Value("${game.taquin.default-board-size}")
    private int defaultBoardSize;

    public TaquinPlugin(MessageSource messageSource) {
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
        return messageSource.getMessage("game.taquin.name", null, locale);
    }

    @Override
    public String getGameType() {
        return factory.getGameFactoryId();
    }
}
