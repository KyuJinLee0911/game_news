package com.kjlee.gamenews.domain.news.controller;

import com.kjlee.gamenews.domain.news.dto.GameNewsDto;
import com.kjlee.gamenews.domain.news.service.GameNewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/news")
public class NewsController {
    private final GameNewsService newsService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<GameNewsDto>> getNews() {
        List<GameNewsDto> news = newsService.fetchPriorityNews();
        return ResponseEntity.ok(news);
    }

    @GetMapping(value = "/share-text", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getShareText() {
        List<GameNewsDto> news = newsService.fetchPriorityNews();

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < news.size(); i++) {
            GameNewsDto n = news.get(i);
            sb.append(i + 1)
                    .append(". ")
                    .append(n.badge()).append(" ")
                    .append(n.title())
                    .append("\n")
                    .append(n.link())
                    .append("\n\n");
        }

        return ResponseEntity.ok(sb.toString());
    }
}
