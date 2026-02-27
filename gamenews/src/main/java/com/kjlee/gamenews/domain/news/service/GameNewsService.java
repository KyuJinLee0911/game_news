package com.kjlee.gamenews.domain.news.service;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.kjlee.gamenews.domain.news.dto.GameNewsDto;
import com.kjlee.gamenews.domain.news.dto.Item;
import com.kjlee.gamenews.domain.news.dto.RssResponse;
import com.kjlee.gamenews.domain.news.entitiy.CrawlHistory;
import com.kjlee.gamenews.domain.news.entitiy.GameNews;
import com.kjlee.gamenews.domain.news.repository.CrawlHistoryRepository;
import com.kjlee.gamenews.domain.news.repository.GameNewsRepository;
import com.kjlee.gamenews.domain.news.utils.NewsFilterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameNewsService {
    private final WebClient webClient;
    private final XmlMapper xmlMapper;
    private final NewsFilterService newsFilter;
    private final GameNewsRepository gameNewsRepository;
    private final CrawlHistoryRepository crawlHistoryRepository;

    private String safe(String s) {
        return (s == null) ? "" : s;
    }

    public List<GameNewsDto> fetchRssData(String query, int count, String badgePrefix) {
        String encodedQ = URLEncoder.encode(query + " when:2d", StandardCharsets.UTF_8);
        String url = "https://news.google.com/rss/search?q=" + encodedQ + "&hl=ko&gl=KR&ceid=KR:ko";
        long startTime = System.currentTimeMillis();
        String source = resolveSource(badgePrefix);

        try {
            String xml = webClient.get()
                    .uri(URI.create(url))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (xml == null || xml.isBlank()) return List.of();


            RssResponse rss = xmlMapper.readValue(xml, RssResponse.class);

            List<Item> items = (rss != null && rss.channel() != null) ? rss.channel().getItems() : null;
            if (items == null || items.isEmpty()) return List.of();

            List<GameNewsDto> collected = new ArrayList<>();
            for (Item item : items) {
                if (collected.size() >= count) break;

                String rawTitle = safe(item.title());
                String link = safe(item.link()).trim();

                if (!newsFilter.isCleanNews(rawTitle)) continue;

                // 중복 크롤링 방지
                if (gameNewsRepository.existsByLink(link)) continue;

                String sourceBadge = newsFilter.resolveSourceBadge(rawTitle, badgePrefix);
                String cleanTitle = newsFilter.cleanTitle(rawTitle);

                String summary;
                try {
                    String rawDesc = safe(item.description());
                    summary = Jsoup.parse(rawDesc).text();
                    if (summary.isBlank()) summary = "기사 원문 보러가기";
                } catch (Exception e) {
                    summary = "기사 원문 보러가기";
                }

                // 의미 없는 summary를 가진 뉴스 제외
                if (!newsFilter.isValidSummary(summary)) continue;

                String date;
                try {
                    String pub = safe(item.pubDate());
                    date = pub.length() >= 16 ? pub.substring(0, 16) : pub;
                    if (date.isBlank()) date = "최신";
                } catch (Exception e) {
                    date = "최신";
                }

                // DB에 저장
                GameNews gameNews = GameNews.builder()
                        .title(cleanTitle)
                        .link(link)
                        .summary(summary)
                        .source(source)
                        .badge(sourceBadge)
                        .publishedAt(LocalDateTime.now())
                        .build();
                gameNewsRepository.save(gameNews);

                collected.add(new GameNewsDto(sourceBadge, cleanTitle, link, date, summary));
            }

            saveCrawlHistory(source, true, collected.size(), null, startTime);
            return collected;
        } catch (Exception e) {
            log.error("기사 정보를 받아오는 데 실패했습니다. {}", e.getMessage(), e);
            saveCrawlHistory(source, false, 0, e.getMessage(), startTime);
            return List.of();
        }
    }

    private void saveCrawlHistory(String source, boolean success,
                                  int articleCount, String errorMessage, long startTime) {
        crawlHistoryRepository.save(CrawlHistory.builder()
                .source(source)
                .success(success)
                .articleCount(articleCount)
                .errorMessage(errorMessage)
                .durationMs(System.currentTimeMillis() - startTime)
                .build());
    }

    private String resolveSource(String badgePrefix) {
        if (badgePrefix == null) return "MIXED";
        if (badgePrefix.contains("TIG")) return "TIG";
        if (badgePrefix.contains("인벤")) return "INVEN";
        if (badgePrefix.contains("메카")) return "GAMEMECA";
        if (badgePrefix.contains("동아")) return "DONGA";
        return "UNKNOWN";
    }

    @Cacheable(cacheNames = "priorityNews", key = "'top20'")
    public List<GameNewsDto> fetchPriorityNews() {
        return fetchPrioirtyNewsNoCache();
    }

    public List<GameNewsDto> fetchPrioirtyNewsNoCache() {

        List<GameNewsDto> tig = fetchRssData("site:thisisgame.com/articles?newsId=263", 8, "🎮 [TIG]");
        List<GameNewsDto> finalList = new ArrayList<>(tig);

        String otherQuery = "site:inven.co.kr/webzine/news OR site:gamemeca.com/view.php OR site:game.donga.com";
        List<GameNewsDto> other = fetchRssData(otherQuery, 12, null);
        finalList.addAll(other);

        return finalList;
    }
}
