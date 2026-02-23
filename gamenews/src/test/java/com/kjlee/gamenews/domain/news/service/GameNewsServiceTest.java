package com.kjlee.gamenews.domain.news.service;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.kjlee.gamenews.domain.news.dto.GameNewsDto;
import com.kjlee.gamenews.domain.news.utils.NewsFilterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GameNewsServiceTest {
    private final XmlMapper xmlMapper = new XmlMapper();
    private final NewsFilterService newsFilter = new NewsFilterService();
    @Mock
    private WebClient webClient;
    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;
    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;
    @Mock
    private WebClient.ResponseSpec responseSpec;
    private GameNewsService gameNewsService;

    @BeforeEach
    void SetUp() {
        gameNewsService = new GameNewsService(webClient, xmlMapper, newsFilter);
    }

    private void mockWebClientResponse(String xmlResponse) {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(java.net.URI.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(xmlResponse));
    }

    private String buildRssXml(String... items) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<rss><channel>");
        for (String item : items) {
            sb.append(item);
        }
        sb.append("</channel></rss>");
        return sb.toString();
    }

    private String buildItem(String title, String link, String description, String pubDate) {
        return "<item>"
                + "<title>" + title + "</title>"
                + "<link>" + link + "</link>"
                + "<description>" + description + "</description>"
                + "<pubDate>" + pubDate + "</pubDate>"
                + "</item>";
    }

    @Test
    @DisplayName("정상적인 RSS 응답에서 뉴스를 수집한다.")
    void fetchRssData_success() {
        String xml = buildRssXml(
                buildItem("메이플 업데이트 소식 - 인벤", "https://example.com/1",
                        "메이플스토리가 대규모 여름 업데이트를 발표했다", "Mon, 10 Feb 2026 12:00")
        );
        mockWebClientResponse(xml);

        List<GameNewsDto> result = gameNewsService.fetchRssData("메이플", 10, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).badge()).isEqualTo("🛡️ [인벤]");
        assertThat(result.get(0).title()).isEqualTo("메이플 업데이트 소식");
        assertThat(result.get(0).link()).isEqualTo("https://example.com/1");
    }

    @Test
    @DisplayName("블랙리스트 키워드가 포함된 뉴스는 제외된다.")
    void fetchRssData_filtersBlacklisted() {
        String xml = buildRssXml(
                buildItem("[공략] 보스 레이드 가이드", "https://example.com/1",
                        "보스 레이드 공략을 상세하게 정리했습니다", "Mon, 10 Feb 2026 12:00"),
                buildItem("신작 RPG 출시 예정 - 게임메카", "https://example.com/2",
                        "올해 하반기 기대작 RPG가 정식 출시를 앞두고 있다", "Mon, 10 Feb 2026 13:00")
        );
        mockWebClientResponse(xml);

        List<GameNewsDto> result = gameNewsService.fetchRssData("게임", 10, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("신작 RPG 출시 예정");
    }

    @Test
    @DisplayName("count 제한에 맞게 수집을 멈춘다.")
    void fetchRssData_respectsCount() {
        String xml = buildRssXml(
                buildItem("뉴스1 - 인벤", "https://example.com/1",
                        "첫 번째 뉴스 내용이 여기에 들어갑니다", "Mon, 10 Feb 2026 12:00"),
                buildItem("뉴스2 - 게임메카", "https://example.com/2",
                        "두 번째 뉴스 내용이 여기에 들어갑니다", "Mon, 10 Feb 2026 13:00"),
                buildItem("뉴스3 - 게임동아", "https://example.com/3",
                        "세 번째 뉴스 내용이 여기에 들어갑니다", "Mon, 10 Feb 2026 14:00")
        );
        mockWebClientResponse(xml);

        List<GameNewsDto> result = gameNewsService.fetchRssData("게임", 2, null);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("빈 RSS 응답이면 빈 리스트를 반환한다.")
    void fetchRssData_emptyResponse() {
        mockWebClientResponse("");

        List<GameNewsDto> result = gameNewsService.fetchRssData("게임", 10, null);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("API 호출 실패 시 빈 리스트를 반환한다.")
    void fetchRssData_apiFailure() {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(java.net.URI.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenThrow(new RuntimeException("Connection refused"));

        List<GameNewsDto> result = gameNewsService.fetchRssData("게임", 10, null);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("유효하지 않은 summary를 가진 뉴스는 제외된다.")
    void fetchRssData_filtersInvalidSummary() {
        String xml = buildRssXml(
                buildItem("뉴스 제목 - 인벤", "https://example.com/1",
                        "인벤", "Mon, 10 Feb 2026 12:00"),
                buildItem("좋은 뉴스 - 게임메카", "https://example.com/2",
                        "이것은 충분히 긴 유효한 뉴스 요약입니다", "Mon, 10 Feb 2026 13:00")
        );
        mockWebClientResponse(xml);

        List<GameNewsDto> result = gameNewsService.fetchRssData("게임", 10, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("좋은 뉴스");
    }
}
