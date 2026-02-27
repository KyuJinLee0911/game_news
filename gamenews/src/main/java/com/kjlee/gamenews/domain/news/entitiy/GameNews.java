package com.kjlee.gamenews.domain.news.entitiy;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "game_news", indexes = {
        @Index(name = "idx_game_news_published_at", columnList = "publishedAt DESC"),
        @Index(name = "idx_game_news_source", columnList = "source")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GameNews {
    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false, length = 1000, unique = true)
    private String link;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(nullable = false, length = 50)
    private String source;

    @Column(length = 50)
    private String badge;

    @Column(nullable = false)
    private LocalDateTime publishedAt;

    @Column(nullable = false)
    private LocalDateTime crawledAt;

    @Column(nullable = false)
    private Long viewCount = 0L;

    @Builder
    public GameNews(String title, String link, String summary, String source, String badge, LocalDateTime publishedAt) {
        this.title = title;
        this.link = link;
        this.summary = summary;
        this.source = source;
        this.badge = badge;
        this.publishedAt = publishedAt;
        this.crawledAt = LocalDateTime.now();
        this.viewCount = 0L;
    }

    public void incrementViewCount() {
        this.viewCount++;
    }
}
