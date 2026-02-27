package com.kjlee.gamenews.domain.news.repository;

import com.kjlee.gamenews.domain.news.entitiy.GameNews;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GameNewsRepository extends JpaRepository<GameNews, Long> {
    Optional<GameNews> findByLink(String link);
    boolean existsByLink(String link);
}
