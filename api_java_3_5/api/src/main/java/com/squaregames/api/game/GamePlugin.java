package com.squaregames.api.game;

import fr.le_campus_numerique.square_games.engine.Game;
import fr.le_campus_numerique.square_games.engine.GameFactory;

import java.util.Locale;

public interface GamePlugin {
    GameFactory getFactory();
    Game createGame();
    String getName(Locale locale);
    String getGameType();
}
