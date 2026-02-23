package com.kjlee.gamenews.domain.news.utils;

import org.springframework.stereotype.Component;

@Component
public class NewsFilterService {
    private static final String[] BLACKLIST = {"[공략]", "기사리스트", "자유게시판", "질문", "잡담",
            "유머", "공지", "모집", "점검", "당첨자", "이벤트"};

    public boolean isCleanNews(String title) {
        if (title == null || title.isBlank()) return false;
        for (String s : BLACKLIST) {
            if (title.contains(s)) {
                return false;
            }
        }
        return true;
    }

    public boolean isValidSummary(String summary) {
        if (summary == null || summary.isBlank()) {
            return false;
        }

        // 의미 없는 summary 패턴
        String[] invalidPatterns = {
                "게임뉴스 인벤",
                "게임뉴스",
                "인벤"
        };

        String trimmedSummary = summary.trim();
        for (String pattern : invalidPatterns) {
            if (trimmedSummary.equalsIgnoreCase(pattern)) {
                return false;
            }
        }

        // 너무 짧은 summary 제외 (10자 미만)
        return trimmedSummary.length() >= 10;
    }

    public String resolveSourceBadge(String rawTitle, String defaultBadge) {
        if (rawTitle.contains("인벤") || rawTitle.contains("Inven")) return "🛡️ [인벤]";
        if (rawTitle.contains("디스이즈게임") || rawTitle.contains("Thisisgame")) return "🎮 [TIG]";
        if (rawTitle.contains("게임메카")) return "🤖 [메카]";
        if (rawTitle.contains("게임동아")) return "📰 [동아]";
        return (defaultBadge != null) ? defaultBadge : "📰";
    }

    public String cleanTitle(String rawTitle) {
        return rawTitle
                .replace("- 인벤", "").replace("- Inven", "")
                .replace("- 디스이즈게임", "").replace("- Thisisgame", "")
                .replace("- 게임메카", "")
                .replace("- 게임동아", "")
                .trim();
    }
}
