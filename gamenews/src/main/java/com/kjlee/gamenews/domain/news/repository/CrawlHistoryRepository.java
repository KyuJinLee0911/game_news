package com.kjlee.gamenews.domain.news.repository;

import com.kjlee.gamenews.domain.news.entitiy.CrawlHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrawlHistoryRepository extends JpaRepository<CrawlHistory, Long> {
}
