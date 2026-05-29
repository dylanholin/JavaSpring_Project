-- Schéma de base de données pour la persistance des parties
-- Utilisé par H2 (peut être adapté pour PostgreSQL/MySQL)

CREATE TABLE IF NOT EXISTS games (
    id VARCHAR(36) PRIMARY KEY,
    factory_id VARCHAR(50) NOT NULL,
    board_size INT NOT NULL,
    player_count INT NOT NULL,
    player_ids VARCHAR(1000),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table pour stocker les positions des tokens
CREATE TABLE IF NOT EXISTS game_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    game_id VARCHAR(36) NOT NULL,
    token_name VARCHAR(10) NOT NULL,
    owner_id VARCHAR(36),
    x_position INT,
    y_position INT,
    is_on_board BOOLEAN DEFAULT FALSE,
    is_removed BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (game_id) REFERENCES games(id) ON DELETE CASCADE
);

-- Index pour accélérer les recherches
CREATE INDEX IF NOT EXISTS idx_game_tokens_game_id ON game_tokens(game_id);
