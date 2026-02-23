package com.kjlee.gamenews.domain.news.controller;

import com.kjlee.gamenews.domain.news.dto.GameNewsDto;
import com.kjlee.gamenews.domain.news.service.GameNewsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.only;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NewsController.class)
public class NewsControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GameNewsService gameNewsService;

    private List<GameNewsDto> createSampleNews(){
        return List.of(
                new GameNewsDto("🛡️ [인벤]", "메이플 업데이트", "https://example.com/1",
                        "Mon, 10 Feb 2026", "메이플스토리 대규모 업데이트 소식"),
                new GameNewsDto("🎮 [TIG]", "신작 RPG 리뷰", "https://example.com/2",
                        "Mon, 10 Feb 2026", "기대작 RPG 상세 리뷰를 공개합니다")
        );
    }

    @Test
    @DisplayName("GET /api/news - JSON 형식으로 뉴스 목록을 반환한다.")
    void getNews_returnsJsonList() throws Exception {
        when(gameNewsService.fetchPriorityNews()).thenReturn(createSampleNews());

        mockMvc.perform(get("/api/news"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].badge").value("🛡️ [인벤]"))
                .andExpect(jsonPath("$[0].title").value("메이플 업데이트"))
                .andExpect(jsonPath("$[0].link").value("https://example.com/1"));
    }

    @Test
    @DisplayName("GET /api/news - 뉴스가 없으면 빈 배열을 반환한다.")
    void getNews_emptyList() throws Exception {
        when(gameNewsService.fetchPriorityNews()).thenReturn(List.of());

        mockMvc.perform(get("/api/news"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("GET /api/news/share-text - 텍스트 형식으로 뉴스를 반환한다.")
    void getShareText_returnsFormattedTest() throws Exception {
        when(gameNewsService.fetchPriorityNews()).thenReturn(createSampleNews());

        mockMvc.perform(get("/api/news/share-text"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/plain"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("1. 🛡️ [인벤]")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("https://example.com/1")));
    }

}
