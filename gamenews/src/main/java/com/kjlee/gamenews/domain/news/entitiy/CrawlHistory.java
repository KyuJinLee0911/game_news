package com.kjlee.gamenews.domain.news.entitiy;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "crawl_history", indexes = {
        @Index(name = "idx_crawl_history_crawled_at", columnList = "crawledAt DESC")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CrawlHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String source;

    @Column(nullable = false)
    private LocalDateTime crawledAt;

    @Column(nullable = false)
    private boolean success;

    @Column(nullable = false)
    private int articleCount;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(nullable = false)
    private long durationMs;

    @Builder
    public CrawlHistory(String source, boolean success, int articleCount, String errorMessage, long durationMs) {
        this.source = source;
        this.crawledAt = LocalDateTime.now();
        this.success = success;
        this.articleCount = articleCount;
        this.errorMessage = errorMessage;
        this.durationMs = durationMs;
    }
}
