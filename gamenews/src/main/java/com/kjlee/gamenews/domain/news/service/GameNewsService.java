package com.kjlee.gamenews.domain.news.service;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.kjlee.gamenews.domain.news.dto.GameNewsDto;
import com.kjlee.gamenews.domain.news.dto.Item;
import com.kjlee.gamenews.domain.news.dto.RssResponse;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GameNewsService {
    private final WebClient webClient;
    private final XmlMapper xmlMapper;
    String[] blacklist = {"[공략]", "기사리스트", "자유게시판", "질문", "잡담",
            "유머", "공지", "모집", "점검", "당첨자", "이벤트"};

    private boolean isCleanNews(String title) {
        for (String s : blacklist) {
            if (title.contains(s)) {
                return false;
            }
        }
        return true;
    }

    private String safe(String s) {
        return (s == null) ? "" : s;
    }

    public List<GameNewsDto> fetchRssData(String query, int count, String badgePrefix) {
        String encodedQ = URLEncoder.encode(query + " when:2d", StandardCharsets.UTF_8);
        String url = "https://news.google.com/rss/search?q=" + encodedQ + "&hl=ko&gl=KR&ceid=KR:ko";

        try {
            String xml = webClient.get()
                    .uri(URI.create(url))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (xml == null || xml.isBlank()) return List.of();

            System.out.println(xml);

            RssResponse rss = xmlMapper.readValue(xml, RssResponse.class);

            System.out.println("channel null? " + (rss.channel() == null));
            System.out.println("items null? " + (rss.channel() == null || rss.channel().getItems() == null));
            System.out.println("items size = " + (rss.channel() == null || rss.channel().getItems() == null ? -1 : rss.channel().getItems().size()));

            List<Item> items = (rss != null && rss.channel() != null) ? rss.channel().getItems() : null;
            if (items == null || items.isEmpty()) return List.of();

            List<GameNewsDto> collected = new ArrayList<>();
            for (Item item : items) {
                if (collected.size() >= count) break;

                String rawTitle = safe(item.title());
                String link = safe(item.link()).trim();

                if (!isCleanNews(rawTitle)) continue;

                String sourceBadge = (badgePrefix != null) ? badgePrefix : "\uD83D\uDCF0";
                String cleanTitle = rawTitle;

                if (rawTitle.contains("인벤") || rawTitle.contains("Inven")) {
                    sourceBadge = "\uD83D\uDEE1\uFE0F [인벤]";
                    cleanTitle = cleanTitle.replace("- 인벤", "").replace("- Inven", "");
                } else if (rawTitle.contains("디스이즈게임") || rawTitle.contains("Thisisgame")) {
                    sourceBadge = "\uD83C\uDFAE [TIG]";
                    cleanTitle = cleanTitle.replace("- 디스이즈게임", "").replace("- Thisisgame", "");
                } else if (rawTitle.contains("게임메카")) {
                    sourceBadge = "\uD83E\uDD16 [메카]";
                    cleanTitle = cleanTitle.replace("- 게임메카", "");
                } else if (rawTitle.contains("게임동아")) {
                    sourceBadge = "\uD83D\uDCF0 [동아]";
                    cleanTitle = cleanTitle.replace("- 게임동아", "");
                }

                cleanTitle = cleanTitle.trim();

                String summary;
                try {
                    String rawDesc = safe(item.description());
                    summary = Jsoup.parse(rawDesc).text();
                    if (summary.isBlank()) summary = "기사 원문 보러가기";
                } catch (Exception e) {
                    summary = "기사 원문 보러가기";
                }

                String date;
                try {
                    String pub = safe(item.pubDate());
                    date = pub.length() >= 16 ? pub.substring(0, 16) : pub;
                    if (date.isBlank()) date = "최신";
                } catch (Exception e) {
                    date = "최신";
                }

                collected.add(new GameNewsDto(sourceBadge, cleanTitle, link, date, summary));
            }
            return collected;
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    @Cacheable(cacheNames = "priorityNews", key = "'top20'")
    public List<GameNewsDto> fetchPriorityNews() {
        return fetchPrioirtyNewsNoCache();
    }

    public List<GameNewsDto> fetchPrioirtyNewsNoCache() {
        List<GameNewsDto> finalList = new ArrayList<>();

        List<GameNewsDto> tig = fetchRssData("site:thisisgame.com/articles?newsId=263", 8, "🎮 [TIG]");
        finalList.addAll(tig);

        String otherQuery = "site:inven.co.kr/webzine/news OR site:gamemeca.com/view.php OR site:game.donga.com";
        List<GameNewsDto> other = fetchRssData(otherQuery, 12, null);
        finalList.addAll(other);

        return finalList;
    }
}
