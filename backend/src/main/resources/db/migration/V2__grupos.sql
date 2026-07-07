-- Grupos: unidade de login do sistema. Participantes (users) e mercados passam a
-- pertencer a exatamente um grupo; sessoes autenticam um grupo via token.

CREATE TABLE grupos (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    nome       VARCHAR(100) NOT NULL,
    senha_hash VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_grupos_nome UNIQUE (nome)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- Grupo "legado" para dados criados antes desta migracao. A senha hash '!' e
-- invalida por construcao (nenhum BCrypt gera isso), entao o grupo nao aceita login.
INSERT INTO grupos (nome, senha_hash)
SELECT 'legado', '!'
FROM DUAL
WHERE EXISTS (SELECT 1 FROM users);

ALTER TABLE users ADD COLUMN grupo_id BIGINT NULL;
UPDATE users SET grupo_id = (SELECT id FROM grupos WHERE nome = 'legado');
ALTER TABLE users MODIFY grupo_id BIGINT NOT NULL;
ALTER TABLE users DROP INDEX uq_users_nome;
ALTER TABLE users ADD CONSTRAINT uq_users_grupo_nome UNIQUE (grupo_id, nome);
ALTER TABLE users ADD CONSTRAINT fk_users_grupo FOREIGN KEY (grupo_id) REFERENCES grupos (id);

ALTER TABLE markets ADD COLUMN grupo_id BIGINT NULL;
UPDATE markets m SET m.grupo_id = (SELECT u.grupo_id FROM users u WHERE u.id = m.criador_id);
ALTER TABLE markets MODIFY grupo_id BIGINT NOT NULL;
ALTER TABLE markets ADD CONSTRAINT fk_markets_grupo FOREIGN KEY (grupo_id) REFERENCES grupos (id);
CREATE INDEX idx_markets_grupo_status ON markets (grupo_id, status);

CREATE TABLE sessoes (
    token     VARCHAR(64) NOT NULL,
    grupo_id  BIGINT      NOT NULL,
    criado_em TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (token),
    CONSTRAINT fk_sessoes_grupo FOREIGN KEY (grupo_id) REFERENCES grupos (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
