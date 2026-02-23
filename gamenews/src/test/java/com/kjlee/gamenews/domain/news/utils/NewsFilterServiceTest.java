package com.kjlee.gamenews.domain.news.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

public class NewsFilterServiceTest {
    private final NewsFilterService filterService = new NewsFilterService();

    @Nested
    @DisplayName("isCleanNews - 블랙리스트 필터링")
    class IsCleanNewsTest {
        @Test
        @DisplayName("정상적인 게임 뉴스 제목은 통과한다.")
        void cleanTitlePasses() {
            assertThat(filterService.isCleanNews("메이플스토리 신규 업데이트 공개")).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "[공략] 보스 레이드 가이드",
                "자유게시판 인기글 모음",
                "12월 점검 안내",
                "이벤트 당첨자 발표",
                "길드원 모집합니다"
        })
        @DisplayName("블랙리스트 키워드가 포함된 제목은 필터링된다.")
        void blacklistedTitleFilltered(String title) {
            assertThat(filterService.isCleanNews(title)).isFalse();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("null 또는 빈 제목은 필터링된다.")
        void nullOrEmptyTitleFiltered(String title) {
            assertThat(filterService.isCleanNews(title)).isFalse();
        }
    }

    @Nested
    @DisplayName("isValidSummary - 요약 유효성 검증")
    class IsValidSummaryTest {
        @Test
        @DisplayName("10자 이상의 정상 요약은 통과한다.")
        void validSummaryPasses() {
            assertThat(filterService.isValidSummary("메이플스토리가 대규모 업데이트를 예고했다")).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"게임뉴스 인벤", "게임뉴스", "인벤"})
        @DisplayName("무의미한 패턴의 요약은 필터링된다")
        void invalidPatternFiltered(String summary) {
            assertThat(filterService.isValidSummary(summary)).isFalse();
        }

        @Test
        @DisplayName("10자 미만의 짧은 요약은 필터링된다")
        void shortSummaryFiltered() {
            assertThat(filterService.isValidSummary("짧은요약")).isFalse();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("null 또는 빈 요약은 필터링된다")
        void nullOrEmptyFiltered(String summary) {
            assertThat(filterService.isValidSummary(summary)).isFalse();
        }
    }

    @Nested
    @DisplayName("resolveSourceBadge - 출처 뱃지 매핑")
    class ResolveSourceBadgeTest {
        @Test
        @DisplayName("인벤 포함 제목은 인벤 뱃지를 반환한다.")
        void invenBadge() {
            assertThat(filterService.resolveSourceBadge("신작 MMORPG 리뷰 - 인벤", null))
                    .isEqualTo("🛡️ [인벤]");
        }

        @Test
        @DisplayName("디스이즈게임 포함 제목은 TIG 뱃지를 반환한다")
        void tigBadge() {
            assertThat(filterService.resolveSourceBadge("E3 현장 리포트 - 디스이즈게임", null))
                    .isEqualTo("🎮 [TIG]");
        }

        @Test
        @DisplayName("게임메카 포함 제목은 메카 뱃지를 반환한다")
        void mecaBadge() {
            assertThat(filterService.resolveSourceBadge("신작 소식 - 게임메카", null))
                    .isEqualTo("🤖 [메카]");
        }

        @Test
        @DisplayName("게임동아 포함 제목은 동아 뱃지를 반환한다")
        void dongaBadge() {
            assertThat(filterService.resolveSourceBadge("업계 동향 - 게임동아", null))
                    .isEqualTo("📰 [동아]");
        }

        @Test
        @DisplayName("출처를 알 수 없는 제목은 기본 뱃지를 반환한다")
        void defaultBadge() {
            assertThat(filterService.resolveSourceBadge("일반 게임 뉴스", "🔥 [기본]"))
                    .isEqualTo("🔥 [기본]");
        }
    }

    @Nested
    @DisplayName("cleanTitle - 출처 문자열 제거")
    class CleanTitleTest {
        @Test
        @DisplayName("인벤 출처 문자열이 제거된다.")
        void removeInven() {
            assertThat(filterService.cleanTitle("신작 리뷰 - 인벤"))
                    .isEqualTo("신작 리뷰");
        }

        @Test
        @DisplayName("디스이즈게임 출처 문자열이 제거된다.")
        void removeTig() {
            assertThat(filterService.cleanTitle("E3 리포트 - 디스이즈게임"))
                    .isEqualTo("E3 리포트");
        }

        @Test
        @DisplayName("출처가 없는 제목은 그대로 반환된다.")
        void noSourceUnchanged() {
            assertThat(filterService.cleanTitle("일반 게임 뉴스"))
                    .isEqualTo("일반 게임 뉴스");
        }
    }
}
