-- Schema inicial do sistema de apostas mutuas (parimutuel).
-- InnoDB e obrigatorio: da suporte a chaves estrangeiras e a row-level locking
-- (SELECT ... FOR UPDATE), usado no lock pessimista do settlement.

CREATE TABLE users (
    id      BIGINT        NOT NULL AUTO_INCREMENT,
    nome    VARCHAR(255)  NOT NULL,
    saldo   DECIMAL(19,2) NOT NULL,
    version BIGINT        NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_users_nome UNIQUE (nome)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE markets (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    pergunta   VARCHAR(255) NOT NULL,
    opcao_a    VARCHAR(255) NOT NULL,
    opcao_b    VARCHAR(255) NOT NULL,
    status     VARCHAR(16)  NOT NULL,
    resultado  VARCHAR(1)   NULL,
    criador_id BIGINT       NOT NULL,
    version    BIGINT       NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_markets_criador FOREIGN KEY (criador_id) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE bets (
    id        BIGINT        NOT NULL AUTO_INCREMENT,
    user_id   BIGINT        NOT NULL,
    market_id BIGINT        NOT NULL,
    opcao     VARCHAR(1)    NOT NULL,
    valor     DECIMAL(19,2) NOT NULL,
    payout    DECIMAL(19,2) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_bets_user   FOREIGN KEY (user_id)   REFERENCES users (id),
    CONSTRAINT fk_bets_market FOREIGN KEY (market_id) REFERENCES markets (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_bets_market ON bets (market_id);
CREATE INDEX idx_markets_status ON markets (status);
