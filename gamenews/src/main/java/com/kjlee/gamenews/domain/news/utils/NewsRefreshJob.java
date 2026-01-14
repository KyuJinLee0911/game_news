package com.kjlee.gamenews.domain.news.utils;

import com.kjlee.gamenews.domain.news.dto.GameNewsDto;
import com.kjlee.gamenews.domain.news.service.GameNewsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NewsRefreshJob {
    private final GameNewsService gameNewsService;
    private final CacheManager cacheManager;

    @Scheduled(cron = "0 0 6 * * *", zone = "Asia/Seoul")
    public void refreshAtSix() {
        try {
            List<GameNewsDto> fresh = gameNewsService.fetchPrioirtyNewsNoCache();
            if (fresh == null || fresh.isEmpty()) {
                log.warn("뉴스 새로고침 결과가 비어있습니다. 캐시를 재활용합니다.");
                return;
            }

            Cache cache = cacheManager.getCache("priorityNews");
            if (cache != null) {
                cache.put("top20", fresh);
            }
            log.info("뉴스 캐시가 갱신되었습니다. size={}", fresh.size());
        } catch (Exception e) {
            log.error("뉴스 새로고침에 실패했습니다. 기존 캐시를 재활용합니다.", e);
        }
    }
}
