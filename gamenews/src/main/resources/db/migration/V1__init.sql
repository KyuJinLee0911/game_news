CREATE TABLE game_news
(
    id           BIGSERIAL PRIMARY KEY,
    title        VARCHAR(500)  NOT NULL,
    link         VARCHAR(1000) NOT NULL UNIQUE,
    summary      TEXT,
    source       VARCHAR(50)   NOT NULL,
    badge        VARCHAR(50),
    published_at TIMESTAMP     NOT NULL,
    crawled_at   TIMESTAMP     NOT NULL DEFAULT NOW(),
    view_count   BIGINT        NOT NULL DEFAULT 0
);



CREATE TABLE crawl_history
(
    id            BIGSERIAL PRIMARY KEY,
    source        VARCHAR(50) NOT NULL,
    crawled_at    TIMESTAMP   NOT NULL DEFAULT NOW(),
    success       BOOLEAN     NOT NULL,
    article_count INT         NOT NULL DEFAULT 0,
    error_message TEXT,
    duration_ms   BIGINT      NOT NULL DEFAULT 0
);

CREATE INDEX idx_game_news_published_at ON game_news(published_at DESC);
CREATE INDEX idx_game_news_source ON game_news(source);
CREATE INDEX idx_crawl_history_crawled_at ON crawl_history(crawled_at DESC);