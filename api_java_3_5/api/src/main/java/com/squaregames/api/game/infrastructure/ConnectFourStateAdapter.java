package com.squaregames.api.game.infrastructure;

import fr.le_campus_numerique.square_games.engine.TokenPosition;

import java.util.*;

/**
 * Adapteur pour la reconstruction d'un état ConnectFour persistant.
 * Le moteur attend que les positions y soient 0, 1, 2… consécutifs par colonne
 * (indices à partir du bas). Or la persistance stocke les positions réelles.
 * Cet adapteur renumérote les y pour qu'ils soient acceptés par createGameWithIds.
 */
public class ConnectFourStateAdapter {

    public static List<TokenPosition<UUID>> normalizePositions(List<TokenPosition<UUID>> onBoardTokens) {
        if (onBoardTokens == null || onBoardTokens.isEmpty()) {
            return Collections.emptyList();
        }

        // Regrouper par colonne (x)
        Map<Integer, List<TokenPosition<UUID>>> byColumn = new HashMap<>();
        for (TokenPosition<UUID> tp : onBoardTokens) {
            byColumn.computeIfAbsent(tp.x(), k -> new ArrayList<>()).add(tp);
        }

        List<TokenPosition<UUID>> result = new ArrayList<>();
        for (List<TokenPosition<UUID>> columnTokens : byColumn.values()) {
            // Trier par y descendant (du bas vers le haut)
            columnTokens.sort((a, b) -> Integer.compare(b.y(), a.y()));

            // Renuméroter : le plus bas = 0, ensuite 1, 2…
            int newY = 0;
            for (TokenPosition<UUID> tp : columnTokens) {
                result.add(new TokenPosition<>(
                        tp.owner(), tp.tokenName(), tp.x(), newY++
                ));
            }
        }
        return result;
    }
}
