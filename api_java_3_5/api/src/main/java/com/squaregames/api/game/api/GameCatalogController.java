package com.squaregames.api.game.api;

import com.squaregames.api.game.api.dto.CatalogEntryDto;
import com.squaregames.api.game.application.GamePlugin;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class GameCatalogController {

    private final List<GamePlugin> plugins;

    public GameCatalogController(List<GamePlugin> plugins) {
        this.plugins = plugins;
    }

    @GetMapping("/games/catalog")
    public List<CatalogEntryDto> getCatalog() {
        return plugins.stream()
                .map(p -> new CatalogEntryDto(
                        p.getGameType(),
                        p.getName(LocaleContextHolder.getLocale())
                ))
                .toList();
    }
}
